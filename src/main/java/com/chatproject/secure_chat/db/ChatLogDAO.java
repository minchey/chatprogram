package com.chatproject.secure_chat.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ChatLogDAO {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:chatlog.db";  // 현재 프로젝트 디렉토리에 chatlog.db 생성됨
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender TEXT," +
                    "receiver TEXT," +
                    "message TEXT," +
                    "timestamp TEXT" +
                    ");";
            stmt.execute(sql);
            System.out.println("✅ 테이블 생성 완료");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
