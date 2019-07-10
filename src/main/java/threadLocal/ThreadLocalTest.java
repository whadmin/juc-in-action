package threadLocal;

import java.util.stream.IntStream;

/**
 * ThreadLocal 内部并没有对传入副本变量做克隆而是引用
 * 因而在设置给一个引用变量是无法保证线程安全的。
 */
public class ThreadLocalTest {

    private static Test value = new Test();

    private static ThreadLocal<Test> threadLocal = new ThreadLocal<Test>() {
        protected Test initialValue() {
            return value;
        }
    };

    private static ThreadLocal<Test> threadLocal2 = ThreadLocal.withInitial(()->new Test());


    public static class ThreadLocalThread implements Runnable {
        @Override
        public void run() {

            IntStream.range(0, 200).forEach(i -> {
                Test vaue = (Test) threadLocal.get();
                vaue.value++;
                threadLocal.set(vaue);
            });
            System.out.println(threadLocal.get());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new ThreadLocalThread());
        Thread thread2 = new Thread(new ThreadLocalThread());
        thread.start();
        thread2.start();
        thread.join();
        thread2.join();
        System.out.println(value);
    }

    static class Test {

        int value = 0;

        @Override
        public String toString() {
            return "Test{value=" + value + "}";
        }
    }
}