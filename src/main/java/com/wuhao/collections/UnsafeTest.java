package com.wuhao.collections;

import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UnSafeTest
 * UnSafe方法的使用说明
 *
 * @author liuguobin
 * @date 2018/5/10
 */

public class UnsafeTest {

    private static final sun.misc.Unsafe unsafe;

    static {
        try {
            Constructor<Unsafe> constructor = Unsafe.class.getDeclaredConstructor(new Class<?>[0]);
            constructor.setAccessible(true);
            unsafe = constructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Test
    public void testArray() throws NoSuchFieldException {
        int[] arr1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] arr2 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int b = unsafe.arrayBaseOffset(int[].class);
        int s = unsafe.arrayIndexScale(int[].class);
        unsafe.putInt(arr1, (long) b + s * 9, 1);
        unsafe.putInt(arr2, (long) b + s * 9, 2);
        for (int i : arr1) {
            System.out.print(i + " ");
        }
        System.out.println();
        for (int i : arr2) {
            System.out.print(i + " ");
        }
    }

}
