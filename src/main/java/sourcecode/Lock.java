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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;


public interface Lock {


    /**
     * 获取锁，成功直接返回，失败则阻塞
     */
    void lock();


    /**
     * 功能同lock()，阻塞响应中断
     */
    void lockInterruptibly() throws InterruptedException;


    /**
     * 尝试获取锁，成功返回true,失败返回false
     */
    boolean tryLock();


    /**
     * 功能同lockInterruptibly(),阻塞可以设置超时
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;


    /**
     * 释放锁
     */
    void unlock();


    /**
     * 获取条件队列
     */
    Condition newCondition();
}
