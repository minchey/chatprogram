# 🔹 Java 21 JDK 슬림 이미지 사용
FROM openjdk:21-jdk-slim

# 🔹 Maven 설치
RUN apt-get update && apt-get install -y maven

# 🔹 작업 디렉토리 설정
WORKDIR /app

# 🔹 프로젝트 파일 복사
COPY . .

# 🔹 종속성 미리 다운로드 (옵션)
RUN mvn dependency:go-offline -B

# 🔹 jar 파일 생성 (테스트 제외)
RUN mvn clean package -DskipTests

# 🔹 애플리케이션 실행 (jar 이름 자동으로 반영)
CMD ["java", "-jar", "target/secure-chat-0.0.1-SNAPSHOT.jar"]
