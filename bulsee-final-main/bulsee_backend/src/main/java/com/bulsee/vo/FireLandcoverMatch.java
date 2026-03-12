package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "FIRE_LANDCOVER_MATCH")
public class FireLandcoverMatch {

    @Id
    @Column(name = "FIRE_ID")
    private Long fireId;      // 산불 발생 식별자

    @Column(name = "MATCHED_YEAR")
    private Integer matchedYear; // 매칭된 연도

    private String sido;      // 시도명
    private String sigungu;   // 시군구명

    @Column(name = "RADIUS_M")
    private Double radiusM;   // 반경

    @Column(name = "FOREST_RATIO")
    private Double forestRatio; // 산림 지역 비율

    @Column(name = "URBAN_RATIO")
    private Double urbanRatio;  // 도심 지역 비율

    @Column(name = "AGRI_RATIO")
    private Double agriRatio;   // 농경지 비율

    @Column(name = "GRASS_RATIO")
    private Double grassRatio;  // 초지 비율

    @Column(name = "MATCH_METHOD")
    private String matchMethod; // 매칭 방식

    @Column(name = "CREATED_DT")
    private LocalDateTime createdDt;
}