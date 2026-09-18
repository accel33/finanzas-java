package com.accel.finanzas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaTasas(
        String result,
        @JsonProperty("base_code") String base,
        @JsonProperty("time_last_update_utc") String actualizado,
        Map<String, BigDecimal> rates) {}
