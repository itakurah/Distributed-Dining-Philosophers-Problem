# Use an official Maven base image
FROM maven:3.8.7-openjdk-18-slim AS build
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY pom.xml $APP_HOME

# Copy the source code and resources
COPY src $APP_HOME/src

# Build the Maven project
RUN mvn clean install -DskipTests

# Use an official OpenJDK runtime as a base image
FROM openjdk:21-jdk-slim
ENV ARTIFACT_NAME=ddpp-1.0.jar
ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME

# Copy the JAR file from the build stage to the final image
COPY --from=build $APP_HOME/target/$ARTIFACT_NAME .

# Run Java application
CMD ["java", "-jar", "Application.java"]
