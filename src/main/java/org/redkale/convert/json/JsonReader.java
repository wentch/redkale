/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.convert.json;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.redkale.convert.*;
import org.redkale.util.ByteTreeNode;
import org.redkale.util.Utility;

/**
 * JSON数据源
 *
 * <p>详情见: https://redkale.org
 *
 * @author zhangjx
 */
public class JsonReader extends Reader {

    public enum ValueType {
        STRING,
        ARRAY,
        MAP;
    }

    protected int position = -1;

    private char[] text;

    private int limit;

    private CharArray array;

    public JsonReader() {}

    public JsonReader(String json) {
        char[] chs = Utility.charArray(json);
        this.text = chs;
        this.limit = chs.length - 1;
    }

    public JsonReader(char[] text) {
        this(text, 0, text.length);
    }

    public JsonReader(char[] text, int start, int len) {
        this.text = Objects.requireNonNull(text);
        this.position = start - 1;
        this.limit = this.position + len;
    }

    public final JsonReader setText(String text) {
        return setText(Utility.charArray(text));
    }

    public final JsonReader setText(char[] text) {
        return setText(text, 0, text.length);
    }

    public final JsonReader setText(char[] text, int start, int len) {
        this.text = text;
        this.position = start - 1;
        this.limit = this.position + len;
        return this;
    }

    @Override
    public void prepare(byte[] bytes) {
        if (bytes != null) {
            setText(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    protected boolean recycle() {
        this.position = -1;
        this.limit = -1;
        this.text = null;
        if (this.array != null) {
            if (this.array.content.length > JsonWriter.DEFAULT_SIZE * 200) {
                this.array = null;
            } else {
                this.array.clear();
            }
        }
        return true;
    }

    protected final CharArray array() {
        if (array == null) {
            array = new CharArray();
        }
        return array.clear();
    }

    public void close() {
        this.recycle();
    }

    /**
     * 找到指定的属性值 例如: {id : 1, data : { name : 'a', items : [1,2,3]}} seek('data.items') 直接跳转到 [1,2,3];
     *
     * @param key 指定的属性名
     */
    public final void seek(String key) {
        if (key == null || key.length() < 1) {
            return;
        }
        final String[] keys = key.split("\\.");
        nextGoodChar(true); // 读掉 { [
        for (String key1 : keys) {
            while (this.hasNext()) {
                String field = this.readStandardString();
                readColon();
                if (key1.equals(field)) {
                    break;
                }
                skipValue();
            }
        }
    }

    /** 跳过属性的值 */
    @Override
    public final void skipValue() {
        final char ch = nextGoodChar(true);
        switch (ch) {
            case '"':
            case '\'':
                backChar(ch);
                readString();
                break;
            case '{':
                while (hasNext()) {
                    this.readStandardString(); // 读掉field
                    this.readColon();
                    this.skipValue();
                }
                break;
            case '[':
                while (hasNext()) {
                    this.skipValue();
                }
                break;
            default:
                char c;
                for (; ; ) {
                    c = nextChar();
                    if (c <= ' ') {
                        return;
                    }
                    if (c == '}' || c == ']' || c == ',' || c == ':') {
                        backChar(c);
                        return;
                    }
                }
        }
    }

    /**
     * 读取下一个字符， 不跳过空白字符
     *
     * @return 空白字符或有效字符
     */
    protected char nextChar() {
        int p = ++this.position;
        if (p > limit) {
            return 0;
        }
        return this.text[p];
    }

    /**
     * 跳过空白字符、单行或多行注释， 返回一个非空白字符
     *
     * @param allowComment 是否容许含注释
     * @return 有效字符
     */
    protected char nextGoodChar(boolean allowComment) {
        char c = this.text[++this.position];
        if (c > ' ' && c != '/') {
            return c;
        }
        char[] text0 = this.text;
        int end = this.limit;
        int curr = this.position;
        for (; curr <= end; curr++) {
            c = text0[curr];
            if (c > ' ') {
                if (c == '/' && allowComment) { // 支持单行和多行注释
                    char n = text0[++curr];
                    if (n == '/') { // 单行注释
                        for (++curr; curr <= end; curr++) {
                            if (text0[curr] == '\n') {
                                break;
                            }
                        }
                        this.position = curr;
                        return nextGoodChar(allowComment);
                    } else if (n == '*') { // 多行注释
                        char nc;
                        char lc = 0;
                        for (++curr; curr <= end; curr++) {
                            nc = text0[curr];
                            if (nc == '/' && lc == '*') {
                                break;
                            }
                            lc = nc;
                        }
                        this.position = curr;
                        return nextGoodChar(allowComment);
                    } else {
                        throw new ConvertException(
                                "illegal escape(" + n + ") (position = " + curr + ") in " + new String(text));
                    }
                }
                break;
            }
        }
        this.position = curr;
        return c;
    }

    /**
     * 回退最后读取的字符
     *
     * @param ch 后退的字符
     */
    protected void backChar(char ch) {
        this.position--;
    }

    /**
     * 是否{开头的对象字符
     *
     * @return 是否对象字符
     */
    public final boolean isNextObject() {
        char ch = nextGoodChar(true);
        backChar(ch);
        return ch == '{';
    }

    /**
     * 是否[开头的数组字符
     *
     * @return 是否数组字符
     */
    public final boolean isNextArray() {
        char ch = nextGoodChar(true);
        backChar(ch);
        return ch == '[';
    }

    public final ValueType readType() {
        char ch = nextGoodChar(true);
        if (ch == '{') {
            backChar(ch);
            return ValueType.MAP;
        }
        if (ch == '[') {
            backChar(ch);
            return ValueType.ARRAY;
        }
        backChar(ch);
        return ValueType.STRING;
    }

    /**
     * 读取对象，返回false表示对象为null
     *
     * @param decoder Decodeable
     * @return 是否存在对象
     */
    @Override
    public boolean readObjectB(final Decodeable decoder) {
        if (this.text.length == 0) {
            return false;
        }
        char ch = nextGoodChar(true);
        if (ch == '{') {
            return true;
        }
        if (ch == 'n' && text[++position] == 'u' && text[++position] == 'l' && text[++position] == 'l') {
            return false;
        }
        if (ch == 'N' && text[++position] == 'U' && text[++position] == 'L' && text[++position] == 'L') {
            return false;
        }
        throw new ConvertException("a json object must begin with '{' (position = " + position + ") but '" + ch
                + "' in " + new String(this.text));
    }

    @Override
    public final void readObjectE() {
        // do nothing
    }

    /**
     * 读取数组，返回false表示数组为null
     *
     * @param componentDecoder Decodeable
     * @return 是否存在对象
     */
    @Override
    public boolean readArrayB(Decodeable componentDecoder) {
        if (this.text.length == 0) {
            return false;
        }
        char ch = nextGoodChar(true);
        if (ch == '[') {
            return true;
        }
        if (ch == '{') {
            return true;
        }
        if (ch == 'n' && text[++position] == 'u' && text[++position] == 'l' && text[++position] == 'l') {
            return false;
        }
        if (ch == 'N' && text[++position] == 'U' && text[++position] == 'L' && text[++position] == 'L') {
            return false;
        }
        throw new ConvertException("a json array text must begin with '[' (position = " + position + ") but '" + ch
                + "' in " + new String(this.text));
    }

    @Override
    public final void readArrayE() {
        // do nothing
    }

    /**
     * 读取map，返回false表示map为null
     *
     * @param keyDecoder Decodeable
     * @param valueDecoder Decodeable
     * @return 是否存在对象
     */
    @Override
    public final boolean readMapB(Decodeable keyDecoder, Decodeable valueDecoder) {
        return readArrayB(keyDecoder);
    }

    @Override
    public final void readMapE() {
        // do nothing
    }

    /** 判断下一个非空白字符是否: */
    @Override
    public void readColon() {
        char ch = nextGoodChar(true);
        if (ch == ':') {
            return;
        }
        throw new ConvertException(
                "expected a ':' but '" + ch + "'(position = " + position + ") in " + new String(this.text));
    }

    @Override
    public int position() {
        return this.position;
    }

    /**
     * 判断对象是否存在下一个属性或者数组是否存在下一个元素
     *
     * @return 是否存在
     */
    @Override
    public final boolean hasNext() {
        char ch = nextGoodChar(true);
        if (ch == ',') {
            char nt = nextGoodChar(true);
            if (nt == '}' || nt == ']') {
                return false;
            }
            backChar(nt);
            return true;
        }
        if (ch == '}' || ch == ']') {
            return false;
        }
        backChar(ch); // { [ 交由 readObjectB 或 readMapB 或 readArrayB 读取
        return true;
    }

    /**
     * 读取一个int值
     *
     * @return int值
     */
    @Override
    public int readInt() {
        char firstchar = nextGoodChar(true);
        char quote = 0;
        if (firstchar == '"' || firstchar == '\'') {
            quote = firstchar;
            firstchar = nextGoodChar(false);
            if (firstchar == quote) {
                return 0;
            }
        }
        int value = 0;
        final boolean negative = firstchar == '-';
        if (negative) { // 负数
            firstchar = nextChar();
            if (firstchar != 'N' && firstchar != 'I') {
                if (firstchar < '0' || firstchar > '9') {
                    throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
                }
                value = digits[firstchar];
            }
        } else { // 正数
            if (firstchar == '+') {
                firstchar = nextChar(); // 兼容+开头的
            }
            if (firstchar != 'N' && firstchar != 'I') {
                if (firstchar < '0' || firstchar > '9') {
                    throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
                }
                value = digits[firstchar];
            }
        }
        if (firstchar == 'N') {
            if (negative) {
                throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
            }
            if (nextChar() != 'a' || nextChar() != 'N') {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            if (quote > 0 && nextChar() != quote) {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            return 0; // NaN 返回0;
        } else if (firstchar == 'I') { // Infinity
            if (nextChar() != 'n'
                    || nextChar() != 'f'
                    || nextChar() != 'i'
                    || nextChar() != 'n'
                    || nextChar() != 'i'
                    || nextChar() != 't'
                    || nextChar() != 'y') {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            if (quote > 0 && nextChar() != quote) {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            return negative ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        int curr = ++this.position;
        int end = this.limit;
        char[] text0 = this.text;
        char ch;
        boolean dot = false;
        if (value > 0) { // 十进制
            for (; curr <= end; curr++) {
                ch = text0[curr];
                if (ch >= '0' && ch <= '9') {
                    if (dot) { // 兼容 123.456
                        continue;
                    }
                    value = (value << 3) + (value << 1) + digits[ch];
                } else if (ch == ',' || ch == '}' || ch == ']' || ch <= ' ' || ch == ':') {
                    curr--;
                    break;
                } else if (ch == quote) {
                    break;
                } else if (quote > 0 && ch <= ' ') { // 兼容 "123 "
                    // do nothing
                } else if (ch == '.') {
                    dot = true;
                } else {
                    throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                }
            }
        } else {
            boolean hex = false;
            for (; curr <= end; curr++) {
                ch = text0[curr];
                if (ch >= '0' && ch <= '9') {
                    if (dot) { // 兼容 123.456
                        continue;
                    }
                    value = (hex ? (value << 4) : ((value << 3) + (value << 1))) + digits[ch];
                } else if (ch == quote) {
                    break;
                } else if (ch == 'x' || ch == 'X') {
                    if (value != 0) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    hex = true;
                } else if (ch >= 'a' && ch <= 'f') {
                    if (!hex) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    if (dot) {
                        continue;
                    }
                    value = (value << 4) + digits[ch];
                } else if (ch >= 'A' && ch <= 'F') {
                    if (!hex) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    if (dot) {
                        continue;
                    }
                    value = (value << 4) + digits[ch];
                } else if (quote > 0 && ch <= ' ') { // 兼容 "123 "
                    // do nothing
                } else if (ch == '.') {
                    dot = true;
                } else if (ch == ',' || ch == '}' || ch == ']' || ch <= ' ' || ch == ':') {
                    curr--;
                    break;
                } else {
                    throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                }
            }
        }
        this.position = curr;
        return negative ? -value : value;
    }

    /**
     * 读取一个long值
     *
     * @return long值
     */
    @Override
    public long readLong() {
        char firstchar = nextGoodChar(true);
        char quote = 0;
        if (firstchar == '"' || firstchar == '\'') {
            quote = firstchar;
            firstchar = nextGoodChar(false);
            if (firstchar == '"' || firstchar == '\'') {
                return 0L;
            }
        }
        long value = 0;
        final boolean negative = firstchar == '-';
        if (negative) { // 负数
            firstchar = nextChar();
            if (firstchar != 'N' && firstchar != 'I') {
                if (firstchar < '0' || firstchar > '9') {
                    throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
                }
                value = digits[firstchar];
            }
        } else { // 正数
            if (firstchar == '+') {
                firstchar = nextChar(); // 兼容+开头的
            }
            if (firstchar != 'N' && firstchar != 'I') {
                if (firstchar < '0' || firstchar > '9') {
                    throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
                }
                value = digits[firstchar];
            }
        }
        if (firstchar == 'N') {
            if (negative) {
                throw new ConvertException("illegal escape(" + firstchar + ") (position = " + position + ")");
            }
            if (nextChar() != 'a' || nextChar() != 'N') {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            if (quote > 0 && nextChar() != quote) {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            return 0L; // NaN 返回0;
        } else if (firstchar == 'I') { // Infinity
            if (nextChar() != 'n'
                    || nextChar() != 'f'
                    || nextChar() != 'i'
                    || nextChar() != 'n'
                    || nextChar() != 'i'
                    || nextChar() != 't'
                    || nextChar() != 'y') {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            if (quote > 0 && nextChar() != quote) {
                throw new ConvertException("illegal escape(" + text[position] + ") (position = " + position + ")");
            }
            return negative ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        int curr = ++this.position;
        int end = this.limit;
        char[] text0 = this.text;
        char ch;
        boolean dot = false;
        if (value > 0) { // 十进制
            for (; curr <= end; curr++) {
                ch = text0[curr];
                if (ch >= '0' && ch <= '9') {
                    if (dot) { // 兼容 123.456
                        continue;
                    }
                    value = (value << 3) + (value << 1) + digits[ch];
                } else if (ch == ',' || ch == '}' || ch == ']' || ch <= ' ' || ch == ':') {
                    curr--;
                    break;
                } else if (ch == quote) {
                    break;
                } else if (quote > 0 && ch <= ' ') { // 兼容 "123 "
                    // do nothing
                } else if (ch == '.') {
                    dot = true;
                } else {
                    throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                }
            }
        } else {
            boolean hex = false;
            for (; curr <= end; curr++) {
                ch = text0[curr];
                if (ch >= '0' && ch <= '9') {
                    if (dot) {
                        continue;
                    }
                    value = (hex ? (value << 4) : ((value << 3) + (value << 1))) + digits[ch];
                } else if (ch == quote) {
                    break;
                } else if (ch == 'x' || ch == 'X') {
                    if (value != 0) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    hex = true;
                } else if (ch >= 'a' && ch <= 'f') {
                    if (!hex) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    if (dot) {
                        continue;
                    }
                    value = (value << 4) + digits[ch];
                } else if (ch >= 'A' && ch <= 'F') {
                    if (!hex) {
                        throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                    }
                    if (dot) {
                        continue;
                    }
                    value = (value << 4) + digits[ch];
                } else if (quote > 0 && ch <= ' ') { // 兼容 "123 "
                    // do nothing
                } else if (ch == '.') {
                    dot = true;
                } else if (ch == ',' || ch == '}' || ch == ']' || ch <= ' ' || ch == ':') {
                    curr--;
                    break;
                } else {
                    throw new ConvertException("illegal escape(" + ch + ") (position = " + position + ")");
                }
            }
        }
        this.position = curr;
        return negative ? -value : value;
    }

    public final String readFieldName() {
        return this.readStandardString();
    }

    @Override
    public DeMember readField(final DeMemberInfo memberInfo) {
        final int eof = this.limit;
        if (this.position == eof) {
            return null;
        }
        ByteTreeNode<DeMember> node = memberInfo.getMemberNode();
        char ch = nextGoodChar(true); // 需要跳过注释
        final char[] text0 = this.text;
        int curr = this.position;
        if (ch == '"' || ch == '\'') {
            final char quote = ch;
            while ((ch = text0[++curr]) != quote) {
                if (node != null) {
                    node = node.getNode(ch);
                }
            }
            this.position = curr;
            return node == null ? null : node.getValue();
        } else {
            if (node != null) {
                node = node.getNode(ch);
            }
            for (; ; ) {
                if (curr == eof) {
                    break;
                }
                ch = text0[++curr];
                if (ch == ',' || ch == ']' || ch == '}' || ch <= ' ' || ch == ':') {
                    curr--;
                    break;
                }
                if (node != null) {
                    node = node.getNode(ch);
                }
            }
            this.position = curr;
            return node == null ? null : node.getValue();
        }
    }
    // ------------------------------------------------------------

    @Override
    public final boolean readBoolean() {
        return "true".equalsIgnoreCase(this.readStandardString());
    }

    @Override
    public final byte readByte() {
        return (byte) readInt();
    }

    @Override
    public final byte[] readByteArray() {
        boolean has = readArrayB(null);
        if (!has) {
            return null;
        }
        int size = 0;
        byte[] data = new byte[8];
        while (hasNext()) {
            if (size >= data.length) {
                byte[] newdata = new byte[data.length + 4];
                System.arraycopy(data, 0, newdata, 0, size);
                data = newdata;
            }
            data[size++] = readByte();
        }
        readArrayE();
        byte[] newdata = new byte[size];
        System.arraycopy(data, 0, newdata, 0, size);
        return newdata;
    }

    @Override
    public final char readChar() {
        return (char) readInt();
    }

    @Override
    public final short readShort() {
        return (short) readInt();
    }

    @Override
    public final float readFloat() {
        String chars = readStandardString();
        if (chars != null) {
            chars = chars.trim();
        }
        if (chars == null || chars.isEmpty()) {
            return 0.f;
        }
        switch (chars) {
            case "Infinity":
                return (float) Double.POSITIVE_INFINITY;
            case "-Infinity":
                return (float) Double.NEGATIVE_INFINITY;
            default:
                return Float.parseFloat(chars); // Float.parseFloat能识别NaN
        }
    }

    @Override
    public final double readDouble() {
        String chars = readStandardString();
        if (chars != null) {
            chars = chars.trim();
        }
        if (chars == null || chars.isEmpty()) {
            return 0.0;
        }
        switch (chars) {
            case "Infinity":
                return Double.POSITIVE_INFINITY;
            case "-Infinity":
                return Double.NEGATIVE_INFINITY;
            default:
                return Double.parseDouble(chars); // Double.parseDouble能识别NaN
        }
    }

    @Override
    public String readStandardString() {
        final int eof = this.limit;
        if (this.position == eof) {
            return null;
        }
        char ch = nextGoodChar(true); // 需要跳过注释
        final char[] text0 = this.text;
        int curr = this.position;
        if (ch == '"' || ch == '\'') {
            final char quote = ch;
            final int start = curr + 1;
            for (; ; ) {
                ch = text0[++curr];
                if (ch == '\\') {
                    this.position = curr - 1;
                    return readEscapeValue(quote, start);
                } else if (ch == quote) {
                    break;
                }
            }
            this.position = curr;
            return new String(text0, start, curr - start);
        } else {
            int start = curr;
            for (; ; ) {
                if (curr == eof) {
                    break;
                }
                ch = text0[++curr];
                if (ch == ',' || ch == ']' || ch == '}' || ch <= ' ' || ch == ':') {
                    break;
                }
            }
            int len = curr - start;
            if (len < 1) {
                this.position = curr;
                return String.valueOf(ch);
            }
            this.position = curr - 1;
            if (len == 4
                    && text0[start] == 'n'
                    && text0[start + 1] == 'u'
                    && text0[start + 2] == 'l'
                    && text0[start + 3] == 'l') {
                return null;
            }
            return new String(text0, start, len == eof ? (len + 1) : len);
        }
    }

    /**
     * 读取字符串， 必须是"或者'包围的字符串值
     *
     * @return String值
     */
    @Override
    public final String readString() {
        return readString(true);
    }

    public final String readStringValue() {
        return readString(false);
    }

    protected String readString(boolean flag) {
        final char[] text0 = this.text;
        final int end = limit;
        char quote = nextGoodChar(true);
        int curr = this.position;
        if (quote == '"' || quote == '\'') {
            final int start = ++curr;
            CharArray tmp = null;
            char c;
            for (; curr <= end; curr++) {
                char ch = text0[curr];
                if (ch == quote) {
                    break;
                } else if (ch == '\\') {
                    if (tmp == null) {
                        tmp = array();
                        tmp.append(text0, start, curr - start);
                    }
                    c = text0[++curr];
                    switch (c) {
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            tmp.append(c);
                            break;
                        case 'n':
                            tmp.append('\n');
                            break;
                        case 'r':
                            tmp.append('\r');
                            break;
                        case 'u':
                            int hex = (Character.digit(text0[++curr], 16) << 12)
                                    + (Character.digit(text0[++curr], 16) << 8)
                                    + (Character.digit(text0[++curr], 16) << 4)
                                    + Character.digit(text0[++curr], 16);
                            tmp.append((char) hex);
                            break;
                        case 't':
                            tmp.append('\t');
                            break;
                        case 'b':
                            tmp.append('\b');
                            break;
                        case 'f':
                            tmp.append('\f');
                            break;
                        default:
                            this.position = curr;
                            throw new ConvertException("illegal escape(" + c + ") (position = " + this.position
                                    + ") in " + new String(this.text));
                    }
                } else if (tmp != null) {
                    tmp.append(ch);
                }
            }
            this.position = curr;
            return tmp != null ? tmp.toStringThenClear() : new String(text0, start, curr - start);
        } else { // 不带双引号
            final int start = curr;
            if (quote == 'n'
                    && end >= curr + 3
                    && text0[1 + curr] == 'u'
                    && text0[2 + curr] == 'l'
                    && text0[3 + curr] == 'l') { // 为null或者null开头的字符串
                curr += 3;
                this.position = curr;
                if (curr < end) {
                    char ch = text0[++curr];
                    if (ch == ',' || ch <= ' ' || ch == '}' || ch == ']' || (flag && ch == ':')) {
                        // null值
                        return null;
                    }
                    // null开头的字符串
                    for (; curr <= end; curr++) {
                        ch = text0[curr];
                        if (ch == ',' || ch <= ' ' || ch == '}' || ch == ']' || (flag && ch == ':')) {
                            break;
                        }
                    }
                    if (curr == start) {
                        throw new ConvertException("expected a string after a key but '" + text0[position]
                                + "' (position = " + position + ") in " + new String(this.text));
                    }
                    this.position = curr - 1;
                    return new String(text0, start, curr - start);
                } else { // null值，已到尾部
                    return null;
                }
            } else {
                for (; ; ) {
                    if (curr > end) {
                        break;
                    }
                    char ch = text0[curr];
                    if (ch == ',' || ch <= ' ' || ch == '}' || ch == ']' || (flag && ch == ':')) {
                        break;
                    }
                    curr++;
                }
                if (curr == start) {
                    throw new ConvertException("expected a string after a key but '" + text0[position]
                            + "' (position = " + position + ") in " + new String(this.text));
                }
                this.position = curr - 1;
                return new String(text0, start, curr - start);
            }
        }
    }

    private String readEscapeValue(final char expected, int start) {
        CharArray tmp = this.array();
        final char[] text0 = this.text;
        int curr = this.position;
        tmp.append(text0, start, curr + 1 - start);
        char c;
        for (; ; ) {
            c = text0[++curr];
            if (c == expected) {
                this.position = curr;
                return tmp.toStringThenClear();
            } else if (c == '\\') {
                c = text0[++curr];
                switch (c) {
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        tmp.append(c);
                        break;
                    case 'n':
                        tmp.append('\n');
                        break;
                    case 'r':
                        tmp.append('\r');
                        break;
                    case 'u':
                        int hex = (Character.digit(text0[++curr], 16) << 12)
                                + (Character.digit(text0[++curr], 16) << 8)
                                + (Character.digit(text0[++curr], 16) << 4)
                                + Character.digit(text0[++curr], 16);
                        tmp.append((char) hex);
                        break;
                    case 't':
                        tmp.append('\t');
                        break;
                    case 'b':
                        tmp.append('\b');
                        break;
                    case 'f':
                        tmp.append('\f');
                        break;
                    default:
                        this.position = curr;
                        throw new ConvertException("illegal escape(" + c + ") (position = " + this.position + ") in "
                                + new String(this.text));
                }
            } else {
                tmp.append(c);
            }
        }
    }

    protected static final int[] digits = new int[255];

    static {
        for (int i = 0; i < digits.length; i++) {
            digits[i] = -1; // -1 错误
        }
        for (int i = '0'; i <= '9'; i++) {
            digits[i] = i - '0';
        }
        for (int i = 'a'; i <= 'f'; i++) {
            digits[i] = i - 'a' + 10;
        }
        for (int i = 'A'; i <= 'F'; i++) {
            digits[i] = i - 'A' + 10;
        }
        digits['"'] = digits['\''] = -2; // -2 跳过
        digits[' '] = digits['\t'] = digits['\b'] = digits['\f'] = digits['\r'] = digits['\n'] = -3; // -3可能跳过
        digits[','] = digits['}'] = digits[']'] = digits[':'] = -4; // -4退出
    }

    protected static class CharArray {

        private int count;

        private char[] content = new char[JsonWriter.DEFAULT_SIZE];

        private char[] expand(int len) {
            int newcount = count + len;
            if (newcount <= content.length) {
                return content;
            }
            char[] newdata = new char[Math.max(content.length * 2, newcount)];
            System.arraycopy(content, 0, newdata, 0, count);
            this.content = newdata;
            return newdata;
        }

        public CharArray append(char[] str, int offset, int len) {
            char[] chs = expand(len);
            System.arraycopy(str, offset, chs, count, len);
            count += len;
            return this;
        }

        public CharArray append(char ch) {
            char[] chs = expand(1);
            chs[count++] = ch;
            return this;
        }

        public CharArray clear() {
            this.count = 0;
            return this;
        }

        public char[] content() {
            return content;
        }

        public int length() {
            return count;
        }

        public char[] getChars() {
            return Arrays.copyOfRange(content, 0, count);
        }

        public String toStringThenClear() {
            String s = toString();
            this.count = 0;
            return s;
        }

        @Override
        public String toString() {
            return new String(content, 0, count);
        }
    }
}
