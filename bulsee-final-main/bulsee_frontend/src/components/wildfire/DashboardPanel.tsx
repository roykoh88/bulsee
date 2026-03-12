// src/components/wildfire/DashboardPanel.tsx
import { useMemo, useState } from "react";
import type { RegionData, RiskLevel } from "../../types/wildfire";
import { DashboardRegionCard } from "./DashboardRegionCard";
import { Filter, AlertTriangle, ShieldAlert } from "lucide-react";

interface DashboardPanelProps {
  regions: RegionData[];
  onRegionClick: (region: RegionData) => void;
}

export function DashboardPanel({ regions, onRegionClick }: DashboardPanelProps) {
  const [filter, setFilter] = useState<RiskLevel | "전체">("전체");

  const DEFAULT_VISIBLE_COUNT = 4; // ✅ 기본으로 보여줄 개수(3~4 중 4로 설정)
  const [showAll, setShowAll] = useState(false); // ✅ 리스트 아래 + 버튼 토글 상태

  // ✅ sigCd 기반 중복 제거
  const uniqueRegions = useMemo(() => {
    const seen = new Set<string>();
    const out: RegionData[] = [];
    for (const r of regions) {
      if (!r.sigCd) continue;
      if (seen.has(r.sigCd)) continue;
      seen.add(r.sigCd);
      out.push(r);
    }
    return out;
  }, [regions]);

  // 필터링
  const filteredList = uniqueRegions.filter((r) => {
    if (filter === "전체") return true;
    return r.riskLevel === filter;
  });

  // 위험도 높은 순 정렬
  const sortedList = [...filteredList].sort((a, b) => {
    const scoreA = a.fireProbability ?? -1;
    const scoreB = b.fireProbability ?? -1;
    return scoreB - scoreA;
  });

  // ✅ 기본 4개만 표시, + 버튼 누르면 전체 표시
  const visibleList = showAll ? sortedList : sortedList.slice(0, DEFAULT_VISIBLE_COUNT);

  // 통계
  const countRisk = uniqueRegions.filter((r) => r.riskLevel === "심각").length;
  const countWarning = uniqueRegions.filter((r) => r.riskLevel === "위험").length;

  return (
    <div className="h-full flex flex-col bg-gray-50/50">
      <div className="p-5 bg-white border-b border-gray-200">
        <h2 className="text-xl font-black text-gray-900 mb-4">실시간 감시 현황</h2>

        <div className="flex gap-2">
          <div className="flex-1 bg-red-50 p-3 rounded-xl border border-red-100 flex items-center justify-between shadow-sm">
            <div className="flex flex-col">
              <span className="text-xs font-bold text-red-500 mb-1 flex items-center gap-1">
                <AlertTriangle size={12} /> 심각 지역
              </span>
              <span className="text-2xl font-black text-red-600">{countRisk}</span>
            </div>
          </div>

          <div className="flex-1 bg-orange-50 p-3 rounded-xl border border-orange-100 flex items-center justify-between shadow-sm">
            <div className="flex flex-col">
              <span className="text-xs font-bold text-orange-500 mb-1 flex items-center gap-1">
                <ShieldAlert size={12} /> 위험 지역
              </span>
              <span className="text-2xl font-black text-orange-600">{countWarning}</span>
            </div>
          </div>
        </div>
      </div>

      {/* 필터 */}
      <div className="flex p-3 gap-2 overflow-x-auto border-b border-gray-200 bg-white sticky top-0 z-10 no-scrollbar">
        {(["전체", "심각", "위험", "경계"] as const).map((level) => (
          <button
            key={level}
            onClick={() => {
              setFilter(level);
              setShowAll(false); // ✅ 필터 바꾸면 다시 접힌 상태로(UX 개선)
            }}
            className={`px-4 py-1.5 rounded-full text-sm font-bold whitespace-nowrap transition-all shadow-sm ${
              filter === level
                ? "bg-gray-800 text-white ring-2 ring-gray-800 ring-offset-1"
                : "bg-white text-gray-500 border border-gray-200 hover:bg-gray-50"
            }`}
          >
            {level}
          </button>
        ))}
      </div>

      {/* 리스트 */}
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {sortedList.length > 0 ? (
          <>
            {visibleList.map((region) => (
              <DashboardRegionCard
                key={region.sigCd}
                region={region}
                onClick={() => onRegionClick(region)}
              />
            ))}

            {/* ✅ 리스트 아래 더보기/접기 버튼 */}
            {sortedList.length > DEFAULT_VISIBLE_COUNT && (
              <div className="flex justify-center pt-2">
                <button
                  type="button"
                  onClick={() => setShowAll((v) => !v)}
                  className="w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center shadow-sm"
                  aria-label={showAll ? "지역 목록 접기" : "지역 목록 더보기"}
                  title={showAll ? "접기" : "더보기"}
                >
                  <span className="text-xl leading-none">{showAll ? "−" : "+"}</span>
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="h-64 flex flex-col items-center justify-center text-gray-400">
            <Filter size={48} className="mb-4 opacity-20" />
            <p className="font-medium">해당 조건의 지역이 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}
