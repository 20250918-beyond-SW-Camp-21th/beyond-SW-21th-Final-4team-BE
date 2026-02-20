FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar file
# Adjust the path according to your Gradle multi-module structure
COPY freebridge/app-main/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
