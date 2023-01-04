/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.net.client;

import java.io.Serializable;
import java.util.function.BiConsumer;
import org.redkale.net.WorkThread;
import org.redkale.util.*;

/**
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 *
 * @since 2.3.0
 */
public abstract class ClientRequest implements BiConsumer<ClientConnection, ByteArray> {

    protected long createTime = System.currentTimeMillis();

    protected WorkThread workThread;

    protected String traceid;

    public Serializable getRequestid() {
        return null;
    }

    //关闭请求一定要返回false
    public boolean isCloseType() {
        return false;
    }

    public long getCreateTime() {
        return createTime;
    }

    public String getTraceid() {
        return traceid;
    }

    public <T extends ClientRequest> T currThread(WorkThread thread) {
        this.workThread = thread;
        return (T) this;
    }

    //是否能合并， requestid=null的情况下值才有效
    protected boolean canMerge(ClientConnection conn) {
        return false;
    }

    //合并成功了返回true
    protected boolean merge(ClientConnection conn, ClientRequest other) {
        return false;
    }

    //数据是否全部写入，如果只写部分，返回false
    protected boolean isCompleted() {
        return true;
    }

    protected void prepare() {
        this.createTime = System.currentTimeMillis();
        this.traceid = Traces.currTraceid();
    }

    protected boolean recycle() {
        this.createTime = 0;
        this.traceid = null;
        return true;
    }
}
