# 🛡️ TCP 암호화 채팅 프로그램

## 📌 프로젝트 목표
- Java 기반 TCP/UDP 채팅 프로그램 구현
- 종단간 암호화(E2EE) 지원
- Docker 환경에서 테스트 가능하도록 구성

## 🧩 현재까지 구현한 기능
- [x] 서버 연결 - Socket, SeverSocket
- [x] 클라이언트 메시지 전송 BufferedReader, PrintWriter, InputStreamReader
- [x] 서버 메시지 수신 및 출력 서버-클라이언트 양방향 완료
- [x] 암호화 적용
- [x] 다중 클라이언트 처리
- [ ] Docker 환경 구성

### ✅ 서버 (ChatServer.java)
- 포트 9999에서 클라이언트 연결 대기
- 클라이언트가 보내는 메시지 실시간 수신
- "종료" 입력 시 연결 종료 및 서버 종료
- 예외 처리 및 자원 정리 완료

### ✅ 클라이언트 (ChatClient.java)
- 서버(`127.0.0.1:9999`)에 연결 요청
- 사용자로부터 메시지 입력 받음 (`BufferedReader` 사용)
- 서버로 메시지를 전송 (`PrintWriter`)
- "종료" 입력 시 연결 종료

## 🛠 기술 스택
- Java 21
- Spring Boot 3 (필요 최소화 사용 예정)
- Docker (구성 예정)
- Hybrid Encryption (RSA + AES) (미구현)

## 📦 디렉토리 구조 (예시)
```
secure_chat/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com.chatproject.secure_chat/
│   │           ├── server/
│   │           │   └── ChatServer.java
│   │           │   └── CLientInfo.java
│   │           │   └── ClientMessageReader.java
│   │           ├── client/
│   │           │    └── ChatClient.java
│   │           │    └── MsgFormat.java
│   │           │    └── SeverMessageReader.java
│   │           ├── crypto
│   │           │    └── AESUtil.java
│   │           │    └── RSAUtil.java
├── README.md
└── ...
```

## 🧭 향후 계획
- [ ] 서버 → 클라이언트 응답 구현
- [ ] 다중 클라이언트 대응 (스레드)
- [ ] RSA/AES 기반 메시지 암호화
- [ ] Docker 기반 실행 환경 구성
- [ ] GUI 또는 명령어 기반 인터페이스 개선 (선택)

## ✨ 실행 방법
```bash
# 터미널 1 (서버 실행)
javac ChatServer.java
java com.chatproject.secure_chat.server.ChatServer

# 터미널 2 (클라이언트 실행)
javac ChatClient.java
java com.chatproject.secure_chat.client.ChatClient
```
