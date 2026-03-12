package com.bulsee.dto;

import com.bulsee.vo.PredictResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegionDataDTO {
    // 기본 위치 정보 (AwsStation에서 가져옴)
    private String sigCd;
    private String name;
    private Long stnId;
    private Double lat;
    private Double lng;

    // 분석 결과 (PredictResponse에서 가져옴)
    private String riskLevel;
    private Double fireProbability; // score
    private Double typicalArea;
    private Double worstArea;
    private String spreadDir;

    // 기상 정보
    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private Double precipitation;
}