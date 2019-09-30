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

package com.parrot.drone.sdkcore.ulog;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.SdkCore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ULog tag used to associate log tag and log level.
 */
public final class ULogTag {

    /** ByteBuffer view on the native logging level for this tag. Level is first int in buffer. */
    private final ByteBuffer mLevel;

    /** Native JNI pointer on the tag ULog cookie. */
    private long mNativePtr;

    /**
     * Constructor.
     *
     * @param name tag.
     */
    public ULogTag(@NonNull String name) {
        mNativePtr = nativeInit(name);
        if (mNativePtr == 0) {
            throw new AssertionError("Failed to create ULogTag native backend");
        }

        mLevel = nativeGetLevel(mNativePtr);
        mLevel.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Destructor.
     */
    public void destroy() {
        nativeDispose(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Return the ULogTag native cookie pointer.
     *
     * @return the native pointer.
     */
    long getNativePtr() {
        return mNativePtr;
    }

    /**
     * Set the minimum level to log for the tag.
     *
     * @param level the minimum level.
     */
    public void setMinLevel(@ULog.Level int level) {
        nativeSetLevel(mNativePtr, level);
    }

    /**
     * Get the minimum level to log of the tag.
     *
     * @return the minimum level of log.
     */
    @ULog.Level
    public int getMinLevel() {
        return mLevel.getInt(0);
    }

    /* JNI declarations and setup */
    private static native long nativeInit(String name);

    private static native void nativeDispose(long nativePtr);

    private static native ByteBuffer nativeGetLevel(long nativePtr);

    private static native void nativeSetLevel(long nativePtr, int level);

    static {
        SdkCore.init();
    }
}


