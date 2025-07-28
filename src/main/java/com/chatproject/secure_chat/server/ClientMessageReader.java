package com.chatproject.secure_chat.server; //서버스레드

import com.chatproject.secure_chat.client.MsgFormat;
import com.google.gson.Gson;

import java.io.*;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.net.Socket;

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
                System.out.println("📨 수신된 메시지(raw): " + message);


                if (message.startsWith("{")) {
                    try {
                        MsgFormat msg = gson.fromJson(message, MsgFormat.class);

                        //로그 저장
                        if("message".equals(msg.getType())) {
                            ClientInfo clientInfo;
                            String sender = msg.getNickname(); //보낸사람
                            String receiver = msg.getTargetList().get(0); //현재 스레드에서 처리중인 사용자
                            System.out.println("👥 로그 저장 대상: sender=" + sender + ", receiver=" + receiver);

                            saveLog(sender, receiver, message);
                        }
                        //복호화 메시지 상대에게 전달
                        if ("history".equals(msg.getType())) {
                            String targetNickname = msg.getTargetList().get(0); //전달 대상
                            synchronized (ChatServer.clientList) {
                                for (ClientInfo client : ChatServer.clientList) {
                                    if (client.getNickname().equals(targetNickname)) {
                                        // 🔸 timestamp가 없을 때만 현재시간으로 대체
                                        if (msg.getTimestamp() == null) {
                                            msg.setTimestamp(LocalDateTime.now().toString());
                                        }

                                        PrintWriter pw = client.getPw();
                                        pw.println(gson.toJson(msg)); // 복호화된 메시지 전달
                                        System.out.println("📤 복호화된 메시지를 " + targetNickname + " 에게 전송함");
                                        break;
                                    }
                                }
                            }
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
                        if("targetListRequest".equals(msg.getType())){
                            StringBuilder sb = new StringBuilder();
                            sb.append("현재 접속자 목록: \n");

                            synchronized (ChatServer.clientList){
                                for(ClientInfo client : ChatServer.clientList){
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
                }

                else {
                    System.out.println("서버로부터 수신된 일반 메시지: " + message);
                }

            }
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void saveLog(String sender,String receiver, String jsonMessage){
        try {

            String[] names = {sender,receiver}; //닉네임 배열로 받기
            Arrays.sort(names); //닉네임 정렬
            String fileName = names[0] + "&" + names[1] + ".log";

            File dir = new File("Message_Logs");
            if(!dir.exists()) dir.mkdirs();


            File logFile = new File (dir,fileName);

            try {
                FileWriter fw = new FileWriter(logFile,true); //logFile에 글을 쓰기 위해 통로 열어두기 true면 이어쓰기 false면 덮어쓰기
                BufferedWriter bw = new BufferedWriter(fw); //버퍼를 하나 더 두고 효율증가 잠시 메모리에 뒀다가 한번에 작성

                LocalDateTime now = LocalDateTime.now(); //현재시간
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timeStamp = now.format(formatter);

                bw.write("[" + timeStamp + "] " + sender + " → " + receiver + ":" + jsonMessage);
                bw.newLine();
                bw.flush(); //버퍼에 남아있는 내용들 강제 기록
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
