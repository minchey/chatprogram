package com.chatproject.secure_chat.crypto;

public class KeyExpantion {
    public static byte[][][] generateRoundKeys(byte [] key){
        byte[][] word = new byte[44][4];

        for(int i = 0; i<4; i++){
            word[i][0] = key[i*4];
            word[i][1] = key[i*4+1];
            word[i][2] = key[i*4+2];
            word[i][3] = key[i*4+3];
        };

    }
}
