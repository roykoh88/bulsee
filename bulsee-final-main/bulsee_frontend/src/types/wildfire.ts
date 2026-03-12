// src/types/wildfire.ts

export type RiskLevel = "심각" | "위험" | "경계" | "정보없음";

export interface RegionData {
  sigCd: string;
  name: string;
  stnId: number;
  lat: number;
  lng: number;

  location?: string;
  mapping?: string;

  // 분석 결과(선택)
  riskLevel: RiskLevel;
  fireProbability?: number;

  typicalArea?: number;
  worstArea?: number;
  spreadDir?: string;

  temperature?: number;
  humidity?: number;
  windSpeed?: number;
  precipitation?: number;

  forestAge?: string;
  fuelModel?: string;
}
