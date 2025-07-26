package com.chatproject.secure_chat.crypto;

import javax.crypto.Cipher;
import java.io.*;
import java.security.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
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
            System.out.println("개인키 저장완료: " + key_File.getPath());
            oos.close();
        } catch (Exception e) {
            System.out.println("개인키 저장 실패");
            e.printStackTrace();
        }
    }

    //저장한 개인키 파일을 불러와 복원 하는 매서드
    public static PrivateKey loadPrivateKeyFromFile(String nickName){
        try{
            //개인키 저장된 파일 경로 지정
            File file = new File("PrivateKey_File", nickName + ".key");
            if(!file.exists()){ //개인키 파일 없을 시에 null 반환
                System.out.println("개인키 파일이 존재하지 않습니다.");
                return null;
            }
            FileInputStream fis = new FileInputStream(file); //파일 읽어주는 스트림
            ObjectInputStream ois = new ObjectInputStream(fis);
            PrivateKey privateKey = (PrivateKey) ois.readObject(); //직렬화 된 privatekey 객체 복원
            ois.close();

            System.out.println("개인키 로딩 완료" + file.getPath());
            return privateKey;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    //공개키 저장매서드
    public static void publicKeyToFile(String nickName, PublicKey publicKey){
        try{
            String fileName = nickName + ".pub"; //확장자는 pub
            File dir = new File("PublicKey_File");
            if(!dir.exists()) dir.mkdir();
            File keyFile = new File(dir,fileName);

            FileOutputStream fos = new FileOutputStream(keyFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(publicKey); //객체 직렬화 해서 저장
            System.out.println("공개키 저장 완료: " + fileName);
            oos.close();
        }catch (Exception e){
            System.out.println("공개키 저장실패");
            e.printStackTrace();
        }
    }

    //저장된 공개키 불러오는 매서드
    public static PublicKey loadPublickeyFromFile(String nickName){
        try{
            //공개키 저장된 파일 경로지정
            File file = new File("PublicKey_File", nickName + ".pub");
            if(!file.exists()){
                System.out.println("파일이 존재하지 않습니다.");
                return null;
            }
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            PublicKey publicKey = (PublicKey) ois.readObject();
            ois.close();

            System.out.println("공개키 복원 성공" + file.getPath());
            return publicKey;
        }catch (Exception e){
            System.out.println("공개키 불러오기가 실패하였습니다.");
            e.printStackTrace();
            return null;
        }
    }
}

