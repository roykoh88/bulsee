package com.bulsee.repository;

import com.bulsee.vo.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {

    // 키워드로 검색
    @Query("SELECT f FROM FAQ f WHERE LOWER(f.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FAQ> findByKeywordContaining(@Param("keyword") String keyword);

    // 카테고리별 검색
    List<FAQ> findByCategory(String category);

    // 인기 FAQ (조회수 높은 순)
    List<FAQ> findTop10ByOrderByViewCountDesc();
}