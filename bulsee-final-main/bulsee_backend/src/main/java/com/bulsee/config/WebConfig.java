package com.bulsee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 주소로 들어오는 요청에 대해
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // 허용할 프론트엔드 주소 (React는 보통 3000)
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 행동
                .allowCredentials(true); // 인증 정보 허용
    }
}