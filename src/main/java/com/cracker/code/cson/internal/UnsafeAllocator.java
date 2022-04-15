package com.cracker.code.cson.internal;

import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 * UnsafeAllocator: UnsafeAllocator
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public abstract class UnsafeAllocator {
    /**
     * 创建实例
     * @param c class of instance
     * @param <T> T
     * @return T
     * @throws Exception Exception
     */
    public abstract <T> T newInstance(Class<T> c) throws Exception;

    public static UnsafeAllocator create() {

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            return new UnsafeAllocator() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T newInstance(Class<T> c) throws Exception {
                    return (T) allocateInstance.invoke(unsafe, c);
                }
            };
        } catch (Exception ignored) {
        }


        try {
            Method getConstructorId = ObjectStreamClass.class
                    .getDeclaredMethod("getConstructorId", Class.class);
            getConstructorId.setAccessible(true);
            int constructorId = (Integer) getConstructorId.invoke(null, Object.class);
            Method newInstance = ObjectStreamClass.class
                    .getDeclaredMethod("newInstance", Class.class, int.class);
            newInstance.setAccessible(true);
            return new UnsafeAllocator() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T newInstance(Class<T> c) throws Exception {
                    return (T) newInstance.invoke(null, c, constructorId);
                }
            };
        } catch (Exception ignored) {
        }

        try {
            final Method newInstance = ObjectInputStream.class
                    .getDeclaredMethod("newInstance", Class.class, Class.class);
            newInstance.setAccessible(true);
            return new UnsafeAllocator() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T newInstance(Class<T> c) throws Exception {
                    return (T) newInstance.invoke(null, c, Object.class);
                }
            };
        } catch (Exception ignored) {
        }

        // give up，放弃查找
        return new UnsafeAllocator() {
            // 异常的常用方式之：一调用就抛异常
            @Override
            public <T> T newInstance(Class<T> c) {
                throw new UnsupportedOperationException("Cannot allocate " + c);
            }
        };
    }
}
