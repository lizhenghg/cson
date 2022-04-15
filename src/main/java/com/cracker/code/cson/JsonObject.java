package com.cracker.code.cson;


import java.util.TreeMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * JsonObject，自定义Json对象类
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonObject extends JsonElement {

    private final TreeMap<String, JsonElement> members = new TreeMap<>();

    /**
     * 这个方法用得好，专门针对各JsonElement嵌套场景，
     * 亮点在于最后的entry.getValue().deepCopy()
     * 不失为深度复制的一种解决思路
     * @return a deepCopy JsonElement
     */
    @Override
    JsonElement deepCopy() {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            result.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return result;
    }

    /**
     * add新增JsonElement元素
     * @param property is the key of the value
     * @param value means be putted JsonElement
     */
    public void add(String property, JsonElement value) {
        if (value == null) {
            value = JsonNull.INSTANCE;
        }
        members.put(property, value);
    }

    /**
     * 移除集合中的元素
     * @param property 属性name
     * @return a packing json element
     */
    public JsonElement remove(String property) {
        return this.members.remove(property);
    }


    /**
     * 添加String属性到集合
     * @param property 属性key
     * @param value    String属性value
     */
    public void addProperty(String property, String value) {
        add(property, createJsonElement(value));
    }

    /**
     * 添加Number属性到集合
     * @param property 属性key
     * @param number   Number属性value
     */
    public void addProperty(String property, Number number) {
        add(property, createJsonElement(number));
    }

    /**
     * 添加Boolean属性到集合
     * @param property 属性key
     * @param value    Boolean属性value
     */
    public void addProperty(String property, Boolean value) {
        add(property, createJsonElement(value));
    }

    /**
     * 添加Character属性到集合
     * @param property 属性key
     * @param value    Character属性value
     */
    public void addProperty(String property, Character value) {
        add(property, createJsonElement(value));
    }

    /**
     * 把字符串、数字、布尔、null这四种数据类型封装为JsonPrimitive
     * @param value may be one of string、number、boolean、null
     * @return a packing json element
     */
    private JsonElement createJsonElement(Object value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    /**
     * 获取集合的entrySet
     * @return entrySet
     */
    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return this.members.entrySet();
    }

    /**
     * 判断集合中是否存在指定property的value
     * @param property 属性key
     * @return true is contain or not
     */
    public boolean has(String property) {
        return this.members.containsKey(property);
    }

    /**
     * 获取JsonElement
     * @param property 属性key
     * @return a packing json element
     */
    public JsonElement get(String property) {
        return this.members.get(property);
    }

    /**
     * 获取JsonPrimitive
     * @param memberName 属性memberName
     * @return a packing json element
     */
    public JsonPrimitive getAsJsonPrimitive(String memberName) {
        return (JsonPrimitive) this.members.get(memberName);
    }

    /**
     * 获取JsonObject
     * @param memberName 属性memberName
     * @return a packing json element
     */
    public JsonObject getAsJsonObject(String memberName) {
        return (JsonObject) this.members.get(memberName);
    }

    /**
     * 获取JsonArray
     * @param memberName 属性memberName
     * @return a packing json element
     */
    public JsonArray getAsJsonArray(String memberName) {
        return (JsonArray) this.members.get(memberName);
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) ||
                (o instanceof JsonObject &&  ((JsonObject) o).members.equals(this.members));
    }

    @Override
    public int hashCode() {
        return this.members.hashCode();
    }

}
