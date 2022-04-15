package com.cracker.code.cson.stream;

import java.io.IOException;

/**
 *
 *
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-31
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class MalformedJsonException extends IOException {

    private static final long serialVersionUID = 1L;

    public MalformedJsonException(final String message) {
        super(message);
    }

    public MalformedJsonException(final String message, final Throwable throwable) {
        super(message);
        this.initCause(throwable);
    }

    public MalformedJsonException(final Throwable throwable) {
        this.initCause(throwable);
    }

}
