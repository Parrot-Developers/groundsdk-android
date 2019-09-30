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

package com.parrot.drone.sdkcore;

import android.graphics.Rect;
import android.system.Os;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.sdkcore.BuildConfig;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class to load jni shared library.
 */
public final class SdkCore {

    /** load guard. */
    private static final AtomicBoolean mLoaded = new AtomicBoolean();

    /**
     * Init jni.
     */
    public static void init() {
        if (mLoaded.compareAndSet(false, true)) {
            // initialize environment variables first ...
            if (BuildConfig.DEBUG) {
                try {
                    Os.setenv(ULog.DEFAULT_LEVEL_ENV_VAR, Integer.toString(ULog.ULOG_DEBUG), true);
                } catch (Exception ignored) {
                    // at this point ULog native is not loaded: failure cannot be logged
                }
            }
            // ... then load library
            System.loadLibrary("sdkcore");
            nativeRectClassInit(Rect.class);
        }
    }

    /**
     * Private constructor of utility class.
     */
    private SdkCore() {
    }

    /* JNI declarations and setup */
    private static native void nativeRectClassInit(@NonNull Class<Rect> rectClass);
}
