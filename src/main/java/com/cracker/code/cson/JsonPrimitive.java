package com.cracker.code.cson;


import com.cracker.code.cson.internal.CsonPreconditions;
import com.cracker.code.cson.internal.LazilyParsedNumber;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * JsonPrimitive，自定义Json对象类，映射String、Number、Boolean、null
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonPrimitive extends JsonElement {

    /**
     * java基本数据类型
     */
    private static final Class<?>[] PRIMITIVE_TYPES = {int.class, long.class, short.class, float.class,
            double.class, byte.class, boolean.class, char.class, Integer.class, Long.class, Short.class,
            Float.class, Double.class, Byte.class, Boolean.class, Character.class
    };

    private Object value;

    @Override
    JsonElement deepCopy() {
        return this;
    }

    public JsonPrimitive(Number number) {
        setValue(number);
    }

    public JsonPrimitive(Boolean bool) {
        setValue(bool);
    }

    public JsonPrimitive(String string) {
        setValue(string);
    }

    public JsonPrimitive(Character c) {
        setValue(c);
    }

    JsonPrimitive(Object primitive) {
        setValue(primitive);
    }

    private void setValue(Object primitive) {
        if (primitive instanceof Character) {
            char c = (Character) primitive;
            this.value = String.valueOf(c);
        } else {
            CsonPreconditions.checkArgument(primitive instanceof Number
                    || isPrimitiveOrString(primitive));
            this.value = primitive;
        }
    }

    private static boolean isPrimitiveOrString(Object target) {
        if (target instanceof String) {
            return true;
        }

        Class<?> classOfPrimitive = target.getClass();
        for (Class<?> standardPrimitive : PRIMITIVE_TYPES) {
            if (standardPrimitive.isAssignableFrom(classOfPrimitive)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBoolean() {
        return this.value instanceof Boolean;
    }

    @Override
    public boolean getAsBoolean() {
        if (isBoolean()) {
            return getAsBooleanWrapper();
        } else {
            // check to see if the value as a String is "true" in any case.
            return Boolean.parseBoolean(this.getAsString());
        }
    }

    @Override
    public Boolean getAsBooleanWrapper() {
        if (isBoolean()) {
            return (Boolean) this.value;
        }
        throw new IllegalStateException("This is not a Boolean primitive: " + this.value);
    }

    public boolean isString() {
        return this.value instanceof String;
    }

    @Override
    public String getAsString() {
        if (isString()) {
            return (String) this.value;
        } else if (isNumber()) {
            return this.getAsNumber().toString();
        } else if (isBoolean()) {
            return getAsBooleanWrapper().toString();
        } else {
            throw new IllegalStateException("Not a String primitive: " + this.value);
        }
    }


    public boolean isNumber() {
        return this.value instanceof Number;
    }

    @Override
    public Number getAsNumber() {
        return this.value instanceof String ?
                new LazilyParsedNumber((String) this.value) : (Number) this.value;
    }

    @Override
    public double getAsDouble() {
        return isNumber() ? this.getAsNumber().doubleValue() : Double.parseDouble(this.getAsString());
    }

    @Override
    public float getAsFloat() {
        return isNumber() ? this.getAsNumber().floatValue() : Float.parseFloat(this.getAsString());
    }

    @Override
    public long getAsLong() {
        return isNumber() ? this.getAsNumber().longValue() : Long.parseLong(this.getAsString());
    }

    @Override
    public int getAsInt() {
        return isNumber() ? this.getAsNumber().intValue() : Integer.parseInt(this.getAsString());
    }

    @Override
    public byte getAsByte() {
        return isNumber() ? this.getAsNumber().byteValue() : Byte.parseByte(this.getAsString());
    }

    @Override
    public short getAsShort() {
        return isNumber() ? this.getAsNumber().shortValue() : Short.parseShort(this.getAsString());
    }

    @Override
    public char getAsCharacter() {
        return this.getAsString().charAt(0);
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        return this.value instanceof BigDecimal
                ? (BigDecimal) this.value : new BigDecimal(this.value.toString());
    }

    @Override
    public BigInteger getAsBigInteger() {
        return this.value instanceof BigInteger
                ? (BigInteger) this.value : new BigInteger(this.value.toString());
    }

    /**
     * 旨在判断对象的value的数值
     * @param obj compared Object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JsonPrimitive other = (JsonPrimitive) obj;
        if (this.value == null) {
            return other.value == null;
        }
        if (isIntegral(this) && isIntegral(other)) {
            return getAsNumber().longValue() == other.getAsNumber().longValue();
        }
        // Float和Double单独处理，里面涉及到isNaN和isInfinite
        if (this.value instanceof Number && other.value instanceof Number) {
            double a = getAsNumber().doubleValue();
            double b = other.getAsNumber().doubleValue();
            return a == b || (Double.isNaN(a) && Double.isNaN(b));
        }
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        if (this.value == null) {
            return 31;
        }
        // 使用Effective java推荐的高性能hash算法处理长整型和双精度类型
        if (isIntegral(this)) {
            long value = getAsNumber().longValue();
            return (int) (value ^ (value >>> 32));
        }
        if (this.value instanceof Number) {
            long value = Double.doubleToLongBits(getAsNumber().doubleValue());
            return (int) (value ^ (value >>> 32));
        }
        return this.value.hashCode();
    }

    /**
     * 检查JsonPrimitive是否属于Long、Integer、Short、Byte、BigInteger
     * @param primitive JsonPrimitive
     * @return boolean
     */
    private static boolean isIntegral(JsonPrimitive primitive) {
        if (primitive != null && primitive.value instanceof Number) {
            Number number = (Number) primitive.value;
            return number instanceof Long || number instanceof Integer ||
                    number instanceof Short || number instanceof Byte || number instanceof BigInteger;
        }
        return false;
    }
}
