package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.server.ClientInfo;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;


public class MsgFormat {
    private String nickname;
    private String msg;
    private String timestamp;
    private String aesKey;
    public MsgFormat(){}
    public MsgFormat(String nickname,String msg, String aesKey){
        this.nickname = nickname;
        this.msg = msg;
        this.timestamp = LocalDateTime.now().toString();
        this.aesKey = aesKey;

    }


    public String getNickname(){
        return nickname;
    }
    public void setNickname(String nickname){
        this.nickname = nickname;
    }

    public String getMsg(){
        return msg;
    }
    public void setMsg(String msg){
        this.msg = msg;
    }

    public String getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public String getAesKey(){
        return aesKey;
    }
}
