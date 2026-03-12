package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "FOREST_AGE_DISTRIBUTION")
public class ForestAgeDistribution {

    @Id
    @Column(name = "REGION_ID")
    private Long regionId;

    private String sido;    // 시도
    private String sigungu; // 시군구

    @Column(name = "FOREST_AREA")
    private Double forestArea; // 산림면적(ha)

    @Column(name = "CONIFER_RATIO")
    private Double coniferRatio; // 침엽수림 면적 비율

    @Column(name = "BROADLEAF_RATIO")
    private Double broadleafRatio; // 활엽수림 면적 비율

    @Column(name = "MIXED_RATIO")
    private Double mixedRatio; // 혼효림 면적 비율

    @Column(name = "TIMBER_STOCK_SUM")
    private Double timberStockSum; // 임목축적 합계(㎥)

    @Column(name = "TIMBER_STOCK_MEAN")
    private Double timberStockMean; // 임목축적 평균(㎥/ha)

    @Column(name = "FOREST_AREA_FROM_AGE")
    private Double forestAreaFromAge; // 영급별 산림면적

    @Column(name = "AGE1_RATIO")
    private Double age1Ratio; // 1영급 비율

    @Column(name = "AGE2_RATIO")
    private Double age2Ratio; // 2영급 비율

    @Column(name = "AGE3_RATIO")
    private Double age3Ratio; // 3영급 비율

    @Column(name = "AGE4_RATIO")
    private Double age4Ratio; // 4영급 비율

    @Column(name = "AGE5_RATIO")
    private Double age5Ratio; // 5영급 비율

    @Column(name = "AGE6_RATIO")
    private Double age6Ratio; // 6영급 비율

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}