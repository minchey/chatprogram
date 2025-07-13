package com.chatproject.secure_chat.crypto; //첫번째행 0칸, 두번째 행 1칸, 세번쨰 2칸, 네번쨰 3칸 왼쪽으로 이동

public class ShiftRows {
    public static byte[][] shiftRows(byte[][] input){
        byte[][] result = new byte[4][4];
        for(int col = 0; col < 4; col++){
            result[0][col] = input[0][col];
        }

        for(int row =1; row<4; row++){
            for(int col=0; col < 4; col++){
                result[row][col] = input[row][(row+col)%4];
            }
        }
        return result;
    }
}
