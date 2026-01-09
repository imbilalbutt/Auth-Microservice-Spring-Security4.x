# Dockerfile let us create docker image

# Step 1: Build code
# FROM maven:3.9.8-eclipse-temurin-21 AS builder
#FROM maven:3.8.4-openjdk-17 AS builder
FROM maven:3.8.4-eclipse-temurin-17 as builder
#FROM maven:3.8.4-amazoncorretto-17 AS builder
WORKDIR /app


#COPY pom xml in working directoy which is /app here
COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

# We tell maven we want to create a new package from our Spring boot app
# It creates single jar file
RUN mvn clean package -DskipTests



# Step 2: Run code

#FROM openjdk:21-jdk AS runner
#FROM eclipse-temurin:17-jdk AS runner
FROM eclipse-temurin:17-jdk-alpine AS runner
#FROM openjdk:17-jdk-alpine AS runner

WORKDIR /app

COPY --from=builder ./app/target/spring-auth-dev-1.0.0.jar ./app.jar

# We set server.port = 8081 in our application.properties
EXPOSE 8081

#ENTRYPOINT ["java" , "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "app.jar"]