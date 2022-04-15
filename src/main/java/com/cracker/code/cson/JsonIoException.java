package com.cracker.code.cson;

/**
 *
 * Json IO exception
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonIoException extends JsonParseException {

    private final static long serialVersionUID = 1L;

    public JsonIoException(String msg) {
        super(msg);
    }

    public JsonIoException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonIoException(Throwable cause) {
        super(cause);
    }
}