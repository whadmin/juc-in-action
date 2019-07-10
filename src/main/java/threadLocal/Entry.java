package threadLocal;

import java.lang.ref.WeakReference;


public class Entry extends WeakReference<ThreadLocal<Object>> {

    public Object value;

    Entry(ThreadLocal<Object> k, Object v) {
        super(k);
        value = v;
    }
}
