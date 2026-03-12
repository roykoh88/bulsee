package com.bulsee.controller;

import com.bulsee.dto.RegionResponse;
import com.bulsee.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // ✅ GET /api/regions 호출 시 여기로 들어옴
    @GetMapping
    public List<RegionResponse> getRegionList() {
        // 1) Service 호출
        return regionService.getRegionsForMap();
    }
}
