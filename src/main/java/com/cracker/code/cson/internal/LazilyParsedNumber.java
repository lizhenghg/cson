package com.cracker.code.cson.internal;

import java.io.ObjectStreamException;
import java.math.BigDecimal;

/**
 *
 * 解析数量类
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class LazilyParsedNumber extends Number {

    private final String value;

    public LazilyParsedNumber(final String value) {
        this.value = value;
    }

    /**
     * one by one to parse, the scope is more and more bigger.
     * @return parsed Number
     */
    @Override
    public int intValue() {
        try {
            return Integer.parseInt(this.value);
        } catch (NumberFormatException e) {

            try {
                return (int) Long.parseLong(this.value);
            } catch (NumberFormatException nfe) {
                return new BigDecimal(this.value).intValue();
            }
        }
    }

    /**
     * one by one to parse, the scope is more and more bigger.
     * @return parsed Number
     */
    @Override
    public long longValue() {
        try {
            return Long.parseLong(this.value);
        } catch (NumberFormatException nfe) {
            return new BigDecimal(this.value).longValue();
        }
    }

    @Override
    public float floatValue() {
        return Float.parseFloat(this.value);
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(this.value);
    }

    @Override
    public String toString() {
        return this.value;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new BigDecimal(this.value);
    }
}
