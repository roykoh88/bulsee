import torch
import torch.nn as nn
import numpy as np
import os
import math

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

# DYN_COLS = ['ws_ing', 'temp_ing', 'hm_ing', 'rn_ing', 'wd_ing_sin', 'wd_ing_cos',

#             'ws24', 'temp24', 'hm24', 'rn24', 'wd24_sin', 'wd24_cos']

           

# STAT_COLS = ['forest_ratio', 'urban_ratio', 'agri_ratio', 'grass_ratio', 'forest_area',

#              'conifer_ratio', 'broadleaf_ratio', 'mixed_ratio', 'timber_stock_sum',

#              'timber_stock_mean', 'age1_ratio', 'age2_ratio', 'age3_ratio',

#              'age4_ratio', 'age5_ratio', 'age6_ratio']

# [1] 모델 구조 정의 (학습 코드와 100% 동일)
class HybridModel(nn.Module):
    def __init__(self, out_dim=1, is_classifier=False):
        super().__init__()
        self.is_classifier = is_classifier
        # Dynamic(12) -> GRU(64)
        self.gru = nn.GRU(12, 64, num_layers=2, batch_first=True, dropout=0.2)
        # GRU Output(64) + Static(16) -> FC Layers
        # (Linear -> ReLU -> Dropout -> Linear -> Linear 순서)
        self.fc = nn.Sequential(
            nn.Linear(64 + 16, 128),    # fc.0
            nn.ReLU(),                  # fc.1
            nn.Dropout(0.2),            # fc.2
            nn.Linear(128, 64),         # fc.3
            nn.Linear(64, out_dim)      # fc.4
        )
        
    def forward(self, d, s):
        _, h = self.gru(d)
        combined = torch.cat((h[-1], s), dim=1)
        out = self.fc(combined)
        return torch.sigmoid(out) if self.is_classifier else out

# [2] 모델 파일 로드 (CPU/GPU 호환)
def load_expert(filename, is_classifier=False):
    path = os.path.join(BASE_DIR, filename)
    if not os.path.exists(path):
        print(f"⚠️ 모델 파일 없음: {path}")
        return None
    
    model = HybridModel(out_dim=1, is_classifier=is_classifier).to(DEVICE)
    try:
        state_dict = torch.load(path, map_location=DEVICE)
        model.load_state_dict(state_dict)
        model.eval()
        print(f"✅ 모델 로드 완료: {filename}")
        return model
    except Exception as e:
        print(f"❌ 모델 로드 실패 ({filename}): {e}")
        return None

gatekeeper = load_expert("tuned_gatekeeper.pth", is_classifier=True)
expert_small = load_expert("tuned_expert_small.pth")
expert_large = load_expert("tuned_expert_large.pth")

# [3] 전처리 유틸리티
def simple_minmax(val, min_v, max_v):
    if val is None: val = 0
    if val < min_v: return 0.0
    
    # 정규화
    norm = (val - min_v) / (max_v - min_v + 1e-7)
    
    # 안전장치: 실제 데이터가 학습 Max보다 조금 더 클 수도 있으므로 1.0으로 캡
    return min(norm, 1.0)

def preprocess_features(history_raw, current_weather, terrain_data):
    # 시계열 데이터 구성 (24시간 데이터 활용)
    full_timeline = history_raw + [current_weather]
    if len(full_timeline) < 1: full_timeline = [current_weather] * 25
        
    seq_data = []
    # 마지막 6개 시점(WINDOW_SIZE=6) 추출
    target_indices = range(len(full_timeline) - 6, len(full_timeline))
    
    for i in target_indices:
        safe_i = max(0, min(i, len(full_timeline)-1))
        curr = full_timeline[safe_i]
        wd_rad = math.radians(curr.get('wd', 0) or 0)
        
        # 24시간 전 데이터
        prev_idx = safe_i - 24
        prev = full_timeline[prev_idx] if prev_idx >= 0 else full_timeline[0]
        prev_wd_rad = math.radians(prev.get('wd', 0) or 0)

        # 12개 Dynamic Feature 생성 (ws, ta, hm, rn ...)
        # 기상 데이터: 한국 기후의 현실적 범위 (태풍급 바람 등 고려)
        row = [
            simple_minmax(curr.get('ws', 0), 0, 35),      # 35m/s면 거의 태풍급 (충분함)
            simple_minmax(curr.get('ta', 0), -25, 40),    # 한국 기온 범위
            simple_minmax(curr.get('hm', 0), 0, 100),
            simple_minmax(curr.get('rn_hr1', 0), 0, 100),
            (math.sin(wd_rad) + 1) / 2,
            (math.cos(wd_rad) + 1) / 2,
            simple_minmax(prev.get('ws', 0), 0, 35),
            simple_minmax(prev.get('ta', 0), -25, 40),
            simple_minmax(prev.get('hm', 0), 0, 100),
            simple_minmax(prev.get('rn_hr1', 0), 0, 100),
            (math.sin(prev_wd_rad) + 1) / 2,
            (math.cos(prev_wd_rad) + 1) / 2
        ]
        seq_data.append(row)

# 🔥 [핵심 수정] 확인해주신 '진짜 Max 값' 적용!
    stat_vec = [
        terrain_data.get('forest_ratio', 0) * 0.01,
        terrain_data.get('urban_ratio', 0) * 0.01,
        terrain_data.get('agri_ratio', 0) * 0.01,
        terrain_data.get('grass_ratio', 0) * 0.01,
        
        # 1. 산림면적 Max: 158,948
        simple_minmax(terrain_data.get('forest_area', 0), 0, 158948.0), 
        
        terrain_data.get('conifer_ratio', 0) * 0.01,
        terrain_data.get('broadleaf_ratio', 0) * 0.01,
        terrain_data.get('mixed_ratio', 0) * 0.01,
        
        # 2. 임목축적합계 Max: 29,236,603 (약 3천만)
        simple_minmax(terrain_data.get('timber_stock_sum', 0), 0, 29236603.0), 
        
        # 3. 임목축적평균 Max: 193.18 (넉넉하게 200으로 설정)
        simple_minmax(terrain_data.get('timber_stock_mean', 0), 0, 200.0),
        
        terrain_data.get('age1_ratio', 0) * 0.01,
        terrain_data.get('age2_ratio', 0) * 0.01,
        terrain_data.get('age3_ratio', 0) * 0.01,
        terrain_data.get('age4_ratio', 0) * 0.01,
        terrain_data.get('age5_ratio', 0) * 0.01,
        terrain_data.get('age6_ratio', 0) * 0.01
    ]

    d_tensor = torch.tensor([seq_data], dtype=torch.float32).to(DEVICE)
    s_tensor = torch.tensor([stat_vec], dtype=torch.float32).to(DEVICE)
    return d_tensor, s_tensor

# [4] 예측 실행
def predict_fire_risk(history_raw, current_weather, terrain_data):
    if not gatekeeper or not expert_small or not expert_large:
        return {"status": "ERROR", "msg": "모델 로드 실패"}

    try:
        d, s = preprocess_features(history_raw, current_weather, terrain_data)
        
        with torch.no_grad():
           
            small_log = expert_small(d, s).item()
            small_ha = max(np.expm1(small_log), 0.0)
            
            # (B) 대형 전문가의 예측 (최악의 시나리오 규모)
            large_log = expert_large(d, s).item()
            large_ha = max(np.expm1(large_log), 0.0)

            LARGE_FIRE_THRESHOLD = 100.0
            
            # 3. 대표값(Typical) 결정: 문지기가 판단한 모델의 값을 사용
            risk_score = min(large_ha / LARGE_FIRE_THRESHOLD, 1.0)
            is_danger = risk_score >= 0.5
            
            weighted_small = small_ha * (1.0 - risk_score)
            weighted_large = large_ha * risk_score
            typical_ha = weighted_small + weighted_large
            
            # 모델 사용 정보도 명확하게 표기
            if is_danger:
                model_used = f"Blended (Large Dominant {int(risk_score*100)}%)"
            else:
                model_used = f"Blended (Small Dominant {int((1-risk_score)*100)}%)"

        # 5. [중요] 논리 보정 (Worst는 항상 Normal보다 커야 함)
            final_worst_ha = max(large_ha, small_ha)

            curr_wd = current_weather.get('wd', 0)
            spread_angle = (curr_wd + 180) % 360
            directions = ['북', '북동', '동', '남동', '남', '남서', '서', '북서']
            dir_idx = int(((spread_angle + 22.5) % 360) / 45) % 8


            return {
                "status": "SUCCESS",
                "predicted_ha": round(typical_ha, 4), 
                "small_case_ha": round(small_ha, 4),
                "large_case_ha": round(final_worst_ha, 4),
                "gatekeeper_score": round(risk_score, 4),
                "is_large_fire": is_danger,
                "model_used": model_used,
                "direction": directions[dir_idx],
                "input_summary": {
                    "curr_ws": current_weather.get('ws'), # [수정됨] 매핑된 키 사용
                    "curr_hm": current_weather.get('hm')
                }
            }
    except Exception as e:
        print(f"❌ 예측 에러: {e}")
        return {"status": "ERROR", "msg": str(e)}