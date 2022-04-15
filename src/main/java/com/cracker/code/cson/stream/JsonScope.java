package com.cracker.code.cson.stream;

/**
 *
 * JsonScope：Json读取-解析范围
 *
 * 只能同一包中的类使用
 *
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-27
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class JsonScope {


    /**
     * 空数组
     */
    static final int EMPTY_ARRAY = 1;
    /**
     * 非空数组
     */
    static final int NONEMPTY_ARRAY = 2;
    /**
     * 空对象
     */
    static final int EMPTY_OBJECT = 3;
    /**
     * 摇摆的属性
     */
    static final int DANGLING_NAME = 4;
    /**
     * 非空对象
     */
    static final int NONEMPTY_OBJECT = 5;
    /**
     * 空文档
     */
    static final int EMPTY_DOCUMENT = 6;
    /**
     * 非空文档
     */
    static final int NONEMPTY_DOCUMENT = 7;
    /**
     * 关闭
     */
    static final int CLOSED = 8;
}
