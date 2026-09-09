# Stage 1: Build jar
FROM maven:3.9.12-eclipse-temurin-21 AS builder
WORKDIR /app

# 複製 Maven 設定和程式碼
COPY concert-ticket-backend/pom.xml .
RUN mvn dependency:go-offline
COPY concert-ticket-backend/src ./src

# Build jar
RUN mvn clean package -DskipTests

# Stage 2: Run jar
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# 從 builder 複製 jar
COPY --from=builder /app/target/*.jar app.jar

RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*
COPY concert-ticket-analytics/requirements.txt /app/concert-ticket-analytics/requirements.txt
RUN python3 -m venv /opt/analytics-venv \
    && /opt/analytics-venv/bin/python -m pip install --no-cache-dir -r /app/concert-ticket-analytics/requirements.txt
COPY concert-ticket-analytics/analyze.py /app/concert-ticket-analytics/analyze.py

# 不指定 profile !!!
ENTRYPOINT ["java","-jar","app.jar"]
