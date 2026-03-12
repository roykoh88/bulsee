package com.bulsee.dao;

import com.bulsee.vo.FireLandcoverMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FireLandcoverMatchDAO extends JpaRepository<FireLandcoverMatch, Long> {

    // 1. 정상 케이스 (서울 중구): 시도+시군구 다 맞고 + 연도 최신순(Desc)으로 1개
    Optional<FireLandcoverMatch> findFirstBySidoAndSigunguOrderByMatchedYearDesc(String sido, String sigungu);

    // 2. 세종시 비상 케이스: 시군구 무시하고 '시도'만 보고 + 연도 최신순(Desc)으로 1개
    // (세종시 전체 중 아무거나 하나 대표값으로 가져오거나, 세종시는 보통 데이터가 하나임)
    Optional<FireLandcoverMatch> findFirstBySidoOrderByMatchedYearDesc(String sido);
}