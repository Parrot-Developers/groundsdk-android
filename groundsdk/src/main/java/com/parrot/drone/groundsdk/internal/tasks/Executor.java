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

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * The Executor utility allows for: <ul>
 * <li>Execution of cancellable tasks on background threads. </li>
 * <li>Execution of runnables from background tasks back to the main thread.</li>
 * <li>Scheduling of delayed runnables on the main thread.</li>
 * </ul>
 */
public final class Executor {

    /**
     * Runs a callable on a background thread.
     * <p>
     * To support cancellation properly, code within the callable should make sure to use interruptible methods
     * and/or to check the current thread's interrupt state in case of active polling.
     * <p>
     * Code within the callable is allowed to throw any {@link Exception}, which will be reported as failure to any
     * listener registered onto the task.
     * <p>
     * Otherwise than throwing an exception, the callable must return a result, possibly {@code null}, in which case
     * any listener registered onto the task will be notified of successful completion and handed that result.
     * <p>
     * Please refer to {@code Task} class for guidance concerning cancellation and observation of a background task.
     *
     * @param job callable to run
     * @param <T> type of result returned by the callable
     *
     * @return a task that can be observed for completion and canceled.
     */
    @NonNull
    public static <T> Task<T> runInBackground(@NonNull Callable<T> job) {
        return getBackgroundThreadScheduler().submit(job);
    }

    /**
     * Ensures that the thread calling this method is the main thread.
     *
     * @throws IllegalStateException if not called from the main thread
     */
    static void requireMainThread() {
        getMainThreadScheduler().assertMainThread();
    }

    /**
     * Posts the given runnable for execution on the main thread.
     * <p>
     * This method may only be called from within a background thread.
     *
     * @param runnable runnable to execute
     *
     * @throws IllegalStateException if called on main thread
     */
    public static void postOnMainThread(@NonNull Runnable runnable) {
        getMainThreadScheduler().post(runnable, MainThreadScheduler.PostFromMainThreadPolicy.DENY);
    }

    /**
     * Schedules the given runnable for execution on the main thread, after a delay.
     * <p>
     * Note that contrary to {@link #postOnMainThread(Runnable)}, this method can <strong>ONLY</strong> be called from
     * the <strong>MAIN</strong> thread.
     *
     * @param runnable    runnable to execute
     * @param delayMillis minimal delay, in milliseconds, to observe before executing the runnable
     *
     * @throws IllegalStateException if not called on main thread
     */
    public static void schedule(@NonNull Runnable runnable, long delayMillis) {
        getMainThreadScheduler().post(runnable, delayMillis);
    }

    /**
     * Cancels a scheduled runnable from execution on the main thread.
     *
     * @param runnable runnable to cancel
     */
    public static void unschedule(@NonNull Runnable runnable) {
        getMainThreadScheduler().cancel(runnable);
    }

    /**
     * Disposes the executor.
     * <p>
     * Cancels all pending and ongoing background tasks, as well as all pending foreground runnables.
     * <p>
     * This method should be called at engine shutdown.
     */
    public static void dispose() {
        synchronized (Executor.class) {
            if (sBackgroundThreadScheduler != null) {
                sBackgroundThreadScheduler.shutdown();
                sBackgroundThreadScheduler = null;
            }
            if (sMainThreadScheduler != null) {
                sMainThreadScheduler.shutdown();
                sMainThreadScheduler = null;
            }
        }
    }

    /** Background executor singleton instance. */
    private static BackgroundThreadScheduler sBackgroundThreadScheduler;

    /** Foreground main thread scheduler instance. */
    private static MainThreadScheduler sMainThreadScheduler;

    /**
     * Retrieves background thread scheduler singleton.
     * <p>
     * A default background scheduler instance, backed by a cached thread pool background executor service is created
     * in case no instance exists yet.
     *
     * @return the background executor service singleton
     */
    @NonNull
    private static BackgroundThreadScheduler getBackgroundThreadScheduler() {
        if (sBackgroundThreadScheduler == null) {
            synchronized (Executor.class) {
                if (sBackgroundThreadScheduler == null) {
                    sBackgroundThreadScheduler = new BackgroundThreadScheduler.Default();
                }
            }
        }
        return sBackgroundThreadScheduler;
    }

    /**
     * Retrieves main thread scheduler service singleton.
     * <p>
     * A default scheduler instance, backed by android main thread looper is created in case no instance exists yet.
     *
     * @return the main thread scheduler service singleton
     */
    @NonNull
    static MainThreadScheduler getMainThreadScheduler() {
        if (sMainThreadScheduler == null) {
            synchronized (Executor.class) {
                if (sMainThreadScheduler == null) {
                    sMainThreadScheduler = new MainThreadScheduler.Default();
                }
            }
        }
        return sMainThreadScheduler;
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public static void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--executor: dumps executor info\n");
        } else if (args.contains("--executor") || args.contains("--all")) {
            if (sBackgroundThreadScheduler instanceof BackgroundThreadScheduler.Default) {
                ((BackgroundThreadScheduler.Default) sBackgroundThreadScheduler).dump(writer);
            } else {
                writer.write("Background scheduler inactive\n");
            }
            if (sMainThreadScheduler instanceof MainThreadScheduler.Default) {
                ((MainThreadScheduler.Default) sMainThreadScheduler).dump(writer);
            } else {
                writer.write("Foreground scheduler inactive\n");
            }
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private Executor() {
    }

    /**
     * Sets the main thread scheduler singleton.
     * <p>
     * This method only exists to allow test code to provide a custom (probably direct) scheduler implementation for
     * easier testing.
     *
     * @param mainThreadScheduler main thread scheduler instance to set
     */
    @VisibleForTesting
    static void setMainThreadScheduler(@Nullable MainThreadScheduler mainThreadScheduler) {
        synchronized (Executor.class) {
            sMainThreadScheduler = mainThreadScheduler;
        }
    }

    /**
     * Sets the background thread scheduler singleton.
     * <p>
     * This method only exists to allow test code to provide a custom (probably direct) scheduler implementation for
     * easier testing.
     *
     * @param backgroundThreadScheduler background thread scheduler service instance to set
     */
    @VisibleForTesting
    static void setBackgroundThreadScheduler(@Nullable BackgroundThreadScheduler backgroundThreadScheduler) {
        synchronized (Executor.class) {
            sBackgroundThreadScheduler = backgroundThreadScheduler;
        }
    }
}
