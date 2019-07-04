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

/**
 * 当我们在使用CAS操作数据时，会存在ABA问题，和AtomicReference一样AtomicStampedReference对对象的引用做原子操作。但不会引发ABA问题
 *
 * AtomicStampedReference内部定义了一个静态内部类，将对象的引用和一个时间戳放入其中。针对对象的操作每次都需要更新时间戳.
 */
public class AtomicStampedReference<V> {

    private static class Pair<T> {

        /** 对象的引用*/
        final T reference;
        /** 对象的时间戳*/
        final int stamp;
        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }
        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<T>(reference, stamp);
        }
    }

    /**
     * 使用封装了对象引用和时间戳的内部类对象实例作为操作对象
     */
    private volatile Pair<V> pair;


    /**
     * 使用指定initialRef对象，时间戳构造，实例化AtomicReference。
     */
    public AtomicStampedReference(V initialRef, int initialStamp) {
        pair = Pair.of(initialRef, initialStamp);
    }


    /**
     * 返回引用对象
     */
    public V getReference() {
        return pair.reference;
    }


    /**
     * 返回引用对象的时间戳
     */
    public int getStamp() {
        return pair.stamp;
    }


    /**
     * 返回引用对象和时间戳（放入参数中）
     */
    public V get(int[] stampHolder) {
        Pair<V> pair = this.pair;
        stampHolder[0] = pair.stamp;
        return pair.reference;
    }


    /**
     * 使用CAS 更新引用对象只有时间戳，成功返回true,失败返回false
     */
    public boolean weakCompareAndSet(V   expectedReference,
                                     V   newReference,
                                     int expectedStamp,
                                     int newStamp) {
        return compareAndSet(expectedReference, newReference,
                             expectedStamp, newStamp);
    }


    /**
     * 使用CAS 更新引用对象只有时间戳和原始引用对象不过期才更新
     * 这里使用  || 对没有做更改则不处理
     */
    public boolean compareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
        Pair<V> current = pair;
        return
            expectedReference == current.reference &&
            expectedStamp == current.stamp &&
            ((newReference == current.reference &&
              newStamp == current.stamp) ||
             casPair(current, Pair.of(newReference, newStamp)));
    }

    /**
     * 设置对象引用为newReference，并更新时间戳
     */
    public void set(V newReference, int newStamp) {
        Pair<V> current = pair;
        if (newReference != current.reference || newStamp != current.stamp)
            this.pair = Pair.of(newReference, newStamp);
    }

    /**
     * 使用CAS 更新对象引用时间戳
     */
    public boolean attemptStamp(V expectedReference, int newStamp) {
        Pair<V> current = pair;
        return
            expectedReference == current.reference &&
            (newStamp == current.stamp ||
             casPair(current, Pair.of(expectedReference, newStamp)));
    }

    /**
     * 使用CAS 更新pair
     */
    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();

    /** 属性pair在内存中偏移位置 **/
    private static final long pairOffset =
        objectFieldOffset(UNSAFE, "pair", AtomicStampedReference.class);


    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            /** 使用unsafe.objectFieldOffset获取属性pair在内存中偏移位置  **/
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // Convert Exception to corresponding Error
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }
}
