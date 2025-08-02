package com.chatproject.secure_chat.server; //서버스레드

import com.chatproject.secure_chat.client.MsgFormat;
import com.chatproject.secure_chat.db.ChatLogDAO;

import com.chatproject.secure_chat.db.ChatMessage;
import com.google.gson.Gson;

import java.io.*;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.net.Socket;
import java.util.List;

public class ClientMessageReader implements Runnable {

    private Socket socket;
    private String nickName;
    private PublicKey pubKey;

    public ClientMessageReader(Socket socket, String nickName, PublicKey pubKey) {
        this.socket = socket;
        this.nickName = nickName;
        this.pubKey = pubKey;
    }

    Gson gson = new Gson();

    @Override
    public void run() {
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);


            while (true) {
                String message = br.readLine();
                if (message == null) {
                    System.out.println("🔌 연결 종료됨. 스레드 종료.");
                    break; // 루프 빠져나오기
                }
                System.out.println("📨 수신된 메시지(raw): " + message);


                if (message.startsWith("{")) {
                    try {
                        MsgFormat msg = gson.fromJson(message, MsgFormat.class);

                        //로그 저장
                        if ("message".equals(msg.getType())) {
                            String sender = msg.getNickname(); // 보낸사람
                            String receiver = msg.getTargetList().get(0); // 받는사람
                            String encryptedMsg = msg.getMsg(); // 암호문
                            String timestamp = msg.getTimestamp(); // 시간
                            if (timestamp == null) {
                                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            }

                            System.out.println(" 로그 저장 대상: sender=" + sender + ", receiver=" + receiver);
                            ChatLogDAO dao = new ChatLogDAO();
                            dao.insertMessage(sender, receiver, encryptedMsg, timestamp);

                        }
                        //복호화 메시지 상대에게 전달
                        if ("history".equals(msg.getType())) {
                            String targetNickname = msg.getTargetList().get(0); //전달 대상
                            String requester = msg.getNickname();
                            ChatLogDAO dao = new ChatLogDAO();
                            List<ChatMessage> historyList = dao.getMessageBetween(requester, targetNickname);
                            for (ChatMessage historyMsg : historyList) {
                                MsgFormat response = new MsgFormat();
                                response.setType("history");
                                response.setNickname(historyMsg.getSender());
                                response.setTargetList(List.of(historyMsg.getReceiver()));
                                response.setMsg(historyMsg.getMessage());
                                response.setTimestamp(historyMsg.getTimestamp());

                                synchronized (ChatServer.clientList) {
                                    for (ClientInfo client : ChatServer.clientList) {
                                        if (client.getNickname().equals(requester)) {
                                            client.getPw().println(gson.toJson(response));
                                            break;
                                        }
                                    }
                                }
                            }
                            continue;
                        }


                        // 메시지 종료 검사
                        if ("종료".equals(msg.getMsg())) break;

                        // 🔐 공개키 요청 처리
                        if ("pubkeyRequest".equals(msg.getType())) {
                            String target = msg.getMsg(); // 요청 대상 닉네임
                            PublicKey key = ChatServer.publicKeyMap.get(target); // 공개키 조회

                            if (key != null) {
                                String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());

                                PrintWriter requesterWriter = null;

                                synchronized (ChatServer.clientList) {
                                    for (ClientInfo client : ChatServer.clientList) {
                                        if (client.getNickname().equals(msg.getNickname())) {
                                            requesterWriter = client.getPw(); // 요청자에게 전송할 writer
                                            break;
                                        }
                                    }
                                }

                                if (requesterWriter != null) {
                                    requesterWriter.println("KEY:" + encodedKey);
                                    System.out.println("✅ " + msg.getNickname() + " 에게 공개키 전송됨");
                                }

                            } else {
                                System.out.println("❌ 공개키 조회 실패: " + target);
                            }

                            continue;
                        }

                        //list 응답 전송
                        if ("targetListRequest".equals(msg.getType())) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("현재 접속자 목록: \n");

                            synchronized (ChatServer.clientList) {
                                for (ClientInfo client : ChatServer.clientList) {
                                    sb.append("- ").append(client.getNickname()).append("\n");
                                }
                            }

                            MsgFormat response = new MsgFormat();
                            response.setType("targetList");
                            response.setNickname("Server");
                            response.setMsg(sb.toString());

                            writer.println(gson.toJson(response)); //Json형식으로 전송
                            continue;
                        }

                        synchronized (ChatServer.clientList) {
                            for (ClientInfo client : ChatServer.clientList) {
                                if (!client.getSocket().equals(this.socket)) {
                                    System.out.println("📤 → " + client.getNickname() + "에게 전달");

                                    client.getPw().println(message);
                                }
                            }
                        }

                        System.out.println(nickName + ": " + msg.getMsg());
                    } catch (Exception e) {
                        System.out.println("❌ JSON 파싱 실패: " + message);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("서버로부터 수신된 일반 메시지: " + message);
                }

            }
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
