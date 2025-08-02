package com.chatproject.secure_chat.crypto;

import com.chatproject.secure_chat.db.ChatLogDAO;
public class dbtest {
    public static void main(String[] args) {
        ChatLogDAO dao = new ChatLogDAO();
        dao.insertMessage("Alice", "Bob", "테스트 메시지입니다", "2025-08-02 16:40:00");
    }

}
