# ---- Build stage ----
FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew
RUN gradle --version
RUN ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# bootJar 결과물이 app.jar로 고정되어 있으므로 정확히 지정
COPY --from=build /workspace/build/libs/app.jar /app/app.jar

# 가장 보수적인 기본값만
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]