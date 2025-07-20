package com.chatproject.secure_chat.auth;

import java.io.*;

public class UserAuth {
    private static final String USER_FILE = "users.txt";

    //회원가입 중복확인 후 파일에 저장
    public static boolean registerUser(String eMail, String nickname, String password){
        try {
            File file = new File(USER_FILE); //파일 객체 생성
            if(!file.exists()) file.createNewFile(); //파일 없으면 새로 생성 (최초동작)

            FileWriter fw = new FileWriter(USER_FILE, true); //true 이어쓰기
            BufferedWriter bw =new BufferedWriter(fw);
            bw.write(eMail + ":" + password + ":" + nickname); //txt에 들어갈 데이터 포맷
            bw.newLine(); //줄바꿈 추가
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
