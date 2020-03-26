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

package com.parrot.drone.groundsdk.mavlink;

import android.util.SparseArray;

import java.io.IOException;
import java.io.Writer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MAVLink command which allows to set the still capture mode.
 */
public final class SetStillCaptureModeCommand extends MavlinkCommand {

    /** Still capture mode. */
    public enum Mode {

        /** Time-lapse mode (photos taken at regular time intervals). */
        TIMELAPSE(0),

        /** GPS-lapse mode (photos taken at regular GPS position intervals). */
        GPSLAPSE(1);

        /** MAVLink value. */
        private final int mValue;

        /**
         * Constructor.
         *
         * @param value MAVLink value
         */
        Mode(int value) {
            mValue = value;
        }

        /**
         * Retrieves the MAVLink value.
         *
         * @return MAVLink value
         */
        int value() {
            return mValue;
        }

        /**
         * Retrieves a {@code Mode} from its value.
         *
         * @param value MAVLink value
         *
         * @return {@code Mode} matching value, or {@code null} if no one matches
         */
        @Nullable
        static Mode fromValue(int value) {
            return MAP.get(value);
        }

        /** Map of Modes, by their value. */
        private static final SparseArray<Mode> MAP;

        static {
            MAP = new SparseArray<>();
            for (Mode mode : values()) {
                MAP.put(mode.mValue, mode);
            }
        }
    }

    /** Still capture mode. */
    @NonNull
    private final Mode mMode;

    /** Time-lapse or GPS-lapse interval. */
    private final double mInterval;

    /**
     * Constructor.
     *
     * @param mode     still capture mode
     * @param interval time-lapse interval in seconds (if mode is {@link Mode#TIMELAPSE TIMELAPSE}), or GPS-lapse
     *                 interval in meters (if mode is {@link Mode#GPSLAPSE GPSLAPSE})
     */
    public SetStillCaptureModeCommand(@NonNull Mode mode, double interval) {
        super(Type.SET_STILL_CAPTURE_MODE);
        mMode = mode;
        mInterval = interval;
    }

    /**
     * Retrieves the still capture mode.
     *
     * @return still capture mode
     */
    @NonNull
    public Mode getMode() {
        return mMode;
    }

    /**
     * Retrieves the time-lapse interval in seconds (if mode is {@link Mode#TIMELAPSE TIMELAPSE}), or GPS-lapse interval
     * in meters (if mode is {@link Mode#GPSLAPSE GPSLAPSE}).
     *
     * @return interval
     */
    public double getInterval() {
        return mInterval;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mMode.value(), mInterval, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a set still capture mode command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @Nullable
    static SetStillCaptureModeCommand create(@NonNull double[] parameters) {
        Mode mode = Mode.fromValue((int) parameters[0]);
        if (mode != null) {
            return new SetStillCaptureModeCommand(mode, parameters[1]);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SetStillCaptureModeCommand that = (SetStillCaptureModeCommand) o;

        if (Double.compare(that.mInterval, mInterval) != 0) return false;
        return mMode == that.mMode;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mMode.hashCode();
        temp = Double.doubleToLongBits(mInterval);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
