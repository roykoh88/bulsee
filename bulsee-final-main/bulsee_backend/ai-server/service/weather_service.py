import requests
import math
from datetime import datetime, timedelta
from cache import weather_cache 

# [설정] 기상청 단기예보 키
SERVICE_KEY = "a073b1657a17d35a1b41c19711b8acd08a6f4cda6480e7e0d7df6c439bff0769"

def map_to_grid(lat, lon):
    # (기존 격자 변환 로직 동일)
    RE = 6371.00877
    GRID = 5.0
    SLAT1 = 30.0
    SLAT2 = 60.0
    OLON = 126.0
    OLAT = 38.0
    XO = 43
    YO = 136

    DEGRAD = math.pi / 180.0
    
    re = RE / GRID
    slat1 = SLAT1 * DEGRAD
    slat2 = SLAT2 * DEGRAD
    olon = OLON * DEGRAD
    olat = OLAT * DEGRAD

    sn = math.tan(math.pi * 0.25 + slat2 * 0.5) / math.tan(math.pi * 0.25 + slat1 * 0.5)
    sn = math.log(math.cos(slat1) / math.cos(slat2)) / math.log(sn)
    sf = math.tan(math.pi * 0.25 + slat1 * 0.5)
    sf = math.pow(sf, sn) * math.cos(slat1) / sn
    ro = math.tan(math.pi * 0.25 + olat * 0.5)
    ro = re * sf / math.pow(ro, sn)

    ra = math.tan(math.pi * 0.25 + (lat) * DEGRAD * 0.5)
    ra = re * sf / math.pow(ra, sn)
    theta = lon * DEGRAD - olon
    if theta > math.pi: theta -= 2.0 * math.pi
    if theta < -math.pi: theta += 2.0 * math.pi
    theta *= sn
    
    rs_x = math.floor(ra * math.sin(theta) + XO + 0.5)
    rs_y = math.floor(ro - ra * math.cos(theta) + YO + 0.5)
    
    return int(rs_x), int(rs_y)

def get_weather(lat, lon):
    nx, ny = map_to_grid(lat, lon)
    cache_key = f"{nx}_{ny}"
    
    # 1. 캐시 확인
    cached_data = weather_cache.get_cached_data(cache_key)
    if cached_data:
        print(f"✨ 캐시 사용: {cache_key}")
        return cached_data

    # 2. API 호출
    print(f"📡 실시간 API 호출: {cache_key} (nx:{nx}, ny:{ny})")
    
    # 2. API 호출
    try:
        now = datetime.now()
        # 초단기예보는 매시간 45분 이후에 생성되므로 1시간 전 데이터를 호출하는 것이 안정적입니다.
        base_time_dt = now - timedelta(hours=1)
        base_date = base_time_dt.strftime("%Y%m%d")
        base_time = base_time_dt.strftime("%H00")
        
        url = f"http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst?serviceKey={SERVICE_KEY}"
        params = {"pageNo": "1", "numOfRows": "60", "dataType": "JSON", "base_date": base_date, "base_time": base_time, "nx": nx, "ny": ny}
        
        response = requests.get(url, params=params, timeout=5)
        res_json = response.json()
        items = res_json['response']['body']['items']['item']
        
        # ✅ 습도(humidity) 필드 추가
        weather_data = {"temp": 0.0, "windSpeed": 0.0, "windDir": 0.0, "precipitation": 0.0, "humidity": 0.0, "time": ""}
        
        for item in items:
            cat = item['category']
            raw_val = item['fcstValue']
            
            if weather_data['time'] == "":
                weather_data['time'] = f"{item['fcstDate']}{item['fcstTime']}"

            # 수치형 변환 (문자열 처리 포함)
            try:
                if cat == 'RN1':
                    if '강수없음' in raw_val: val = 0.0
                    else: val = float(raw_val.replace('mm', ''))
                else:
                    val = float(raw_val)
            except: val = 0.0

            # 명세서 기반 매핑
            if cat == 'T1H': weather_data['temp'] = val        # 기온
            elif cat == 'WSD': weather_data['windSpeed'] = val  # 풍속
            elif cat == 'VEC': weather_data['windDir'] = val    # 풍향
            elif cat == 'RN1': weather_data['precipitation'] = val # 1시간 강수량
            elif cat == 'REH': weather_data['humidity'] = val   # ✅ 습도 (%)
            
        # 3. 캐시 저장 (10분)
        weather_cache.save_to_cache(cache_key, weather_data, duration_minutes=10)
        
        print(f"✅ [실시간날씨] 기온:{weather_data['temp']}℃ | 습도:{weather_data['humidity']}% | 풍속:{weather_data['windSpeed']}m/s | 풍향:{weather_data['windDir']}° | 강우:{weather_data['precipitation']}mm")
        return weather_data

    except Exception as e:
        print(f"❌ 실시간 날씨 실패: {e}")
        # 실패 시 기본값 (2월 평균 습도 35% 적용)
        return {"temp": 0.0, "windSpeed": 2.0, "windDir": 0.0, "precipitation": 0.0, "humidity": 35.0}