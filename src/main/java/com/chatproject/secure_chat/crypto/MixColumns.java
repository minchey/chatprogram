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
}
