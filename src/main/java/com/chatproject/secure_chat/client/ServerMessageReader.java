package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.google.gson.Gson;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ServerMessageReader
 *
 * <역할>
 * - 서버에서 오는 라인 기반 메시지를 수신
 * - KEY: (상대 공개키), JSON 메시지(MsgFormat) 등을 분기 처리
 * - "message" / "history"는 RSA→AES 순으로 복호화하여 출력/저장
 *
 * <프로토콜 요약>
 * - KEY:<base64 X509>  : 상대 공개키 전달
 * - JSON(MsgFormat)   : type에 따라 분기
 *   - message: msg=AES Base64, aesKey=내 공개키로 RSA 암호화된 AES 키
 *   - history: msg=AES Base64, aesKey=요청자 기준으로 선택된 RSA 암호문 키
 *
 * <주의>
 * - aesKey가 null/blank면 복호화 시도하지 않고 안내 로그/placeholder 저장
 * - UTF-8 고정
 */
public class ServerMessageReader implements Runnable {

    private final Socket socket;
    private volatile PublicKey otherPublicKey; // 상대 공개키
    private final PrivateKey privateKey;       // 내 개인키(RSA 복호화)
    private final PrintWriter printWriter;     // 서버로 응답 필요 시 사용
    private final String nickName;             // 내 닉네임

    public final List<MsgFormat> receivedMsg = new ArrayList<>();

    public ServerMessageReader(Socket socket, PrivateKey privateKey, PrintWriter printWriter, String nickName) {
        this.socket = socket;
        this.privateKey = privateKey;
        this.printWriter = printWriter;
        this.nickName = nickName;
    }

    public PublicKey getOtherPublicKey() {
        return otherPublicKey;
    }
    public void setOtherPublicKey(PublicKey otherPublicKey) {
        this.otherPublicKey = otherPublicKey;
    }

    @Override
    public void run() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Gson gson = new Gson();

            while (true) {
                String line = br.readLine();
                if (line == null) {
                    System.out.println("🔌 서버 연결 종료 (readLine=null).");
                    break;
                }
                if ("종료".equals(line)) {
                    System.out.println("🔚 종료 신호 수신.");
                    break;
                }

                // 공개키 수신
                if (line.startsWith("KEY:")) {
                    try {
                        String keyString = line.substring(4);
                        byte[] keyBytes = Base64.getDecoder().decode(keyString);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PublicKey receivedKey = keyFactory.generatePublic(spec);
                        this.setOtherPublicKey(receivedKey);
                        System.out.println("📩 공개키 수신 완료.");
                    } catch (Exception e) {
                        System.out.println("❌ 공개키 파싱 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                    continue;
                }

                // 서버 오류 라인
                if (line.startsWith("ERROR:")) {
                    System.out.println("❌ 서버 오류: " + line.substring(6));
                    continue;
                }

                // JSON 메시지
                if (line.startsWith("{")) {
                    MsgFormat msgFormat;
                    try {
                        msgFormat = gson.fromJson(line, MsgFormat.class);
                    } catch (Exception e) {
                        System.out.println("❌ JSON 파싱 실패: " + line);
                        e.printStackTrace();
                        continue;
                    }

                    String type = msgFormat.getType();
                    System.out.println("📦 msgFormat.type = " + type);

                    if ("message".equals(type)) {
                        // 실시간 수신: aesKey(내 공개키로 RSA 암호화된 AES 키)가 온다고 가정
                        try {
                            String encAesKey = msgFormat.getAesKey();
                            if (encAesKey == null || encAesKey.isBlank()) {
                                System.out.println("⚠️ message aesKey 없음 → 복호화 스킵");
                                System.out.println(msgFormat.getNickname() + ": " + "[암호문 수신, 키 누락]");
                                continue;
                            }

                            String aesKeyBase64 = RSAUtil.decrypt(encAesKey, privateKey);
                            byte[] decodedKey = Base64.getDecoder().decode(aesKeyBase64);
                            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                            String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey);
                            System.out.println(msgFormat.getNickname() + ": " + decryptedMsg);
                        } catch (Exception e) {
                            System.out.println("❌ message 복호화 실패: from=" + msgFormat.getNickname());
                            e.printStackTrace();
                        }
                        continue;
                    }

                    if ("targetList".equals(type)) {
                        System.out.println(msgFormat.getMsg());
                        continue;
                    }

                    if ("history".equals(type)) {
                        try {
                            String from = msgFormat.getNickname();
                            String encAesKey = msgFormat.getAesKey();   // 요청자 기준 키
                            String cipher = msgFormat.getMsg();

                            if (encAesKey == null || encAesKey.isBlank()) {
                                boolean sentByMe = nickName.equals(from);
                                String note = sentByMe
                                        ? "내가 보낸 메시지(요청자용 AES 키 없음) → 내가 복호화 불가"
                                        : "상대가 보낸 메시지(aesKey 누락) → 서버 응답/DB 키 매핑 점검 필요";

                                System.out.println(
                                        "⚠️ history aesKey 없음 | ts=" + msgFormat.getTimestamp()
                                                + " | from=" + from
                                                + " | sentByMe=" + sentByMe
                                                + " | cipherLen=" + (cipher == null ? "null" : cipher.length())
                                );

                                MsgFormat placeholder = new MsgFormat();
                                placeholder.setNickname(from);
                                placeholder.setMsg("[" + note + "]");
                                placeholder.setTargetList(List.of(nickName));
                                placeholder.setType("history");
                                placeholder.setTimestamp(msgFormat.getTimestamp());
                                receivedMsg.add(placeholder);
                                continue;
                            }

                            String aesKeyBase64 = RSAUtil.decrypt(encAesKey, privateKey);
                            if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
                                throw new IllegalStateException("RSA 복호화 결과(aesKeyBase64)가 비어있음");
                            }

                            byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
                            SecretKeySpec secretKey2 = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");

                            String plainText = AESUtil.decrypt(cipher, secretKey2);

                            MsgFormat decrypted = new MsgFormat();
                            decrypted.setNickname(from);
                            decrypted.setMsg(plainText);
                            decrypted.setTargetList(List.of(nickName));
                            decrypted.setType("history");
                            decrypted.setTimestamp(msgFormat.getTimestamp());

                            receivedMsg.add(decrypted);
                            System.out.println("[" + decrypted.getTimestamp() + "] "
                                    + decrypted.getNickname() + ": " + decrypted.getMsg());
                        } catch (Exception e) {
                            System.out.println("❌ history 복호화 실패: ts=" + msgFormat.getTimestamp()
                                    + ", from=" + msgFormat.getNickname()
                                    + ", aesKeyLen=" + (msgFormat.getAesKey() == null ? "null" : msgFormat.getAesKey().length()));
                            e.printStackTrace();
                        }
                        continue;
                    }

                    if ("pubkeyRequest".equals(type)) {
                        // (참고) 이 분기는 "다른 누군가가 내 키를 요청했다"는 서버 알림에 대응하는 용도
                        String requester = msgFormat.getNickname();
                        System.out.println("🔐 [" + requester + "] 님이 당신의 공개키를 요청했습니다.");

                        PublicKey myPubKey = RSAUtil.loadPublickeyFromFile(nickName);
                        if (myPubKey == null) {
                            System.out.println("⚠️ 내 공개키 파일을 찾을 수 없어 응답 불가");
                            continue;
                        }
                        String encodedKey = Base64.getEncoder().encodeToString(myPubKey.getEncoded());
                        printWriter.println("KEY:" + encodedKey);
                        printWriter.flush();
                        continue;
                    }

                    // 기타 타입
                    System.out.println("📨 시스템 메시지: " + msgFormat.getMsg());
                    continue;
                }

                // JSON/KEY/ERROR가 아닌 일반 텍스트
                System.out.println("💬 일반 수신: " + line);
            }
        } catch (Exception e) {
            System.out.println("❌ ServerMessageReader 루프 예외");
            e.printStackTrace();
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignore) {}
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
