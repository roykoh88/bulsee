// src/components/wildfire/DashboardRegionCard.tsx
import { RegionData } from "../../types/wildfire";
import { ChevronRight } from "lucide-react";
import { getRiskColor, getRiskTextColor } from "../../lib/utils";

interface DashboardRegionCardProps {
  region: RegionData;
  onClick: () => void;
}

export function DashboardRegionCard({ region, onClick }: DashboardRegionCardProps) {
  // ✅ riskLevel이 없을 수도 있다고 TS가 판단할 때 대비
  const level = region.riskLevel ?? "정보없음";

  const badgeClass = getRiskColor(level);
  const barClass = badgeClass.split(" ")[0]; // "bg-..."만 뽑기

  return (
    <div
      onClick={onClick}
      className="group flex items-center justify-between p-4 bg-white hover:bg-blue-50 rounded-xl transition-all cursor-pointer border border-gray-100 hover:border-blue-200 shadow-sm mb-3"
    >
      <div className="flex items-center gap-4">
        {/* 상태 표시 바 (좌측 색상 띠) */}
        <div className={`w-1.5 h-12 rounded-full ${barClass}`}></div>

        <div>
          <h3 className="font-bold text-gray-900 text-lg">{region.name}</h3>
          <p className="text-xs text-gray-500 mt-0.5 flex items-center gap-1">
            <span className="text-gray-300">|</span>
            SIG: {region.sigCd}
            <span className="text-gray-300">|</span>
            STN: {region.stnId}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <div className="text-right">
          <span className={`inline-block px-2.5 py-1 rounded-md text-xs font-bold ${badgeClass}`}>
            {level}
          </span>

          {region.fireProbability !== undefined && (
            <p className={`text-xs mt-1 font-semibold ${getRiskTextColor(level)}`}>
              위험도 {region.fireProbability.toFixed(1)}%
            </p>
          )}
        </div>

        <ChevronRight size={20} className="text-gray-300 group-hover:text-blue-500 transition-colors" />
      </div>
    </div>
  );
}
