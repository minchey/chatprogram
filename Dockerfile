FROM openjdk:21-slim

# 1. 작업 디렉터리 설정
WORKDIR /app

# 2. 프로젝트 전체 복사
COPY . .

# 3. Maven 설치 및 빌드
RUN apt-get update && apt-get install -y maven \
  && mvn clean package -DskipTests

# 4. 실행
CMD ["java", "-jar", "target/secure-chat-1.0.jar"]
