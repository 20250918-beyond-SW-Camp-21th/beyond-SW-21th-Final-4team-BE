# 1단계: 빌드된 통짜 JAR 파일을 추출(Extract)하는 단계
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
# 빌드된 JAR 파일을 가져옵니다 (경로는 기존과 동일하게 맞춤)
COPY freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar app.jar
# JAR 파일 내부에 있는 클래스, 라이브러리들을 레이어별로 해체시킵니다.
RUN java -Djarmode=layertools -jar app.jar extract

# 2단계: 해체된 레이어들을 순서대로 새 이미지에 복사하는 단계
# 기존 jammy(우분투 기반) 대신 alpine 리눅스 기반으로 변경하여 베이스 이미지 크기를 대폭 줄입니다.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 변경 빈도가 낮고 크기가 거대한 라이브러리 레이어를 먼저 캐싱합니다. (이 부분은 한 번 올라가면 안 올라감!)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./

# 변경 빈도가 높고 크기가 아주 작은 소스코드 레이어를 마지막에 캐싱
COPY --from=builder /app/application/ ./

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Spring Boot 구동 명령 변경 (Fat JAR가 아닌 클래스 기반 로딩)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
