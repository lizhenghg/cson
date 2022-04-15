package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.Cson;
import com.cracker.code.cson.JsonNull;
import com.cracker.code.cson.TypeAdapter;
import com.cracker.code.cson.TypeAdapterFactory;
import com.cracker.code.cson.internal.CsonTypes;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;


import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 适配器ArrayTypeAdapter
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-02
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class ArrayTypeAdapter<E> extends TypeAdapter<Object> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> typeToken) {
            Type type = typeToken.getType();
            if (!(type instanceof GenericArrayType
                    || (type instanceof Class<?> && ((Class<?>) type).isArray()))) {
                return null;
            }

            Type componentType = CsonTypes.getArrayComponentType(type);
            TypeAdapter<?> componentTypeAdapter = cson.getAdapter(TypeToken.get(componentType));
            return new ArrayTypeAdapter(cson, componentTypeAdapter, CsonTypes.getRawType(componentType));
        }
    };

    private final Class<E> componentType;
    private final TypeAdapter<E> componentTypeAdapter;

    public ArrayTypeAdapter(Cson context, TypeAdapter<E> componentTypeAdapter, Class<E> componentType) {
        this.componentTypeAdapter =
                new TypeAdapterRuntimeTypeWrapper<E>(context, componentTypeAdapter, componentType);
        this.componentType = componentType;
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        List<E> list = new ArrayList<E>();
        in.beginArray();
        while (in.hasNext()) {
            E instance = this.componentTypeAdapter.read(in);
            list.add(instance);
        }
        in.endArray();
        Object array = Array.newInstance(this.componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(JsonWriter out, Object array) throws IOException {
        if (array == null || array instanceof JsonNull) {
            out.nullValue();
            return;
        }

        out.beginArray();
        for (int i = 0, length = Array.getLength(array); i < length; i++) {
            E value = (E) Array.get(array, i);
            this.componentTypeAdapter.write(out, value);
        }
        out.endArray();
    }
}