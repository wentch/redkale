/*
 * Copyright (c) 2016-2116 Redkale
 * All rights reserved.
 */
package org.redkale.test.convert;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.redkale.convert.ConvertColumn;
import org.redkale.convert.json.JsonConvert;
import org.redkale.convert.json.JsonFactory;
import org.redkale.convert.pb.ProtobufConvert;
import org.redkale.util.TypeToken;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class GenericEntityTest {
    private static final Type ENTITY_TYPE = new TypeToken<GenericEntity<Long, String, SimpleEntity>>() {}.getType();
    private static final String JSON =
            "{\"oneEntry\":{\"key\":\"aaaa\",\"value\":{\"addr\":\"127.0.0.1:6666\",\"addrs\":[22222,-33333,44444,-55555,66666,-77777,88888,-99999],\"desc\":\"\",\"id\":1000000001,\"lists\":[\"aaaa\",\"bbbb\",\"cccc\"],\"map\":{\"AAA\":111,\"CCC\":333,\"BBB\":-222},\"name\":\"this is name\\n \\\"test\",\"strings\":[\"zzz\",\"yyy\",\"xxx\"]}},\"oneList\":[1234567890],\"oneName\":\"你好\",\"oneStatus\":[true,false],\"oneTimes\":[128]}";

    public static void main(String[] args) throws Throwable {
        GenericEntityTest test = new GenericEntityTest();
        test.runJson1();
        test.runJson2();
        test.runJson3();
        test.runPb1();
        test.runPb2();
        test.runPb3();
    }

    @Test
    public void runJson1() throws Exception {
        System.out.println("-------------------- runJson1 ---------------------------------");
        JsonFactory.root().loadEncoder(TreeNode.class);
        JsonFactory.root().loadEncoder(TreeNode2.class);
        JsonConvert convert = JsonConvert.root();
        GenericEntity<Long, String, SimpleEntity> bean = createBean();
        String json = convert.convertTo(ENTITY_TYPE, bean);
        System.out.println(json);
        System.out.println(convert.convertFrom(ENTITY_TYPE, json).toString());
        Assertions.assertEquals(JSON, json);
    }

    @Test
    public void runJson2() throws Exception {
        JsonConvert convert = JsonConvert.root();
        ByteBuffer in = ConvertHelper.createByteBuffer(createBytes());
        GenericEntity<Long, String, SimpleEntity> bean = convert.convertFrom(ENTITY_TYPE, in);
        Assertions.assertEquals(JSON, bean.toString());
        Supplier<ByteBuffer> out = ConvertHelper.createSupplier();
        ByteBuffer[] buffers = convert.convertTo(out, ENTITY_TYPE, bean);
        Assertions.assertArrayEquals(createBytes(), ConvertHelper.toBytes(buffers));
    }

    @Test
    public void runJson3() throws Exception {
        JsonConvert convert = JsonConvert.root();
        InputStream in = ConvertHelper.createInputStream(createBytes());
        GenericEntity<Long, String, SimpleEntity> bean = convert.convertFrom(ENTITY_TYPE, in);
        Assertions.assertEquals(JSON, bean.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        convert.convertTo(out, ENTITY_TYPE, bean);
        Assertions.assertArrayEquals(createBytes(), out.toByteArray());
    }

    @Test
    public void runPb1() throws Exception {
        System.out.println("-------------------- runPb1 ---------------------------------");
        JsonFactory.root().loadEncoder(TreeNode.class);
        JsonFactory.root().loadEncoder(TreeNode2.class);
        ProtobufConvert convert = ProtobufConvert.root();
        GenericEntity<Long, String, SimpleEntity> bean = createBean();
        byte[] bs = convert.convertTo(ENTITY_TYPE, bean);
        Utility.println("proto0 ", bs);
        String rs = convert.convertFrom(ENTITY_TYPE, bs).toString();
        System.out.println("反解析: " + rs);
        Assertions.assertEquals(JSON, rs);
    }

    @Test
    public void runPb2() throws Exception {
        System.out.println("-------------------- runPb2 ---------------------------------");
        ProtobufConvert convert = ProtobufConvert.root();
        GenericEntity<Long, String, SimpleEntity> bean = createBean();
        byte[] bs = convert.convertTo(ENTITY_TYPE, bean);
        ByteBuffer in = ConvertHelper.createByteBuffer(bs);
        GenericEntity<Long, String, SimpleEntity> rs = convert.convertFrom(ENTITY_TYPE, in);
        Assertions.assertEquals(JSON, rs.toString());
        Supplier<ByteBuffer> out = ConvertHelper.createSupplier();
        ByteBuffer[] buffers = convert.convertTo(out, ENTITY_TYPE, rs);
        byte[] bs2 = ConvertHelper.toBytes(buffers);
        Utility.println("proto1 ", bs);
        Utility.println("proto2 ", bs2);
        Assertions.assertArrayEquals(bs, bs2);
    }

    @Test
    public void runPb3() throws Exception {
        System.out.println("-------------------- runPb3 ---------------------------------");
        ProtobufConvert convert = ProtobufConvert.root();
        GenericEntity<Long, String, SimpleEntity> bean = createBean();
        byte[] bs = convert.convertTo(ENTITY_TYPE, bean);
        Utility.println("proto1 ", bs);
        InputStream in = ConvertHelper.createInputStream(bs);
        GenericEntity<Long, String, SimpleEntity> rs = convert.convertFrom(ENTITY_TYPE, in);
        Assertions.assertEquals(JSON, rs.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        convert.convertTo(out, ENTITY_TYPE, rs);
        byte[] bs2 = out.toByteArray();
        Utility.println("proto2 ", bs2);
        Assertions.assertArrayEquals(bs, bs2);
    }

    private byte[] createBytes() {
        return JSON.getBytes(StandardCharsets.UTF_8);
    }

    private GenericEntity<Long, String, SimpleEntity> createBean() {
        GenericEntity<Long, String, SimpleEntity> bean = new GenericEntity<>();
        bean.setOneName("你好");
        List<Long> list = new ArrayList<>();
        list.add(1234567890L);
        bean.setOneList(list);
        bean.setOneStatus(new AtomicBoolean[] {new AtomicBoolean(true), new AtomicBoolean(false)});
        List<AtomicLong> times = new ArrayList<>();
        times.add(new AtomicLong(128L));
        bean.setOneTimes(times);
        bean.setOneEntry(new Entry<>("aaaa", SimpleEntity.create()));
        return bean;
    }

    public static class TreeNode {
        @ConvertColumn(index = 1)
        public String name;

        @ConvertColumn(index = 2)
        public TreeNode next;
    }

    public static class TreeNode2 {
        @ConvertColumn(index = 1)
        public String name;

        @ConvertColumn(index = 2)
        public List<TreeNode2> next;
    }

    public static class GenericEntity<T, K, V> {

        @ConvertColumn(index = 1)
        private Entry<K, V> oneEntry;

        @ConvertColumn(index = 2)
        private List<? extends T> oneList;

        @ConvertColumn(index = 3)
        private K oneName;

        @ConvertColumn(index = 4)
        private AtomicBoolean[] oneStatus;

        @ConvertColumn(index = 5)
        private List<AtomicLong> oneTimes;

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }

        public Entry<K, V> getOneEntry() {
            return oneEntry;
        }

        public void setOneEntry(Entry<K, V> oneEntry) {
            this.oneEntry = oneEntry;
        }

        public List<? extends T> getOneList() {
            return oneList;
        }

        public void setOneList(List<? extends T> oneList) {
            this.oneList = oneList;
        }

        public K getOneName() {
            return oneName;
        }

        public void setOneName(K oneName) {
            this.oneName = oneName;
        }

        public AtomicBoolean[] getOneStatus() {
            return oneStatus;
        }

        public void setOneStatus(AtomicBoolean[] oneStatus) {
            this.oneStatus = oneStatus;
        }

        public List<AtomicLong> getOneTimes() {
            return oneTimes;
        }

        public void setOneTimes(List<AtomicLong> oneTimes) {
            this.oneTimes = oneTimes;
        }
    }

    public static class Entry<K, V> {

        @ConvertColumn(index = 1)
        private K key;

        @ConvertColumn(index = 2)
        private V value;

        public Entry() {}

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }

        public K getKey() {
            return key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }
}
