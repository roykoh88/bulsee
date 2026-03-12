package com.bulsee.dao;

import com.bulsee.vo.AwsStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwsStationDAO extends JpaRepository<AwsStation, Long> {

    /**
     * ✅ /api/regions 용: 시군구 폴리곤 매칭에 필요한 최소 데이터
     * 반환 타입 RegionRow는 "프로젝션(Projection)"이라서
     * SQL alias를 getter 이름(sigCd, name, lat, lng, stnId)에 맞추면 자동 매핑됨
     */
    @Query(value = """
SELECT *
FROM (
  SELECT
      k.SIG_CD       AS sigCd,
      k.FULL_KOR_NM  AS name,
      a.LAT          AS lat,
      a.LON          AS lng,
      a.STN_ID       AS stnId,
      ROW_NUMBER() OVER (PARTITION BY k.SIG_CD ORDER BY a.STN_ID) AS rn
  FROM KOR_SIG_MAP k
  LEFT JOIN DISTRICT_MAPPING dm
    ON dm.SIG_CD = k.SIG_CD
  LEFT JOIN AWS_STATION a
    ON a.NAME = dm.PARENT_NAME
)
WHERE rn = 1
ORDER BY sigCd
""", nativeQuery = true)
    List<RegionRow> findRegionsForMap();

}
