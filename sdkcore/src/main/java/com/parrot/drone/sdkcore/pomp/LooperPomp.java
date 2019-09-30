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

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * {@link PompLoop} implementation that runs the pomp loop on the client looper thread.
 */
final class LooperPomp implements PompLoop {

    /** Main thread, creator of this instance. */
    @NonNull
    private final Thread mMainThread;

    /** Dispatches runnables on the main loop. */
    @NonNull
    private final Dispatcher mMainDispatcher;

    /** Dispatches runnables on the pomp loop. */
    @NonNull
    private final Dispatcher mPompDispatcher;

    /** First byte indicates whether calling code runs in pomp or main loop context. Also modified from native. */
    @NonNull
    private final ByteBuffer mContextFlag;

    /** {@link SdkCorePomp} native backend pointer. */
    private long mPompPtr;

    /**
     * Constructor.
     * <p>
     * Caller thread <strong>MUST</strong> have a looper.
     */
    LooperPomp() {
        mMainThread = Thread.currentThread();
        mContextFlag = ByteBuffer.allocateDirect(1).put(0, SdkCorePomp.FLAG_IN_MAIN);
        mMainDispatcher = new FlaggingDispatcher(SdkCorePomp.FLAG_IN_MAIN);
        mPompDispatcher = new FlaggingDispatcher(SdkCorePomp.FLAG_IN_POMP);
        mPompPtr = SdkCorePomp.nativeInit(mContextFlag);
        if (mPompPtr == 0) {
            throw new Error("Could not initialize native pomp loop");
        }
    }

    @Override
    public void onMain(@NonNull Runnable runnable) {
        if (mPompPtr == 0) {
            throw new IllegalStateException("Pomp disposed");
        }
        if (inMain()) {
            throw new IllegalStateException("Already on main loop");
        }
        mMainDispatcher.post(runnable);
    }

    @Override
    public void onPomp(@NonNull Runnable runnable) {
        if (mPompPtr == 0) {
            throw new IllegalStateException("Pomp disposed");
        }
        if (inPomp()) {
            throw new IllegalStateException("Already on pomp loop");
        }
        mPompDispatcher.post(runnable);
    }

    @Override
    public boolean inPomp() {
        return Thread.currentThread() == mMainThread && mContextFlag.get(0) == SdkCorePomp.FLAG_IN_POMP;
    }

    @Override
    public boolean inMain() {
        return Thread.currentThread() == mMainThread && mContextFlag.get(0) == SdkCorePomp.FLAG_IN_MAIN;
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
        if (mPompPtr == 0) {
            throw new IllegalStateException("Already disposed");
        }
        SdkCorePomp.nativeDispose(mPompPtr);
        mPompDispatcher.close();
        mMainDispatcher.close();
        mPompPtr = 0;
    }

    /**
     * Dispatcher that manages context flag around posted runnable processing.
     */
    private final class FlaggingDispatcher extends Dispatcher {

        /** Flag set before a posted runnable is processed. */
        private final byte mFlag;

        /**
         * Constructor.
         *
         * @param flag flag to set before some posted runnable gets processed.
         */
        FlaggingDispatcher(@SdkCorePomp.Flag int flag) {
            mFlag = (byte) flag;
        }

        @Override
        void handle(@NonNull Runnable runnable) {
            byte flag = mContextFlag.get(0);
            try {
                mContextFlag.put(0, mFlag);
                super.handle(runnable);
            } finally {
                mContextFlag.put(0, flag);
            }
        }
    }
}
