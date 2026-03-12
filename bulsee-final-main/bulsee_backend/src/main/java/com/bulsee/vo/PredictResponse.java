package com.bulsee.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictResponse {

    @JsonProperty("stn_id")
    private String stnId;

    private String risk;

    private Double score;

    @JsonProperty("typical_area_ha")
    private Double typicalAreaHa;

    @JsonProperty("worst_area_ha")
    private Double worstAreaHa;

    private String direction;

    @JsonProperty("hourly_predictions")
    private Map<String, Double> hourlyPredictions;

    @JsonProperty("model_name")
    private String modelName;

    private WeatherData weather;

    @JsonProperty("data_status")
    private String dataStatus;

    private String notice;

    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherData {
        private Double temp;

        @JsonProperty("windSpeed")
        private Double windSpeed;

        @JsonProperty("windDir")
        private Double windDir;

        private Double precipitation;

        private Double humidity;
    }
}