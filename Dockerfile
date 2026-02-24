FROM eclipse-temurin:21-jre-jammy

WORKDIR /app


RUN groupadd --system appgroup && useradd --system --gid appgroup appuser


COPY --chown=appuser:appgroup freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]