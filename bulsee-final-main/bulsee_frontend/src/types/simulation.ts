// src/types/simulation.ts

export interface AreaPoint {
  tHour: number;   // 예: 1,2,3,6,12,24
  areaHa: number;  // ha
}

export interface AreaTimeline {
  baseTime: string;
  points: AreaPoint[];
}
