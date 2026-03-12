package com.bulsee.service;

import com.bulsee.dao.*;
import com.bulsee.dto.RegionDataDTO;
import com.bulsee.repository.FAQRepository;
import com.bulsee.vo.*;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OpenAIService openAIService;
    private final FastAPIClient fastAPIClient;
    private final FAQRepository faqRepository;
    private final StatisticsService statisticsService;

    private final AwsStationDAO awsStationDAO;
    private final ForestAgeDistributionDAO forestAgeDistributionDAO;
    private final FireLandcoverMatchDAO fireLandcoverMatchDAO;

    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    private final Map<String, AwsStation> regionSearchMap = new HashMap<>();

    @PostConstruct
    public void initRegionKeywords() {
        List<AwsStation> allStations = awsStationDAO.findAll();
        log.info("🚀 지역 데이터 검색 인덱싱 시작... (총 {}개)", allStations.size());

        for (AwsStation station : allStations) {
            String fullName = station.getName(); // 예: "강원도 춘천시", "서울특별시"

            // 1. 전체 이름으로도 찾을 수 있게 저장 ("강원도 춘천시")
            regionSearchMap.put(fullName, station);

            // 2. 띄어쓰기 기준으로 쪼개서 등록
            String[] parts = fullName.split(" ");

            for (String part : parts) {
                // "춘천시" -> "춘천"으로 접미사 제거하여 등록
                String shortName = removeSuffix(part);

                // 2글자 이상인 경우만 등록
                if (shortName.length() >= 2) {
                    regionSearchMap.put(shortName, station);
                }
            }
        }
        log.info("✅ 지역 검색 키워드 생성 완료! (총 {}개 키워드)", regionSearchMap.size());
    }

    /**
     * 행정구역 접미사 제거 (춘천시 -> 춘천)
     */
    private String removeSuffix(String name) {
        return name.replaceAll("(특별시|광역시|자치시|자치도|시|군|구|도)$", "");
    }

    /**
     * DB에서 지역 찾기 (예측용) - 개선 버전
     */
    private AwsStation findLocationFromDB(String userMessage) {
        // 1차: 긴 키워드부터 정확히 매칭
        List<String> sortedKeys = new ArrayList<>(regionSearchMap.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String keyword : sortedKeys) {
            if (userMessage.contains(keyword)) {
                AwsStation station = regionSearchMap.get(keyword);
                log.info("🎯 지역 매칭 성공! 키워드: '{}' -> DB명: '{}'", keyword, station.getName());
                return station;
            }
        }

        // 2차: 실패하면 사용자 입력에서 단어를 추출하고 접미사 제거 후 재시도
        log.info("🔍 1차 매칭 실패, 접미사 제거 후 재시도...");

        String[] words = userMessage.split("\\s+");
        for (String word : words) {
            // 2글자 이상 단어만 처리
            if (word.length() < 2) continue;

            // 접미사 제거 ("서울시" → "서울")
            String cleaned = removeSuffix(word);

            if (cleaned.length() >= 2 && regionSearchMap.containsKey(cleaned)) {
                AwsStation station = regionSearchMap.get(cleaned);
                log.info("🎯 지역 매칭 성공 (정규화 후)! 원본: '{}' → 정규화: '{}' -> DB명: '{}'",
                        word, cleaned, station.getName());
                return station;
            }
        }

        log.warn("❌ 지역 매칭 실패: {}", userMessage);
        return null;
    }

    public ChatResponse processMessage(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();

        log.info("📩 메시지 수신 [세션: {}]: {}", sessionId, userMessage);

        try {
            List<ChatMessage> history = conversationHistory.getOrDefault(sessionId, new ArrayList<>());

            // 의도 판단
            boolean needsPrediction = detectPredictionIntent(userMessage);
            boolean needsStatistics = detectStatisticsIntent(userMessage);
            boolean hasPredictionKeywordOnly = detectPredictionKeywordOnly(userMessage);

            log.info("🔍 의도 판단: {} (예측={}, 통계={}, 예측키워드만={})",
                    userMessage, needsPrediction, needsStatistics, hasPredictionKeywordOnly);

            ChatResponse response;

            if (needsPrediction) {
                response = handlePredictionRequest(userMessage, history);
            } else if (hasPredictionKeywordOnly) {
                response = handlePredictionKeywordOnly();
            } else if (needsStatistics) {
                response = handleStatisticsRequest(userMessage);
            } else {
                response = handleGeneralChat(userMessage, history);
            }

            history.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));
            history.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), response.getMessage()));

            if (history.size() > 10) {
                history = new ArrayList<>(history.subList(history.size() - 10, history.size()));
            }

            conversationHistory.put(sessionId, history);

            log.info("✅ 응답 전송 완료 [세션: {}]", sessionId);
            return response;

        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 [세션: {}]: {}", sessionId, e.getMessage(), e);
            return ChatResponse.builder()
                    .message("죄송합니다. 오류가 발생했습니다. 다시 시도해주세요.")
                    .type("error")
                    .build();
        }
    }

    /**
     * 예측 의도 감지
     */
    private boolean detectPredictionIntent(String message) {
        String lowerMessage = message.toLowerCase();

        // 1단계: FAQ 키워드 먼저 체크
        String[] faqKeywords = {
                "예방", "방법", "신고", "119", "1688-3119",
                "도움", "help", "기능", "사용법", "안내", "어떻게"
        };

        for (String keyword : faqKeywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("💬 FAQ 키워드 감지: {}", keyword);
                return false;
            }
        }

        // 2단계: 통계 키워드 체크
        String[] statisticsKeywords = {
                "통계", "몇번", "몇 번", "몇건", "몇 건", "얼마나",
                "발생", "현황", "데이터", "많이", "순위"
        };

        for (String keyword : statisticsKeywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("📊 통계 키워드 감지: {}", keyword);
                return false;
            }
        }

        // 3단계: 예측 관련 키워드 체크
        boolean hasLocationKeyword = findLocationFromDB(message) != null;
        boolean hasPredictionKeyword = false;

        String[] predictionKeywords = {
                "위험도", "예측", "확률", "위험", "안전", "어때", "어떤가",
                "내일", "모레", "오늘", "지금", "현재", "일 후"
        };

        for (String keyword : predictionKeywords) {
            if (lowerMessage.contains(keyword)) {
                hasPredictionKeyword = true;
                log.info("🔮 예측 키워드 감지: {}", keyword);
                break;
            }
        }

        return hasLocationKeyword && hasPredictionKeyword;
    }

    /**
     * 예측 키워드만 있는지 체크 (지역 없음)
     */
    private boolean detectPredictionKeywordOnly(String message) {
        String lowerMessage = message.toLowerCase();

        // 지역이 있으면 false
        if (findLocationFromDB(message) != null) {
            return false;
        }

        // 통계나 FAQ 키워드가 있으면 false
        if (detectStatisticsIntent(message)) {
            return false;
        }

        String[] faqKeywords = {
                "예방", "방법", "신고", "119", "1688-3119",
                "도움", "help", "기능", "사용법", "안내"
        };
        for (String keyword : faqKeywords) {
            if (lowerMessage.contains(keyword)) {
                return false;
            }
        }

        // 예측 키워드만 있는지 체크
        String[] predictionKeywords = {
                "예측", "위험도", "확률", "위험"
        };

        for (String keyword : predictionKeywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("🔮 예측 키워드만 감지 (지역 없음): {}", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * 통계 의도 감지
     */
    private boolean detectStatisticsIntent(String message) {
        String lower = message.toLowerCase();

        String[] statisticsKeywords = {
                "통계", "몇번", "몇 번", "몇건", "몇 건", "얼마나",
                "발생", "현황", "데이터", "많이", "순위"
        };

        for (String keyword : statisticsKeywords) {
            if (lower.contains(keyword)) {
                log.info("📊 통계 키워드 감지: {}", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * 예측 요청 처리
     */
    private ChatResponse handlePredictionRequest(String userMessage, List<ChatMessage> history) {
        log.info("🔮 RegionDataDTO를 사용한 정밀 예측 시작");
        try {
            // 1. DB에서 지역 정보 찾기
            log.info("1️⃣ 지역 정보 찾기 시작...");
            AwsStation station = findLocationFromDB(userMessage);
            log.info("1️⃣ 지역 정보 찾기 완료: {}", station != null ? station.getName() : "null");

            if (station == null) {
                return ChatResponse.builder()
                        .message("어느 지역의 산불 위험도가 궁금하신가요? '춘천 산불 어때?', '강원도 위험도 알려줘'와 같이 지역명을 포함해서 물어봐주세요! 📍")
                        .type("text")
                        .build();
            }

            // 2. 지역명 분리 및 정규화
            log.info("2️⃣ 지역명 분리 시작...");
            String[] parts = station.getName().split(" ");
            String sido = parts[0];
            String sigungu = (parts.length > 1) ? parts[1] : null;

            sido = normalizeSidoName(sido);
            log.info("2️⃣ 정규화 완료: sido={}, sigungu={}", sido, sigungu);

            // 3. 산림 데이터 조회
            log.info("3️⃣ 산림 데이터 조회 시작...");
            ForestAgeDistribution forest = null;

            if (sigungu != null) {
                sigungu = removeSuffix(sigungu);
                log.info("3️⃣ 시군구로 조회 시도: sido={}, sigungu={}", sido, sigungu);
                forest = forestAgeDistributionDAO.findBySidoAndSigungu(sido, sigungu).orElse(null);
            }

            if (forest == null) {
                log.info("3️⃣ 시도 전체 조회 시도: sido={}", sido);
                forest = forestAgeDistributionDAO.findFirstBySido(sido).orElse(null);

                if (forest != null) {
                    log.info("3️⃣ ✅ 대표 지역 데이터 사용: {} {}", forest.getSido(), forest.getSigungu());
                }
            }

            if (forest == null) {
                log.warn("3️⃣ ❌ 산림 데이터 없음: sido={}, sigungu={}", sido, sigungu);
                return ChatResponse.builder()
                        .message("죄송합니다. 해당 지역의 산림 데이터가 부족하여 정확한 예측이 어렵습니다. 🌲\n\n" +
                                "다른 지역을 검색해보시거나, 시/군/구 단위로 더 구체적으로 물어봐주세요!")
                        .type("text")
                        .build();
            }

            String actualSigungu = removeSuffix(forest.getSigungu());
            log.info("3️⃣ 산림 데이터 조회 완료: actualSigungu={}", actualSigungu);

            // 4. 토지피복 데이터 조회
            log.info("4️⃣ 토지피복 데이터 조회 시작...");
            FireLandcoverMatch landcover = fireLandcoverMatchDAO.findFirstBySidoAndSigunguOrderByMatchedYearDesc(sido, actualSigungu)
                    .orElse(new FireLandcoverMatch());
            log.info("4️⃣ 토지피복 데이터 조회 완료");

            // 5. 파이썬 ML 서버 호출
            log.info("5️⃣ ML 서버 호출 시작...");
            PredictRequest predictRequest = createRealPredictRequest(station, forest, landcover);
            log.info("5️⃣ 요청 생성 완료: {}", predictRequest);

            PredictResponse res = fastAPIClient.predict(predictRequest);
            log.info("5️⃣ ML 서버 응답 수신: risk={}", res.getRisk());

            // 6. 응답 데이터 변환
            log.info("6️⃣ 응답 변환 시작...");
            RegionDataDTO regionData = RegionDataDTO.builder()
                    .name(station.getName())
                    .lat(station.getLat())
                    .lng(station.getLon())
                    .stnId(station.getStn())
                    .riskLevel(res.getRisk())
                    .fireProbability(res.getScore())
                    .typicalArea(res.getTypicalAreaHa())
                    .worstArea(res.getWorstAreaHa())
                    .spreadDir(res.getDirection())
                    .temperature(res.getWeather().getTemp())
                    .humidity(res.getWeather().getHumidity())
                    .windSpeed(res.getWeather().getWindSpeed())
                    .precipitation(res.getWeather().getPrecipitation())
                    .build();
            log.info("6️⃣ 응답 변환 완료");

            // 7. OpenAI를 통한 자연어 답변 생성
            log.info("7️⃣ OpenAI 답변 생성 시작...");
            String answer = openAIService.formatPredictionResult(userMessage, res);
            log.info("7️⃣ OpenAI 답변 생성 완료");

            return ChatResponse.builder()
                    .message(answer)
                    .type("prediction")
                    .data(regionData)
                    .build();

        } catch (Exception e) {
            log.error("❌ 챗봇 예측 처리 실패: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .message("죄송합니다. 예측 중 오류가 발생했습니다. 🙏\n\n" +
                            "• 지역명을 다시 확인해주세요.\n" +
                            "• 잠시 후 다시 시도해주세요.\n\n" +
                            "예시: \"강원도 산불 위험도\", \"춘천 산불 예측\"")
                    .type("error")
                    .build();
        }
    }

    /**
     * 시도명 정규화 (강원도 → 강원특별자치도)
     */
    private String normalizeSidoName(String sido) {
        Map<String, String> sidoMapping = new HashMap<>();
        sidoMapping.put("강원도", "강원특별자치도");
        sidoMapping.put("강원특별자치도", "강원특별자치도");
        sidoMapping.put("제주도", "제주특별자치도");
        sidoMapping.put("제주특별자치도", "제주특별자치도");
        sidoMapping.put("전라북도", "전라북도");
        sidoMapping.put("전북특별자치도", "전라북도");
        sidoMapping.put("경기도", "경기도");
        sidoMapping.put("충청북도", "충청북도");
        sidoMapping.put("충청남도", "충청남도");
        sidoMapping.put("전라남도", "전라남도");
        sidoMapping.put("경상북도", "경상북도");
        sidoMapping.put("경상남도", "경상남도");
        sidoMapping.put("서울특별시", "서울특별시");
        sidoMapping.put("부산광역시", "부산광역시");
        sidoMapping.put("대구광역시", "대구광역시");
        sidoMapping.put("인천광역시", "인천광역시");
        sidoMapping.put("광주광역시", "광주광역시");
        sidoMapping.put("대전광역시", "대전광역시");
        sidoMapping.put("울산광역시", "울산광역시");
        sidoMapping.put("세종특별자치시", "세종특별자치시");

        return sidoMapping.getOrDefault(sido, sido);
    }

    /**
     * 예측 키워드만 있을 때 지역 요청
     */
    private ChatResponse handlePredictionKeywordOnly() {
        log.info("🔮 예측 키워드만 감지 - 지역 입력 요청");

        return ChatResponse.builder()
                .message("🔥 산불 위험도 예측을 도와드릴게요!\n\n" +
                        "어느 지역의 위험도가 궁금하신가요?\n\n" +
                        "예시:\n" +
                        "• \"강원도 산불 위험도\"\n" +
                        "• \"춘천 산불 예측\"\n" +
                        "• \"서울 위험도 알려줘\"")
                .type("text")
                .quickReplies(createLocationQuickReplies())
                .build();
    }

    /**
     * 예측 요청을 위한 실제 데이터 매핑
     */
    private PredictRequest createRealPredictRequest(AwsStation s, ForestAgeDistribution f, FireLandcoverMatch l) {
        return PredictRequest.builder()
                .lat(s.getLat())
                .lon(s.getLon())
                // ForestAgeDistribution 필드 (null → 0.0)
                .forestArea(f.getForestArea() != null ? f.getForestArea() : 0.0)
                .coniferRatio(f.getConiferRatio() != null ? f.getConiferRatio() : 0.0)
                .broadleafRatio(f.getBroadleafRatio() != null ? f.getBroadleafRatio() : 0.0)
                .mixedRatio(f.getMixedRatio() != null ? f.getMixedRatio() : 0.0)
                .timberStockSum(f.getTimberStockSum() != null ? f.getTimberStockSum() : 0.0)
                .timberStockMean(f.getTimberStockMean() != null ? f.getTimberStockMean() : 0.0)
                .forestAreaFromAge(f.getForestAreaFromAge() != null ? f.getForestAreaFromAge() : 0.0)
                .age1Ratio(f.getAge1Ratio() != null ? f.getAge1Ratio() : 0.0)
                .age2Ratio(f.getAge2Ratio() != null ? f.getAge2Ratio() : 0.0)
                .age3Ratio(f.getAge3Ratio() != null ? f.getAge3Ratio() : 0.0)
                .age4Ratio(f.getAge4Ratio() != null ? f.getAge4Ratio() : 0.0)
                .age5Ratio(f.getAge5Ratio() != null ? f.getAge5Ratio() : 0.0)
                .age6Ratio(f.getAge6Ratio() != null ? f.getAge6Ratio() : 0.0)
                // FireLandcoverMatch 필드 (null → 0.0)
                .forestRatio(l.getForestRatio() != null ? l.getForestRatio() : 0.0)
                .urbanRatio(l.getUrbanRatio() != null ? l.getUrbanRatio() : 0.0)
                .agriRatio(l.getAgriRatio() != null ? l.getAgriRatio() : 0.0)
                .grassRatio(l.getGrassRatio() != null ? l.getGrassRatio() : 0.0)
                .build();
    }

    /**
     * 통계 요청 처리
     */
    private ChatResponse handleStatisticsRequest(String userMessage) {
        log.info("📊 통계 요청 처리 시작");

        try {
            Integer year = extractYear(userMessage);

            if (year != null && year == -1) {
                return ChatResponse.builder()
                        .message("📊 죄송합니다. 현재 **2016년부터 2025년**까지의 통계만 제공하고 있습니다.\n\n" +
                                "예시:\n" +
                                "• \"2024년 산불 통계\"\n" +
                                "• \"2020년 강원도 산불 통계\"")
                        .type("text")
                        .build();
            }

            String region = extractRegion(userMessage);
            String displayRegion = region; // 📌 화면 표시용 원본 보존

            // 검색용 패턴으로 변환
            if (region != null && !region.isEmpty()) {
                region = region.replaceAll("(특별자치도|특별시|광역시|도)$", "") + "%";
                log.info("📍 지역명 정규화: {} → {}", displayRegion, region);
            }

            String statistics;

            if (year != null && region != null) {
                statistics = statisticsService.getStatisticsByYearAndRegion(year, region, displayRegion);
            } else if (year != null) {
                statistics = statisticsService.getStatisticsByYear(year);
            } else if (region != null) {
                statistics = statisticsService.getStatisticsByRegion(region, displayRegion);
            } else {
                statistics = "📊 통계를 확인하시려면 년도나 지역을 함께 말씀해주세요!\n\n" +
                        "예시:\n" +
                        "• \"2024년 산불 통계\"\n" +
                        "• \"강원도 산불 통계\"\n" +
                        "• \"2024년 강원도 산불 통계\"";
            }

            return ChatResponse.builder()
                    .message(statistics)
                    .type("statistics")
                    .build();

        } catch (Exception e) {
            log.error("❌ 통계 처리 실패: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .message("통계 정보를 가져오는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    .type("error")
                    .build();
        }
    }

    /**
     * 일반 대화 처리
     */
    private ChatResponse handleGeneralChat(String userMessage, List<ChatMessage> history) {
        log.info("💬 일반 대화 처리");

        Optional<FAQ> faqAnswer = searchFAQ(userMessage);

        if (faqAnswer.isPresent()) {
            FAQ faq = faqAnswer.get();
            log.info("✅ FAQ에서 답변 발견: {}", faq.getQuestion());

            faq.setViewCount(faq.getViewCount() + 1);
            faqRepository.save(faq);

            return ChatResponse.builder()
                    .message(faq.getAnswer())
                    .type("faq")
                    .data(faq)
                    .build();
        }

        log.info("💰 OpenAI API 호출 (FAQ 매칭 실패)");
        String responseMessage = openAIService.chat(userMessage, history);

        return ChatResponse.builder()
                .message(responseMessage)
                .type("text")
                .build();
    }

    /**
     * FAQ 검색
     */
    private Optional<FAQ> searchFAQ(String userMessage) {
        String normalized = userMessage.toLowerCase().trim();
        List<FAQ> allFaqs = faqRepository.findAll();

        log.info("🔍 FAQ 검색 시작: 총 {}개 FAQ 데이터", allFaqs.size());

        for (FAQ faq : allFaqs) {
            String keywords = faq.getKeywords().toLowerCase();
            String[] keywordArray = keywords.split(",");

            for (String keyword : keywordArray) {
                String trimmedKeyword = keyword.trim();
                if (normalized.contains(trimmedKeyword)) {
                    log.info("✨ 키워드 매칭 성공: '{}' → FAQ: {}", trimmedKeyword, faq.getQuestion());
                    return Optional.of(faq);
                }
            }
        }

        log.info("❌ FAQ 매칭 실패");
        return Optional.empty();
    }

    /**
     * 년도 추출 (2016~2025년 데이터만 존재, 친절한 안내)
     */
    private Integer extractYear(String message) {
        // 4자리 숫자 패턴 찾기
        Pattern pattern = Pattern.compile("(\\d{4})");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));

            // 데이터 범위 체크 (2016~2025)
            if (year >= 2016 && year <= 2025) {
                log.info("📅 년도 추출: {}", year);
                return year;
            } else if (year >= 1990 && year <= 2040) {
                // 합리적인 년도지만 데이터가 없는 경우
                log.warn("⚠️ 데이터 범위 밖 년도: {} (데이터: 2016~2025)", year);
                return -1;  // 특수 값: 범위 밖
            }
        }

        return null;
    }

    /**
     * 지역 추출 (시도 우선, 시군구 후순위)
     */
    private String extractRegion(String message) {
        // 1단계: 시도 전체명 정확 매칭 (최우선)
        String[] provinces = {
                "강원도", "경기도", "충청북도", "충청남도",
                "전라북도", "전라남도", "경상북도", "경상남도", "제주도",
                "서울특별시", "부산광역시", "대구광역시", "인천광역시",
                "광주광역시", "대전광역시", "울산광역시", "세종특별자치시"
        };

        for (String province : provinces) {
            if (message.contains(province)) {
                log.info("📍 시도 추출 (전체명): {}", province);
                return province;
            }
        }

        // 2단계: 시도 축약형 매칭
        Map<String, String> provinceMap = new HashMap<>();
        provinceMap.put("강원", "강원도");
        provinceMap.put("경기", "경기도");
        provinceMap.put("충북", "충청북도");
        provinceMap.put("충남", "충청남도");
        provinceMap.put("전북", "전라북도");
        provinceMap.put("전남", "전라남도");
        provinceMap.put("경북", "경상북도");
        provinceMap.put("경남", "경상남도");
        provinceMap.put("제주", "제주도");
        provinceMap.put("서울", "서울특별시");
        provinceMap.put("부산", "부산광역시");
        provinceMap.put("대구", "대구광역시");
        provinceMap.put("인천", "인천광역시");
        provinceMap.put("광주", "광주광역시");
        provinceMap.put("대전", "대전광역시");
        provinceMap.put("울산", "울산광역시");
        provinceMap.put("세종", "세종특별자치시");

        for (Map.Entry<String, String> entry : provinceMap.entrySet()) {
            if (message.contains(entry.getKey())) {
                log.info("📍 시도 추출 (축약): {} → {}", entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }

        // 3단계: 시군구 찾기 (시도가 없을 경우만)
        List<String> sortedKeys = new ArrayList<>(regionSearchMap.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String keyword : sortedKeys) {
            if (message.contains(keyword)) {
                AwsStation station = regionSearchMap.get(keyword);
                String fullName = station.getName();

                String[] parts = fullName.split(" ");
                String region = parts.length > 1 ? parts[1] : parts[0];

                log.info("📍 시군구 추출: {} (원본: {})", region, fullName);
                return region;
            }
        }

        return null;
    }

    /**
     * 빠른 답장 버튼 생성
     */
    private List<QuickReply> createLocationQuickReplies() {
        List<QuickReply> replies = new ArrayList<>();

        replies.add(QuickReply.builder()
                .label("서울 산불 위험도")
                .value("서울 내일 산불 위험도 알려줘")
                .build());

        replies.add(QuickReply.builder()
                .label("강원도 산불 위험도")
                .value("강원도 내일 산불 위험도 알려줘")
                .build());

        replies.add(QuickReply.builder()
                .label("부산 산불 위험도")
                .value("부산 내일 산불 위험도 알려줘")
                .build());

        return replies;
    }

    /**
     * 세션 히스토리 삭제
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("🗑️ 세션 히스토리 삭제: {}", sessionId);
    }
}