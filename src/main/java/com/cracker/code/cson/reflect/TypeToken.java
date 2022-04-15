package com.cracker.code.cson.reflect;

import com.cracker.code.cson.internal.CsonPreconditions;
import com.cracker.code.cson.internal.CsonTypes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * TypeToken: 类型令牌，对类型进行鉴定
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-18
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class TypeToken<T> {

    /**
     * 下边界通配符，表示?包括T和T的父类
     */
    final Class<? super T> rawType;
    final Type type;
    final int hashCode;


    @SuppressWarnings("unchecked")
    protected TypeToken() {
        this.type = getSuperclassTypeParameter(getClass());
        this.rawType = (Class<? super T>) CsonTypes.getRawType(this.type);
        this.hashCode = this.type.hashCode();
    }

    @SuppressWarnings("unchecked")
    TypeToken(Type type) {
        this.type = CsonTypes.canonicalize(CsonPreconditions.checkNotNull(type));
        this.rawType = (Class<? super T>) CsonTypes.getRawType(this.type);
        this.hashCode = this.type.hashCode();
    }


    /**
     * 这个方法不能在外部单独使用，目前只能搭配protected TypeToken() {}使用。也就是不同包的不同子类调用
     * @param subClass 子类Class
     * @return Type
     */
    private static Type getSuperclassTypeParameter(Class<?> subClass) {
        // genericSuperType不会为null。如果subClass为Class com.cracker.code.cson.reflect.TypeToken，
        // genericSuperType为Class java.lang.Object
        Type genericSuperType = subClass.getGenericSuperclass();
        if (genericSuperType instanceof Class) {
            throw new RuntimeException("Missing type parameter");
        }
        ParameterizedType parameterized = (ParameterizedType) genericSuperType;
        // 这里只获取第一个参数，是因为这个subClass固定为? extends TypeToken，永远只有一个T
        return CsonTypes.canonicalize(parameterized.getActualTypeArguments()[0]);
    }

    public Class<? super T> getRawType() {
        return this.rawType;
    }

    public Type getType() {
        return this.type;
    }

    public int getHashCode() {
        return this.hashCode;
    }


    public static TypeToken<?> get(Type type) {
        return new TypeToken<>(type);
    }


    public static <T> TypeToken<T> get(Class<T> type) {
        return new TypeToken<>(type);
    }

    @Override
    public final int hashCode() {
        return this.hashCode;
    }
    
    @Override
    public final boolean equals(Object o) {
        return o instanceof TypeToken<?>
                && CsonTypes.equals(type, ((TypeToken<?>) o).type);
    }
}
