
package com.cracker.code.cson.internal.bind;



import com.cracker.code.cson.*;
import com.cracker.code.cson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * JsonTreeWriter：JsonTreeWriter
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-05
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonTreeWriter extends JsonWriter {
    private static final Writer UNWRITABLE_WRITER = new Writer() {
        @Override public void write(char[] buffer, int offset, int counter) {
            throw new AssertionError();
        }
        @Override public void flush() throws IOException {
            throw new AssertionError();
        }
        @Override public void close() throws IOException {
            throw new AssertionError();
        }
    };
    private static final JsonPrimitive SENTINEL_CLOSED = new JsonPrimitive("closed");

    private final List<JsonElement> stack = new ArrayList<JsonElement>();

    private String pendingName;

    // TODO: is this really what we want?;
    private JsonElement product = JsonNull.INSTANCE;

    public JsonTreeWriter() {
        super(UNWRITABLE_WRITER);
    }

    /**
     * Returns the top level object produced by this writer.
     */
    public JsonElement get() {
        if (!stack.isEmpty()) {
            throw new IllegalStateException("Expected one JSON element but was " + stack);
        }
        return product;
    }

    private JsonElement peek() {
        return stack.get(stack.size() - 1);
    }



    private void put(JsonElement value) {
        if (pendingName != null) {
            if (!value.isJsonNull() || isSerializeNulls()) {
                JsonObject object = (JsonObject) peek();
                object.add(pendingName, value);
            }
            pendingName = null;
        } else if (stack.isEmpty()) {
            product = value;
        } else {
            JsonElement element = peek();
            if (element instanceof JsonArray) {
                ((JsonArray) element).add(value);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override public JsonWriter beginArray() throws IOException {
        JsonArray array = new JsonArray();
        put(array);
        stack.add(array);
        return this;
    }

    @Override public JsonWriter endArray() throws IOException {
        if (stack.isEmpty() || pendingName != null) {
            throw new IllegalStateException();
        }
        JsonElement element = peek();
        if (element instanceof JsonArray) {
            stack.remove(stack.size() - 1);
            return this;
        }
        throw new IllegalStateException();
    }

    @Override public JsonWriter beginObject() throws IOException {
        JsonObject object = new JsonObject();
        put(object);
        stack.add(object);
        return this;
    }

    @Override public JsonWriter endObject() throws IOException {
        if (stack.isEmpty() || pendingName != null) {
            throw new IllegalStateException();
        }
        JsonElement element = peek();
        if (element instanceof JsonObject) {
            stack.remove(stack.size() - 1);
            return this;
        }
        throw new IllegalStateException();
    }

    @Override public JsonWriter name(String name) throws IOException {
        if (stack.isEmpty() || pendingName != null) {
            throw new IllegalStateException();
        }
        JsonElement element = peek();
        if (element instanceof JsonObject) {
            pendingName = name;
            return this;
        }
        throw new IllegalStateException();
    }

    @Override public JsonWriter value(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        put(new JsonPrimitive(value));
        return this;
    }

    @Override public JsonWriter nullValue() throws IOException {
        put(JsonNull.INSTANCE);
        return this;
    }

    @Override public JsonWriter value(boolean value) throws IOException {
        put(new JsonPrimitive(value));
        return this;
    }

    @Override public JsonWriter value(double value) throws IOException {
        if (!isLenient() && (Double.isNaN(value) || Double.isInfinite(value))) {
            throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
        }
        put(new JsonPrimitive(value));
        return this;
    }

    @Override public JsonWriter value(long value) throws IOException {
        put(new JsonPrimitive(value));
        return this;
    }

    @Override public JsonWriter value(Number value) throws IOException {
        if (value == null) {
            return nullValue();
        }

        if (!isLenient()) {
            double d = value.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
            }
        }

        put(new JsonPrimitive(value));
        return this;
    }

    @Override public void flush() throws IOException {
    }

    @Override public void close() throws IOException {
        if (!stack.isEmpty()) {
            throw new IOException("Incomplete document");
        }
        stack.add(SENTINEL_CLOSED);
    }
}
