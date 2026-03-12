import asyncio
from fastapi import APIRouter
from pydantic import BaseModel
from service import model_service, weather_service, weather_24h_service

router = APIRouter()

# Java CamelCase 입력 대응
class PredictRequest(BaseModel):
    lat: float
    lon: float
    forestArea: float
    coniferRatio: float
    broadleafRatio: float
    mixedRatio: float
    timberStockSum: float
    timberStockMean: float
    forestAreaFromAge: float
    age1Ratio: float
    age2Ratio: float
    age3Ratio: float
    age4Ratio: float
    age5Ratio: float
    age6Ratio: float
    forestRatio: float
    urbanRatio: float
    agriRatio: float
    grassRatio: float

@router.post("/predict")
async def predict_risk(data: PredictRequest):
    try:
        # 1. 날씨 데이터 수집 (병렬 처리)
        # 실시간 날씨
        realtime_task = asyncio.to_thread(weather_service.get_weather, data.lat, data.lon)
        # 24시간 과거 날씨
        history_task = weather_24h_service.get_weather_24h_by_coords(data.lat, data.lon)
        
        current_weather, history_raw = await asyncio.gather(realtime_task, history_task)
        
        # 2. 통계 및 상태 추출 (stnId, notice 확보용)
        stats = weather_24h_service.get_history_stats(history_raw, current_weather)
        
        # 3. 지형 데이터 매핑 (CamelCase -> SnakeCase)
        terrain_data = {
            'forest_area': data.forestArea,
            'conifer_ratio': data.coniferRatio,
            'broadleaf_ratio': data.broadleafRatio,
            'mixed_ratio': data.mixedRatio,
            'timber_stock_sum': data.timberStockSum,
            'timber_stock_mean': data.timberStockMean,
            'age1_ratio': data.age1Ratio,
            'age2_ratio': data.age2Ratio,
            'age3_ratio': data.age3Ratio,
            'age4_ratio': data.age4Ratio,
            'age5_ratio': data.age5Ratio,
            'age6_ratio': data.age6Ratio,
            'forest_ratio': data.forestRatio,
            'urban_ratio': data.urbanRatio,
            'agri_ratio': data.agriRatio,
            'grass_ratio': data.grassRatio
        }

        # 4. AI 모델 입력용 날씨 데이터 매핑
        # weather_service의 키(temp, windSpeed...)를 모델 학습용 키(ta, ws...)로 변환
        current_weather_mapped = {
            "ta": current_weather.get("temp", 0),
            "ws": current_weather.get("windSpeed", 0),
            "wd": current_weather.get("windDir", 0),
            "hm": current_weather.get("humidity", 0),
            "rn_hr1": current_weather.get("precipitation", 0)
        }
        
        # 5. AI 모델 예측 실행 (Raw History 전달)
        prediction = model_service.predict_fire_risk(history_raw, current_weather_mapped, terrain_data)
        
        print(f"📊 [AI 예측 결과] {prediction}")

        # 6. 결과 조립 (요청하신 포맷 준수)
        # 데이터 상태 결정
        

        gate_score = prediction.get("gatekeeper_score", 0) # 0.0 ~ 1.0 (피해규모 기반 점수)
       
        normal_ha = prediction.get("small_case_ha", 0.0)  # Normal
        worst_ha = prediction.get("large_case_ha", 0.0) 
        
        is_danger = prediction.get("is_large_fire", False) 
        
        data_status = "NORMAL"
        notice_msg = "실시간 관측 및 지역 산림 데이터를 기반으로 산출되었습니다."
        
        if stats.get("is_fallback"):
            data_status = "FALLBACK"
            notice_msg = "⚠️ API 장애로 인해 일부 통계가 현재 날씨 기반 평균값으로 대체되었습니다."
        
        res = {
            "stnId": stats.get("stn_id", 0), # 24시간 데이터 조회에 사용된 관측소 ID
            "risk": "위험" if is_danger else "주의",
            "score": round(gate_score * 100, 2), # 0~100점 변환, # 문지기가 판단한 대형산불 확률
            "typical_area_ha": normal_ha,
            "normal_area_ha": normal_ha,
            "worst_area_ha": worst_ha, 
            "direction": prediction.get("direction", "정보없음"),
            "weather": {
                "temp": current_weather.get("temp", 0),
                "windSpeed": current_weather.get("windSpeed", 0),
                "windDir": current_weather.get("windDir", 0),
                "precipitation": current_weather.get("precipitation", 0),
                "humidity": current_weather.get("humidity", 0)
            },
            "data_status": data_status,
            "notice": stats.get("notice", notice_msg) 
        }

        # 🔥 로그 출력 (Java 서버 전송 전 확인)
        print(f"📤 [PYTHON -> JAVA] 최종 응답 전송 데이터: {res}\n")
        
        return res

    except Exception as e:
        print(f"❌ 서버 에러: {str(e)}")
        return {"error": str(e), "data_status": "ERROR"}