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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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
public class FutureImpl<ReturnType> implements Future<ReturnType> {
    private final AtomicBoolean done = new AtomicBoolean();
    private volatile boolean successful;
    private volatile boolean cancelled;
    private volatile Throwable cause;
    private volatile ReturnType returnValue;
    private List<Callback<Future<ReturnType>>> listeners = new ArrayList<>();
    private Lock lock = new ReentrantLock(true);
    private Condition waitDone = lock.newCondition();
    private final IOService service;
    private final Procedure whenCancelled;
    private Future<?> timeoutFuture;

    public FutureImpl(IOService service, Procedure whenCancelled) {
        this.service = service;
        this.whenCancelled = whenCancelled;
    }

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
    public boolean isCancelable() {
        return whenCancelled != null;
    }

    @Override
    public void cancel(boolean mayInterrupt) {
        if(!isCancelable()) throw new IllegalStateException("This task cannot be cancelled");
        lock.lock();
        if(!cancelled && !done.get()) {
            whenCancelled.call();
            cancelled = true;
            postError(new CancellationException("Task was cancelled"));
        }
        lock.unlock();
    }

    @NotNull
    @Override
    public Future<ReturnType> whenDone(@NotNull Callback<Future<ReturnType>> cbk) {
        lock.lock();
        if(!done.get()) {
            listeners.add(cbk);
            lock.unlock();
        } else {
            lock.unlock();
            cbk.call(this);
        }
        return this;
    }

    @NotNull
    @Override
    public Future<ReturnType> setTimeout(long milliseconds) {
        if(milliseconds <= 0) throw new IllegalArgumentException("Only positive non 0 values are accepted");
        if(isDone()) throw new IllegalStateException("The task is done");
        if(!isCancelable()) throw new IllegalStateException("This task cannot be cancelled");
        if(timeoutFuture != null) timeoutFuture.cancel(false);
        timeoutFuture = service.schedule(new Procedure() {
            @Override
            public void call() {
                if(!isDone()) {
                    whenCancelled.call();
                    cancel(true);
                }
            }
        }, milliseconds);
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
            if(!waitDone.await(millis, TimeUnit.MILLISECONDS)) {
                lock.unlock();
                throw new TimeoutException("Has passed " + millis + "ms and no result got");
            }
            lock.unlock();
        }
        if(!isSuccessful()) throw new ExecutionException(cause);
        return returnValue;
    }

    @Override
    public ReturnType getValueUninterrumptibly(long millis) throws ExecutionException, TimeoutException {
        if(!done.get()) {
            long currentMillis = System.currentTimeMillis();
            while(!isDone() && millis > 0) {
                try {
                    getValue(millis);
                } catch (InterruptedException ignore) {
                    millis -= System.currentTimeMillis() - currentMillis;
                }
            }
        }
        if(!isSuccessful()) throw new ExecutionException(cause);
        return returnValue;
    }

    @Override
    public ReturnType getValue() throws ExecutionException, InterruptedException {
        if(!done.get()) {
            lock.lock();
            waitDone.await();
            lock.unlock();
        }
        if(!isSuccessful()) throw new ExecutionException(cause);
        return returnValue;
    }

    @Override
    public ReturnType getValueUninterrumptibly() throws ExecutionException, InterruptedException {
        if(!done.get()) {
            lock.lock();
            waitDone.awaitUninterruptibly();
            lock.unlock();
        }
        if(!isSuccessful()) throw new ExecutionException(cause);
        return returnValue;
    }

    @NotNull
    @Override
    public Future<ReturnType> sync() {
        if(!done.get()) {
            lock.lock();
            waitDone.awaitUninterruptibly();
            lock.unlock();
            if(!isSuccessful()) doThrow(cause);
        }
        return this;
    }

    public void postSuccess(@Nullable ReturnType result) {
        if(isDone()) throw new IllegalStateException("Task is already done");
        lock.lock();
        if(timeoutFuture != null) timeoutFuture.cancel(false);
        returnValue = result;
        done.set(successful = true);
        lock.unlock();
        executeListeners();
        lock.lock();
        waitDone.signalAll();
        lock.unlock();
    }

    public void postError(@NotNull Throwable cause) {
        if(isDone()) throw new IllegalStateException("Task is already done");
        lock.lock();
        if(timeoutFuture != null) timeoutFuture.cancel(false);
        this.cause = cause;
        done.set(true);
        lock.unlock();
        executeListeners();
        lock.lock();
        waitDone.signalAll();
        lock.unlock();
    }

    private void executeListeners() {
        for(Callback<Future<ReturnType>> cbk : listeners) {
            try {
                cbk.call(this);
            } catch(Throwable t) {
                System.err.println("Caught a Throwable inside a whenDone() Callback");
                t.printStackTrace();
            }
        }
    }

    private void doThrow(Throwable e) {
        // http://stackoverflow.com/questions/6302015/throw-checked-exceptions
        FutureImpl.<RuntimeException> doThrow0(e);
    }

    @SuppressWarnings("unchecked") private static <E extends Throwable> void doThrow0(Throwable e) throws E {
        throw (E) e;
    }
}
