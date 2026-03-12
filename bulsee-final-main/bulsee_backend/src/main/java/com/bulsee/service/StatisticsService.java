package com.bulsee.service;

import com.bulsee.repository.FireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final FireRepository fireRepository;

    /**
     * 년도별 통계
     */
    public String getStatisticsByYear(int year) {
        log.info("📊 {}년 전체 통계 조회", year);

        List<Object[]> results = fireRepository.getYearlyStatistics(year);

        if (results.isEmpty() || results.get(0)[0] == null) {
            return String.format("📊 %d년도 산불 발생 기록이 없습니다.", year);
        }

        Object[] row = results.get(0);
        long count = ((Number) row[0]).longValue();
        double totalArea = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double avgArea = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        return String.format(
                "📊 **%d년 산불 통계**\n\n" +
                        "🔥 발생 건수: **%,d건**\n" +
                        "🌲 총 피해 면적: **%.2fha**\n" +
                        "📏 평균 피해 면적: **%.2fha**\n\n" +
                        "특정 지역의 통계를 보시려면:\n" +
                        "• \"%d년 강원도 산불 통계\"\n" +
                        "• \"%d년 서울시 산불 통계\"",
                year, count, totalArea, avgArea, year, year
        );
    }

    /**
     * 년도 + 지역별 통계
     */
    public String getStatisticsByYearAndRegion(int year, String region, String displayRegion) {
        log.info("📊 통계 조회: {}년 {} 지역 (패턴: {})", year, displayRegion, region);

        log.warn("🔍 DEBUG - year: {}, region: '{}', displayRegion: '{}'", year, region, displayRegion);

        List<Object[]> sidoResults = fireRepository.getStatisticsByYearAndSido(year, region);
        List<Object[]> sigunguResults = fireRepository.getStatisticsByYearAndSigungu(year, region);

        log.warn("🔍 DEBUG - sidoResults.size(): {}", sidoResults.size());
        log.warn("🔍 DEBUG - sigunguResults.size(): {}", sigunguResults.size());

        // 🔥 수정: 0도 무효한 결과로 취급
        boolean sigunguHasData = !sigunguResults.isEmpty()
                && sigunguResults.get(0)[0] != null
                && ((Number) sigunguResults.get(0)[0]).longValue() > 0;

        List<Object[]> results = sigunguHasData ? sigunguResults : sidoResults;

        if (results.isEmpty() || results.get(0)[0] == null) {
            return String.format(
                    "📊 %d년 %s 지역의 산불 발생 기록이 없습니다.\n\n" +
                            "다른 지역이나 년도를 검색해보세요!",
                    year, displayRegion
            );
        }

        Object[] row = results.get(0);
        long count = ((Number) row[0]).longValue();

        // 🔥 추가 안전장치: count가 0이면 데이터 없음 처리
        if (count == 0) {
            return String.format(
                    "📊 %d년 %s 지역의 산불 발생 기록이 없습니다.\n\n" +
                            "다른 지역이나 년도를 검색해보세요!",
                    year, displayRegion
            );
        }

        double totalArea = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double avgArea = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        long springCount = row.length > 3 && row[3] != null ? ((Number) row[3]).longValue() : 0;

        return String.format(
                "📊 **%d년 %s 산불 통계**\n\n" +
                        "🔥 발생 건수: **%,d건**\n" +
                        "🌲 총 피해 면적: **%.2fha**\n" +
                        "📏 평균 피해 면적: **%.2fha**\n" +
                        "🌸 봄철 발생: **%d건** (%.1f%%)\n\n" +
                        "💡 %s은(는) %d년에 총 %,d건의 산불이 발생했습니다.",
                year, displayRegion, count, totalArea, avgArea,
                springCount, (springCount * 100.0 / count),
                displayRegion, year, count
        );
    }

    /**
     * 지역별 통계 (전체 년도)
     */
    public String getStatisticsByRegion(String region, String displayRegion) {
        log.info("📊 {} 전체 통계 조회 (패턴: {})", displayRegion, region);

        log.warn("🔍 DEBUG - region: '{}', displayRegion: '{}'", region, displayRegion);

        List<Object[]> sidoResults = fireRepository.getStatisticsBySido(region);
        List<Object[]> sigunguResults = fireRepository.getStatisticsBySigungu(region);

        log.warn("🔍 DEBUG - sidoResults.size(): {}", sidoResults.size());
        log.warn("🔍 DEBUG - sigunguResults.size(): {}", sigunguResults.size());

        if (!sidoResults.isEmpty()) {
            log.warn("🔍 DEBUG - sidoResults[0]: {}", java.util.Arrays.toString(sidoResults.get(0)));
        }
        if (!sigunguResults.isEmpty()) {
            log.warn("🔍 DEBUG - sigunguResults[0]: {}", java.util.Arrays.toString(sigunguResults.get(0)));
        }

        // 🔥 수정: 0도 무효한 결과로 취급
        boolean sigunguHasData = !sigunguResults.isEmpty()
                && sigunguResults.get(0)[0] != null
                && ((Number) sigunguResults.get(0)[0]).longValue() > 0;

        List<Object[]> results = sigunguHasData ? sigunguResults : sidoResults;

        log.warn("🔍 DEBUG - 최종 선택된 results.size(): {}", results.size());
        if (!results.isEmpty()) {
            log.warn("🔍 DEBUG - 최종 results[0]: {}", java.util.Arrays.toString(results.get(0)));
        }

        if (results.isEmpty() || results.get(0)[0] == null) {
            return String.format("📊 %s 지역의 산불 발생 기록이 없습니다.", displayRegion);
        }

        Object[] row = results.get(0);
        long count = ((Number) row[0]).longValue();

        // 🔥 추가 안전장치: count가 0이면 데이터 없음 처리
        if (count == 0) {
            return String.format("📊 %s 지역의 산불 발생 기록이 없습니다.", displayRegion);
        }

        double totalArea = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;

        return String.format(
                "📊 **%s 전체 산불 통계**\n\n" +
                        "🔥 총 발생 건수: **%,d건**\n" +
                        "🌲 총 피해 면적: **%.2fha**\n\n" +
                        "특정 년도의 통계를 보시려면:\n" +
                        "• \"2024년 %s 산불 통계\"",
                displayRegion, count, totalArea, displayRegion
        );
    }
}