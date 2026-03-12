from fastapi import FastAPI
from api import predict_api, weather_api 

app = FastAPI()

# 라우터 등록 (조립)
app.include_router(predict_api.router)
app.include_router(weather_api.router)

@app.get("/")
def root():
    return {"message": "산불 예측 AI 서버 가동 중 🔥"}