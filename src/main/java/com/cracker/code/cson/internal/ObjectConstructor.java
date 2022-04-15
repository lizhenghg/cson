package com.cracker.code.cson.internal;

/**
 *
 * 自定义Object构造器
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public interface ObjectConstructor<T> {

    /**
     * 构造方法
     * @return T
     */
    T construct();

}
