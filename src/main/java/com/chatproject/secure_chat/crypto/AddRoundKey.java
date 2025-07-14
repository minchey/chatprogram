package com.chatproject.secure_chat.crypto;

public class AddRoundKey {

    public byte[][] rounding(byte[][] input, byte[][] roundKey){
        byte[][] result = new byte[4][4];
        for(int row = 0 ; row < 4; row++){
            for(int col = 0; col <4; col++){
                result[row][col] = (byte) (input[row][col] ^ roundKey[row][col]);
            }
        }
        return result;
    }
}
