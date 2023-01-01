/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.net.client;

import java.util.Queue;
import java.util.concurrent.*;
import org.redkale.net.*;

/**
 *
 * @author zhangjx
 * @param <T> 泛型
 */
public class ClientFuture<T> extends CompletableFuture<T> implements Runnable {

    public static final ClientFuture EMPTY = new ClientFuture(null) {
        @Override
        public boolean complete(Object value) {
            return true;
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            return true;
        }
    };

    protected final ClientRequest request;

    ScheduledFuture timeout;

    int mergeCount; //合并的个数，不算自身

    ClientConnection conn;

    public ClientFuture(ClientRequest request) {
        super();
        this.request = request;
    }

    public int getMergeCount() {
        return mergeCount;
    }

    @Override //JDK9+
    public <U> ClientFuture<U> newIncompleteFuture() {
        ClientFuture future = new ClientFuture<>(request);
        future.timeout = timeout;
        future.mergeCount = mergeCount;
        future.conn = conn;
        return future;
    }

    public <R extends ClientRequest> R getRequest() {
        return (R) request;
    }

    @Override
    public void run() {
        if (conn == null) {
            return;
        }
        AsyncConnection channel = conn.getChannel();
        if (channel.inCurrThread()) {
            this.runTimeout();
        } else {
            channel.execute(this::runTimeout);
        }
    }

    private void runTimeout() {
        Queue<ClientFuture> responseQueue = conn.responseQueue2;
        if (responseQueue != null) {
            responseQueue.remove(this);
        }
        if (request.getRequestid() != null) {
            conn.responseMap.remove(request.getRequestid());
        }

        TimeoutException ex = new TimeoutException();
        WorkThread workThread = null;
        if (request != null) {
            workThread = request.workThread;
            request.workThread = null;
        }
//        if (workThread == null || workThread == Thread.currentThread() || workThread.inIO()
//            || workThread.getState() != Thread.State.RUNNABLE) {
//            this.completeExceptionally(ex);
//        } else {
//            workThread.execute(() -> completeExceptionally(ex));
//        }
        if (workThread == null || workThread.getWorkExecutor() == null) {
            workThread = conn.getChannel().getAsyncIOThread();
        }
        workThread.runWork(() -> completeExceptionally(ex));
    }
}
