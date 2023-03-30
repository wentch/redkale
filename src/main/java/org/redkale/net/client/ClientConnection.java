/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.net.client;

import java.io.Serializable;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.redkale.annotation.Nullable;
import org.redkale.net.*;
import org.redkale.util.ByteArray;

/**
 * 注意: 要确保AsyncConnection的读写过程都必须在channel.ioThread中运行
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 * @since 2.3.0
 *
 * @param <R> 请求对象
 * @param <P> 响应对象
 */
public abstract class ClientConnection<R extends ClientRequest, P> implements Consumer<AsyncConnection> {

    //=-2 表示连接放在ThreadLocal存储
    //=-1 表示连接放在connAddrEntrys存储
    //>=0 表示connArray的下坐标，从0开始
    protected final int index;

    protected final Client client;

    @Nullable
    protected final LongAdder respWaitingCounter; //可能为null

    protected final LongAdder doneRequestCounter = new LongAdder();

    protected final LongAdder doneResponseCounter = new LongAdder();

    protected final ByteArray writeArray = new ByteArray();

    final AtomicBoolean pauseWriting = new AtomicBoolean();

    final ConcurrentLinkedQueue<ClientFuture> pauseRequests = new ConcurrentLinkedQueue<>();

    ClientFuture currHalfWriteFuture; //pauseWriting=true，此字段才会有值; pauseWriting=false，此字段值为null

    @Nullable
    private final Client.AddressConnEntry connEntry;

    protected final AsyncConnection channel;

    private final ClientCodec<R, P> codec;

    //respFutureQueue、respFutureMap二选一， SPSC队列模式
    private final Deque<ClientFuture<R, P>> respFutureQueue = new ConcurrentLinkedDeque<>(); //Utility.unsafe() != null ? new MpscGrowableArrayQueue<>(16, 1 << 16) : new ConcurrentLinkedQueue<>();

    //respFutureQueue、respFutureMap二选一, key: requestid， SPSC模式
    private final Map<Serializable, ClientFuture<R, P>> respFutureMap = new ConcurrentHashMap<>();

    Iterator<ClientFuture<R, P>> currRespIterator; //必须在调用decodeMessages之前重置为null

    private int maxPipelines; //最大并行处理数

    private boolean authenticated;

    @SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor"})
    public ClientConnection(Client<? extends ClientConnection<R, P>, R, P> client, int index, AsyncConnection channel) {
        this.client = client;
        this.codec = createCodec();
        this.index = index;
        this.connEntry = index == -2 ? null : (index >= 0 ? null : client.connAddrEntrys.get(channel.getRemoteAddress()));
        this.respWaitingCounter = index == -2 ? new LongAdder() : (index >= 0 ? client.connRespWaitings[index] : this.connEntry.connRespWaiting);
        this.channel = channel.beforeCloseListener(this);
    }

    protected abstract ClientCodec createCodec();

    protected final CompletableFuture<P> writeChannel(R request) {
        return writeChannel(request, null);
    }

    //respTransfer只会在ClientCodec的读线程里调用
    protected final <T> CompletableFuture<T> writeChannel(R request, Function<P, T> respTransfer) {
        request.respTransfer = respTransfer;
        ClientFuture respFuture = createClientFuture(request);
        int rts = this.channel.getReadTimeoutSeconds();
        if (rts > 0 && !request.isCloseType()) {
            respFuture.setTimeout(client.timeoutScheduler.schedule(respFuture, rts, TimeUnit.SECONDS));
        }
        respWaitingCounter.increment(); //放在writeChannelInWriteThread计数会延迟，导致不准确
        if (client.isThreadLocalConnMode()) {
            offerRespFuture(respFuture);
            writeArray.clear();
            request.writeTo(this, writeArray);
            doneRequestCounter.increment();
            if (writeArray.length() > 0) {
                channel.write(writeArray, this, writeHandler);
            }
        } else {
            if (channel.inCurrWriteThread()) {
                writeChannelInThread(request, respFuture);
            } else {
                channel.executeWrite(() -> writeChannelInThread(request, respFuture));
            }
        }
        return respFuture;
    }

    private void writeChannelInThread(R request, ClientFuture respFuture) {
        offerRespFuture(respFuture);
        if (pauseWriting.get()) {
            pauseRequests.add(respFuture);
        } else {
            sendRequestInThread(request, respFuture);
        }
    }

    private void sendRequestInThread(R request, ClientFuture respFuture) {
        //发送请求数据包
        writeArray.clear();
        request.writeTo(this, writeArray);
        if (request.isCompleted()) {
            doneRequestCounter.increment();
        } else { //还剩半包没发送完
            pauseWriting.set(true);
            currHalfWriteFuture = respFuture;
        }
        if (writeArray.length() > 0) {
            channel.write(writeArray, this, writeHandler);
        }
    }

    //发送半包和积压的请求数据包
    private void sendHalfWriteInThread(R request, Throwable halfRequestExc) {
        pauseWriting.set(false);
        ClientFuture respFuture = this.currHalfWriteFuture;
        if (respFuture != null) {
            this.currHalfWriteFuture = null;
            if (halfRequestExc == null) {
                offerFirstRespFuture(respFuture);
                sendRequestInThread(request, respFuture);
            } else {
                codec.responseComplete(true, respFuture, null, halfRequestExc);
            }
        }
        while (!pauseWriting.get() && (respFuture = pauseRequests.poll()) != null) {
            sendRequestInThread((R) respFuture.getRequest(), respFuture);
        }
    }

    void sendHalfWrite(R request, Throwable halfRequestExc) {
        if (channel.inCurrWriteThread()) {
            sendHalfWriteInThread(request, halfRequestExc);
        } else {
            channel.executeWrite(() -> sendHalfWriteInThread(request, halfRequestExc));
        }
    }

    CompletableFuture<P> writeVirtualRequest(R request) {
        if (!request.isVirtualType()) {
            return CompletableFuture.failedFuture(new RuntimeException("ClientVirtualRequest must be virtualType = true"));
        }
        ClientFuture<R, P> respFuture = createClientFuture(request);
        offerRespFuture(respFuture);
        return respFuture;
    }

    protected void preComplete(P resp, R req, Throwable exc) {
    }

    protected ClientFuture<R, P> createClientFuture(R request) {
        return new ClientFuture(this, request);
    }

    @Override //AsyncConnection.beforeCloseListener
    public void accept(AsyncConnection t) {
        respWaitingCounter.reset();
        if (index >= 0) {
            client.connOpenStates[index].set(false);
            client.connArray[index] = null; //必须connOpenStates之后
        } else if (connEntry != null) { //index=-1
            connEntry.connOpenState.set(false);
        } else {//index=-2
            client.localConnList.remove(this);
        }
    }

    public void dispose(Throwable exc) {
        channel.dispose();
        Throwable e = exc == null ? new ClosedChannelException() : exc;
        CompletableFuture f;
        respWaitingCounter.reset();
        WorkThread thread = channel.getReadIOThread();
        if (!respFutureQueue.isEmpty()) {
            while ((f = respFutureQueue.poll()) != null) {
                CompletableFuture future = f;
                thread.runWork(() -> future.completeExceptionally(e));
            }
        }
        if (!respFutureMap.isEmpty()) {
            respFutureMap.forEach((key, future) -> {
                respFutureMap.remove(key);
                thread.runWork(() -> future.completeExceptionally(e));
            });
        }
    }

    //只会在WriteIOThread中调用
    void offerFirstRespFuture(ClientFuture<R, P> respFuture) {
        Serializable requestid = respFuture.request.getRequestid();
        if (requestid == null) {
            respFutureQueue.offerFirst(respFuture);
        } else {
            respFutureMap.put(requestid, respFuture);
        }
    }

    //只会在WriteIOThread中调用
    void offerRespFuture(ClientFuture<R, P> respFuture) {
        Serializable requestid = respFuture.request.getRequestid();
        if (requestid == null) {
            respFutureQueue.offer(respFuture);
        } else {
            respFutureMap.put(requestid, respFuture);
        }
    }

    //只会被Timeout在ReadIOThread中调用
    void removeRespFuture(Serializable requestid, ClientFuture<R, P> respFuture) {
        if (requestid == null) {
            respFutureQueue.remove(respFuture);
        } else {
            respFutureMap.remove(requestid);
        }
    }

    //只会被ClientCodec在ReadIOThread中调用
    ClientFuture<R, P> pollRespFuture(Serializable requestid) {
        if (requestid == null) {
            return respFutureQueue.poll();
        } else {
            return respFutureMap.remove(requestid);
        }
    }

    //只会被ClientCodec在ReadIOThread中调用
    R findRequest(Serializable requestid) {
        if (requestid == null) {
            if (currRespIterator == null) {
                currRespIterator = respFutureQueue.iterator();
            }
            ClientFuture<R, P> future = currRespIterator.hasNext() ? currRespIterator.next() : null;
            return future == null ? null : future.request;
        } else {
            ClientFuture<R, P> future = respFutureMap.get(requestid);
            return future == null ? null : future.request;
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public AsyncConnection getChannel() {
        return channel;
    }

    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    public long getDoneRequestCounter() {
        return doneRequestCounter.longValue();
    }

    public long getDoneResponseCounter() {
        return doneResponseCounter.longValue();
    }

    public <C extends ClientCodec<R, P>> C getCodec() {
        return (C) codec;
    }

    public int getMaxPipelines() {
        return maxPipelines;
    }

    protected ClientConnection setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        return this;
    }

    protected ClientConnection setMaxPipelines(int maxPipelines) {
        this.maxPipelines = maxPipelines;
        return this;
    }

    protected ClientConnection resetMaxPipelines() {
        this.maxPipelines = client.maxPipelines;
        return this;
    }

    public int runningCount() {
        return respWaitingCounter.intValue();
    }

    public long getLastWriteTime() {
        return channel.getLastWriteTime();
    }

    public long getLastReadTime() {
        return channel.getLastReadTime();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public String toString() {
        String s = super.toString();
        int pos = s.lastIndexOf('@');
        if (pos < 1) {
            return s;
        }
        int cha = pos + 10 - s.length();
        if (cha < 1) {
            return s;
        }
        for (int i = 0; i < cha; i++) s += ' ';
        return s;
    }

    protected final CompletionHandler<Integer, ClientConnection> writeHandler = new CompletionHandler<Integer, ClientConnection>() {

        @Override
        public void completed(Integer result, ClientConnection attachment) {
        }

        @Override
        public void failed(Throwable exc, ClientConnection attachment) {
            attachment.dispose(exc);
        }
    };
}
