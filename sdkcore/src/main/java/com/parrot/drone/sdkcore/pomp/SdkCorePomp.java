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

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.SdkCore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Native pomp loop wrapper API.
 */
final class SdkCorePomp {

    /** Int definition of context flags. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLAG_IN_MAIN, FLAG_IN_POMP})
    @interface Flag {}

    /* Numerical values MUST be kept in sync with C enum context_flag in sdkcore_pomp.c */

    /** Indicates that current call runs in main loop context. */
    static final byte FLAG_IN_MAIN = 0;

    /** Indicates that current call runs in pomp loop context. */
    static final byte FLAG_IN_POMP = 1;

    /**
     * Creates a new {@code SdkCorePomp} native backend.
     * <p>
     * Caller thread <strong>MUST</strong> have a looper.
     *
     * @param contextFlag optional byte buffer used by native code to flag whether current call is running in pomp loop
     *                    context; given byte buffer must be {@link ByteBuffer#isDirect() direct}; flag is first byte
     *                    in buffer; value is either {@link #FLAG_IN_MAIN} when running in main loop context, or
     *                    {@link #FLAG_IN_POMP} when running in pomp loop context; may be {@code null} in order not to
     *                    use this facility
     *
     * @return pointer onto created native backend if successful, otherwise {@code 0}
     */
    static native long nativeInit(@Nullable ByteBuffer contextFlag);

    /**
     * Disposes {@code SdkCorePomp} native backend.
     * <p>
     * This method <strong>MUST</strong> be called from the same looper thread from which the native backend was
     * {@link #nativeInit(ByteBuffer) created}.
     *
     * @param nativePtr pointer onto SdkCorePomp native backend to dispose
     */
    static native void nativeDispose(long nativePtr);

    /**
     * Private constructor for static utility class.
     */
    private SdkCorePomp() {
    }

    /* JNI declarations and setup */
    static {
        SdkCore.init();
    }
}
