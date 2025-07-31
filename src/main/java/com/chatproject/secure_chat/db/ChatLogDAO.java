package com.chatproject.secure_chat.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class ChatLogDAO {
    public Connection connect() throws Exception{ //connect 메서드 정의
        String url = "jdbc:sqlite:chatlog.db";
        return DriverManager.getConnection(url);
    }
    public void insertMessage(String sender, String receiver, String message, String timeStamp){
        String sql = "INSERT INTO messages(sender, receiver, message, timestamp) VALUES (?, ?, ?, ?)";

        // ✅ try-with-resources를 사용하여 자원 자동 종료
        try( Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // SQL 준비

            // ✅ 하드코딩된 값 제거하고 매개변수 사용
            pstmt.setString(1, sender);      // sender
            pstmt.setString(2, receiver);     // receiver
            pstmt.setString(3, message);     // message
            pstmt.setString(4, timeStamp); // timestamp

            pstmt.executeUpdate(); // SQL 실행
            System.out.println("✅ 메시지 삽입 완료");

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        String url = "jdbc:sqlite:chatlog.db";  // 현재 프로젝트 디렉토리에 chatlog.db 생성됨
        try (Connection conn = DriverManager.getConnection(url); //db에 연결
             Statement stmt = conn.createStatement()) { //sql 구문 실행을 위한 객체
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
