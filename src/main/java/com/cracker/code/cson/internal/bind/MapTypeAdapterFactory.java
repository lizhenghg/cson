package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.*;
import com.cracker.code.cson.internal.*;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * 针对Map的适配器: MapTypeAdapterFactory
 * class instance of Object
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-05
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class MapTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;
    private final boolean complexMapKeySerialization;

    public MapTypeAdapterFactory(ConstructorConstructor constructorConstructor,
                                 boolean complexMapKeySerialization) {
        this.constructorConstructor = constructorConstructor;
        this.complexMapKeySerialization = complexMapKeySerialization;
    }

    @Override
    public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!Map.class.isAssignableFrom(rawType)) {
            return null;
        }

        Class<?> rawTypeOfSrc = CsonTypes.getRawType(type);
        Type[] keyAndValueTypes = CsonTypes.getMapKeyAndValueTypes(type, rawTypeOfSrc);
        TypeAdapter<?> keyAdapter = getKeyAdapter(cson, keyAndValueTypes[0]);
        TypeAdapter<?> valueAdapter = cson.getAdapter(TypeToken.get(keyAndValueTypes[1]));
        ObjectConstructor<T> constructor = constructorConstructor.get(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeAdapter<T> result = new Adapter(cson, keyAndValueTypes[0], keyAdapter,
                keyAndValueTypes[1], valueAdapter, constructor);

        return result;
    }

    private TypeAdapter<?> getKeyAdapter(Cson context, Type keyType) {
        return (keyType == boolean.class || keyType == Boolean.class)
                ? TypeAdapters.BOOLEAN_AS_STRING
                : context.getAdapter(TypeToken.get(keyType));
    }

    private final class Adapter<K, V> extends TypeAdapter<Map<K, V>> {
        private final TypeAdapter<K> keyTypeAdapter;
        private final TypeAdapter<V> valueTypeAdapter;
        private final ObjectConstructor<? extends Map<K, V>> constructor;

        public Adapter(Cson context, Type keyType, TypeAdapter<K> keyTypeAdapter,
                       Type valueType, TypeAdapter<V> valueTypeAdapter,
                       ObjectConstructor<? extends Map<K, V>> constructor) {
            this.keyTypeAdapter =
                    new TypeAdapterRuntimeTypeWrapper<K>(context, keyTypeAdapter, keyType);
            this.valueTypeAdapter =
                    new TypeAdapterRuntimeTypeWrapper<V>(context, valueTypeAdapter, valueType);
            this.constructor = constructor;
        }


        @Override
        public Map<K, V> read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            Map<K, V> map = constructor.construct();

            if (peek == JsonToken.BEGIN_ARRAY) {
                in.beginArray();
                while (in.hasNext()) {
                    in.beginArray(); // entry array
                    K key = keyTypeAdapter.read(in);
                    V value = valueTypeAdapter.read(in);
                    V replaced = map.put(key, value);
                    if (replaced != null) {
                        throw new JsonSyntaxException("duplicate key: " + key);
                    }
                    in.endArray();
                }
                in.endArray();
            } else {
                in.beginObject();
                while (in.hasNext()) {
                    JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
                    K key = keyTypeAdapter.read(in);
                    V value = valueTypeAdapter.read(in);
                    V replaced = map.put(key, value);
                    if (replaced != null) {
                        throw new JsonSyntaxException("duplicate key: " + key);
                    }
                }
                in.endObject();
            }
            return map;
        }

        @Override
        public void write(JsonWriter out, Map<K, V> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            if (!complexMapKeySerialization) {
                out.beginObject();
                for (Map.Entry<K, V> entry : value.entrySet()) {
                    out.name(String.valueOf(entry.getKey()));
                    valueTypeAdapter.write(out, entry.getValue());
                }
                out.endObject();
                return;
            }

            boolean hasComplexKeys = false;
            List<JsonElement> keys = new ArrayList<JsonElement>(value.size());

            List<V> values = new ArrayList<V>(value.size());
            for (Map.Entry<K, V> entry : value.entrySet()) {
                JsonElement keyElement = keyTypeAdapter.toJsonTree(entry.getKey());
                keys.add(keyElement);
                values.add(entry.getValue());
                hasComplexKeys |= keyElement.isJsonArray() || keyElement.isJsonObject();
            }

            if (hasComplexKeys) {
                out.beginArray();
                for (int i = 0; i < keys.size(); i++) {
                    out.beginArray(); // entry array
                    Streams.write(keys.get(i), out);
                    valueTypeAdapter.write(out, values.get(i));
                    out.endArray();
                }
                out.endArray();
            } else {
                out.beginObject();
                for (int i = 0; i < keys.size(); i++) {
                    JsonElement keyElement = keys.get(i);
                    out.name(keyToString(keyElement));
                    valueTypeAdapter.write(out, values.get(i));
                }
                out.endObject();
            }
        }

        private String keyToString(JsonElement keyElement) {
            if (keyElement.isJsonPrimitive()) {
                JsonPrimitive primitive = keyElement.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return String.valueOf(primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    return Boolean.toString(primitive.getAsBoolean());
                } else if (primitive.isString()) {
                    return primitive.getAsString();
                } else {
                    throw new AssertionError();
                }
            } else if (keyElement.isJsonNull()) {
                return "null";
            } else {
                throw new AssertionError();
            }
        }
    }

}
