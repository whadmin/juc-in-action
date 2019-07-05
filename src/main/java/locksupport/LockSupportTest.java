package locksupport;

import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class LockSupportTest {

    @Test
    public void park1(){
        LockSupport.park();
    }

    @Test
    public void park2(){
        long star=System.currentTimeMillis();
        LockSupport.parkNanos(5000000000L);
        System.out.println(System.currentTimeMillis()-star);
        System.out.println(Thread.interrupted());
    }

    @Test
    public void park3(){
        long star=System.currentTimeMillis();
        LockSupport.parkUntil(System.currentTimeMillis()+5000L);
        System.out.println(System.currentTimeMillis()-star);
    }


    @Test
    public void park5() {
        Thread currThread = Thread.currentThread();
        LockSupport.unpark(currThread);
        LockSupport.unpark(currThread);
        LockSupport.unpark(currThread);
        LockSupport.park();
        LockSupport.park();
        System.out.println("execute success"); // 线程挂起，不会打印。
    }
}
