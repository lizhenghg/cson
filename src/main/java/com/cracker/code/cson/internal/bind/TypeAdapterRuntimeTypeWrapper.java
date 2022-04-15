package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.Cson;
import com.cracker.code.cson.TypeAdapter;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 *
 * 运行时适配器包装类
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-02
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {

    private final Cson context;
    private final TypeAdapter<T> delegate;
    private final Type type;

    TypeAdapterRuntimeTypeWrapper(final Cson context, final TypeAdapter<T> delegate, final Type type) {
        this.context = context;
        this.delegate = delegate;
        this.type = type;
    }


    @Override
    public T read(JsonReader in) throws IOException {
        return this.delegate.read(in);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(JsonWriter out, final T value) throws IOException {
        TypeAdapter chosen = this.delegate;
        final Type runTimeType = this.getRuntimeTypeIfMoreSpecific(this.type, value);
        if (runTimeType != this.type) {
            final TypeAdapter<?> runtimeTypeAdapter = this.context.getAdapter(TypeToken.get(runTimeType));
            if (!(runtimeTypeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                chosen = runtimeTypeAdapter;
            } else if (!(this.delegate instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                chosen = delegate;
            } else {
                chosen = runtimeTypeAdapter;
            }
        }
        chosen.write(out, value);
    }

    private Type getRuntimeTypeIfMoreSpecific(Type type, final Object value) {
        if (value != null && (type instanceof TypeVariable || type instanceof Class)) {
            type = value.getClass();
        }
        return type;
    }
}
