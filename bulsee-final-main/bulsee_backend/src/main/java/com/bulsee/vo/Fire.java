package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "FIRE")
public class Fire {

    @Id
    @Column(name = "FIRE_ID")
    private Long fireId; // 산불 ID

    // NUMBER(12,0) -> 202601211200 같은 숫자 형식이므로 Long 사용
    @Column(name = "FIRE_DT")
    private Long fireDt; // 발화 시간

    @Column(name = "EXTNGS_END_DT")
    private Long extngsEndDt; // 진화 완료 시간

    @Column(name = "LON")
    private Double lon; // 경도 (NUMBER 18,12 -> 정밀한 실수)

    @Column(name = "LAT")
    private Double lat; // 위도

    @Column(name = "STN_ID")
    private Long stnId; // 가까운 관측소 ID

    @Column(name = "GRS_FRSTFR_DAM_AREA")
    private Double damageArea; // 피해 면적

    @Column(name = "NEAR_DIST_KM")
    private Double nearDistKm; // 관측소와의 거리
}