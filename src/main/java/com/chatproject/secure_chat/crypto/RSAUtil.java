package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

public class RSAUtil {
    public static KeyPair generateKeyPair() throws Exception{ //키 쌍 생성
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
