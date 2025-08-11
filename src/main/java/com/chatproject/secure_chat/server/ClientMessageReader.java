package com.chatproject.secure_chat.server;

import com.chatproject.secure_chat.client.MsgFormat;
import com.chatproject.secure_chat.db.ChatLogDAO;
import com.chatproject.secure_chat.db.ChatMessage;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 📌 ClientMessageReader
 * - 서버에서 "각 클라이언트 소켓"별로 동작하는 Reader 스레드
 * - 클라이언트가 보낸 JSON 기반 메시지를 타입에 따라 처리
 *
 * 주요 기능:
 * 1) message         → DB 저장 후 대상 클라이언트에게만 릴레이
 * 2) history         → 두 사용자의 과거 메시지 조회 후 요청자에게만 전송
 * 3) pubkeyRequest   → 특정 사용자의 공개키 반환
 * 4) targetListRequest → 현재 접속자 목록 반환
 *
 * ⚠ E2EE 구조
 * - 서버는 평문을 보지 않고, 암호문과 암호화된 AES 키만 저장
 * - history 응답 시 요청자가 복호화 가능한 AES 키를 함께 내려줌
 */
public class ClientMessageReader implements Runnable {

    private final Socket socket;
    private final String nickName;
    private final PublicKey pubKey;
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
                    System.out.println("🔌 연결 종료됨 (" + nickName + ")");
                    break;
                }

                // JSON 포맷 체크
                if (!message.startsWith("{")) {
                    System.out.println("📩 일반 메시지 수신: " + message);
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

                System.out.println("📥 [" + nickName + "] 타입=" + msg.getType());

                switch (msg.getType()) {

                    // 1) 채팅 메시지 저장 및 릴레이
                    case "message": {
                        handleMessage(msg, message);
                        break;
                    }

                    // 2) 히스토리 요청 처리
                    case "history": {
                        handleHistory(msg);
                        break;
                    }

                    // 3) 공개키 요청 처리
                    case "pubkeyRequest": {
                        handlePubkeyRequest(msg);
                        break;
                    }

                    // 4) 접속자 목록 요청 처리
                    case "targetListRequest": {
                        handleTargetListRequest(writer);
                        break;
                    }

                    default:
                        System.out.println("ℹ️ 처리되지 않은 타입: " + msg.getType());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignore) {}
        }
    }

    /** 📌 message 타입 처리 */
    private void handleMessage(MsgFormat msg, String rawMessage) {
        try {
            String sender = msg.getNickname();
            String receiver = (msg.getTargetList() != null && !msg.getTargetList().isEmpty())
                    ? msg.getTargetList().get(0) : null;
            String ciphertext = msg.getMsg();

            String aesForRecv = msg.getAesKeyForReceiver();
            String aesForSend = msg.getAesKeyForSender();

            String timeStamp = (msg.getTimestamp() != null)
                    ? msg.getTimestamp()
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            if (receiver == null || ciphertext == null || ciphertext.isBlank()) {
                System.out.println("⚠️ message 필수 값 누락 (receiver/ciphertext)");
                return;
            }

            // DB 저장
            ChatLogDAO dao = new ChatLogDAO();
            dao.insertMessage(sender, receiver, ciphertext, aesForRecv, aesForSend, timeStamp);

            // 대상에게 전달
            synchronized (ChatServer.clientList) {
                for (ClientInfo client : ChatServer.clientList) {
                    if (client.getNickname().equals(receiver)) {
                        client.getPw().println(rawMessage);
                        break;
                    }
                }
            }

            System.out.println("💾 저장 및 전달 완료: " + sender + " → " + receiver);
        } catch (Exception e) {
            System.out.println("❌ message 처리 실패");
            e.printStackTrace();
        }
    }

    /** 📌 history 타입 처리 */
    private void handleHistory(MsgFormat msg) {
        try {
            String requester = msg.getNickname();
            String target = (msg.getTargetList() != null && !msg.getTargetList().isEmpty())
                    ? msg.getTargetList().get(0) : null;

            if (requester == null || target == null) {
                System.out.println("⚠️ history 필수 값 누락");
                return;
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
                return;
            }

            for (ChatMessage row : historyList) {
                MsgFormat resp = new MsgFormat();
                resp.setType("history");
                resp.setNickname(row.getSender());
                resp.setTargetList(List.of(row.getReceiver()));
                resp.setMsg(row.getCiphertext());
                resp.setTimestamp(row.getTimestamp());

                // 요청자가 수신자면 수신자용 키, 아니면 발신자용 키
                String aesKey = requester.equals(row.getReceiver())
                        ? row.getAesKeyForReceiver()
                        : row.getAesKeyForSender();

                if (aesKey == null || aesKey.isBlank()) {
                    System.out.println("⚠️ history aesKey 없음 | req=" + requester +
                            " | from=" + row.getSender());
                }

                resp.setAesKey(aesKey);
                requesterWriter.println(gson.toJson(resp));
            }

            System.out.println("🗂 history 전송 완료: " + requester + " ↔ " + target);
        } catch (Exception e) {
            System.out.println("❌ history 처리 실패");
            e.printStackTrace();
        }
    }

    /** 📌 pubkeyRequest 타입 처리 */
    private void handlePubkeyRequest(MsgFormat msg) {
        String target = msg.getMsg();
        PublicKey key = ChatServer.publicKeyMap.get(target);

        if (key != null) {
            String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
            synchronized (ChatServer.clientList) {
                for (ClientInfo client : ChatServer.clientList) {
                    if (client.getNickname().equals(msg.getNickname())) {
                        client.getPw().println("KEY:" + encodedKey);
                        System.out.println("✅ 공개키 전송: " + msg.getNickname());
                        break;
                    }
                }
            }
        } else {
            System.out.println("❌ 공개키 없음: " + target);
        }
    }

    /** 📌 targetListRequest 타입 처리 */
    private void handleTargetListRequest(PrintWriter writer) {
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

        writer.println(gson.toJson(response));
    }
}
