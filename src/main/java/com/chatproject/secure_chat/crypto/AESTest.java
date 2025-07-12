package com.chatproject.secure_chat.crypto;

public class AESTest {
    public static void main(String[] args) {
        // 1. 테스트용 문자열 (16바이트)
        String text = "AES Test String!"; // 정확히 16글자 (16바이트)
        byte[] inputBytes = text.getBytes();

        // 2. 상태 행렬 생성
        StateMatrix state = new StateMatrix(inputBytes);

        // 3. SubBytes 적용
        byte[][] substituted = SubBytes.applySubBytes(state.getState());
         substituted = ShiftRows.shiftRows(substituted);

        // 4. 결과 출력
        System.out.println("🔹 SubBytes 결과:");
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                System.out.printf("%02X ", substituted[row][col]);
            }
            System.out.println();
        }
    }
}
