/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.convert.pb;

import java.lang.reflect.Type;
import java.util.stream.Stream;
import org.redkale.annotation.Nonnull;
import org.redkale.convert.*;

/**
 * @author zhangjx
 * @param <T> T
 */
public class ProtobufStreamEncoder<T> extends StreamEncoder<ProtobufWriter, T>
        implements ProtobufEncodeable<ProtobufWriter, Stream<T>> {

    protected final boolean componentPrimitived;

    protected final boolean componentSimpled;

    public ProtobufStreamEncoder(ConvertFactory factory, Type type) {
        super(factory, type);
        this.componentPrimitived = getComponentEncoder() instanceof ProtobufPrimitivable;
        this.componentSimpled = getComponentEncoder() instanceof SimpledCoder;
    }

    @Override
    public void convertTo(final ProtobufWriter out, @Nonnull EnMember member, Stream<T> value) {
        this.checkInited();
        Object[] array = out.getStreamArray(value);
        if (array == null || array.length < 1) {
            return;
        }
        if (componentPrimitived) {
            convertPrimitivedTo(out, member, array);
        } else {
            convertObjectTo(out, member, array);
        }
    }

    protected void convertObjectTo(final ProtobufWriter out, @Nonnull EnMember member, Object[] value) {
        ProtobufEncodeable itemEncoder = (ProtobufEncodeable) this.componentEncoder;
        // out.writeArrayB(value.length, itemEncoder, value);
        boolean first = true;
        for (Object item : value) {
            if (!first) {
                out.writeField(member);
            }
            if (item == null) {
                out.writeLength(0);
            } else {
                itemEncoder.convertTo(out, member, item);
            }
            first = false;
        }
        // out.writeArrayE();
    }

    protected void convertPrimitivedTo(final ProtobufWriter out, @Nonnull EnMember member, Object[] value) {
        ProtobufEncodeable itemEncoder = (ProtobufEncodeable) this.componentEncoder;
        int dataSize = 0;
        for (Object item : value) {
            dataSize += itemEncoder.computeSize(out, 0, item);
        }
        out.writeLength(dataSize);
        for (Object item : value) {
            itemEncoder.convertTo(out, item);
        }
    }

    // 对象是否为空
    @Override
    public boolean isEmpty(ProtobufWriter out, Stream<T> value) {
        Object[] array = out.putStreamArray(value);
        return array == null || array.length == 0;
    }

    @Override
    public int computeSize(ProtobufWriter out, int tagSize, Stream<T> value) {
        Object[] array = out.putStreamArray(value);
        if (array == null || array.length < 1) {
            return 0;
        }
        return computeSize(out, tagSize, array);
    }

    protected int computeSize(ProtobufWriter out, int tagSize, Object[] value) {
        ProtobufEncodeable itemEncoder = (ProtobufEncodeable) this.componentEncoder;
        if (componentPrimitived) {
            int dataSize = 0;
            for (Object item : value) {
                dataSize += itemEncoder.computeSize(out, tagSize, item);
            }
            return ProtobufFactory.computeSInt32SizeNoTag(dataSize) + dataSize;
        } else {
            int dataSize = tagSize * (value.length - 1);
            for (Object item : value) {
                dataSize += itemEncoder.computeSize(out, tagSize, item);
            }
            return dataSize;
        }
    }
}
