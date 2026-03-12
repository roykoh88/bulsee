// src/components/wildfire/DetailPanel.tsx
import type { RegionData } from "../../types/wildfire";
import type { AreaTimeline } from "../../types/simulation";
import { AreaTimelineView } from "./AreaTimelineView";

import { X, Wind, Thermometer, Droplets, CloudRain, AlertTriangle, Map } from "lucide-react";

interface DetailPanelProps {
  region: RegionData;
  onClose: () => void;
  onSimulate: () => void;
  loading: boolean;

  // ✅ 시각화용 타임라인 (없으면 표시 안 함)
  timeline: AreaTimeline | null;
  selectedHour: number;
  onSelectHour: (h: number) => void;
  simulated: boolean;   // 추가
}



export function DetailPanel({
  region,
  onClose,
  onSimulate,
  loading,
  timeline,
  selectedHour,
  onSelectHour,
  simulated, 
}: DetailPanelProps) {

  const getRiskColor = (level?: string) => {
    if (level === "심각") return "text-red-600 bg-red-50 border-red-200";
    if (level === "위험") return "text-orange-600 bg-orange-50 border-orange-200";
    if (level === "경계") return "text-yellow-700 bg-yellow-50 border-yellow-200";
    return "text-gray-600 bg-gray-50 border-gray-200";
  };

 // const getRiskColor = (level?: string) => {
 //   if (level === "위험" || level === "Danger") return "text-red-600 bg-red-50 border-red-200";
 //   if (level === "경계" || level === "Warning") return "text-orange-600 bg-orange-50 border-orange-200";
 //   if (level === "주의" || level === "Caution") return "text-yellow-600 bg-yellow-50 border-yellow-200";
 ///   return "text-gray-600 bg-gray-50 border-gray-200";
 // };

  const riskStyle = getRiskColor(region.riskLevel);

   const hasWeather =
    region.temperature !== undefined ||
    region.humidity !== undefined ||
    region.windSpeed !== undefined ||
    region.precipitation !== undefined;

  const hasDamage =
    region.worstArea !== undefined ||
    region.typicalArea !== undefined ||
    (region.spreadDir != null && region.spreadDir !== "");

  const hintText = "정보없음 - 시뮬레이션을 눌러서 확인하세요.";

const baseTimeline: AreaTimeline = timeline ?? {
  baseTime: new Date().toISOString(), // ✅ 필수 필드 채움
  points: [
    { tHour: 1, areaHa: 0 },
    { tHour: 2, areaHa: 0 },
    { tHour: 3, areaHa: 0 },
    { tHour: 6, areaHa: 0 },
    { tHour: 12, areaHa: 0 },
    { tHour: 24, areaHa: 0 },
  ],
};

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h2 className="text-2xl font-black text-gray-900">{region.name}</h2>
        </div>
        <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full" type="button">
          <X size={24} className="text-gray-400" />
        </button>
      </div>

      {/* 분석 실행 */}
      <button
        onClick={onSimulate}
        disabled={loading}
        className={`w-full py-4 rounded-xl font-bold text-lg mb-6 shadow-md transition-all ${
          loading ? "bg-gray-300 text-gray-500 cursor-not-allowed" : "bg-blue-600 text-white hover:bg-blue-700"
        }`}
        type="button"
      >
        {loading ? "AI 분석 중..." : "⚡ 실시간 산불 위험도 분석"}
      </button>

      {/* 위험도 결과 */}
      <div className={`p-5 rounded-2xl border-2 mb-6 ${riskStyle}`}>
        <div className="flex items-center gap-2 mb-2">
          <AlertTriangle size={20} />
          <span className="font-bold text-sm uppercase">현재 위험 등급</span>
        </div>
        <div className="text-4xl font-black mb-1">{region.riskLevel}</div>
        
      </div>

<section className="mb-6">
  <h3 className="text-lg font-bold text-gray-800 mb-3">
    
  </h3>

  {timeline && (
<AreaTimelineView
  timeline={baseTimeline}
  selectedHour={selectedHour}
  onSelectHour={onSelectHour}
  simulated={simulated}
/>
  )}
</section>



  {/* ✅ 기상: 항상 보이게 */}
      <section className="mb-6">
        <h3 className="text-lg font-bold text-gray-800 mb-3 flex items-center gap-2">
          <CloudRain size={18} /> 실시간 기상
        </h3>

        {!hasWeather && (
          <div className="mb-3 text-sm text-gray-500 bg-gray-50 border border-gray-100 rounded-lg p-3">
            {hintText}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <div className="text-xs text-gray-500 mb-1 flex items-center gap-1">
              <Thermometer size={12} /> 기온
            </div>
            <div className="font-bold text-lg">
              {region.temperature !== undefined ? `${region.temperature}°C` : "—"}
            </div>
          </div>

          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <div className="text-xs text-gray-500 mb-1 flex items-center gap-1">
              <Droplets size={12} /> 습도
            </div>
            <div className="font-bold text-lg">
              {region.humidity !== undefined ? `${region.humidity}%` : "—"}
            </div>
          </div>

          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <div className="text-xs text-gray-500 mb-1 flex items-center gap-1">
              <Wind size={12} /> 풍속
            </div>
            <div className="font-bold text-lg">
              {region.windSpeed !== undefined ? `${region.windSpeed}m/s` : "—"}
            </div>
          </div>

          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <div className="text-xs text-gray-500 mb-1 flex items-center gap-1">
              <CloudRain size={12} /> 강수량
            </div>
            <div className="font-bold text-lg">
              {region.precipitation !== undefined ? `${region.precipitation}mm` : "—"}
            </div>
          </div>
        </div>
      </section>

      {/* ✅ 피해 규모: 항상 보이게 */}
      <section>
        <h3 className="text-lg font-bold text-gray-800 mb-3 flex items-center gap-2">
          <Map size={18} /> 예상 피해 분석
        </h3>

        {!hasDamage && (
          <div className="mb-3 text-sm text-gray-500 bg-gray-50 border border-gray-100 rounded-lg p-3">
            {hintText}
          </div>
        )}

        <div className="space-y-3">
          <div className="bg-red-50 p-4 rounded-xl border border-red-100">
            <p className="text-xs text-red-500 font-bold mb-1">최대 피해 예상 면적</p>
            <p className="text-2xl font-black text-red-700">
              {region.worstArea !== undefined ? `${region.worstArea} ha` : "—"}
            </p>
          </div>

          <div className="bg-blue-50 p-4 rounded-xl border border-blue-100">
            <p className="text-xs text-blue-500 font-bold mb-1">평균 피해 예상 면적</p>
            <p className="text-2xl font-black text-blue-700">
              {region.typicalArea !== undefined ? `${region.typicalArea} ha` : "—"}
            </p>
          </div>

          <div className="bg-gray-50 p-4 rounded-xl border border-gray-100">
            <p className="text-xs text-gray-500 font-bold mb-1">확산 방향</p>
            <p className="text-lg font-bold text-gray-800">{region.spreadDir ? region.spreadDir : "—"}</p>
          </div>
        </div>
      </section>
    </div>
  );
}
