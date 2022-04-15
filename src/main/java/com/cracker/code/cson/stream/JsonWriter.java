package com.cracker.code.cson.stream;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

import static com.cracker.code.cson.stream.JsonScope.*;

/**
 *
 * JsonWriter：Json解析器
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-26
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class JsonWriter implements Closeable, Flushable {

    /**
     * 依据RFC 4627-json格式定义，"所有Unicode字符可以放置在
     * 引号除必须转义的字符外:
     * 引号、反向solidus和控制字符
     * 通过U + 001 (U + 0000 f)。"
     * 
     * 转义' u2028'和' u2029'，JavaScript将其解释为
     * 换行字符。 这可以防止eval()语法失败错误。
     *
     * JS的JSON数据格式中，JSON包含4种基础类型（字符串，数字，布尔和null）和两种结构类型（对象和数组）
     *
     */
    private static final String[] REPLACEMENT_CHARS;
    private static final String[] HTML_SAFE_REPLACEMENT_CHARS;
    
    
    static {
        REPLACEMENT_CHARS = new String[128];
        // 0x：16进制，1f表示十进制的31
        for (int i = 0; i <= 0x1f; i++) {
            // 表示在0-31个下标里，依次使用十六进制0-31去填充
            // \u0000、\u0001、\u0002 ... 详情可以查看最后面的二进制、十进制、十六进制转换表
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
        }
        REPLACEMENT_CHARS['"'] = "\\\"";
        REPLACEMENT_CHARS['\\'] = "\\\\";
        REPLACEMENT_CHARS['\t'] = "\\t";
        REPLACEMENT_CHARS['\b'] = "\\b";
        REPLACEMENT_CHARS['\n'] = "\\n";
        REPLACEMENT_CHARS['\r'] = "\\r";
        REPLACEMENT_CHARS['\f'] = "\\f";
        HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
        HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
        HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
        HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
        HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
        HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
    }


    /**
     * 最多包含一个顶级数组或对象的输出数据
     */
    private final Writer out;

    /**
     * 堆栈操作记录表
     */
    private int[] stack = new int[32];
    private int stackSize = 0;



    {
        push(EMPTY_DOCUMENT);
    }


    /**
     * 这个属性叫做放宽、宽大、仁慈
     * 配置这个属性为true，去放松其语法规则。
     * 在JS的JSON数据定义中，任何类型的顶层值，用严格的书写，顶层value必须是一个对象或数组
     *
     * lenient宽大处理下列事项：
     * 1、任何类型的顶层值，用严格的书写，顶层value必须是一个对象或数组
     * 2、数字可以是 {@link Double#isNaN()} NaNs} or {@link Double#isInfinite() infinities}.
     *
     * 在JS中，NaN是属于JavaScript的数值类型Number类型，意思是指数据not a number不是一个数字
     * let a = "abcdef";
     * let b = 7;
     * c = a * b;
     * console.log(c); // NaN
     * 在java中，也存在NaN，在Number的子对象Float和Double中，${@link Float#isNaN() NaNs} or
     * {@link Double#isNaN()} NaNs} or {@link Double#isInfinite() infinities}
     *
     */
    private boolean lenient;

    public boolean isLenient() {
        return lenient;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }


    /**
     * 解析json时，每个key:value中的key指
     */
    private String deferredName;

    /**
     * 是否接受key:value中，value可为空情况
     */
    private boolean serializeNulls = true;


    public boolean isSerializeNulls() {
        return serializeNulls;
    }

    public void setSerializeNulls(boolean serializeNulls) {
        this.serializeNulls = serializeNulls;
    }

    /**
     * The name/value separator; either ":" or ": ".
     */
    private String separator = ":";

    /**
     * A string containing a full set of spaces for a single level of
     * indentation, or null for no pretty printing
     */
    private String indent;


    public final void setIndent(String indent) {
        if (indent.length() == 0) {
            this.indent = null;
            this.separator = ":";
        } else {
            this.indent = indent;
            this.separator = ": ";
        }
    }


    private boolean htmlSafe;

    public boolean isHtmlSafe() {
        return htmlSafe;
    }

    public void setHtmlSafe(boolean htmlSafe) {
        this.htmlSafe = htmlSafe;
    }



    public JsonWriter(Writer out) {
        if (out == null) {
            throw new NullPointerException("out == null");
        }
        this.out = out;
    }

    /**
     * A Flushable is a destination of data that can be flushed.  The
     * flush method is invoked to write any buffered output to the underlying stream
     *
     * 实现Flushable接口，可让缓冲区的数据实现可刷新。
     * 调用Flush方法将任何缓冲输出写入底层流。调用flush方法，底层也就是直接调用操作系统提供的接口
     *
     * for instance:
     * 操作系统在写文件时，先把要写的内容从用户缓冲区复制到内核缓冲区，等待真正的写入到"文件"
     * java中的Flushable.flush()方法显然也是调用操作系统提供的接口。不管怎么调用，他们的原理都是一样的，比如要写
     * 4K大小的文件，操作系统有几种策略把字节写入到"文件"中：
     * 1、应用程序每写一个字节，操作系统马上把这个字节写入"文件"
     * 2、应用程序写入字节后，操作系统不马上写入，而是先把它缓存起来，到达一定数量时才写入"文件"
     * 3、应用程序写入字节后，没有到达可写的字节数量时，操作系统不写入，而是由应用程序控制
     *
     * 第一种，显然效率太低，不可取
     * 第二种，现代大部分操作系统也都这样实现的。那么问题来了，当写入一定数量的字节后，虽然还没有达到操作系统可写入的数量，
     * 但是应用程序有这个需求说，我得马上写入。那怎么办？为了应对这种策略，操作系统提供了flush系统调用，让应用程序可以控制何时马上写入文件。
     * 这也是第三种策略。
     *
     * 应用程序写入字节数不足以达到操作系统要写入的数量，而且没有调用flush方法，那这些字节是不是就丢失了？答案是否定的，当打开一个文件句柄，
     * 不管写入多少字节的内容，在调用close方法时，系统会自动写入未写的内容，很多操作系统的close方法实现中就有调用flush方法的部分。
     *
     * @throws IOException IOException
     */
    @Override
    public void flush() throws IOException {
        if (stackSize == 0) {
            throw new IllegalStateException("JsonWriter is closed.");
        }
        this.out.flush();
    }

    /**
     * 1、初始化堆栈记录数组，赋值空document
     * 2、对堆栈记录数组进行扩容
     *
     * 这里不加锁，是因为在多线程场景下，每执行一次json解析，都会创建一个新的JsonWriter
     * @param newTop 新的头部
     */
    private void push(int newTop) {
        if (stackSize == stack.length) {
            int[] newStack = new int[stackSize * 2];
            System.arraycopy(stack, 0, newStack, 0, stackSize);
            stack = newStack;
        }
        // 先赋值再自增
        stack[stackSize++] = newTop;
    }

    /**
     * @return the value on the top of the stack.
     */
    private int peek() {
        if (stackSize == 0) {
            throw new IllegalStateException("JsonWriter is closed.");
        }
        return stack[stackSize - 1];
    }

    /**
     * 把读取到的key:value中的key赋值给deferredName
     * @param name the name of the forthcoming value. Must not be null.
     * @return this writer.
     */
    public JsonWriter name(String name) throws IOException{
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (deferredName != null) {
            throw new IllegalStateException();
        }
        if (this.stackSize == 0) {
            throw new IllegalStateException("JsonWriter is closed.");
        }
        this.deferredName = name;
        return this;
    }

    /**
     * Write a null into the writer
     * @return a writer of json
     */
    public JsonWriter nullValue() throws IOException {
        if (deferredName != null) {
            if (serializeNulls) {
                writeDeferredName();
            } else {
                deferredName = null;
                // 举个例子：假如你输入的是{"country": "China", "name": null}
                // 那么这里直接忽略掉name属性，只返回{"country": "China"}
                return this;
            }
        }
        beforeValue(false);
        this.out.write("null");
        return this;
    }

    /**
     * beforeName()：在整个解析的json后面加逗号,这个是针对一个对象{}内部的。如果需要，再划线\n，并且操作堆栈记录表stack，修改顶部top值
     * string()：把deferredName写在最后面
     * 3、赋空值给deferredName
     */
    private void writeDeferredName() throws IOException {
        if (deferredName != null) {
            beforeName();
            string(deferredName);
            deferredName = null;
        }
    }


    /**
     * 1、对异常进程进行处理
     * 2、修改堆栈数组最顶部数字
     * 3、如果需要，美丽打印json
     * @param root is top-level value or not
     * @throws IOException IOException
     */
    private void beforeValue(boolean root) throws IOException {

        switch (peek()) {
            case NONEMPTY_DOCUMENT:
                if (!lenient) {
                    throw new IllegalStateException(
                            "JSON must have only top-level value.");
                }
                break;

            case EMPTY_DOCUMENT:
                if (!lenient && !root) {
                    throw new IllegalStateException(
                            "JSON must start with an array or an object.");
                }
                replaceTop(NONEMPTY_DOCUMENT);
                break;

            case EMPTY_ARRAY:
                replaceTop(NONEMPTY_ARRAY);
                newline();
                break;

            case NONEMPTY_ARRAY:
                this.out.append(',');
                newline();
                break;

            case DANGLING_NAME:
                this.out.append(separator);
                replaceTop(NONEMPTY_OBJECT);
                break;

            default:
                throw new IllegalStateException("Nesting problem.");
        }
    }


    /**
     * modify the top of the stack array
     * @param topOfStack the top number
     */
    private void replaceTop(int topOfStack) {
        this.stack[stackSize - 1] = topOfStack;
    }

    /**
     * 专门处理一个对象{}里的全部属性
     * 1、给对象{}里面的各个key:value之间设置,
     * 2、如果需要，美丽打印json
     * 3、把stack数组最顶部数字替换为DANGLING_NAME
     * @throws IOException IOException
     */
    private void beforeName() throws IOException {
        int context = peek();
        if (context == NONEMPTY_OBJECT) {
            this.out.write(',');
        } else if (context != EMPTY_OBJECT) {
            throw new IllegalStateException("Nesting problem.");
        }
        newline();
        replaceTop(DANGLING_NAME);
    }

    /**
     * 缩排、压痕打印
     * @throws IOException IOException
     */
    private void newline() throws IOException {
        if (this.indent == null) {
            return;
        }

        this.out.write("\n");
        for (int i = 1, size = stackSize; i < size; i++) {
            this.out.write(indent);
        }
    }

    /**
     * 逐个字符解析，然后write down
     * @param value String value
     * @throws IOException IOException
     */
    private void string(String value) throws IOException {
        String[] replacements = htmlSafe ? HTML_SAFE_REPLACEMENT_CHARS : REPLACEMENT_CHARS;
        this.out.write("\"");
        int last = 0;
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            String replacement;
            if (c < 128) {
                replacement = replacements[c];
                if (replacement == null) {
                    continue;
                }
            } else if (c == '\u2028') {
                replacement = "\\u2028";
            } else if (c == '\u2029') {
                replacement = "\\u2029";
            } else {
                continue;
            }
            if (last < i) {
                this.out.write(value, last, i - last);
            }
            this.out.write(replacement);
            last = i + 1;
        }
        if (last < length) {
            this.out.write(value, last, length - last);
        }
        this.out.write("\"");
    }

    /**
     * write number
     * @param value Number
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter value(Number value) throws IOException {
        if (value == null) {
            return nullValue();
        }

        writeDeferredName();
        String string = value.toString();
        if (!lenient
                && ("-Infinity".equals(string) || "Infinity".equals(string) || "NaN".equals(string))) {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
        }
        beforeValue(false);
        this.out.append(string);
        return this;
    }


    /**
     * write boolean
     * @param value boolean
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter value(boolean value) throws IOException {
        writeDeferredName();
        beforeValue(false);
        this.out.write(value ? "true" : "false");
        return this;
    }


    /**
     * write String
     * @param value String
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter value(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        writeDeferredName();
        beforeValue(false);
        string(value);
        return this;
    }


    public JsonWriter value(double value) throws IOException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
        }
        writeDeferredName();
        beforeValue(false);
        out.append(Double.toString(value));
        return this;
    }


    public JsonWriter value(long value) throws IOException {
        writeDeferredName();
        beforeValue(false);
        out.write(Long.toString(value));
        return this;
    }

    /**
     * 开始写一个数组的起始部分[
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter beginArray() throws IOException {
        writeDeferredName();
        return open(EMPTY_ARRAY, "[");
    }

    /**
     * 开始写一个对象的起始部分{
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter beginObject() throws IOException {
        writeDeferredName();
        return open(EMPTY_OBJECT, "{");
    }


    /**
     * 写一个数组的结束部分]
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter endArray() throws IOException {
        return close(EMPTY_ARRAY, NONEMPTY_ARRAY, "]");
    }

    /**
     * 写一个对象的结束部分}
     * @return JsonWriter
     * @throws IOException IOException
     */
    public JsonWriter endObject() throws IOException {
        return close(EMPTY_OBJECT, NONEMPTY_OBJECT, "}");
    }


    /**
     * Enters a new scope by appending any necessary whitespace and the given bracket
     * @param empty empty for object or array
     * @param openBracket open bracket
     * @return JsonWriter
     * @throws IOException IOException
     */
    private JsonWriter open(int empty, String openBracket) throws IOException {
        beforeValue(true);
        push(empty);
        this.out.write(openBracket);
        return this;
    }

    /**
     * Closes the current scope by appending any necessary whitespace and the given bracket.
     * @param empty empty for object or array
     * @param nonempty non empty for object or array
     * @param closeBracket close bracket
     * @return JsonWriter
     * @throws IOException IOException
     */
    private JsonWriter close(int empty, int nonempty, String closeBracket) throws IOException {
        int context = peek();
        if (context != nonempty && context != empty) {
            throw new IllegalStateException("Nesting problem.");
        }
        if (deferredName != null) {
            throw new IllegalStateException("Dangling name: " + deferredName);
        }

        stackSize--;
        if (context == nonempty) {
            newline();
        }
        this.out.write(closeBracket);
        return this;
    }

    @Override
    public void close() throws IOException {
        out.close();

        int size = stackSize;
        if (size > 1 || size == 1 && stack[size - 1] != NONEMPTY_DOCUMENT) {
            throw new IOException("Incomplete document");
        }
        stackSize = 0;
    }










    /*
     * 二进制、十进制、十六进制比较表
     * Bin	Dec	Hex	缩写/字符	解释
     * 0	0	0	NUL(null)	空字符
     * 1	1	1	SOH(start of headling)	标题开始
     * 10	2	2	STX (start of text)	正文开始
     * 11	3	3	ETX (end of text)	正文结束
     * 100	4	4	EOT (end of transmission)	传输结束
     * 101	5	5	ENQ (enquiry)	请求
     * 110	6	6	ACK (acknowledge)	收到通知
     * 111	7	7	BEL (bell)	响铃
     * 1000	8	8	BS (backspace)	退格	\b
     * 1001	9	9	HT (horizontal tab)	水平制表符	\t
     * 1010	10	0A	LF (NL line feed, new line)	换行键	\n
     * 1011	11	0B	VT (vertical tab)	垂直制表符
     * 1100	12	0C	FF (NP form feed, new page)	换页键	\f
     * 1101	13	0D	CR (carriage return)	回车键	\r
     * 1110	14	0E	SO (shift out)	不用切换
     * 1111	15	0F	SI (shift in)	启用切换
     * 10000	16	10	DLE (data link escape)	数据链路转义
     * 10001	17	11	DC1 (device control 1)	设备控制1
     * 10010	18	12	DC2 (device control 2)	设备控制2
     * 10011	19	13	DC3 (device control 3)	设备控制3
     * 10100	20	14	DC4 (device control 4)	设备控制4
     * 10101	21	15	NAK (negative acknowledge)	拒绝接收
     * 10110	22	16	SYN (synchronous idle)	同步空闲
     * 10111	23	17	ETB (end of trans. block)	传输块结束
     * 11000	24	18	CAN (cancel)	取消
     * 11001	25	19	EM (end of medium)	介质中断
     * 11010	26	1A	SUB (substitute)	替补
     * 11011	27	1B	ESC (escape)	溢出
     * 11100	28	1C	FS (file separator)	文件分割符
     * 11101	29	1D	GS (group separator)	分组符
     * 11110	30	1E	RS (record separator)	记录分离符
     * 11111	31	1F	US (unit separator)	单元分隔符
     * 100000	32	20	(space)	空格
     * 100001	33	21	!
     * 100010	34	22	"
     * 100011	35	23	#
     * 100100	36	24	$
     * 100101	37	25	%
     * 100110	38	26	&
     * 100111	39	27	'
     * 101000	40	28	(
     * 101001	41	29	)
     * 101010	42	2A	*
     * 101011	43	2B	+
     * 101100	44	2C	,
     * 101101	45	2D	-
     * 101110	46	2E	.
     * 101111	47	2F	/
     * 110000	48	30	0
     * 110001	49	31	1
     * 110010	50	32	2
     * 110011	51	33	3
     * 110100	52	34	4
     * 110101	53	35	5
     * 110110	54	36	6
     * 110111	55	37	7
     * 111000	56	38	8
     * 111001	57	39	9
     * 111010	58	3A	:
     * 111011	59	3B	;
     * 111100	60	3C	<
     * 111101	61	3D	=
     * 111110	62	3E	>
     * 111111	63	3F	?
     * 1000000	64	40	@
     * 1000001	65	41	A
     * 1000010	66	42	B
     * 1000011	67	43	C
     * 1000100	68	44	D
     * 1000101	69	45	E
     * 1000110	70	46	F
     * 1000111	71	47	G
     * 1001000	72	48	H
     * 1001001	73	49	I
     * 1001010	74	4A	J
     * 1001011	75	4B	K
     * 1001100	76	4C	L
     * 1001101	77	4D	M
     * 1001110	78	4E	N
     * 1001111	79	4F	O
     * 1010000	80	50	P
     * 1010001	81	51	Q
     * 1010010	82	52	R
     * 1010011	83	53	S
     * 1010100	84	54	T
     * 1010101	85	55	U
     * 1010110	86	56	V
     * 1010111	87	57	W
     * 1011000	88	58	X
     * 1011001	89	59	Y
     * 1011010	90	5A	Z
     * 1011011	91	5B	[
     * 1011100	92	5C	\
     * 1011101	93	5D	]
     * 1011110	94	5E	^
     * 1011111	95	5F	_
     * 1100000	96	60	`
     * 1100001	97	61	a
     * 1100010	98	62	b
     * 1100011	99	63	c
     * 1100100	100	64	d
     * 1100101	101	65	e
     * 1100110	102	66	f
     * 1100111	103	67	g
     * 1101000	104	68	h
     * 1101001	105	69	i
     * 1101010	106	6A	j
     * 1101011	107	6B	k
     * 1101100	108	6C	l
     * 1101101	109	6D	m
     * 1101110	110	6E	n
     * 1101111	111	6F	o
     * 1110000	112	70	p
     * 1110001	113	71	q
     * 1110010	114	72	r
     * 1110011	115	73	s
     * 1110100	116	74	t
     * 1110101	117	75	u
     * 1110110	118	76	v
     * 1110111	119	77	w
     * 1111000	120	78	x
     * 1111001	121	79	y
     * 1111010	122	7A	z
     * 1111011	123	7B	{
     * 1111100	124	7C	|
     * 1111101	125	7D	}
     * 1111110	126	7E	~
     * 1111111	127	7F	DEL (delete)	删除
     *
     *
     */
}
