package com.cracker.code.cson;

import com.cracker.code.cson.reflect.TypeToken;

/**
 *
 * 类型适配器工厂，只生产类型适配器。本Json框架主要使用适配器模式
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-18
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public interface TypeAdapterFactory {
    /**
     * 工厂创建不同类型的适配器
     * @param cson Cson
     * @param type TypeToken
     * @param <T> T
     * @return TypeAdapter
     */
    <T> TypeAdapter<T> create(Cson cson, TypeToken<T> type);

}
