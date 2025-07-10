package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.server.ClientInfo;

import java.time.LocalDateTime;


public class MsgFormat {
    private String nickname;
    private String msg;
    private String timestamp;
    public MsgFormat(){}
    public MsgFormat(String nickname,String msg){
        this.nickname = nickname;
        this.msg = msg;
        this.timestamp = LocalDateTime.now().toString();

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
}
