package com.cracker.code.cson;

import java.lang.reflect.Type;

/**
 *
 * 实例创建器
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public interface InstanceCreator<T> {

    /**
     * 创建实例
     * @param type is Type
     * @return created instance
     */
    public abstract T createInstance(Type type);

}
