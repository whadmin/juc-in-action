package interrupt;

import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class InterruptTest {


    /**
     * 当线程处于wait、sleep以及jion三个方法引起的阻塞，调用interrupt会将线程中断标志设置true,同时从阻塞中唤醒，在重新设置中断标记设置false，并抛出一个InterruptedException
     *
     */
    @Test
    public void test1() throws InterruptedException {
        TnterruptRunnable1 t1 = new TnterruptRunnable1();
        t1.start();
        t1.interrupt();
    }

    /**
     * 当线程处于运行状态下，调用interrupt会将线程中断标志设置true
     */
    @Test
    public void test2() throws InterruptedException {
        TnterruptRunnable2 t1 = new TnterruptRunnable2();
        t1.start();
        t1.interrupt();
    }

    /**
     * 当线程处于LockSupport阻塞，调用interrupt会将线程中断标志设置true,同时从阻塞中唤醒，并抛出一个InterruptedException
     */
    @Test
    public void test3() throws InterruptedException {
        TnterruptRunnable4 t1 = new TnterruptRunnable4();
        t1.start();
        t1.interrupt();
    }

    @Test
    public void test4() throws InterruptedException {
        TnterruptRunnable3 t3 = new TnterruptRunnable3();
        t3.start();
        t3.interrupt();
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


    @Override
    public void run() {
        synchronized (InterruptTest.class) {
            LockSupport.park();
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                System.out.println(Thread.currentThread().isInterrupted());
            }
        }
    }
}
