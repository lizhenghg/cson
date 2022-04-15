package com.cracker.code.cson;


import com.cracker.code.cson.internal.ConstructorConstructor;
import com.cracker.code.cson.internal.CsonPreconditions;
import com.cracker.code.cson.internal.Primitives;
import com.cracker.code.cson.internal.Streams;
import com.cracker.code.cson.internal.bind.*;
import com.cracker.code.cson.reflect.TypeToken;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.JsonWriter;
import com.cracker.code.cson.stream.MalformedJsonException;

import java.io.*;

import java.lang.reflect.Type;
import java.util.*;


/**
 *
 * Cson，一个对外使用的Json客户端
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-18
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class Cson {

    /**
     * 生成不可执行的json
     */
    static final boolean DEFAULT_JSON_NON_EXECUTABLE = false;

    private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";

    /**
     * TypeAdapter缓存池，不使用static
     * Map集合里面的key为Object时，一般需要重写hashCode()、equals()
    */
    private final Map<TypeToken<?>, TypeAdapter<?>> typeTokenCache
            = Collections.synchronizedMap(new HashMap<>());

    private final ThreadLocal<Map<TypeToken<?>, FutureTypeAdapter<?>>> calls
            = new ThreadLocal<>();

    private final List<TypeAdapterFactory> factories;

    private final ConstructorConstructor constructorConstructor;



    private final boolean serializeNulls;
    private final boolean htmlSafe;
    private final boolean generateNonExecutableJson;
    private final boolean prettyPrinting;

    public Cson() {
        this(FieldNamingPolicy.IDENTITY, Collections.<Type, InstanceCreator<?>>emptyMap(),
                false, false, DEFAULT_JSON_NON_EXECUTABLE, true,
                false, Collections.<TypeAdapterFactory>emptyList());
    }


    Cson (FieldNamingStrategy fieldNamingStrategy, Map<Type, InstanceCreator<?>> instanceCreators,
          boolean serializeNulls, boolean complexMapKeySerialization, boolean generateNonExecutableJson, boolean htmlSafe,
          boolean prettyPrinting, List<TypeAdapterFactory> typeAdapterFactories) {
        this.constructorConstructor = new ConstructorConstructor(instanceCreators);
        this.serializeNulls = serializeNulls;
        this.generateNonExecutableJson = generateNonExecutableJson;
        this.htmlSafe = htmlSafe;
        this.prettyPrinting = prettyPrinting;

        List<TypeAdapterFactory> factories = new ArrayList<>();

        /* 如下为不能被重写的内置类型适配器  */
        // 添加基于JSON_ELEMENT的TypeAdapterFactory
        factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
        // 添加基于Object的TypeAdapterFactory
        factories.add(ObjectTypeAdapter.FACTORY);

        /* 如下为用户自定义的类型适配器 */
        factories.addAll(typeAdapterFactories);


        /* 如下为用于基本平台类型的类型适配器 */
        // 添加基于String的TypeAdapterFactory
        factories.add(TypeAdapters.STRING_FACTORY);
        // 添加基于int的TypeAdapterFactory
        factories.add(TypeAdapters.INTEGER_FACTORY);
        // 添加基于Number的TypeAdapterFactory
        factories.add(TypeAdapters.NUMBER_FACTORY);
        // 添加基于Array的TypeAdapterFactory
        factories.add(ArrayTypeAdapter.FACTORY);


        /* 如下为复合类型和用户定义类型的类型适配器 */
        // 添加基于反射类型的ReflectiveTypeAdapterFactory
        factories.add(new CollectionTypeAdapterFactory(constructorConstructor));
        factories.add(new MapTypeAdapterFactory(constructorConstructor, complexMapKeySerialization));
        factories.add(TypeAdapters.ENUM_FACTORY);
        factories.add(new ReflectiveTypeAdapterFactory(constructorConstructor, fieldNamingStrategy));


        this.factories = Collections.unmodifiableList(factories);
    }

    /**
     * 创建一个TypeAdapter的子类，FutureTypeAdapter
     * @param <T>
     */
    static class FutureTypeAdapter<T> extends TypeAdapter<T> {

        private TypeAdapter<T> delegate;

        public void setDelegate(TypeAdapter<T> typeAdapter) {
            if (delegate != null) {
                throw new AssertionError();
            }
            delegate = typeAdapter;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (delegate == null) {
                throw new AssertionError();
            }
            return delegate.read(in);
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            CsonPreconditions.checkArgument(delegate != null);
            delegate.write(out, value);
        }
    }

    public <T> TypeAdapter<T> getAdapter(Class<T> type) {
        return getAdapter(TypeToken.get(type));
    }

    /**
     * 双缓存获取TypeAdapter
     * @param type TypeToken
     * @param <T> ParameterizedType
     * @return TypeAdapter<T>
     */
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
        TypeAdapter<?> cached = this.typeTokenCache.get(type);
        if (cached != null) {
            return (TypeAdapter<T>) cached;
        }

        Map<TypeToken<?>, FutureTypeAdapter<?>> threadCalls = calls.get();
        boolean requiresThreadLocalCleanup = false;
        if (threadCalls == null) {
            threadCalls = new HashMap<>(2 << 4);
            calls.set(threadCalls);
            requiresThreadLocalCleanup = true;
        }

        FutureTypeAdapter<T> ongoingCall = (FutureTypeAdapter<T>) threadCalls.get(type);
        if (ongoingCall != null) {
            return ongoingCall;
        }

        try {
            FutureTypeAdapter<T> call = new FutureTypeAdapter<>();
            threadCalls.put(type, call);

            for (TypeAdapterFactory factory : factories) {
                TypeAdapter<T> candidate = factory.create(this, type);
                if (candidate != null) {
                    call.setDelegate(candidate);
                    typeTokenCache.put(type, candidate);
                    return candidate;
                }
            }
            throw new IllegalArgumentException("CSON cannot handle " + type);
        } finally {
            threadCalls.remove(type);

            if (requiresThreadLocalCleanup) {
                calls.remove();
            }
        }
    }


    public JsonElement toJsonTree(Object src) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }
        return toJsonTree(src, src.getClass());
    }

    public JsonElement toJsonTree(Object src, Type typeOfSrc) {
        JsonTreeWriter writer = new JsonTreeWriter();
        toJson(src, typeOfSrc, writer);
        return writer.get();
    }

    public String toJson(Object src) {
        if (src == null) {
            return toJson(JsonNull.INSTANCE);
        }
        return toJson(src, src.getClass());
    }

    public String toJson(Object src, Type typeOfSrc) {
        StringWriter writer = new StringWriter();
        toJson(src, typeOfSrc, writer);
        return writer.toString();
    }

    public String toJson(JsonElement jsonElement) {
        StringWriter writer = new StringWriter();
        toJson(jsonElement, writer);
        return writer.toString();
    }

    public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIoException {
        try {
            JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
            toJson(src, typeOfSrc, jsonWriter);
        } catch (IOException e) {
            throw new JsonIoException(e);
        }
    }

    public void toJson(JsonElement jsonElement, Appendable writer) throws JsonIoException {
        try {
            JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
            toJson(jsonElement, jsonWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIoException {
        boolean oldLenient = writer.isLenient();
        writer.setLenient(true);
        boolean oldHtmlSafe = writer.isHtmlSafe();
        writer.setHtmlSafe(htmlSafe);
        boolean oldSerializeNulls = writer.isSerializeNulls();
        writer.setSerializeNulls(serializeNulls);
        try {
            Streams.write(jsonElement, writer);
        } catch (IOException e) {
            throw new JsonIoException(e);
        } finally {
            writer.setLenient(oldLenient);
            writer.setHtmlSafe(oldHtmlSafe);
            writer.setSerializeNulls(oldSerializeNulls);
        }
    }

    @SuppressWarnings("unchecked")
    public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIoException {
        TypeAdapter<?> adapter = getAdapter(TypeToken.get(typeOfSrc));
        boolean oldLenient = writer.isLenient();
        writer.setLenient(true);
        boolean oldHtmlSafe = writer.isHtmlSafe();
        writer.setHtmlSafe(htmlSafe);
        boolean oldSerializeNulls = writer.isSerializeNulls();
        writer.setSerializeNulls(serializeNulls);
        try {
            ((TypeAdapter<Object>) adapter).write(writer, src);
        } catch (IOException e) {
            throw new JsonIoException(e);
        } finally {
            writer.setLenient(oldLenient);
            writer.setHtmlSafe(oldHtmlSafe);
            writer.setSerializeNulls(oldSerializeNulls);
        }
    }

    private JsonWriter newJsonWriter(Writer writer) throws IOException {
        if (generateNonExecutableJson) {
            writer.write(JSON_NON_EXECUTABLE_PREFIX);
        }
        JsonWriter jsonWriter = new JsonWriter(writer);
        if (prettyPrinting) {
            jsonWriter.setIndent("  ");
        }
        jsonWriter.setSerializeNulls(serializeNulls);
        return jsonWriter;
    }



    public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        Object object = fromJson(json, (Type) classOfT);
        return Primitives.wrap(classOfT).cast(object);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }
        StringReader reader = new StringReader(json);
        T target = (T) fromJson(reader, typeOfT);
        return target;
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(Reader json, Type typeOfT) throws JsonIoException, JsonSyntaxException {
        JsonReader jsonReader = new JsonReader(json);
        T object = (T) fromJson(jsonReader, typeOfT);
        assertFullConsumption(object, jsonReader);
        return object;
    }


    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIoException, JsonSyntaxException {
        boolean isEmpty = true;
        boolean oldLenient = reader.isLenient();
        reader.setLenient(true);
        try {
            reader.peek();
            isEmpty = false;
            TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT);
            TypeAdapter<T> typeAdapter = getAdapter(typeToken);
            T object = typeAdapter.read(reader);
            return object;
        } catch (EOFException e) {
            /*
             * For compatibility with JSON 1.5 and earlier, we return null for empty
             * documents instead of throwing.
             */
            if (isEmpty) {
                return null;
            }
            throw new JsonSyntaxException(e);
        } catch (IllegalStateException | IOException e) {
            throw new JsonSyntaxException(e);
        } // TODO(inder): Figure out whether it is indeed right to rethrow this as JsonSyntaxException
        finally {
            reader.setLenient(oldLenient);
        }
    }

    private static void assertFullConsumption(Object obj, JsonReader reader) {
        try {
            if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonIoException("JSON document was not fully consumed.");
            }
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIoException(e);
        }
    }







    @Override
    public String toString() {
        return new StringBuilder("{serializeNulls:")
                .append(serializeNulls)
                .append(",factories:").append(factories)
                .append("}")
                .toString();
    }
}