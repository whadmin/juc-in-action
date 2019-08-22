/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.*;


/**
 * 是一个双向链表结构，是实现了List,Deque
 */
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    /**
     * 记录当前链表的长度
     */
    transient int size = 0;

    /**
     * 第一个节点
     */
    transient Node<E> first;

    /**
     * 最后一个节点
     */
    transient Node<E> last;

    /**
     * 实例化LinkedList
     */
    public LinkedList() {
    }

    /**
     * 实话化LinkedList，将指定集合中元素添加到LinkedList中
     */
    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * 将一个元素添加到链表头部
     */
    private void linkFirst(E e) {
        /** 先将原链表头部暂存给变量f **/
        final Node<E> f = first;
        /** 定义一个新节点，其next指向f **/
        final Node<E> newNode = new Node<>(null, e, f);
        /** 将fist指向新添加节点 **/
        first = newNode;
        /** 如果原链表为空表，把last也指向新添加节点 **/
        if (f == null)
            last = newNode;
        else
        /** 将f的prev指向新添加节点 **/
            f.prev = newNode;
        /** 数量+1 **/
        size++;
        /** 修改次数+1 **/
        modCount++;
    }

    /**
     * L添加元素到链表尾部
     */
    void linkLast(E e) {
        /** 先将原链表头部暂存给变量l **/
        final Node<E> l = last;
        /** 定义一个新节点，其prev指向l **/
        final Node<E> newNode = new Node<>(l, e, null);
        /** 将last指向新添加节点 **/
        last = newNode;
        /** 如果原链表为空表，把first也指向新添加节点 **/
        if (l == null)
            first = newNode;
        else
            /** 将f的prev指向新添加节点 **/
            l.next = newNode;
        /** 数量+1 **/
        size++;
        /** 修改次数+1 **/
        modCount++;
    }

    /**
     * 在某个非空节点之前添加元素,suc表示指定的非空节点
     */
    void linkBefore(E e, Node<E> succ) {
        /** 先将succ节点的前置节点暂存给变量 pred **/
        final Node<E> pred = succ.prev;
        /** 定义一个新节点，其prev指向pred **/
        final Node<E> newNode = new Node<>(pred, e, succ);
        /** succ的prev指向新添加节点 **/
        succ.prev = newNode;
        /** 如果指定节点为fist节点，部存在前置节点 **/
        if (pred == null)
            /** 将first指向新添加节点 **/
            first = newNode;
        else
            /** pred的next指向新添加节点 **/
            pred.next = newNode;
        /** 数量+1 **/
        size++;
        /** 修改次数+1 **/
        modCount++;
    }

    /**
     * 移除链表first节点，并返回其中数据对象
     *
     * 传入的f表示为first节点
     */
    private E unlinkFirst(Node<E> f) {
        /** 将first节点的数据暂存给变量 element**/
        final E element = f.item;
        /** 将first节点的next暂存给变量 next**/
        final Node<E> next = f.next;
        /** 设置first属性item为null (等待gc回收)**/
        f.item = null;
        /** 设置first的next为null (等待gc回收)**/
        f.next = null; // help GC
        /** first指向next,原始fist出队完毕 **/
        first = next;
        /** 如果原链表只保存一个节点 **/
        if (next == null)
            /** last设置为null **/
            last = null;
        else
            /** next的prev设置为null(next为新的fist节点完毕) **/
            next.prev = null;
        /** 数量-1 **/
        size--;
        /** 修改次数+1 **/
        modCount++;
        return element;
    }

    /**
     * 移除链表tail节点，并返回其中数据对象
     *
     * 传入的l表示为last节点
     */
    private E unlinkLast(Node<E> l) {
        /** 将last节点的数据暂存给变量 element**/
        final E element = l.item;
        /** 将last节点的prev暂存给变量 prev**/
        final Node<E> prev = l.prev;
        /** 设置last属性item为null (等待gc回收)**/
        l.item = null;
        /** 设置last的prev为null (等待gc回收)**/
        l.prev = null;
        /** last指向prev,原始last出队完毕 **/
        last = prev;
        /** 如果原链表只保存一个节点 **/
        if (prev == null)
             /** first设置为null **/
            first = null;
        else
            /** prev的next设置为null(prev为新的last节点完毕) **/
            prev.next = null;
        /** 数量-1 **/
        size--;
        /** 修改次数+1 **/
        modCount++;
        return element;
    }

    /**
     * 移除链表某个节点，并返回
     */
    E unlink(Node<E> x) {
        /** 将last节点的数据暂存给变量 element，next，prev**/
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        /** 如果移除节点为fist节点**/
        if (prev == null) {
            /** first指向next  **/
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        /** 如果移除节点为next节点**/
        if (next == null) {
            /** last指向prev  **/
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }

        /** 设置移除对象数据引用为null **/
        x.item = null;
        /** 数量-1 **/
        size--;
        /** 修改次数+1 **/
        modCount++;
        return element;
    }

    /**
     * Deque接口实现
     * 获取头节点fist内数据，如果队列为空，抛出异常
     */
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }


    /**
     * Deque接口实现
     * 获取队列点fist内数据，如果队列为空，直接返回null
     */
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }


    /**
     * Deque接口实现
     * 获取队尾节点内数据，如果队列为空，抛出异常
     */
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    /**
     * Deque接口实现
     * 获取队尾节点内数据，如果队列为空，直接返回null
     */
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }



    /**
     * Deque接口实现
     * 删除队首元素，如果队列为空,抛出异常
     */
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    /**
     * Deque接口实现
     * 删除队首元素，如果队列为空，直接返回null
     */
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }


    /**
     * Deque接口实现
     * 删除队尾元素，如果队列为空，抛出异常
     */
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }


    /**
     * Deque接口实现
     * 删除队尾元素，如果队列为空，直接返回null
     */
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    /**
     * Deque接口实现
     * 在队首添加元素
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Deque接口实现
     * 在队首添加元素,成功返回true
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Deque接口实现
     * 在队尾添加元素
     */
    public void addLast(E e) {
        linkLast(e);
    }


    /**
     * Deque接口实现
     * 在队尾添加元素,成功返回true
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * 判断链表是否包含此元素
     */
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /**
     * 返回链表中元素个数
     */
    public int size() {
        return size;
    }

    /**
     * Queue接口实现
     * 将元素插入队列尾部，成功返回true
     */
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    /**
     * Queue接口实现
     * 将元素插入队列尾部
     */
    public boolean offer(E e) {
        return add(e);
    }

    /**
     * Queue接口实现
     * 将队首的元素删除，如果队列为空,抛出异常
     */
    public E remove() {
        return removeFirst();
    }


    /**
     * Queue接口实现
     * 将队首的元素删除，如果队列为空,返回null
     */
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Queue接口实现
     * 获取队首元素，但不移除，队列为空则抛出异常
     */
    public E element() {
        return getFirst();
    }


    /**
     * 获取队首元素，但不移除，队列为空则返回null
     */
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }


    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        Node<E> pred, succ;
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }

        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }

        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }

        size += numNew;
        modCount++;
        return true;
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }


    // Positional Access Operations

    /**
     * List接口实现
     * 获取指定位置的元素
     */
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    /**
     * 返回一个元素在集合中首次出现的位置
     */
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    /**
     * 返回一个元素在集合中最后一次出现的位置
     */
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    /**
     * List接口实现
     * 指定下标修改某个元素值
     */
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    /**
     * List接口实现
     * 可以在指定下标位置添加单个元素到列表中
     */
    public void add(int index, E element) {
        checkPositionIndex(index);

        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }

    /**
     * List接口实现
     * 删除指定某个下标中的元素，并返回
     */
    public E remove(int index) {
        checkElementIndex(index);
        /** 返回指定元素索引处的（非null）节点，移除 **/
        return unlink(node(index));
    }



    /**
     * Tells if the argument is the index of an existing element.
     */
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    /**
     * Tells if the argument is the index of a valid position for an
     * iterator or an add operation.
     */
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 返回指定元素索引处的（非null）节点
     */
    Node<E> node(int index) {
        // assert isElementIndex(index);

        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations



    // Queue operations.




    /**
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this list.  In other
     * words, removes and returns the first element of this list.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     *         of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     * @since 1.6
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of {@code List.listIterator(int)}.<p>
     *
     * The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param index index of the first element to be returned from the
     *              list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see List#listIterator(int)
     */
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned = null;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    /**
     * @since 1.6
     */
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    /**
     * Adapter to provide descending iterators via ListItr.previous
     */
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *
     * @return a shallow copy of this {@code LinkedList} instance
     */
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list
     *         in proper sequence
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *
     * @serialData The size of the list (the number of elements it
     *             contains) is emitted (int), followed by all of its
     *             elements (each an Object) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }
}
