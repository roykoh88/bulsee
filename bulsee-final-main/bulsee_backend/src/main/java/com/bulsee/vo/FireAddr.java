package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "FIRE_ADDR")
public class FireAddr {

    @Id
    @Column(name = "FIRE_ID")
    private Long fireId;

    @Column(name = "SIDO")
    private String sido; // 시도 (예: 부산, 충남, 경기)

    @Column(name = "SIGUNGU")
    private String sigungu; // 시군구 (예: 사상구, 공주시, 화성시)

    @Column(name = "EMD")
    private String emd; // 읍면동

    @Column(name = "RI")
    private String ri; // 리

    @Column(name = "ADDR_STD")
    private String addrStd; // 표준화된 주소

    @Column(name = "EMD_NORM")
    private String emdNorm; // 정규화된 읍면동

    @Column(name = "EMD_CENTER_LON")
    private Double emdCenterLon;

    @Column(name = "EMD_CENTER_LAT")
    private Double emdCenterLat;

    @Column(name = "GEO_STATUS")
    private String geoStatus;
}