package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "WEATHER_AWS")
public class WeatherAws {

    // 일단 FIRE_ID를 PK로 잡았습니다.
    // (JPA는 @Id가 없으면 에러가 납니다)
    @Id
    @Column(name = "FIRE_ID")
    private Long fireId;

    @Column(name = "STN_ID")
    private Long stnId; // 관측소 ID

    @Column(name = "OBS_DT")
    private Long obsDt; // 관측 시간 (NUMBER 12)

    @Column(name = "TEMP")
    private Double temp; // 기온

    @Column(name = "WIND_SPEED")
    private Double windSpeed; // 풍속

    @Column(name = "WIND_DIR")
    private Double windDir; // 풍향

    @Column(name = "PRECIPITATION")
    private Double precipitation; // 강수량
}