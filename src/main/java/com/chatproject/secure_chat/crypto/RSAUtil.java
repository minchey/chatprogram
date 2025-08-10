package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * RSA 유틸리티
 *
 * <기능>
 * - 키쌍 생성(2048bit)
 * - 공개키로 문자열 암호화(Base64) / 개인키로 복호화
 * - 공개키/개인키 직렬화 파일 저장/로딩
 *
 * <보안 참고>
 * - 현재 변환 문자열은 "RSA/ECB/PKCS1Padding" 입니다.
 *   실제 서비스에선 OAEP 전환 권장:
 *   "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
 *   (양쪽 encrypt/decrypt 모두 동시에 바꿔야 호환됩니다)
 */
public class RSAUtil {

    /** 현재 패딩(전환 쉬움) */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    // private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"; // 권장(동시 변경 필요)

    /** 키 파일 저장 폴더 */
    private static final String PRIVATE_DIR = "PrivateKey_File";
    private static final String PUBLIC_DIR  = "PublicKey_File";

    /** 키 파일 확장자 */
    private static final String PRIVATE_EXT = ".key";
    private static final String PUBLIC_EXT  = ".pub";

    private RSAUtil() {}

    /**
     * 2048-bit RSA 키쌍 생성
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /**
     * 공개키로 문자열 암호화 후 Base64로 반환
     *
     * @param plainText 암호화할 평문(UTF-8)
     * @param publicKey 공개키
     * @return Base64 인코딩 암호문
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        if (plainText == null) {
            throw new IllegalArgumentException("RSAUtil.encrypt: plainText is null");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("RSAUtil.encrypt: publicKey is null");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Base64 암호문을 개인키로 복호화하여 문자열(UTF-8) 반환
     *
     * @param encryptedText Base64 인코딩 암호문
     * @param privateKey    개인키
     * @return 복호화된 평문(UTF-8)
     */
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("RSAUtil.decrypt: encryptedText is null/blank");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("RSAUtil.decrypt: privateKey is null");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 개인키 직렬화 저장
     *
     * @param nickName  닉네임(파일명 프리픽스)
     * @param privateKey 저장할 개인키
     */
    public static void privateKeyToFile(String nickName, PrivateKey privateKey) {
        if (nickName == null || nickName.isBlank()) {
            throw new IllegalArgumentException("privateKeyToFile: nickName is null/blank");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKeyToFile: privateKey is null");
        }

        File dir = new File(PRIVATE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("개인키 디렉토리 생성 실패: " + dir.getAbsolutePath());
        }
        File keyFile = new File(dir, nickName + PRIVATE_EXT);

        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(keyFile))) {
            oos.writeObject(privateKey);
            System.out.println("개인키 저장 완료: " + keyFile.getPath());
        } catch (Exception e) {
            System.out.println("개인키 저장 실패: " + keyFile.getPath());
            e.printStackTrace();
        }
    }

    /**
     * 개인키 로딩(직렬화 복원)
     *
     * @param nickName 닉네임(파일명 프리픽스)
     * @return PrivateKey 또는 null(실패 시)
     */
    public static PrivateKey loadPrivateKeyFromFile(String nickName) {
        if (nickName == null || nickName.isBlank()) {
            System.out.println("loadPrivateKeyFromFile: nickName is null/blank");
            return null;
        }
        File file = new File(PRIVATE_DIR, nickName + PRIVATE_EXT);
        if (!file.exists()) {
            System.out.println("개인키 파일이 존재하지 않습니다: " + file.getPath());
            return null;
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(file))) {
            PrivateKey privateKey = (PrivateKey) ois.readObject();
            System.out.println("개인키 로딩 완료: " + file.getPath());
            return privateKey;
        } catch (Exception e) {
            System.out.println("개인키 로딩 실패: " + file.getPath());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 공개키 직렬화 저장
     *
     * @param nickName 닉네임(파일명 프리픽스)
     * @param publicKey 저장할 공개키
     */
    public static void publicKeyToFile(String nickName, PublicKey publicKey) {
        if (nickName == null || nickName.isBlank()) {
            throw new IllegalArgumentException("publicKeyToFile: nickName is null/blank");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("publicKeyToFile: publicKey is null");
        }

        File dir = new File(PUBLIC_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("공개키 디렉토리 생성 실패: " + dir.getAbsolutePath());
        }
        File keyFile = new File(dir, nickName + PUBLIC_EXT);

        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(keyFile))) {
            oos.writeObject(publicKey);
            System.out.println("공개키 저장 완료: " + keyFile.getPath());
        } catch (Exception e) {
            System.out.println("공개키 저장 실패: " + keyFile.getPath());
            e.printStackTrace();
        }
    }

    /**
     * 공개키 로딩(직렬화 복원)
     *
     * @param nickName 닉네임(파일명 프리픽스)
     * @return PublicKey 또는 null(실패 시)
     */
    public static PublicKey loadPublickeyFromFile(String nickName) {
        if (nickName == null || nickName.isBlank()) {
            System.out.println("loadPublickeyFromFile: nickName is null/blank");
            return null;
        }
        File file = new File(PUBLIC_DIR, nickName + PUBLIC_EXT);
        if (!file.exists()) {
            System.out.println("공개키 파일이 존재하지 않습니다: " + file.getPath());
            return null;
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(file))) {
            PublicKey publicKey = (PublicKey) ois.readObject();
            System.out.println("공개키 로딩 완료: " + file.getPath());
            return publicKey;
        } catch (Exception e) {
            System.out.println("공개키 불러오기 실패: " + file.getPath());
            e.printStackTrace();
            return null;
        }
    }
}
