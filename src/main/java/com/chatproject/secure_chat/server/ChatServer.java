package com.chatproject.secure_chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Base64;

/**
 * ChatServer
 *
 * <역할>
 * - 클라이언트 연결을 받아 닉네임/공개키 등록
 * - 전역 상태로 접속자 목록(clientList)과 공개키 맵(publicKeyMap) 유지
 * - 각 연결에 대해 ClientMessageReader 스레드 실행
 *
 * <프로토콜(초기 핸드셰이크)>
 * 1) 클라이언트가 첫 두 줄로 전송:
 *    - 1줄: nickname
 *    - 2줄: Base64(X.509) 공개키
 *
 * <운영 팁>
 * - 포트는 환경변수 SERVER_PORT로 바꿀 수 있음(기본 9999)
 * - 동일 닉네임 재접속 시 기존 연결을 정리 후 덮어씀(중복 세션 방지)
 */
public class ChatServer {

    /** 접속자 리스트(순회 시 동기화 필요) */
    public static final List<ClientInfo> clientList = Collections.synchronizedList(new ArrayList<>());

    /** 닉네임→공개키 매핑(조회/갱신 동시 안전성) */
    public static final Map<String, PublicKey> publicKeyMap = new ConcurrentHashMap<>();

    public void start() {
        final int PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "9999"));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] listen on port " + PORT + " ...");

            while (true) {
                // 1) 새 연결 수락
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] incoming connection from " + clientSocket.getRemoteSocketAddress());

                try {
                    // 2) 초기 핸드셰이크: nickname, base64 public key (UTF-8 고정)
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8);

                    String nickname = br.readLine();
                    String base64Key = br.readLine();

                    if (nickname == null || nickname.isBlank() || base64Key == null || base64Key.isBlank()) {
                        System.out.println("[SERVER] ⛔ invalid handshake: nickname/base64Key is null/blank. Closing.");
                        safeClose(clientSocket);
                        continue;
                    }

                    // 3) Base64 → X509 → PublicKey 복원
                    PublicKey publicKey;
                    try {
                        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        publicKey = keyFactory.generatePublic(spec);
                    } catch (Exception e) {
                        System.out.println("[SERVER] ⛔ public key parse failed for user=" + nickname + ": " + e.getMessage());
                        safeClose(clientSocket);
                        continue;
                    }

                    // 4) 동일 닉네임 기존 연결 제거(있다면 소켓/Writer 닫기 포함)
                    removeIfExists(nickname);

                    // 5) 상태 등록
                    publicKeyMap.put(nickname, publicKey);
                    ClientInfo clientInfo = new ClientInfo(nickname, clientSocket, publicKey); // (ClientInfo가 pw를 내부에서 생성/보관한다고 가정)
                    clientList.add(clientInfo);

                    System.out.println("[SERVER] ✅ " + nickname + " connected");
                    System.out.println("[SERVER]    pubkey=" + publicKey);

                    // 6) per-connection reader 스레드 기동
                    Thread thread = new Thread(new ClientMessageReader(clientSocket, nickname, publicKey));
                    thread.setName("ClientMessageReader-" + nickname);
                    thread.start();

                } catch (Exception e) {
                    System.out.println("[SERVER] ⛔ handshake/accept block error: " + e.getMessage());
                    // clientSocket은 finally에서 닫지 않음(정상 스레드가 이어받을 수 있음). 여기선 즉시 닫아도 무방.
                    safeClose(clientSocket);
                }
            }

        } catch (Exception e) {
            System.out.println("[SERVER] ⛔ server loop error");
            e.printStackTrace();
        }
    }

    /**
     * 같은 닉네임의 기존 연결이 있다면 리스트에서 제거하고 소켓을 안전하게 닫는다.
     */
    private void removeIfExists(String nickname) {
        synchronized (clientList) {
            Iterator<ClientInfo> it = clientList.iterator();
            while (it.hasNext()) {
                ClientInfo c = it.next();
                if (nickname.equals(c.getNickname())) {
                    System.out.println("[SERVER] ℹ️ duplicate login. closing old session of " + nickname);
                    safeClose(c.getSocket());
                    it.remove();
                }
            }
        }
    }

    /** 소켓 안전 종료 */
    private void safeClose(Socket s) {
        if (s == null) return;
        try { s.close(); } catch (Exception ignore) {}
    }
}
