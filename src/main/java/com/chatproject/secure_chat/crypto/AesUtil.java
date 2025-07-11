package com.chatproject.secure_chat.crypto;

import java.util.List;
import java.util.ArrayList;

public class AesUtil {
    public static List<StateMatrix> converToBlocks(byte[] input){
        List<StateMatrix> blocks = new ArrayList<>();
        for(int i =0 ; i < input.length ; i+= 16){
            byte[] block = new byte[16];
            for(int j = 0 ; j < 16 ; j++){
                if(i + j < input.length) {
                    block[j] = input[i + j];
                }
                else {
                    block[j] = 0x00;
                }
            }
            blocks.add(new StateMatrix(block));
        }
        return blocks;
    }
}
