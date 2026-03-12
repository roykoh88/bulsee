package com.bulsee.repository;

import com.bulsee.vo.FireAddr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireAddrRepository extends JpaRepository<FireAddr, Long> {
}