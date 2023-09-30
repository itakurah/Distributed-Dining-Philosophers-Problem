# Use an official OpenJDK runtime as a base image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the compiled Java application JAR into the container
COPY build/libs/vsdiedritte-1.0-SNAPSHOT.jar .

# Define the command to run your Java application
CMD ["java", "-jar", "Application.jar"]