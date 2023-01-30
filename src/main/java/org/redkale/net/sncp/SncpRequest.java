/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.net.sncp;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.*;
import org.redkale.convert.bson.BsonConvert;
import org.redkale.net.Request;
import static org.redkale.net.sncp.Sncp.HEADER_SIZE;
import org.redkale.util.Uint128;

/**
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 */
public class SncpRequest extends Request<SncpContext> {

    public static final byte[] DEFAULT_HEADER = new byte[HEADER_SIZE];

    protected static final int READ_STATE_ROUTE = 1;

    protected static final int READ_STATE_HEADER = 2;

    protected static final int READ_STATE_BODY = 3;

    protected static final int READ_STATE_END = 4;

    protected final BsonConvert convert;

    private long seqid;

    protected int readState = READ_STATE_ROUTE;

    private int serviceVersion;

    private Uint128 serviceid;

    private Uint128 actionid;

    private int bodyLength;

    private int bodyOffset;

    private boolean ping;

    private byte[] body;

    private final byte[] addrBytes = new byte[6];

    protected SncpRequest(SncpContext context) {
        super(context);
        this.convert = context.getBsonConvert();
    }

    @Override  //request.header与response.header数据格式保持一致
    protected int readHeader(ByteBuffer buffer, Request last) {
        //---------------------head----------------------------------
        if (this.readState == READ_STATE_ROUTE) {
            if (buffer.remaining() < HEADER_SIZE) {
                return HEADER_SIZE - buffer.remaining(); //小于60
            }
            this.seqid = buffer.getLong(); //8
            if (buffer.getChar() != HEADER_SIZE) { //2
                if (context.getLogger().isLoggable(Level.FINEST)) {
                    context.getLogger().finest("sncp buffer header.length not " + HEADER_SIZE);
                }
                return -1;
            }
            this.serviceid = Uint128.read(buffer); //16
            this.serviceVersion = buffer.getInt(); //4
            this.actionid = Uint128.read(buffer); //16
            buffer.get(addrBytes); //ipaddr   //6
            this.bodyLength = buffer.getInt(); //4

            if (buffer.getInt() != 0) { //4  retcode
                if (context.getLogger().isLoggable(Level.FINEST)) {
                    context.getLogger().finest("sncp buffer header.retcode not 0");
                }
                return -1;
            }
            this.body = new byte[this.bodyLength];
            this.readState = READ_STATE_BODY;
        }
        //---------------------body----------------------------------
        if (this.readState == READ_STATE_BODY) {
            if (this.bodyLength == 0) {
                this.readState = READ_STATE_END;
                if (this.seqid == 0 && this.serviceid == Uint128.ZERO && this.actionid == Uint128.ZERO) {
                    this.ping = true;
                }
                return 0;
            }
            int len = Math.min(this.bodyLength, buffer.remaining());
            buffer.get(body, 0, len);
            this.bodyOffset = len;
            int rs = bodyLength - len;
            if (rs == 0) {
                this.readState = READ_STATE_END;
            }
            return rs;
        }
        return 0;
    }

    @Override
    protected Serializable getRequestid() {
        return seqid;
    }

    @Override
    protected void prepare() {
        this.keepAlive = true;
    }

    //被SncpAsyncHandler.sncp_setParams调用
    protected void sncp_setParams(SncpDynServlet.SncpServletAction action, Logger logger, Object... params) {
    }

    @Override
    public String toString() {
        return SncpRequest.class.getSimpleName() + "{seqid=" + this.seqid
            + ",serviceVersion=" + this.serviceVersion + ",serviceid=" + this.serviceid
            + ",actionid=" + this.actionid + ",bodyLength=" + this.bodyLength
            + ",bodyOffset=" + this.bodyOffset + ",remoteAddress=" + getRemoteAddress() + "}";
    }

    @Override
    protected void recycle() {
        this.seqid = 0;
        this.readState = READ_STATE_ROUTE;
        this.serviceid = null;
        this.serviceVersion = 0;
        this.actionid = null;
        this.bodyLength = 0;
        this.bodyOffset = 0;
        this.body = null;
        this.ping = false;
        this.addrBytes[0] = 0;
        super.recycle();
    }

    protected boolean isPing() {
        return ping;
    }

    public byte[] getBody() {
        return body;
    }

    public long getSeqid() {
        return seqid;
    }

    public int getServiceVersion() {
        return serviceVersion;
    }

    public Uint128 getServiceid() {
        return serviceid;
    }

    public Uint128 getActionid() {
        return actionid;
    }

    public InetSocketAddress getRemoteAddress() {
        if (addrBytes[0] == 0) {
            return null;
        }
        return new InetSocketAddress((0xff & addrBytes[0]) + "." + (0xff & addrBytes[1]) + "." + (0xff & addrBytes[2]) + "." + (0xff & addrBytes[3]),
            ((0xff00 & (addrBytes[4] << 8)) | (0xff & addrBytes[5])));
    }

}
