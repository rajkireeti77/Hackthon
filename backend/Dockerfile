FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Python and necessary machine learning libraries from apt
RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-pandas python3-sklearn python3-joblib && \
    rm -rf /var/lib/apt/lists/*

COPY ml /app/ml
RUN mkdir -p /app/ml_data

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
