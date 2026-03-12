import httpx
import asyncio
import math
from datetime import datetime, timedelta

# ---------------------------------------------------------
# [설정] 기상청 API Hub 키
# ---------------------------------------------------------
HISTORY_API_KEY = "5rvB--chTku7wfvnIe5LDA"

# ---------------------------------------------------------
# [안전 설정] 동시 요청 제한
# ---------------------------------------------------------
MAX_CONCURRENT_REQUESTS = 5 

# ---------------------------------------------------------
# [데이터] 전국 기상 관측소 (ASOS 95개 지점 - 촘촘한 버전)
# ---------------------------------------------------------
STATION_LIST = [
    # [서울/경기/인천]
    {"id": 108, "name": "서울", "lat": 37.5714, "lon": 126.9658},
    {"id": 112, "name": "인천", "lat": 37.4777, "lon": 126.6249},
    {"id": 119, "name": "수원", "lat": 37.2723, "lon": 126.9918},
    {"id": 202, "name": "양평", "lat": 37.4886, "lon": 127.4945},
    {"id": 203, "name": "이천", "lat": 37.2640, "lon": 127.4842},
    {"id": 98,  "name": "동두천", "lat": 37.9019, "lon": 127.0607},
    {"id": 99,  "name": "파주", "lat": 37.8859, "lon": 126.7665},
    {"id": 102, "name": "백령도", "lat": 37.9661, "lon": 124.6305},
    {"id": 201, "name": "강화", "lat": 37.7074, "lon": 126.4463},

    # [강원도] (산불 핵심 지역)
    {"id": 105, "name": "강릉", "lat": 37.7515, "lon": 128.8910},
    {"id": 100, "name": "대관령", "lat": 37.6771, "lon": 128.7183}, # 산간
    {"id": 101, "name": "춘천", "lat": 37.9026, "lon": 127.7357},
    {"id": 104, "name": "북강릉", "lat": 37.8046, "lon": 128.8554},
    {"id": 106, "name": "동해", "lat": 37.5071, "lon": 129.1243},
    {"id": 114, "name": "원주", "lat": 37.3375, "lon": 127.9466},
    {"id": 211, "name": "인제", "lat": 38.0582, "lon": 128.1657},
    {"id": 212, "name": "홍천", "lat": 37.6836, "lon": 127.8804},
    {"id": 216, "name": "태백", "lat": 37.1627, "lon": 128.9867},
    {"id": 217, "name": "정선", "lat": 37.3807, "lon": 128.6665},
    {"id": 90,  "name": "속초", "lat": 38.2509, "lon": 128.5647},
    {"id": 93,  "name": "북춘천", "lat": 37.9474, "lon": 127.7544},
    {"id": 95,  "name": "철원", "lat": 38.1479, "lon": 127.3042},

    # [충청도]
    {"id": 131, "name": "청주", "lat": 36.6392, "lon": 127.4407},
    {"id": 133, "name": "대전", "lat": 36.3720, "lon": 127.3721},
    {"id": 127, "name": "충주", "lat": 36.9705, "lon": 127.9525},
    {"id": 129, "name": "서산", "lat": 36.7766, "lon": 126.4939},
    {"id": 177, "name": "홍성", "lat": 36.6110, "lon": 126.6875},
    {"id": 221, "name": "제천", "lat": 37.1592, "lon": 128.1943},
    {"id": 226, "name": "보은", "lat": 36.4876, "lon": 127.7302},
    {"id": 232, "name": "천안", "lat": 36.7611, "lon": 127.2983},
    {"id": 235, "name": "보령", "lat": 36.3260, "lon": 126.5545},
    {"id": 236, "name": "부여", "lat": 36.2719, "lon": 126.9016},
    {"id": 238, "name": "금산", "lat": 36.1060, "lon": 127.4879},

    # [경상도]
    {"id": 143, "name": "대구", "lat": 35.8779, "lon": 128.6530},
    {"id": 159, "name": "부산", "lat": 35.1047, "lon": 129.0320},
    {"id": 152, "name": "울산", "lat": 35.5651, "lon": 129.3200},
    {"id": 155, "name": "창원", "lat": 35.1702, "lon": 128.5728},
    {"id": 136, "name": "안동", "lat": 36.5729, "lon": 128.7073},
    {"id": 138, "name": "포항", "lat": 36.0320, "lon": 129.3800},
    {"id": 192, "name": "진주", "lat": 35.1638, "lon": 128.0400},
    {"id": 253, "name": "김해", "lat": 35.2266, "lon": 128.8930},
    {"id": 257, "name": "양산", "lat": 35.3072, "lon": 129.0200},
    {"id": 271, "name": "봉화", "lat": 36.9436, "lon": 128.9145},
    {"id": 272, "name": "영주", "lat": 36.8718, "lon": 128.5170},
    {"id": 273, "name": "문경", "lat": 36.6273, "lon": 128.1488},
    {"id": 277, "name": "영덕", "lat": 36.5334, "lon": 129.4093},
    {"id": 278, "name": "의성", "lat": 36.3561, "lon": 128.6886},
    {"id": 279, "name": "구미", "lat": 36.1306, "lon": 128.3206},
    {"id": 281, "name": "영천", "lat": 35.9774, "lon": 128.9514},
    {"id": 284, "name": "거창", "lat": 35.6674, "lon": 127.9090},
    {"id": 285, "name": "합천", "lat": 35.5650, "lon": 128.1699},
    {"id": 288, "name": "밀양", "lat": 35.4915, "lon": 128.7441},
    {"id": 289, "name": "산청", "lat": 35.4130, "lon": 127.8791},
    {"id": 294, "name": "거제", "lat": 34.8882, "lon": 128.6046},
    {"id": 295, "name": "남해", "lat": 34.8166, "lon": 127.9264},

    # [전라도]
    {"id": 156, "name": "광주", "lat": 35.1729, "lon": 126.8916},
    {"id": 146, "name": "전주", "lat": 35.8408, "lon": 127.1172},
    {"id": 165, "name": "목포", "lat": 34.8169, "lon": 126.3812},
    {"id": 168, "name": "여수", "lat": 34.7383, "lon": 127.7306},
    {"id": 140, "name": "군산", "lat": 36.0053, "lon": 126.7614},
    {"id": 172, "name": "고창", "lat": 35.4266, "lon": 126.6970},
    {"id": 174, "name": "순천", "lat": 35.0210, "lon": 127.3820},
    {"id": 243, "name": "부안", "lat": 35.7369, "lon": 126.7093},
    {"id": 244, "name": "임실", "lat": 35.6119, "lon": 127.2876},
    {"id": 245, "name": "정읍", "lat": 35.5630, "lon": 126.8530},
    {"id": 247, "name": "남원", "lat": 35.4055, "lon": 127.3912},
    {"id": 248, "name": "장수", "lat": 35.6514, "lon": 127.5255},
    {"id": 260, "name": "장흥", "lat": 34.6888, "lon": 126.9195},
    {"id": 261, "name": "해남", "lat": 34.5538, "lon": 126.5691},
    {"id": 262, "name": "고흥", "lat": 34.6183, "lon": 127.2757},

    # [제주도]
    {"id": 184, "name": "제주", "lat": 33.5141, "lon": 126.5297},
    {"id": 185, "name": "고산", "lat": 33.2938, "lon": 126.1628},
    {"id": 188, "name": "성산", "lat": 33.3868, "lon": 126.8802},
    {"id": 189, "name": "서귀포", "lat": 33.2462, "lon": 126.5653},

    # [울릉도/독도]
    {"id": 115, "name": "울릉도", "lat": 37.4925, "lon": 130.9127}
]

def get_nearest_station(user_lat, user_lon):
    min_dist = float('inf')
    nearest_stn_id = 108
    for stn in STATION_LIST:
        dist = math.sqrt((user_lat - stn["lat"])**2 + (user_lon - stn["lon"])**2)
        if dist < min_dist:
            min_dist = dist
            nearest_stn_id = stn["id"]
    return nearest_stn_id

# ---------------------------------------------------------
# [비동기] 재시도 + 카운팅 기능 포함
# ---------------------------------------------------------
async def get_weather_24h_by_coords(lat, lon):
    stn_id = get_nearest_station(lat, lon)
    weather_data_list = []
    
    current_dt = datetime.now().replace(minute=0, second=0, microsecond=0)
    print(f"⚡ [Robust Async] 과거 데이터 수집 시작 (지점: {stn_id}, 재시도 모드 ON)")

    urls = []
    for i in range(24):
        target_dt = current_dt - timedelta(hours=i)
        tm_str = target_dt.strftime("%Y%m%d%H%M")
        url = f"https://apihub.kma.go.kr/api/typ01/url/awsh.php?tm={tm_str}&stn={stn_id}&help=0&authKey={HISTORY_API_KEY}"
        urls.append(url)

    sem = asyncio.Semaphore(MAX_CONCURRENT_REQUESTS)
    
    # 🔥 [추가] 총 API 호출 횟수를 셀 변수
    total_api_calls = 0

    async def fetch_with_retry(client, url):
        nonlocal total_api_calls # 바깥 변수를 가져와서 씁니다
        
        async with sem:
            for attempt in range(3):
                total_api_calls += 1 # 👈 호출할 때마다 카운트 증가!
                try:
                    response = await client.get(url, timeout=10.0)
                    if response.status_code == 200:
                        return response
                except Exception as e:
                    if attempt < 2: 
                        await asyncio.sleep(0.5)
                    else:
                        print(f"⚠️ 3회 시도 실패 (URL: {url[-20:]}): {e}")
                        
        return None

    # verify=False로 보안 무시
    async with httpx.AsyncClient(verify=False) as client:
        tasks = [fetch_with_retry(client, url) for url in urls]
        responses = await asyncio.gather(*tasks)

    for response in responses:
        if response and response.status_code == 200:
            if "Error" in response.text or "Auth" in response.text:
                continue
            
            parsed_data = parse_api_hub_manual(response.text)
            if parsed_data:
                weather_data_list.append(parsed_data)

    # 🔥 [수정] 로그에 총 호출 횟수 포함
    print(f"✅ [End] 수집 완료 (성공: {len(weather_data_list)}개 / 총 API 호출: {total_api_calls}회)")

    # 시간순 정렬
    weather_data_list.sort(key=lambda x: x['tm'])
    return weather_data_list

def parse_api_hub_manual(text_data):
    lines = text_data.strip().split('\n')
    
    for line in lines:
        line = line.strip()
        # 주석(#)이나 헤더 건너뛰기
        if not line or line.startswith("#") or line.startswith("YYMMDD"):
            continue
            
        parts = line.split()
        
        # 샘플 구조: TM STN TA WD WS RN_DAY RN_HR1 HM PA PS (최소 8개 필요)
        if len(parts) < 8:
            continue

        try:
            def sf(v):
                try: 
                    val = float(v)
                    # 결측치(-99.0 등)는 0.0으로 처리
                    return val if val > -50.0 else 0.0 
                except: return 0.0

            # 1. 데이터 추출
            ta = sf(parts[2])      # 기온 (C)
            wd = sf(parts[3])      # 풍향 (deg)
            ws = sf(parts[4])      # 풍속 (m/s)
            rn_hr1 = sf(parts[6])  # 1시간 강수량 (mm)
            hm = sf(parts[7])      # 습도 (%)

            # 2. 🔥 모든 지표 로그 출력 (가독성 업그레이드)
            print(f"📊 [관측데이터] 기온: {ta:>5}℃ | 풍향: {wd:>5}° | 풍속: {ws:>4}m/s | 강수: {rn_hr1:>4}mm | 습도: {hm:>5}%")

            return {
                "tm": parts[0],
                "stn": parts[1],
                "ta": ta,
                "wd": wd,
                "ws": ws,
                "rn_hr1": rn_hr1,
                "hm": hm
            }
        except Exception:
            continue
    return None

def get_history_stats(weather_data_list, current_weather=None):
    """
    24시간 전 데이터를 지표별(온도, 습도, 강수, 풍속, 풍향)로 검사하여
    누락된 항목만 실시간 데이터로 개별 대체하고 지정된 안내 멘트를 생성합니다.
    """
    is_fallback = False

    stn_id = 0
    if weather_data_list and len(weather_data_list) > 0:
        # 첫 번째 데이터에서 stn 값을 가져옴
        val = weather_data_list[0].get('stn')
        if val: stn_id = int(val)
    
    # 실시간 데이터에서 5개 지표 추출 (대체용 기본값)
    cw = current_weather if current_weather else {}
    c_ta = cw.get("temp", 0.0)
    c_wd = cw.get("windDir", 0.0)
    c_ws = cw.get("windSpeed", 2.0)
    c_hm = cw.get("humidity", 35.0)
    c_rn = cw.get("precipitation", 0.0)

    # 24개 슬롯 보장 및 지표별 개별 메꾸기
    tas, wds, wss, hms, rns = [], [], [], [], []
    
    for i in range(24):
        # 해당 시간에 데이터가 아예 없거나 지표가 누락된 경우를 하나씩 체크
        data = weather_data_list[i] if (weather_data_list and i < len(weather_data_list)) else {}
        
        # 기온 메꾸기
        if "ta" in data: tas.append(data["ta"])
        else:
            tas.append(c_ta)
            is_fallback = True
            
        # 풍향 메꾸기
        if "wd" in data: wds.append(data["wd"])
        else:
            wds.append(c_wd)
            is_fallback = True

        # 풍속 메꾸기
        if "ws" in data: wss.append(data["ws"])
        else:
            wss.append(c_ws)
            is_fallback = True

        # 강수량 메꾸기
        if "rn_hr1" in data: rns.append(data["rn_hr1"])
        else:
            rns.append(c_rn)
            is_fallback = True

        # 습도 메꾸기
        if "hm" in data: hms.append(data["hm"])
        else:
            hms.append(c_hm)
            is_fallback = True

    # 안내 멘트 설정 (요청하신 문구 그대로)
    notice_msg = "실시간 관측 및 지역별 산림 데이터를 기반으로 산출되었습니다."
    if is_fallback:
        notice_msg = "현재 24시간 전 데이터가 API 호출 실패로 인하여 현재날씨로 변경되었습니다"

    return {
        "stn_id": stn_id,
        "ta_list": tas, "wd_list": wds, "ws_list": wss, "hm_list": hms, "rn_list": rns,
        "min_hm": min(hms), "max_ws": max(wss), "mean_ta": sum(tas)/len(tas),
        "is_fallback": is_fallback,
        "notice": notice_msg
    }