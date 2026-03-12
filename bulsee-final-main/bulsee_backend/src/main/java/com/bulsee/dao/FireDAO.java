package com.bulsee.dao;

import com.bulsee.vo.Fire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireDAO extends JpaRepository<Fire, Long> {
}