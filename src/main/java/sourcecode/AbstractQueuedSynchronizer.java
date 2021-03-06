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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;


/**
 * AQS 同步状态管理器
 */
public abstract class AbstractQueuedSynchronizer

    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;


    /**
     * 构建同步队列
     */
    protected AbstractQueuedSynchronizer() { }


    /**
     * 用来作为同步队列和等待队列的节点对象.
     * 作为不同队列的节点.其使用的属性和含义是不同的.
     */
    static final class Node {
        /** 共享同步节点标识 */
        static final Node SHARED = new Node();
        /** 独占同步节点标识 */
        static final Node EXCLUSIVE = null;

        /** 作为同步队列节点时，表示当前节点为取消状态（线程因为超时或者中断），
         *  被取消的节点时不会参与到竞争中的，他会一直保持取消状态不会转变为其他状态；
         * */
        static final int CANCELLED =  1;
        /** 作为同步队列节点时，表示当前节点后继节点处于等待状态，
         *  如果当前节点获取同步状态，会将后继节点从等待唤醒，去竞争同步状态
         * */
        static final int SIGNAL    = -1;

        /** 作为等待队列节点时，表示该节点需要被唤醒 */
        static final int CONDITION = -2;
        /**
         * 作为同步队列节点时，在共享式同步释放时，会将节点设置为此状态，并一直传播通知后续节点停止阻塞。尝试获取锁。
         */
        static final int PROPAGATE = -3;

        /** 等待状态 */
        volatile int waitStatus;

        /** 同步队列节点时使用，前置节点指针 */
        volatile Node prev;

        /** 同步队列节点时使用，后置节点指针 */
        volatile Node next;

        /** 节点中保存的线程 */
        volatile Thread thread;

        /** 等待队列节点时使用，后置节点指针**/
        Node nextWaiter;


        /**
         * 当前节点是共享式同步在同步队列中的节点
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }


        /** 获取当前节点的前置节点 **/
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        /**创建同步队列节点,node表示节点类似Node.SHARED或Node.EXCLUSIVE**/
        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        /** 创建等待队列节点，waitStatus传入Node.CONDITION **/
        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }


    /**
     * 同步队列头节点
     */
    private transient volatile Node head;


    /**
     * 同步队列尾节点
     */
    private transient volatile Node tail;


    /**
     * 同步状态
     */
    private volatile int state;


    /**
     * 获取同步状态
     */
    protected final int getState() {
        return state;
    }


    /**
     * 设置同步状态
     */
    protected final void setState(int newState) {
        state = newState;
    }


    /**
     *  CAS设置同步状态
     */
    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    static final long spinForTimeoutThreshold = 1000L;


    /**
     * 如果同步队列未初始化，则初始化
     * 将新节点添加到同步队尾
     */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) {
                /** 构造头节点尾节点指向一个空节点 **/
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                /**node节点插入同步队列尾，CAS+循环操作失败重新操作直到成功 **/
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }


    /**
     * 创建节点node
     * 如果同步队列已初始化则将当前节点添加到同步队列尾部,
     * 如果同步队列没有初始化则需要调用子函数enq，enq会创建一个同步队列并将当前节点添加到同步队列尾部.
     */
    private Node addWaiter(Node mode) {
        /**  1. 将当前线程构建成Node **/
        Node node = new Node(Thread.currentThread(), mode);
        /** 2. 将尾节点设置给一个中间引用变量pred **/
        Node pred = tail;
        /**  3. 判断尾节点是否为null,如果为null说明同步队列未初始化 **/
        if (pred != null) {
            /**   将当前节点插入同步队列尾部 **/
            /**   将当前节点前置节点为pred  **/
            node.prev = pred;
            /**   使用cas将尾部节点引用到插入节点 **/
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        /** 同步队列未初始化 **/
        enq(node);
        return node;
    }


    /**
     * 将传入节点设置未同步队列头部节点
     * 头部节点内部不存在等待线程
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }


    /**
     * 更新同步队列Head节点的等待状态，将Head节点后置节点中线程从阻塞状态中唤醒
     */
    private void unparkSuccessor(Node node) {
        /**
         * 将Head节点的等待状态设置为0
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /** 默认情况下释放的节点为head节点后置节点s. **/
        /**
         * 如果head节点后置节点等待状态为1(取消),从尾节点开始遍历寻找最接近head节点等待状态为-1的节点作为释放节点s
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        /** 唤醒s节点中线程阻塞 **/
        if (s != null)
            LockSupport.unpark(s.thread);
    }


    /**
     * 释放head节点后置节点线程重阻塞中唤醒，
     *
     * 同时会检查释放节点的线程是否会获取同步状态，如果获取则在次释放释放节点的下一个节点中的线程从阻塞中唤醒，重复迭代
     */
    private void doReleaseShared() {
        /** 进入自旋 **/
        for (;;) {
         /**获取当前head节点赋值给变量h,判断等待队列中是否存在等待节点,不存在进入步骤4直接退出. **/
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                 /**如果当前节点的状态为-1(存在等待的后置节点),使用CAS设置其为0(使用CAS失败进入自旋重新设置)同时会唤醒head后置节点中线程从阻塞中释放. **/
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);
                }
                /**判断唤醒线程是否获取同步状态,获取同步状态同步队列head节点引用会发生改变，则释放被释放节点后置节点的线程中阻塞中唤醒竞争同步状态 **/
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            /**判断唤醒线程是否获取同步状态,获取同步状态同步队列head节点引用会发生改变，则释放被释放节点后置节点的线程中阻塞中唤醒竞争同步状态 **/
            if (h == head)                   // loop if head changed
                break;
        }
    }


    /**
     * 将当前节点设置为head,同时只要队列中存在等待的节点,且节点为共享节点则会调用doReleaseShared,唤醒head节点后置节点阻塞去竞争同步状态.
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);

        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }


    /**
     * 将当前节点等待状态设置未取消
     */
    private void cancelAcquire(Node node) {
        if (node == null)
            return;

        node.thread = null;

        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        Node predNext = pred.next;


        node.waitStatus = Node.CANCELLED;

        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }


    /**
     * 获取同步状态自旋设置节点等待状态
     * 返回 true 调用parkAndCheckInterrupt()阻塞当前线程
     * 返回 false 则重新进入acquireQueued自旋
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        /** 前置节点状态为-1 返回true 准备直塞当前节点线程  */
        if (ws == Node.SIGNAL)

            return true;
        /** 前置节点状态为 1，剔除出队列，在外层自悬循环    中从新开始查找  返回false */
        if (ws > 0) {
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        }
        /** 前置节点状态为 0，设置为 -1  在外层自悬循环    中从新开始查找  返回false */
        else {
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 中断当前线程
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 阻塞当前线程（可响应中断）
     * 返回true 表示中断导致线程阻塞被唤醒
     */
    private final boolean parkAndCheckInterrupt() {
        /** 阻塞当前线程（可响应中断）**/
        LockSupport.park(this);
        /** 如果线程是中断从阻塞唤醒返回true **/
        return Thread.interrupted();
    }


    /**
     * 自旋,找到头部后置第一个节点，尝试获取同步状态,成功则设置其为新head节点.失败则阻塞.
     */
    final boolean acquireQueued(final Node node, int arg) {
        /** 执行是否发生异常 **/
        boolean failed = true;
        try {
            /** 标识是否被中断 **/
            boolean interrupted = false;
            /** 进入自旋 **/
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                 /** 如果当前节点的先驱节点是头结点并且成功获取同步状态，即可以获得独占式锁  **/
                if (p == head && tryAcquire(arg)) {
                     /** 并将当前节点设置为head节点  **/
                    setHead(node);
                     /** 释放当前节前驱节点指针(这里前驱节点也相当于原始的head节点)等待GC回收  **/
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                 /** 获取锁失败，在shouldParkAfterFailedAcquire中设置节点的等待状态，并线程阻塞（可响应线程被中断）,
                  * 如果是中断响应设置interrupted = true;
                  * 重新进入自旋**/
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            /** 发生异常失败  **/
            if (failed)
            /** 设置当前节点状态为消息  **/
                cancelAcquire(node);
        }
    }

    /**
     * 独占获取同步状态，响应中断
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        /** addWaiter创建一个独占式节点node,添加到同步队列尾部.**/
        final Node node = addWaiter(Node.EXCLUSIVE);
        /** 执行是否发生异常 **/
        boolean failed = true;
        try {
            /** 进入自旋 **/
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                /** 如果当前节点的先驱节点是头结点并且成功获取同步状态 **/
                if (p == head && tryAcquire(arg)) {
                    /** 并将当前节点设置为head节点  **/
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                /** 2.2 获取锁失败，线程阻塞（可响应线程被中断）,  如果是中断响应设置interrupted = true;
                 * 抛出异常，中断导致退出自旋线程不在等待！！**/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            /** 发生异常，将当前节点等待状态设置为取消**/
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 独占获取同步状态，响应中断,同时增加超时机制
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        /** 计算超时绝对时间ns **/
        final long deadline = System.nanoTime() + nanosTimeout;
        /** addWaiter创建一个独占式节点node,添加到同步队列尾部.**/
        final Node node = addWaiter(Node.EXCLUSIVE);
        /** 执行是否发生异常 **/
        boolean failed = true;
        try {
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                /** 如果当前节点的先驱节点是头结点并且成功获取同步状态 **/
                if (p == head && tryAcquire(arg)) {
                    /** 并将当前节点设置为head节点  **/
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                /** 计算有无超时，如超时则退出 ！！！**/
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                /** 获取锁失败，在shouldParkAfterFailedAcquire中设置节点的等待状态，并线程阻塞（可响应线程被中断）,
                 * 如果是中断响应设置interrupted = true;
                 * 抛出异常，中断导致退出自旋线程不在等待！！
                 * 如果阻塞超时被唤醒，进入自旋并判断超时后退出自旋
                 * **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            /** 发生异常，将当前节点等待状态设置为取消**/
            if (failed)
                cancelAcquire(node);
        }
    }


    /**
     * 创建一个共享式节点node,添加到同步队列尾部.
     * 进入自旋,找到CLH头部后置第一个节点，尝试获取同步状态,成功则设置其为新head节点，
     * 并通知后置节点线程从阻塞中唤醒竞争同步状态.失败则阻塞.
     */
    private void doAcquireShared(int arg) {
        /** 创建一个共享式节点node,添加到同步队列尾部..**/
        final Node node = addWaiter(Node.SHARED);
        /** 执行是否发生异常 **/
        boolean failed = true;
        try {
            /** 标识是否被中断 **/
            boolean interrupted = false;
            /** 进入自旋 **/
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                if (p == head) {
                    /** 如果当前节点的先驱节点是头结点并且成功获取同步状态 **/
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        /** 将当前节点设置为head,同时只要同步队列中存在等待的节点,
                         * 且节点为共享节点则唤醒head节点后置节点阻塞去竞争同步状态. **/
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        /** 如果当前线程中断 **/
                        if (interrupted)
                            /** 中断当前线程 **/
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                /**获取锁失败，在shouldParkAfterFailedAcquire中设置节点的等待状态，并线程阻塞（可响应线程被中断,有超时阻塞）,
                 * 如果是中断响应设置interrupted = true;
                 * 重新进入自旋**/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            /** 发生异常，将当前节点等待状态设置为取消**/
            if (failed)
                cancelAcquire(node);
        }
    }


    /**
     * 功能同doAcquireShared，可响应中断
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        /** 创建一个共享式节点node,添加到同步队列尾部..**/
        final Node node = addWaiter(Node.SHARED);
        /** 执行是否发生异常 **/
        boolean failed = true;
        try {
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                if (p == head) {
                    /** 如果当前节点的先驱节点是头结点并且成功获取同步状态 **/
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        /** 将当前节点设置为head,同时只要同步队列中存在等待的节点,
                         * 且节点为共享节点则唤醒head节点后置节点阻塞去竞争同步状态. **/
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                /** 获取锁失败，在shouldParkAfterFailedAcquire中设置节点的等待状态，并线程阻塞（可响应线程被中断）,
                 * 如果是中断响应设置interrupted = true;
                 * 抛出异常，中断导致退出自旋线程不在等待！！
                 * **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            /** 发生异常，将当前节点等待状态设置为取消**/
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 功能同doAcquireSharedInterruptibly，添加超时等待
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        /** 超时时间小于0 直接返回 **/
        if (nanosTimeout <= 0L)
            return false;
        /** 计算超时绝对时间ns **/
        final long deadline = System.nanoTime() + nanosTimeout;
        /** 创建一个共享式节点node,添加到同步队列尾部..**/
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                /** 1. 获得当前节点的先驱节点  **/
                final Node p = node.predecessor();
                /** 如果当前节点的先驱节点是头结点并且成功获取同步状态 **/
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        /** 将当前节点设置为head,同时只要同步队列中存在等待的节点,
                         * 且节点为共享节点则唤醒head节点后置节点阻塞去竞争同步状态. **/
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                /** 计算有无超时，如超时则退出 ！！！**/
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                /** 获取锁失败，在shouldParkAfterFailedAcquire中设置节点的等待状态，并线程阻塞（可响应线程被中断）,
                 * 如果是中断响应设置interrupted = true;
                 * 抛出异常，中断导致退出自旋线程不在等待！！
                 * 如果阻塞超时被唤醒，进入自旋并判断超时后退出自旋
                 * **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            /** 发生异常，将当前节点等待状态设置为取消**/
            if (failed)
                cancelAcquire(node);
        }
    }


    /** 模板方法，尝试独占式获取同步状态,返回值为true则表示获取成功，否则获取失败。 **/
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /** 模板方法，尝试独占式释放同步状态，返回值为true则表示获取成功，否则获取失败。 **/
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /** 模板方法，尝试共享式获取同步状态,当返回值为大于等于0的时获得同步状态成功，否则获取失败。 **/
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /** 模板方法，尝试共享式释放同步状态，返回值为true则表示获取成功，否则获取失败。*/
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }


    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }


    /**
     * 独占式获取同步状态,如果当前线程获取同步状态成功则直接返回，
     * 如果获取失败则线程阻塞，并插入同步队列进行.等待调用release
     * 释放同步状态时，重新尝试获取同步状态。成功则返回，失败则阻塞等待下次release
     */
    public final void acquire(int arg) {
        /**
         *子类实现tryAcquire能否获取的独占式同步状态
         *如果返回true则获取同步状态成功方法直接返回
         *如果返回false则获取同步状态失败进入if语句
         */
        if (!tryAcquire(arg) &&
                /** addWaiter创建一个独占式节点node,添加到同步队列尾部. */
                /** acquireQueued自旋,同步队列头部后置第一个节点线程尝试获取同步状态,成功则设置其为head节点.失败则阻塞 */
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * 功能同acquire，可以响应线程中断
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        /** 测试当前线程是否已经中断（如果已经中断会将中断标识重新设置为false）**/
        if (Thread.interrupted())
            /** 抛出异常 **/
            throw new InterruptedException();
        /**
         *子类实现tryAcquire能否获取的独占式同步状态
         *如果返回true则获取同步状态成功方法直接返回
         *如果返回false则获取同步状态失败进入if语句
         */
        if (!tryAcquire(arg))
            /** 创建一个独占式节点node,添加到同步队列尾部.进入自旋,同步队列头部后置第一个节点线程尝试获取同步状态,成功则设置其为head节点.失败则阻塞
             * 如果发生中断，抛出异常线程退出自旋 **/
            doAcquireInterruptibly(arg);
    }

    /**
     * 功能同acquireInterruptibly，增加等待超时
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        /** 测试当前线程是否已经中断（如果已经中断会将中断标识重新设置为false）**/
        if (Thread.interrupted())
            /** 抛出异常 **/
            throw new InterruptedException();

        /**
         *子类实现tryAcquire能否获取的独占式同步状态
         *如果返回true则获取同步状态成功方法直接返回
         *如果返回false则获取同步状态失败进入if语句
         */
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }


    /**
     * 释放独占式同步入口函数，
     * 参数arg传递给模板方法用来判断释放同步状态
     * 释放同步状态会释放Head节点后置节点中线程从阻塞状态中唤醒
     */
    public final boolean release(int arg) {
        /**
         *子类实现能否释放的独占式同步状态
         *如果返回true则表示释放同步状态准入条件成功进入if语句
         *如果返回false则表示释放同步状态失败返回false
         */
        if (tryRelease(arg)) {
            /** 判断同步队列是否存在等待节点 **/
            Node h = head;
            if (h != null && h.waitStatus != 0)
                /**
                 * 更新同步队列Head节点的等待状态，将Head节点后置节点中线程从阻塞状态中唤醒
                 */
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 共享式获取同步状态,如果当前线程获取同步状态成功则直接返回，
     * 如果获取失败则线程阻塞，并插入同步队列进行.等待调用releaseShared
     * 释放同步状态时，重新尝试获取同步状态。成功则，同时会通知后置节点线程从阻塞中唤醒，
     * 获取同步状态并返回，失败则阻塞等待下次release
     */
    public final void acquireShared(int arg) {
        /**
         *子类实现tryAcquireShared能否获取的共享式同步状态
         *如果返回>=0则获取同步状态成功方法直接返回
         *如果返回< 0则获取同步状态失败进入if语句
         */
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * 功能同acquireShared，可以响应线程中断
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        /** 测试当前线程是否已经中断（如果已经中断会将中断标识重新设置为false）**/
        if (Thread.interrupted())
        /** 抛出异常 **/
            throw new InterruptedException();
        /**
         *子类实现tryAcquireShared能否获取的共享式同步状态
         *如果返回>=0则获取同步状态成功方法直接返回
         *如果返回< 0则获取同步状态失败进入if语句
         */
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * 功能同acquireSharedInterruptibly，可以响应线程中断
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        /** 测试当前线程是否已经中断（如果已经中断会将中断标识重新设置为false）**/
        if (Thread.interrupted())
            /** 抛出异常 **/
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }


    /**
     * 释放共享式同步入口函数，
     * 参数arg传递给模板方法用来判断释放同步状态
     *
     * 释放同步状态会释放Head节点后置节点中线程从阻塞状态中唤醒。
     * 同时会检查释放节点的线程是否会获取同步状态，如果获取则在次释放释放节点的下一个节点中的线程从阻塞中唤醒，重复迭代
     */
    public final boolean releaseShared(int arg) {
        /**
         *子类实现能否释放的共享式同步状态
         *如果返回true则表示释放同步状态准入条件成功进入if语句
         *如果返回false则表示释放同步状态失败返回false
         */
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }


    /**
     * 在同步队列中是否存在等待线程
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }


    /**
     * 同步队列head节点是否存在
     */
    public final boolean hasContended() {
        return head != null;
    }


    /**
     * 获取同步队列中第一个等待的线程
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }


    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }


    /**
     * 传入的线程是否在同步队列等待
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }


    /**
     * 同步队列的哥等待的节点是独占的
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }


    /**
     * 当前线程的节点是否为head节点后置节点
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }



    /**
     * 获取同步队列的长度
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }


    /**
     * 获取同步队列中等待的线程
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * 获取同步队列中等待节点为独占的线程
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 获取同步队列中等待节点为共享的线程
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }


    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }



    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        return findNodeFromTail(node);
    }

    /**
     *
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     *
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * 将传入节点的等待状态从Node.CONDITION设置0，加入同步队列
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * 释放同步状态，并返回释放前的同步状态值。
     * 如果失败将传入的节点等待状态设置为取消
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions


    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }


    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }


    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }


    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }


    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * Removes and transfers all nodes.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }


    private static final Unsafe unsafe = Unsafe.getUnsafe();
    /**
     * 状态在内存中的偏移位置
     */
    private static final long stateOffset;
    /**
     * 同步队列头节点在内存中的偏移位置
     */
    private static final long headOffset;
    /**
     * 同步队列尾节点在内存中的偏移位置
     */
    private static final long tailOffset;
    /**
     * 节点等待状态在内存中的偏移位置
     */
    private static final long waitStatusOffset;
    /**
     * 节点next节点在内存中的偏移位置
     */
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * 使用CAS 初始化同步对了头节点
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * 使用CAS,更新同步队列的头节点
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * 使用CAS 更新节点中等待状态waitStatus
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * 使用CAS 更新节点中next
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
