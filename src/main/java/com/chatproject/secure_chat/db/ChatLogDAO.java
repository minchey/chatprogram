package com.chatproject.secure_chat.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatLogDAO {
    public Connection connect() throws Exception {
        String url = "jdbc:sqlite:chatlog.db";
        return DriverManager.getConnection(url);
    }

    // ★ 암호문 + 두 종류의 aesKey 저장(IV 없음)
    public void insertMessage(String sender, String receiver,
                              String ciphertext,
                              String aesKeyForReceiver,
                              String aesKeyForSender,
                              String timeStamp) {
        String sql = "INSERT INTO messages(sender, receiver, ciphertext, aes_key_for_receiver, aes_key_for_sender, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, ciphertext);
            pstmt.setString(4, aesKeyForReceiver);
            pstmt.setString(5, aesKeyForSender);
            pstmt.setString(6, timeStamp);

            pstmt.executeUpdate();
            System.out.println("✅ 메시지 삽입 완료");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 요청자/상대 간 모든 메시지 반환 (암호문 + 두 키 포함)
    public List<ChatMessage> getMessageBetween(String user1, String user2) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql =
                "SELECT sender, receiver, ciphertext, aes_key_for_receiver, aes_key_for_sender, timestamp " +
                        "FROM messages " +
                        "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                        "ORDER BY timestamp ASC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user1);
            pstmt.setString(2, user2);
            pstmt.setString(3, user2);
            pstmt.setString(4, user1);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new ChatMessage(
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("ciphertext"),
                        rs.getString("aes_key_for_receiver"),
                        rs.getString("aes_key_for_sender"),
                        rs.getString("timestamp")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("메시지를 가져오는데 실패했습니다.");
        }
        return messages;
    }

    public static void main(String[] args) {
        String url = "jdbc:sqlite:chatlog.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // ★ 신규 생성 시의 스키마(IV 없음)
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender TEXT NOT NULL," +
                    "receiver TEXT NOT NULL," +
                    "ciphertext TEXT NOT NULL," +               // Base64 AES 암호문
                    "aes_key_for_receiver TEXT NOT NULL," +     // 수신자용 RSA 암호문 키
                    "aes_key_for_sender  TEXT NOT NULL," +      // 발신자용 RSA 암호문 키
                    "timestamp TEXT NOT NULL" +
                    ");";
            stmt.execute(sql);
            System.out.println("✅ 테이블 생성/확인 완료");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
