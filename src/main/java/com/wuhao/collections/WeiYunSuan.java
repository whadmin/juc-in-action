package com.wuhao.collections;

import org.junit.Test;

import java.util.stream.IntStream;

public class WeiYunSuan {

    public static void main(String[] args) {
        final int SHARED_SHIFT = 16;
        final int SHARED_UNIT = (1 << SHARED_SHIFT);
        final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
        final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
//        System.out.println(Integer.toBinaryString(EXCLUSIVE_MASK));
//        System.out.println(Integer.toBinaryString(MAX_COUNT));
//        System.out.println(Integer.toBinaryString(SHARED_UNIT));
//        System.out.println(SHARED_UNIT);
//        System.out.println(MAX_COUNT);
//        System.out.println(EXCLUSIVE_MASK);
        System.out.println(1 >>> SHARED_SHIFT);
        System.out.println(1 << SHARED_SHIFT);
        System.out.println(Integer.toBinaryString(1 >>> SHARED_SHIFT));
        System.out.println(1 & EXCLUSIVE_MASK);
        System.out.println(Integer.toBinaryString(1 & EXCLUSIVE_MASK));

        System.out.println(Integer.toBinaryString(5));
        System.out.println(Integer.toBinaryString(5 >> 2));
        System.out.println(Integer.toBinaryString(-5));
        System.out.println(Integer.toBinaryString(-5 >> 2));
        System.out.println(Integer.toBinaryString(1 % 7));
        System.out.println(Integer.toBinaryString(-2 & 127));
        System.out.println(-2 & 127);


    }

    @Test
    public void testArraysCopyOf() {
//        System.out.println(Integer.MAX_VALUE);
//        System.out.println(Integer.toBinaryString(Integer.MAX_VALUE));
//        System.out.println(Integer.MIN_VALUE);
//        System.out.println(Integer.toBinaryString(Integer.MIN_VALUE));
//        IntStream.range(1, 31).forEach(p -> {
//            int result = 1;
//            for (int i = 0; i < p; i++) {
//                result=result*2;
//            }
//            System.out.println(result);
//            System.out.println(Integer.toBinaryString(result));
//            System.out.println(result-1);
//            System.out.println(Integer.toBinaryString(result-1));
//            System.out.println("------");
//        });


//        IntStream.range(1, 17).forEach(p -> {
//            System.out.println((0 - p) & (8 - 1));
//        });
//        System.out.println("-------------------");
//        IntStream.range(1, 17).forEach(p -> {
//            System.out.println((0 +p) & (8 - 1));
//        });


//        System.out.println(Integer.toBinaryString(-5));
//        System.out.println(Integer.toBinaryString(7));
//        System.out.println((0 - 5) & (8 - 1));
//        System.out.println(Integer.toBinaryString((0 - 5) & (8 - 1)));
//        System.out.println((-5) %7);


        System.out.println(Integer.toBinaryString(5));
        System.out.println(Integer.toBinaryString(7));
        System.out.println((0 + 5) & (8 - 1));
        System.out.println(Integer.toBinaryString((0 + 5) & (8 - 1)));
        System.out.println((5) %7);
    }
}
