package com.chatproject.secure_chat.crypto;

public class AESTest {
    public static void main(String[] args) {
        try {
            String key = "MySecretAESKey12"; // 16바이트 키
            String message = "Hello AES World!";

            String encrypted = AESUtil.encrypt(message, key);
            System.out.println("🔐 Encrypted: " + encrypted);

            String decrypted = AESUtil.decrypt(encrypted, key);
            System.out.println("🔓 Decrypted: " + decrypted);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
