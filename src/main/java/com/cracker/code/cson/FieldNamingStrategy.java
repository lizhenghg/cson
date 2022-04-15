package com.cracker.code.cson;

import java.lang.reflect.Field;

/**
 * 序列化Object时，对Object中每个属性的命名策略
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public interface FieldNamingStrategy {

    /**
     * 翻译属性名
     * @param f Field
     * @return translate name
     */
    public abstract String translateName(Field f);
}
