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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MAVLink command which allows to start photo capture.
 */
public final class StartPhotoCaptureCommand extends MavlinkCommand {

    /** Photo format. */
    public enum Format {

        /** Rectilinear projection (de-warped), JPEG format. */
        RECTILINEAR(12),

        /** Full sensor resolution (not de-warped), JPEG format. */
        FULL_FRAME(13),

        /** Full sensor resolution (not de-warped), JPEG and DNG format. */
        FULL_FRAME_DNG(14);

        /** MAVLink value. */
        private final int mValue;

        /**
         * Constructor.
         *
         * @param value MAVLink value
         */
        Format(int value) {
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
         * Retrieves a {@code Format} from its value.
         *
         * @param value MAVLink value
         *
         * @return {@code Format} matching value, or {@code null} if no one matches
         */
        @Nullable
        static Format fromValue(int value) {
            return MAP.get(value);
        }

        /** Map of Formats, by their value. */
        private static final SparseArray<Format> MAP;

        static {
            MAP = new SparseArray<>();
            for (Format format : values()) {
                MAP.put(format.mValue, format);
            }
        }

    }

    /** Elapsed time between two consecutive pictures. */
    private final double mInterval;

    /** Total number of photos to capture. */
    @IntRange(from = 0)
    private final int mCount;

    /** Capture format. */
    @NonNull
    private final Format mFormat;

    /**
     * Constructor.
     *
     * @param interval desired elapsed time between two consecutive pictures, in seconds; when interval is 0, the value
     *                 defined with {@link SetStillCaptureModeCommand} is used, else the value given here is used and
     *                 capture mode is set to {@link SetStillCaptureModeCommand.Mode#TIMELAPSE TIMELAPSE}
     * @param count    total number of photos to capture; 0 to capture until {@link StopPhotoCaptureCommand} is sent
     * @param format   capture format
     */
    public StartPhotoCaptureCommand(double interval, @IntRange(from = 0) int count, @NonNull Format format) {
        super(Type.START_PHOTO_CAPTURE);
        mInterval = interval;
        mCount = count;
        mFormat = format;
    }

    /**
     * Retrieves the elapsed time between two consecutive pictures, in seconds.
     * <p>
     * If interval is 0, the value defined with {@link SetStillCaptureModeCommand} is used instead, else the value
     * returned here is used and capture mode is set to {@link SetStillCaptureModeCommand.Mode#TIMELAPSE TIMELAPSE}.
     *
     * @return interval between two photos
     */
    public double getInterval() {
        return mInterval;
    }

    /**
     * Retrieves the total number of photos to capture.
     *
     * @return photo count
     */
    @IntRange(from = 0)
    public int getCount() {
        return mCount;
    }

    /**
     * Retrieves the photo capture format.
     *
     * @return photo format
     */
    @NonNull
    public Format getFormat() {
        return mFormat;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mInterval, mCount, mFormat.value(), 0, 0, 0, 0);
    }

    /**
     * Creates a start photo capture command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @Nullable
    static StartPhotoCaptureCommand create(@NonNull double[] parameters) {
        Format format = Format.fromValue((int) parameters[2]);
        if (format != null) {
            return new StartPhotoCaptureCommand(parameters[0], (int) parameters[1], format);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StartPhotoCaptureCommand that = (StartPhotoCaptureCommand) o;

        if (Double.compare(that.mInterval, mInterval) != 0) return false;
        if (mCount != that.mCount) return false;
        return mFormat == that.mFormat;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mInterval);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + mCount;
        result = 31 * result + mFormat.hashCode();
        return result;
    }
}
