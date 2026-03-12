package com.bulsee.dao;

import com.bulsee.vo.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * FAQ 데이터 접근 객체
 */
@Repository
public interface FAQDAO extends JpaRepository<FAQ, Long> {

    /**
     * 키워드로 FAQ 검색
     * 질문, 답변, 키워드 필드에서 검색
     */
    @Query("SELECT f FROM FAQ f WHERE " +
            "LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY f.viewCount DESC")
    List<FAQ> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 카테고리별 조회
     */
    List<FAQ> findByCategoryOrderByViewCountDesc(String category);

    /**
     * 인기 FAQ (조회수 높은 순)
     */
    @Query("SELECT f FROM FAQ f ORDER BY f.viewCount DESC")
    List<FAQ> findTopFAQs();
}