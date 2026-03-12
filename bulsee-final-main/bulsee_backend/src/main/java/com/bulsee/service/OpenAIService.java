package com.bulsee.service;

import com.bulsee.vo.PredictResponse;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final StatisticsService statisticsService;

    /**
     * 일반 대화 처리 (FAQ 기반)
     */
    public String chat(String userMessage, List<ChatMessage> history) {
        String lower = userMessage.toLowerCase();

        // 인사
        if (lower.contains("안녕") || lower.contains("hello") || lower.contains("hi")) {
            return "안녕하세요! 🔥 산불 예측 AI 챗봇입니다.\n\n" +
                    "저는 다음과 같은 도움을 드릴 수 있어요:\n" +
                    "• 지역별 산불 위험도 예측\n" +
                    "• 산불 예방 방법 안내\n" +
                    "• 산불 신고 방법 안내\n" +
                    "• 산불 통계 정보\n\n" +
                    "예: '강원도 산불 위험도 알려줘'";
        }

        // 통계
        if (lower.contains("통계") || lower.contains("데이터") ||
                lower.contains("발생 건수") || lower.contains("몇 건")) {

            String region = extractRegionFromMessage(userMessage);
            String year = extractYearFromMessage(userMessage);

            StringBuilder response = new StringBuilder();
            response.append("📊 산불 통계 정보\n\n");

            // 지역 + 연도
            if (region != null && year != null) {
                int yearInt = Integer.parseInt(year);
                // region을 displayRegion으로도 전달
                response.append(statisticsService.getStatisticsByYearAndRegion(yearInt, region, region));
            }
            // 지역만
            else if (region != null) {
                response.append(statisticsService.getStatisticsByRegion(region, region));
            }
            // 연도만
            else if (year != null) {
                int yearInt = Integer.parseInt(year);
                response.append(statisticsService.getStatisticsByYear(yearInt));
            }
            // 전체
            else {
                response.append("**전국 산불 통계**\n\n");
                response.append("구체적인 통계를 확인하시려면:\n");
                response.append("• \"2024년 산불 통계\"\n");
                response.append("• \"강원도 산불 통계\"\n");
                response.append("• \"2024년 강원도 산불 통계\"\n");
                response.append("\n처럼 년도나 지역을 함께 말씀해주세요!");
            }

            return response.toString();
        }

        // 예방법
        if (lower.contains("예방") || lower.contains("방법")) {
            return "🌲 산불 예방 수칙\n\n" +
                    "✅ 해야 할 것:\n" +
                    "• 산행 전 산불 위험 정보 확인\n" +
                    "• 지정된 등산로만 이용\n" +
                    "• 산불 발견 즉시 신고 (119)\n\n" +
                    "❌ 하지 말아야 할 것:\n" +
                    "• 산에서 담배 피우기\n" +
                    "• 화기 취급 (버너, 라이터 등)\n" +
                    "• 쓰레기 소각\n" +
                    "• 인화물질 반입\n\n" +
                    "💡 작은 불씨도 큰 산불로 이어질 수 있습니다!";
        }

        // 신고
        if (lower.contains("신고") || lower.contains("119") || lower.contains("전화")) {
            return "🚨 산불 신고 방법\n\n" +
                    "📞 긴급 신고:\n" +
                    "• 119 (소방서)\n" +
                    "• 1688-3119 (산림청)\n\n" +
                    "📱 스마트폰 앱:\n" +
                    "• '산불 신고' 앱 사용\n" +
                    "• 정확한 위치 정보 자동 전송\n\n" +
                    "📍 신고 시 알려야 할 정보:\n" +
                    "• 정확한 위치 (주소, 랜드마크)\n" +
                    "• 산불 규모 및 상황\n" +
                    "• 연락처\n\n" +
                    "⚠️ 발견 즉시 빠른 신고가 피해를 줄입니다!";
        }

        // 감사
        if (lower.contains("고마") || lower.contains("감사") || lower.contains("thanks")) {
            return "천만에요! 😊 더 궁금한 점이 있으시면 언제든 물어보세요!\n\n" +
                    "산불 예방은 우리 모두의 책임입니다. 🌲";
        }

        // 도움말
        if (lower.contains("도움") || lower.contains("help") || lower.contains("기능")) {
            return "💬 이용 가능한 기능\n\n" +
                    "🔮 산불 위험도 예측:\n" +
                    "• '강원도 산불 위험도 알려줘'\n" +
                    "• '서울 산불 위험도는?'\n\n" +
                    "📊 통계 정보:\n" +
                    "• '산불 통계 알려줘'\n" +
                    "• '2025년 산불 통계'\n" +
                    "• '강원도 산불 통계'\n\n" +
                    "ℹ️ 정보 문의:\n" +
                    "• '산불 예방 방법 알려줘'\n" +
                    "• '산불 신고는 어떻게 해?'\n\n" +
                    "무엇을 도와드릴까요?";
        }

        // 기본 응답
        return "죄송하지만 잘 이해하지 못했습니다. 🤔\n\n" +
                "다음과 같이 물어보실 수 있어요:\n\n" +
                "🔮 예측: '강원도 산불 위험도 알려줘'\n" +
                "📊 통계: '산불 통계' 또는 '2025년 통계'\n" +
                "ℹ️ 정보: '산불 예방 방법 알려줘'\n" +
                "🚨 신고: '산불 신고 방법은?'\n" +
                "💬 도움말: '도움말' 또는 '기능'";
    }

    /**
     * 예측 결과를 자연스러운 문장으로 변환
     */
    public String formatPredictionResult(String userMessage, PredictResponse prediction) {
        if (prediction == null) {
            return "예측 결과를 받지 못했습니다.";
        }

        String risk = prediction.getRisk();
        Double typicalArea = prediction.getTypicalAreaHa();
        Double worstArea = prediction.getWorstAreaHa();
        String direction = prediction.getDirection();

        // 기본값 설정
        if (risk == null) risk = "알 수 없음";
        if (typicalArea == null) typicalArea = 0.0;
        if (worstArea == null) worstArea = 0.0;
        if (direction == null) direction = "정보 없음";

        // 이모지 및 위험도 텍스트
        String emoji = "🟢";
        String riskText = risk;

        if (risk.contains("위험") || risk.contains("높음") || risk.contains("high")) {
            emoji = "🔴";
        } else if (risk.contains("보통") || risk.contains("medium")) {
            emoji = "🟡";
        } else if (risk.contains("낮음") || risk.contains("안전") || risk.contains("low")) {
            emoji = "🟢";
        }

        // 메시지 구성
        StringBuilder message = new StringBuilder();
        message.append(String.format("%s 산불 위험도: %s\n\n", emoji, riskText));

        // 기본 정보
        message.append("📊 예측 정보\n");
        message.append(String.format("• 일반적 피해 예상: %.2f ha\n", typicalArea));
        message.append(String.format("• 최악의 경우: %.2f ha\n", worstArea));

        // 날씨 정보
        if (prediction.getWeather() != null) {
            PredictResponse.WeatherData weather = prediction.getWeather();
            message.append("\n🌤️ 기준 날씨\n");
            message.append(String.format("• 온도: %.1f°C\n", weather.getTemp() != null ? weather.getTemp() : 0.0));
            message.append(String.format("• 풍속: %.1f m/s\n", weather.getWindSpeed() != null ? weather.getWindSpeed() : 0.0));
            message.append(String.format("• 풍향: %s\n", direction));
            message.append(String.format("• 습도: %.0f%%\n", weather.getHumidity() != null ? weather.getHumidity() : 0.0));
            if (weather.getPrecipitation() != null && weather.getPrecipitation() > 0) {
                message.append(String.format("• 강수량: %.1f mm\n", weather.getPrecipitation()));
            }
        }

        // 시간별 예측
        if (prediction.getHourlyPredictions() != null && !prediction.getHourlyPredictions().isEmpty()) {
            message.append("\n⏰ 시간별 피해 예상\n");
            Map<String, Double> hourly = prediction.getHourlyPredictions();
            if (hourly.containsKey("1h")) message.append(String.format("• 1시간: %.2f ha\n", hourly.get("1h")));
            if (hourly.containsKey("3h")) message.append(String.format("• 3시간: %.2f ha\n", hourly.get("3h")));
            if (hourly.containsKey("6h")) message.append(String.format("• 6시간: %.2f ha\n", hourly.get("6h")));
            if (hourly.containsKey("12h")) message.append(String.format("• 12시간: %.2f ha\n", hourly.get("12h")));
            if (hourly.containsKey("24h")) message.append(String.format("• 24시간: %.2f ha\n", hourly.get("24h")));
        }

        // 권고 사항
        message.append("\n");
        if (risk.contains("위험") || risk.contains("높음")) {
            message.append("⚠️ 산불 위험이 매우 높습니다!\n\n");
            message.append("🚫 긴급 권고:\n");
            message.append("• 산행 자제를 적극 권고합니다\n");
            message.append("• 모든 화기 사용을 절대 금지합니다\n");
            message.append("• 산불 감시를 강화해주세요\n");
            message.append("• 산불 발견 시 즉시 119 또는 1688-3119 신고\n");
        } else if (risk.contains("보통")) {
            message.append("⚠️ 주의가 필요합니다.\n\n");
            message.append("✅ 권고 사항:\n");
            message.append("• 산행 시 화기 사용 금지\n");
            message.append("• 주변 상황을 잘 살펴주세요\n");
            message.append("• 등산로 이탈 금지\n");
            message.append("• 담배꽁초 투기 절대 금지\n");
        } else {
            message.append("✅ 현재 산불 위험도가 낮습니다.\n\n");
            message.append("💡 참고 사항:\n");
            message.append("• 하지만 항상 주의가 필요합니다\n");
            message.append("• 화기 사용은 삼가주세요\n");
            message.append("• 산불 예방 수칙을 준수해주세요\n");
        }

        return message.toString();
    }

    /**
     * 메시지에서 지역명 추출
     */
    private String extractRegionFromMessage(String message) {
        String[] regions = {
                "강원도", "경기도", "충청북도", "충청남도", "전라북도", "전라남도",
                "경상북도", "경상남도", "제주도"
        };

        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }

        // 축약형 처리
        if (message.contains("강원")) return "강원도";
        if (message.contains("경기")) return "경기도";
        if (message.contains("충북")) return "충청북도";
        if (message.contains("충남")) return "충청남도";
        if (message.contains("전북")) return "전라북도";
        if (message.contains("전남")) return "전라남도";
        if (message.contains("경북")) return "경상북도";
        if (message.contains("경남")) return "경상남도";
        if (message.contains("제주")) return "제주도";

        return null;
    }

    /**
     * 메시지에서 연도 추출
     */
    private String extractYearFromMessage(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(202[0-9]|203[0])");
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}