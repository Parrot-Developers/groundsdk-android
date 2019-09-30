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

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * Singleton object providing system time info.
 * <p>
 * This class exist mainly for mocking purposes in tests, which replace the default time provider singleton by their own
 * implementation to control time.
 */
public class TimeProvider {

    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return elapsed milliseconds since boot.
     */
    public static long elapsedRealtime() {
        return sInstance.getElapsedRealtime();
    }

    /** Default TimeProvider implementation. */
    private static final TimeProvider DEFAULT = new TimeProvider();

    /** Current TimeProvider singleton instance, points to the DEFAULT impl by default. */
    @NonNull
    private static TimeProvider sInstance = DEFAULT;

    /**
     * Sets TimeProvider singleton instance.
     * <p>
     * Used by test code to mock time.
     *
     * @param instance TimeProvider implementation to use as TimeProvider singleton
     *
     * @return {@code instance}
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static TimeProvider setInstance(@NonNull TimeProvider instance) {
        sInstance = instance;
        return instance;
    }

    /**
     * Resets default TimeProvider singleton.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void resetDefault() {
        setInstance(DEFAULT);
    }

    /**
     * Constructor.
     * <p>
     * For tests that defines their own TimeProvider mock implementation.
     */
    @VisibleForTesting
    protected TimeProvider() {
    }

    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return elapsed milliseconds since boot.
     */
    @VisibleForTesting
    public long getElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }
}
