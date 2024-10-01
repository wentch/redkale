/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.convert.pb;

import java.lang.reflect.Type;
import org.redkale.annotation.ClassDepends;
import org.redkale.convert.*;
import org.redkale.util.Attribute;
import org.redkale.util.Utility;

/**
 * @author zhangjx
 * @param <T> T
 */
public class ProtobufObjectEncoder<T> extends ObjectEncoder<ProtobufWriter, T>
        implements ProtobufEncodeable<ProtobufWriter, T> {

    protected boolean memberSizeRequired;

    protected ProtobufObjectEncoder(Type type) {
        super(type);
    }

    @Override
    public final void convertTo(ProtobufWriter out, T value) {
        convertTo(out, null, value);
    }

    @Override
    public void convertTo(ProtobufWriter out, EnMember parentMember, T value) {
        this.checkInited();
        if (value == null) {
            out.writeObjectNull(null);
            return;
        }
        if (parentMember != null) {
            out.writeField(parentMember);
        }
        ProtobufWriter objout = objectWriter(out, parentMember, value);
        objout.writeObjectB(value);
        int maxPosition = 0;
        for (EnMember member : members) {
            maxPosition = member.getPosition();
            objout.writeObjectField(member, value);
        }
        if (objout.objExtFunc() != null) {
            ConvertField[] extFields = objout.objExtFunc().apply(value);
            if (extFields != null) {
                Encodeable<ProtobufWriter, ?> anyEncoder = factory.getAnyEncoder();
                for (ConvertField en : extFields) {
                    if (en != null) {
                        maxPosition++;
                        objout.writeObjectField(
                                en.getName(),
                                en.getType(),
                                Math.max(en.getPosition(), maxPosition),
                                anyEncoder,
                                en.getValue());
                    }
                }
            }
        }
        objout.writeObjectE(value);
        offerWriter(out, objout);
    }

    @ClassDepends
    protected ProtobufWriter objectWriter(ProtobufWriter out, EnMember parentMember, T value) {
        //        if (parentMember != null) {
        //            out.writeLength(computeSize(out, parentMember.getTagSize(), value));
        //        }
        if (out.length() > out.initOffset) {
            return out.pollChild().configParentFunc(out);
        }
        return out;
    }

    @ClassDepends
    protected void offerWriter(ProtobufWriter parent, ProtobufWriter out) {
        if (parent != out) {
            parent.offerChild(out);
        }
    }

    @Override
    protected void initForEachEnMember(ConvertFactory factory, EnMember member) {
        if (member.getIndex() < 1) {
            throw new ConvertException(Utility.orElse(member.getField(), member.getMethod()) + " not found @"
                    + ConvertColumn.class.getSimpleName() + ".index");
        }
        Attribute attr = member.getAttribute();
        boolean enumtostring = ((ProtobufFactory) factory).enumtostring;
        this.memberSizeRequired |= ((ProtobufEncodeable) member.getEncoder()).requireSize();
        setTag(member, ProtobufFactory.getTag(attr.field(), attr.genericType(), member.getPosition(), enumtostring));
        setTagSize(member, ProtobufFactory.computeSInt32SizeNoTag(member.getTag()));
    }

    @Override
    public int computeSize(ProtobufWriter out, int tagSize, T value) {
        int size = 0;
        for (EnMember member : members) {
            ProtobufEncodeable encodeable = (ProtobufEncodeable) member.getEncoder();
            int itemTagSize = member.getTagSize();
            int itemDataSize = encodeable.computeSize(out, itemTagSize, member.getFieldValue(value));
            if (itemDataSize > 0) {
                size += itemTagSize + itemDataSize;
                System.out.println(
                        "member: " + member.getFieldName() + ", tag: " + itemTagSize + ", data: " + itemDataSize);
            }
        }
        System.out.println("对象: " + value + ", 大小: " + size);
        return size;
    }

    public boolean requiredMemberSize() {
        return memberSizeRequired;
    }

    @Override
    public final boolean requireSize() {
        return true;
    }
}
