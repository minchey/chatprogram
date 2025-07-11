package com.chatproject.secure_chat.crypto;

public class SubBytes {
    public static byte[][] applySubBytes(byte[][] input){
        byte[][] result = new byte[4][4];
        for(int col = 0 ; col < 4 ; col++){
            for(int row = 0 ; row < 4 ; row++){
                result[row][col] = SBox.block[input[row][col] & 0xFF];
            }
        }
        return result;
    }

}
