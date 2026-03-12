package com.bulsee.dto;

public record RegionResponse(
        String sigCd,
        String name,
        Long stnId,
        Double lat,
        Double lng
) {}

