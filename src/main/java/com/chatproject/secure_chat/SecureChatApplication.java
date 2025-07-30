package com.chatproject.secure_chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.chatproject.secure_chat.server.ChatServer;


@SpringBootApplication
public class SecureChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureChatApplication.class, args);
		// ✅ 여기에 실제 소켓 서버 실행 코드가 있어야 살아 있음
		ChatServer server = new ChatServer();
		server.start();
	}

}
