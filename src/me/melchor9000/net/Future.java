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
