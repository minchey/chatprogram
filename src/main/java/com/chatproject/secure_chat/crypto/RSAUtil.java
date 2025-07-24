package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileOutputStream;

/*
 * RSA 암호화를 위한 유틸리티 클래스
 * - 공개키/개인키 생성
 * - 암호화 및 복호화 기능 제공
 */

public class RSAUtil {
    private static KeyPair keyPair;     // RSA 키 쌍(KeyPair: 공개키 + 개인키)을 저장할 static 변수

    static {    // 클래스 로딩 시 키쌍을 자동으로 생성
        try {
            keyPair = generateKeyPair(); // 공개키/개인키 자동으로 생성
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static KeyPair generateKeyPair() throws Exception { //키 쌍 생성
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); // 보안 강도를 위한 2048비트 키 설정
        return generator.generateKeyPair();
    }

    public static PublicKey getPublicKey() { //공개키 가져오기
        return keyPair.getPublic();
    }

    public static PrivateKey getPrivateKey() {//개인키 가져오기
        return keyPair.getPrivate();
    }

    /**
     * 🔐 문자열을 RSA 공개키로 암호화
     *
     * @param plainText 평문 문자열
     * @param publicKey 암호화에 사용할 공개키
     * @return Base64 인코딩된 암호문
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); //암호화 알고리즘 설정
        cipher.init(Cipher.ENCRYPT_MODE, publicKey); //공개키로 암호화 모드 설정
        byte[] encrypted = cipher.doFinal(plainText.getBytes()); // 평문 암호화
        return Base64.getEncoder().encodeToString(encrypted); // 암호문을 Base64로 인코딩
    }

    /**
     * 🔓 암호문을 RSA 개인키로 복호화
     *
     * @param encryptedText Base64 인코딩된 암호문
     * @param privateKey    복호화에 사용할 개인키
     * @return 복호화된 평문 문자열
     */
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // 암호화 알고리즘 설정
        cipher.init(Cipher.DECRYPT_MODE, privateKey); // 개인키로 복호화 모드 설정
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8); //바이트를 문자열로 변환

    }

    public static void privateKeyToFile(String nickName, PrivateKey privateKey) {
        try {
            String fileName = nickName + ".key";
            File dir = new File("PrivateKey_File");
            if (!dir.exists()) dir.mkdir();
            File key_File = new File(dir, fileName);

            //FileOutputStream을 통해 파일로 바이트 저장
            FileOutputStream fos = new FileOutputStream(key_File);//파일에 저장해주는 객체
            ObjectOutputStream oos = new ObjectOutputStream(fos); //개인키 직렬화 해주는 객체
            oos.writeObject(privateKey); //privateKey 직렬화 해서 저장
            System.out.println("개인키 저장완: " + key_File.getPath());
        } catch (Exception e) {
            System.out.println("개인키 저장 실패");
            e.printStackTrace();
        }
    }
}

