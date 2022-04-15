package com.cracker.code.cson;

import com.cracker.code.cson.internal.Streams;
import com.cracker.code.cson.stream.JsonReader;
import com.cracker.code.cson.stream.JsonToken;
import com.cracker.code.cson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 *
 * JsonParser: JsonParser
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-05
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonParser {

    public JsonElement parse(String json) throws JsonSyntaxException {
        return parse(new StringReader(json));
    }

    public JsonElement parse(Reader json) throws JsonIoException, JsonSyntaxException {
        try {
            JsonReader jsonReader = new JsonReader(json);
            JsonElement element = parse(jsonReader);
            if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("Did not consume the entire document.");
            }
            return element;
        } catch (MalformedJsonException | NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIoException(e);
        }
    }

    public JsonElement parse(JsonReader json) throws JsonIoException, JsonSyntaxException {
        boolean lenient = json.isLenient();
        json.setLenient(true);
        try {
            return Streams.parse(json);
        } catch (StackOverflowError | OutOfMemoryError e) {
            throw new JsonParseException("Failed parsing JSON source: " + json + " to Json", e);
        } finally {
            json.setLenient(lenient);
        }
    }

}
