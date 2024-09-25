/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.convert.pb;

import java.lang.reflect.Type;
import org.redkale.convert.*;

/**
 * @author zhangjx
 * @param <T> T
 */
public class ProtobufStreamEncoder<T> extends StreamEncoder<ProtobufWriter, T> {

    protected final boolean simple;

    public ProtobufStreamEncoder(ConvertFactory factory, Type type) {
        super(factory, type);
        this.simple = ProtobufFactory.isNoLenBytesType(getComponentType());
    }

    @Override
    protected void writeMemberValue(ProtobufWriter out, EnMember member, Object item, boolean first) {
        if (simple) {
            if (item == null) {
                out.writeUInt32(0);
            } else {
                componentEncoder.convertTo(out, item);
            }
            return;
        }
        if (member != null) {
            out.writeFieldName(member);
        }
        if (item instanceof CharSequence) {
            componentEncoder.convertTo(out, item);
        } else {
            ProtobufWriter tmp = out.pollChild();
            componentEncoder.convertTo(tmp, item);
            out.writeUInt32(tmp.count());
            out.writeTo(tmp.toArray());
            out.offerChild(tmp);
        }
    }
}
