package com.bulsee.dao;

import com.bulsee.vo.WeatherAws;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherAwsDAO extends JpaRepository<WeatherAws, Long> {
}