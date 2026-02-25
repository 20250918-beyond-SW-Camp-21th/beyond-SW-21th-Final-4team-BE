FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. 사용자 생성을 먼저 해서 레이어를 고정 (변하지 않는 부분)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 2. JAR 복사 (이 부분이 자주 변함)
ARG APP_JAR=freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar
COPY ${APP_JAR} app.jar

# 3. 권한 설정
RUN chown appuser:appgroup app.jar

USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]