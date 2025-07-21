package com.chatproject.secure_chat.auth;

import java.io.*;
import java.security.MessageDigest;

public class UserAuth {
    private static final String USER_FILE = "users.txt";

    //회원가입 중복확인 후 파일에 저장
    public static boolean registerUser(String eMail, String nickname, String password){
        try {
            File file = new File(USER_FILE); //파일 객체 생성
            if(!file.exists()) file.createNewFile(); //파일 없으면 새로 생성 (최초동작)

            //비밀번호 해시화
            String userPassWord = hashPassword(password);


            FileWriter fw = new FileWriter(USER_FILE, true); //true 이어쓰기
            BufferedWriter bw =new BufferedWriter(fw);
            bw.write(eMail + ":" + userPassWord + ":" + nickname); //txt에 들어갈 데이터 포맷
            bw.newLine(); //줄바꿈 추가
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean loginUser(String eMail, String passWord){
        try {
            File file = new File(USER_FILE);
            if(!file.exists()) return false;
            String userPassword = hashPassword(passWord);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null){
                String[] parts = line.split(":");
                if(parts.length ==3) {
                    String storedEmail = parts[0];
                    String storedPassword = parts[1];
                    if(storedEmail.equals(eMail) && storedPassword.equals(userPassword)){
                        br.close();
                        return true;
                    }
                }
            }
            br.close();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String hashPassword(String passWord) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(passWord.getBytes());

            //바이트를 문자열로 변환
            StringBuilder sb = new StringBuilder(); //string 이어 붙이기 좋은 클래스
            for (byte b : hashBytes) { //hashBytes 반복 할때마다 b는 각 값이 됨
                sb.append(String.format("%02x", b)); //16진수로 변환
            }
            return sb.toString(); //최종 문자열로 변환
        }catch (Exception e){
            e.printStackTrace();
            return  null;
        }
    }
    public static String getNicknameFromUserFile(String email){
        try {

            File file = new File(USER_FILE);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] findNickname = line.split(":");
                if (findNickname.length == 3) {
                    if (findNickname[0].equals(email)) {
                        br.close();
                        return findNickname[2];
                    }
                }
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
