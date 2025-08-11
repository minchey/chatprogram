package com.chatproject.secure_chat.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 저장/조회 DAO (강화판)
 * - SQLite: chatlog.db
 * - 스키마(IV 없음): sender, receiver, ciphertext, aes_key_for_receiver, aes_key_for_sender, timestamp
 * - 부가 기능:
 *   1) ensurePragmas(): WAL 등 SQLite 설정
 *   2) ensureSchema(): 테이블 생성(없으면)
 *   3) migrateIfNeeded(): 컬럼 없으면 추가 (과거 스키마 호환)
 *   4) ensureIndexes(): 조회 성능 인덱스
 *
 * 사용 권장: 서버 시작 시 아래 순서로 한 번만 호출
 *   ChatLogDAO.bootstrap();  // PRAGMA → 스키마 → 마이그 → 인덱스
 */
public class ChatLogDAO {

    private static final String DB_URL = "jdbc:sqlite:chatlog.db";

    public static void bootstrap() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ensurePragmas(conn);
            ensureSchema(conn);
            migrateIfNeeded(conn);
            ensureIndexes(conn);
            System.out.println("✅ ChatLogDAO bootstrap 완료");
        } catch (Exception e) {
            System.out.println("❌ ChatLogDAO bootstrap 실패");
            e.printStackTrace();
        }
    }

    /** 요청마다 연결을 열고 닫습니다(간단/안전). */
    public Connection connect() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    /** SQLite 동작 모드 권장 설정 (선택) */
    private static void ensurePragmas(Connection conn) {
        try (Statement st = conn.createStatement()) {
            // WAL 모드: 동시 읽기/쓰기 개선
            st.execute("PRAGMA journal_mode=WAL;");
            // 안전하게 디스크 동기화 (성능↔안전 트레이드오프: NORMAL도 가능)
            st.execute("PRAGMA synchronous=NORMAL;");
        } catch (Exception e) {
            System.out.println("⚠️ PRAGMA 설정 실패(무시 가능): " + e.getMessage());
        }
    }

    /** 테이블 없으면 생성 */
    private static void ensureSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
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
        }
    }

    /** 과거 스키마에서 넘어올 때 컬럼 보강 (데이터 변환은 불가) */
    private static void migrateIfNeeded(Connection conn) {
        try {
            DatabaseMetaData md = conn.getMetaData();

            boolean hasCipher = hasColumn(md, "messages", "ciphertext");
            boolean hasRecv   = hasColumn(md, "messages", "aes_key_for_receiver");
            boolean hasSend   = hasColumn(md, "messages", "aes_key_for_sender");

            try (Statement st = conn.createStatement()) {
                if (!hasCipher) {
                    st.executeUpdate("ALTER TABLE messages ADD COLUMN ciphertext TEXT");
                    System.out.println("🧩 migration: add ciphertext");
                }
                if (!hasRecv) {
                    st.executeUpdate("ALTER TABLE messages ADD COLUMN aes_key_for_receiver TEXT");
                    System.out.println("🧩 migration: add aes_key_for_receiver");
                }
                if (!hasSend) {
                    st.executeUpdate("ALTER TABLE messages ADD COLUMN aes_key_for_sender TEXT");
                    System.out.println("🧩 migration: add aes_key_for_sender");
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ migrateIfNeeded 실패(무시 가능): " + e.getMessage());
        }
    }

    private static boolean hasColumn(DatabaseMetaData md, String table, String column) throws SQLException {
        try (ResultSet rs = md.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    /** 조회 성능 인덱스 */
    private static void ensureIndexes(Connection conn) {
        try (Statement st = conn.createStatement()) {
            // 쿼리 패턴: WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY timestamp
            st.execute("CREATE INDEX IF NOT EXISTS idx_messages_pair_time " +
                    "ON messages (sender, receiver, timestamp);");
        } catch (Exception e) {
            System.out.println("⚠️ 인덱스 생성 실패(무시 가능): " + e.getMessage());
        }
    }

    /** 메시지 1건 저장 */
    public void insertMessage(String sender, String receiver,
                              String ciphertext,
                              String aesKeyForReceiver,
                              String aesKeyForSender,
                              String timeStamp) {
        String sql = "INSERT INTO messages(sender, receiver, ciphertext, aes_key_for_receiver, aes_key_for_sender, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nonNull(sender));
            pstmt.setString(2, nonNull(receiver));
            pstmt.setString(3, nonNull(ciphertext));
            pstmt.setString(4, nonNull(aesKeyForReceiver));
            pstmt.setString(5, aesKeyForSender); // null 허용
            pstmt.setString(6, nonNull(timeStamp));

            pstmt.executeUpdate();
            System.out.println("✅ 메시지 삽입 완료");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 두 사용자 간의 모든 메시지(오래된 순) 조회 */
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

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("메시지를 가져오는데 실패했습니다.");
        }
        return messages;
    }

    private static String nonNull(String s) {
        if (s == null) throw new IllegalArgumentException("null not allowed");
        return s;
    }

    /** 수동 실행용: 테이블/인덱스 보장 */
    public static void main(String[] args) {
        bootstrap();
    }
}
