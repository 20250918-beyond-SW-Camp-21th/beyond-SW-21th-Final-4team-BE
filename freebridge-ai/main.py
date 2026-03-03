import os
import logging
from fastapi import FastAPI
from dotenv import load_dotenv
from routers import recommendation

# .env 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# FastAPI 앱 생성
app = FastAPI(title="FreeBridge AI Service")

# [수정] 분리한 추천 라우터를 앱에 등록 (Spring의 Component Scan 느낌)
app.include_router(recommendation.router)

@app.get("/")
async def health_check():
    return {"status": "ok", "message": "FreeBridge AI Service is running"}
