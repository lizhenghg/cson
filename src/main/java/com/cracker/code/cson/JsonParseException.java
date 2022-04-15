package com.cracker.code.cson;

/**
 *
 * Json parse exception
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-01
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class JsonParseException extends RuntimeException {
    /**
     * Throwable实现了Serializable接口
     */
    private static final long serialVersionUID = 1L;

    public JsonParseException(String msg) {
        super(msg);
    }

    public JsonParseException(Throwable cause) {
        super(cause);
    }

    public JsonParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}