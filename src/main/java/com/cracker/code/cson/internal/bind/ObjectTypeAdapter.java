package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.Cson;
import com.cracker.code.cson.TypeAdapter;
import com.cracker.code.cson.TypeAdapterFactory;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * 适配器ObjectTypeAdapter
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-28
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class ObjectTypeAdapter extends TypeAdapter<Object> {


    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> type) {
            if (type.getRawType() == Object.class) {
                return (TypeAdapter<T>) new ObjectTypeAdapter(cson);
            }
            return null;
        }
    };

    private final Cson cson;

    private ObjectTypeAdapter(Cson cson) {
        this.cson = cson;
    }


    @Override
    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;

            case BEGIN_OBJECT:
                // 测试可删，这里可以手写一个LinkedTreeMap
                Map<String, Object> map = new LinkedHashMap<>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;

            case STRING:
                return in.nextString();

            case NUMBER:
                return in.nextDouble();

            case BOOLEAN:
                return in.nextBoolean();

            case NULL:
                in.nextNull();
                return null;

            default:
                throw new IllegalStateException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) cson.getAdapter(value.getClass());
        if (typeAdapter instanceof ObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }

        typeAdapter.write(out, value);
    }
}
