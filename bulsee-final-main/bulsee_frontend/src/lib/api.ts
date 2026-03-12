import type { RegionData } from "@/types/wildfire";

// 상황에 맞게 유지
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

const toNumber = (v: any, fallback = Number.NaN) => {
  const n = typeof v === "number" ? v : Number(v);
  return Number.isFinite(n) ? n : fallback;
};

const isValidLatLng = (lat: number, lng: number) => {
  // 한국만 쓸 거면 범위를 더 좁혀도 됨(예: lat 33~39, lng 124~132)
  return Number.isFinite(lat) && Number.isFinite(lng)
    && lat >= -90 && lat <= 90
    && lng >= -180 && lng <= 180;
};

const normalizeSigCd = (v: any) => {
  const s = String(v ?? "").trim();
  // 숫자/공백/소수점 등 섞여도 숫자만 추출
  const digits = s.replace(/[^\d]/g, "");
  // 보통 시군구 코드는 5자리. 4자리면 앞에 0 패딩(예외 방어)
  if (digits.length === 4) return digits.padStart(5, "0");
  return digits;
};


const toString = (v: any, fallback = "") => {
  return typeof v === "string" ? v : (v == null ? fallback : String(v));
};

/**
 * 1) 시군구(Region) 목록 가져오기
 * - 핵심: sigCd가 반드시 있어야 함 (폴리곤 매칭 키)
 */
export const fetchRegions = async (): Promise<RegionData[]> => {
  const response = await fetch(`${API_BASE_URL}/regions`);

  if (!response.ok) {
    // ✅ MOCK로 숨기지 말고, 원인을 노출하는 게 디버깅에 유리
    const text = await response.text().catch(() => "");
    throw new Error(`regions API 실패: ${response.status} ${text}`);
  }

  const data = await response.json();

  // ✅ 백엔드 응답이 배열이 아닐 때 방어
  if (!Array.isArray(data)) {
    throw new Error("regions API 응답이 배열이 아닙니다.");
  }

  // ✅ 응답 매핑 (백엔드 계약: sigCd/name/stnId/lat/lng)
  const mapped: RegionData[] = data
    .map((item: any) => {
      const sigCd = normalizeSigCd(item.sigCd);
      const name = toString(item.name);
const lat = toNumber(item.lat);
const lng = toNumber(item.lng);

if (!sigCd || !name) return null;
if (!isValidLatLng(lat, lng)) {
  console.warn("❌ 잘못된 좌표로 region 제외:", { sigCd, name, lat, lng, raw: item });
  return null;
}

return {
  sigCd,
  name,
  stnId: toNumber(item.stnId, 0), // stnId는 0 fallback 허용 가능
  lat,
  lng,
  riskLevel: "정보없음",
} as RegionData;
    })
    .filter(Boolean) as RegionData[];

  return mapped;
};

/**
 * 2) 시뮬레이션 실행 (AI 예측)
 * - 권장: name 대신 sigCd + stnId 기반으로 요청 (백엔드도 안정)
 */
export const runSimulation = async (region: RegionData) => {
  const payload = {
    // ✅ 백엔드가 실제로 사용하는 값
    name: region.name,     // ← 추가 (중요)
    lat: region.lat,
    lng: region.lng,

    // ✅ 있으면 백엔드에서 활용 가능 (로그/확장)
    sigCd: region.sigCd,
    stnId: region.stnId,
  };

  const response = await fetch(`${API_BASE_URL}/analysis/simulate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`simulate 실패: ${response.status} ${text}`);
  }

  return await response.json();
};
