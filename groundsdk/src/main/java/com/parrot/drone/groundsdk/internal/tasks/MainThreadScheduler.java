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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_EXECUTOR;

/**
 * The interface to implement the main thread scheduler.
 * <p>
 * Only exposed to allow mocking in tests.
 */
interface MainThreadScheduler {

    /**
     * Defines the policy to observe when {@link #post(Runnable, PostFromMainThreadPolicy)} method is called from
     * main thread.
     */
    enum PostFromMainThreadPolicy {

        /** Denies call, throwing an IllegalStateException. */
        DENY,

        /** Allows call, runnable is posted onto the main thread for later execution. */
        POST,

        /** Allows call, runnable {@link Runnable#run run} method is called directly. */
        RUN
    }

    /**
     * Posts the given runnable to the main thread.
     *
     * @param runnable runnable to post
     * @param policy   policy to observe when the runnable is posted from the main thread
     */
    void post(@NonNull Runnable runnable, @NonNull PostFromMainThreadPolicy policy);

    /**
     * Schedules the given runnable for later execution on the main thread.
     *
     * @param runnable    runnable to post
     * @param delayMillis delay, in milliseconds, before the runnable is executed.
     */
    void post(@NonNull Runnable runnable, long delayMillis);

    /**
     * Cancels the scheduled runnable from later execution on the main thread.
     *
     * @param runnable runnable to cancel
     */
    void cancel(@NonNull Runnable runnable);

    /**
     * Cancels all pending runnables from execution on the main thread.
     */
    void shutdown();

    /**
     * Asserts that the thread calling this method is the main thread.
     *
     * @throws IllegalStateException if not called from main thread
     */
    void assertMainThread() throws IllegalStateException;

    /**
     * Default {@code MainThreadScheduler} implementation.
     */
    final class Default implements MainThreadScheduler {

        /** All submitted runnables that have not been processed yet. */
        @NonNull
        private final List<Runnable> mSubmittedRunnables;

        /** Handler on android's main looper thread. */
        @NonNull
        private final Handler mMainHandler;

        /**
         * Constructor.
         */
        Default() {
            ULog.d(TAG_EXECUTOR, "Starting main thread scheduler");
            mSubmittedRunnables = new CopyOnWriteArrayList<>();
            mMainHandler = new Handler(Looper.getMainLooper()) {

                @Override
                public void dispatchMessage(Message msg) {
                    Runnable runnable = msg.getCallback();
                    if (ULog.d(TAG_EXECUTOR)) {
                        ULog.d(TAG_EXECUTOR, "[main] About to process: " + runnable);
                    }

                    super.dispatchMessage(msg);

                    if (ULog.d(TAG_EXECUTOR)) {
                        ULog.d(TAG_EXECUTOR, "[main] Done processing: " + runnable);
                    }

                    mSubmittedRunnables.remove(msg.getCallback());
                }
            };
        }

        @Override
        public void post(@NonNull Runnable runnable, @NonNull PostFromMainThreadPolicy policy) {
            if (ULog.d(TAG_EXECUTOR)) {
                ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName() + "] Posting on main [policy: "
                                     + policy + "]: " + runnable);
            }
            if (mMainHandler.getLooper().getThread() == Thread.currentThread()) {
                switch (policy) {
                    case DENY:
                        throw new IllegalStateException("Already on main thread");
                    case POST:
                        mSubmittedRunnables.add(runnable);
                        mMainHandler.post(runnable);
                        break;
                    case RUN:
                        runnable.run();
                        break;
                }
            } else {
                mSubmittedRunnables.add(runnable);
                mMainHandler.post(runnable);
            }
        }

        @Override
        public void post(@NonNull Runnable runnable, long delayMillis) {
            if (ULog.d(TAG_EXECUTOR)) {
                ULog.d(TAG_EXECUTOR, "[" + Thread.currentThread().getName() + "] Scheduling on main [delay: "
                                     + delayMillis + "]: " + runnable);
            }
            // post with delay is a facility that is only supposed to be used to post
            // timeout-like runnables from the main thread, to the main thread.
            // Any other use is forbidden until proved worthwhile
            assertMainThread();
            mSubmittedRunnables.add(runnable);
            mMainHandler.postDelayed(runnable, delayMillis);
        }

        @Override
        public void cancel(@NonNull Runnable runnable) {

            assertMainThread();
            mMainHandler.removeCallbacks(runnable);
            if (mSubmittedRunnables.remove(runnable) && ULog.d(TAG_EXECUTOR)) {
                ULog.d(TAG_EXECUTOR, "[main] Unscheduled from main: " + runnable);
            }
        }

        @Override
        public void shutdown() {
            assertMainThread();
            mMainHandler.removeCallbacksAndMessages(null);
            mSubmittedRunnables.clear();
            ULog.d(TAG_EXECUTOR, "Stopped main thread scheduler");
        }

        @Override
        public void assertMainThread() throws IllegalStateException {
            if (mMainHandler.getLooper().getThread() != Thread.currentThread()) {
                throw new IllegalStateException("Not on main thread");
            }
        }

        /**
         * Debug dump.
         *
         * @param writer writer to dump to
         */
        void dump(@NonNull PrintWriter writer) {
            writer.write("Main scheduler: \n");
            writer.write("\tPending runnables:" + mSubmittedRunnables.size() + "\n");
            for (Runnable runnable : mSubmittedRunnables) {
                writer.write("\t\t" + runnable + "\n");
            }
        }
    }
}
