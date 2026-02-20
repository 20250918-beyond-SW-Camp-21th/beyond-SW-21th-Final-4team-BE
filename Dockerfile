FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Create non-root user for security (Trivy DS-0002)
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the built jar file (only bootJar is produced; plain jar is disabled in build.gradle)
COPY freebridge/app-main/build/libs/*.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appgroup /app/app.jar

# Expose the port the app runs on
EXPOSE 8080

# Switch to non-root user
USER appuser

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
