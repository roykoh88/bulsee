package com.bulsee.repository;

import com.bulsee.vo.Fire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FireRepository extends JpaRepository<Fire, Long> {

    /**
     * 년도별 통계 (지역 무관)
     */
    @Query(value = "SELECT COUNT(*), SUM(GRS_FRSTFR_DAM_AREA), AVG(GRS_FRSTFR_DAM_AREA) " +
            "FROM FIRE " +
            "WHERE TO_NUMBER(SUBSTR(TO_CHAR(FIRE_DT), 1, 4)) = :year " +
            "AND GRS_FRSTFR_DAM_AREA IS NOT NULL",
            nativeQuery = true)
    List<Object[]> getYearlyStatistics(@Param("year") int year);

    /**
     * 년도 + 지역별 통계 (시군구)
     */
    @Query(value = "SELECT COUNT(*), SUM(f.GRS_FRSTFR_DAM_AREA), AVG(f.GRS_FRSTFR_DAM_AREA), " +
            "COUNT(CASE WHEN TO_NUMBER(SUBSTR(TO_CHAR(f.FIRE_DT), 5, 2)) BETWEEN 3 AND 5 THEN 1 END) " +
            "FROM FIRE f " +
            "JOIN FIRE_ADDR fa ON f.FIRE_ID = fa.FIRE_ID " +
            "WHERE TO_NUMBER(SUBSTR(TO_CHAR(f.FIRE_DT), 1, 4)) = :year " +
            "AND fa.SIGUNGU LIKE :region " +
            "AND f.GRS_FRSTFR_DAM_AREA IS NOT NULL",
            nativeQuery = true)
    List<Object[]> getStatisticsByYearAndSigungu(@Param("year") int year, @Param("region") String region);

    /**
     * 년도 + 지역별 통계 (시도)
     */
    @Query(value = "SELECT COUNT(*), SUM(f.GRS_FRSTFR_DAM_AREA), AVG(f.GRS_FRSTFR_DAM_AREA), " +
            "COUNT(CASE WHEN TO_NUMBER(SUBSTR(TO_CHAR(f.FIRE_DT), 5, 2)) BETWEEN 3 AND 5 THEN 1 END) " +
            "FROM FIRE f " +
            "JOIN FIRE_ADDR fa ON f.FIRE_ID = fa.FIRE_ID " +
            "WHERE TO_NUMBER(SUBSTR(TO_CHAR(f.FIRE_DT), 1, 4)) = :year " +
            "AND fa.SIDO LIKE :region " +
            "AND f.GRS_FRSTFR_DAM_AREA IS NOT NULL",
            nativeQuery = true)
    List<Object[]> getStatisticsByYearAndSido(@Param("year") int year, @Param("region") String region);

    /**
     * 지역별 통계 - 시군구 (전체 년도)
     */
    @Query(value = "SELECT COUNT(*), SUM(f.GRS_FRSTFR_DAM_AREA), AVG(f.GRS_FRSTFR_DAM_AREA) " +
            "FROM FIRE f " +
            "JOIN FIRE_ADDR fa ON f.FIRE_ID = fa.FIRE_ID " +
            "WHERE fa.SIGUNGU LIKE :region " +
            "AND f.GRS_FRSTFR_DAM_AREA IS NOT NULL",
            nativeQuery = true)
    List<Object[]> getStatisticsBySigungu(@Param("region") String region);

    /**
     * 지역별 통계 - 시도 (전체 년도)
     */
    @Query(value = "SELECT COUNT(*), SUM(f.GRS_FRSTFR_DAM_AREA), AVG(f.GRS_FRSTFR_DAM_AREA) " +
            "FROM FIRE f " +
            "JOIN FIRE_ADDR fa ON f.FIRE_ID = fa.FIRE_ID " +
            "WHERE fa.SIDO LIKE :region " +
            "AND f.GRS_FRSTFR_DAM_AREA IS NOT NULL",
            nativeQuery = true)
    List<Object[]> getStatisticsBySido(@Param("region") String region);
}