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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * My implementation of the Future
 */
class FutureImpl<ReturnType> implements Future<ReturnType> {
    private final AtomicBoolean done = new AtomicBoolean();
    private volatile boolean successful;
    private volatile boolean cancelled;
    private volatile Throwable cause;
    private volatile ReturnType returnValue;
    private List<Callback<Future<ReturnType>>> listeners = new ArrayList<>();
    private Lock lock = new ReentrantLock(true);
    private Condition waitDone = lock.newCondition();

    @Override
    public boolean isDone() {
        lock.lock();
        boolean done = this.done.get();
        lock.unlock();
        return done;
    }

    @Override
    public boolean isSuccessful() {
        lock.lock();
        boolean done = this.done.get();
        boolean successful = this.successful;
        lock.unlock();
        return done && successful;
    }

    @Override
    public boolean isCancelled() {
        lock.lock();
        boolean done = this.done.get();
        boolean cancelled = this.cancelled;
        lock.unlock();
        return done && cancelled;
    }

    @Override
    public void cancel(boolean mayInterrupt) {
        lock.lock();
        if(!cancelled && !done.get()) {
            //TODO
            done.set(cancelled = true);
        }
        lock.unlock();
    }

    @Override
    public Future<ReturnType> whenDone(Callback<Future<ReturnType>> cbk) {
        lock.lock();
        if(!done.get()) {
            listeners.add(cbk);
            lock.unlock();
        } else {
            lock.unlock();
            try {
                cbk.call(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    @Override
    public ReturnType getValueNow() {
        lock.lock();
        ReturnType t = returnValue;
        lock.unlock();
        return t;
    }

    @Override
    public Throwable cause() {
        lock.lock();
        Throwable t = cause;
        lock.unlock();
        return t;
    }

    @Override
    public ReturnType getValue(long millis) throws InterruptedException, ExecutionException, TimeoutException {
        if(!done.get()) {
            lock.lock();
            waitDone.await(millis, TimeUnit.MILLISECONDS);
            lock.unlock();
        }
        return returnValue;
    }

    @Override
    public ReturnType getValueUninterrumptibly(long millis) throws ExecutionException, TimeoutException {
        ReturnType ret = null;
        if(!done.get()) {
            long currentMillis = System.currentTimeMillis();
            while (!isDone()) {
                try {
                    ret = getValue(millis);
                } catch (InterruptedException ignore) {
                    millis -= System.currentTimeMillis() - currentMillis;
                }
            }
        } else {
            ret = returnValue;
        }
        return ret;
    }

    @Override
    public ReturnType getValue() throws ExecutionException, InterruptedException {
        if(!done.get()) {
            lock.lock();
            waitDone.await();
            lock.unlock();
        }
        return returnValue;
    }

    @Override
    public ReturnType getValueUninterrumptibly() throws ExecutionException, InterruptedException {
        if(!done.get()) {
            lock.lock();
            waitDone.awaitUninterruptibly();
            lock.unlock();
        }
        return returnValue;
    }

    @Override
    public Future<ReturnType> sync() {
        if(!done.get()) {
            lock.lock();
            waitDone.awaitUninterruptibly();
            lock.unlock();
        }
        return this;
    }

    void postSuccess(ReturnType result) throws Exception {
        lock.lock();
        returnValue = result;
        done.set(successful = true);
        lock.unlock();
        executeListeners();
        lock.lock();
        waitDone.signalAll();
        lock.unlock();
    }

    void postError(Throwable cause) throws Exception {
        lock.lock();
        this.cause = cause;
        done.set(true);
        lock.unlock();
        executeListeners();
        lock.lock();
        waitDone.signalAll();
        lock.unlock();
    }

    void postSuccessSafe(ReturnType result) {
        try {
            postSuccess(result);
        } catch (Exception e) {
            System.out.println("Caught an exception on Future whenDone() callback");
            e.printStackTrace();
        }
    }

    void postErrorSafe(Throwable cause) {
        try {
            postError(cause);
        } catch (Exception e) {
            System.out.println("Caught an exception on Future whenDone() callback");
            e.printStackTrace();
        }
    }

    private void executeListeners() throws Exception {
        for(Callback<Future<ReturnType>> cbk : listeners) {
            cbk.call(this);
        }
    }
}
