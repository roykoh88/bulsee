package com.bulsee.service;

import com.bulsee.vo.PredictRequest;
import com.bulsee.vo.PredictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Slf4j
@Component
public class FastAPIClient {

    @Value("${python.api.url}")
    private String pythonApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * FastAPI 예측 호출 (POST 방식)
     */
    public PredictResponse predict(PredictRequest request) {
        try {
            String url = pythonApiUrl + "/predict";

            log.info("🤖 FastAPI 예측 요청: {}", url);
            log.info("📍 요청 데이터: lat={}, lon={}, forestArea={}",
                    request.getLat(), request.getLon(), request.getForestArea());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PredictRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PredictResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PredictResponse.class
            );

            PredictResponse result = response.getBody();

            if (result != null) {
                log.info("✅ FastAPI 응답: risk={}, score={}, typical_area={}",
                        result.getRisk(), result.getScore(), result.getTypicalAreaHa());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ FastAPI 호출 실패: {}", e.getMessage(), e);
            return PredictResponse.builder()
                    .error("ML 서버 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * FastAPI 서버 상태 확인
     */
    public boolean isHealthy() {
        try {
            String url = pythonApiUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("❌ FastAPI 서버 연결 실패: {}", e.getMessage());
            return false;
        }
    }
}