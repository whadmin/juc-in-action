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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;


public class CountDownLatch {


    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        /** 实例化Sync，设置同步状态 **/
        Sync(int count) {
            setState(count);
        }

        /** 获取同步状态 **/
        int getCount() {
            return getState();
        }

        /**
         * 获取同步状态
         * 同步状态为0时释放获取同步状态成功，否则失败
         */
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        /**
         * 释放同步状态
         *
         * 使用CAS+循环将同步状态-1
         */
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;


    /**
     * 创建一个闭锁，设置需要多少次通知，才能从阻塞中唤醒
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }


    /**
     * 阻塞等待唤醒
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }


    /**
     * 阻塞等待唤醒，添加超时机制
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }


    /**
     * 释放一次通知
     */
    public void countDown() {
        sync.releaseShared(1);
    }


    /**
     * 获取同步状态
     */
    public long getCount() {
        return sync.getCount();
    }


    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
