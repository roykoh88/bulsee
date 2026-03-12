// src/lib/simulationTimeline.ts
import type { AreaTimeline } from "../types/simulation";

type HourlyPredictionsObj = Record<string, number>;

/**
 * 백엔드 PredictionLog.hourlyPredictions는 "JSON 문자열"로 내려온다.
 * 예) "{\"1h\":0.02,\"2h\":0.05,...}"
 *
 * - string이면 JSON.parse
 * - object면 그대로 사용
 * - 키는 "1h" 형태라 가정하되, 정규식으로 숫자만 뽑아서 정렬
 */
export function toAreaTimeline(hourly: unknown): AreaTimeline | null {
  if (hourly == null) return null;

  let obj: HourlyPredictionsObj | null = null;

  if (typeof hourly === "string") {
    const s = hourly.trim();
    if (!s || s === "{}") return null;
    try {
      const parsed = JSON.parse(s);
      if (parsed && typeof parsed === "object") obj = parsed as HourlyPredictionsObj;
    } catch {
      return null;
    }
  } else if (typeof hourly === "object") {
    obj = hourly as HourlyPredictionsObj;
  }

  if (!obj) return null;

  const points = Object.entries(obj)
    .map(([k, v]) => {
      const m = String(k).match(/(\d+)\s*h/i);
      if (!m) return null;
      const hour = Number(m[1]);
      const area = typeof v === "number" ? v : Number(v);
      if (!Number.isFinite(hour) || !Number.isFinite(area)) return null;
      return { tHour: hour, areaHa: Number(area.toFixed(3)) };
    })
    .filter(Boolean)
    .sort((a, b) => (a!.tHour - b!.tHour)) as Array<{ tHour: number; areaHa: number }>;

  if (points.length === 0) return null;

  return {
    baseTime: new Date().toISOString(),
    points,
  };
}
