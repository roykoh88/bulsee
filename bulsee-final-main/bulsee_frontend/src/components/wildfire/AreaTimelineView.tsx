// src/components/wildfire/AreaTimelineView.tsx
import { useMemo } from "react";
import type { AreaTimeline } from "../../types/simulation";

type Props = {
  timeline: AreaTimeline;
  selectedHour: number;
  onSelectHour: (h: number) => void;
  simulated: boolean; // ✅ 시뮬 실행 여부
};

export function AreaTimelineView({ timeline, selectedHour, onSelectHour, simulated }: Props) {
  const points = timeline.points ?? [];

  const current = useMemo(() => {
    if (!points.length) return undefined;
    return points.find((p) => p.tHour === selectedHour) ?? points[0];
  }, [points, selectedHour]);

  const maxArea = useMemo(() => {
    if (!points.length) return undefined;
    return Math.max(...points.map((p) => p.areaHa), 0.0001);
  }, [points]);

  // ✅ 시뮬 전이면 값 표시 금지(정보없음 처리)
  const displayArea = simulated && current ? current.areaHa : undefined;
  const displayMax = simulated && typeof maxArea === "number" ? maxArea : undefined;

  const pct = useMemo(() => {
    if (!simulated) return undefined;
    if (displayArea == null || displayMax == null) return undefined;
    const v = displayArea;
    return Math.min(100, Math.max(0, (v / displayMax) * 100));
  }, [simulated, displayArea, displayMax]);

  return (
    <div className="mt-4 rounded-xl border bg-white p-4">
      {/* ✅ 시뮬 전 안내문 */}
      {!simulated && (
        <div className="mb-3 text-sm text-gray-500 bg-gray-50 border border-gray-100 rounded-lg p-3">
        시뮬레이션을 눌러서 확인하세요.
        </div>
      )}

      {/* 큰 숫자 영역 */}
      <div className="flex items-end justify-between">
        <div>
          <div className="text-sm text-gray-500">예상 피해 면적</div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black">
              {displayArea !== undefined ? displayArea.toFixed(2) : ""}
            </span>
            <span className="text-lg font-bold text-gray-600">ha (헥타르)</span>
          </div>
        </div>

        <div className="text-sm text-gray-500 text-right">
          기준: {selectedHour}시간 후<br />
          최대: {displayMax !== undefined ? `${displayMax.toFixed(2)} ha` : ""}
        </div>
      </div>

      {/* 진행바 */}
      <div className="mt-3">
        <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
          <div
            className="h-full rounded-full bg-red-500 transition-all"
            style={{ width: `${pct !== undefined ? pct : 0}%` }}
          />
        </div>
        <div className="mt-2 text-xs text-gray-400">
          진행률: {pct !== undefined ? `${pct.toFixed(0)}%` : ""}
        </div>
      </div>

      {/* 시간 버튼(항상 표시) */}
      <div className="mt-4 flex flex-wrap gap-2">
        {points.map((p) => (
          <button
            key={p.tHour}
            type="button"
            onClick={() => onSelectHour(p.tHour)}
            className={[
              "px-3 py-1.5 rounded-full text-sm border transition-colors",
              p.tHour === selectedHour ? "bg-black text-white" : "bg-white hover:bg-gray-50",
            ].join(" ")}
          >
            {p.tHour}h
          </button>
        ))}
      </div>

      <div className="mt-3 flex flex-col gap-1 text-xs text-gray-400">
        <p>* 1ha(헥타르)는 약 3,000평(10,000m²) 크기입니다.</p>
        <p>* 지도의 피해면적을 타임라인에 따라 시각화한 것으로 실제와 차이가 있을 수 있습니다.</p>
      </div>
    </div>
  );
}
