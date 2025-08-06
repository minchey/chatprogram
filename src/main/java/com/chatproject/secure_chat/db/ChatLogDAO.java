package com.chatproject.secure_chat.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 저장/조회 DAO
 * - SQLite 파일: chatlog.db
 * - 스키마(IV 없음):
 *   sender, receiver, ciphertext, aes_key_for_receiver, aes_key_for_sender, timestamp
 */
public class ChatLogDAO {
    public Connection connect() throws Exception {
        String url = "jdbc:sqlite:chatlog.db";
        return DriverManager.getConnection(url);
    }

    /**
     * 메시지 1건 저장
     * @param sender   보낸 사람
     * @param receiver 받는 사람
     * @param ciphertext AES 암호문(Base64)
     * @param aesKeyForReceiver 수신자용 RSA 암호문 키(Base64)
     * @param aesKeyForSender  발신자용 RSA 암호문 키(Base64) — 없으면 null 허용(그 경우 송신자는 복호화 불가)
     * @param timeStamp 타임스탬프(문자열)
     */
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
            pstmt.setString(5, aesKeyForSender);      // null 가능
            pstmt.setString(6, timeStamp);

            pstmt.executeUpdate();
            System.out.println("✅ 메시지 삽입 완료");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 두 사용자 간의 모든 메시지(오래된 순) 조회
     */
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

    /**
     * 최초 실행 시 테이블 생성 (이미 있으면 그대로 둠)
     */
    public static void main(String[] args) {
        String url = "jdbc:sqlite:chatlog.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender TEXT NOT NULL," +
                    "receiver TEXT NOT NULL," +
                    "ciphertext TEXT NOT NULL," +               // AES 암호문(Base64)
                    "aes_key_for_receiver TEXT NOT NULL," +     // 수신자용 RSA 암호문 키
                    "aes_key_for_sender  TEXT," +               // 발신자용 RSA 암호문 키(없을 수도 있음)
                    "timestamp TEXT NOT NULL" +
                    ");";
            stmt.execute(sql);
            System.out.println("✅ 테이블 생성/확인 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
