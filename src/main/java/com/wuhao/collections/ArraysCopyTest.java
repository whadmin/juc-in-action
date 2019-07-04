package com.wuhao.collections;

import org.junit.Test;

import java.util.Arrays;

public class ArraysCopyTest {
    /**
     * Arrays.copyOf(dataType[] srcArray,int length);
     * srcArray 表示要进行复制的数组，
     * length 表示复制后的新数组的长度。
     */
    @Test
    public void testArraysCopyOf() {
        //定义长度为 5 的数组
        int scores[] = new int[]{57, 81, 68, 75, 91};
        //输出源数组
        System.out.println("源数组内容如下：");
        //循环遍历源数组
        for (int i = 0; i < scores.length; i++) {
            //将数组元素输出
            System.out.print(scores[i] + "\t");
        }
        //定义一个新的数组，将scores数组中的5个元素复制过来,同时留3个内存空间供以后开发使用
        int[] newScores = (int[]) Arrays.copyOf(scores, 8);
        System.out.println("\n复制的新数组内容如下：");
        //循环遍历复制后的新数组
        for (int j = 0; j < newScores.length; j++) {
            //将新数组的元素输出
            System.out.print(newScores[j] + "\t");
        }
    }

    /**
     * 使用 CopyOfRange() 方法对数组进行复制
     * Arrays.copyOfRange(dataType[] srcArray,int startIndex,int endIndex)
     * srcArray 表示源数组；
     * startIndex 表示开始复制的起始索引，目标数组中将包含起始索引对应的元素，
     * startIndex 必须在 0 到 srcArray.length 之间。
     * endIndex   表示终止索引，目标数组中将不包含终止索引对应的元素，
     * endIndex 必须大于等于 startIndex，
     * endIndex 可以大于 srcArray.length，如果大于 srcArray.length，则目标数组中使用默认值填充。
     */
    @Test
    public void testCopyOfRange() {
        //定义长度为8的数组
        int scores[] = new int[]{57, 81, 68, 75, 91, 66, 75, 84};
        System.out.println("源数组内容如下：");
        //循环遍历源数组
        for (int i = 0; i < scores.length; i++) {
            System.out.print(scores[i] + "\t");
        }
        //复制源数组的前5个元素到newScores数组中
        int newScores[] = (int[]) Arrays.copyOfRange(scores, 0, 5);
        System.out.println("\n复制的新数组内容如下：");
        //循环遍历目标数组，即复制后的新数组
        for (int j = 0; j < newScores.length; j++) {
            System.out.print(newScores[j] + "\t");
        }
    }

    /**
     * 使用arraycopy()方法对数组进行复制
     * System.arraycopy(Object srcArray,int srcIndex,Object destArray,int destIndex,int length)
     * <p>
     * srcArray 表示源数组；
     * srcIndex 表示源数组中的起始索引；
     * destArray 表示目标数组；
     * destIndex 表示目标数组中的起始索引；
     * length 表示要复制的数组长度。
     * <p>
     * length+srcIndex 必须小于等于 srcArray.length，
     * length+destIndex 必须小于等于 destArray.length。
     */
    @Test
    public void testSystemArraycopy() {

        //定义源数组，长度为8
        int scores[] = new int[]{100, 81, 68, 75, 91, 66, 75, 100};
        //定义目标数组
        int newScores[] = new int[]{80, 82, 71, 92, 68, 71, 87, 88, 81, 79, 90, 77};
        System.out.println("源数组中的内容如下：");
        //遍历源数组
        for (int i = 0; i < scores.length; i++) {
            System.out.print(scores[i] + "\t");
        }
        System.out.println("\n目标数组中的内容如下：");
        //遍历目标数组
        for (int j = 0; j < newScores.length; j++) {
            System.out.print(newScores[j] + "\t");
        }
        System.arraycopy(scores, 0, newScores, 2, 8);

        //复制源数组中的一部分到目标数组中
        System.out.println("\n替换元素后的目标数组内容如下：");
        //循环遍历替换后的数组
        for (int k = 0; k < newScores.length; k++) {
            System.out.print(newScores[k] + "\t");
        }
    }


}
