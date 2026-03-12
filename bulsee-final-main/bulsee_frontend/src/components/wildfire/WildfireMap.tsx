// src/components/wildfire/WildfireMap.tsx
import { MapContainer, TileLayer, useMap, GeoJSON, Polygon, Polyline } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import React, { useEffect, useMemo, useRef, useState } from "react";
import type { RegionData, RiskLevel } from "../../types/wildfire";
import type { AreaTimeline } from "../../types/simulation";

interface WildfireMapProps {
  regions: RegionData[];
  selectedRegion: RegionData | null;
  onRegionSelect: (region: RegionData) => void;

  timeline: AreaTimeline | null;
  selectedHour: number;
  simulated: boolean;

  // 추가
  defaultCenter: [number, number];
  defaultZoom: number;
  mapResetKey: number;
}

function contourColor(idx: number, total: number) {
  const t = (idx - 1) / Math.max(1, total - 1); // 0(안쪽) ~ 1(바깥)

  // 빨강(239,68,68) → 노랑(253,224,71)
  const r = Math.round(239 + (253 - 239) * t);
  const g = Math.round(68 + (224 - 68) * t);
  const b = Math.round(68 + (71 - 68) * t);

  // ✅ 안쪽은 진하게(0.95), 바깥은 옅게(0.35)
  const a = 0.95 - 0.60 * t;

  return `rgba(${r}, ${g}, ${b}, ${a.toFixed(2)})`;
}

// 지도 시점 이동 도우미 (시뮬레이션 실행 시 zoom을 더 키움)
function MapUpdater({
  center,
  simulated,
  defaultCenter,
  defaultZoom,
  mapResetKey,
}: {
  center: [number, number] | null;
  simulated: boolean;
  defaultCenter: [number, number];
  defaultZoom: number;
  mapResetKey: number;
}) {
  const map = useMap();

  useEffect(() => {
    // ✅ 선택된 지역이 있으면 그쪽으로
    if (center) {
      map.flyTo(center, simulated ? 12.5 : 10, { animate: true, duration: 0.8 });
      return;
    }

    // ✅ 선택이 없으면(=디테일 패널 닫힘) 전국으로 복귀
    map.flyTo(defaultCenter, defaultZoom, { animate: true, duration: 0.8 });
  }, [center, simulated, defaultCenter, defaultZoom, mapResetKey, map]);

  return null;
}

function normalizeSigCd(v: any) {
  const s = String(v ?? "").trim();

  // 숫자만 추출
  const digits = s.replace(/[^\d]/g, "");
  if (!digits) return "";

  // ✅ 시군구(Si/Gun/Gu) 폴리곤 키는 "5자리"가 기준
  // 예: "50130.0" -> "501300" 이 되더라도 앞 5자리만 취한다.
  if (digits.length >= 5) return digits.slice(0, 5);

  // 4자리면 0 패딩
  if (digits.length === 4) return digits.padStart(5, "0");

  // 그 외는 그대로(방어)
  return digits;
}

/**
 * ✅ 시각화 반경(m) (과장/최대 제한 조절 가능)
 * - ha를 로그로 부스팅해서 작은 면적도 보이게
 */
function visualRadiusMeters(areaHa: number) {
  const ha = Math.max(0, areaHa);

  // ✅ 로그 부스팅 (값을 줄이면 덜 과장됨)
  const boost = Math.log10(1 + ha * 40);

  // ✅ “조금만 작게” 튜닝된 값
  const MIN_R = 650;   // 최소 반경
  const MAX_R = 8500;  // 최대 반경
  const SCALE = 6500;  // 성장폭

  const r = MIN_R + boost * SCALE;
  return Math.min(MAX_R, Math.max(MIN_R, r));
}

// 확산 방향 텍스트 -> 각도
function dirToBearing(dir?: string | null) {
  if (!dir) return null;
  const d = dir.trim();

  const map: Record<string, number> = {
    북: 0,
    북동: 45,
    동: 90,
    남동: 135,
    남: 180,
    남서: 225,
    서: 270,
    북서: 315,
  };
  return map[d] ?? null;
}

// 위경도에서 bearing 방향으로 dist(m) 이동
function movePoint(lat: number, lng: number, bearingDeg: number, distM: number): [number, number] {
  const R = 6378137;
  const brng = (bearingDeg * Math.PI) / 180;
  const d = distM / R;

  const lat1 = (lat * Math.PI) / 180;
  const lon1 = (lng * Math.PI) / 180;

  const lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) + Math.cos(lat1) * Math.sin(d) * Math.cos(brng));
  const lon2 =
    lon1 +
    Math.atan2(
      Math.sin(brng) * Math.sin(d) * Math.cos(lat1),
      Math.cos(d) - Math.sin(lat1) * Math.sin(lat2)
    );

  return [(lat2 * 180) / Math.PI, (lon2 * 180) / Math.PI];
}

// 회전된 타원 폴리곤 생성
function buildEllipsePolygon(
  centerLat: number,
  centerLng: number,
  a: number,
  b: number,
  bearingDeg: number,
  steps = 72
): [number, number][] {
  const pts: [number, number][] = [];
  for (let i = 0; i < steps; i++) {
    const theta = (i / steps) * 2 * Math.PI;
    const cos = Math.cos(theta);
    const sin = Math.sin(theta);

    // 타원 극좌표 반경 공식
    const r = (a * b) / Math.sqrt((b * cos) ** 2 + (a * sin) ** 2);

    const bearing = bearingDeg + (theta * 180) / Math.PI;
    pts.push(movePoint(centerLat, centerLng, bearing, r));
  }
  return pts;
}

export function WildfireMap({
  regions,
  selectedRegion,
  onRegionSelect,
  timeline,
  selectedHour,
  simulated,
  defaultCenter,
  defaultZoom,
  mapResetKey,
}: WildfireMapProps) {
  const [geoJsonData, setGeoJsonData] = useState<any>(null);

  // 1) GeoJSON 로드
  useEffect(() => {
    const url = `${import.meta.env.BASE_URL}sig.json`;
    fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error("GeoJSON 파일을 찾을 수 없습니다.");
        return res.json();
      })
      .then((data) => setGeoJsonData(data))
      .catch((err) => console.error("GeoJSON 로드 실패:", err));
  }, []);

  // sigCd로 빠르게 조회할 수 있게 Map 구성
  const regionBySigCd = useMemo(() => {
    const map = new Map<string, RegionData>();
    for (const r of regions) {
      const key = normalizeSigCd(r.sigCd);
      if (!key) continue;
      map.set(key, r); // 항상 최신 r로 overwrite
    }
    return map;
  }, [regions]);

  // ✅ 데이터 참조용 Ref (기존 코드)
  const regionBySigCdRef = useRef(regionBySigCd);
  useEffect(() => {
    regionBySigCdRef.current = regionBySigCd;
  }, [regionBySigCd]);

  // ✅ [중요 수정] 핸들러 참조용 Ref 추가
  // 부모에서 전달받은 onRegionSelect가 변경되어도
  // GeoJSON click 이벤트 내부에서 항상 "최신 버전"의 함수를 실행하기 위함
  const onRegionSelectRef = useRef(onRegionSelect);
  useEffect(() => {
    onRegionSelectRef.current = onRegionSelect;
  }, [onRegionSelect]);

  const findRegionData = (featureProperties: any) => {
    const sigCd = normalizeSigCd(featureProperties?.SIG_CD);
    if (!sigCd) return null;

    const match = regionBySigCd.get(sigCd) ?? null;
    if (!match) {
      // console.warn(...) // 필요시 주석 해제
    }
    return match;
  };

  const getRiskColor = (level?: RiskLevel) => {
    switch (level) {
      case "심각":
        return "#ef4444"; // red
      case "위험":
        return "#f97316"; // orange
      case "경계":
        return "#eab308"; // yellow
      case "정보없음":
      default:
        return "#cccccc";
    }
  };

  // ✅ 선택 시간의 면적(ha)
  const currentAreaHa = useMemo(() => {
    if (!selectedRegion) return 0;

    if (timeline && timeline.points?.length) {
      const found = timeline.points.find((p) => p.tHour === selectedHour);
      return found?.areaHa ?? timeline.points[0]?.areaHa ?? 0;
    }

    return 0;
  }, [selectedRegion, timeline, selectedHour]);

  const baseR = useMemo(() => visualRadiusMeters(currentAreaHa), [currentAreaHa]);

  const bearingDeg = useMemo(() => dirToBearing(selectedRegion?.spreadDir ?? null), [selectedRegion]);

  /**
   * ✅ 등고선/컨투어 링: 여러 겹 타원(Polygon) 생성
   */
  const contours = useMemo(() => {
    if (!timeline || !timeline.points?.length) return [];
    if (!selectedRegion) return [];
    if (currentAreaHa <= 0) return [];
    if (bearingDeg === null) return [];

    const LEVELS = 6; // 4~7 추천
    const result: { ring: [number, number][], idx: number }[] = [];

    for (let i = 1; i <= LEVELS; i++) {
      const t = i / LEVELS;
      const eased = Math.pow(t, 1.35); // 초반 촘촘, 후반 넓게

      const r = baseR * eased;

      // ✅ 타원 축
      const a = r * 2.6;
      const b = r * 1.0;

      const ring = buildEllipsePolygon(selectedRegion.lat, selectedRegion.lng, a, b, bearingDeg, 72);
      result.push({ ring, idx: i });
    }

    return result;
  }, [timeline, selectedRegion, currentAreaHa, baseR, bearingDeg]);

  const style = (feature: any) => {
    const data = findRegionData(feature.properties);

    const riskColor = data ? getRiskColor(data.riskLevel) : "#cccccc";
    const isSelected = !!(selectedRegion && data && selectedRegion.sigCd === data.sigCd);

    // ✅ "전체 분포 모드" (디테일 패널이 꺼져있음)
    const overviewMode = !selectedRegion;

    let color = "rgba(255,255,255,0.65)";
    let weight = 1;
    let opacity = 0.9;
    let fillColor = riskColor;

    let fillOpacity = overviewMode
      ? (data?.riskLevel && data.riskLevel !== "정보없음" ? 0.22 : 0.10)
      : (data?.riskLevel && data.riskLevel !== "정보없음" ? 0.10 : 0.05);

    // ✅ 선택 지역 강조
    if (isSelected) {
      fillOpacity = 0.28;
      color = "rgba(0,0,0,0.85)";
      weight = 3;
      opacity = 1.0;
    }

    // ✅ 시뮬 상태
    if (simulated && !overviewMode) {
      fillOpacity = isSelected ? 0.14 : 0.03;
      if (isSelected) {
        color = riskColor;
        weight = 4;
        opacity = 1.0;
      }
    }

    return { fillColor, fillOpacity, color, weight, opacity };
  };

  // 폴리곤 이벤트/툴팁
  const onEachFeature = (feature: any, layer: any) => {
    const sigCd = normalizeSigCd(feature.properties?.SIG_CD);

    const data = findRegionData(feature.properties);
    if (data) {
      layer.bindTooltip(`<div style="text-align:center; font-weight:bold;">${data.name}</div>`, {
        sticky: true,
      });
    }

    layer.on({
      click: () => {
        const latest = sigCd ? regionBySigCdRef.current.get(sigCd) : null;
        // ✅ [중요 수정] Ref를 통해 최신 onRegionSelect 함수를 호출함
        if (latest) {
          onRegionSelectRef.current(latest);
        }
      },
    });
  };

  return (
    <MapContainer center={[36.5, 127.8]} zoom={7} className="h-full w-full" zoomControl={false}>
      <TileLayer
        url="https://xdworld.vworld.kr/2d/Base/service/{z}/{x}/{y}.png"
        attribution='&copy; <a href="http://www.vworld.kr/" target="_blank">V-WORLD</a>'
        minZoom={6}
        maxZoom={19}
      />

      {geoJsonData && <GeoJSON data={geoJsonData} style={style} onEachFeature={onEachFeature} />}

      {/* ✅ 등고선(컨투어) */}
      {contours.map(({ ring, idx }) => {
        const total = contours.length;
        const t = (idx - 1) / Math.max(1, total - 1);
        let c = contourColor(idx, total);

        if (idx === 1) {
          c = "rgba(185, 28, 28, 0.98)";
        }

        const fillOpacity = idx === 1 ? 0.45 : 0.28 - 0.22 * t;
        const strokeWeight = idx === 1 ? 4.5 : 3;
        const dashArray = idx === 1 ? undefined : "5 9";

        return (
          <React.Fragment key={`contour-${idx}`}>
            <Polygon
              positions={ring}
              pathOptions={{
                color: "rgba(0,0,0,0)",
                weight: 0,
                fillColor: c,
                fillOpacity,
              }}
            />
            <Polyline
              positions={ring}
              pathOptions={{
                color: c,
                weight: strokeWeight,
                dashArray,
                opacity: 1.0,
              }}
            />
          </React.Fragment>
        );
      })}

      <MapUpdater
        center={selectedRegion ? [selectedRegion.lat, selectedRegion.lng] : null}
        simulated={simulated}
        defaultCenter={defaultCenter}
        defaultZoom={defaultZoom}
        mapResetKey={mapResetKey}
      />
    </MapContainer>
  );
}