package com.cracker.code.cson.internal.bind;

import com.cracker.code.cson.*;
import com.cracker.code.cson.annotations.SerializedName;
import com.cracker.code.cson.internal.ConstructorConstructor;
import com.cracker.code.cson.internal.CsonTypes;
import com.cracker.code.cson.internal.ObjectConstructor;
import com.cracker.code.cson.internal.Primitives;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * 反射型的的类型适配器工厂，适合所有的
 * class instance of Object
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-02
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;
    private final FieldNamingStrategy fieldNamingStrategy;

    public ReflectiveTypeAdapterFactory(final ConstructorConstructor constructorConstructor,
                                        final FieldNamingStrategy fieldNamingStrategy) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingStrategy = fieldNamingStrategy;
    }

    private String getFieldName(Field f) {
        return getFieldName(this.fieldNamingStrategy, f);
    }

    private String getFieldName(FieldNamingStrategy fieldNamingStrategy, Field f) {
        final SerializedName serializedName = f.getAnnotation(SerializedName.class);
        return (serializedName == null) ? fieldNamingStrategy.translateName(f) : serializedName.value();
    }


    @Override
    public <T> TypeAdapter<T> create(Cson cson, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        // 把所有的类型都囊括
        if (!Object.class.isAssignableFrom(raw)) {
            return null;
        }
        ObjectConstructor<T> constructor = this.constructorConstructor.get(type);
        return new Adapter<T>(constructor, this.getBoundFields(cson, type, raw));
    }


    /**
     * 自定义属性封装类
     */
    abstract static class BoundField {
        String name;
        boolean serialized;
        boolean deSerialized;

        protected BoundField(String name, boolean  serialized, boolean deSerialized) {
            this.name = name;
            this.serialized = serialized;
            this.deSerialized = deSerialized;
        }

        /**
         * 是否可执行write into操作
         * @param src Object
         * @return true can be written
         * @throws IOException IOException
         * @throws IllegalAccessException IllegalAccessException
         */
        abstract boolean writeField(Object src) throws IOException, IllegalAccessException;

        /**
         * 执行write操作
         * @param out JsonWriter
         * @param src Object
         * @throws IOException IOException
         * @throws IllegalAccessException IllegalAccessException
         */
        abstract void write(JsonWriter out, Object src) throws IOException, IllegalAccessException;

        /**
         * 执行read操作
         * @param in JsonReader
         * @param src Object
         * @throws IOException IOException
         * @throws IllegalAccessException IllegalAccessException
         */
        abstract void read(JsonReader in, Object src) throws IOException, IllegalAccessException;
    }


    public static final class Adapter<T> extends TypeAdapter<T> {
        private final ObjectConstructor<T> constructor;
        private final Map<String, BoundField> boundFields;

        private Adapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            T instance = this.constructor.construct();
            try {
                in.beginObject();
                while (in.hasNext()) {
                    final String name = in.nextName();
                    final BoundField field = this.boundFields.get(name);
                    if (field == null || !field.deSerialized) {
                        in.skipValue();
                    }
                    else {
                        field.read(in, instance);
                    }
                }
            }
            catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            }
            catch (IllegalAccessException e2) {
                throw new AssertionError(e2);
            }
            in.endObject();
            return instance;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null || value instanceof JsonNull) {
                out.nullValue();
                return;
            }
            out.beginObject();
            try {
                for (final BoundField boundField : this.boundFields.values()) {
                    if (boundField.writeField(value)) {
                        out.name(boundField.name);
                        boundField.write(out, value);
                    }
                }
            }
            catch (IllegalAccessException e) {
                throw new AssertionError();
            }
            out.endObject();
        }
    }

    private Map<String, BoundField> getBoundFields(Cson context, TypeToken<?> type, Class<?> raw) {
        Map<String, BoundField> result = new LinkedHashMap<>();
        if (raw.isInterface()) {
            return result;
        }
        Type declaredType = type.getType();
        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                // 测试可删。这里先忽略
//                final boolean serialize = this.excludeField(field, true);
//                final boolean deserialize = this.excludeField(field, false);
                field.setAccessible(true);
                Type fieldType = CsonTypes.resolve(type.getType(), raw, field.getGenericType());
                BoundField boundField = this.createBoundField(context, field, this.getFieldName(field), TypeToken.get(fieldType));
                BoundField previous = result.put(boundField.name, boundField);
                if (previous != null) {
                    throw new IllegalArgumentException(declaredType + " declares multiple JSON fields named " + previous.name);
                }
            }
            type = TypeToken.get(CsonTypes.resolve(type.getType(), raw, raw.getGenericSuperclass()));
            raw = type.getRawType();
        }
        return result;
    }

    private BoundField createBoundField(Cson context, Field field, String name,
                                        TypeToken<?> fieldType) {
        boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
        return new BoundField(name, true, true) {

            final TypeAdapter<?> typeAdapter = ReflectiveTypeAdapterFactory.this.getFieldAdapter(context, field, fieldType);

            @Override
            boolean writeField(Object src) throws IllegalAccessException {
                if (!this.serialized) {
                    return false;
                }
                Object fieldValue = field.get(src);
                return fieldValue != src;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            void write(JsonWriter out, Object src) throws IllegalAccessException, IOException {
                Object fieldValue = field.get(src);
                TypeAdapter t = new TypeAdapterRuntimeTypeWrapper<>(context, this.typeAdapter, fieldType.getType());
                t.write(out, fieldValue);
            }

            @Override
            void read(JsonReader in, Object src) throws IOException, IllegalAccessException {
                Object fieldValue = this.typeAdapter.read(in);
                if (fieldValue != null || !isPrimitive) {
                    field.set(src, fieldValue);
                }
            }
        };
    }

    // 测试可删，先屏蔽JsonAdapter注解
    private TypeAdapter<?> getFieldAdapter(final Cson cson, final Field field, final TypeToken<?> fieldType) {
//        final JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
//        if (annotation != null) {
//            final TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(this.constructorConstructor, gson, fieldType, annotation);
//            if (adapter != null) {
//                return adapter;
//            }
//        }
        return cson.getAdapter(fieldType);
    }

}
