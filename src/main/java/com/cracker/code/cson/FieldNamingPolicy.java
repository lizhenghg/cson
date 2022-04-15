package com.cracker.code.cson;

import java.lang.reflect.Field;

/**
 * 序列化Object时，对Object中每个属性的命名政策
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public enum FieldNamingPolicy implements FieldNamingStrategy {

    /**
     * 不做任何modify
     * <ul>
     *     <li>someFieldName --> someFieldName</li>
     * </ul>
     *
     */
    IDENTITY() {
        @Override
        public String translateName(Field f) {
            return f.getName();
        }
    },

    /**
     * 对属性modify如下
     * <ul>
     *     <li>someFieldName --> SomeFieldName</li>
     *     <li>_someFieldName --> _SomeFieldName</li>
     * </ul>
     */
    UPPER_CAMEL_CASE() {
        @Override
        public String translateName(Field f) {
            return upperCaseFirstLetter(f.getName());
        }
    },

    /**
     * 对属性modify如下
     * <ul>
     *     <li>someFieldName --> Some Field Name</li>
     *     <li>_someFieldName --> _Some Field Name</li>
     * </ul>
     */
    UPPER_CAMEL_CASE_WITH_SPACES() {
        @Override
        public String translateName(Field f) {
            return upperCaseFirstLetter(separateCamelCase(f.getName(), " "));
        }
    },

    /**
     * 对属性modify如下
     * <ul>
     *     <li>someFieldName --> some_field_name</li>
     *     <li>_someFieldName --> _some_field_name</li>
     * </ul>
     */
    LOWER_CASE_WITH_UNDERSCORES() {
        @Override
        public String translateName(Field f) {
            return separateCamelCase(f.getName(), "_").toLowerCase();
        }
    },

    /**
     * 对属性modify如下
     * <ul>
     *     <li>someFieldName --> some-field-name</li>
     *     <li>_someFieldName --> _some-field-name</li>
     * </ul>
     */
    LOWER_CASE_WITH_DASHES() {
        @Override
        public String translateName(Field f) {
            return separateCamelCase(f.getName(), "-").toLowerCase();
        }
    };


    private static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }


    private static String upperCaseFirstLetter(String name) {
        StringBuilder fieldNameBuilder = new StringBuilder();
        int index = 0;
        char firstCharacter = name.charAt(index);

        while (index < name.length() - 1) {
            if (Character.isLetter(firstCharacter)) {
                break;
            }

            fieldNameBuilder.append(firstCharacter);
            firstCharacter = name.charAt(++index);
        }

        if (!Character.isUpperCase(firstCharacter)) {
            String modifiedTarget = modifyString(Character.toUpperCase(firstCharacter), name, ++index);
            return fieldNameBuilder.append(modifiedTarget).toString();
        } else {
            return name;
        }
    }

    private static String modifyString(char firstCharacter, String srcString, int indexOfSubstring) {
        return (indexOfSubstring < srcString.length())
                ? firstCharacter + srcString.substring(indexOfSubstring)
                : String.valueOf(firstCharacter);
    }
}
