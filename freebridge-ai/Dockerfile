FROM python:3.11-slim

# 패키지 설치 최적화 및 보안 강화
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 비특권 사용자 생성
RUN addgroup --system appgroup && adduser --system --group appuser

WORKDIR /app

# 의존성 설치 (캐시 사용 안함)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사 및 권한 설정
COPY . .
RUN chown -R appuser:appgroup /app

# 사용자 전환
USER appuser

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]