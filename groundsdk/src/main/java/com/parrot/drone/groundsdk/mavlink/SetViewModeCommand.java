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
 * MAVLink command which allows to set the view mode.
 */
public final class SetViewModeCommand extends MavlinkCommand {

    /** View mode. */
    public enum Mode {

        /** Drone orientation is fixed between two waypoints. Orientation changes when the waypoint is reached. */
        ABSOLUTE(0),

        /** Drone orientation changes linearly between two waypoints. */
        CONTINUE(1),

        /** Drone orientation is given by a Region Of Interest. */
        ROI(2);

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

    /** View mode. */
    @NonNull
    private final Mode mMode;

    /** Index of the ROI, if mode is {@link Mode#ROI ROI}. */
    private final int mRoiIndex;

    /**
     * Constructor.
     *
     * @param mode     view mode
     * @param roiIndex index of the Region Of Interest if mode is {@link Mode#ROI ROI} (if index is invalid,
     *                 {@link Mode#ABSOLUTE ABSOLUTE} mode is used instead); value is ignored for any other mode
     */
    public SetViewModeCommand(@NonNull Mode mode, int roiIndex) {
        super(Type.SET_VIEW_MODE);
        mMode = mode;
        mRoiIndex = roiIndex;
    }

    /**
     * Retrieves the view mode.
     *
     * @return view mode
     */
    public Mode getMode() {
        return mMode;
    }

    /**
     * Retrieves the index of the Region Of Interest.
     * <p>
     * Value is meaningless if the {@link #getMode() view mode} is not {@link Mode#ROI ROI}.
     *
     * @return index of the ROI if mode is ROI
     */
    public int getRoiIndex() {
        return mRoiIndex;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mMode.value(), mRoiIndex, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a set view mode command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @Nullable
    static SetViewModeCommand create(@NonNull double[] parameters) {
        Mode mode = Mode.fromValue((int) parameters[0]);
        if (mode != null) {
            return new SetViewModeCommand(mode, (int) parameters[1]);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SetViewModeCommand that = (SetViewModeCommand) o;

        if (mRoiIndex != that.mRoiIndex) return false;
        return mMode == that.mMode;
    }

    @Override
    public int hashCode() {
        int result = mMode.hashCode();
        result = 31 * result + mRoiIndex;
        return result;
    }
}
