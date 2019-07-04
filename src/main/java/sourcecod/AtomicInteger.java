package sourcecod;

import sun.misc.Unsafe;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 原子Integer实现，所有操作都具有 '原子性" 线程安全
 */
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    /** unsafe **/
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    /** 获取属性value在内存中偏移位置 **/
    private static final long valueOffset;

    static {
        try {
            /** 获取属性value在内存中偏移位置  **/
            valueOffset = unsafe.objectFieldOffset
                    (java.util.concurrent.atomic.AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * int值，声明为volatile，"可见性"
     */
    private volatile int value;

    /**
     * 使用给定的初始值实例化AtomicInteger。
     */
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    /**
     * 实例化一个初始值为{@code 0}的新AtomicInteger。
     */
    public AtomicInteger() {
    }

    /**
     * 获取int值
     */
    public final int get() {
        return value;
    }

    /**
     * 设置int值为新值newValue
     */
    public final void set(int newValue) {
        value = newValue;
    }

    /**
     * 设置int值为新值newValue（不保证其他线程立刻看到），
     * 因为value设置为volatile一般情况下其他线程都可见，这里就是抛弃了volatile特性，
     * 1 清空其他线程中缓存，保证可见性
     * 2 内存屏障保证有序性。
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(int newValue) {
        unsafe.putOrderedInt(this, valueOffset, newValue);
    }

    /**
     * 原子设置为给定值并返回旧值。(内部使用CAS乐观锁+循环)
     */
    public final int getAndSet(int newValue) {
        return unsafe.getAndSetInt(this, valueOffset, newValue);
    }

    /**
     * 使用CAS设置新值newValue，成功返回true,失败返回false
     */
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 使用CAS设置新值newValue，成功返回true,失败返回false
     */
    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 以原子方式将当前值+1。(内部使用CAS乐观锁+循环),返回旧值
     */
    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }

    /**
     * 以原子方式将当前值-1。(内部使用CAS乐观锁+循环),返回旧值
     */
    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }

    /**
     * 以原子方式将当前值增加delta。这里传入负数就是减少|delta|(内部使用CAS乐观锁+循环)，返回旧值
     */
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

    /**
     * 以原子方式将当前值+1。(内部使用CAS乐观锁+循环),返回新值
     */
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

    /**
     * 以原子方式将当前值-1。(内部使用CAS乐观锁+循环),返回新值
     */
    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }

    /**
     * 以原子方式将当前值增加delta。这里传入负数就是减少|delta|(内部使用CAS乐观锁+循环)，返回新值
     */
    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }

    /**
     * 以原子方式执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 以原子方式执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 以原子方式执行IntBinaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final int getAndAccumulate(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 以原子方式执行IntBinaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }


    public String toString() {
        return Integer.toString(get());
    }


    public int intValue() {
        return get();
    }


    public long longValue() {
        return (long)get();
    }


    public float floatValue() {
        return (float)get();
    }


    public double doubleValue() {
        return (double)get();
    }

}
