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

import java.util.concurrent.Callable;

/**
 * A background job.
 * <p>
 * This is a convenience abstract base to implement self-contained tasks. Implementors may implement <ul>
 * <li>{@link #doInBackground} to specify the task background operation. By default, nothing is done. </li>
 * <li>{@link #onComplete} to specify foreground behaviour once the task completes</li>
 * </ul>
 *
 * @param <T> task result type
 */
public abstract class Job<T> {

    /** Running background task. */
    @Nullable
    private Task<T> mTask;

    /**
     * Launches the job.
     * <p>
     * A job can only be launched once after instantiation; subsequent calls to this method will only return the
     * background running task for this job, even after it completes.
     * <p>
     * job's {@link #doInBackground} will be executed on a background thread, then when this method completes
     * (either by returning a value, by throwing an exception, or because it was canceled externally),
     * job's {@link #onComplete} is executed on main thread.
     *
     * @return the background running task this job executes.
     */
    @NonNull
    public final Task<T> launch() {
        if (mTask == null) {
            mTask = Executor.runInBackground(mBackgroundBlock).whenComplete(mCompletionListener);
        }
        return mTask;
    }

    /**
     * Called on a background thread to actually perform the background task.
     * <p>
     * Subclasses may override this method. Default implementation simply returns null.
     *
     * @return the job result, may be {@code null}
     *
     * @throws Exception in case the background tasks completes with an error
     */
    @SuppressWarnings("RedundantThrows")
    @Nullable
    protected T doInBackground() throws Exception {
        return null;
    }

    /**
     * Called on the main thread when the background task is complete.
     * <p>
     * Subclasses may override this method. Default implementation does nothing.
     *
     * @param result   background task result, may be {@code null}
     * @param error    exception thrown by the background task. If {@code null}, then the task either completed
     *                 successfully or was canceled externally
     * @param canceled {@code true} if the background task was canceled externally before completion, otherwise
     *                 {@code false}
     */
    protected void onComplete(@Nullable T result, @Nullable Throwable error, boolean canceled) {

    }

    /** Background callable that is actually executed. */
    private final Callable<T> mBackgroundBlock = new Callable<T>() {

        @Override
        public T call() throws Exception {
            return doInBackground();
        }

        @Override
        public String toString() {
            return Job.this.toString();
        }
    };

    /** Completion listener notified when the background task completes. */
    private final Task.CompletionListener<T> mCompletionListener = this::onComplete;
}
