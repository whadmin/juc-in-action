/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package sourcecode;

import sun.misc.Unsafe;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 原子AtomicIntegerArray 实现，所有操作都具有 '原子性" 线程安全
 */
public class AtomicIntegerArray implements java.io.Serializable {
    private static final long serialVersionUID = 2862133569453604235L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    /** 获取该类型的数组，在对象存储时，存放第一个元素的内存地址 **/
    private static final int base = unsafe.arrayBaseOffset(int[].class);
    /** 用于计算数组中每个元素的偏移位置 **/
    private static final int shift;
    /** 操作对象 **/
    private final int[] array;

    static {
        /** 返回数组中每一个元素占用的大小(int类型返回4)**/
        int scale = unsafe.arrayIndexScale(int[].class);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        /** 返回 scale 高位连续0的个数(int 类型返回2) **/
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }

    /**
     * 获取数组中每个元素的偏移位置（存在校验）
     */
    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);

        return byteOffset(i);
    }

    /**
     * 获取数组中每个元素的偏移位置
     */
    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    /**
     * 实例化一个AtomicIntegerArray，设置素组大小
     */
    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    /**
     * 创建一个新的AtomicIntegerArray，并给定一个数组初始化
     */
    public AtomicIntegerArray(int[] array) {
        // Visibility guaranteed by final field guarantees
        this.array = array.clone();
    }

    /**
     * 返回数组的长度
     */
    public final int length() {
        return array.length;
    }

    /**
     * 获取位置{@code i}的当前值。
     */
    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private int getRaw(long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    /**
     * 将位置{@code i}的元素设置为给定值。
     */
    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
    }

    /**
       将位置{@code i}的元素设置为给定值newValue（不保证其他线程立刻看到），
          因为value设置为volatile一般情况下其他线程都可见，这里就是抛弃了volatile特性，
          1 清空其他线程中缓存，保证可见性
          2 内存屏障保证有序性。
     *
     */
    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * 原子方式将位置{@code i}的元素设置为给定值newValue并返回旧值。(内部使用CAS乐观锁+循环)
     */
    public final int getAndSet(int i, int newValue) {
        return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * 使用CAS将位置{@code i}的元素设置为给定值newValue，成功返回true,失败返回false
     */
    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

    /**
     * 使用CAS将位置{@code i}的元素设置为给定值newValue，成功返回true,失败返回false
     */
    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    /**
     * 以原子方式将位置{@code i}的元素+1。(内部使用CAS乐观锁+循环),返回旧值
     */
    public final int getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    /**
     * 以原子方式将位置{@code i}的元素-1。(内部使用CAS乐观锁+循环),返回旧值
     */
    public final int getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    /**
     * 以原子方式将位置{@code i}的元素+delta。这里传入负数就是减少|delta|(内部使用CAS乐观锁+循环),返回旧值
     */
    public final int getAndAdd(int i, int delta) {
        return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
    }

    /**
     * 以原子方式将位置{@code i}的元素+1。(内部使用CAS乐观锁+循环),返回新值
     */
    public final int incrementAndGet(int i) {
        return getAndAdd(i, 1) + 1;
    }

    /**
     * 以原子方式将位置{@code i}的元素-1。(内部使用CAS乐观锁+循环),返回新值
     */
    public final int decrementAndGet(int i) {
        return getAndAdd(i, -1) - 1;
    }

    /**
     * 以原子方式将位置{@code i}的元素+delta。这里传入负数就是减少|delta|(内部使用CAS乐观锁+循环),返回新值
     */
    public final int addAndGet(int i, int delta) {
        return getAndAdd(i, delta) + delta;
    }


    /**
     * 以原子方式将位置{@code i}的元素+执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * 以原子方式将位置{@code i}的元素+执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    /**
     * 以原子方式将位置{@code i}的元素+执行IntBinaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final int getAndAccumulate(int i, int x,
                                      IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * 以原子方式将位置{@code i}的元素+执行IntBinaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final int accumulateAndGet(int i, int x,
                                      IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    /**
     * Returns the String representation of the current values of array.
     * @return the String representation of the current values of array
     */
    public String toString() {
        int iMax = array.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getRaw(byteOffset(i)));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',').append(' ');
        }
    }

}
