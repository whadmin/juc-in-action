package com.wuhao.collections;

import org.junit.Test;

import java.util.Random;

public class FinalT {

    private final int a = 10;

    private final Value v1 = new Value(10);


    private static Random random = new Random(20);
    private final int VALUE_A = 10;
    private static final int VALUE_B = 20;
    public static final int VALUE_C = random.nextInt(10);
    public final int VALUE_D = random.nextInt(10);

   //private final int b;

    public FinalT(){

    }

    public FinalT(int i)
    {
//        this.b = i;
    }


    @Test
    public void test1() {
        FinalT test = new FinalT();
        //test.VALUE_A = 5;//Error:不可以这样做
        //test.VALUE_B  =21;//Error:不可以这样做

        FinalT finalT = new FinalT();
        FinalT finalT1 = new FinalT();

        System.out.println("VALUE_C:" + VALUE_C);
        System.out.println("VALUE_C:" + finalT.VALUE_C);
        System.out.println("VALUE_C:" + finalT1.VALUE_C);
        System.out.println("---------");
        System.out.println("VALUE_D:" + finalT.VALUE_D);
        System.out.println("VALUE_D:" + finalT1.VALUE_D);
    }

    @Test
    public void test2() {
        FinalT test = new FinalT();
        //test.a = 5;//不可以这样做
        test.v1.value++;
        //test.v1 = new Value(12);//Error:不可以这样做
        System.out.println("对象内的数据:"+test.v1.value);

    }

    @Test
    public void test3() {
        FinalT test = new FinalT();
        //test.a = 5;//不可以这样做
        test.v1.value++;
        //test.v1 = new Value(12);//Error:不可以这样做
        System.out.println("对象内的数据:"+test.v1.value);

    }


    class Value{
        int value;
        public Value(int value) {
            this.value = value;
        }
    }

}
