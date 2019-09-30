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

package com.parrot.drone.sdkcore.pomp;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Allows to dispatch {@link Runnable runnables} from a thread to the client looper thread.
 */
class Dispatcher {

    /** Collects all posted runnables that have not been processed yet. */
    @NonNull
    private final Deque<Runnable> mQueue;

    /** Android Handler that will process runnables. {@code null} when dispatcher is closed. */
    @Nullable
    private Handler mHandler;

    /**
     * Constructor.
     * <p>
     * Calling thread <strong>MUST</strong> have a looper.
     */
    Dispatcher() {
        // Use an array-backed collection as we don't want to pay for node allocation of linked-list variants each time
        // we post an element, which may occur quite frequently.
        mQueue = new ArrayDeque<>();
        mHandler = new Handler();
    }

    /**
     * Processes a dispatched runnable on the client looper thread.
     * <p>
     * Subclasses may override this method to customize runnable processing to their own needs. <br/>
     * By default, this method just {@link Runnable#run runs} the given {@code runnable}.
     *
     * @param runnable runnable to process
     */
    void handle(@NonNull Runnable runnable) {
        runnable.run();
    }

    /**
     * Post a runnable for processing on the client looper thread.
     * <p>
     * This method may be called from any thread.
     *
     * @param runnable runnable to post
     *
     * @return {@code true} in case the runnable could be dispatched, otherwise {@code false}
     */
    final boolean post(@NonNull Runnable runnable) {
        synchronized (this) {
            if (mHandler == null) {
                return false;
            }
            mQueue.addLast(runnable);
            mHandler.removeCallbacksAndMessages(null);
            if (mHandler.post(mDispatch)) {
                return true;
            }
            mQueue.removeLast();
            return false;
        }
    }

    /**
     * Closes the dispatcher.
     * <p>
     * All posted runnable that have not yet been processed are processed synchronously from this method. <br/>
     * Note that {@link #handle(Runnable)} is still called for each such pending runnable as a result.
     * <p>
     * This method <strong>MUST</strong> be called on the client looper thread.
     * <p>
     * Once closed, this {@code Dispatcher} instance cannot be re-used.
     */
    final void close() {
        synchronized (this) {
            if (mHandler == null) {
                throw new IllegalStateException("Already closed");
            }
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        processQueue();
    }

    /**
     * Runnable wrapping call to {@link #processQueue()}.
     * <p>
     * Allows not to allocate a lambda each time {@link #mHandler}.{@link Handler#post post()} is called, as would be
     * the case if the method reference was passed to {@code post()} directly.
     */
    @NonNull
    private final Runnable mDispatch = this::processQueue;

    /**
     * Process all queued runnables in post order.
     */
    private void processQueue() {
        Runnable next;
        do {
            synchronized (this) {
                next = mQueue.pollFirst();
            }
            if (next != null) {
                handle(next);
            }
        } while (next != null);
    }
}
