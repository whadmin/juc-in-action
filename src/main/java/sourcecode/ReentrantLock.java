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

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;


    private final Sync sync;

    /**
     * Sync 继承 AbstractQueuedSynchronizer 抽象类
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * 模板方法子类去实现获取锁
         */
        abstract void lock();

        /**
         * 非公平锁获取同步状态逻辑
         *  返回true 表示获取同步状态成功
         *  返回false 表示获取同步状态失败
         */
        final boolean nonfairTryAcquire(int acquires) {
            /** 获取当前线程 **/
            final Thread current = Thread.currentThread();
            /** 获取父类同步状态 **/
            int c = getState();
            /** 同步状态为0 表示锁没有被占用 **/
            if (c == 0) {
                /** 使用CAS设置同步状态为1， **/
                if (compareAndSetState(0, acquires)) {
                    /** 将当前线程设置为exclusiveOwnerThread **/
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            /** 同步状态为1，表示锁被占用，判断占用锁的线程是否是当前线程,锁重入  **/
            else if (current == getExclusiveOwnerThread()) {
                /** 同步状态+1 **/
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                /** 设置同步状态 **/
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * 释放锁
         */
        protected final boolean tryRelease(int releases) {
            /** 重新计算当前同步状态 **/
            int c = getState() - releases;
            /** 校验当前线程是否拥有锁 **/
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            /** 只能当同步状态为0时，重置exclusiveOwnerThread，释放同步状态成功 **/
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            /** 设置同步状态 **/
            setState(c);
            return free;
        }

        /**
         * 判断当前线程是否获取锁
         */
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        /**
         * 创建一个条件队列
         */
        final ConditionObject newCondition() {
            return new ConditionObject();
        }


        /**
         * 返回占由有锁的线程
         */
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        /**
         * 获取拥有锁的线程的重入次数
         */
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        /**
         * 尝试能够获取锁，
         */
        final boolean isLocked() {
            return getState() != 0;
        }


        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * NonfairSync（非公平锁） 继承 Sync 抽象类。
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * 非公平获取锁
         */
        final void lock() {
            /** 使用CAS 将同步状态设置为1，成功则将当前线程设置为exclusiveOwnerThread**/
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                /** 调用AQS 获取锁 **/
                acquire(1);
        }

        /**
         * 能否获取非公平锁
         */
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * FairSync（公平锁） 继承 Sync 抽象类。
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        /**
         * 获取锁
         */
        final void lock() {
            acquire(1);
        }

        /**
         * 能否获取公平锁
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            /** 只有同步队列中不存在线程。且同步状态可以获取才能获取锁 **/
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            /**  同步状态为1，表示锁被占用，判断占用锁的线程是否是当前线程,锁重入  **/
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * 实例化ReentrantLock 默认非公平的锁
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 实例化一个ReentrantLock，设置是否为公平锁
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获取锁，失败则进入同步队列，并阻塞
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 功能同lock()，阻塞响应中断
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }


    /**
     * 尝试获取锁，成功返回true,失败返回false
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 功能同lockInterruptibly(),阻塞可以设置超时
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放锁，同步队列前面的线程从阻塞中唤醒，在次尝试获取锁，成功返回，失败继续阻塞等待唤醒
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 获取条件队列
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 获取拥有锁的线程的重入次数
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 判断当前线程是否获取锁
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 尝试能够获取锁，
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 判断当前对象是否时公平锁
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回占由有锁的线程
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 判断同步队列是否初始化
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 判断线程是否在同步队列中
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 获取等待锁的线程数量
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 获取等待锁的线程集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     *
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     *
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     *
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     *
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
