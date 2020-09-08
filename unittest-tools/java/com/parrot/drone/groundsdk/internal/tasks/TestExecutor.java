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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class TestExecutor {

    public static void setup() {
        Executor.setBackgroundThreadScheduler(DIRECT_BACKGROUND_SCHEDULER);
        Executor.setMainThreadScheduler(DIRECT_MAIN_SCHEDULER);
    }

    public static void setDirectMainThreadScheduler() {
        Executor.setMainThreadScheduler(DIRECT_MAIN_SCHEDULER);
    }

    public static void allowBackgroundTasksFromAnyThread() {
        Executor.setMainThreadScheduler(new MainThreadSchedulerWrapper(Executor.getMainThreadScheduler()) {

            @Override
            public void assertMainThread() throws IllegalStateException {
            }
        });
    }

    public static void teardown() {
        Executor.setBackgroundThreadScheduler(null);
        Executor.setMainThreadScheduler(null);
        DIRECT_MAIN_SCHEDULER.shutdown();
    }

    public static void mockTimePasses(long time, @NonNull TimeUnit unit) {
        MainThreadScheduler scheduler = Executor.getMainThreadScheduler();
        if (scheduler == DIRECT_MAIN_SCHEDULER) {
            ((DirectMainScheduler) scheduler).mockTimePasses(unit.toMillis(time));
        }
    }

    private static final ExecutorService DIRECT_EXECUTOR_SERVICE = new AbstractExecutorService() {

        @Override
        public void shutdown() {
        }

        @NonNull
        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };

    private static final BackgroundThreadScheduler DIRECT_BACKGROUND_SCHEDULER = new BackgroundThreadScheduler() {

        @Override
        public <T> Task<T> submit(@NonNull Callable<T> job) {
            return Task.execute(job, DIRECT_EXECUTOR_SERVICE);
        }

        @Override
        public void shutdown() {
        }
    };

    private static final class DirectMainScheduler implements MainThreadScheduler {

        @NonNull
        private final Map<Runnable, Long> mSchedule = new HashMap<>();

        @Override
        public void post(@NonNull Runnable runnable, @NonNull PostFromMainThreadPolicy policy) {
            runnable.run();
        }

        @Override
        public void post(@NonNull Runnable runnable, long delayMillis) {
            mSchedule.put(runnable, delayMillis);
        }

        @Override
        public void cancel(@NonNull Runnable runnable) {
            mSchedule.remove(runnable);
        }

        @Override
        public void shutdown() {
            mSchedule.clear();
        }

        @Override
        public void assertMainThread() {
        }

        void mockTimePasses(long milliseconds) {
            for (Iterator<Runnable> iter = mSchedule.keySet().iterator(); iter.hasNext(); ) {
                Runnable runnable = iter.next();
                //noinspection ConstantConditions
                long schedule = mSchedule.get(runnable);
                schedule -= milliseconds;
                if (schedule <= 0) {
                    iter.remove();
                    runnable.run();
                } else {
                    mSchedule.put(runnable, schedule);
                }
            }
        }
    }

    private static final MainThreadScheduler DIRECT_MAIN_SCHEDULER = new DirectMainScheduler();

    private static class MainThreadSchedulerWrapper implements MainThreadScheduler {

        @NonNull
        private final MainThreadScheduler mDelegate;

        MainThreadSchedulerWrapper(@NonNull MainThreadScheduler delegate) {
            mDelegate = delegate;
        }

        @Override
        public void post(@NonNull Runnable runnable, @NonNull PostFromMainThreadPolicy policy) {
            mDelegate.post(runnable, policy);
        }

        @Override
        public void post(@NonNull Runnable runnable, long delayMillis) {
            mDelegate.post(runnable, delayMillis);
        }

        @Override
        public void cancel(@NonNull Runnable runnable) {
            mDelegate.cancel(runnable);
        }

        @Override
        public void shutdown() {
            mDelegate.shutdown();
        }

        @Override
        public void assertMainThread() throws IllegalStateException {
            mDelegate.assertMainThread();
        }
    }

    private TestExecutor() {
    }
}
