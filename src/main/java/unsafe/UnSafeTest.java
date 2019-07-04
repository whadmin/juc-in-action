package unsafe;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

public class UnSafeTest {

    static Unsafe unsafe = null;

    @Before
    public void init() {
        try {
            Field f = null;
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createTestA() throws Exception {
        /** 使用构造器 **/
        TestA constructorA = new TestA();
        /** 打印1 **/
        System.out.println(constructorA.getA());

        /** 使用反射 **/
        TestA reflectionA = TestA.class.newInstance();
        /** 打印1 **/
        System.out.println(reflectionA.getA());

        /** 使用allocateInstance 类会被加载初始化（会执行静态代码块），但类的对象不会初始化（不会执行方法块，和构造函数）**/
        TestA unsafeA = (TestA) unsafe.allocateInstance(TestA.class);
        /** 对象没有实例化打印 0 **/
        System.out.println(unsafeA.getA());
        /** 类被加载静态变量被初始化打印 2 **/
        System.out.println(unsafeA.b);
    }

    @Test
    public void updateFiled() throws Exception {
        /** 默认情况下打印false **/
        TestA constructorA = new TestA();
        System.out.println(constructorA.giveAccess());

        /**
         * 使用objectFieldOffset获取属性在内存种的偏移
         * 使用putInt修改对象内存中属性值 打印true
         * **/
        TestA unsafeA = (TestA) unsafe.allocateInstance(TestA.class);
        Field unsafeAField = unsafeA.getClass().getDeclaredField("ACCESS_ALLOWED");
        unsafe.putInt(unsafeA, unsafe.objectFieldOffset(unsafeAField), 40);
        System.out.println(unsafeA.giveAccess());
    }

    @Test
    public void createClass() throws Exception{
        byte[] classContents = new byte[0];

        classContents = getClassContent();
        Class c= (Class) unsafe.defineClass(null,classContents,0,classContents.length,this.getClass().getClassLoader(),null);;
        System.out.println(c.getMethod("getA").invoke(c.newInstance(), null));
    }


    private static byte[] getClassContent() throws Exception {
        /** 以下几种方式都是相同的 **/
        UnSafeTest.class.getResource("TestB.class");
//        UnSafeTest.class.getResource("/unsafe/TestB.class");
//        ClassLoader.getSystemResource("unsafe/TestB.class");
//        ClassLoader.getSystemResource("unsafe/TestB.class");
        return FileCopyUtils.copyToByteArray(UnSafeTest.class.getResource("TestB.class").openStream());
    }

    @Test
    public void createBigArray(){

        long SUPER_SIZE = (long) Integer.MAX_VALUE * 2;
        SuperArray array = new SuperArray(SUPER_SIZE);
        System.out.println("Array size:" + array.size()); //print 4294967294
        int sum = 0;
        for (int i = 0; i < 100; i++) {
            array.set((long) Integer.MAX_VALUE + i, (byte) 3);
            sum += array.get((long) Integer.MAX_VALUE + i);
        }
        System.out.println("Sum of 100 elements:" + sum);  //print 300
    }

    @Test
    public void cas() {
        final TestB b = new TestB();
        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                b.counter.increment();
            }
        });
        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                b.counter.increment();
            }
        });
        Thread threadC = new Thread(new Runnable() {
            @Override
            public void run() {
                b.counter.increment();
            }
        });
        threadA.start();
        threadB.start();
        threadC.start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(b.counter.getCounter()); //print 3
    }


    private static class TestB {
        private CASCounter counter;

        public TestB() {
            try {
                counter = new CASCounter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void park1(){
        unsafe.park(false, 0);
    }

    @Test
    public void park2(){
        long star=System.currentTimeMillis();
        unsafe.park(false, 5000000000L);
        System.out.println(System.currentTimeMillis()-star);
    }


    @Test
    public void park3(){
        long star=System.currentTimeMillis();
        unsafe.park(true, System.currentTimeMillis()+5000L);
        System.out.println(System.currentTimeMillis()-star);
    }

    @Test
    public void park4(){
        unsafe.park(true, 0L);
    }

    @Test
    public void park5() {
        Thread currThread = Thread.currentThread();
        unsafe.unpark(currThread);
        unsafe.unpark(currThread);
        unsafe.unpark(currThread);

        unsafe.park(false, 0);
        unsafe.park(false, 0);
        System.out.println("execute success"); // 线程挂起，不会打印。
    }
}
