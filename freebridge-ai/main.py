import os
import logging
from fastapi import FastAPI
from dotenv import load_dotenv
from routers import recommendation, analysis

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="FreeBridge AI Service")

app.include_router(recommendation.router)
app.include_router(analysis.router)

@app.get("/")
async def health_check():
    return {"status": "ok", "message": "FreeBridge AI Service is running"}
