/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.drone.groundsdk.internal.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_EXECUTOR;

/**
 * A cancellable background task.
 *
 * @param <T> type of result for this task
 */
public class Task<T> {

    /**
     * Creates a successfully completed task.
     *
     * @param result task result
     * @param <T>    type of result ot the completed task
     *
     * @return a new {@code Task} instance, that has already completed
     */
    public static <T> Task<T> success(@Nullable T result) {
        return new Task<>("", result, null, false);
    }

    /**
     * Creates a failed task.
     *
     * @param error task error
     * @param <T>   type of result ot the completed task
     *
     * @return a new {@code Task} instance, that has already completed
     */
    public static <T> Task<T> failure(@NonNull Throwable error) {
        return new Task<>("", null, error, false);
    }

    /**
     * A task completion listener.
     *
     * @param <T> type of result of the completed task
     */
    public interface CompletionListener<T> {

        /**
         * Called back when a task completes.
         * <p>
         * Called on the main thread when the task completes either successfully, or because it threw an exception,
         * or because it was canceled before completion.
         *
         * @param result   result of the task, may be {@code null} even in case of successful completion
         * @param error    exception thrown by the task. {@code null} if the task either is successful or was canceled
         * @param canceled {@code true} when the task was canceled before completion, otherwise {@code false}
         */
        void onTaskComplete(@Nullable T result, @Nullable Throwable error, boolean canceled);
    }

    /**
     * Executes a background task.
     * <p>
     * Creates a new {@code Task} that submits the given callable for background execution and monitors it.
     * <p>
     * This method <strong>MUST</strong> be called from <strong>MAIN</strong> thread.
     *
     * @param job      callable to run
     * @param executor executor service that will process the job
     * @param <T>      type of result the callable returns
     *
     * @return a new {@code Task} instance that can be used to cancel the background callable and be notified when it
     *         completes
     */
    static <T> Task<T> execute(@NonNull Callable<T> job, @NonNull ExecutorService executor) {
        return new Task<>(job, executor);
    }

    /** The task name, collected from the constructor argument {@link Callable#toString()}. */
    @NonNull
    private final String mName;

    /**
     * The task submitted to the executor, used for cancellation purposes. Instantiated at task construction time,
     * becomes {@code null} once the task completes.
     */
    @Nullable
    private FutureTask<?> mFutureTask;

    /**
     * Collects listeners registered until the tasks completes, at which point all registered listeners are notified
     * and this becomes {@code null}. Further registrations are notified directly without being added.
     */
    @Nullable
    private Set<CompletionListener<? super T>> mCompletionListeners;

    /**
     * Task result, {@code null} until the task completes. Instantiated and hydrated on background thread once the task
     * completes, then forwarded to main thread.
     */
    @Nullable
    private Result<T> mResult;

    /**
     * Constructor.
     * <p>
     * This method <strong>MUST</strong> be called from <strong>MAIN</strong> thread.
     *
     * @param job             callable block to run as a background task
     * @param executorService executor service that will process the job
     */
    private Task(@NonNull Callable<T> job, @NonNull ExecutorService executorService) {
        Executor.requireMainThread();
        mName = job.toString();
        mFutureTask = new FutureTask<T>(job) {

            /**
             * {@code true} if {@link #cancel(boolean)} has been called at least once, otherwise {@code false}.
             * <p>
             * The purpose of this flag is to ensure that {@link #isCancelled()} returns {@code true} when
             * {@link #cancel(boolean)} has been called, even if it has been called after task completion.
             */
            private boolean mCancelled;

            @Override
            public boolean isCancelled() {
                return super.isCancelled() || mCancelled;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                mCancelled = true;
                return super.cancel(mayInterruptIfRunning);
            }

            @Override
            public void run() {

                if (ULog.d(TAG_EXECUTOR)) {
                    ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName()
                                         + "] About to process: " + Task.this);
                }

                super.run();

                if (ULog.d(TAG_EXECUTOR)) {
                    ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName()
                                         + "] Done processing: " + Task.this);
                }

                RunnableResult result = new RunnableResult();
                try {
                    result.mValue = get();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    // since the task is 'done', getting the future should be immediate and non-blocking, so
                    // InterruptedException should never happen
                } catch (CancellationException e) {
                    result.mCanceled = true;
                } catch (ExecutionException e) {
                    result.mError = e.getCause();
                } finally {
                    Executor.getMainThreadScheduler().post(result,
                            MainThreadScheduler.PostFromMainThreadPolicy.RUN);
                }
            }
        };
        executorService.submit(mFutureTask);
    }

    /**
     * Constructor for an already completed task.
     *
     * @param name     task name
     * @param result   task result
     * @param error    task error
     * @param canceled {@code true} to make the task canceled, otherwise {@code false}
     */
    private Task(@NonNull String name, @Nullable T result, @Nullable Throwable error, boolean canceled) {
        mName = name;
        mResult = new Result<>();
        mResult.mValue = result;
        mResult.mCanceled = canceled;
        mResult.mError = error;
    }

    /**
     * Registers a listener to be notified of task completion.
     * <p>
     * The listener is called back on main thread, as soon as the task completes, or immediately in case the
     * tasks has already completed.
     * <p>
     * This method <strong>MUST</strong> be called from <strong>MAIN</strong> thread.
     *
     * @param listener completion listener to register
     *
     * @return this {@code Task}, to allow call chaining
     */
    public final Task<T> whenComplete(@NonNull CompletionListener<? super T> listener) {
        Executor.requireMainThread();
        if (mResult == null) {
            if (mCompletionListeners == null) {
                mCompletionListeners = new CopyOnWriteArraySet<>();
            }
            mCompletionListeners.add(listener);
        } else {
            notify(listener, mResult);
        }
        return this;
    }

    /**
     * Tells whether the task has completed, either successfully, with failure or because it was canceled.
     *
     * @return {@code true} if the task has completed, otherwise {@code false}
     */
    public boolean isComplete() {
        Executor.requireMainThread();
        return mResult != null;
    }

    /**
     * Cancels this task.
     * <p>
     * This method <strong>MUST</strong> be called from <strong>MAIN</strong> thread.
     *
     * @return {@code true} if the task was effectively canceled, otherwise {@code false} (which means the task has
     *         already completed)
     */
    public boolean cancel() {
        Executor.requireMainThread();
        boolean canceled = mFutureTask != null && mFutureTask.cancel(true);
        if (canceled && ULog.d(TAG_EXECUTOR)) {
            ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName() + "] Canceled: " + this);
        }
        return canceled;
    }

    /**
     * Called back on the main thread after the background task completes.
     *
     * @param result background task result
     */
    @VisibleForTesting
    final void onResult(@NonNull Result<T> result) {
        mResult = result;
        if (mFutureTask != null) {
            if (mResult.mCanceled |= mFutureTask.isCancelled()) {
                mResult.mError = null;
                mResult.mValue = null;
            }
            mFutureTask = null;
        }
        if (mCompletionListeners != null) {
            for (CompletionListener<? super T> listener : mCompletionListeners) {
                notify(listener, mResult);
            }
            mCompletionListeners = null;
        }
    }

    /**
     * Notifies the given completion listener with the given result's data.
     *
     * @param listener completion listener to notify
     * @param result   result to notify the listener with
     */
    private void notify(@NonNull CompletionListener<? super T> listener, @NonNull Result<T> result) {
        listener.onTaskComplete(result.mValue, result.mError, result.mCanceled);
    }

    /** Task result, aggregates task result value, possible error and cancellation status. */
    @VisibleForTesting
    static class Result<T> {

        /** Task result value. May be {@code null} even on successful completion. */
        @Nullable
        T mValue;

        /** Task error. Not {@code null} when the task failed. */
        @Nullable
        Throwable mError;

        /** {@code true} when the task has been canceled before completion. */
        boolean mCanceled;

        @NonNull
        @Override
        public String toString() {
            if (mCanceled) {
                return "CANCELED";
            } else if (mError != null) {
                return "FAILED [" + mError + "]";
            } else if (mValue != null) {
                return "RESULT: " + mValue;
            }
            return "SUCCESS";
        }
    }

    /** Task result that is also a runnable that posts back itself to the main thread. */
    private final class RunnableResult extends Result<T> implements Runnable {

        @Override
        public void run() {
            onResult(this);
        }

        @NonNull
        @Override
        public String toString() {
            return Task.this + " " + super.toString();
        }
    }

    @Override
    public final String toString() {
        return "Task [" + mName + "]";
    }

    /**
     * Constructor for test mocks.
     */
    @VisibleForTesting
    Task() {
        mName = super.toString();
    }
}
