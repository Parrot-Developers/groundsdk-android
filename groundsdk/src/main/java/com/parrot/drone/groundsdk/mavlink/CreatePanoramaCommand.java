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
 * MAVLink command which allows to create a panorama.
 */
public final class CreatePanoramaCommand extends MavlinkCommand {

    /** Horizontal rotation angle. */
    private final double mHorizontalAngle;

    /** Horizontal rotation speed. */
    private final double mHorizontalSpeed;

    /** Vertical rotation angle. */
    private final double mVerticalAngle;

    /** Vertical rotation speed. */
    private final double mVerticalSpeed;

    /**
     * Constructor.
     *
     * @param horizontalAngle horizontal rotation angle, in degrees
     * @param horizontalSpeed horizontal rotation speed, in degrees/second
     * @param verticalAngle   vertical rotation angle, in degrees
     * @param verticalSpeed   vertical rotation speed, in degrees/second
     */
    public CreatePanoramaCommand(double horizontalAngle, double horizontalSpeed, double verticalAngle,
                                 double verticalSpeed) {
        super(Type.CREATE_PANORAMA);
        mHorizontalAngle = horizontalAngle;
        mHorizontalSpeed = horizontalSpeed;
        mVerticalAngle = verticalAngle;
        mVerticalSpeed = verticalSpeed;
    }

    /**
     * Retrieves the horizontal angle, in degrees.
     *
     * @return horizontal angle
     */
    public double getHorizontalAngle() {
        return mHorizontalAngle;
    }

    /**
     * Retrieves the horizontal speed, in degrees/second.
     *
     * @return horizontal speed
     */
    public double getHorizontalSpeed() {
        return mHorizontalSpeed;
    }

    /**
     * Retrieves the vertical angle, in degrees.
     *
     * @return vertical angle
     */
    public double getVerticalAngle() {
        return mVerticalAngle;
    }

    /**
     * Retrieves the vertical speed, in degrees/second.
     *
     * @return vertical speed
     */
    public double getVerticalSpeed() {
        return mVerticalSpeed;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mHorizontalAngle, mVerticalAngle, mHorizontalSpeed, mVerticalSpeed, 0, 0, 0);
    }

    /**
     * Generates a create panorama command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @NonNull
    static CreatePanoramaCommand create(@NonNull double[] parameters) {
        return new CreatePanoramaCommand(parameters[0], parameters[2], parameters[1], parameters[3]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreatePanoramaCommand that = (CreatePanoramaCommand) o;

        if (Double.compare(that.mHorizontalAngle, mHorizontalAngle) != 0) return false;
        if (Double.compare(that.mHorizontalSpeed, mHorizontalSpeed) != 0) return false;
        if (Double.compare(that.mVerticalAngle, mVerticalAngle) != 0) return false;
        return Double.compare(that.mVerticalSpeed, mVerticalSpeed) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mHorizontalAngle);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mHorizontalSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mVerticalAngle);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mVerticalSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
