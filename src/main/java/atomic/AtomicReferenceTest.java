package atomic;

import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class AtomicReferenceTest {

    @Test
    public void test() throws Exception {
        AtomicReference<Simple> atomic = new AtomicReference<Simple>(new Simple("Alex", 12));
        System.out.println(atomic.get());

        boolean result = atomic.compareAndSet(new Simple("Alex", 12), new Simple("sdfs", 234));
        System.out.println(result);
    }


    static class Simple {
        private String name;
        private int age;

        public Simple(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
