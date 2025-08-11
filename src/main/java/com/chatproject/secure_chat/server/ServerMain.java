package com.chatproject.secure_chat.server;

import com.chatproject.secure_chat.db.ChatLogDAO;

/**
 * 서버 실행 엔트리포인트.
 */
public class ServerMain {
    public static void main(String[] args) {
        // (선택) DB 테이블/인덱스 보장. 강화판 DAO를 쓰는 경우만 호출하세요.
        // ChatLogDAO.bootstrap();

        new ChatServer().start();
    }
}
