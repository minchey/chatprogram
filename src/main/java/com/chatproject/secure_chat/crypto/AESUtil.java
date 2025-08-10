package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 유틸리티 (IV 없이 동작하는 ECB 버전)
 *
 * <주의>
 * - 현재 모드는 "AES/ECB/PKCS5Padding" 입니다. ECB는 보안상 권장되지 않으며,
 *   실제 서비스에서는 CBC 혹은 GCM(권장)을 사용하는 것이 안전합니다.
 * - 프로젝트 요구로 IV 없는 버전을 유지해야 한다면, 평문 패턴 노출 위험을 인지하세요.
 *
 * <입출력 규약>
 * - encrypt(plainText, key)  → Base64 문자열(암호문)
 * - decrypt(cipherBase64, key) → 평문 문자열
 * - 문자 인코딩은 항상 UTF-8을 사용합니다.
 */
public final class AESUtil {

    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private AESUtil() {} // 유틸 클래스: 인스턴스화 금지

    /**
     * 평문을 AES(ECB/PKCS5Padding)로 암호화한 후 Base64로 인코딩하여 반환한다.
     *
     * @param plainText 암호화할 평문(UTF-8)
     * @param key       대칭키(SecretKey)
     * @return Base64 인코딩된 암호문
     * @throws Exception 유효하지 않은 키/입력, 암호화 오류 등
     */
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        if (plainText == null) {
            throw new IllegalArgumentException("AESUtil.encrypt: plainText is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("AESUtil.encrypt: key is null");
        }

        SecretKeySpec secretKey = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Base64 인코딩된 암호문을 AES(ECB/PKCS5Padding)로 복호화하여 평문(UTF-8)으로 반환한다.
     *
     * @param encryptedBase64 Base64 인코딩된 암호문
     * @param key             대칭키(SecretKey)
     * @return 복호화된 평문(UTF-8)
     * @throws Exception 유효하지 않은 키/입력, 복호화 오류 등
     */
    public static String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new IllegalArgumentException("AESUtil.decrypt: encryptedBase64 is null/blank");
        }
        if (key == null) {
            throw new IllegalArgumentException("AESUtil.decrypt: key is null");
        }

        SecretKeySpec secretKey = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
