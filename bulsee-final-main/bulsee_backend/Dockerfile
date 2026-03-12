# 1단계: 도커 안에서 자바 17과 Gradle을 이용해 .jar 파일 만들기 (빌드)
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

# 2단계: 만들어진 .jar 파일만 쏙 뽑아서 서버 실행하기
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]