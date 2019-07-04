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

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * AtomicReferenceFieldUpdater用来表示类中引用类型属性字段原子操作类
 *
 * 有如下限制
 *
 * 字段必须是volatile类型的，在线程之间共享变量时保证立即可见
 *
 * 字段的描述类型（修饰符public/protected/default/private）是与调用者与操作对象字段的关系一致
 *
 * 只能是实例变量，不能是类变量
 *
 * 只能是可修改变量，不能使final变量，因为final的语义就是不可修改
 *
 */
public abstract class AtomicReferenceFieldUpdater<T,V> {


    /**
     * 获取一个AtomicReferenceFieldUpdater实例（用来表示类中的一个属性字段）
     * @param tclass  表示该字段所在的类
     * @param vclass  vclass表示该字段的类型
     * @param fieldName  表示要更新的字段名
     */
    @CallerSensitive
    public static <U,W> AtomicReferenceFieldUpdater<U,W> newUpdater(Class<U> tclass,
                                                                    Class<W> vclass,
                                                                    String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl<U,W>
            (tclass, vclass, fieldName, Reflection.getCallerClass());
    }


    protected AtomicReferenceFieldUpdater() {
    }


    /**
     * 使用CAS设置新值newValue，成功返回true,失败返回false
     */
    public abstract boolean compareAndSet(T obj, V expect, V update);

    /**
     * 使用CAS设置新值newValue，成功返回true,失败返回false
     */
    public abstract boolean weakCompareAndSet(T obj, V expect, V update);


    /**
     * 设置对象obj的AtomicReferenceFieldUpdater表示的属性值为新值newValue
     */
    public abstract void set(T obj, V newValue);

    /**
     * 设置对象obj的AtomicReferenceFieldUpdater指定的属性值为新值newValue（抛弃volatile特性）
     */
    public abstract void lazySet(T obj, V newValue);


    /**
     * 获取对象obj的AtomicReferenceFieldUpdater表示的属性值
     */
    public abstract V get(T obj);

    /**
     * 原子设置AtomicReferenceFieldUpdater表示的属性值为给定值并返回旧值。(内部使用CAS乐观锁+循环)
     */
    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /**
     * 以原子方式将AtomicReferenceFieldUpdater表示的属性值执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 以原子方式将AtomicReferenceFieldUpdater表示的属性值执行IntUnaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }


    /**
     * 以原子方式将AtomicReferenceFieldUpdater表示的属性值执行BinaryOperator函数处理，使用CAS乐观锁+循环，返回旧值
     */
    public final V getAndAccumulate(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 以原子方式将AtomicReferenceFieldUpdater表示的属性值执行BinaryOperator函数处理，使用CAS乐观锁+循环，返回新值
     */
    public final V accumulateAndGet(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    private static final class AtomicReferenceFieldUpdaterImpl<T,V>
        extends AtomicReferenceFieldUpdater<T,V> {
        private static final sun.misc.Unsafe U = sun.misc.Unsafe.getUnsafe();


        /**
         * 属性在内存中的偏移地址
         */
        private final long offset;

        /**
         * 操作类Class
         */
        private final Class<?> cclass;

        private final Class<T> tclass;

        /**
         * 操作类属性Class
         */
        private final Class<V> vclass;



        AtomicReferenceFieldUpdaterImpl(final Class<T> tclass,
                                        final Class<V> vclass,
                                        final String fieldName,
                                        final Class<?> caller) {

            /** 操作类属性Field **/
            final Field field;
            /** 操作类属性的class **/
            final Class<?> fieldClass;
            /** 操作类属性修饰符 **/
            final int modifiers;
            try {
                /** 使用反射获取对象的属性 field **/
                field = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Field>() {
                        public Field run() throws NoSuchFieldException {
                            return tclass.getDeclaredField(fieldName);
                        }
                    });

                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                    caller, tclass, null, modifiers);


                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) &&
                    ((cl == null) || !isAncestor(cl, ccl))) {
                    sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }

                fieldClass = field.getType();
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            if (vclass != fieldClass)
                throw new ClassCastException();
            if (vclass.isPrimitive())
                throw new IllegalArgumentException("Must be reference type");

            if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");


            this.cclass = (Modifier.isProtected(modifiers) &&
                           tclass.isAssignableFrom(caller) &&
                           !isSamePackage(tclass, caller))
                          ? caller : tclass;
            this.tclass = tclass;
            this.vclass = vclass;
            this.offset = U.objectFieldOffset(field);
        }


        /**
         * 传入的ClassLoader是否存在父子关系
         */
        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if (second == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }


        /**
         * 传入参数Class包路径名称是否相同
         */
        private static boolean isSamePackage(Class<?> class1, Class<?> class2) {
            return class1.getClassLoader() == class2.getClassLoader()
                   && Objects.equals(getPackageName(class1), getPackageName(class2));
        }

        /**
         * 获取包路径尾部最后包名称
         */
        private static String getPackageName(Class<?> cls) {
            String cn = cls.getName();
            int dot = cn.lastIndexOf('.');
            return (dot != -1) ? cn.substring(0, dot) : "";
        }


        /**
         * 检查传入对象能否实例化，检查失败抛出异常
         */
        private final void accessCheck(T obj) {
            if (!cclass.isInstance(obj))
                throwAccessCheckException(obj);
        }


        private final void throwAccessCheckException(T obj) {
            if (cclass == tclass)
                throw new ClassCastException();
            else
                throw new RuntimeException(
                    new IllegalAccessException(
                        "Class " +
                        cclass.getName() +
                        " can not access a protected member of class " +
                        tclass.getName() +
                        " using an instance of " +
                        obj.getClass().getName()));
        }

        /**
         * 检查传入实例是否和为属性类的实例，失败抛出异常
         */
        private final void valueCheck(V v) {
            if (v != null && !(vclass.isInstance(v)))
                throwCCE();
        }

        static void throwCCE() {
            throw new ClassCastException();
        }

        /**
         * 使用CAS设置对象属性的值
         */
        public final boolean compareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, offset, expect, update);
        }

        /**
         * 使用CAS设置对象属性的值
         */
        public final boolean weakCompareAndSet(T obj, V expect, V update) {
            // same implementation as strong form for now
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, offset, expect, update);
        }

        /**
         * 设置对象属性的值（其他线程可见）
         */
        public final void set(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putObjectVolatile(obj, offset, newValue);
        }

        /**
         * 设置对象属性的值（其他线程不一定可见）
         */
        public final void lazySet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putOrderedObject(obj, offset, newValue);
        }

        /**
         * 获取对象属性的值（其他线程可见）
         */
        @SuppressWarnings("unchecked")
        public final V get(T obj) {
            accessCheck(obj);
            return (V)U.getObjectVolatile(obj, offset);
        }

        /**
         * 使用原子的方式设置对象属性的值（CAS+循环），并返回旧值
         */
        @SuppressWarnings("unchecked")
        public final V getAndSet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            return (V)U.getAndSetObject(obj, offset, newValue);
        }
    }
}
