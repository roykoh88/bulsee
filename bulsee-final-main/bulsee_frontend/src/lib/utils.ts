// src/lib/utils.ts
import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { RiskLevel } from "../types/wildfire";

// Tailwind 클래스 병합 함수
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 1. 위험 등급별 배경색 (뱃지/마커용)
export const getRiskColor = (level: RiskLevel | string) => {
  switch (level) {
    case "위험": return "bg-red-600 text-white border-red-700";
    case "Danger": return "bg-red-600 text-white border-red-700";
    
    case "경계": return "bg-orange-500 text-white border-orange-600";
    case "Warning": return "bg-orange-500 text-white border-orange-600";
    
    case "주의": return "bg-yellow-400 text-black border-yellow-500";
    case "Caution": return "bg-yellow-400 text-black border-yellow-500";
    
    default: return "bg-gray-100 text-gray-500 border-gray-200"; // 정보없음
  }
};

// 2. 위험 등급별 텍스트 색상 (강조용)
export const getRiskTextColor = (level: RiskLevel | string) => {
  switch (level) {
    case "위험": return "text-red-600";
    case "Danger": return "text-red-600";
    
    case "경계": return "text-orange-600";
    case "Warning": return "text-orange-600";
    
    case "주의": return "text-yellow-600";
    case "Caution": return "text-yellow-600";
    
    default: return "text-gray-400";
  }
};