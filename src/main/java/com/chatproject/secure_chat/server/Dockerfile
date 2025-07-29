#베이스 이미지 선택 (JDK가 설치 된 환경)
From openjdk:17

#작업 디렉토리 생성
WORKDIR / app

#현재 디렉토리에 있는 모든 파일을 컨테이너 안의 /app으로 복사
COPY . .

#자바 파일을 컴파일 (패키지 구조 주의)
RUN javac com/chatproject/secure_chat/server/ChatServer.java

#서버 실행 명령어 (ChatServer가 main 메서드를 가지고 있어야함)
CMD ["java", "com.chatproject.secure_chat.server.ChatServer"]