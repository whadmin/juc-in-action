/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.util.*;



public class HashMap<K,V>
    extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
{

    /**
     * 默认的初始容量为16
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * 最大的容量为2的30次方
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认的装载因子
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 空的哈希表
     */
    static final Entry<?,?>[] EMPTY_TABLE = {};

    /**
     * 存储Entry结构数组，长度必须始终是2的幂。即哈希表
     */
    transient Entry<K,V>[] table = (Entry<K,V>[]) EMPTY_TABLE;

    /**
     * Map中存储Entry数量
     */
    transient int size;

    /**
     * 进行下一次扩容Entry结构数组元素的数量的阀值  计算公式=(capacity * loadFactor).
     * 扩容阀值
     */
    int threshold;

    /**
     * 装载因子
     */
    final float loadFactor;

    /**
     * 数据修改次数
     */
    transient int modCount;

    /**
     * 映射容量的默认阈值，在该阈值之上，对字符串键使用可选散列。由于字符串键的散列代码计算能力较弱，可选散列可以减少冲突的发生率。
     */
    static final int ALTERNATIVE_HASHING_THRESHOLD_DEFAULT = Integer.MAX_VALUE;

    /**
     * 保存在VM启动之后才能初始化的值。
     */
    private static class Holder {

        /**
         * 表容量，在其上切换为使用可选散列
         */
        static final int ALTERNATIVE_HASHING_THRESHOLD;

        static {
            String altThreshold = java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction(
                    "jdk.map.althashing.threshold"));

            int threshold;
            try {
                threshold = (null != altThreshold)
                        ? Integer.parseInt(altThreshold)
                        : ALTERNATIVE_HASHING_THRESHOLD_DEFAULT;

                // disable alternative hashing if -1
                if (threshold == -1) {
                    threshold = Integer.MAX_VALUE;
                }

                if (threshold < 0) {
                    throw new IllegalArgumentException("value must be positive integer.");
                }
            } catch(IllegalArgumentException failed) {
                throw new Error("Illegal value for 'jdk.map.althashing.threshold'", failed);
            }

            ALTERNATIVE_HASHING_THRESHOLD = threshold;
        }
    }

    /**
     * 与此实例关联的随机化值，应用于键的哈希代码以使哈希冲突更难找到。如果为0，则禁用替代哈希。
     */
    transient int hashSeed = 0;

    /**
     * 用指定的初始容量和默认装载因子实例化一个空的哈希映射
     * @param  initialCapacity 初始值
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 用默认初始容量,默认装载因子实例化一个空的哈希映射
     */
    public HashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 用指定的初始容量,装载因子实例化一个空的哈希映射
     * @param  initialCapacity 初始值
     * @param  loadFactor      装载因子
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        /** 设置装载因子（默认0.75f） **/
        this.loadFactor = loadFactor;
        /** 设置扩容阀值  **/
        threshold = initialCapacity;
        /** jdk1.7中hashMap的init方法是空实现，并没有创建存储Entry结构数组**/
        init();
    }

    /**
     * 空实现
     */
    void init() {
    }


    /**
     * 用指定哈希映射实例化一个新的哈希映射
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                      DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        /** 实例化存储Entry结构数组  **/
        inflateTable(threshold);
        /** 将指定Map元素遍历添加到Map中 **/
        putAllForCreate(m);
    }


    /**
     * 实例化存储Entry结构数组
     */
    private void inflateTable(int toSize) {
        /** 用来返回大于等于最接近number的2的冪数 **/
        int capacity = roundUpToPowerOf2(toSize);
        /** 设置扩容阀值  **/
        threshold = (int) Math.min(capacity * loadFactor, MAXIMUM_CAPACITY + 1);
        /** 实例化存储Entry结构数组 **/
        table = new Entry[capacity];
        /** 初始化哈希掩码值。我们推迟初始化，直到真正需要它。**/
        initHashSeedAsNeeded(capacity);
    }

    /**
     * 用来返回大于等于最接近number的2的冪数
     */
    private static int roundUpToPowerOf2(int number) {
        // assert number >= 0 : "number must be non-negative";
        return number >= MAXIMUM_CAPACITY
                ? MAXIMUM_CAPACITY
                : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
    }

    /**
     * 初始化哈希掩码值。我们推迟初始化，直到真正需要它。
     */
    final boolean initHashSeedAsNeeded(int capacity) {
        boolean currentAltHashing = hashSeed != 0;
        boolean useAltHashing = sun.misc.VM.isBooted() &&
                (capacity >= Holder.ALTERNATIVE_HASHING_THRESHOLD);
        boolean switching = currentAltHashing ^ useAltHashing;
        if (switching) {
            hashSeed = useAltHashing
                    ? sun.misc.Hashing.randomHashSeed(this)
                    : 0;
        }
        return switching;
    }

    /**
     * 计算对象hash值
     */
    final int hash(Object k) {
        int h = hashSeed;
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);
        }
        h ^= k.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * 计算hash对应Entry结构数组下标
     */
    static int indexFor(int h, int length) {
        // assert Integer.bitCount(length) == 1 : "length must be a non-zero power of 2";
        return h & (length-1);
    }

    /**
     * 返回Map中存储 key-value结构数据数量
     */
    public int size() {
        return size;
    }

    /**
     * 返回Map中存储 key-value结构数据数量是否为0
     */
    public boolean isEmpty() {
        return size == 0;
    }



    /**
     * 向Map添加一个key-value结构,如果key存在返回于Map中，会覆盖value返回原始值
     */
    public V put(K key, V value) {
        /** 如果Entry结构数组为EMPTY_TABLE,调用inflateTable(threshold)实例化存储Entry结构数组 **/
        if (table == EMPTY_TABLE) {
            inflateTable(threshold);
        }
        /** 向Map添加一个key-value结构，其key为null **/
        if (key == null)
            return putForNullKey(value);

        /** 计算对象hash值**/
        int hash = hash(key);
        /** 计算hash对应Entry结构数组下标 **/
        int i = indexFor(hash, table.length);
        /**
         * 判断table[i]是否存在元素Entry，
         * 如果存在则从Entry开始，作为链表的头部元素，向后遍历查找key&hash相同元素Entry(用来表示插入的key-value以存在于Map中).
         * 如果找到则覆盖Entry.value
         */
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        /** 修改次数+1 **/
        modCount++;
        /** 创建一个Entry，存储在Entry类型数组指定下标位置，该方法支持扩容 **/
        addEntry(hash, key, value, i);
        return null;
    }

    /**
     * 向Map添加一个key-value结构，其key为null
     */
    private V putForNullKey(V value) {
        /**
         * 判断table[0]是否存在元素Entry，
         * 如果存在则从Entry开始，作为链表的头部元素，向后遍历查找key=null元素Entry(用来表示插入的key-value以存在于Map中).
         * 如果找到则覆盖Entry.value,并返回原始value
         */
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        /** 修改次数+1 **/
        modCount++;
        /** 创建一个Entry，存储在Entry类型数组指定下标位置，该方法支持扩容 **/
        addEntry(0, null, value, 0);
        return null;
    }

    /**
     * 创建一个Entry，存储在Entry类型数组指定下标位置，该方法支持扩容
     */
    void addEntry(int hash, K key, V value, int bucketIndex) {
        /** 如果size打印阀值进行扩容处理 **/
        if ((size >= threshold) && (null != table[bucketIndex])) {
            /** 扩容处理 **/
            resize(2 * table.length);
            /** 重新计算hash **/
            hash = (null != key) ? hash(key) : 0;
            /** 重新计算hash对应Entry结构数组下标bucketIndex **/
            bucketIndex = indexFor(hash, table.length);
        }
        /** 创建一个Entry，next指向的原始table[bucketIndex]，存储到table[bucketIndex]位置，作为链表的新头部元素**/
        createEntry(hash, key, value, bucketIndex);
    }


    /**
     * 将指定Map中所有key-value元素添加到当前Map中
     */
    private void putAllForCreate(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            putForCreate(e.getKey(), e.getValue());
        }
    }

    /**
     * 添加key-value 内部方法，它被构造函数等调用
     */
    private void putForCreate(K key, V value) {
        /** 计算hash **/
        int hash = null == key ? 0 : hash(key);
        /** 计算hash对应Entry结构数组下标bucketIndex **/
        int i = indexFor(hash, table.length);

        /**
         * 判断table[i]是否存在元素Entry，
         * 如果存在则从Entry开始，作为链表的头部元素，向后遍历查找key&hash相同元素Entry(用来表示插入的key-value以存在于Map中).
         * 如果找到则覆盖Entry.value
         */
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash &&
                ((k = e.key) == key || (key != null && key.equals(k)))) {
                e.value = value;
                return;
            }
        }
        /** 创建一个Entry，next指向的原始table[bucketIndex]，存储到table[bucketIndex]位置，作为链表的新头部元素  **/
        createEntry(hash, key, value, i);
    }


    /**
     * 创建一个Entry，next指向的原始table[bucketIndex]，存储到table[bucketIndex]位置，作为链表的新头部元素
     */
    void createEntry(int hash, K key, V value, int bucketIndex) {
        /** 获取原始table[bucketIndex]**/
        Entry<K,V> e = table[bucketIndex];
        /** 创建一个Entry，next指向的原始table[bucketIndex]，存储到table[bucketIndex]位置，作为链表的新头部元素**/
        table[bucketIndex] = new Entry<>(hash, key, value, e);
        /** 数量+1 **/
        size++;
    }

    /**
     * 扩容
     */
    void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        /** 创建2倍大小的新数组 **/
        Entry[] newTable = new Entry[newCapacity];
        /** 将旧数组的链表转移到新数组，就是这个方法导致的hashMap不安全 **/
        transfer(newTable, initHashSeedAsNeeded(newCapacity));
        /** 设置新的Entry数组**/
        table = newTable;
        /** 重新计算扩容阀值**/
        threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
    }

    /**
     * 将旧数组的链表转移到新数组，就是这个方法导致的hashMap不安全
     */
    void transfer(Entry[] newTable, boolean rehash) {
        int newCapacity = newTable.length;
        /** 遍历原始table中所有Entry **/
        for (Entry<K,V> e : table) {
            /** 遍历table数组中每一个Entry对应的链表中所有Entry **/
            while(null != e) {
                Entry<K,V> next = e.next;
                /** 是否重新计算hash **/
                if (rehash) {
                    e.hash = null == e.key ? 0 : hash(e.key);
                }
                /** 计算hash对应Entry结构数组下标 **/
                int i = indexFor(e.hash, newCapacity);
                /** 将当前Entry.next指向的原始table[i]，存储到table[i]位置,作为链表的新头部元素**/
                e.next = newTable[i];
                newTable[i] = e;
                e = next;
            }
        }
    }

    /**
     * 从Map删除指定的key
     */
    public V remove(Object key) {
        Entry<K,V> e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }

    /**
     * 从Map获取删除指定的key对应Entry，并将Entry从存储结构中剔除
     */
    final Entry<K,V> removeEntryForKey(Object key) {
        if (size == 0) {
            return null;
        }
        /** 计算hash **/
        int hash = (key == null) ? 0 : hash(key);
        /** 计算hash对应Entry结构数组下标bucketIndex **/
        int i = indexFor(hash, table.length);
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;

        /** 从Map获取删除指定的key对应Entry，并将Entry从存储结构中剔除  **/
        while (e != null) {
            Entry<K,V> next = e.next;
            Object k;
            if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                modCount++;
                size--;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;
                e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }
        return e;
    }


    /**
     * 获取key对应值value
     */
    public V get(Object key) {
        /** **/
        if (key == null)
            return getForNullKey();
        Entry<K,V> entry = getEntry(key);

        return null == entry ? null : entry.getValue();
    }

    /**
     * 获取key为null对应值value，key为NULL对应Entry，存储在类型为Entry数组的第一个下标位置
     */
    private V getForNullKey() {
        if (size == 0) {
            return null;
        }
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null)
                return e.value;
        }
        return null;
    }

    /**
     * 判断指定key是否存储在Map
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * 获取key存储在Map中的结构对象Entry
     */
    final Entry<K,V> getEntry(Object key) {
        if (size == 0) {
            return null;
        }
        /** 获取key对应的hash **/
        int hash = (key == null) ? 0 : hash(key);
        /** 获取hash对应存在Entry结构的下标，并遍历此下标为开始节点链表，找到存在在key相同Entry，并返回**/
        for (Entry<K,V> e = table[indexFor(hash, table.length)];
             e != null;
             e = e.next) {
            Object k;
            if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                return e;
        }
        return null;
    }


    public void putAll(Map<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        if (table == EMPTY_TABLE) {
            inflateTable((int) Math.max(numKeysToBeAdded * loadFactor, threshold));
        }

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }



    final Entry<K,V> removeMapping(Object o) {
        if (size == 0 || !(o instanceof Map.Entry))
            return null;

        Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
        Object key = entry.getKey();
        int hash = (key == null) ? 0 : hash(key);
        int i = indexFor(hash, table.length);
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;
                e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    public void clear() {
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }


    public boolean containsValue(Object value) {
        if (value == null)
            return containsNullValue();

        Entry[] tab = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    private boolean containsNullValue() {
        Entry[] tab = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (e.value == null)
                    return true;
        return false;
    }

    public Object clone() {
        HashMap<K,V> result = null;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // assert false;
        }
        if (result.table != EMPTY_TABLE) {
            result.inflateTable(Math.min(
                (int) Math.min(
                    size * Math.min(1 / loadFactor, 4.0f),
                    // we have limits...
                    HashMap.MAXIMUM_CAPACITY),
               table.length));
        }
        result.entrySet = null;
        result.modCount = 0;
        result.size = 0;
        result.init();
        result.putAllForCreate(this);

        return result;
    }

    static class Entry<K,V> implements Map.Entry<K,V> {
        final K key;
        V value;
        Entry<K,V> next;
        int hash;

        /**
         * Creates new entry.
         */
        Entry(int h, K k, V v, Entry<K,V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        public final String toString() {
            return getKey() + "=" + getValue();
        }

        /**
         * This method is invoked whenever the value in an entry is
         * overwritten by an invocation of put(k,v) for a key k that's already
         * in the HashMap.
         */
        void recordAccess(HashMap<K,V> m) {
        }

        /**
         * This method is invoked whenever the entry is
         * removed from the table.
         */
        void recordRemoval(HashMap<K,V> m) {
        }
    }



    private abstract class HashIterator<E> implements Iterator<E> {
        Entry<K,V> next;        // next entry to return
        int expectedModCount;   // For fast-fail
        int index;              // current slot
        Entry<K,V> current;     // current entry

        HashIterator() {
            expectedModCount = modCount;
            if (size > 0) { // advance to first entry
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();

            if ((next = e.next) == null) {
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
            current = e;
            return e;
        }

        public void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Object k = current.key;
            current = null;
            HashMap.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }
    }

    private final class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private final class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private final class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    Iterator<K> newKeyIterator()   {
        return new KeyIterator();
    }
    Iterator<V> newValueIterator()   {
        return new ValueIterator();
    }
    Iterator<Map.Entry<K,V>> newEntryIterator()   {
        return new EntryIterator();
    }

    private transient Set<Map.Entry<K,V>> entrySet = null;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return newKeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return HashMap.this.removeEntryForKey(o) != null;
        }
        public void clear() {
            HashMap.this.clear();
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private final class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return newValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            HashMap.this.clear();
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        return entrySet0();
    }

    private Set<Map.Entry<K,V>> entrySet0() {
        Set<Map.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return newEntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>) o;
            Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        public boolean remove(Object o) {
            return removeMapping(o) != null;
        }
        public int size() {
            return size;
        }
        public void clear() {
            HashMap.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();

        // Write out number of buckets
        if (table==EMPTY_TABLE) {
            s.writeInt(roundUpToPowerOf2(threshold));
        } else {
           s.writeInt(table.length);
        }

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        if (size > 0) {
            for(Map.Entry<K,V> e : entrySet0()) {
                s.writeObject(e.getKey());
                s.writeObject(e.getValue());
            }
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " +
                                               loadFactor);
        }

        // set other fields that need values
        table = (Entry<K,V>[]) EMPTY_TABLE;

        // Read in number of buckets
        s.readInt(); // ignored.

        // Read number of mappings
        int mappings = s.readInt();
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                               mappings);

        // capacity chosen by number of mappings and desired load (if >= 0.25)
        int capacity = (int) Math.min(
                    mappings * Math.min(1 / loadFactor, 4.0f),
                    // we have limits...
                    HashMap.MAXIMUM_CAPACITY);

        // allocate the bucket array;
        if (mappings > 0) {
            inflateTable(capacity);
        } else {
            threshold = capacity;
        }

        init();  // Give subclass a chance to do its thing.

        // Read the keys and values, and put the mappings in the HashMap
        for (int i = 0; i < mappings; i++) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            putForCreate(key, value);
        }
    }

    // These methods are used when serializing HashSets
    int   capacity()     { return table.length; }
    float loadFactor()   { return loadFactor;   }
}
