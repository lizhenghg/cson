package com.cracker.code.cson;


import com.cracker.code.cson.internal.bind.JsonTreeWriter;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;

/**
 *
 * 抽象类型适配器
 * JS的JSON数据格式中，包含4种基础类型（字符串，数字，布尔和null）和两种结构类型（对象和数组）
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-25
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public abstract class TypeAdapter<T> {


    /**
     * Reads one JSON value (an array, object, string, number, boolean or null)
     * and converts it to a Java object. Returns the converted object.
     * @param in JsonReader
     * @return the converted Java object. May be null.
     * @throws IOException IOException
     */
    public abstract T read(JsonReader in) throws IOException;

    /**
     * Writes one JSON value (an array, object, string, number, boolean or null)
     * @param out json输出字符流
     * @param value the Java object to write. May be null.
     * @throws IOException IOException
     */
    public abstract void write(JsonWriter out, T value) throws IOException;


    public final JsonElement toJsonTree(T value) {
        try {
            JsonTreeWriter jsonWriter = new JsonTreeWriter();
            write(jsonWriter, value);
            return jsonWriter.get();
        } catch (IOException e) {
            throw new JsonIoException(e);
        }
    }
}
