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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Tests for {@link FutureImpl}
 */
public class FutureImplTest {
    private static IOService service;

    @BeforeClass
    public static void setUp() throws Exception {
        service = new IOService();
    }

    @Test
    public void doneStateOnSuccess() {
        FutureImpl<Void> future = createFuture();

        assertTrue("Done must be false", !future.isDone());
        assertTrue("Successful must be false", !future.isSuccessful());
        assertTrue("Cancelled must be false", !future.isCancelled());
        assertNull("Cause must be null", future.cause());

        future.postSuccess(null);

        assertTrue("Done must be true", future.isDone());
        assertTrue("Successful must be true", future.isSuccessful());
        assertTrue("Cancelled must be false", !future.isCancelled());
        assertNull("Cause must be null", future.cause());
    }

    @Test
    public void doneStateOnFailure() {
        FutureImpl<Void> future = createFuture();

        assertTrue("Done must be false", !future.isDone());
        assertTrue("Successful must be false", !future.isSuccessful());
        assertTrue("Cancelled must be false", !future.isCancelled());
        assertNull("Cause must be null", future.cause());

        future.postError(new RuntimeException());

        assertTrue("Done must be true", future.isDone());
        assertTrue("Successful must be false", !future.isSuccessful());
        assertTrue("Cancelled must be false", !future.isCancelled());
        assertNotNull("Cause must be non-null", future.cause());
    }

    @Test
    public void cancellableWhenHasProcedure() {
        assertTrue("Should be cancellable", createFuture(new Procedure() { @Override public void call() { } }).isCancelable());
    }

    @Test
    public void notCancellableWhenHasntProcedure() {
        assertTrue("Shouldn't be cancellable", !createFuture(null).isCancelable());
    }

    @Test
    public void doneStateOnCancel() {
        FutureImpl<Void> future = createFuture(new Procedure() { @Override public void call() { } });

        assertTrue("Done must be false", !future.isDone());
        assertTrue("Successful must be false", !future.isSuccessful());
        assertTrue("Cancelled must be false", !future.isCancelled());
        assertNull("Cause must be null", future.cause());

        future.cancel(true);

        assertTrue("Done must be true", future.isDone());
        assertTrue("Successful must be false", !future.isSuccessful());
        assertTrue("Cancelled must be true", future.isCancelled());
        assertNotNull("Cause must be non-null", future.cause());
    }

    @Test(expected = IllegalStateException.class)
    public void cancelOnUncancellableTask() {
        createFuture().cancel(true);
    }

    @Test
    public void callWhenDone() {
        FutureImpl<Void> future = createFuture();
        final boolean whenDoneCalled[] = new boolean[] { false };

        future.whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                whenDoneCalled[0] = true;
            }
        });

        assertTrue("whenDone() callback must not be called", !whenDoneCalled[0]);
        future.postSuccess(null);
        assertTrue("whenDone() callback must be called", whenDoneCalled[0]);
    }

    @Test
    public void immediateCallOfWhenDone() {
        FutureImpl<Void> future = createFuture();
        final boolean whenDoneCalled[] = new boolean[] { false };

        future.postSuccess(null);
        assertTrue("whenDone() callback must not be called", !whenDoneCalled[0]);
        future.whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                whenDoneCalled[0] = true;
            }
        });

        assertTrue("whenDone() callback must be called", whenDoneCalled[0]);
    }

    @Test
    public void getValueNowWhenNotDone() {
        assertNull("getValueNow() must return null when task is not done", createFuture().getValueNow());
    }

    @Test
    public void getValueNowWhenDoneAndSuccessful() {
        FutureImpl<Boolean> future = createFuture();
        future.postSuccess(true);
        assertNotNull("getValueNow() must return something after a non null postSuccess()", future.getValueNow());
        assertTrue("getValueNow() don't return the value", future.getValueNow());
    }

    @Test
    public void syncWillWaitUntilTaskIsDone() {
        final FutureImpl<Void> future = createFuture();
        assertTrue("The task is not done", !future.isDone());
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postSuccess(null);
            }
        }, 2);
        future.sync();
        assertTrue("The task is done", future.isDone());
    }

    @Test(expected = RuntimeException.class)
    public void syncWillThrowIfTaskFailed() {
        final FutureImpl<Void> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postError(new RuntimeException("fail :("));
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.sync();
    }

    @Test
    public void getValueWillWaitAndReturnValue() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postSuccess(10);
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        Integer value = future.getValue();
        assertTrue("The task is done", future.isDone());
        assertNotNull("The value is null", value);
        assertEquals("The value doesn't match", 10, (int) value);
    }

    @Test(expected = ExecutionException.class)
    public void getValueWillThrowIfTaskFailed() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postError(new RuntimeException("fail, again :("));
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValue();
    }

    @Test(expected = InterruptedException.class)
    public void getValueWillThrowIfThreadIsInterrupted() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        final Thread thisThread = Thread.currentThread();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                thisThread.interrupt();
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValue();
    }

    @Test
    public void getValueUninterrumptiblyWillWaitAndReturnValue() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postSuccess(10);
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        Integer value = future.getValueUninterrumptibly();
        assertTrue("The task is done", future.isDone());
        assertNotNull("The value is null", value);
        assertEquals("The value doesn't match", 10, (int) value);
    }

    @Test(expected = ExecutionException.class)
    public void getValueUninterrumptiblyWillThrowIfTaskFailed() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postError(new RuntimeException("fail, again :("));
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValueUninterrumptibly();
    }

    @Test
    public void getValueUninterrumptiblyWontThrowIfThreadIsInterrupted() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        final Thread thisThread = Thread.currentThread();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                thisThread.interrupt();
                service.schedule(new Procedure() {
                    @Override
                    public void call() {
                        future.postSuccess(11);
                    }
                }, 1);
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValueUninterrumptibly();
    }

    @Test
    public void getValueTimeOutWillWaitAndReturnValue() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postSuccess(10);
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        Integer value = future.getValue(4);
        assertTrue("The task is done", future.isDone());
        assertNotNull("The value is null", value);
        assertEquals("The value doesn't match", 10, (int) value);
    }

    @Test(expected = ExecutionException.class)
    public void getValueTimeOutWillThrowIfTaskFailed() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postError(new RuntimeException("fail, again :("));
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValue(4);
    }

    @Test(expected = InterruptedException.class)
    public void getValueTimeOutWillThrowIfThreadIsInterrupted() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        final Thread thisThread = Thread.currentThread();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                thisThread.interrupt();
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        future.getValue(4);
    }

    @Test(expected = TimeoutException.class)
    public void getValueTimedOutThrows() throws Exception {
        FutureImpl<Void> future = createFuture();
        future.getValue(1);
    }

    @Test
    public void getValueUninterrumptiblyTimeOutWillWaitAndReturnValue() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postSuccess(10);
            }
        }, 2);
        assertTrue("The task is not done", !future.isDone());
        Integer value = future.getValueUninterrumptibly(4);
        assertTrue("The task is done", future.isDone());
        assertNotNull("The value is null", value);
        assertEquals("The value doesn't match", 10, (int) value);
    }

    @Test(expected = ExecutionException.class)
    public void getValueUninterrumptiblyTimeOutWillThrowIfTaskFailed() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                future.postError(new RuntimeException("fail, again :("));
            }
        }, 1);
        assertTrue("The task is not done", !future.isDone());
        future.getValueUninterrumptibly(2);
    }

    @Test
    public void getValueUninterrumptiblyTimeOutWontThrowIfThreadIsInterrupted() throws Exception {
        final FutureImpl<Integer> future = createFuture();
        final Thread thisThread = Thread.currentThread();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                thisThread.interrupt();
                service.schedule(new Procedure() {
                    @Override
                    public void call() {
                        future.postSuccess(11);
                    }
                }, 1);
            }
        }, 1);
        assertTrue("The task is not done", !future.isDone());
        future.getValueUninterrumptibly(10);
    }

    @Test(expected = TimeoutException.class)
    public void getValueUninterrumptiblyTimedOutThrows() throws Exception {
        FutureImpl<Void> future = createFuture();
        future.getValueUninterrumptibly(1);
    }

    @Test
    public void throwInCallbackDoesNothing() {
        FutureImpl<Void> future = createFuture();
        future.whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                throw new RuntimeException("go!");
            }
        });
        future.postSuccess(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTimeoutThrowIfInvalidTime() {
        createFuture(new Procedure() { @Override public void call() { } }).setTimeout(0);
    }

    @Test(expected = IllegalStateException.class)
    public void setTimeoutThrowIfIsNotCancelable() {
        createFuture().setTimeout(1);
    }

    @Test(expected = IllegalStateException.class)
    public void setTimeoutThrowWhenDone() {
        FutureImpl<Void> future = createFuture(new Procedure() { @Override public void call() { } });
        future.postSuccess(null);
        future.setTimeout(1);
    }

    @Test(expected = CancellationException.class)
    public void setTimeoutCancelsTaskWhenTimedOut() {
        FutureImpl<Void> future = createFuture(new Procedure() { @Override public void call() { } });
        future.setTimeout(1).sync();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        service.cancel();
    }

    private <ReturnType> FutureImpl<ReturnType> createFuture(Procedure p) {
        return new FutureImpl<>(service, p);
    }

    private <ReturnType> FutureImpl<ReturnType> createFuture() {
        return createFuture(null);
    }
}