/*
 * Copyright (c) 2016-2116 Redkale
 * All rights reserved.
 */
package org.redkale.convert.pb;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.redkale.convert.SimpledCoder;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class ProtobufCoders {
    private ProtobufCoders() {
        // do nothing
    }

    public static class ProtobufBoolArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, boolean[]>
            implements ProtobufPrimitivable {

        public static final ProtobufBoolArraySimpledCoder instance = new ProtobufBoolArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, boolean[] values) {
            out.writeBools(values);
        }

        @Override
        public boolean[] convertFrom(ProtobufReader in) {
            return in.readBools();
        }
    }

    public static class ProtobufByteArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, byte[]>
            implements ProtobufPrimitivable {

        public static final ProtobufByteArraySimpledCoder instance = new ProtobufByteArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, byte[] values) {
            out.writeBytes(values);
        }

        @Override
        public byte[] convertFrom(ProtobufReader in) {
            return in.readBytes();
        }
    }

    public static class ProtobufCharArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, char[]>
            implements ProtobufPrimitivable {

        public static final ProtobufCharArraySimpledCoder instance = new ProtobufCharArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, char[] values) {
            out.writeChars(values);
        }

        @Override
        public char[] convertFrom(ProtobufReader in) {
            return in.readChars();
        }
    }

    public static class ProtobufShortArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, short[]>
            implements ProtobufPrimitivable {

        public static final ProtobufShortArraySimpledCoder instance = new ProtobufShortArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, short[] values) {
            out.writeShorts(values);
        }

        @Override
        public short[] convertFrom(ProtobufReader in) {
            return in.readShorts();
        }
    }

    public static class ProtobufIntArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, int[]>
            implements ProtobufPrimitivable {

        public static final ProtobufIntArraySimpledCoder instance = new ProtobufIntArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, int[] values) {
            out.writeInts(values);
        }

        @Override
        public int[] convertFrom(ProtobufReader in) {
            return in.readInts();
        }
    }

    public static class ProtobufFloatArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, float[]>
            implements ProtobufPrimitivable {

        public static final ProtobufFloatArraySimpledCoder instance = new ProtobufFloatArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, float[] values) {
            out.writeFloats(values);
        }

        @Override
        public float[] convertFrom(ProtobufReader in) {
            return in.readFloats();
        }
    }

    public static class ProtobufLongArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, long[]>
            implements ProtobufPrimitivable {

        public static final ProtobufLongArraySimpledCoder instance = new ProtobufLongArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, long[] values) {
            out.writeLongs(values);
        }

        @Override
        public long[] convertFrom(ProtobufReader in) {
            return in.readLongs();
        }
    }

    public static class ProtobufDoubleArraySimpledCoder extends SimpledCoder<ProtobufReader, ProtobufWriter, double[]>
            implements ProtobufPrimitivable {

        public static final ProtobufDoubleArraySimpledCoder instance = new ProtobufDoubleArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, double[] values) {
            out.writeDoubles(values);
        }

        @Override
        public double[] convertFrom(ProtobufReader in) {
            return in.readDoubles();
        }
    }

    public static class ProtobufBoolArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Boolean[]>
            implements ProtobufPrimitivable {

        public static final ProtobufBoolArraySimpledCoder2 instance = new ProtobufBoolArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Boolean[] values) {
            out.writeBools(values);
        }

        @Override
        public Boolean[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readBools());
        }
    }

    public static class ProtobufByteArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Byte[]>
            implements ProtobufPrimitivable {

        public static final ProtobufByteArraySimpledCoder2 instance = new ProtobufByteArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Byte[] values) {
            out.writeBytes(values);
        }

        @Override
        public Byte[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readBytes());
        }
    }

    public static class ProtobufCharArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Character[]>
            implements ProtobufPrimitivable {

        public static final ProtobufCharArraySimpledCoder2 instance = new ProtobufCharArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Character[] values) {
            out.writeChars(values);
        }

        @Override
        public Character[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readChars());
        }
    }

    public static class ProtobufShortArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Short[]>
            implements ProtobufPrimitivable {

        public static final ProtobufShortArraySimpledCoder2 instance = new ProtobufShortArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Short[] values) {
            out.writeShorts(values);
        }

        @Override
        public Short[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readShorts());
        }
    }

    public static class ProtobufIntArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Integer[]>
            implements ProtobufPrimitivable {

        public static final ProtobufIntArraySimpledCoder2 instance = new ProtobufIntArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Integer[] values) {
            out.writeInts(values);
        }

        @Override
        public Integer[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readInts());
        }
    }

    public static class ProtobufFloatArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Float[]>
            implements ProtobufPrimitivable {

        public static final ProtobufFloatArraySimpledCoder2 instance = new ProtobufFloatArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Float[] values) {
            out.writeFloats(values);
        }

        @Override
        public Float[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readFloats());
        }
    }

    public static class ProtobufLongArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Long[]>
            implements ProtobufPrimitivable {

        public static final ProtobufLongArraySimpledCoder2 instance = new ProtobufLongArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Long[] values) {
            out.writeLongs(values);
        }

        @Override
        public Long[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readLongs());
        }
    }

    public static class ProtobufDoubleArraySimpledCoder2 extends SimpledCoder<ProtobufReader, ProtobufWriter, Double[]>
            implements ProtobufPrimitivable {

        public static final ProtobufDoubleArraySimpledCoder2 instance = new ProtobufDoubleArraySimpledCoder2();

        @Override
        public void convertTo(ProtobufWriter out, Double[] values) {
            out.writeDoubles(values);
        }

        @Override
        public Double[] convertFrom(ProtobufReader in) {
            return Utility.box(in.readDoubles());
        }
    }

    public static class ProtobufAtomicIntegerArraySimpledCoder
            extends SimpledCoder<ProtobufReader, ProtobufWriter, AtomicInteger[]> implements ProtobufPrimitivable {

        public static final ProtobufAtomicIntegerArraySimpledCoder instance =
                new ProtobufAtomicIntegerArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, AtomicInteger[] values) {
            out.writeAtomicIntegers(values);
        }

        @Override
        public AtomicInteger[] convertFrom(ProtobufReader in) {
            return in.readAtomicIntegers();
        }
    }

    public static class ProtobufAtomicLongArraySimpledCoder
            extends SimpledCoder<ProtobufReader, ProtobufWriter, AtomicLong[]> implements ProtobufPrimitivable {

        public static final ProtobufAtomicLongArraySimpledCoder instance = new ProtobufAtomicLongArraySimpledCoder();

        @Override
        public void convertTo(ProtobufWriter out, AtomicLong[] values) {
            out.writeAtomicLongs(values);
        }

        @Override
        public AtomicLong[] convertFrom(ProtobufReader in) {
            return in.readAtomicLongs();
        }
    }
}
