# 1단계: Maven으로 프로젝트 빌드
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 실행용 JDK 이미지
FROM eclipse-temurin:21-jdk
WORKDIR /app

# ✅ 시간대 설정
ENV TZ=Asia/Seoul
RUN apt-get update && apt-get install -y tzdata \
  && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
  && echo $TZ > /etc/timezone

# ✅ 앱 복사 및 실행 설정
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# ✅ JVM 시간대도 명시적으로 설정 (중복 보장 차원)
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
