package com.cracker.code.cson;

import com.cracker.code.cson.internal.Streams;
import com.cracker.code.cson.stream.JsonWriter;


import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * JsonElement，自定义抽象父类Json元素
 *
 * JS的JSON数据格式中，包含4种基础类型（字符串，数字，布尔和null）和两种结构类型（对象和数组）
 *
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public abstract class JsonElement {

    /**
     * Returns a deep copy of this element. Immutable element like primitives
     * and nulls are not copied
     * @return JsonElement
     */
    abstract JsonElement deepCopy();

    /**
     * provides check for verifying if this element is an array or not
     * @return true if this element is of type {@link JsonArray}, false otherwise.
     */
    public boolean isJsonArray() {
        return this instanceof JsonArray;
    }

    /**
     * provides check for verifying if this element is a json or not
     * @return true if this element is of type {@link JsonObject}, false otherwise
     */
    public boolean isJsonObject() {
        return this instanceof JsonObject;
    }

    public boolean isJsonPrimitive() {
        return this instanceof JsonPrimitive;
    }

    public boolean isJsonNull() {
        return this instanceof JsonNull;
    }

    public JsonObject getAsJsonObject() {
        if (isJsonObject()) {
            return (JsonObject) this;
        }
        throw new IllegalStateException("Not a JSON Object: " + this);
    }

    public JsonArray getAsJsonArray() {
        if (isJsonArray()) {
            return (JsonArray) this;
        }
        throw new IllegalStateException("This is not a JSON Array.");
    }

    public JsonPrimitive getAsJsonPrimitive() {
        if (isJsonPrimitive()) {
            return (JsonPrimitive) this;
        }
        throw new IllegalStateException("This is not a JSON Primitive.");
    }

    public JsonNull getAsJsonNull() {
        if (isJsonNull()) {
            return (JsonNull) this;
        }
        throw new IllegalStateException("This is not a JSON Null.");
    }


    /**
     * 只允许子类调，父类一调就抛异常，不失为一种奇特思路！
     * @return true or false
     */
    public boolean getAsBoolean() {
        // 如果是JsonElement调用，则抛出该异常。getSimpleName() // JsonElement
        // getName() // com.cracker.code.cson.JsonElement
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public Boolean getAsBooleanWrapper() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }


    public String getAsString() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public double getAsDouble() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public float getAsFloat() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public long getAsLong() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public int getAsInt() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public byte getAsByte() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public short getAsShort() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public char getAsCharacter() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public BigDecimal getAsBigDecimal() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public BigInteger getAsBigInteger() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public Number getAsNumber() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    /**
     * returns a String representation of this element.
     */
    @Override
    public String toString() {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(true);
            // 这里可以不使用flush方法，通过查看jdk1.8 StringWriter的源码可知，
            // StringWriter并没有对flush进行任何操作，close方法一样
            Streams.write(this, jsonWriter);
            return stringWriter.toString();
        } catch (IOException ioException) {
            throw new AssertionError(ioException);
        }
    }
}
