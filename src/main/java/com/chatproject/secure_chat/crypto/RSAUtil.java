package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;


public class RSAUtil {
    private static KeyPair keyPair;

    static {
        try{
            keyPair = generateKeyPair();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static KeyPair generateKeyPair() throws Exception{ //키 쌍 생성
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
    public static PublicKey getPublicKey(){ //공개키 가져오기
        return keyPair.getPublic();
    }
    public static PrivateKey getPrivateKey(){//개인키 가져오기
        return keyPair.getPrivate();
    }
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);

    }
}
