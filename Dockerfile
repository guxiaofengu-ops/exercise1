FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY config ./config
RUN mvn -B -q clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends libxext6 libxrender1 libxtst6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/massey-text-editor.jar /app/massey-text-editor.jar
COPY config /app/config
ENTRYPOINT ["java", "-jar", "/app/massey-text-editor.jar"]
