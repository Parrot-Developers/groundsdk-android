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

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class ExecutorTests {

    private Thread mTestThread;

    private Thread mMainThread;

    private Handler mMainThreadHandler;

    @Before
    public void setUp() {
        mTestThread = Thread.currentThread();
        mMainThread = Looper.getMainLooper().getThread();
        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    @After
    public void tearDown() {
        mMainThreadHandler.removeCallbacksAndMessages(null);
        runOnMainThread(Executor::dispose);
    }

    @Test
    public void testDispose() {
        runOnMainThread(() -> {
            ConditionVariable fgLock = new ConditionVariable();

            // this task should be canceled
            Task<Void> task = Executor.runInBackground(() -> {
                // this runnable should never get a chance to run
                Executor.postOnMainThread(SHOULD_NEVER_RUN);
                fgLock.open();
                return BLOCKS_UNTIL_CANCELED.call();
            });

            // ensure the runnable is posted
            fgLock.block();

            // Dispose the executor now
            Executor.dispose();

            assertThat(task, notNullValue());
            assertThat(task.cancel(), is(false)); // false means already canceled
        });
    }

    @Test
    public void testPostOnMainThread() {
        runOnMainThread(() -> assertThat(Thread.currentThread(), is(mMainThread)));
    }

    @Test(expected = IllegalStateException.class)
    public void testPostFromMainThread() {
        callOnMainThread((Callable<Void>) () -> {
            // postOnMainThread should throw when called from main thread.
            Executor.postOnMainThread(SHOULD_NEVER_RUN);
            return null;
        });
    }

    @Test
    public void testSchedule() {
        ConditionVariable fgLock = new ConditionVariable();
        long delayMs = 100;

        runOnMainThread(() -> {
            // now on main thread
            long now = SystemClock.currentThreadTimeMillis();
            Executor.schedule(() -> {
                // should still be on main thread...
                assertThat(Thread.currentThread(), is(mMainThread));
                // ... but later
                assertThat(System.currentTimeMillis(), greaterThanOrEqualTo(now + delayMs));
                fgLock.open();
            }, delayMs);
        });

        fgLock.block();
    }

    @Test(expected = IllegalStateException.class)
    public void testScheduleNotFromMainThread() {
        // schedule should throw when not called from main thread.
        Executor.schedule(SHOULD_NEVER_RUN, 1000);
    }

    @Test
    public void testUnschedule() {
        runOnMainThread(() -> {
            // now on main thread
            Executor.schedule(SHOULD_NEVER_RUN, 0);
            Executor.unschedule(SHOULD_NEVER_RUN);
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testUnscheduleNotFromMainThread() {
        // unschedule should throw when not called from main thread.
        Executor.unschedule(SHOULD_NEVER_RUN);
    }

    @Test
    public void testRunInBackground() {
        ConditionVariable fgLock = new ConditionVariable();
        Task<?> task = callOnMainThread((Callable<Task<?>>) () -> Executor.runInBackground((Callable<Void>) () -> {
            assertThat(Thread.currentThread(), not(mTestThread));
            assertThat(Thread.currentThread(), not(mMainThread));
            fgLock.open();
            return null;
        }));

        assertThat(task, notNullValue());
        fgLock.block();
    }

    @Test
    public void testBackgroundTaskResult() {
        ConditionVariable fgLock = new ConditionVariable();

        // first we register the listener _before_ the task completes
        Task.CompletionListener<Object> listensBefore = newMockListener(() -> {
            // should be called on the main thread when complete
            assertThat(Thread.currentThread(), is(mMainThread));
            fgLock.open();
        });

        Task<Object> task = callOnMainThread(() -> {
            ConditionVariable bgLock = new ConditionVariable();

            Task<Object> t = Executor.runInBackground(() -> {
                bgLock.block();
                return RESULT;
            }).whenComplete(listensBefore);

            Mockito.verify(listensBefore, Mockito.never()).onTaskComplete(
                    Mockito.any(), Mockito.any(), Mockito.anyBoolean());

            bgLock.open();
            return t;
        });

        assertThat(task, notNullValue());

        fgLock.block();
        Mockito.verify(listensBefore, Mockito.times(1)).onTaskComplete(RESULT, null, false);

        runOnMainThread(() -> {
            // now we register another listener _after_ the task has complete
            Task.CompletionListener<Object> listensAfter = newMockListener(() -> {
                // should be called immediately on the main thread
                assertThat(Thread.currentThread(), is(mMainThread));
            });

            task.whenComplete(listensAfter);
            Mockito.verify(listensAfter, Mockito.times(1)).onTaskComplete(RESULT, null, false);

            // finally, cancelling a completed task should do nothing
            assertThat(task.cancel(), is(false));
            Mockito.verifyNoMoreInteractions(listensBefore, listensAfter);
        });
    }

    @Test
    public void testBackgroundTaskError() {
        ConditionVariable fgLock = new ConditionVariable();

        // first we register the listener _before_ the task completes
        Task.CompletionListener<Object> listensBefore = newMockListener(() -> {
            // should be called on the main thread when complete
            assertThat(Thread.currentThread(), is(mMainThread));
            fgLock.open();
        });

        Task<Object> task = callOnMainThread(() -> {
            ConditionVariable bgLock = new ConditionVariable();

            Task<Object> t = Executor.runInBackground(() -> {
                bgLock.block();
                throw FAILURE;
            }).whenComplete(listensBefore);

            Mockito.verify(listensBefore, Mockito.never()).onTaskComplete(
                    Mockito.any(), Mockito.any(), Mockito.anyBoolean());

            bgLock.open();
            return t;
        });

        assertThat(task, notNullValue());

        fgLock.block();
        Mockito.verify(listensBefore, Mockito.times(1)).onTaskComplete(null, FAILURE, false);

        runOnMainThread(() -> {
            // now we register another listener _after_ the task has complete
            Task.CompletionListener<Object> listensAfter = newMockListener(() -> {
                // should be called immediately on the main thread
                assertThat(Thread.currentThread(), is(mMainThread));
            });

            task.whenComplete(listensAfter);
            Mockito.verify(listensAfter, Mockito.times(1)).onTaskComplete(null, FAILURE, false);

            // finally, cancelling a completed task should do nothing
            assertThat(task.cancel(), is(false));
            Mockito.verifyNoMoreInteractions(listensBefore, listensAfter);

        });
    }

    @Test
    public void testBackgroundTaskCancel() {
        ConditionVariable fgLock = new ConditionVariable();

        // first we register the listener _before_ the task is canceled
        Task.CompletionListener<Object> listensBefore = newMockListener(() -> {
            // should be called on the main thread when canceled
            assertThat(Thread.currentThread(), is(mMainThread));
            fgLock.open();
        });

        Task<Void> task = callOnMainThread(() -> {
            Task<Void> t = Executor.runInBackground(BLOCKS_UNTIL_CANCELED).whenComplete(listensBefore);

            Mockito.verify(listensBefore, Mockito.never()).onTaskComplete(
                    Mockito.any(), Mockito.any(), Mockito.anyBoolean());

            // now cancel the task
            assertThat(t.cancel(), is(true));

            return t;
        });

        fgLock.block();
        Mockito.verify(listensBefore, Mockito.times(1)).onTaskComplete(null, null, true);

        runOnMainThread(() -> {
            // now we register another listener _after_ the task has been canceled
            Task.CompletionListener<Object> listensAfter = newMockListener(() -> {
                // should be called immediately on the main thread
                assertThat(Thread.currentThread(), is(mMainThread));
            });

            task.whenComplete(listensAfter);
            Mockito.verify(listensAfter, Mockito.times(1)).onTaskComplete(null, null, true);

            // finally, cancelling a completed task should do nothing
            assertThat(task.cancel(), is(false));
            Mockito.verifyNoMoreInteractions(listensBefore, listensAfter);
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testRunInBackgroundFromNonMainThread() {
        // runInBackground should throw when not called from main thread.
        Executor.runInBackground(RETURNS_NULL);
    }

    @Test(expected = IllegalStateException.class)
    public void testBackgroundTaskObservationFromNonMainThread() {
        Task<?> task = callOnMainThread((Callable<Task<?>>) () -> Executor.runInBackground(RETURNS_NULL));

        // whenComplete should throw when not called from main thread.
        task.whenComplete((Task.CompletionListener<Object>) (result, error, canceled) -> {
            throw new AssertionError("Reached the unreachable");
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testBackgroundTaskCancelFromNonMainThread() {
        Task<?> task = callOnMainThread((Callable<Task<?>>) () -> Executor.runInBackground(RETURNS_NULL));

        // cancel should throw when not called from main thread.
        task.cancel();
    }

    @NonNull
    private static Task.CompletionListener<Object> newMockListener(@NonNull Runnable onCalled) {
        @SuppressWarnings("unchecked")
        Task.CompletionListener<Object> listener = Mockito.mock(Task.CompletionListener.class);
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            onCalled.run();
            return null;
        }).when(listener).onTaskComplete(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        return listener;
    }

    private void runOnMainThread(@NonNull Runnable runnable) {
        callOnMainThread(Executors.callable(runnable));
    }

    // posts the given callable on the main thread and waits for it on the current thread then return the result
    // Rethrows on the current thread any exception thrown from the callable on the main thread
    private <T> T callOnMainThread(@NonNull Callable<T> callable) {
        ConditionVariable sync = new ConditionVariable();
        @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
        T[] result = (T[]) new Object[] {null};
        Throwable[] exception = new Exception[] {null};
        mMainThreadHandler.post(() -> {
            try {
                result[0] = callable.call();
            } catch (Exception e) {
                exception[0] = e;
            } finally {
                sync.open();
            }
        });

        sync.block();

        if (exception[0] != null) {
            throwUnchecked(exception[0]);
        }
        return result[0];
    }

    private static final Object RESULT = new Object();

    private static final Exception FAILURE = new Exception();

    private static final Callable<Void> RETURNS_NULL = () -> null;

    private static final Callable<Void> BLOCKS_UNTIL_CANCELED = () -> {
        new Semaphore(0).acquire(); // blocks until thread is interrupted then throws InterruptException
        throw new AssertionError("Reached the unreachable");
    };

    private static final Runnable SHOULD_NEVER_RUN = () -> {
        throw new AssertionError("Reached the unreachable");
    };

    private static void throwUnchecked(@NonNull Throwable throwable) {
        final class Thrower<T extends Throwable> {

            @SuppressWarnings("unchecked")
            private Thrower() throws T {
                throw (T) throwable;
            }
        }
        //noinspection Convert2Diamond,ResultOfObjectAllocationIgnored
        new Thrower<RuntimeException>();
    }
}
