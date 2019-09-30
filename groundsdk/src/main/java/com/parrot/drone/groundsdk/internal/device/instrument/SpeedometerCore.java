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

package com.parrot.drone.groundsdk.internal.device.instrument;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.instrument.Speedometer;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleCore;
import com.parrot.drone.groundsdk.value.OptionalDouble;

/** Core class for the Speedometer. */
public final class SpeedometerCore extends SingletonComponentCore implements Speedometer {

    /** Description of Speedometer instrument. */
    private static final ComponentDescriptor<Instrument, Speedometer> DESC = ComponentDescriptor.of(Speedometer.class);

    /** Current overall speed of the drone, relative to the ground (in meters/second). */
    private double mGroundSpeed;

    /** Current drone speed along the north axis, relative to the ground (in meters/second). */
    private double mNorthSpeed;

    /** Current drone speed along the east axis, relative to the ground (in meters/second). */
    private double mEastSpeed;

    /** Current drone speed along the down axis, relative to the ground (in meters/second). */
    private double mDownSpeed;

    /** Current drone speed along its front axis, relative to the ground (in meters/second). */
    private double mForwardSpeed;

    /** Current drone speed along its right axis, relative to the ground (in meters/second). */
    private double mRightSpeed;

    /** Current overall speed of the drone, relative to the air (in meters/second). */
    @NonNull
    private final OptionalDoubleCore mAirSpeed;

    /**
     * Constructor.
     *
     * @param instrumentStore Store where this instrument belongs.
     */
    public SpeedometerCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mAirSpeed = new OptionalDoubleCore();
    }

    @Override
    public double getGroundSpeed() {
        return mGroundSpeed;
    }

    @Override
    public double getNorthSpeed() {
        return mNorthSpeed;
    }

    @Override
    public double getEastSpeed() {
        return mEastSpeed;
    }

    @Override
    public double getDownSpeed() {
        return mDownSpeed;
    }

    @Override
    public double getForwardSpeed() {
        return mForwardSpeed;
    }

    @Override
    public double getRightSpeed() {
        return mRightSpeed;
    }

    @Override
    @NonNull
    public OptionalDouble getAirSpeed() {
        return mAirSpeed;
    }

    /**
     * Updates the current overall ground speed.
     *
     * @param groundSpeed new overall ground speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateGroundSpeed(double groundSpeed) {
        if (Double.compare(mGroundSpeed, groundSpeed) != 0) {
            mChanged = true;
            mGroundSpeed = groundSpeed;
        }
        return this;
    }

    /**
     * Updates the current north speed.
     *
     * @param speed new north speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateNorthSpeed(double speed) {
        if (Double.compare(mNorthSpeed, speed) != 0) {
            mChanged = true;
            mNorthSpeed = speed;
        }
        return this;
    }

    /**
     * Updates the current east speed.
     *
     * @param speed new east speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateEastSpeed(double speed) {
        if (Double.compare(mEastSpeed, speed) != 0) {
            mChanged = true;
            mEastSpeed = speed;
        }
        return this;
    }

    /**
     * Updates the current down speed.
     *
     * @param speed new down speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateDownSpeed(double speed) {
        if (Double.compare(mDownSpeed, speed) != 0) {
            mChanged = true;
            mDownSpeed = speed;
        }
        return this;
    }

    /**
     * Updates the current forward speed.
     *
     * @param speed new forward speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateForwardSpeed(double speed) {
        if (Double.compare(mForwardSpeed, speed) != 0) {
            mChanged = true;
            mForwardSpeed = speed;
        }
        return this;
    }

    /**
     * Updates the current right speed.
     *
     * @param speed new right speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateRightSpeed(double speed) {
        if (Double.compare(mRightSpeed, speed) != 0) {
            mChanged = true;
            mRightSpeed = speed;
        }
        return this;
    }

    /**
     * Updates the current overall air speed.
     * <p>
     * Setting this value will also mark it as available in the API.
     *
     * @param airSpeed new overall air speed
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public SpeedometerCore updateAirSpeed(double airSpeed) {
        mChanged |= mAirSpeed.setValue(airSpeed);
        return this;
    }
}
