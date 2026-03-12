from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from service import weather_service       
from service import weather_24h_service   

router = APIRouter(
    prefix="/weather",
    tags=["Weather"],
    responses={404: {"description": "Not found"}}
)

class WeatherRequest(BaseModel):
    lat: float
    lon: float

# [POST] 현재 날씨 (동기 함수라 그대로 둠)
@router.post("/current", summary="현재 날씨 조회")
def get_current_weather(request: WeatherRequest):
    try:
        return weather_service.get_weather(request.lat, request.lon)
    except Exception as e:
        print(f"❌ 날씨 조회 실패: {e}")
        raise HTTPException(status_code=500, detail="Error")

# 🔥 [수정됨] async def 사용 및 await 추가
@router.post("/history", summary="과거 24시간 조회 (고속 병렬 처리)")
async def get_weather_history(request: WeatherRequest):
    try:
        # await 키워드 필수! (비동기 함수 호출)
        raw_data = await weather_24h_service.get_weather_24h_by_coords(request.lat, request.lon)
        
        if not raw_data:
            raise HTTPException(status_code=404, detail="데이터 없음")

        ai_input_data = weather_24h_service.get_history_stats(raw_data)
        
        return {
            "status": "success",
            "station_count": len(raw_data),
            "ai_input_feature": ai_input_data,
            "raw_data": raw_data
        }
        
    except Exception as e:
        print(f"❌ 에러: {e}")
        raise HTTPException(status_code=500, detail=str(e))