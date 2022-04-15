package com.cracker.code.cson.internal;

import com.cracker.code.cson.stream.JsonReader;

import java.io.IOException;

/**
 *
 * JsonReaderInternalAccess: JsonReaderInternalAccess
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-31
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public abstract class JsonReaderInternalAccess {

    public static JsonReaderInternalAccess INSTANCE;

    /**
     * name 转换为value
     * @param reader JsonReader
     * @throws IOException IOException
     */
    public abstract void promoteNameToValue(JsonReader reader) throws IOException;

}
