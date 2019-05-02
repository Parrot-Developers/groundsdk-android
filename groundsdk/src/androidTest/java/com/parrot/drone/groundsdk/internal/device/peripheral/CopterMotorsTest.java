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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.CopterMotors;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.motor.MotorError;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CopterMotorsTest {

    private MockComponentStore<Peripheral> mStore;

    private CopterMotorsCore mCopterMotorsImpl;

    private CopterMotors mCopterMotors;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mCopterMotorsImpl = new CopterMotorsCore(mStore);
        mCopterMotors = mStore.get(CopterMotors.class);
        mStore.registerObserver(CopterMotors.class, () -> {
            mChangeCnt++;
            mCopterMotors = mStore.get(CopterMotors.class);
        });
        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mCopterMotors, nullValue());
        assertThat(mChangeCnt, is(0));

        mCopterMotorsImpl.publish();
        assertThat(mCopterMotors, is(mCopterMotorsImpl));
        assertThat(mChangeCnt, is(1));

        mCopterMotorsImpl.unpublish();
        assertThat(mCopterMotors, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testMotorsErrors() {
        mCopterMotorsImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.NONE));
        }

        // set same error (NONE), should not trigger a notify
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.NONE).notifyUpdated();
        assertThat(mChangeCnt, is(1));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.NONE));

        // set a current error
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.STALLED).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.STALLED));

        // change the current error
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.SECURITY_MODE).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        // set same error, should not trigger a notify
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.SECURITY_MODE).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        // set a different past error on same motor, should not change the error
        mCopterMotorsImpl.updateLatestError(CopterMotors.Motor.FRONT_LEFT, MotorError.EMERGENCY_STOP).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        // reset current error on the motor, motor should not be in error anymore, but past error should be kept.
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.NONE).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.EMERGENCY_STOP));

        // change the past error, should update accordingly
        mCopterMotorsImpl.updateLatestError(CopterMotors.Motor.FRONT_LEFT, MotorError.OTHER).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        // set other motors errors, check it does not impact the already set past error
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.REAR_LEFT, MotorError.STALLED)
                         .updateCurrentError(CopterMotors.Motor.REAR_RIGHT, MotorError.SECURITY_MODE).notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.REAR_LEFT,
                CopterMotors.Motor.REAR_RIGHT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.REAR_LEFT), is(MotorError.STALLED));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.REAR_RIGHT), is(MotorError.SECURITY_MODE));

        // update motors past errors, check it only alter motors not currently in error
        mCopterMotorsImpl.updateLatestError(CopterMotors.Motor.FRONT_RIGHT, MotorError.SECURITY_MODE)
                         .updateLatestError(CopterMotors.Motor.FRONT_LEFT, MotorError.STALLED)
                         .updateLatestError(CopterMotors.Motor.REAR_RIGHT, MotorError.EMERGENCY_STOP).notifyUpdated();
        assertThat(mChangeCnt, is(7));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.REAR_LEFT,
                CopterMotors.Motor.REAR_RIGHT));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.STALLED));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_RIGHT), is(MotorError.SECURITY_MODE));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.REAR_LEFT), is(MotorError.STALLED));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.REAR_RIGHT), is(MotorError.SECURITY_MODE));

        // clear all current errors and past errors
        mCopterMotorsImpl.updateCurrentError(CopterMotors.Motor.FRONT_LEFT, MotorError.NONE)
                         .updateCurrentError(CopterMotors.Motor.FRONT_RIGHT, MotorError.NONE)
                         .updateCurrentError(CopterMotors.Motor.REAR_LEFT, MotorError.NONE)
                         .updateCurrentError(CopterMotors.Motor.REAR_RIGHT, MotorError.NONE).notifyUpdated();
        assertThat(mChangeCnt, is(8));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        mCopterMotorsImpl.updateLatestError(CopterMotors.Motor.FRONT_LEFT, MotorError.NONE)
                         .updateLatestError(CopterMotors.Motor.FRONT_RIGHT, MotorError.NONE)
                         .updateLatestError(CopterMotors.Motor.REAR_LEFT, MotorError.NONE)
                         .updateLatestError(CopterMotors.Motor.REAR_RIGHT, MotorError.NONE).notifyUpdated();
        assertThat(mChangeCnt, is(9));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.NONE));
        }
    }
}
