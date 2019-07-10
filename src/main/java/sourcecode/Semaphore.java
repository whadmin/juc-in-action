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


public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    private final Sync sync;

    /**
     * Sync 继承 AbstractQueuedSynchronizer 抽象类
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        /**
         * 初始化同步状态
         */
        Sync(int permits) {
            setState(permits);
        }

        /**
         * 返回同步状态
         */
        final int getPermits() {
            return getState();
        }

        /**
         * 非公平信号量获取同步状态逻辑
         * 返回true 表示获取同步状态成功
         * 返回false 表示获取同步状态失败
         */
        final int nonfairTryAcquireShared(int acquires) {
            /** 循环+CAS **/
            for (;;) {
                /** 获取父类同步状态 **/
                int available = getState();
                /** 计算同步状态 -acquires **/
                int remaining = available - acquires;
                /** 使用CAS设置同步状态， **/
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        /**
         * 释放同步状态
         */
        protected final boolean tryReleaseShared(int releases) {
            /** 循环+CAS **/
            for (;;) {
                /** 获取父类同步状态 **/
                int current = getState();
                /** 计算同步状态 +releases **/
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                /** 使用CAS设置同步状态， **/
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        /**
         * 清理指定数量许可，修改同步状态
         */
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        /**
         * 清空许可，修改同步状态
         */
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }


    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        /**
         * 获取同步状态
         */
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }


    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        /**
         * 公平信号量获取同步状态逻辑
         */
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                /** 只有同步队列中不存在线程。且同步状态可以获取才能获取锁 **/
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**  创建具有给定的许可数和非公平的公平设置的 Semaphore。 **/
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**  创建具有给定的许可数和给定的公平设置的 Semaphore。 **/
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }



    /** 获取一个许可，获取失败将线程添加到同步队列，并阻塞，等待归还唤醒  **/
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /** 和acquireUninterruptibly功能，同时能响应中断  **/
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /** 和acquireUninterruptibly功能，同时增加阻塞超时  **/
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /** 尝试获取许可，如果信号量中不存在许可之际返回false,成功返回true **/
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /** 获取指定数量的许可，获取失败将线程添加到同步队列，并阻塞，等待归还唤醒  **/
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /** 和acquireUninterruptibly(int )功能，同时能响应中断  **/
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /** 和acquire(int)功能，同时增加阻塞超时  **/
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }


    /** 尝试指定数量的许可，如果信号量中不存在许可之际返回false,成功返回true **/
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }


    /** 释放一个许可， **/
    public void release() {
        sync.releaseShared(1);
    }


    /** 释放指定数量许可， **/
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }


    /**
     * 获取许可的数量
     */
    public int availablePermits() {
        return sync.getPermits();
    }


    /**
     * 清空许可
     */
    public int drainPermits() {
        return sync.drainPermits();
    }


    /**
     * 根据指定的缩减量减小可用许可的数目
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * 判断当前对象是否是公平信号量
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }


    /**
     * 判断是否有线程在同步队列等待许可
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }


    /**
     * 获取等待许可的线程数量
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }


    /**
     * 获取等待许可的线程集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }


    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
