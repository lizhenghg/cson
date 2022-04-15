package com.cracker.code.cson.stream;

/**
 *
 * Json令牌
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-31
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public enum JsonToken {

    BEGIN_ARRAY,
    END_ARRAY,
    BEGIN_OBJECT,
    END_OBJECT,
    NAME,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    END_DOCUMENT
}
