package com.bulsee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정
 * 
 * WebClient:
 * Spring의 비동기/논블로킹 HTTP 클라이언트
 * FastAPI ML 서버와 통신하는 데 사용
 * 
 * 사용 예:
 * webClient.post()
 *   .uri("http://localhost:8000/predict")
 *   .bodyValue(request)
 *   .retrieve()
 *   .bodyToMono(Response.class)
 *   .block();
 */
@Configuration
public class WebClientConfig {
    
    /**
     * WebClient.Builder Bean 등록
     * 
     * @return WebClient.Builder (재사용 가능)
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            // FastAPI 서버 기본 URL
            .baseUrl("http://ai:8000")
            // 기본 헤더 설정
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json");
    }
}
