package com.bulsee.controller;

import com.bulsee.service.AnalysisService;
import com.bulsee.vo.PredictionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // ✅ 시도명 표준화: 특별자치도 → 기존 도 명칭
    private String normalizeSido(String sido) {
        if (sido == null) return "";
        sido = sido.trim();

        // 앞부분만 정확히 치환
        if (sido.startsWith("강원특별자치도")) return "강원도";
        if (sido.startsWith("전북특별자치도")) return "전라북도";

        // 필요시 추가 가능
        // if (sido.startsWith("제주특별자치도")) return "제주도";

        return sido;
    }
        // 예측 실행 API
    // URL: http://localhost:8080/api/analysis/simulate
    @PostMapping("/simulate")
    public ResponseEntity<?> simulateWildfire(

            @RequestBody Map<String, Object> requestData // 프론트에서 준 데이터
    ) {
        try {
            Double lat = Double.parseDouble(requestData.get("lat").toString());
            Double lon = Double.parseDouble(requestData.get("lng").toString());

            // ✅ name은 필수로 받되, 혹시 name이 없을 때를 대비해 방어(권장)
            String fullName = (String) requestData.get("name");

            // ✅ stnId도 받아서 저장/추적 가능하게
            String stnId = requestData.get("stnId") == null ? null : requestData.get("stnId").toString();

            String sido = "";
            String sigungu = "";

            // 이름 쪼개기 로직
            if (fullName != null) {
                String[] parts = fullName.split(" ");
                if (parts.length >= 2) {
                    sido = parts[0];
                    sigungu = parts[1];
                } else {
                    sido = fullName;
                    sigungu = fullName;
                }
            } else {
                // name이 없으면 명확히 에러 처리 (또는 sigCd로 찾는 로직 추가 가능)
                return ResponseEntity.badRequest().body("name(예: '강원도 춘천시') 필드가 필요합니다.");
            }

            // ✅ 여기! 서비스 호출 전에 sido를 표준화
            String normalizedSido = normalizeSido(sido);

            // 서비스 호출
            PredictionLog result = analysisService.runSimulation(stnId, normalizedSido, sigungu, lat, lon); // ✅
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("에러: " + e.getMessage());
        }
    }
}