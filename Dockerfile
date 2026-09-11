# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 캐시 레이어 분리 (소스 변경 시 의존성 재다운로드 방지)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 타임존 설정
ENV TZ=Asia/Seoul

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
# exec — 셸 대신 JVM이 PID 1이 되어 docker stop의 SIGTERM을 직접 받는다 (graceful shutdown·종료 시 조회수 동기화 실행)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]