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

import android.os.Process;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_EXECUTOR;

/**
 * The interface to implement the background thread scheduler.
 * <p>
 * Only exposed to allow mocking in tests.
 */
interface BackgroundThreadScheduler {

    /**
     * Submits a callable for execution on a background thread.
     *
     * @param job callable to execute
     * @param <T> type of result returned by the callable
     *
     * @return a background task that can be observed for completion and canceled.
     */
    <T> Task<T> submit(@NonNull Callable<T> job);

    /**
     * Cancels all submitted and all executing tasks.
     */
    void shutdown();

    /**
     * Default {@code BackgroundThreadScheduler} implementation.
     */
    final class Default implements BackgroundThreadScheduler {

        /** Generates pool's thread identifiers. */
        private final AtomicInteger mThreadNumSequence;

        /** All submitted tasks that have not completed yet. Used for final cancellation on shutdown. */
        @NonNull
        private final TaskGroup mSubmittedTasks;

        /** Background executor service processing all jobs. */
        @NonNull
        private final ExecutorService mExecutorService;

        /**
         * Constructor.
         */
        Default() {
            ULog.d(TAG_EXECUTOR, "Starting background thread scheduler");
            mThreadNumSequence = new AtomicInteger();
            mSubmittedTasks = new TaskGroup();
            mExecutorService = Executors.newCachedThreadPool(runnable ->
                    new Thread("bg-" + mThreadNumSequence.incrementAndGet()) {

                        @Override
                        public void run() {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                            runnable.run();
                        }
                    });
        }

        @Override
        public <T> Task<T> submit(@NonNull Callable<T> job) {
            if (ULog.d(TAG_EXECUTOR)) {
                ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName() + "] Submitting in background: " + job);
            }
            Task<T> task = Task.execute(job, mExecutorService);
            mSubmittedTasks.add(task);
            return task;
        }

        @Override
        public void shutdown() {
            mExecutorService.shutdown();
            mSubmittedTasks.cancelAll();
            ULog.d(TAG_EXECUTOR, "Stopped background thread scheduler");
        }

        /**
         * Debug dump.
         *
         * @param writer writer to dump to
         */
        void dump(@NonNull PrintWriter writer) {
            writer.write("Background scheduler: \n");
            writer.write("\t" + mExecutorService + "\n");
            Set<Task<?>> pendingTasks = mSubmittedTasks.listAll();
            writer.write("\t Pending tasks: " + pendingTasks.size() + "\n");
            for (Task<?> task : pendingTasks) {
                writer.write("\t\t" + task + "\n");
            }
        }
    }
}
