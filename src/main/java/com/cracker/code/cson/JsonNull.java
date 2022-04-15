package com.cracker.code.cson;

/**
 *
 * JsonNull，自定义Json空
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonNull extends JsonElement {

    public static final JsonNull INSTANCE = new JsonNull();

    @Deprecated
    public JsonNull() {
        // Do nothing
    }

    @Override
    JsonNull deepCopy() {
        return INSTANCE;
    }


    @Override
    public boolean equals(Object other) {
        return other instanceof JsonNull;
    }

    @Override
    public int hashCode() {
        return JsonNull.class.hashCode();
    }

}
