# --- 첫 번째 스테이지: 빌드
FROM gradle:jdk21-graal-jammy AS builder

WORKDIR /workspace

COPY build.gradle .
COPY settings.gradle .
RUN gradle wrapper
RUN ./gradlew dependencies
COPY src src
RUN ./gradlew build -x test

# --- 두 번째 스테이지: 실행 (distroless)
FROM gcr.io/distroless/java21-debian12

WORKDIR /workspace
COPY --from=builder /workspace/build/libs/*.jar app.jar

# JVM 옵션
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/workspace/app.jar"]
