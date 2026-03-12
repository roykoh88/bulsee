package com.bulsee.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictRequest {
    private Double lat;
    private Double lon;

    // 지형 데이터
    private Double forestArea;
    private Double coniferRatio;
    private Double broadleafRatio;
    private Double mixedRatio;
    private Double timberStockSum;
    private Double timberStockMean;
    private Double forestAreaFromAge;
    private Double age1Ratio;
    private Double age2Ratio;
    private Double age3Ratio;
    private Double age4Ratio;
    private Double age5Ratio;
    private Double age6Ratio;
    private Double forestRatio;
    private Double urbanRatio;
    private Double agriRatio;
    private Double grassRatio;
}