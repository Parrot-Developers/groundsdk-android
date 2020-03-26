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

import java.io.IOException;
import java.io.Writer;

import androidx.annotation.NonNull;

/**
 * MAVLink command which allows to control the camera tilt.
 */
public final class MountControlCommand extends MavlinkCommand {

    /** Value always used for mount mode; set to MAV_MOUNT_MODE_MAVLINK_TARGETING. */
    private static final int MODE = 2;

    /** Camera tilt angle. */
    private final double mTiltAngle;

    /**
     * Constructor.
     *
     * @param tiltAngle the tilt angle value, in degrees
     */
    public MountControlCommand(double tiltAngle) {
        super(Type.MOUNT_CONTROL);
        mTiltAngle = tiltAngle;
    }

    /**
     * Retrieves the camera tilt angle, in degrees.
     *
     * @return tilt angle
     */
    public double getTiltAngle() {
        return mTiltAngle;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mTiltAngle, 0, 0, 0, 0, 0, MODE);
    }

    /**
     * Creates a mount control command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @NonNull
    static MountControlCommand create(@NonNull double[] parameters) {
        return new MountControlCommand(parameters[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MountControlCommand that = (MountControlCommand) o;

        return Double.compare(that.mTiltAngle, mTiltAngle) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(mTiltAngle);
        return (int) (temp ^ (temp >>> 32));
    }
}
