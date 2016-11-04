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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * About the result of an asynchronous task.
 */
public interface Future<ReturnType> {
    /**
     * Returns true when the task is completed, false otherwise.
     * Completion only refers to the end of the task, if the task
     * was cancelled, and error was thrown or it's successful, this
     * method will return true.
     * @return true if the task is done
     */
    boolean isDone();

    /**
     * Returns true when the task is done and was not cancelled nor
     * any error was raised.
     * @return true if the task is completed successfully
     */
    boolean isSuccessful();

    /**
     * Returns true when the task is done and was cancelled.
     * @return true if the task was cancelled
     */
    boolean isCancelled();

    /**
     * Returns true if the task can be cancelled and it is able to set a timeout
     * @return true if cancellable
     */
    boolean isCancelable();

    /**
     * Attempts to cancel the task. If it is running the task,
     * with {@code mayInterrupt} will also interrupt the execution.
     * If the task is done, the attempt will fail.<br>
     * When cancelled, the cause will be an {@link CancellationException}.
     * @param mayInterrupt indicates if may interrupt the execution
     */
    void cancel(boolean mayInterrupt);

    /**
     * Calls the {@link Callback} when the task is done. If the task is done,
     * the callback is called directly. The callback cannot be null.
     * @param cbk callback
     * @return this same {@link Future}
     */
    Future<ReturnType> whenDone(final Callback<Future<ReturnType>> cbk);

    /**
     * When set to a positive value (0 not included), will set a timeout,
     * starting the instant of type that this method was called, until
     * the {@code milliseconds}. When the timeout is fired and the {@link Future}
     * is not done, will cancel the task.
     * <p>
     * Call this method when a timeout is already set, will replace the
     * timeout for a new one.
     * </p>
     * @param milliseconds time to timeout the task
     * @return this same {@link Future}
     */
    Future<ReturnType> setTimeout(long milliseconds);

    /**
     * Returns the value of the future if it is done. If not, then
     * returns {@code null}. Beware that {@code null} can also be
     * returned by the task, check {@link #isDone()} to determine if
     * the task is done.
     * @return the returned value when done and successful
     */
    ReturnType getValueNow();

    /**
     * If the task is done and is not successful, then this method
     * gets the cause of the failure. In other cases, returns {@code null}.
     * @return a {@link Throwable} cause of the failure of the task
     */
    Throwable cause();

    /**
     * Waits at most {@code millis} milliseconds for the ending of the task.
     * @param millis time in milliseconds to wait
     * @return the returned value
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws CancellationException if the computation was cancelled
     */
    ReturnType getValue(long millis) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Waits at most {@code millis} milliseconds for the ending of the task.
     * If this {@link Thread} is interrupted, silently will catch the exception
     * and continue waiting.
     * @param millis time to wait in milliseconds
     * @return the returned value
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws CancellationException if the computation was cancelled
     */
    ReturnType getValueUninterrumptibly(long millis) throws ExecutionException, TimeoutException;

    /**
     * Waits until the task is done.
     * @return the returned value
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws CancellationException if the computation was cancelled
     */
    ReturnType getValue() throws ExecutionException, InterruptedException;

    /**
     * Waits until the task is done, ignoring interruptions.
     * @return the returned value
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws CancellationException if the computation was cancelled
     */
    ReturnType getValueUninterrumptibly() throws ExecutionException, InterruptedException;

    /**
     * Waits until the end of the task. If the task fails, rethrows
     * the cause of the failure.
     * @return this same {@link Future}
     */
    Future<ReturnType> sync();
}
