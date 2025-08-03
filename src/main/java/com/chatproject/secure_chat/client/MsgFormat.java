package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.server.ClientInfo;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;


public class MsgFormat {
    private String type;
    private String nickname;
    private String msg;
    private List<String> targetList;
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

    //현재 시간
    public String getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    //aes키
    public String getAesKey(){
        return aesKey;
    }

    public void setAesKey(String aesKey){
        this.aesKey = aesKey;
    }

    //타입 지정
    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public List<String> getTargetList(){
        return targetList;
    }
    public void setTargetList(List<String> targetList){
        this.targetList = targetList;
    }
}
