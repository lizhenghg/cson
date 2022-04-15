package com.cracker.code.cson.internal;

import com.cracker.code.cson.*;
import com.cracker.code.cson.internal.bind.TypeAdapters;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonWriter;
import com.cracker.code.cson.stream.MalformedJsonException;

import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;

/**
 *
 * Stream流业务操作类
 *
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-28
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class Streams {

    /**
     * Writes this JSON element to the writer, recursively
     * @param element be written element
     * @param writer write container
     */
    public static void write(JsonElement element, JsonWriter writer) throws IOException {
        TypeAdapters.JSON_ELEMENT.write(writer, element);
    }

    @SuppressWarnings("resource")
    public static Writer writerForAppendable(Appendable appendable) {
        return appendable instanceof Writer ? (Writer) appendable : new AppendableWriter(appendable);
    }

    private static final class AppendableWriter extends Writer {
        private final Appendable appendable;
        private final CurrentWrite currentWrite = new CurrentWrite();

        private AppendableWriter(Appendable appendable) {
            this.appendable = appendable;
        }

        @Override public void write(char[] chars, int offset, int length) throws IOException {
            currentWrite.chars = chars;
            appendable.append(currentWrite, offset, offset + length);
        }

        @Override public void write(int i) throws IOException {
            appendable.append((char) i);
        }

        @Override public void flush() {}
        @Override public void close() {}

        /**
         * A mutable char sequence pointing at a single char[].
         */
        static class CurrentWrite implements CharSequence {
            char[] chars;
            @Override
            public int length() {
                return chars.length;
            }
            @Override
            public char charAt(int i) {
                return chars[i];
            }
            @Override
            public CharSequence subSequence(int start, int end) {
                return new String(chars, start, end - start);
            }
        }
    }

    public static JsonElement parse(JsonReader reader) throws JsonParseException {
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return TypeAdapters.JSON_ELEMENT.read(reader);
        } catch (EOFException e) {
            /*
             * For compatibility with JSON 1.5 and earlier, we return a JsonNull for
             * empty documents instead of throwing.
             */
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            // The stream ended prematurely so it is likely a syntax error.
            throw new JsonSyntaxException(e);
        } catch (MalformedJsonException | NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIoException(e);
        }
    }
}
