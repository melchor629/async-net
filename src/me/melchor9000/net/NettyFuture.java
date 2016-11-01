/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2016  melchor629 (melchor9000@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package me.melchor9000.net;

import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * About the result of an asynchronous task, hidding the Netty implementation
 */
class NettyFuture<ReturnType> implements Future<ReturnType> {
    private final io.netty.util.concurrent.Future<ReturnType> future;

    NettyFuture(io.netty.util.concurrent.Future<ReturnType> future) {
        this.future = future;
    }

    public boolean isDone() {
        return future.isDone();
    }

    public boolean isSuccessful() {
        return future.isSuccess();
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public void cancel(boolean mayInterrupt) {
        future.cancel(mayInterrupt);
    }

    public Future<ReturnType> whenDone(final Callback<Future<ReturnType>> cbk) {
        if(cbk == null) throw new NullPointerException("Callback cannot be null");
        future.addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super ReturnType>>() {
            @Override
            public void operationComplete(io.netty.util.concurrent.Future<? super ReturnType> future) throws Exception {
                try {
                    cbk.call(NettyFuture.this);
                } catch(Exception exception) {
                    throw exception;
                } catch(Throwable throwable) {
                    System.err.println("Caught a Throwable in " + cbk.getClass().getName() + ".call()");
                    throwable.printStackTrace();
                }
            }
        });
        return this;
    }

    public ReturnType getValueNow() {
        return future.getNow();
    }

    public Throwable cause() {
        return future.cause();
    }

    public ReturnType getValue(long millis) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(millis, TimeUnit.MILLISECONDS);
    }

    public ReturnType getValueUninterrumptibly(long millis) throws ExecutionException, TimeoutException {
        ReturnType ret = null;
        long currentMillis = System.currentTimeMillis();
        while(!isDone()) {
            try {
                ret = getValue(millis);
            } catch(InterruptedException ignore) {
                millis -= System.currentTimeMillis() - currentMillis;
            }
        }
        return ret;
    }

    public ReturnType getValue() throws ExecutionException, InterruptedException {
        return future.get();
    }

    public ReturnType getValueUninterrumptibly() throws ExecutionException, InterruptedException {
        ReturnType ret = null;
        while(!isDone()) {
            try {
                ret = getValue();
            } catch(InterruptedException ignore) {}
        }
        return ret;
    }

    public Future<ReturnType> sync() {
        future.syncUninterruptibly();
        return this;
    }
}
