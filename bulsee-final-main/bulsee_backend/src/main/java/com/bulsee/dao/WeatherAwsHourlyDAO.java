package com.bulsee.dao;

import com.bulsee.vo.WeatherAwsHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherAwsHourlyDAO extends JpaRepository<WeatherAwsHourly, Long> {

    // 특정 관측소(stnId)의 데이터를 시간 역순(최신순)으로 24개만 가져오기
    // Top24: 상위 24개
    // OrderByObsDtDesc: 최근 시간부터 과거 순으로 정렬
    List<WeatherAwsHourly> findTop24ByStnIdOrderByObsDtDesc(Long stnId);
}