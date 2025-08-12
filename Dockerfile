# ===== Build & Run in one image (JDK + Maven) =====
FROM openjdk:21-jdk-slim

# Maven 설치
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 빌드 캐시 최적화: 먼저 POM만 복사해서 의존성 내려받기
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# 소스 복사 후 빌드
COPY src ./src
RUN mvn -q -DskipTests package

# 기본 실행할 메인 클래스 (서버). 컨테이너별로 MAIN env로 덮어씀
ENV MAIN=com.chatproject.secure_chat.server.ServerMain
# 서버/클라이언트 공통 환경변수 (클라이언트는 SERVER_HOST를 chat-server로 받음)
ENV SERVER_HOST=127.0.0.1
ENV SERVER_PORT=9999

# 실행: exec-maven-plugin으로 MAIN 실행 (클래스패스 자동 구성)
CMD ["bash","-lc","mvn -q -DskipTests -Dexec.mainClass=${MAIN} exec:java"]
