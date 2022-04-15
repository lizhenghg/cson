package com.cracker.code.cson;

/**
 *
 * Json syntax exception
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-01
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonSyntaxException extends JsonParseException {

    /**
     * Throwable实现了Serializable接口
     */
    private static final long serialVersionUID = 1L;

    public JsonSyntaxException(String msg) {
        super(msg);
    }

    public JsonSyntaxException(Throwable throwable) {
        super(throwable);
    }

    public JsonSyntaxException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}