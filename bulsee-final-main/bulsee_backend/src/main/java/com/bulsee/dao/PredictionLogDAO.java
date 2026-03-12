package com.bulsee.dao;


import com.bulsee.vo.PredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictionLogDAO extends JpaRepository<PredictionLog, Long> {
    // 필요한 쿼리가 있다면 여기에 메소드 추가 (지금은 기본 저장만 하면 돼서 비워도 됨)
}