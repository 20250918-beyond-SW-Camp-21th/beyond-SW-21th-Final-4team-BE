FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 모듈러 모놀리스 구조 특성상 레이어 분리 시 호환성/용량 및 Docker Hub 400 에러 문제가 발생하므로
# 빌드된 Fat JAR만 단순 복사하여 안정적인 빌드 및 Push를 최우선으로 합니다.
COPY freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
