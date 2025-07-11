package com.chatproject.secure_chat.crypto;

public class StateMatrix { //행렬 만들기
    private byte[][] state = new byte[4][4];

    public StateMatrix(byte[] input){
        for(int i = 0 ; i < 16 ; i++){
            state[i%4][i/4] = input[i];
        }
    }
    public byte[][] getState(){
        return state;
    }
}
