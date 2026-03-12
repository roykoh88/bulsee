package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "PREDICTION_LOG")
public class PredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PRED_LOG_GEN")
    @SequenceGenerator(name = "PRED_LOG_GEN", sequenceName = "PREDICTION_LOG_SEQ", allocationSize = 1)
    private Long id;

    private String stnId;
    private Double lat;
    private Double lon;

    private Double temp;
    private Double windSpeed;
    private Double humidity;
    private Double precipitation;

    private String riskLevel;
    private Double riskScore;
    @Column(name = "TYPICAL_AREA")
    private Double typicalArea;

    @Column(name = "WORST_AREA")
    private Double worstArea;

    @Column(name = "HOURLY_PREDICTIONS", length = 4000)
    private String hourlyPredictions;

    @Column(name = "SPREAD_DIR")
    private String spreadDir;

    private LocalDateTime createdAt = LocalDateTime.now();
}