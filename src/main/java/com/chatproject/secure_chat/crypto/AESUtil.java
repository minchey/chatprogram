package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

public class AESUtil {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding"; //암호화 알고리즘 / 모드 / 패딩 순서

    public static String encrypt(String plainText, SecretKey key) throws Exception{
        SecretKeySpec secretKey = new SecretKeySpec(key.getEncoded(),"AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted); //보기좋게 만들기
    }

    public static String decrypt(String encryptedText, SecretKey key) throws Exception{
        SecretKeySpec secretKey = new SecretKeySpec(key.getEncoded(),"AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE,key);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }
}
