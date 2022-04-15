package com.cracker.code.cson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * JsonArray，自定义Json数组类
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonArray extends JsonElement implements Iterable<JsonElement> {

    private final List<JsonElement> elements;

    public JsonArray() {
        this.elements = new ArrayList<>();
    }

    @Override
    JsonElement deepCopy() {
        JsonArray result = new JsonArray();
        for (JsonElement element : this.elements) {
            result.add(element.deepCopy());
        }
        return result;
    }

    /**
     * 新增JsonElement
     * @param element JsonElement
     */
    public void add(JsonElement element) {
        if (element == null) {
            element = JsonNull.INSTANCE;
        }
        this.elements.add(element);
    }

    /**
     * 新增JsonArray
     * @param array JsonArray
     */
    public void addAll(JsonArray array) {
        this.elements.addAll(array.elements);
    }

    /**
     * 移除集合中的JsonElement
     * @param element JsonElement
     * @return true is removed successfully
     */
    public boolean remove(JsonElement element) {
        return this.elements.remove(element);
    }

    /**
     * 移除集合中的JsonElement
     * @param index 移除下标
     * @return JsonElement
     */
    public JsonElement remove(int index) {
        return this.elements.remove(index);
    }


    /**
     * 新增String property
     * @param value String property
     */
    public void addProperty(String value) {
        add(createJsonElement(value));
    }

    /**
     * 新增Number property
     * @param number Number property
     */
    public void addProperty(Number number) {
        add(createJsonElement(number));
    }

    /**
     * 新增Boolean property
     * @param bool Boolean property
     */
    public void addProperty(Boolean bool) {
        add(createJsonElement(bool));
    }

    /**
     * 新增Character property
     * @param c Character property
     */
    public void addProperty(Character c) {
        add(createJsonElement(c));
    }

    /**
     * 创建JsonElement
     * @param value any Object
     * @return JsonElement
     */
    private JsonElement createJsonElement(Object value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }


    public boolean contains(JsonElement element) {
        return this.elements.contains(element);
    }

    public int size() {
        return this.elements.size();
    }

    public JsonElement set(int index, JsonElement element) {
        return this.elements.set(index, element);
    }

    public JsonElement get(int index) {
        return this.elements.get(index);
    }


    @Override
    public Iterator<JsonElement> iterator() {
        return this.elements.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof JsonArray && ((JsonArray) obj).elements.equals(this.elements));
    }

    @Override
    public int hashCode() {
        return this.elements.hashCode();
    }
}
