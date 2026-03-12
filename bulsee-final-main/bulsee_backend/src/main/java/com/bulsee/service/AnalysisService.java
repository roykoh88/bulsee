package com.bulsee.service;

import com.bulsee.dao.FireLandcoverMatchDAO;
import com.bulsee.dao.ForestAgeDistributionDAO;
import com.bulsee.dao.PredictionLogDAO;
import com.bulsee.vo.FireLandcoverMatch;
import com.bulsee.vo.ForestAgeDistribution;
import com.bulsee.vo.PredictionLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final PredictionLogDAO predictionLogDAO;
    private final ForestAgeDistributionDAO forestAgeDistributionDAO;
    private final FireLandcoverMatchDAO fireLandcoverMatchDAO;
    private final WebClient.Builder webClientBuilder;

    public PredictionLog runSimulation(String stnId, String sido, String sigungu, double lat, double lon) {

        // 1. 임상도 데이터 조회
        ForestAgeDistribution forestData;

        if (sido.contains("세종특별")) {
            forestData = forestAgeDistributionDAO.findFirstBySido(sido)
                    .orElseThrow(() -> new RuntimeException("세종시 산림 데이터가 없습니다."));
        } else {
            forestData = forestAgeDistributionDAO.findBySidoAndSigungu(sido, sigungu)
                    .orElseThrow(() -> new RuntimeException("산림 데이터 없음: " + sido + " " + sigungu));
        }

        // 2. 랜드커버 데이터 조회
        FireLandcoverMatch landcoverData;

        if (sido.contains("세종특별")) {
            landcoverData = fireLandcoverMatchDAO.findFirstBySidoOrderByMatchedYearDesc(sido)
                    .orElse(new FireLandcoverMatch());
        } else {
            landcoverData = fireLandcoverMatchDAO.findFirstBySidoAndSigunguOrderByMatchedYearDesc(sido, sigungu)
                    .orElse(new FireLandcoverMatch());
        }

        // 3. 파이썬 전송 데이터 구성
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("lat", lat);
        requestMap.put("lon", lon);

        // Forest Data
        requestMap.put("forestArea", forestData.getForestArea());
        requestMap.put("coniferRatio", forestData.getConiferRatio());
        requestMap.put("broadleafRatio", forestData.getBroadleafRatio());
        requestMap.put("mixedRatio", forestData.getMixedRatio());
        requestMap.put("timberStockSum", forestData.getTimberStockSum());
        requestMap.put("timberStockMean", forestData.getTimberStockMean());
        requestMap.put("forestAreaFromAge", forestData.getForestAreaFromAge());
        requestMap.put("age1Ratio", forestData.getAge1Ratio());
        requestMap.put("age2Ratio", forestData.getAge2Ratio());
        requestMap.put("age3Ratio", forestData.getAge3Ratio());
        requestMap.put("age4Ratio", forestData.getAge4Ratio());
        requestMap.put("age5Ratio", forestData.getAge5Ratio());
        requestMap.put("age6Ratio", forestData.getAge6Ratio());

        // Landcover Data
        requestMap.put("forestRatio", landcoverData.getForestRatio());
        requestMap.put("urbanRatio", landcoverData.getUrbanRatio());
        requestMap.put("agriRatio", landcoverData.getAgriRatio());
        requestMap.put("grassRatio", landcoverData.getGrassRatio());

        // 4. WebClient를 사용한 Python 호출

        AnalysisService.PythonResponseDto response = webClientBuilder.build()
                .post()
                .uri("/predict") // base + /predict
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(AnalysisService.PythonResponseDto.class)
                .block(); // 기존처럼 동기 방식으로 결과를 기다림

        if (response == null) {
            throw new RuntimeException("파이썬 서버 응답 없음");
        }

        // DB 저장
        PredictionLog log = new PredictionLog();
        log.setStnId(stnId);
        log.setLat(lat);
        log.setLon(lon);

        if (response != null) {
            // 1. 위험 등급 및 점수 세팅
            log.setRiskLevel(response.getRisk());
            log.setRiskScore(response.getScore());

            // 2. 면적 및 방향 세팅
            log.setTypicalArea(response.getTypicalArea());
            log.setWorstArea(response.getWorstArea());
            log.setSpreadDir(response.getDirection());

            // 3. 시계열 데이터 JSON 변환 및 저장 (핵심 추가 로직)
            try {
                ObjectMapper mapper = new ObjectMapper();
                // Map 객체를 문자열 "{ '1h': 0.02 ... }" 로 변환
                if (response.getHourlyPredictions() != null) {
                    String jsonHourly = mapper.writeValueAsString(response.getHourlyPredictions());
                    log.setHourlyPredictions(jsonHourly);
                } else {
                    log.setHourlyPredictions("{}");
                }
            } catch (Exception e) {
                log.setHourlyPredictions("{}"); // 에러 시 빈 JSON 저장
                System.out.println("JSON 변환 실패: " + e.getMessage());
            }

            // 4. 날씨 세팅
            if (response.getWeather() != null) {
                log.setTemp(response.getWeather().getTemp());
                log.setWindSpeed(response.getWeather().getWindSpeed());
                log.setPrecipitation(response.getWeather().getPrecipitation());
                log.setHumidity(response.getWeather().getHumidity());
            }
        }

        return predictionLogDAO.save(log);
    }

    // DTO 클래스
    @lombok.Data
    public static class PythonResponseDto {

        @com.fasterxml.jackson.annotation.JsonProperty("stn_id")
        private String stnId;

        private String risk;
        private Double score;
        private String direction;

        @com.fasterxml.jackson.annotation.JsonProperty("typical_area_ha")
        private Double typicalArea;

        @com.fasterxml.jackson.annotation.JsonProperty("worst_area_ha")
        private Double worstArea;

        @com.fasterxml.jackson.annotation.JsonProperty("hourly_predictions")
        private Map<String, Double> hourlyPredictions;

        private WeatherDto weather;

        @lombok.Data
        public static class WeatherDto {
            private Double temp;
            private Double windSpeed;
            private Double windDir;
            private Double precipitation;
            private Double humidity;
        }
    }
}