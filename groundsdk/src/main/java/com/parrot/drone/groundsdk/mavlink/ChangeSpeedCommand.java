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
 * MAVLink command which allows to change the drone speed.
 */
public final class ChangeSpeedCommand extends MavlinkCommand {

    /** Speed type. */
    public enum SpeedType {

        /** Air speed. */
        AIR_SPEED(0),

        /** Ground speed. */
        GROUND_SPEED(1);

        /** MAVLink value. */
        private final int mValue;

        /**
         * Constructor.
         *
         * @param value MAVLink value
         */
        SpeedType(int value) {
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
         * Retrieves a {@code SpeedType} from its value.
         *
         * @param value MAVLink value
         *
         * @return {@code SpeedType} matching value, or {@code null} if no one matches
         */
        @Nullable
        static SpeedType fromValue(int value) {
            return MAP.get(value);
        }

        /** Map of SpeedTypes, by their value. */
        private static final SparseArray<SpeedType> MAP;

        static {
            MAP = new SparseArray<>();
            for (SpeedType speedType : values()) {
                MAP.put(speedType.mValue, speedType);
            }
        }
    }

    /** Speed type. */
    @NonNull
    private final SpeedType mSpeedType;

    /** Speed. */
    private final double mSpeed;

    /**
     * Constructor.
     *
     * @param speedType speed type
     * @param speed     speed, in meters/second
     */
    public ChangeSpeedCommand(@NonNull SpeedType speedType, double speed) {
        super(Type.CHANGE_SPEED);
        mSpeedType = speedType;
        mSpeed = speed;
    }

    /**
     * Retrieves the speed type.
     *
     * @return speed type
     */
    @NonNull
    public SpeedType getSpeedType() {
        return mSpeedType;
    }

    /**
     * Retrieves the speed, in meters/second.
     *
     * @return speed
     */
    public double getSpeed() {
        return mSpeed;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mSpeedType.value(), mSpeed, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a change speed command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @Nullable
    static ChangeSpeedCommand create(@NonNull double[] parameters) {
        SpeedType speedType = SpeedType.fromValue((int) parameters[0]);
        if (speedType != null) {
            return new ChangeSpeedCommand(speedType, parameters[1]);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangeSpeedCommand that = (ChangeSpeedCommand) o;

        if (Double.compare(that.mSpeed, mSpeed) != 0) return false;
        return mSpeedType == that.mSpeedType;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mSpeedType.hashCode();
        temp = Double.doubleToLongBits(mSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
