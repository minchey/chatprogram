package com.chatproject.secure_chat.crypto;

public class MixColumns {
    public static final byte[][] mixMatrix = {
            {0x02, 0x03, 0x01, 0x01},
            {0x01, 0x02, 0x03, 0x01},
            {0x01, 0x01, 0x02, 0x03},
            {0x03, 0x01, 0x01, 0x02}
    };
    public static byte mulBy01(byte b){
        return b;
    }

    public static byte mulBy02(byte b){
        int value = b & 0xFF;
        int result = value << 1;
        if((value & 0x80) != 0){
            result ^= 0x1B;
        }
        return (byte) (result & 0xFF);
    }

    public static byte mulBy03(byte b){
        return (byte)(mulBy02(b) ^ (b & 0xFF));
    }

    public static byte mul(byte a, byte b){
        switch (a){
            case 0x01: return mulBy01(b);
            case 0x02: return mulBy02(b);
            case 0x03: return mulBy03(b);
            default: return 0;
        }
    }
    public static byte[][] applyMixColumns(byte[][] state){
        byte[][] result = new byte[4][4];
        for(int col = 0; col<4; col++){
            for(int row =0; row<4; row++){
                result[row][col] = (byte)(
                        mul(mixMatrix[row][0], state[0][col]) ^
                        mul(mixMatrix[row][1], state[1][col]) ^
                        mul(mixMatrix[row][2], state[2][col]) ^
                        mul(mixMatrix[row][3], state[3][col])
                        );
            }
        }
        return result;
    }
}
