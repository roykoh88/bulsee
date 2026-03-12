package com.bulsee.service;

import com.bulsee.dao.AwsStationDAO;
import com.bulsee.dto.RegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final AwsStationDAO awsStationDAO;

    public List<RegionResponse> getRegionsForMap() {

        // 2) DAO 조회 호출: 여기서 DB 쿼리 실행됨
        return awsStationDAO.findRegionsForMap()
                .stream()
                // 3) DAO 결과(RegionRow)를 프론트에 내려줄 DTO로 매핑
                .map(r -> new RegionResponse(
                        r.getSigCd(),
                        r.getName(),
                        r.getStnId(),
                        r.getLat(),
                        r.getLng()
                ))
                .toList();
    }
}
