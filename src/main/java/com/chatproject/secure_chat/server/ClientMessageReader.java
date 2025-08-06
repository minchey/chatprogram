package com.chatproject.secure_chat.server; //서버스레드

import com.chatproject.secure_chat.client.MsgFormat;
import com.chatproject.secure_chat.db.ChatLogDAO;
import com.chatproject.secure_chat.db.ChatMessage;
import com.google.gson.Gson;

import java.io.*;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.net.Socket;
import java.util.List;

/**
 * 서버 측에서 각 클라이언트 소켓에 대해 돌며
 * - JSON 메시지를 파싱
 * - type에 따라 DB 저장/히스토리 응답/키 요청/목록 응답/릴레이 등을 수행한다.
 *
 * E2EE 원칙:
 * - 서버는 평문을 보지 않음.
 * - DB에는 항상 암호문과 RSA로 암호화된 AES 키만 저장.
 * - history 응답 시 요청자(=복호화 주체)에게 "그가 풀 수 있는 aesKey"를 함께 내려줌.
 */
public class ClientMessageReader implements Runnable {

    private final Socket socket;
    private final String nickName; // 이 소켓의 사용자 닉네임
    private final PublicKey pubKey; // 이 사용자의 공개키(필요 시 사용)

    private final Gson gson = new Gson();

    public ClientMessageReader(Socket socket, String nickName, PublicKey pubKey) {
        this.socket = socket;
        this.nickName = nickName;
        this.pubKey = pubKey;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            while (true) {
                String message = br.readLine();
                if (message == null) {
                    System.out.println("🔌 연결 종료됨. 스레드 종료.");
                    break;
                }
                System.out.println("📨 수신된 메시지(raw): " + message);

                // JSON이 아닌 경우(또는 기타 프로토콜)
                if (!message.startsWith("{")) {
                    System.out.println("서버로부터 수신된 일반 메시지: " + message);
                    continue;
                }

                // JSON 파싱
                MsgFormat msg;
                try {
                    msg = gson.fromJson(message, MsgFormat.class);
                } catch (Exception e) {
                    System.out.println("❌ JSON 파싱 실패: " + message);
                    e.printStackTrace();
                    continue;
                }

                System.out.println("📥 받은 메시지 타입: " + msg.getType());

                // ----- 타입별 처리 -----
                switch (msg.getType()) {

                    /* 1) 채팅 메시지 저장 + 대상에게만 전달 (브로드캐스트 금지) */
                    case "message": {
                        try {
                            String sender = msg.getNickname();   // 보낸 사람
                            String receiver = (msg.getTargetList() != null && !msg.getTargetList().isEmpty())
                                    ? msg.getTargetList().get(0) : null; // 받는 사람
                            String ciphertext = msg.getMsg();    // AES 암호문(Base64)

                            // 클라이언트가 함께 보낸 AES 키(수신자/발신자용 RSA 암호문)
                            String aesForRecv = msg.getAesKeyForReceiver(); // 수신자용
                            String aesForSend = msg.getAesKeyForSender();   // 발신자용(없을 수도 있음)

                            String timeStamp = (msg.getTimestamp() != null)
                                    ? msg.getTimestamp()
                                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                            if (receiver == null || ciphertext == null || ciphertext.isBlank()) {
                                System.out.println("⚠️ message 필수 필드 누락: receiver/ciphertext 확인 필요");
                                break;
                            }

                            // 1) DB 저장
                            ChatLogDAO dao = new ChatLogDAO();
                            dao.insertMessage(sender, receiver, ciphertext, aesForRecv, aesForSend, timeStamp);

                            // 2) 대상(receiver)에게만 릴레이
                            synchronized (ChatServer.clientList) {
                                for (ClientInfo client : ChatServer.clientList) {
                                    if (client.getNickname().equals(receiver)) {
                                        client.getPw().println(message); // 그대로 전달 (상대는 자신의 개인키로 복호화)
                                        break;
                                    }
                                }
                            }

                            System.out.println("💾 저장 및 전달 완료: " + sender + " → " + receiver);
                        } catch (Exception e) {
                            System.out.println("❌ message 처리 실패");
                            e.printStackTrace();
                        }
                        break;
                    }

                    /* 2) 히스토리 요청 → 요청자에게만 '복호화 가능한 aesKey'를 붙여 내려줌 */
                    case "history": {
                        try {
                            String requester = msg.getNickname(); // history 요청한 사람
                            String target = (msg.getTargetList() != null && !msg.getTargetList().isEmpty())
                                    ? msg.getTargetList().get(0) : null;

                            if (requester == null || target == null) {
                                System.out.println("⚠️ history 필수 필드 누락: requester/target");
                                break;
                            }

                            ChatLogDAO dao = new ChatLogDAO();
                            List<ChatMessage> historyList = dao.getMessageBetween(requester, target);

                            PrintWriter requesterWriter = null;
                            synchronized (ChatServer.clientList) {
                                for (ClientInfo client : ChatServer.clientList) {
                                    if (client.getNickname().equals(requester)) {
                                        requesterWriter = client.getPw();
                                        break;
                                    }
                                }
                            }
                            if (requesterWriter == null) {
                                System.out.println("⚠️ 요청자 writer 없음: " + requester);
                                break;
                            }

                            // 각 레코드에 대해 요청자 기준 aesKey를 선택하여 내려보냄
                            for (ChatMessage row : historyList) {
                                MsgFormat resp = new MsgFormat();
                                resp.setType("history");
                                resp.setNickname(row.getSender());              // 누가 보냈는지
                                resp.setTargetList(List.of(row.getReceiver())); // 원래 수신자
                                resp.setMsg(row.getCiphertext());               // 암호문(Base64)
                                resp.setTimestamp(row.getTimestamp());

                                // 요청자가 receiver면 수신자용 키, 아니면 발신자용 키
                                String aesKey = requester.equals(row.getReceiver())
                                        ? row.getAesKeyForReceiver()
                                        : row.getAesKeyForSender();

                                if (aesKey == null || aesKey.isBlank()) {
                                    // 과거 데이터 호환(발신자용 키를 저장하지 않은 기록 등)
                                    System.out.println("⚠️ history aesKey 없음: req=" + requester
                                            + ", row(sender=" + row.getSender()
                                            + ", receiver=" + row.getReceiver() + ")");
                                }

                                resp.setAesKey(aesKey); // ← 클라이언트는 이 키로만 복호화 가능
                                requesterWriter.println(gson.toJson(resp));
                            }
                            System.out.println("🗂 history 전송 완료: " + requester + " ↔ " + target);
                        } catch (Exception e) {
                            System.out.println("❌ history 처리 실패");
                            e.printStackTrace();
                        }
                        break;
                    }

                    /* 3) 공개키 요청 → 현재는 원시 KEY: 응답 (추후 pubkeyResponse(JSON) 권장) */
                    case "pubkeyRequest": {
                        String target = msg.getMsg(); // 공개키를 알고 싶은 대상 닉네임
                        PublicKey key = ChatServer.publicKeyMap.get(target); // 서버가 기억하고 있는 공개키

                        if (key != null) {
                            String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());

                            synchronized (ChatServer.clientList) {
                                for (ClientInfo client : ChatServer.clientList) {
                                    if (client.getNickname().equals(msg.getNickname())) {
                                        client.getPw().println("KEY:" + encodedKey); // 원시 프로토콜 유지
                                        System.out.println("✅ " + msg.getNickname() + " 에게 공개키 전송됨");
                                        break;
                                    }
                                }
                            }
                        } else {
                            System.out.println("❌ 공개키 조회 실패: " + target);
                        }
                        break;
                    }

                    /* 4) 접속자 목록 요청 */
                    case "targetListRequest": {
                        StringBuilder sb = new StringBuilder("현재 접속자 목록:\n");
                        synchronized (ChatServer.clientList) {
                            for (ClientInfo client : ChatServer.clientList) {
                                sb.append("- ").append(client.getNickname()).append("\n");
                            }
                        }

                        MsgFormat response = new MsgFormat();
                        response.setType("targetList");
                        response.setNickname("Server");
                        response.setMsg(sb.toString());

                        writer.println(gson.toJson(response)); // 요청자에게 전송
                        break;
                    }

                    default: {
                        System.out.println("ℹ️ 처리되지 않은 타입: " + msg.getType());
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignore) {}
        }
    }
}
