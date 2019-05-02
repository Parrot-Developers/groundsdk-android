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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf.SmartTakeOffLandAction;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.DoubleRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsAvailable;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ManualCopterPilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private ManualCopterPilotingItfCore mPilotingItfImpl;

    private ManualCopterPilotingItf mPilotingItf;

    private Backend mBackend;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mPilotingItf = mStore.get(ManualCopterPilotingItf.class);
        mStore.registerObserver(ManualCopterPilotingItf.class, () -> {
            mPilotingItf = mStore.get(ManualCopterPilotingItf.class);
            mChangeCnt++;
        });
        mBackend = new Backend();
        mPilotingItfImpl = new ManualCopterPilotingItfCore(mStore, mBackend);
    }

    @After
    public void tearDown() {
        mPilotingItfImpl = null;
        mBackend = null;
        mStore = null;
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingItf, nullValue());

        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf, is(mPilotingItfImpl));

        mPilotingItfImpl.unpublish();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf, nullValue());
    }

    @Test
    public void testTakeOff() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // take off
        mPilotingItf.takeOff();
        assertThat(mBackend.mTakeOffSent, is(true));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));
        // land
        mPilotingItf.land();
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(true));
    }

    @Test
    public void testThrownTakeOff() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // take off
        mPilotingItf.thrownTakeOff();
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(true));
        assertThat(mBackend.mLandSent, is(false));
        // land
        mPilotingItf.land();
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(true));
    }

    @Test
    public void testLand() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // land
        mPilotingItf.land();
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(true));
    }

    @Test
    public void testThrownTakeOffSetting() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mSentThrownTakeOff, is(false));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // change setting, should do nothing since unavailable
        mPilotingItf.getThrownTakeOffMode().toggle();
        assertThat(mBackend.mSentThrownTakeOff, is(false));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // mock backend notifies setting supported
        mPilotingItfImpl.getThrownTakeOffMode().updateSupportedFlag(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting
        mPilotingItf.getThrownTakeOffMode().toggle();
        assertThat(mBackend.mSentThrownTakeOff, is(true));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mPilotingItfImpl.getThrownTakeOffMode().updateValue(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting again
        mPilotingItf.getThrownTakeOffMode().toggle();
        assertThat(mBackend.mSentThrownTakeOff, is(false));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mPilotingItfImpl.getThrownTakeOffMode().updateValue(false);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));
    }

    /**
     * Check action performed when {@link ManualCopterPilotingItf#smartTakeOffLand()} is called.
     *
     * @param action Action that should be performed by @link ManualCopterPilotingItf#smartTakeOffLand()}
     */
    private void checkSmartTakeOffLandAction(SmartTakeOffLandAction action) {
        boolean expectedTakeOffSent, expectedThrownTakeOffSent, expectedLandSent;
        switch (action) {
            case TAKE_OFF:
                expectedTakeOffSent = true;
                expectedLandSent = expectedThrownTakeOffSent = false;
                break;
            case THROWN_TAKE_OFF:
                expectedThrownTakeOffSent = true;
                expectedLandSent = expectedTakeOffSent = false;
                break;
            case LAND:
                expectedLandSent = true;
                expectedTakeOffSent = expectedThrownTakeOffSent = false;
                break;
            case NONE:
            default:
                expectedTakeOffSent = expectedThrownTakeOffSent = expectedLandSent = false;
                break;
        }
        assertThat(mPilotingItfImpl.getSmartTakeOffLandAction(), is(action));
        mPilotingItf.smartTakeOffLand();
        assertThat(mBackend.mTakeOffSent, is(expectedTakeOffSent));
        assertThat(mBackend.mThrownTakeOffSent, is(expectedThrownTakeOffSent));
        assertThat(mBackend.mLandSent, is(expectedLandSent));
    }

    /**
     * Test {@link ManualCopterPilotingItf#smartTakeOffLand()} when usage of thrown take-off is disabled.
     */
    @Test
    public void testSmartTakeOffLandWithoutThrownTakeOff() {
        mPilotingItfImpl.getThrownTakeOffMode().updateSupportedFlag(true).updateValue(false);
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getThrownTakeOffMode().isAvailable(), is(true));
        assertThat(mPilotingItf.getThrownTakeOffMode().isEnabled(), is(false));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // initial state, should do nothing
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can take off state and cannot land, should take off
        mPilotingItfImpl.updateCanTakeOff(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.TAKE_OFF);

        // cannot take off and can land, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);

        // cannot land and cannot take off, should do nothing
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(false).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can land state and can take off, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(true).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);
    }

    /**
     * Test {@link ManualCopterPilotingItf#smartTakeOffLand()} when usage of thrown take off is enabled
     * and when drone is not moving.
     */
    @Test
    public void testSmartTakeOffLandWithThrownTakeOffNotMoving() {
        mPilotingItfImpl.getThrownTakeOffMode().updateSupportedFlag(true).updateValue(true);
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getThrownTakeOffMode().isAvailable(), is(true));
        assertThat(mPilotingItf.getThrownTakeOffMode().isEnabled(), is(true));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // drone not moving
        mPilotingItfImpl.updateSmartWillDoThrownTakeOff(false);

        // initial state, should do nothing
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can take off state and cannot land, should take off
        mPilotingItfImpl.updateCanTakeOff(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.TAKE_OFF);

        // cannot take off and can land, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);

        // cannot land and cannot take off, should do nothing
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(false).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can land state and can take off, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(true).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);
    }

    /**
     * Test {@link ManualCopterPilotingItf#smartTakeOffLand()} when usage of thrown take-off is enabled
     * and when drone is moving.
     */
    @Test
    public void testSmartTakeOffLandWithThrownTakeOffMoving() {
        mPilotingItfImpl.getThrownTakeOffMode().updateSupportedFlag(true).updateValue(true);
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getThrownTakeOffMode().isAvailable(), is(true));
        assertThat(mPilotingItf.getThrownTakeOffMode().isEnabled(), is(true));
        assertThat(mBackend.mTakeOffSent, is(false));
        assertThat(mBackend.mThrownTakeOffSent, is(false));
        assertThat(mBackend.mLandSent, is(false));

        // drone moving
        mPilotingItfImpl.updateSmartWillDoThrownTakeOff(true);

        // initial state, should do nothing
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can take off state and cannot land, should do thrown take off
        mPilotingItfImpl.updateCanTakeOff(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.THROWN_TAKE_OFF);

        // cannot take off and can land, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);

        // cannot land and cannot take off, should do nothing
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(false).updateCanLand(false).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.NONE);

        // can land state and can take off, should land
        mBackend.mTakeOffSent = mBackend.mThrownTakeOffSent = mBackend.mLandSent = false;
        mPilotingItfImpl.updateCanTakeOff(true).updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        checkSmartTakeOffLandAction(SmartTakeOffLandAction.LAND);
    }

    @Test
    public void testCanTakeOffCanLand() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // test initial value
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(false));

        // change canLand
        mPilotingItfImpl.updateCanLand(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // change canTakeOff
        mPilotingItfImpl.updateCanTakeOff(true).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.canTakeOff(), is(true));
        assertThat(mPilotingItf.canLand(), is(true));
    }

    @Test
    public void testEmergency() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // test emergency
        mPilotingItf.emergencyCutOut();
        assertThat(mBackend.mEmergencyCutOutSent, is(true));
    }

    @Test
    public void testMaxPitchRoll() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // change setting from low-level (backend)
        mPilotingItfImpl.getMaxPitchRoll().updateBounds(DoubleRange.of(2, 15)).updateValue(10);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpToDateAt(2, 10, 15));

        // change setting from user side (sdk)
        mPilotingItf.getMaxPitchRoll().setValue(12);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentMaxPitchRoll, is(12.0));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpdatingTo(2, 12, 15));

        // mock update from low-level
        mPilotingItfImpl.getMaxPitchRoll().updateBounds(DoubleRange.of(2, 15)).updateValue(13);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpToDateAt(2, 13, 15));
    }

    @Test
    public void testMaxPitchRollVelocity() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // test initial value
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalSettingIsUnavailable());

        // change setting from low-level (backend)
        mPilotingItfImpl.getMaxPitchRollVelocity().updateBounds(DoubleRange.of(0.5, 3)).updateValue(1);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpToDateAt(0.5, 1, 3));

        // change setting from user side (sdk)
        mPilotingItf.getMaxPitchRollVelocity().setValue(1.5);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentMaxPitchRollVelocity, is(1.5));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpdatingTo(0.5, 1.5, 3));

        // mock update from low-level
        mPilotingItfImpl.getMaxPitchRollVelocity().updateBounds(DoubleRange.of(0.5, 3)).updateValue(1.6);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpToDateAt(0.5, 1.6, 3));
    }

    @Test
    public void testMaxVerticalSpeed() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // change setting from low-level (backend)
        mPilotingItfImpl.getMaxVerticalSpeed().updateBounds(DoubleRange.of(10, 100)).updateValue(10);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(10, 10, 100));

        // change setting from user side (sdk)
        mPilotingItf.getMaxVerticalSpeed().setValue(50);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentMaxVerticalSpeed, is(50.0));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpdatingTo(10, 50, 100));

        // mock update from low-level
        mPilotingItfImpl.getMaxVerticalSpeed().updateBounds(DoubleRange.of(10, 100)).updateValue(55);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(10, 55, 100));
    }

    @Test
    public void testMaxYawRotationSpeed() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // change setting from low-level (backend)
        mPilotingItfImpl.getMaxYawRotationSpeed().updateBounds(DoubleRange.of(0.1, 1)).updateValue(0.5);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(0.1, 0.5, 1));

        // change setting from user side (sdk)
        mPilotingItf.getMaxYawRotationSpeed().setValue(0.55);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentMaxYawSpeed, is(0.55));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpdatingTo(0.1, 0.55, 1));

        // mock update from low-level
        mPilotingItfImpl.getMaxYawRotationSpeed().updateBounds(DoubleRange.of(0.1, 1)).updateValue(0.6);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(0.1, 0.6, 1));
    }

    @Test
    public void testBankedTurnMode() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mSentBankedTurnMode, is(false));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // change setting, should do nothing since unavailable
        mPilotingItf.getBankedTurnMode().toggle();
        assertThat(mBackend.mSentBankedTurnMode, is(false));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // mock backend notifies setting supported
        mPilotingItfImpl.getBankedTurnMode().updateSupportedFlag(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting
        mPilotingItf.getBankedTurnMode().toggle();
        assertThat(mBackend.mSentBankedTurnMode, is(true));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mPilotingItfImpl.getBankedTurnMode().updateValue(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting again
        mPilotingItf.getBankedTurnMode().toggle();
        assertThat(mBackend.mSentBankedTurnMode, is(false));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mPilotingItfImpl.getBankedTurnMode().updateValue(false);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));
    }

    @Test
    public void testNotifyWithoutChanges() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // test notify without changes
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));
    }

    @Test
    public void testNotifyWithOneChange() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        mPilotingItfImpl.getMaxYawRotationSpeed().updateBounds(DoubleRange.of(0.1, 1)).updateValue(0.5);
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(0.1, 0.5, 1));

        mPilotingItfImpl.getMaxVerticalSpeed().updateBounds(DoubleRange.of(10.0, 100.0)).updateValue(55.0);
        mPilotingItfImpl.getMaxYawRotationSpeed().updateBounds(DoubleRange.of(0.1, 1.0)).updateValue(0.5);
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(0.1, 0.5, 1.0));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(10.0, 55.0, 100.0));
    }

    @Test
    public void testSetYawPitchRollGaz() {
        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));

        // test initial values
        assertThat(mBackend.mPitch, is(0));
        assertThat(mBackend.mRoll, is(0));
        assertThat(mBackend.mYawRotationSpeed, is(0));
        assertThat(mBackend.mVerticalSpeed, is(0));

        // test set values
        mPilotingItf.setPitch(1);
        mPilotingItf.setRoll(2);
        mPilotingItf.setYawRotationSpeed(3);
        mPilotingItf.setVerticalSpeed(4);

        assertThat(mBackend.mPitch, is(1));
        assertThat(mBackend.mRoll, is(2));
        assertThat(mBackend.mYawRotationSpeed, is(3));
        assertThat(mBackend.mVerticalSpeed, is(4));

        // check upper bounds
        mPilotingItf.setPitch(101);
        mPilotingItf.setRoll(102);
        mPilotingItf.setYawRotationSpeed(103);
        mPilotingItf.setVerticalSpeed(104);

        assertThat(mBackend.mPitch, is(100));
        assertThat(mBackend.mRoll, is(100));
        assertThat(mBackend.mYawRotationSpeed, is(100));
        assertThat(mBackend.mVerticalSpeed, is(100));

        // check lower bounds
        mPilotingItf.setPitch(-101);
        mPilotingItf.setRoll(-102);
        mPilotingItf.setYawRotationSpeed(-103);
        mPilotingItf.setVerticalSpeed(-104);

        assertThat(mBackend.mPitch, is(-100));
        assertThat(mBackend.mRoll, is(-100));
        assertThat(mBackend.mYawRotationSpeed, is(-100));
        assertThat(mBackend.mVerticalSpeed, is(-100));

        // check hover
        assertThat(mBackend.mHover, is(false));
        mPilotingItf.hover();
        assertThat(mBackend.mHover, is(true));
    }

    @Test
    public void testCancelRollbacks() {
        mPilotingItfImpl.getBankedTurnMode()
                        .updateSupportedFlag(true)
                        .updateValue(false);
        mPilotingItfImpl.getThrownTakeOffMode()
                        .updateSupportedFlag(true)
                        .updateValue(false);
        mPilotingItfImpl.getMaxPitchRollVelocity()
                        .updateBounds(DoubleRange.of(0, 1))
                        .updateValue(0);
        mPilotingItfImpl.getMaxVerticalSpeed()
                        .updateBounds(DoubleRange.of(0, 1))
                        .updateValue(0);
        mPilotingItfImpl.getMaxPitchRoll()
                        .updateBounds(DoubleRange.of(0, 1))
                        .updateValue(0);
        mPilotingItfImpl.getMaxYawRotationSpeed()
                        .updateBounds(DoubleRange.of(0, 1))
                        .updateValue(0);
        mPilotingItfImpl.publish();

        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        // mock user changes settings
        mPilotingItf.getBankedTurnMode().setEnabled(true);
        mPilotingItf.getThrownTakeOffMode().setEnabled(true);
        mPilotingItf.getMaxPitchRollVelocity().setValue(1);
        mPilotingItf.getMaxVerticalSpeed().setValue(1);
        mPilotingItf.getMaxPitchRoll().setValue(1);
        mPilotingItf.getMaxYawRotationSpeed().setValue(1);

        // cancel all rollbacks
        mPilotingItfImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements ManualCopterPilotingItfCore.Backend {

        boolean mTakeOffSent;

        boolean mThrownTakeOffSent;

        boolean mLandSent;

        boolean mEmergencyCutOutSent;

        double mSentMaxPitchRoll;

        double mSentMaxPitchRollVelocity;

        double mSentMaxVerticalSpeed;

        double mSentMaxYawSpeed;

        boolean mSentBankedTurnMode;

        boolean mSentThrownTakeOff;

        int mPitch, mRoll, mYawRotationSpeed, mVerticalSpeed;

        boolean mHover;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public void takeOff() {
            mTakeOffSent = true;
        }

        @Override
        public void thrownTakeOff() {
            mThrownTakeOffSent = true;
        }

        @Override
        public void land() {
            mLandSent = true;
        }

        @Override
        public void emergencyCutOut() {
            mEmergencyCutOutSent = true;
        }

        @Override
        public boolean setMaxPitchRoll(double maxPitchRoll) {
            mSentMaxPitchRoll = maxPitchRoll;
            return true;
        }

        @Override
        public boolean setMaxPitchRollVelocity(double maxPitchRollVelocity) {
            mSentMaxPitchRollVelocity = maxPitchRollVelocity;
            return true;
        }

        @Override
        public boolean setMaxVerticalSpeed(double maxVerticalSpeed) {
            mSentMaxVerticalSpeed = maxVerticalSpeed;
            return true;
        }

        @Override
        public boolean setMaxYawRotationSpeed(double maxYawSpeed) {
            mSentMaxYawSpeed = maxYawSpeed;
            return true;
        }

        @Override
        public boolean setBankedTurnMode(boolean enable) {
            mSentBankedTurnMode = enable;
            return true;
        }

        @Override
        public boolean useThrownTakeOffForSmartTakeOff(boolean enable) {
            mSentThrownTakeOff = enable;
            return true;
        }

        @Override
        public void setPitch(int pitch) {
            mPitch = pitch;
        }

        @Override
        public void setRoll(int roll) {
            mRoll = roll;
        }

        @Override
        public void setYawRotationSpeed(int yawRotationSpeed) {
            mYawRotationSpeed = yawRotationSpeed;
        }

        @Override
        public void setVerticalSpeed(int verticalSpeed) {
            mVerticalSpeed = verticalSpeed;
        }

        @Override
        public void hover() {
            mHover = true;
        }
    }
}
