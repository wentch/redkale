/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.convert.ext;

import org.redkale.convert.*;

/**
 * String[] 的SimpledCoder实现
 *
 * <p>详情见: https://redkale.org
 *
 * @author zhangjx
 * @param <R> Reader输入的子类型
 * @param <W> Writer输出的子类型
 */
public final class StringArraySimpledCoder<R extends Reader, W extends Writer> extends SimpledCoder<R, W, String[]> {

    public static final StringArraySimpledCoder instance = new StringArraySimpledCoder();

    @Override
    public void convertTo(W out, String[] values) {
        if (values == null) {
            out.writeNull();
            return;
        }
        out.writeArrayB(values.length, StringSimpledCoder.instance, values);
        boolean flag = false;
        for (String v : values) {
            if (flag) {
                out.writeArrayMark();
            }
            out.writeString(v);
            flag = true;
        }
        out.writeArrayE();
    }

    @Override
    public String[] convertFrom(R in) {
        if (!in.readArrayB(StringSimpledCoder.instance)) {
            return null;
        }
        int size = 0;
        String[] data = new String[8];
        while (in.hasNext()) {
            if (size >= data.length) {
                String[] newdata = new String[data.length + 4];
                System.arraycopy(data, 0, newdata, 0, size);
                data = newdata;
            }
            data[size++] = in.readString();
        }
        in.readArrayE();
        String[] newdata = new String[size];
        System.arraycopy(data, 0, newdata, 0, size);
        return newdata;
    }
}
