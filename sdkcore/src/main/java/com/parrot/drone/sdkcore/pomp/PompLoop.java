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

/**
 * Pomp loop.
 */
public interface PompLoop {

    /**
     * Creates a new pomp loop running on a dedicated, new background thread.
     *
     * @param name loop thread name
     *
     * @return a new {@code PompLoop} instance
     */
    @NonNull
    static PompLoop createOnNewThread(@NonNull String name) {
        return new HandlerThreadPomp(name);
    }

    /**
     * Creates a new pomp loop running in the current thread's looper.
     * <p>
     * Current thread <strong>MUST</strong> have a looper.
     *
     * @return a new {@code PompLoop} instance
     */
    @NonNull
    static PompLoop createOnMyLooper() {
        return new LooperPomp();
    }

    /**
     * Posts a runnable to the main loop.
     * <p>
     * This method <strong>MUST NOT</strong> be called from main loop.
     *
     * @param runnable runnable to post
     */
    void onMain(@NonNull Runnable runnable);

    /**
     * Posts a runnable to the pomp loop.
     * <p>
     * This method <strong>MUST NOT</strong> be called from pomp loop.
     *
     * @param runnable runnable to post
     */
    void onPomp(@NonNull Runnable runnable);

    /**
     * Tells whether caller currently runs in pomp loop.
     *
     * @return {@code true} if in pomp loop, otherwise {@code false}
     */
    boolean inPomp();

    /**
     * Tells whether caller currently runs in main loop.
     *
     * @return {@code true} if in main loop, otherwise {@code false}
     */
    boolean inMain();

    /**
     * Provides access to the loop native backend, for interfacing with other native sdkcore APIs that require a pomp
     * loop.
     * <p>
     * This method <strong>MUST ONLY</strong> be called either from main or pomp loop.
     *
     * @return pomp loop native backend pointer; {@code 0} if this {@code PompLoop} instance is disposed
     */
    long nativePtr();

    /**
     * Disposes this {@code PompLoop} instance.
     * <p>
     * This method blocks until all runnables that have been posted to the pomp loop are processed, then all runnables
     * posted to the main loop, which have not been processed yet are processed synchronously before the method returns.
     * <p>
     * This method <strong>MUST</strong> be called from main loop.
     */
    void dispose();
}
