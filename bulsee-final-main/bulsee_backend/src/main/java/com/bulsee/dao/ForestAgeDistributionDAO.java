package com.bulsee.dao;

import com.bulsee.vo.ForestAgeDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // 이 import가 꼭 필요합니다!

@Repository
public interface ForestAgeDistributionDAO extends JpaRepository<ForestAgeDistribution, Long> {

    // 1. 시도 + 시군구로 조회 (서울, 경기 등 일반 지역용)
    // "SELECT * FROM ... WHERE sido = ? AND sigungu = ?" 와 같습니다.
    Optional<ForestAgeDistribution> findBySidoAndSigungu(String sido, String sigungu);

    // 2. 시도만으로 조회 (세종특별자치시용)
    // "SELECT * FROM ... WHERE sido = ? LIMIT 1" 와 같습니다.
    Optional<ForestAgeDistribution> findFirstBySido(String sido);
}