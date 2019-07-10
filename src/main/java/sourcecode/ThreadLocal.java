/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sourcecode;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ThreadLocal<T> {

    /** 获取ThreadLocal hash值，每次实话化其hash值就原子增加HASH_INCREMENT，
     * 第一次实例化值为HASH_INCREMENT **/
    private final int threadLocalHashCode = nextHashCode();

    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * 够保证hash表的每个散列桶能够均匀的分布，这是Fibonacci Hashing
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /** 模板方法，设置线程初始值 **/
    protected T initialValue() {
        return null;
    }

    /**
     * 实例化ThreadLocal，通过Supplier返回当前ThreadLocal初始值
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }


    /**
     * 实例化ThreadLocal
     */
    public ThreadLocal() {
    }


    /**
     * 获取当前线程容器中以当前TheadLocal对象为（key）对应值(value)
     */
    public T get() {
        /** 获取当前线程 **/
        Thread t = Thread.currentThread();
        /** 取出线程内部的 ThreadLocalMap容器 (key-value结构)**/
        ThreadLocalMap map = getMap(t);
        /** 如果ThreadLocalMap存在，获取TheadLocal对象（key）对应值(value) **/
        if (map != null) {
            /** 以ThreadLocal为key  在ThreadLocalMap查找ThreadLocalMap.Entry(值) **/
            ThreadLocalMap.Entry e = map.getEntry(this);
            /**如果ThreadLocalMap.Entry不为空，返回内部存放的对象值 **/
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T) e.value;
                return result;
            }
        }
        /** 如果ThreadLocalMap不存在，实例化ThreadLocalMap，
         *  并设置TheadLocal对象（key）对应初始值，并返回  **/
        return setInitialValue();
    }


    /**
     * 例化ThreadLocalMap，
     * 并设置TheadLocal对象（key）对应初始值，并返回
     */
    private T setInitialValue() {
        /** 模板方法，设置当前TheadLocal（key）初始值 **/
        T value = initialValue();
        Thread t = Thread.currentThread();
        /** 取出线程内部的 ThreadLocalMap容器 (key-value结构)**/
        ThreadLocalMap map = getMap(t);
        /** 判断ThreadLocalMap是否已初始化 **/
        if (map != null)
            /** 将当前TheadLocal（key）对应值(value).设置到ThreadLocalMap 容器中 **/
            map.set(this, value);
        else
            /** 创建一个ThreadLocalMap
             *  将当前TheadLocal（key）对应值(value).设置到ThreadLocalMap 容器中**/
            createMap(t, value);
        return value;
    }

    /**
     * 设置当前线程容器中当前TheadLocal对象（key）对应(value)
     */
    public void set(T value) {
        /** 获取当前线程 **/
        Thread t = Thread.currentThread();
        /** 取出线程内部的 ThreadLocalMap容器 (key-value结构)**/
        ThreadLocalMap map = getMap(t);
        if (map != null)
            /** 将当前TheadLocal为（key），对应value.设置到ThreadLocalMap 容器中 **/
            map.set(this, value);
        else
        /** 创建一个ThreadLocalMap
         *  将当前TheadLocal（key）对应值(value).设置到ThreadLocalMap 容器中**/
            createMap(t, value);
    }

    /**
     * 删除当前线程容器中当前TheadLocal对象（key）对应值(value)
     */
     public void remove() {
         /** 取出线程内部的 ThreadLocalMap容器 (key-value结构)**/
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             /** 从线程容器中删除 当前TheadLocal（key）对应值值(value) **/
             m.remove(this);
     }


    /**
     *  取出线程内部的 ThreadLocalMap容器 (key-value结构)
     */
    ThreadLocalMap getMap(Thread t) {
        //return t.threadLocals;
        return null;
    }


    void createMap(Thread t, T firstValue) {
        //t.threadLocals = new ThreadLocalMap(this, firstValue);
    }



    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }


    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }




    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }


    /**
     * 存放线程变量副本的容器，内部维护了一个hash表，表中每一个元素对应Entry
     *
     *
     */
    static class ThreadLocalMap {

        /**
         * 哈希表中节点对象
         * key ：ThreadLocal对象
         * value：线程变量副本的值
         *
         * 这里我们会将key给WeakReference.referent 引用，被referent引用的属性如果没有被其他指针强引用
         * 会在垃圾收集时被清理，产生脏Entry
         * */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** ThreadLocal */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * 默认哈希表容量，必须是2的幂次方
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 哈希表(Entry)数组
         */
        private Entry[] table;

        /**
         * 容器中存储Entry的个数
         */
        private int size = 0;

        /**
         * 下一次需要扩容的阈值
         */
        private int threshold; // Default to 0

        /**
         * 设置扩容的阈值，设置值为输入值len的三分之二
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }


        /**
         * 哈希表i后一个位置（轮询数组）
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 哈希表i前一个位置（轮询数组）
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }


        /**
         * 初始化ThreadLocalMap，并添加一个Entry（firstKey，firstValue）到ThreadLocalMap容器中
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            /** 使用默认容量构造哈希表 **/
            table = new Entry[INITIAL_CAPACITY];
            /** 通过hash运算获取ThreadLocal在哈希表的 i位置**/
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            /** 构建Entry（firstKey，firstValue）并添加到哈希表中 **/
            table[i] = new Entry(firstKey, firstValue);
            /** 设置哈希表当前容量1 **/
            size = 1;
            /** 使用默认容量2/3作为哈希表扩容的阈值 **/
            setThreshold(INITIAL_CAPACITY);
        }


        /**
         * 构造一个ThreadLocalMap，包括指定ThreadLocalMap中所有Entry
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            /** 使用指定ThreadLocalMap容量2/3作为哈希表扩容的阈值 **/
            setThreshold(len);
            table = new Entry[len];

            /** 遍历指定ThreadLocalMap所有Entry，添加到构造的新ThreadLocalMap中 **/
            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 给定指定key，查找Entry
         */
        private Entry getEntry(ThreadLocal<?> key) {
            /** 通过hash运算获取ThreadLocal在哈希表的 i位置**/
            int i = key.threadLocalHashCode & (table.length - 1);
            /** 获取哈希表 i位置Entry **/
            Entry e = table[i];
            /** 如果Entry存在且Entry.key和传入的key匹配，则直接返回Entry **/
            if (e != null && e.get() == key)
                return e;
            else
                /**
                 * 1 传入key对应Entry不存在直接返回
                 * 2 传入key对应Entry存在，传入key和Entry.key不匹配(hash碰撞)，使用开放定址法在哈希表中查找
                 * **/
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 由于存在不同ThreadLocalhash运算获取ThreadLocal在哈希表的 i位置相同（Hash碰撞）
         * 因而在往ThreadLocalMap插入过程中可能存在hashCode映射到哈希表数组下标位置已经被占用情况
         * 此时ThreadLocalMap会使用开放定址法，从计算下标位置 i 向后查找未被暂用的哈希表节点插入
         *
         * 所以在查找可能出现映射到哈希表数组下标位置的Entry和查看key不相同情况。
         * 此时按照开放定址法，从计算下标位置向后查找遍历Entry，找到和自身传入key相同的Entry节点返回
         *
         * 这其中如果遇到key==null的脏节点，调用expungeStaleEntry清理
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                /** 对比key是否相同，如果相同则返回 **/
                if (k == key)
                    return e;
                /**  如果发现脏节点，则调用expungeStaleEntry清理 **/
                if (k == null)
                    expungeStaleEntry(i);
                else
                    /** 获取当前位置下一个哈希表节点 **/
                    i = nextIndex(i, len);
                e = tab[i];
            }
            /** 如果ashCode映射到哈希表数组下标位置为null，说明传入key对应的节点在哈希表中不存在 **/
            return null;
        }

        /**
         * 设置ThreadLocal（key），对应值
         */
        private void set(ThreadLocal<?> key, Object value) {

            /** 获取哈希数组 **/
            Entry[] tab = table;
            /** 获取哈希数组长度 **/
            int len = tab.length;
            /** 通过hash运算获取ThreadLocal在哈希表的 i位置**/
            int i = key.threadLocalHashCode & (len-1);

            /** 获取下标位置的Entry，判断Entry是否存在，且Entry.key和传入key相同
             *  如果Entry存在且Entry.key和传入key相同，则覆盖值
             *  如果Entry存在且Entry,key和传入key不相同，表明发生hash碰撞，按照开放定址法，从下标位置向后查找未被暂用的哈希表节点插入
             *  如果Entry存在且Entry,key==null,表明此Entry是一个脏Entry，用当前传入值构造一个新Entry，替换这个下标位置脏Entry
             * **/
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {

                ThreadLocal<?> k = e.get();
                /** 如果Entry存在且Entry.key和传入key相同，则覆盖值 **/
                if (k == key) {
                    e.value = value;
                    return;
                }
                /** 如果Entry存在且Entry,key==null,表明此Entry是一个脏Entry，使用replaceStaleEntry处理插入 **/
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
                /** 如果Entry存在且Entry,key和传入key不相同，表明发生hash碰撞，按照开放定址法，从下标位置向后查找未被暂用的哈希表节点插入 **/
            }

            /** 新建entry并插入哈希表 i 位置 **/
            tab[i] = new Entry(key, value);
            /** 哈希表节点数量+1 **/
            int sz = ++size;
            /** 插入后从哈希表中查找并擦除脏Entry,如果未找到脏，且哈希表容量大于阈值就需要扩容 **/
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * 删除ThreadLocal（key），对应的副本变量值
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            /** 通过hash运算获取ThreadLocal在哈希表的 i位置**/
            int i = key.threadLocalHashCode & (len-1);

            /** 获取下标位置的Entry，判断Entry是否存在，且Entry.key和传入key相同
             *  如果Entry存在，且Entry.key和传入key相同，则将查找到Entry设置为脏节点，然后调用expungeStaleEntry清理
             *  如果Entry存在，且Entry.key和传入key不相同，表明发生hash碰撞，按照开放定址法，从下标位置向后查找key和传入相同哈希表节
             *  如果找到则将查找到Entry设置为脏节点，然后调用expungeStaleEntry清理
             */
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }


        /**
         * 1 从staleSlot向前找到第一个脏entry的位置slotToExpunge
         * 2 从staleSlot向后找到和传入key节点相同的节点，如果找到则替换该节点，直到遇到一个空节点结束
         * 3 如果步骤2没有找到key相同的节点，则表明staleSlot位置脏Entry节点就是需要替换的节点
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            /** 从staleSlot向前找到第一个脏entry的位置slotToExpunge **/
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            /** 从staleSlot向后找和传入key相同的节点， **/
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();


                /** 从staleSlot向后找到和传入key节点相同的节点，如果找到则替换该节点 **/
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    /** 如果向前未搜索到脏entry，则从slotToExpunge节点位置开始清理脏 **/
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                /**  **/
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            /** staleSlot位置脏Entry节点就是需要替换的节点 **/
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            /** 如果向前未搜索到脏entry，则从slotToExpunge节点位置开始清理脏 **/
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * 1 擦除脏Entry，并从脏Entry位置开始向后查找脏Entry，发现则擦除直到一个空节点结束
         * 2 从当前staleSlot位置向后环形（nextIndex）继续搜索，发现冲突的节点填充到前面的擦除节点
         * 返回探测到为null节点
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            /**  直接擦除指定索引位置Entry； **/
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                /**  直接擦除指定索引位置Entry； **/
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    /** 通过hash运算获取ThreadLocal在哈希表的 i位置**/
                    int h = k.threadLocalHashCode & (len - 1);
                    /** 比较有无碰撞，存在则使用 开放定址法，从下标位置向后查找未被暂用的哈希表节点插入**/
                    if (h != i) {
                        tab[i] = null;

                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * 从位置i开始向后扫描logN次，
         *   如果每发现一次脏Entry则，则调用expungeStaleEntry擦除脏Entry，
         *   其内部会继续向后探测脏Entry并擦除，把hash冲突节点填充到擦除的位置。直到找到一个空节点
         *   同时重新扫描次数为loglen
         *
         * 返回true则表示找到脏Entry并已擦除
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }


        /**
         * 哈希表扩容
         */
        private void rehash() {
            /** 擦除哈希表中所有脏Entry **/
            expungeStaleEntries();

            /** 当容量大于阈值的3/4则扩容 **/
            if (size >= threshold - threshold / 4)
                resize();
        }


        /**
         * 每次扩容偶会创建当前容量*2的哈希表，并将原来哈希表中的数据拷贝过去
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            /** 遍历原始的哈希表，脏Entry擦除，非脏Entry重新计算ThreadLocal在新哈希表的 i位置
             * 并插入 **/
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            /** 重新设置新的阈值 **/
            setThreshold(newLen);
            /** 重新设置新的大小 **/
            size = count;
            table = newTab;
        }

        /**
         * 擦除哈希表中所有脏Entry
         * 1 擦除哈希表中所有脏Entry
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
