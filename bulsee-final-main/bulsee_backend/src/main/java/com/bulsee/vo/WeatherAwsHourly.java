package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Data
@Table(name = "WEATHER_AWS_HOURLY")
public class WeatherAwsHourly {

    @Id
    @Column(name = "WEATHER_ID")
    private Long weatherId;      // PK

    @Column(name = "FIRE_ID")
    private Long fireId;         // 산불 ID (학습용, 실시간 예측시엔 null일 수 있음)

    @Column(name = "STN_ID")
    private Long stnId;          // 관측소 ID

    @Column(name = "OBS_DT")
    private Long obsDt;          // 관측 시각 (YYYYMMDDHH00 형식)

    @Column(name = "WS")
    private Double ws;           // 풍속 (Wind Speed)

    @Column(name = "WD")
    private Double wd;           // 풍향 (Wind Direction)

    @Column(name = "HM")
    private Double hm;           // 습도 (Humidity)

    @Column(name = "RN_HR1")
    private Double rnHr1;        // 1시간 강수량

    @Column(name = "RN_DAY")
    private Double rnDay;        // 일 누적 강수량

    @Column(name = "TA")
    private Double ta;           // 기온 (Temperature Air)

    @Column(name = "REG_DT", insertable = false, updatable = false)
    private Date regDt;          // 적재 시각
}