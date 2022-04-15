package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.*;

import com.cracker.code.cson.annotations.SerializedName;
import com.cracker.code.cson.internal.LazilyParsedNumber;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;

import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

/**
 *
 * 适配器mapper类，另一种方法可以是TypeAdapterManager和TypeAdapterPool结合使用
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-28
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class TypeAdapters {

    /*
     * JSON_ELEMENT TypeAdapter begin ----------------------->
     */
    /**
     * 这种写法，节省了一个实现子类。一般写法为
     * public class JsonElementTypeAdapter extends TypeAdapter<JsonElement> {}
     */
    public static final TypeAdapter<JsonElement> JSON_ELEMENT = new TypeAdapter<JsonElement>() {

        @Override
        public JsonElement read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case STRING:
                    return new JsonPrimitive(in.nextString());
                case NUMBER:
                    String number = in.nextString();
                    return new JsonPrimitive(new LazilyParsedNumber(number));
                case BOOLEAN:
                    return new JsonPrimitive(in.nextBoolean());
                case NULL:
                    in.nextNull();
                    return JsonNull.INSTANCE;
                case BEGIN_ARRAY:
                    JsonArray array = new JsonArray();
                    in.beginArray();
                    while (in.hasNext()) {
                        array.add(read(in));
                    }
                    in.endArray();
                    return array;
                case BEGIN_OBJECT:
                    JsonObject object = new JsonObject();
                    in.beginObject();
                    while (in.hasNext()) {
                        object.add(in.nextName(), read(in));
                    }
                    in.endObject();
                    return object;
                case END_DOCUMENT:
                case NAME:
                case END_OBJECT:
                case END_ARRAY:
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void write(JsonWriter out, JsonElement value) throws IOException {
            if (value == null || value.isJsonNull()) {
                out.nullValue();
            } else if (value.isJsonPrimitive()) {
                JsonPrimitive primitive = (JsonPrimitive) value;
                if (primitive.isNumber()) {
                    out.value(primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    out.value(primitive.getAsBoolean());
                } else {
                    out.value(primitive.getAsString());
                }
            } else if (value.isJsonArray()) {
                out.beginArray();
                // 递归
                for (JsonElement element : value.getAsJsonArray()) {
                    write(out, element);
                }
                out.endArray();

            } else if (value.isJsonObject()) {
                out.beginObject();
                // 递归
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    out.name(entry.getKey());
                    write(out, entry.getValue());
                }
                out.endObject();
            } else {
                throw new IllegalArgumentException("Couldn't write " + value.getClass());
            }
        }
    };

    /**
     * 这种写法，节省了一个实现子类。一般写法为
     * public class TypeHierarchyFactory implements TypeAdapterFactory {}
     */
    public static final TypeAdapterFactory JSON_ELEMENT_FACTORY
            = newTypeHierarchyFactory(JsonElement.class, JSON_ELEMENT);


    public static <TT> TypeAdapterFactory newTypeHierarchyFactory(final Class<TT> clazz,
                                                                  final TypeAdapter<TT> typeAdapter) {
        return new TypeAdapterFactory() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> typeToken) {
                // 后面的TypeToken里面的rawType，全部为JsonElement及其子类
                return clazz.isAssignableFrom(typeToken.getRawType()) ? (TypeAdapter<T>) typeAdapter : null;
            }

            @Override
            public String toString() {
                return "Factory[typeHierarchy = " + clazz.getName() + ", adapter = " + typeAdapter + "]";
            }
        };
    }
    /*
     * <----------------------- JSON_ELEMENT TypeAdapter end
     */



    /*
     * STRING TypeAdapter begin ----------------------->
     */

    public static final TypeAdapter<String> STRING = new TypeAdapter<String>() {
        @Override
        public String read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            /* coerce booleans to strings for backwards compatibility */
            if (peek == JsonToken.BOOLEAN) {
                return Boolean.toString(in.nextBoolean());
            }
            return in.nextString();
        }

        @Override
        public void write(JsonWriter out, String value) throws IOException {
            out.value(value);
        }
    };

    public static final TypeAdapterFactory STRING_FACTORY = newFactory(String.class, STRING);
    /*
     * <----------------------- STRING TypeAdapter end
     */



    /*
     * Number TypeAdapter begin ----------------------->
     */

    public static final TypeAdapter<Number> NUMBER = new TypeAdapter<Number>() {

        @Override
        public Number read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else if (peek == JsonToken.NUMBER) {
                return new LazilyParsedNumber(in.nextString());
            } else {
                throw new JsonSyntaxException("Expecting number, got: " + peek);
            }
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
            out.value(value);
        }
    };
    public static final TypeAdapterFactory NUMBER_FACTORY = newFactory(Number.class, NUMBER);
    /*
     * <----------------------- Number TypeAdapter end
     */



    /*
     * Boolean TypeAdapter begin ----------------------->
     */

    public static final TypeAdapter<Boolean> BOOLEAN_AS_STRING = new TypeAdapter<Boolean>() {
        @Override public Boolean read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Boolean.valueOf(in.nextString());
        }

        @Override public void write(JsonWriter out, Boolean value) throws IOException {
            out.value(value == null ? "null" : value.toString());
        }
    };
    /*
     * <----------------------- Boolean TypeAdapter end
     */




    /*
     * Enum TypeAdapter begin ----------------------->
     */

    private static final class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
        private final Map<String, T> nameToConstant = new HashMap<String, T>();
        private final Map<T, String> constantToName = new HashMap<T, String>();

        public EnumTypeAdapter(Class<T> classOfT) {
            try {
                for (T constant : classOfT.getEnumConstants()) {
                    String name = constant.name();
                    SerializedName annotation = classOfT.getField(name).getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        name = annotation.value();
                    }
                    nameToConstant.put(name, constant);
                    constantToName.put(constant, name);
                }
            } catch (NoSuchFieldException e) {
                throw new AssertionError();
            }
        }
        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return nameToConstant.get(in.nextString());
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value == null ? null : constantToName.get(value));
        }
    }

    public static final TypeAdapterFactory ENUM_FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <T> TypeAdapter<T> create(Cson gson, TypeToken<T> typeToken) {
            Class<? super T> rawType = typeToken.getRawType();
            if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
                return null;
            }
            if (!rawType.isEnum()) {
                rawType = rawType.getSuperclass(); // handle anonymous subclasses
            }
            return (TypeAdapter<T>) new EnumTypeAdapter(rawType);
        }
    };
    /*
     * <----------------------- Enum TypeAdapter end
     */


    /*
     * Integer TypeAdapter begin ----------------------->
     */

    public static final TypeAdapter<Number> INTEGER = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            try {
                return in.nextInt();
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException(e);
            }
        }
        @Override
        public void write(JsonWriter out, Number value) throws IOException {
            out.value(value);
        }
    };

    public static final TypeAdapterFactory INTEGER_FACTORY
            = newFactory(int.class, Integer.class, INTEGER);
    /*
     * <----------------------- Integer TypeAdapter end
     */



    public static <TT> TypeAdapterFactory newFactory(
            final Class<TT> unboxed, final Class<TT> boxed, final TypeAdapter<? super TT> typeAdapter) {
        return new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
            public <T> TypeAdapter<T> create(Cson gson, TypeToken<T> typeToken) {
                Class<? super T> rawType = typeToken.getRawType();
                return (rawType == unboxed || rawType == boxed) ? (TypeAdapter<T>) typeAdapter : null;
            }
            @Override public String toString() {
                return "Factory[type=" + boxed.getName()
                        + "+" + unboxed.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }


    public static <TT> TypeAdapterFactory newFactory(final Class<TT> type, final TypeAdapter<TT> typeAdapter) {

        return new TypeAdapterFactory() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> typeToken) {
                return typeToken.getRawType() == type ? (TypeAdapter<T>) typeAdapter : null;
            }

            @Override
            public String toString() {
                return "Factory[type=" + type.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }
}