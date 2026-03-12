// src/pages/Index.tsx
import { useState, useEffect } from "react";
import { WildfireMap } from "../components/wildfire/WildfireMap";
import { DetailPanel } from "../components/wildfire/DetailPanel";
import { DashboardPanel } from "../components/wildfire/DashboardPanel";
import { fetchRegions, runSimulation } from "../lib/api";
import type { RegionData, RiskLevel } from "../types/wildfire";
import { SearchOverlay } from "../components/wildfire/SearchOverlay";
import { ChatBot } from "../components/wildfire/ChatBot";
import type { AreaTimeline } from "../types/simulation";
import { toAreaTimeline } from "../lib/simulationTimeline";

// ✅ 백엔드 PredictionLog 응답 형태(필요한 필드만)
type PredictionLogResponse = {
  riskLevel?: string;
  riskScore?: number;

  temp?: number;
  windSpeed?: number;
  humidity?: number;
  precipitation?: number;

  typicalArea?: number;
  worstArea?: number;
  spreadDir?: string;

  // ⭐ 핵심: String(JSON) 로 내려옴
  hourlyPredictions?: string;

  // 기타: stnId, lat, lon, createdAt 등
};



const normalizeRiskLevel = (v?: string): RiskLevel => {
  const s = (v ?? "").trim();

  // 구버전 -> 3단계 압축
  if (s === "위험" || s === "DANGER" || s === "HIGH") return "심각";
  if (s === "경계" || s === "WARNING" || s === "MEDIUM") return "위험";
  if (s === "주의" || s === "보통" || s === "LOW" || s === "NORMAL") return "경계";

  // 이미 3단계로 오는 경우
  if (s === "심각" || s === "위험" || s === "경계") return s;

  return "정보없음";
};



export default function Index() {
  const [mapResetKey, setMapResetKey] = useState(0); // ✅ 여기로 이동
  const [regions, setRegions] = useState<RegionData[]>([]);
  const [selectedRegion, setSelectedRegion] = useState<RegionData | null>(null);
  const [loading, setLoading] = useState(false);

  // ✅ 시각화용 타임라인 상태
  const [areaTimeline, setAreaTimeline] = useState<AreaTimeline | null>(null);
  const [selectedHour, setSelectedHour] = useState<number>(1);

  // ✅ "시뮬레이션 실행됨" 상태 (지도 추가 확대용)
  const [simulated, setSimulated] = useState(false);

  // 1) 초기 데이터 로드
  useEffect(() => {
    const loadData = async () => {
      try {
        const data = await fetchRegions();
        setRegions(data);
      } catch (error) {
        console.error(error);
      }
    };
    loadData();
  }, []);


const selectRegionWithCache = (r: RegionData) => {
  // ✅ 지도에서 넘어온 r이 오래된 객체일 수 있으니,
  // regions state에서 같은 sigCd의 "최신 객체"를 다시 가져온다.
  const latest =
    regions.find((x) => x.sigCd === r.sigCd) ?? r;

  setSelectedRegion(latest);

  const hasCache =
    latest.riskLevel !== "정보없음" ||
    latest.fireProbability !== undefined ||
    latest.worstArea !== undefined ||
    latest.typicalArea !== undefined ||
    latest.temperature !== undefined ||
    latest.humidity !== undefined ||
    latest.windSpeed !== undefined ||
    latest.precipitation !== undefined;

  setSimulated(hasCache);

  setSelectedHour(1);
};

  // 2) 시뮬레이션 실행
  const handleSimulate = async () => {
    if (!selectedRegion) return;
    setLoading(true);

    try {
      const result = (await runSimulation(selectedRegion)) as PredictionLogResponse;

      // ✅ 백엔드 hourlyPredictions(JSON 문자열) → AreaTimeline
      const timeline = toAreaTimeline(result.hourlyPredictions);
      setAreaTimeline(timeline);
      setSelectedHour(timeline?.points?.[0]?.tHour ?? 1);

      const updatedRegion: RegionData = {
        ...selectedRegion,

        // ✅ RiskLevel 타입에 맞게 정규화
        riskLevel: normalizeRiskLevel(result.riskLevel),
        fireProbability: typeof result.riskScore === "number" ? result.riskScore : undefined,

        temperature: typeof result.temp === "number" ? result.temp : undefined,
        humidity: typeof result.humidity === "number" ? result.humidity : undefined,
        windSpeed: typeof result.windSpeed === "number" ? result.windSpeed : undefined,
        precipitation: typeof result.precipitation === "number" ? result.precipitation : undefined,

        typicalArea: typeof result.typicalArea === "number" ? result.typicalArea : undefined,
        worstArea: typeof result.worstArea === "number" ? result.worstArea : undefined,
        spreadDir: result.spreadDir ?? undefined,
      };

      // ✅ 시뮬레이션 실행 플래그 ON → 지도에서 한 번 더 확대
      setSimulated(true);

      setSelectedRegion(updatedRegion);
      setRegions((prev) =>
        prev.map((r) => (r.sigCd === updatedRegion.sigCd ? updatedRegion : r))
      );
    } catch (e) {
      alert("분석 중 오류가 발생했습니다. 백엔드 서버가 켜져 있는지 확인해주세요.");
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  // ✅ [누락된 부분 추가] 챗봇 전용 선택 핸들러
  const handleChatbotSelect = (data: RegionData) => {
    console.log("🤖 챗봇이 선택한 지역:", data);
    
    // 새로 만드신 캐시 확인 로직을 재사용합니다.
    selectRegionWithCache(data); 

    // (선택 사항) 만약 챗봇이 선택해준 곳은 
    // 무조건 시뮬레이션 결과가 있다고 가정하고 지도를 확대하려면 아래 줄 주석 해제
    // setSimulated(true); 
  };
  return (
    <div className="relative w-full h-screen overflow-hidden bg-gray-50">
      {/* 1) 검색창 */}
      <SearchOverlay
        regions={regions}
        onSelectRegion={(r) => selectRegionWithCache(r)}
      />

      {/* 2) 지도 */}
      <div className="absolute inset-0 z-0 pr-[400px]">
     <WildfireMap
        regions={regions}
        selectedRegion={selectedRegion}
        onRegionSelect={(r) => selectRegionWithCache(r)}
        timeline={areaTimeline}
        selectedHour={selectedHour}
        simulated={simulated}

        defaultCenter={[36.5, 127.8]}
        defaultZoom={7}
        mapResetKey={mapResetKey}
      />
      </div>

      {/* 3) 우측 패널 */}
      <div className="absolute top-0 right-0 h-full w-[400px] bg-white shadow-xl z-10 overflow-hidden flex flex-col">
        {selectedRegion ? (
          <div className="flex-1 overflow-y-auto custom-scrollbar">
            <DetailPanel
              region={selectedRegion}
              onClose={() => {
                setSelectedRegion(null);
                setAreaTimeline(null);
                setSelectedHour(1);
                setSimulated(false);

                setMapResetKey((k) => k + 1); // ✅ 전국으로 복귀 트리거
              }}
              onSimulate={handleSimulate}
              loading={loading}
              timeline={areaTimeline}
              selectedHour={selectedHour}
              onSelectHour={setSelectedHour}
              simulated={simulated}   //  추가
            />
          </div>
        ) : (
          <DashboardPanel
            regions={regions}
            onRegionClick={(r) => selectRegionWithCache(r)}
          />
        )}
         </div>
         {/* ✅ 3. 챗봇 배치 (onSelectRegion 연결) */}
      <ChatBot onSelectRegion={handleChatbotSelect} />
    </div>
  );
}
