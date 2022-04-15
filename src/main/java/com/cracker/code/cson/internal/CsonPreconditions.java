package com.cracker.code.cson.internal;

/**
 *
 * CsonPreconditions: Cson框架的预处理器
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-18
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class CsonPreconditions {

    public static <T> T checkNotNull(final T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    /**
     * 类似断言，主要判断在于boolean
     * @param condition 判断条件
     */
    public static void checkArgument(final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

}
