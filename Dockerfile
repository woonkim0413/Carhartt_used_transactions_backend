# ------------------- Build stage -------------------
FROM --platform=linux/amd64 gradle:8.14.3-jdk17 AS build

WORKDIR /workspace

# Gradle wrapper와 설정 파일
COPY gradlew .
# gradle는 폴더라 .을 쓰면 gradle폴더 안의 값들이 해당 위치로 감
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱 레이어)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 후 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ------------------- Run stage (ARM64 런타임) -------------------
# 새로운 stage시작, 이전 stage와 다르게 JDK, Gradle없이 jre만 사용하여 가벼움
# FROM --platform=linux/arm64 eclipse-temurin:17-jre-jammy
# -> t4g -> t3,t2로 변경하면서 EC2 환경도 amd가 됐음, arm64 image 불필요
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 이전 stage에서 빌드된 JAR을 현재 stage로 가져옴
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]