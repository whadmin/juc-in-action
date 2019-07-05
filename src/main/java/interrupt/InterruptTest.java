package interrupt;

import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class InterruptTest {


    /**
     * 当线程处于wait、sleep以及jion三个方法引起的阻塞，调用interrupt会将，那么会将线程的中断标志重新设置为false，并抛出一个InterruptedException
     * 当线程处于运行状态下，用interrupt会将，那么会将线程的中断标志设置为true
     */
    @Test
    public void test1() throws InterruptedException {
        /** 启动一个线程 **/
        TnterruptRunnable1 t1 = new TnterruptRunnable1();
        t1.start();
        t1.interrupt();
    }

    /**
     * 当线程interrupt中断LockSupport阻塞将抛出异常，中断不会重置依旧为true
     */
    @Test
    public void test2() throws InterruptedException {
        TnterruptRunnable4 t1 = new TnterruptRunnable4("t1");
        t1.start();
        t1.interrupt();
        Thread.sleep(5000);
    }

    @Test
    public void test3() throws InterruptedException {
        TnterruptRunnable3 t3 = new TnterruptRunnable3();
        t3.start();
        t3.interrupt();
    }

    @Test
    public void test4() throws InterruptedException {
        TnterruptRunnable2 t2 = new TnterruptRunnable2();
        t2.start();
        t2.interrupt();
    }
}

class TnterruptRunnable1 extends Thread {


    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(1000l);
            }
        } catch (InterruptedException e) {

        }
        System.out.println(isInterrupted());
        return;
    }
};

class TnterruptRunnable2 extends Thread {


    public void run() {
        while (true) {
            boolean isIn = this.isInterrupted();
            if (isInterrupted()) break;
        }
        System.out.println(isInterrupted());
    }
};


class TnterruptRunnable3 extends Thread {


    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(1000l);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(isInterrupted());
    }
};

class TnterruptRunnable4 extends Thread {
    public TnterruptRunnable4(String name) {
        super.setName(name);
    }

    @Override
    public void run() {
        synchronized (InterruptTest.class) {
            System.out.println("in " + getName());
            LockSupport.park();
            if (Thread.currentThread().isInterrupted()) {
                System.out.println(getName() + " 被中断了!");
                System.out.println(Thread.currentThread().isInterrupted());
            }
        }
        System.out.println(getName() + " 执行结束");
    }
}
