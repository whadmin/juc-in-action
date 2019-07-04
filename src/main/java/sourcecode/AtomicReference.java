package sourcecode;

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import sun.misc.Unsafe;

/**
 * 原子引用类实现，所有操作都具有 '原子性" 线程安全（存在ABA问题）
 */
public class AtomicReference<V> implements java.io.Serializable {
    private static final long serialVersionUID = -1848883965231344442L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /** 获取属性value在内存中偏移位置 **/
    private static final long valueOffset;

    static {
        try {
            /** 使用unsafe.objectFieldOffset获取属性value在内存中偏移位置  **/
            valueOffset = unsafe.objectFieldOffset
                    (java.util.concurrent.atomic.AtomicReference.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile V value;

    /**
     * 操作对象(类型为引用)，声明为volatile，"可见性"
     */
    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    /**
     * 实例化一个初始值为null的新AtomicReference
     */
    public AtomicReference() {
    }

    /**
     * 获取对象引用value
     */
    public final V get() {
        return value;
    }

    /**
     * 设置对象引用为新值newValue
     */
    public final void set(V newValue) {
        value = newValue;
    }

    /**
     * 设置对象引用value为新值newValue（不保证其他线程立刻看到），
     * 因为value设置为volatile一般情况下其他线程都可见，这里就是抛弃了volatile特性，
     * 1 清空其他线程中缓存，保证可见性
     * 2 内存屏障保证有序性。
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(V newValue) {
        unsafe.putOrderedObject(this, valueOffset, newValue);
    }

    /**
     * 使用CAS设置对象引用为新值newValue，成功返回true,失败返回false
     */
    public final boolean compareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }

    /**
     * 使用CAS设置对象引用为新值newValue，成功返回true,失败返回false
     */
    public final boolean weakCompareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }

    /**
     * 原子方式设置对象引用为新值newValue，并返回旧值。(内部使用CAS乐观锁+循环)
     */
    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        return (V)unsafe.getAndSetObject(this, valueOffset, newValue);
    }

    /**
     * 以原子方式将value执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }


    /**
     * 以原子方式将value执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 以原子方式将value执行BinaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final V getAndAccumulate(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 以原子方式将value执行BinaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final V accumulateAndGet(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }



    public String toString() {
        return String.valueOf(get());
    }

}

