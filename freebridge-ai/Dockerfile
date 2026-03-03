# 1단계: 빌드 스테이지
FROM python:3.11-slim AS builder

WORKDIR /app

# 빌드에 필요한 시스템 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
# --user를 빼고 --prefix를 써서 /install 폴더에 라이브러리를 모으기
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt


# 2단계: 실행 스테이지
FROM python:3.11-slim

WORKDIR /app

# 빌드 단계에서 설치한 라이브러리를 파이썬 기본 경로(/usr/local)로 복사
COPY --from=builder /install /usr/local
COPY . .

# 권한 설정
RUN addgroup --system appgroup && adduser --system --group appuser && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

# uvicorn 앞에 python -m을 붙여서 더 안전하게 실행
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]