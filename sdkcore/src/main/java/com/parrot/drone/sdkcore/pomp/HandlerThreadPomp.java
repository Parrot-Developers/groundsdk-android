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

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * {@link PompLoop} implementation that runs the pomp loop on a dedicated background thread.
 */
final class HandlerThreadPomp implements PompLoop {

    /** Main thread, creator of this instance. */
    @NonNull
    private final Thread mMainThread;

    /** Dispatches runnables to the main loop. */
    @NonNull
    private final Dispatcher mMainDispatcher;

    /** Pomp loop background thread. */
    @NonNull
    private final HandlerThread mPompThread;

    /** Dispatches runnables to the pomp loop. */
    @NonNull
    private final Handler mPompHandler;

    /** {@link SdkCorePomp} native backend pointer. */
    private long mPompPtr;

    /**
     * Constructor.
     * <p>
     * Caller thread <strong>MUST</strong> have a looper; it is considered to be the main thread.
     *
     * @param name background loop thread name
     */
    HandlerThreadPomp(@NonNull String name) {
        mMainThread = Thread.currentThread();
        mMainDispatcher = new Dispatcher();
        mPompThread = new HandlerThread(name) {

            /** Condition unlocked once native SdkCorePomp has been initialized. */
            private final ConditionVariable mPompInit = new ConditionVariable();

            @Override
            protected void onLooperPrepared() {
                mPompPtr = SdkCorePomp.nativeInit(null);
                mPompInit.open();
            }

            @Override
            public void run() {
                try {
                    super.run();
                } finally {
                    if (mPompPtr != 0) {
                        SdkCorePomp.nativeDispose(mPompPtr);
                        mPompPtr = 0;
                    }
                }
            }

            @Override
            public Looper getLooper() {
                // ensure that we pomp loop init is done
                mPompInit.block();
                return super.getLooper();
            }
        };

        mPompThread.start();
        mPompHandler = new Handler(mPompThread.getLooper());
        if (mPompPtr == 0) {
            mPompThread.quit();
            throw new Error("Could not initialize native pomp loop");
        }
    }

    @Override
    public void onMain(@NonNull Runnable runnable) {
        // ensure not already on client thread
        if (inMain()) {
            throw new IllegalStateException("Already on main loop");
        }
        if (!mMainDispatcher.post(runnable)) {
            throw new IllegalStateException("Pomp disposed");
        }
    }

    @Override
    public void onPomp(@NonNull Runnable runnable) {
        // ensure not already on pomp thread
        if (inPomp()) {
            throw new IllegalStateException("Already on pomp loop");
        }
        if (!mPompHandler.post(runnable)) {
            throw new IllegalStateException("Pomp disposed");
        }
    }

    @Override
    public boolean inPomp() {
        return Thread.currentThread() == mPompThread;
    }

    @Override
    public boolean inMain() {
        return Thread.currentThread() == mMainThread;
    }

    @Override
    public long nativePtr() {
        if (!inMain() && !inPomp()) {
            throw new IllegalStateException("Neither on main nor on pomp loop");
        }
        if (mPompPtr == 0) {
            throw new IllegalStateException("Pomp disposed");
        }
        return mPompPtr;
    }

    @Override
    public void dispose() {
        if (!inMain()) {
            throw new IllegalStateException("Not on main loop");
        }
        mPompThread.quitSafely();
        try {
            mPompThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        mMainDispatcher.close();
    }
}
