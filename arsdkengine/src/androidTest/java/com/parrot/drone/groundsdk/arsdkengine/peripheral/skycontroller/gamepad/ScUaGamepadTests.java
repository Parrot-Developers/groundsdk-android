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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad;

import android.content.Intent;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.SkyControllerUaGamepad;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.MappingEntry;
import com.parrot.drone.groundsdk.internal.ApplicationNotifier;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMapper;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.parrot.drone.groundsdk.MapMatcher.mapHasSize;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsAvailable;
import static com.parrot.drone.groundsdk.ScUaMappingEntryMatcher.hasAction;
import static com.parrot.drone.groundsdk.ScUaMappingEntryMatcher.hasAxis;
import static com.parrot.drone.groundsdk.ScUaMappingEntryMatcher.hasButtons;
import static com.parrot.drone.groundsdk.ScUaMappingEntryMatcher.hasModel;
import static com.parrot.drone.groundsdk.ScUaMappingEntryMatcher.noButtons;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_0;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_1;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_2;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_3;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_4;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_AXIS_5;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_0;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_1;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_10;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_11;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_12;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_13;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_14;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_15;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_17;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_18;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_19;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_2;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_20;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_21;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_3;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_4;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_5;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_6;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_7;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_8;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.GamepadControllerBase.MASK_BUTTON_9;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ScUaGamepadTests extends ArsdkEngineTestBase {

    private static final long NAVIGATION_BUTTONS_MASK =
            MASK_BUTTON_4 | MASK_BUTTON_5 | MASK_BUTTON_6 | MASK_BUTTON_7 | MASK_BUTTON_2 | MASK_BUTTON_3;

    private static final long NAVIGATION_AXES_MASK = MASK_AXIS_0 | MASK_AXIS_1;

    private static final long ALL_BUTTONS_MASK =
            MASK_BUTTON_0 | MASK_BUTTON_1 | MASK_BUTTON_2 | MASK_BUTTON_3 | MASK_BUTTON_4 | MASK_BUTTON_5
            | MASK_BUTTON_6 | MASK_BUTTON_7 | MASK_BUTTON_8 | MASK_BUTTON_9 | MASK_BUTTON_10 | MASK_BUTTON_11
            | MASK_BUTTON_12 | MASK_BUTTON_13 | MASK_BUTTON_14 | MASK_BUTTON_15 | MASK_BUTTON_17 | MASK_BUTTON_18 
            | MASK_BUTTON_19 | MASK_BUTTON_20 | MASK_BUTTON_21;

    private static final long ALL_AXES_MASK =
            MASK_AXIS_0 | MASK_AXIS_1 | MASK_AXIS_2 | MASK_AXIS_3 | MASK_AXIS_4 | MASK_AXIS_5;

    private RemoteControlCore mRemoteControl;

    private VirtualGamepad mVirtualGamepad;

    private SkyControllerUaGamepad mSkyControllerUaGamepad;

    private int mVirtualGamepadChangeCnt;

    private int mSkyControllerUaGamepadChangeCnt;

    private final NavigationListener mNavigationListener = new NavigationListener();

    private final ButtonEventListener mButtonEventListener = new ButtonEventListener();

    private final AxisEventListener mAxisEventListener = new AxisEventListener();

    private final GamepadEventReceiver mGamepadEventReceiver = new GamepadEventReceiver();

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_UA.id(), "RC", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("456");
        assert mRemoteControl != null;

        mVirtualGamepad = mRemoteControl.getPeripheralStore().get(mMockSession, VirtualGamepad.class);
        mRemoteControl.getPeripheralStore().registerObserver(VirtualGamepad.class, () -> {
            mVirtualGamepad = mRemoteControl.getPeripheralStore().get(mMockSession, VirtualGamepad.class);
            mVirtualGamepadChangeCnt++;
        });

        mSkyControllerUaGamepad = mRemoteControl.getPeripheralStore().get(mMockSession, SkyControllerUaGamepad.class);
        mRemoteControl.getPeripheralStore().registerObserver(SkyControllerUaGamepad.class, () -> {
            mSkyControllerUaGamepad = mRemoteControl.getPeripheralStore().get(mMockSession, SkyControllerUaGamepad.class);
            mSkyControllerUaGamepadChangeCnt++;
        });

        mNavigationListener.reset();
        mButtonEventListener.reset();
        mAxisEventListener.reset();
        mGamepadEventReceiver.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mVirtualGamepad, nullValue());
        assertThat(mVirtualGamepadChangeCnt, is(0));

        assertThat(mSkyControllerUaGamepad, nullValue());
        assertThat(mSkyControllerUaGamepadChangeCnt, is(0));

        // both virtual gamepad and sc3 gamepad should be unavailable when the drone is connected
        connectRemoteControl(mRemoteControl, 1);

        assertThat(mVirtualGamepad, notNullValue());
        assertThat(mVirtualGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad, notNullValue());
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        disconnectRemoteControl(mRemoteControl, 1);

        assertThat(mVirtualGamepad, nullValue());
        assertThat(mVirtualGamepadChangeCnt, is(2));

        assertThat(mSkyControllerUaGamepad, nullValue());
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));
    }

    @Test
    public void testGrab() {
        connectRemoteControl(mRemoteControl, 1);

        // grab using virtual gamepad
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK), true));
        mVirtualGamepad.grab(mNavigationListener);
        mMockArsdkCore.assertNoExpectation();

        // mock grab state so that we can release afterwards
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK, 0));

        // release virtual gamepad
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperGrab(0, 0)));
        mVirtualGamepad.release();
        mMockArsdkCore.assertNoExpectation();

        // grab every input separately using sc3 gamepad
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_TOP_LEFT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_0, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_TOP_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_1, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_LEFT_1),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_18, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_LEFT_2),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_19, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_1),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_21, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_2),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_17, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_3),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_20, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.REAR_LEFT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_2, 0);
        testGrab(EnumSet.of(SkyControllerUaGamepad.Button.REAR_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), MASK_BUTTON_3, 0);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL), MASK_BUTTON_4 | MASK_BUTTON_5,
                MASK_AXIS_0);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL), MASK_BUTTON_6 | MASK_BUTTON_7,
                MASK_AXIS_1);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL), MASK_BUTTON_8 | MASK_BUTTON_9,
                MASK_AXIS_2);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL), MASK_BUTTON_10 | MASK_BUTTON_11,
                MASK_AXIS_3);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_SLIDER), MASK_BUTTON_12 | MASK_BUTTON_13, MASK_AXIS_4);
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_SLIDER), MASK_BUTTON_14 | MASK_BUTTON_15, MASK_AXIS_5);

        // grab all inputs together using sc3 gamepad
        testGrab(EnumSet.allOf(SkyControllerUaGamepad.Button.class),
                EnumSet.allOf(SkyControllerUaGamepad.Axis.class), ALL_BUTTONS_MASK, ALL_AXES_MASK);

        // mock grab state so that we can release afterwards
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(ALL_BUTTONS_MASK, ALL_AXES_MASK, 0));

        // release all sc3 gamepad inputs
        testGrab(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), 0, 0);
    }

    private void testGrab(@NonNull Set<SkyControllerUaGamepad.Button> buttons,
                          @NonNull Set<SkyControllerUaGamepad.Axis> axes, long buttonsMask, long axisMask) {
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(buttonsMask, axisMask), true));
        mSkyControllerUaGamepad.grabInputs(buttons, axes);
        mMockArsdkCore.assertNoExpectation();
    }

    @Test
    public void testNavigationEvents() {
        connectRemoteControl(mRemoteControl, 1);

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK), true));
        mVirtualGamepad.grab(mNavigationListener);

        // mock grab state so that we can receive events
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK, 0));

        // Test non-navigation buttons never trigger an event
        testNavigationEvents(null, MASK_BUTTON_0);
        testNavigationEvents(null, MASK_BUTTON_1);
        testNavigationEvents(null, MASK_BUTTON_8);
        testNavigationEvents(null, MASK_BUTTON_9);
        testNavigationEvents(null, MASK_BUTTON_10);
        testNavigationEvents(null, MASK_BUTTON_11);
        testNavigationEvents(null, MASK_BUTTON_12);
        testNavigationEvents(null, MASK_BUTTON_13);
        testNavigationEvents(null, MASK_BUTTON_14);
        testNavigationEvents(null, MASK_BUTTON_15);
        testNavigationEvents(null, MASK_BUTTON_17);
        testNavigationEvents(null, MASK_BUTTON_18);
        testNavigationEvents(null, MASK_BUTTON_19);
        testNavigationEvents(null, MASK_BUTTON_20);
        testNavigationEvents(null, MASK_BUTTON_21);

        // Test navigation buttons
        testNavigationEvents(VirtualGamepad.Event.CANCEL, MASK_BUTTON_2);
        testNavigationEvents(VirtualGamepad.Event.OK, MASK_BUTTON_3);
        testNavigationEvents(VirtualGamepad.Event.LEFT, MASK_BUTTON_4);
        testNavigationEvents(VirtualGamepad.Event.RIGHT, MASK_BUTTON_5);
        testNavigationEvents(VirtualGamepad.Event.UP, MASK_BUTTON_6);
        testNavigationEvents(VirtualGamepad.Event.DOWN, MASK_BUTTON_7);

        // release navigation
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperGrab(0, 0)));
        mVirtualGamepad.release();

        // send navigation buttons again, events should not be forwarded
        testNavigationEvents(null, MASK_BUTTON_2);
        testNavigationEvents(null, MASK_BUTTON_3);
        testNavigationEvents(null, MASK_BUTTON_4);
        testNavigationEvents(null, MASK_BUTTON_5);
        testNavigationEvents(null, MASK_BUTTON_6);
        testNavigationEvents(null, MASK_BUTTON_7);
    }

    private void testNavigationEvents(@Nullable VirtualGamepad.Event event, long buttonMask) {
        mNavigationListener.reset();
        long code = Long.numberOfTrailingZeros(buttonMask);

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabButtonEvent(code,
                ArsdkFeatureMapper.ButtonEvent.PRESS));
        assertThat(mNavigationListener.mCnt, is(event == null ? 0 : 1));
        assertThat(mNavigationListener.mEvent, is(event));
        assertThat(mNavigationListener.mState, is(event == null ? null : VirtualGamepad.Event.State.PRESSED));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabButtonEvent(code,
                ArsdkFeatureMapper.ButtonEvent.RELEASE));
        assertThat(mNavigationListener.mCnt, is(event == null ? 0 : 2));
        assertThat(mNavigationListener.mEvent, is(event));
        assertThat(mNavigationListener.mState, is(event == null ? null : VirtualGamepad.Event.State.RELEASED));
    }

    @Test
    public void testButtonEvents() {
        connectRemoteControl(mRemoteControl, 1);

        mSkyControllerUaGamepad.setButtonEventListener(mButtonEventListener);
        // mock grab state so we receive all button events
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(ALL_BUTTONS_MASK, ALL_AXES_MASK, 0));

        // ensure events are forwarded for all buttons
        testButtonEvents(MASK_BUTTON_0, ButtonEvent.FRONT_TOP_LEFT_BUTTON);
        testButtonEvents(MASK_BUTTON_1, ButtonEvent.FRONT_TOP_RIGHT_BUTTON);
        testButtonEvents(MASK_BUTTON_2, ButtonEvent.REAR_LEFT_BUTTON);
        testButtonEvents(MASK_BUTTON_3, ButtonEvent.REAR_RIGHT_BUTTON);
        testButtonEvents(MASK_BUTTON_4, ButtonEvent.LEFT_STICK_LEFT);
        testButtonEvents(MASK_BUTTON_5, ButtonEvent.LEFT_STICK_RIGHT);
        testButtonEvents(MASK_BUTTON_6, ButtonEvent.LEFT_STICK_UP);
        testButtonEvents(MASK_BUTTON_7, ButtonEvent.LEFT_STICK_DOWN);
        testButtonEvents(MASK_BUTTON_8, ButtonEvent.RIGHT_STICK_LEFT);
        testButtonEvents(MASK_BUTTON_9, ButtonEvent.RIGHT_STICK_RIGHT);
        testButtonEvents(MASK_BUTTON_10, ButtonEvent.RIGHT_STICK_UP);
        testButtonEvents(MASK_BUTTON_11, ButtonEvent.RIGHT_STICK_DOWN);
        testButtonEvents(MASK_BUTTON_12, ButtonEvent.LEFT_SLIDER_DOWN);
        testButtonEvents(MASK_BUTTON_13, ButtonEvent.LEFT_SLIDER_UP);
        testButtonEvents(MASK_BUTTON_14, ButtonEvent.RIGHT_SLIDER_UP);
        testButtonEvents(MASK_BUTTON_15, ButtonEvent.RIGHT_SLIDER_DOWN);
        testButtonEvents(MASK_BUTTON_17, ButtonEvent.FRONT_BOTTOM_RIGHT_2_BUTTON);
        testButtonEvents(MASK_BUTTON_18, ButtonEvent.FRONT_BOTTOM_LEFT_1_BUTTON);
        testButtonEvents(MASK_BUTTON_19, ButtonEvent.FRONT_BOTTOM_LEFT_2_BUTTON);
        testButtonEvents(MASK_BUTTON_20, ButtonEvent.FRONT_BOTTOM_RIGHT_3_BUTTON);
        testButtonEvents(MASK_BUTTON_21, ButtonEvent.FRONT_BOTTOM_RIGHT_1_BUTTON);

        // unregister listener
        mSkyControllerUaGamepad.setButtonEventListener(null);

        // ensure events are not forwarded anymore
        testButtonEvents(MASK_BUTTON_0, null);
    }

    private void testButtonEvents(long buttonMask, @Nullable ButtonEvent event) {
        mButtonEventListener.reset();
        long code = Long.numberOfTrailingZeros(buttonMask);

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabButtonEvent(code,
                ArsdkFeatureMapper.ButtonEvent.PRESS));
        assertThat(mButtonEventListener.mCnt, is(event == null ? 0 : 1));
        assertThat(mButtonEventListener.mEvent, is(event));
        assertThat(mButtonEventListener.mState, is(event == null ? null : ButtonEvent.State.PRESSED));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabButtonEvent(code,
                ArsdkFeatureMapper.ButtonEvent.RELEASE));
        assertThat(mButtonEventListener.mCnt, is(event == null ? 0 : 2));
        assertThat(mButtonEventListener.mEvent, is(event));
        assertThat(mButtonEventListener.mState, is(event == null ? null : ButtonEvent.State.RELEASED));
    }

    @Test
    public void testAxisEvents() {
        connectRemoteControl(mRemoteControl, 1);

        mSkyControllerUaGamepad.setAxisEventListener(mAxisEventListener);

        // ensure events are forwarded for all axes
        testAxisEvents(MASK_AXIS_0, AxisEvent.LEFT_STICK_HORIZONTAL);
        testAxisEvents(MASK_AXIS_1, AxisEvent.LEFT_STICK_VERTICAL);
        testAxisEvents(MASK_AXIS_2, AxisEvent.RIGHT_STICK_HORIZONTAL);
        testAxisEvents(MASK_AXIS_3, AxisEvent.RIGHT_STICK_VERTICAL);
        testAxisEvents(MASK_AXIS_4, AxisEvent.LEFT_SLIDER);
        testAxisEvents(MASK_AXIS_5, AxisEvent.RIGHT_SLIDER);

        // unregister listener
        mSkyControllerUaGamepad.setAxisEventListener(null);

        // ensure events are not forwarded anymore
        testAxisEvents(MASK_AXIS_0, null);
    }

    private void testAxisEvents(long axisMask, @Nullable AxisEvent event) {
        mAxisEventListener.reset();
        long code = Long.numberOfTrailingZeros(axisMask);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabAxisEvent(code, 42));
        assertThat(mAxisEventListener.mCnt, is(event == null ? 0 : 1));
        assertThat(mAxisEventListener.mEvent, is(event));
        assertThat(mAxisEventListener.mValue, is(event == null ? 0 : 42));
    }

    @Test
    public void testAppButtonEvents() {
        ApplicationNotifier.setInstance(mGamepadEventReceiver);

        connectRemoteControl(mRemoteControl, 1);

        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_0, ButtonsMappableAction.APP_ACTION_SETTINGS);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_1, ButtonsMappableAction.APP_ACTION_1);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_2, ButtonsMappableAction.APP_ACTION_2);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_3, ButtonsMappableAction.APP_ACTION_3);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_4, ButtonsMappableAction.APP_ACTION_4);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_5, ButtonsMappableAction.APP_ACTION_5);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_6, ButtonsMappableAction.APP_ACTION_6);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_7, ButtonsMappableAction.APP_ACTION_7);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_8, ButtonsMappableAction.APP_ACTION_8);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_9, ButtonsMappableAction.APP_ACTION_9);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_10, ButtonsMappableAction.APP_ACTION_10);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_11, ButtonsMappableAction.APP_ACTION_11);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_12, ButtonsMappableAction.APP_ACTION_12);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_13, ButtonsMappableAction.APP_ACTION_13);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_14, ButtonsMappableAction.APP_ACTION_14);
        testAppButtonEvents(ArsdkFeatureMapper.ButtonAction.APP_15, ButtonsMappableAction.APP_ACTION_15);

        ApplicationNotifier.setInstance(null);
    }

    private void testAppButtonEvents(@NonNull ArsdkFeatureMapper.ButtonAction arsdkAction,
                                     @NonNull ButtonsMappableAction gsdkAction) {
        mGamepadEventReceiver.reset();
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperApplicationButtonEvent(arsdkAction));
        assertThat(mGamepadEventReceiver.mAction, is(gsdkAction));
    }

    @Test
    public void testNavigationGrabState() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mVirtualGamepadChangeCnt, is(1));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // order to grab so that virtual gamepad receives the state
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK), true));
        mVirtualGamepad.grab(mNavigationListener);

        // ensure grabbing a non navigation button does not grab navigation
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(MASK_BUTTON_0, 0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(1));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // ensure grabbing a non navigation axis does not grab navigation
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(0, MASK_AXIS_3, 0));
        assertThat(mVirtualGamepadChangeCnt, is(1));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // ensure grabbing any of the navigation buttons makes navigation grabbed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(MASK_BUTTON_4, 0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(2));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));

        // mock a release all
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(0, 0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(3));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // ensure grabbing any of the navigation axes makes navigation grabbed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(0, MASK_AXIS_0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(4));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));

        // mock a release all
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(0, 0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(5));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // ensure grabbing all of navigation buttons/axes makes navigation grabbed (which is the expected use case)
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK, 0));
        assertThat(mVirtualGamepadChangeCnt, is(6));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));

        // ensure we receive events for pressed buttons from grab state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK,
                        NAVIGATION_BUTTONS_MASK));
        assertThat(mVirtualGamepadChangeCnt, is(6));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));
        assertThat(mNavigationListener.mAllEvents, containsInAnyOrder(VirtualGamepad.Event.OK,
                VirtualGamepad.Event.CANCEL, VirtualGamepad.Event.LEFT, VirtualGamepad.Event.RIGHT,
                VirtualGamepad.Event.UP, VirtualGamepad.Event.DOWN));
        assertThat(mNavigationListener.mAllStates, contains(VirtualGamepad.Event.State.PRESSED,
                VirtualGamepad.Event.State.PRESSED, VirtualGamepad.Event.State.PRESSED,
                VirtualGamepad.Event.State.PRESSED, VirtualGamepad.Event.State.PRESSED,
                VirtualGamepad.Event.State.PRESSED));
    }

    @Test
    public void testInputsGrabState() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), empty());
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), empty());
        assertThat(mSkyControllerUaGamepad.getGrabbedButtonsState().keySet(), empty());

        // test each input separately
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_TOP_LEFT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_TOP_LEFT_BUTTON), MASK_BUTTON_0, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_TOP_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_TOP_RIGHT_BUTTON), MASK_BUTTON_1, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_LEFT_1),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_BOTTOM_LEFT_1_BUTTON), MASK_BUTTON_18, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_LEFT_2),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_BOTTOM_LEFT_2_BUTTON), MASK_BUTTON_19, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_1),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_BOTTOM_RIGHT_1_BUTTON), MASK_BUTTON_21, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_2),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_BOTTOM_RIGHT_2_BUTTON), MASK_BUTTON_17, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_BOTTOM_RIGHT_3),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.FRONT_BOTTOM_RIGHT_3_BUTTON), MASK_BUTTON_20, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.REAR_LEFT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.REAR_LEFT_BUTTON), MASK_BUTTON_2, 0);
        testInputsGrabState(EnumSet.of(SkyControllerUaGamepad.Button.REAR_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class),
                EnumSet.of(ButtonEvent.REAR_RIGHT_BUTTON), MASK_BUTTON_3, 0);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL),
                EnumSet.of(ButtonEvent.LEFT_STICK_LEFT, ButtonEvent.LEFT_STICK_RIGHT),
                MASK_BUTTON_4 | MASK_BUTTON_5, MASK_AXIS_0);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL),
                EnumSet.of(ButtonEvent.LEFT_STICK_UP, ButtonEvent.LEFT_STICK_DOWN),
                MASK_BUTTON_6 | MASK_BUTTON_7, MASK_AXIS_1);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL),
                EnumSet.of(ButtonEvent.RIGHT_STICK_LEFT, ButtonEvent.RIGHT_STICK_RIGHT),
                MASK_BUTTON_8 | MASK_BUTTON_9, MASK_AXIS_2);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL),
                EnumSet.of(ButtonEvent.RIGHT_STICK_UP, ButtonEvent.RIGHT_STICK_DOWN),
                MASK_BUTTON_10 | MASK_BUTTON_11, MASK_AXIS_3);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_SLIDER),
                EnumSet.of(ButtonEvent.LEFT_SLIDER_DOWN, ButtonEvent.LEFT_SLIDER_UP),
                MASK_BUTTON_12 | MASK_BUTTON_13, MASK_AXIS_4);
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.RIGHT_SLIDER),
                EnumSet.of(ButtonEvent.RIGHT_SLIDER_UP, ButtonEvent.RIGHT_SLIDER_DOWN),
                MASK_BUTTON_14 | MASK_BUTTON_15, MASK_AXIS_5);

        // test all inputs simultaneously
        testInputsGrabState(EnumSet.allOf(SkyControllerUaGamepad.Button.class),
                EnumSet.allOf(SkyControllerUaGamepad.Axis.class), EnumSet.allOf(ButtonEvent.class),
                ALL_BUTTONS_MASK, ALL_AXES_MASK);

        // test no inputs (release all)
        testInputsGrabState(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class), EnumSet.noneOf(ButtonEvent.class), 0, 0);
    }

    private void testInputsGrabState(@NonNull Set<SkyControllerUaGamepad.Button> buttons,
                                     @NonNull Set<SkyControllerUaGamepad.Axis> axes,
                                     @NonNull Set<ButtonEvent> events, long buttonsMask, long axesMask) {
        int changeCnt = mSkyControllerUaGamepadChangeCnt;

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(buttonsMask, axesMask, 0));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(changeCnt + 1));
        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), is(buttons));
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), is(axes));
        assertThat(mSkyControllerUaGamepad.getGrabbedButtonsState().keySet(), is(events));
        assertThat(mSkyControllerUaGamepad.getGrabbedButtonsState(),
                both(not(hasValue(ButtonEvent.State.PRESSED))).and(not(nullValue())));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(buttonsMask, axesMask, buttonsMask));
        if (buttonsMask != 0 || axesMask != 0) {
            // if nothing is grabbed, press state is not forwarded, so we won't get a change, skip test.
            assertThat(mSkyControllerUaGamepadChangeCnt, is(changeCnt + 2));
        }
        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), is(buttons));
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), is(axes));
        assertThat(mSkyControllerUaGamepad.getGrabbedButtonsState().keySet(), is(events));
        assertThat(mSkyControllerUaGamepad.getGrabbedButtonsState(),
                both(not(hasValue(ButtonEvent.State.RELEASED))).and(not(nullValue())));
    }

    @Test
    public void testPreemption() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mVirtualGamepadChangeCnt, is(1));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // virtual gamepad should be released
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));
        // no input should be grabbed on sc3 gamepad
        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), empty());
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), empty());

        mSkyControllerUaGamepad.setButtonEventListener(mButtonEventListener);

        // grab navigation using virtual gamepad
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK), true));
        mVirtualGamepad.grab(mNavigationListener);
        mMockArsdkCore.assertNoExpectation();

        // mock grab state acknowledge
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK, 0));
        assertThat(mVirtualGamepadChangeCnt, is(2));

        // virtual gamepad should be grabbed
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));
        // no input should be grabbed on sc3 gamepad
        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), empty());
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), empty());

        // ensure we receive navigation events
        testNavigationEvents(VirtualGamepad.Event.LEFT, MASK_BUTTON_4);
        // ensure we don't receive non-navigation events
        testNavigationEvents(null, MASK_BUTTON_0);

        // grab some input with sc3 gamepad
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperGrab(MASK_BUTTON_1, 0)));
        mSkyControllerUaGamepad.grabInputs(EnumSet.of(SkyControllerUaGamepad.Button.FRONT_TOP_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class));
        mMockArsdkCore.assertNoExpectation();
        // mock grab state acknowledge
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(MASK_BUTTON_1, 0, 0));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));

        // virtual gamepad should be preempted
        assertThat(mVirtualGamepadChangeCnt, is(3));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.PREEMPTED));

        // ensure virtual gamepad does not receive any event
        testNavigationEvents(null, MASK_BUTTON_4);
        testNavigationEvents(null, MASK_BUTTON_1); // count +2 on mSkyControllerUaGamepadChangeCnt
        testNavigationEvents(null, MASK_BUTTON_0);

        // ensure sc3 gamepad receives grabbed input events
        testButtonEvents(MASK_BUTTON_1, ButtonEvent.FRONT_TOP_RIGHT_BUTTON); // count +2 on mSkyControllerUaGamepadChangeCnt

        // release grabbed input from sc3 gamepad (we expect a grab request back on navigation masks)
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK), true));
        mSkyControllerUaGamepad.grabInputs(EnumSet.noneOf(
                SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class));
        mMockArsdkCore.assertNoExpectation();

        // mock grab state acknowledge
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(NAVIGATION_BUTTONS_MASK, NAVIGATION_AXES_MASK, 0));
        assertThat(mVirtualGamepadChangeCnt, is(4));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(7));

        // virtual gamepad should be grabbed again
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));

        // ensure sc3 gamepad does not receives any event
        testButtonEvents(MASK_BUTTON_1, null);

        // ensure we receive navigation events
        testNavigationEvents(VirtualGamepad.Event.LEFT, MASK_BUTTON_4);
        // ensure we don't receive non-navigation events
        testNavigationEvents(null, MASK_BUTTON_0);

        // grab some input with sc3 gamepad again
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperGrab(MASK_BUTTON_3, 0)));
        mSkyControllerUaGamepad.grabInputs(EnumSet.of(SkyControllerUaGamepad.Button.REAR_RIGHT),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class));
        mMockArsdkCore.assertNoExpectation();
        // mock grab state acknowledge
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperGrabState(MASK_BUTTON_3, 0, 0));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(8));

        // should be preempted again
        assertThat(mVirtualGamepadChangeCnt, is(5));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.PREEMPTED));

        // then release virtual gamepad (we don't expect any grab command to be sent since it is preempted)
        mVirtualGamepad.release();

        // virtual gamepad should be released
        assertThat(mVirtualGamepadChangeCnt, is(6));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));
        // however its internal preemption status shall remain, so that we cannot grab
        assertThat(mVirtualGamepad.canGrab(), is(false));

        // ensure sc3 gamepad still receives grabbed input events
        testButtonEvents(MASK_BUTTON_3, ButtonEvent.REAR_RIGHT_BUTTON); // count +2 on mSkyControllerUaGamepadChangeCnt

        // ensure virtual gamepad does not receive any event
        testNavigationEvents(null, MASK_BUTTON_4);
        testNavigationEvents(null, MASK_BUTTON_3); // count +2 on mSkyControllerUaGamepadChangeCnt
        testNavigationEvents(null, MASK_BUTTON_0);

        // release grabbed input from sc3 gamepad (we expect a grab release since virtual gamepad is also released)
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.mapperGrab(0, 0), true));
        mSkyControllerUaGamepad.grabInputs(EnumSet.noneOf(
                SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class));
        mMockArsdkCore.assertNoExpectation();

        // mock grab state acknowledge
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeMapperGrabState(0, 0, 0));
        assertThat(mVirtualGamepadChangeCnt, is(7));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(13));

        // virtual gamepad should still be released
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));
        // but can be grabbed again now
        assertThat(mVirtualGamepad.canGrab(), is(true));
        // sc3 gamepad inputs should be released too
        assertThat(mSkyControllerUaGamepad.getGrabbedButtons(), empty());
        assertThat(mSkyControllerUaGamepad.getGrabbedAxes(), empty());

        // finally ensure neither virtual nor sc3 gamepad receive events
        testNavigationEvents(null, MASK_BUTTON_4);
        testNavigationEvents(null, MASK_BUTTON_3);
        testNavigationEvents(null, MASK_BUTTON_0);
        testButtonEvents(MASK_BUTTON_4, null);
        testButtonEvents(MASK_BUTTON_3, null);
        testButtonEvents(MASK_BUTTON_0, null);
    }

    @Test
    public void testButtonsMappingsList() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), nullValue());

        setSupportedDroneModels(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL);

        // add first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(1,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_DEC,
                MASK_BUTTON_2, ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // should not be notified until 'last'
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add item (neither first, nor last)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(2,
                Drone.Model.ANAFI_THERMAL.id(), ArsdkFeatureMapper.ButtonAction.APP_3,
                MASK_BUTTON_0 | MASK_BUTTON_1, ArsdkFeatureGeneric.ListFlags.toBitField()));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add last
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(3,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.VIDEO_RECORD,
                ALL_BUTTONS_MASK, ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(ButtonsMappableAction.DECREASE_CAMERA_EXPOSITION),
                        hasButtons(ButtonEvent.REAR_LEFT_BUTTON)),
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(ButtonsMappableAction.RECORD_VIDEO),
                        hasButtons(ButtonEvent.values()))));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), contains(
                allOf(hasModel(Drone.Model.ANAFI_THERMAL), hasAction(ButtonsMappableAction.APP_ACTION_3),
                        hasButtons(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.FRONT_TOP_RIGHT_BUTTON))));

        // remove
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(2, 0,
                ArsdkFeatureMapper.ButtonAction.APP_0, 0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.REMOVE, ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(3));
        // mapping should be removed
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());
        // other mappings should still be there
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(ButtonsMappableAction.DECREASE_CAMERA_EXPOSITION),
                        hasButtons(ButtonEvent.REAR_LEFT_BUTTON)),
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(ButtonsMappableAction.RECORD_VIDEO),
                        hasButtons(ButtonEvent.values()))));

        // empty
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(0, 0,
                ArsdkFeatureMapper.ButtonAction.APP_0, 0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(4));
        // mappings should be empty
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), empty());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());

        //first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(4,
                Drone.Model.ANAFI_THERMAL.id(), ArsdkFeatureMapper.ButtonAction.EMERGENCY,
                MASK_BUTTON_8, ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(5,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.FLIP_BACK,
                MASK_BUTTON_14 | MASK_BUTTON_15,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(5));
        // only last mapping should be present
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(ButtonsMappableAction.FLIP_BACK),
                        hasButtons(ButtonEvent.RIGHT_SLIDER_UP, ButtonEvent.RIGHT_SLIDER_DOWN))));

        // test ButtonEvent values are mapped properly
        testButtonsMappingsList(MASK_BUTTON_0, ButtonEvent.FRONT_TOP_LEFT_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_1, ButtonEvent.FRONT_TOP_RIGHT_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_2, ButtonEvent.REAR_LEFT_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_3, ButtonEvent.REAR_RIGHT_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_4, ButtonEvent.LEFT_STICK_LEFT);
        testButtonsMappingsList(MASK_BUTTON_5, ButtonEvent.LEFT_STICK_RIGHT);
        testButtonsMappingsList(MASK_BUTTON_6, ButtonEvent.LEFT_STICK_UP);
        testButtonsMappingsList(MASK_BUTTON_7, ButtonEvent.LEFT_STICK_DOWN);
        testButtonsMappingsList(MASK_BUTTON_8, ButtonEvent.RIGHT_STICK_LEFT);
        testButtonsMappingsList(MASK_BUTTON_9, ButtonEvent.RIGHT_STICK_RIGHT);
        testButtonsMappingsList(MASK_BUTTON_10, ButtonEvent.RIGHT_STICK_UP);
        testButtonsMappingsList(MASK_BUTTON_11, ButtonEvent.RIGHT_STICK_DOWN);
        testButtonsMappingsList(MASK_BUTTON_12, ButtonEvent.LEFT_SLIDER_DOWN);
        testButtonsMappingsList(MASK_BUTTON_13, ButtonEvent.LEFT_SLIDER_UP);
        testButtonsMappingsList(MASK_BUTTON_14, ButtonEvent.RIGHT_SLIDER_UP);
        testButtonsMappingsList(MASK_BUTTON_15, ButtonEvent.RIGHT_SLIDER_DOWN);
        testButtonsMappingsList(MASK_BUTTON_17, ButtonEvent.FRONT_BOTTOM_RIGHT_2_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_18, ButtonEvent.FRONT_BOTTOM_LEFT_1_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_19, ButtonEvent.FRONT_BOTTOM_LEFT_2_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_20, ButtonEvent.FRONT_BOTTOM_RIGHT_3_BUTTON);
        testButtonsMappingsList(MASK_BUTTON_21, ButtonEvent.FRONT_BOTTOM_RIGHT_1_BUTTON);

        // test ButtonMappableAction values are mapped properly
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_0, ButtonsMappableAction.APP_ACTION_SETTINGS);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_1, ButtonsMappableAction.APP_ACTION_1);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_2, ButtonsMappableAction.APP_ACTION_2);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_3, ButtonsMappableAction.APP_ACTION_3);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_4, ButtonsMappableAction.APP_ACTION_4);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_5, ButtonsMappableAction.APP_ACTION_5);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_6, ButtonsMappableAction.APP_ACTION_6);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_7, ButtonsMappableAction.APP_ACTION_7);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_8, ButtonsMappableAction.APP_ACTION_8);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_9, ButtonsMappableAction.APP_ACTION_9);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_10, ButtonsMappableAction.APP_ACTION_10);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_11, ButtonsMappableAction.APP_ACTION_11);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_12, ButtonsMappableAction.APP_ACTION_12);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_13, ButtonsMappableAction.APP_ACTION_13);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_14, ButtonsMappableAction.APP_ACTION_14);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.APP_15, ButtonsMappableAction.APP_ACTION_15);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.RETURN_HOME, ButtonsMappableAction.RETURN_HOME);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.TAKEOFF_LAND, ButtonsMappableAction.TAKEOFF_OR_LAND);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.VIDEO_RECORD, ButtonsMappableAction.RECORD_VIDEO);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.TAKE_PICTURE, ButtonsMappableAction.TAKE_PICTURE);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_INC,
                ButtonsMappableAction.INCREASE_CAMERA_EXPOSITION);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_DEC,
                ButtonsMappableAction.DECREASE_CAMERA_EXPOSITION);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.FLIP_LEFT, ButtonsMappableAction.FLIP_LEFT);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.FLIP_RIGHT, ButtonsMappableAction.FLIP_RIGHT);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.FLIP_FRONT, ButtonsMappableAction.FLIP_FRONT);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.FLIP_BACK, ButtonsMappableAction.FLIP_BACK);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.EMERGENCY, ButtonsMappableAction.EMERGENCY_CUTOFF);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.CENTER_CAMERA, ButtonsMappableAction.CENTER_CAMERA);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.CYCLE_HUD, ButtonsMappableAction.CYCLE_HUD);
        testButtonsMappingsList(ArsdkFeatureMapper.ButtonAction.CAMERA_AUTO, ButtonsMappableAction.PHOTO_OR_VIDEO);
    }

    private void testButtonsMappingsList(long buttonMask, @NonNull ButtonEvent event) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(1,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.APP_0, buttonMask,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(hasButtons(event)));
    }

    private void testButtonsMappingsList(@NonNull ArsdkFeatureMapper.ButtonAction arsdkAction,
                                         @NonNull ButtonsMappableAction gsdkAction) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperButtonMappingItem(1,
                Drone.Model.ANAFI_4K.id(), arsdkAction, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(hasAction(gsdkAction)));
    }

    @Test
    public void testAxisMappingsList() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), nullValue());

        setSupportedDroneModels(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL);

        // add first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(1,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.ROLL,
                Long.numberOfTrailingZeros(MASK_AXIS_0), 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // should not be notified until 'last'
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add item (neither first, nor last)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(2,
                Drone.Model.ANAFI_THERMAL.id(), ArsdkFeatureMapper.AxisAction.CAMERA_PAN,
                Long.numberOfTrailingZeros(MASK_AXIS_2), MASK_BUTTON_0 | MASK_BUTTON_1,
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add last
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(3,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.CAMERA_TILT,
                Long.numberOfTrailingZeros(MASK_AXIS_3), ALL_BUTTONS_MASK,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));
        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K,
                Drone.Model.ANAFI_THERMAL));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(AxisMappableAction.CONTROL_ROLL),
                        hasAxis(AxisEvent.LEFT_STICK_HORIZONTAL), noButtons()),
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(AxisMappableAction.TILT_CAMERA),
                        hasAxis(AxisEvent.RIGHT_STICK_VERTICAL), hasButtons(ButtonEvent.values()))));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), contains(
                allOf(hasModel(Drone.Model.ANAFI_THERMAL), hasAction(AxisMappableAction.PAN_CAMERA),
                        hasAxis(AxisEvent.RIGHT_STICK_HORIZONTAL),
                        hasButtons(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.FRONT_TOP_RIGHT_BUTTON))));

        // remove
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(2, 0,
                ArsdkFeatureMapper.AxisAction.APP_0, 0, 0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.REMOVE, ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(3));
        // this should not alter the set of supported products
        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K,
                Drone.Model.ANAFI_THERMAL));
        // however mapping should be removed
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());
        // other mappings should still be there
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(AxisMappableAction.CONTROL_ROLL),
                        hasAxis(AxisEvent.LEFT_STICK_HORIZONTAL), noButtons()),
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(AxisMappableAction.TILT_CAMERA),
                        hasAxis(AxisEvent.RIGHT_STICK_VERTICAL), hasButtons(ButtonEvent.values()))));

        // empty
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(0, 0,
                ArsdkFeatureMapper.AxisAction.APP_0, 0, 0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(4));
        // this should not alter the set of supported products
        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K,
                Drone.Model.ANAFI_THERMAL));
        // however mappings should be empty
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), empty());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());

        //first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(4,
                Drone.Model.ANAFI_THERMAL.id(), ArsdkFeatureMapper.AxisAction.GAZ,
                Long.numberOfTrailingZeros(MASK_AXIS_4), MASK_BUTTON_2,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(5,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.YAW,
                Long.numberOfTrailingZeros(MASK_AXIS_0), MASK_BUTTON_0 | MASK_BUTTON_15,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(5));
        // this should not alter the set of supported products
        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K,
                Drone.Model.ANAFI_THERMAL));
        // however only last mapping should be present
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_THERMAL), empty());
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(
                allOf(hasModel(Drone.Model.ANAFI_4K), hasAction(AxisMappableAction.CONTROL_YAW_ROTATION_SPEED),
                        hasAxis(AxisEvent.LEFT_STICK_HORIZONTAL),
                        hasButtons(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.RIGHT_SLIDER_DOWN))));

        // test AxisEvent values are mapped properly
        testAxisMappingsList(MASK_AXIS_0, AxisEvent.LEFT_STICK_HORIZONTAL);
        testAxisMappingsList(MASK_AXIS_1, AxisEvent.LEFT_STICK_VERTICAL);
        testAxisMappingsList(MASK_AXIS_2, AxisEvent.RIGHT_STICK_HORIZONTAL);
        testAxisMappingsList(MASK_AXIS_3, AxisEvent.RIGHT_STICK_VERTICAL);
        testAxisMappingsList(MASK_AXIS_4, AxisEvent.LEFT_SLIDER);
        testAxisMappingsList(MASK_AXIS_5, AxisEvent.RIGHT_SLIDER);

        // test AxisMappableAction values are mapped properly
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.ROLL, AxisMappableAction.CONTROL_ROLL);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.PITCH, AxisMappableAction.CONTROL_PITCH);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.YAW, AxisMappableAction.CONTROL_YAW_ROTATION_SPEED);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.GAZ, AxisMappableAction.CONTROL_THROTTLE);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.CAMERA_PAN, AxisMappableAction.PAN_CAMERA);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.CAMERA_TILT, AxisMappableAction.TILT_CAMERA);
        testAxisMappingsList(ArsdkFeatureMapper.AxisAction.CAMERA_ZOOM, AxisMappableAction.ZOOM_CAMERA);
    }

    private void testAxisMappingsList(long axisMask, @NonNull AxisEvent event) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(1,
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.ROLL,
                Long.numberOfTrailingZeros(axisMask), 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(hasAxis(event)));
    }

    private void testAxisMappingsList(@NonNull ArsdkFeatureMapper.AxisAction arsdkAction,
                                      @NonNull AxisMappableAction gsdkAction) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperAxisMappingItem(1,
                Drone.Model.ANAFI_4K.id(), arsdkAction, 0, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getMapping(Drone.Model.ANAFI_4K), contains(hasAction(gsdkAction)));
    }

    @Test
    public void testMapButtonsAction() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // models test
        testMapButtonsAction(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_4K.id());
        testMapButtonsAction(Drone.Model.ANAFI_THERMAL, Drone.Model.ANAFI_THERMAL.id());

        // Actions test
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_SETTINGS, ArsdkFeatureMapper.ButtonAction.APP_0);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_1, ArsdkFeatureMapper.ButtonAction.APP_1);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_2, ArsdkFeatureMapper.ButtonAction.APP_2);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_3, ArsdkFeatureMapper.ButtonAction.APP_3);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_4, ArsdkFeatureMapper.ButtonAction.APP_4);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_5, ArsdkFeatureMapper.ButtonAction.APP_5);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_6, ArsdkFeatureMapper.ButtonAction.APP_6);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_7, ArsdkFeatureMapper.ButtonAction.APP_7);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_8, ArsdkFeatureMapper.ButtonAction.APP_8);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_9, ArsdkFeatureMapper.ButtonAction.APP_9);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_10, ArsdkFeatureMapper.ButtonAction.APP_10);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_11, ArsdkFeatureMapper.ButtonAction.APP_11);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_12, ArsdkFeatureMapper.ButtonAction.APP_12);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_13, ArsdkFeatureMapper.ButtonAction.APP_13);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_14, ArsdkFeatureMapper.ButtonAction.APP_14);
        testMapButtonsAction(ButtonsMappableAction.APP_ACTION_15, ArsdkFeatureMapper.ButtonAction.APP_15);
        testMapButtonsAction(ButtonsMappableAction.RETURN_HOME, ArsdkFeatureMapper.ButtonAction.RETURN_HOME);
        testMapButtonsAction(ButtonsMappableAction.TAKEOFF_OR_LAND, ArsdkFeatureMapper.ButtonAction.TAKEOFF_LAND);
        testMapButtonsAction(ButtonsMappableAction.RECORD_VIDEO, ArsdkFeatureMapper.ButtonAction.VIDEO_RECORD);
        testMapButtonsAction(ButtonsMappableAction.TAKE_PICTURE, ArsdkFeatureMapper.ButtonAction.TAKE_PICTURE);
        testMapButtonsAction(ButtonsMappableAction.PHOTO_OR_VIDEO, ArsdkFeatureMapper.ButtonAction.CAMERA_AUTO);
        testMapButtonsAction(ButtonsMappableAction.CENTER_CAMERA, ArsdkFeatureMapper.ButtonAction.CENTER_CAMERA);
        testMapButtonsAction(ButtonsMappableAction.INCREASE_CAMERA_EXPOSITION,
                ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_INC);
        testMapButtonsAction(ButtonsMappableAction.DECREASE_CAMERA_EXPOSITION,
                ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_DEC);
        testMapButtonsAction(ButtonsMappableAction.FLIP_LEFT, ArsdkFeatureMapper.ButtonAction.FLIP_LEFT);
        testMapButtonsAction(ButtonsMappableAction.FLIP_RIGHT, ArsdkFeatureMapper.ButtonAction.FLIP_RIGHT);
        testMapButtonsAction(ButtonsMappableAction.FLIP_FRONT, ArsdkFeatureMapper.ButtonAction.FLIP_FRONT);
        testMapButtonsAction(ButtonsMappableAction.FLIP_BACK, ArsdkFeatureMapper.ButtonAction.FLIP_BACK);
        testMapButtonsAction(ButtonsMappableAction.EMERGENCY_CUTOFF, ArsdkFeatureMapper.ButtonAction.EMERGENCY);
        testMapButtonsAction(ButtonsMappableAction.CYCLE_HUD, ArsdkFeatureMapper.ButtonAction.CYCLE_HUD);

        // ButtonEvent tests
        testMapButtonsAction(ButtonEvent.FRONT_TOP_LEFT_BUTTON, MASK_BUTTON_0);
        testMapButtonsAction(ButtonEvent.FRONT_TOP_RIGHT_BUTTON, MASK_BUTTON_1);
        testMapButtonsAction(ButtonEvent.REAR_LEFT_BUTTON, MASK_BUTTON_2);
        testMapButtonsAction(ButtonEvent.REAR_RIGHT_BUTTON, MASK_BUTTON_3);
        testMapButtonsAction(ButtonEvent.LEFT_STICK_LEFT, MASK_BUTTON_4);
        testMapButtonsAction(ButtonEvent.LEFT_STICK_RIGHT, MASK_BUTTON_5);
        testMapButtonsAction(ButtonEvent.LEFT_STICK_UP, MASK_BUTTON_6);
        testMapButtonsAction(ButtonEvent.LEFT_STICK_DOWN, MASK_BUTTON_7);
        testMapButtonsAction(ButtonEvent.RIGHT_STICK_LEFT, MASK_BUTTON_8);
        testMapButtonsAction(ButtonEvent.RIGHT_STICK_RIGHT, MASK_BUTTON_9);
        testMapButtonsAction(ButtonEvent.RIGHT_STICK_UP, MASK_BUTTON_10);
        testMapButtonsAction(ButtonEvent.RIGHT_STICK_DOWN, MASK_BUTTON_11);
        testMapButtonsAction(ButtonEvent.LEFT_SLIDER_UP, MASK_BUTTON_13);
        testMapButtonsAction(ButtonEvent.LEFT_SLIDER_DOWN, MASK_BUTTON_12);
        testMapButtonsAction(ButtonEvent.RIGHT_SLIDER_UP, MASK_BUTTON_14);
        testMapButtonsAction(ButtonEvent.RIGHT_SLIDER_DOWN, MASK_BUTTON_15);
        testMapButtonsAction(ButtonEvent.FRONT_BOTTOM_RIGHT_2_BUTTON, MASK_BUTTON_17);
        testMapButtonsAction(ButtonEvent.FRONT_BOTTOM_LEFT_1_BUTTON, MASK_BUTTON_18);
        testMapButtonsAction(ButtonEvent.FRONT_BOTTOM_LEFT_2_BUTTON, MASK_BUTTON_19);
        testMapButtonsAction(ButtonEvent.FRONT_BOTTOM_RIGHT_3_BUTTON, MASK_BUTTON_20);
        testMapButtonsAction(ButtonEvent.FRONT_BOTTOM_RIGHT_1_BUTTON, MASK_BUTTON_21);

        // test unregister
        MappingEntry entry = new ButtonsMappingEntry(Drone.Model.ANAFI_4K, ButtonsMappableAction.APP_ACTION_SETTINGS,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapButtonAction(
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.APP_0, 0), true));
        mSkyControllerUaGamepad.unregisterMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapButtonsAction(@NonNull Drone.Model model, int productId) {
        MappingEntry entry = new ButtonsMappingEntry(model, ButtonsMappableAction.APP_ACTION_SETTINGS,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapButtonAction(productId,
                ArsdkFeatureMapper.ButtonAction.APP_0, ALL_BUTTONS_MASK), true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapButtonsAction(@NonNull ButtonsMappableAction gsdkAction,
                                      @NonNull ArsdkFeatureMapper.ButtonAction arsdkAction) {
        MappingEntry entry = new ButtonsMappingEntry(Drone.Model.ANAFI_4K, gsdkAction,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapButtonAction(
                Drone.Model.ANAFI_4K.id(), arsdkAction, ALL_BUTTONS_MASK), true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapButtonsAction(@NonNull ButtonEvent event, long buttonMask) {
        MappingEntry entry = new ButtonsMappingEntry(Drone.Model.ANAFI_4K, ButtonsMappableAction.APP_ACTION_SETTINGS,
                EnumSet.of(event));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapButtonAction(
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.ButtonAction.APP_0, buttonMask), true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    @Test
    public void testMapAxisAction() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // models test
        testMapAxisAction(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_4K.id());
        testMapAxisAction(Drone.Model.ANAFI_THERMAL, Drone.Model.ANAFI_THERMAL.id());

        // Actions test
        testMapAxisAction(AxisMappableAction.CONTROL_ROLL, ArsdkFeatureMapper.AxisAction.ROLL);
        testMapAxisAction(AxisMappableAction.CONTROL_PITCH, ArsdkFeatureMapper.AxisAction.PITCH);
        testMapAxisAction(AxisMappableAction.CONTROL_YAW_ROTATION_SPEED, ArsdkFeatureMapper.AxisAction.YAW);
        testMapAxisAction(AxisMappableAction.CONTROL_THROTTLE, ArsdkFeatureMapper.AxisAction.GAZ);
        testMapAxisAction(AxisMappableAction.PAN_CAMERA, ArsdkFeatureMapper.AxisAction.CAMERA_PAN);
        testMapAxisAction(AxisMappableAction.TILT_CAMERA, ArsdkFeatureMapper.AxisAction.CAMERA_TILT);
        testMapAxisAction(AxisMappableAction.ZOOM_CAMERA, ArsdkFeatureMapper.AxisAction.CAMERA_ZOOM);

        // AxisEvent tests
        testMapAxisAction(AxisEvent.LEFT_STICK_HORIZONTAL, MASK_AXIS_0);
        testMapAxisAction(AxisEvent.LEFT_STICK_VERTICAL, MASK_AXIS_1);
        testMapAxisAction(AxisEvent.RIGHT_STICK_HORIZONTAL, MASK_AXIS_2);
        testMapAxisAction(AxisEvent.RIGHT_STICK_VERTICAL, MASK_AXIS_3);
        testMapAxisAction(AxisEvent.LEFT_SLIDER, MASK_AXIS_4);
        testMapAxisAction(AxisEvent.RIGHT_SLIDER, MASK_AXIS_5);

        // ButtonEvent tests
        testMapAxisAction(ButtonEvent.FRONT_TOP_LEFT_BUTTON, MASK_BUTTON_0);
        testMapAxisAction(ButtonEvent.FRONT_TOP_RIGHT_BUTTON, MASK_BUTTON_1);
        testMapAxisAction(ButtonEvent.REAR_LEFT_BUTTON, MASK_BUTTON_2);
        testMapAxisAction(ButtonEvent.REAR_RIGHT_BUTTON, MASK_BUTTON_3);
        testMapAxisAction(ButtonEvent.LEFT_STICK_LEFT, MASK_BUTTON_4);
        testMapAxisAction(ButtonEvent.LEFT_STICK_RIGHT, MASK_BUTTON_5);
        testMapAxisAction(ButtonEvent.LEFT_STICK_UP, MASK_BUTTON_6);
        testMapAxisAction(ButtonEvent.LEFT_STICK_DOWN, MASK_BUTTON_7);
        testMapAxisAction(ButtonEvent.RIGHT_STICK_LEFT, MASK_BUTTON_8);
        testMapAxisAction(ButtonEvent.RIGHT_STICK_RIGHT, MASK_BUTTON_9);
        testMapAxisAction(ButtonEvent.RIGHT_STICK_UP, MASK_BUTTON_10);
        testMapAxisAction(ButtonEvent.RIGHT_STICK_DOWN, MASK_BUTTON_11);
        testMapAxisAction(ButtonEvent.LEFT_SLIDER_UP, MASK_BUTTON_13);
        testMapAxisAction(ButtonEvent.LEFT_SLIDER_DOWN, MASK_BUTTON_12);
        testMapAxisAction(ButtonEvent.RIGHT_SLIDER_UP, MASK_BUTTON_14);
        testMapAxisAction(ButtonEvent.RIGHT_SLIDER_DOWN, MASK_BUTTON_15);
        testMapAxisAction(ButtonEvent.FRONT_BOTTOM_RIGHT_2_BUTTON, MASK_BUTTON_17);
        testMapAxisAction(ButtonEvent.FRONT_BOTTOM_LEFT_1_BUTTON, MASK_BUTTON_18);
        testMapAxisAction(ButtonEvent.FRONT_BOTTOM_LEFT_2_BUTTON, MASK_BUTTON_19);
        testMapAxisAction(ButtonEvent.FRONT_BOTTOM_RIGHT_3_BUTTON, MASK_BUTTON_20);
        testMapAxisAction(ButtonEvent.FRONT_BOTTOM_RIGHT_1_BUTTON, MASK_BUTTON_21);

        // test unregister
        MappingEntry entry = new AxisMappingEntry(Drone.Model.ANAFI_4K, AxisMappableAction.PAN_CAMERA,
                AxisEvent.LEFT_SLIDER, EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapAxisAction(
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.CAMERA_PAN, -1, 0), true));
        mSkyControllerUaGamepad.unregisterMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapAxisAction(@NonNull Drone.Model model, int productId) {
        MappingEntry entry = new AxisMappingEntry(model, AxisMappableAction.PAN_CAMERA, AxisEvent.LEFT_SLIDER,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapAxisAction(productId,
                ArsdkFeatureMapper.AxisAction.CAMERA_PAN, Long.numberOfTrailingZeros(MASK_AXIS_4), ALL_BUTTONS_MASK),
                true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapAxisAction(@NonNull AxisMappableAction gsdkAction,
                                   @NonNull ArsdkFeatureMapper.AxisAction arsdkAction) {
        MappingEntry entry = new AxisMappingEntry(Drone.Model.ANAFI_4K, gsdkAction, AxisEvent.LEFT_SLIDER,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapAxisAction(
                Drone.Model.ANAFI_4K.id(), arsdkAction, Long.numberOfTrailingZeros(MASK_AXIS_4),
                ALL_BUTTONS_MASK), true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapAxisAction(@NonNull AxisEvent event, long axisMask) {
        MappingEntry entry = new AxisMappingEntry(Drone.Model.ANAFI_4K, AxisMappableAction.PAN_CAMERA, event,
                EnumSet.allOf(ButtonEvent.class));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapAxisAction(
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.CAMERA_PAN,
                Long.numberOfTrailingZeros(axisMask), ALL_BUTTONS_MASK),
                true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testMapAxisAction(@NonNull ButtonEvent event, long buttonMask) {
        MappingEntry entry = new AxisMappingEntry(Drone.Model.ANAFI_4K, AxisMappableAction.PAN_CAMERA,
                AxisEvent.LEFT_SLIDER, EnumSet.of(event));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperMapAxisAction(
                Drone.Model.ANAFI_4K.id(), ArsdkFeatureMapper.AxisAction.CAMERA_PAN,
                Long.numberOfTrailingZeros(MASK_AXIS_4), buttonMask), true));
        mSkyControllerUaGamepad.registerMappingEntry(entry);
        mMockArsdkCore.assertNoExpectation();
    }

    @Test
    public void testResetMappings() {
        connectRemoteControl(mRemoteControl, 1);

        // reset all products mappings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperResetMapping(0)));
        mSkyControllerUaGamepad.resetAllDefaultMappings();
        mMockArsdkCore.assertNoExpectation();

        // reset one product mapping
        testResetMappings(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_4K.id());
        testResetMappings(Drone.Model.ANAFI_THERMAL, Drone.Model.ANAFI_THERMAL.id());
    }

    private void testResetMappings(@NonNull Drone.Model model, int productId) {
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperResetMapping(productId)));
        mSkyControllerUaGamepad.resetDefaultMappings(model);
        mMockArsdkCore.assertNoExpectation();
    }


    @Test
    public void testActiveProduct() {
        connectRemoteControl(mRemoteControl, 1);

        // ensure no active product until notified
        assertThat(mSkyControllerUaGamepad.getActiveDroneModel(), nullValue());

        // notify new active product
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperActiveProduct(
                Drone.Model.ANAFI_4K.id()));
        assertThat(mSkyControllerUaGamepad.getActiveDroneModel(), is(Drone.Model.ANAFI_4K));

        // notify yet another active product
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperActiveProduct(
                Drone.Model.ANAFI_THERMAL.id()));
        assertThat(mSkyControllerUaGamepad.getActiveDroneModel(), is(Drone.Model.ANAFI_THERMAL));
    }

    @Test
    public void testExpoMap() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), empty());

        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_THERMAL), nullValue());

        // add first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_0),
                ArsdkFeatureMapper.ExpoType.LINEAR,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // should not be notified until 'last'
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add item (neither first, nor last)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(2,
                Drone.Model.ANAFI_THERMAL.id(), Long.numberOfTrailingZeros(MASK_AXIS_1),
                ArsdkFeatureMapper.ExpoType.EXPO_0,
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add last
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(3,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_2),
                ArsdkFeatureMapper.ExpoType.EXPO_1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));

        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K,
                Drone.Model.ANAFI_THERMAL));

        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), allOf(
                hasEntry(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR),
                hasEntry(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL, AxisInterpolator.MEDIUM_EXPONENTIAL),
                mapHasSize(2)));
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_THERMAL), both(hasEntry(
                SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL, AxisInterpolator.LIGHT_EXPONENTIAL)).and(
                mapHasSize(1)));

        // remove
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(2, 0, 0,
                ArsdkFeatureMapper.ExpoType.EXPO_0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.REMOVE, ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(3));

        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K));

        // interpolator should be removed
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_THERMAL), nullValue());
        // other interpolators should still be there
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), allOf(
                hasEntry(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR),
                hasEntry(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL, AxisInterpolator.MEDIUM_EXPONENTIAL),
                mapHasSize(2)));

        // empty
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(0, 0, 0,
                ArsdkFeatureMapper.ExpoType.EXPO_0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(4));

        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), empty());

        // all interpolators should be removed
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_THERMAL), nullValue());

        //first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(4,
                Drone.Model.ANAFI_THERMAL.id(), Long.numberOfTrailingZeros(MASK_AXIS_3),
                ArsdkFeatureMapper.ExpoType.EXPO_2,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(5,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_4),
                ArsdkFeatureMapper.ExpoType.EXPO_4,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(5));

        assertThat(mSkyControllerUaGamepad.getSupportedDroneModels(), containsInAnyOrder(Drone.Model.ANAFI_4K));

        // only last interpolator should be present
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyControllerUaGamepad.Axis.LEFT_SLIDER, AxisInterpolator.STRONGEST_EXPONENTIAL)).and(mapHasSize(1)));

        // test axis values are mapped properly
        testExpoMap(MASK_AXIS_0, SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL);
        testExpoMap(MASK_AXIS_1, SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL);
        testExpoMap(MASK_AXIS_2, SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL);
        testExpoMap(MASK_AXIS_3, SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL);
        testExpoMap(MASK_AXIS_4, SkyControllerUaGamepad.Axis.LEFT_SLIDER);
        testExpoMap(MASK_AXIS_5, SkyControllerUaGamepad.Axis.RIGHT_SLIDER);

        // test AxisInterpolator values are mapped properly
        testExpoMap(ArsdkFeatureMapper.ExpoType.LINEAR, AxisInterpolator.LINEAR);
        testExpoMap(ArsdkFeatureMapper.ExpoType.EXPO_0, AxisInterpolator.LIGHT_EXPONENTIAL);
        testExpoMap(ArsdkFeatureMapper.ExpoType.EXPO_1, AxisInterpolator.MEDIUM_EXPONENTIAL);
        testExpoMap(ArsdkFeatureMapper.ExpoType.EXPO_2, AxisInterpolator.STRONG_EXPONENTIAL);
        testExpoMap(ArsdkFeatureMapper.ExpoType.EXPO_4, AxisInterpolator.STRONGEST_EXPONENTIAL);
    }

    private void testExpoMap(long axisMask, @NonNull SkyControllerUaGamepad.Axis axis) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask),
                ArsdkFeatureMapper.ExpoType.EXPO_0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                axis, AxisInterpolator.LIGHT_EXPONENTIAL)).and(mapHasSize(1)));
    }

    private void testExpoMap(@NonNull ArsdkFeatureMapper.ExpoType expoType, @NonNull AxisInterpolator interpolator) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_3), expoType,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL, interpolator)).and(mapHasSize(1)));
    }

    @Test
    public void testSetExpo() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        setSupportedDroneModels(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL);

        // models test
        testSetExpo(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_4K.id());
        testSetExpo(Drone.Model.ANAFI_THERMAL, Drone.Model.ANAFI_THERMAL.id());

        // axes test
        testSetExpo(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL,
                MASK_AXIS_0);
        testSetExpo(SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL,
                MASK_AXIS_1);
        testSetExpo(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL,
                MASK_AXIS_2);
        testSetExpo(SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL,
                MASK_AXIS_3);
        testSetExpo(SkyControllerUaGamepad.Axis.LEFT_SLIDER, MASK_AXIS_4);
        testSetExpo(SkyControllerUaGamepad.Axis.RIGHT_SLIDER, MASK_AXIS_5);

        // interpolators test
        testSetExpo(AxisInterpolator.LINEAR, ArsdkFeatureMapper.ExpoType.LINEAR);
        testSetExpo(AxisInterpolator.LIGHT_EXPONENTIAL, ArsdkFeatureMapper.ExpoType.EXPO_0);
        testSetExpo(AxisInterpolator.MEDIUM_EXPONENTIAL, ArsdkFeatureMapper.ExpoType.EXPO_1);
        testSetExpo(AxisInterpolator.STRONG_EXPONENTIAL, ArsdkFeatureMapper.ExpoType.EXPO_2);
        testSetExpo(AxisInterpolator.STRONGEST_EXPONENTIAL, ArsdkFeatureMapper.ExpoType.EXPO_4);
    }

    private void testSetExpo(@NonNull Drone.Model droneModel, int productId) {
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetExpo(productId,
                Long.numberOfTrailingZeros(MASK_AXIS_2), ArsdkFeatureMapper.ExpoType.EXPO_4),
                true));
        mSkyControllerUaGamepad.setAxisInterpolator(droneModel, SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL,
                AxisInterpolator.STRONGEST_EXPONENTIAL);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testSetExpo(@NonNull SkyControllerUaGamepad.Axis axis, long axisMask) {
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetExpo(
                Drone.Model.ANAFI_4K.id(),
                Long.numberOfTrailingZeros(axisMask), ArsdkFeatureMapper.ExpoType.EXPO_2),
                true));
        mSkyControllerUaGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K, axis, AxisInterpolator.STRONG_EXPONENTIAL);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testSetExpo(@NonNull AxisInterpolator interpolator, ArsdkFeatureMapper.ExpoType expoType) {
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetExpo(
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_3), expoType),
                true));
        mSkyControllerUaGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL, interpolator);
        mMockArsdkCore.assertNoExpectation();
    }

    @Test
    public void testInvertedMap() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_THERMAL), nullValue());

        // add first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_0), 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // should not be notified until 'last'
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add item (neither first, nor last)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(2,
                Drone.Model.ANAFI_THERMAL.id(), Long.numberOfTrailingZeros(MASK_AXIS_1), 0,
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // add last
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(3,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_2), 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));

        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), containsInAnyOrder(
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL,
                SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL));
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_THERMAL), empty());

        // remove
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(2, 0, 0, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.REMOVE, ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(3));
        // inverted axis should be removed
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_THERMAL), nullValue());
        // other inverted axes should still be there
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), containsInAnyOrder(
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL,
                SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL));

        // empty
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(0, 0, 0, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(4));
        // all inverted axes should be removed
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), nullValue());
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_THERMAL), nullValue());

        //first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(4,
                Drone.Model.ANAFI_THERMAL.id(), Long.numberOfTrailingZeros(MASK_AXIS_3), 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(5,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(MASK_AXIS_4), 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(5));
        // only last inverted axis should be present
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_THERMAL), nullValue());
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), contains(
                SkyControllerUaGamepad.Axis.LEFT_SLIDER));

        // test axis values are mapped properly
        testInvertedMap(MASK_AXIS_0, SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL);
        testInvertedMap(MASK_AXIS_1, SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL);
        testInvertedMap(MASK_AXIS_2, SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL);
        testInvertedMap(MASK_AXIS_3, SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL);
        testInvertedMap(MASK_AXIS_4, SkyControllerUaGamepad.Axis.LEFT_SLIDER);
        testInvertedMap(MASK_AXIS_5, SkyControllerUaGamepad.Axis.RIGHT_SLIDER);
    }

    private void testInvertedMap(long axisMask, @NonNull SkyControllerUaGamepad.Axis axis) {
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask), 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), contains(axis));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(1,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask), 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K), not(contains(axis)));
    }

    @Test
    public void testSetInverted() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));

        // populate all drone models, so that we can enter the setAxisInterpolator API
        setSupportedDroneModels(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL);

        // models test
        testSetInverted(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_4K.id());
        testSetInverted(Drone.Model.ANAFI_THERMAL, Drone.Model.ANAFI_THERMAL.id());

        // axes test (also tests inverted flag)
        testSetInverted(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL
                , MASK_AXIS_0);
        testSetInverted(SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL,
                MASK_AXIS_1);
        testSetInverted(SkyControllerUaGamepad.Axis.RIGHT_STICK_HORIZONTAL, MASK_AXIS_2);
        testSetInverted(SkyControllerUaGamepad.Axis.RIGHT_STICK_VERTICAL, MASK_AXIS_3);
        testSetInverted(SkyControllerUaGamepad.Axis.LEFT_SLIDER, MASK_AXIS_4);
        testSetInverted(SkyControllerUaGamepad.Axis.RIGHT_SLIDER, MASK_AXIS_5);
    }

    private void testSetInverted(@NonNull Drone.Model droneModel, int productId) {
        //noinspection ConstantConditions
        boolean reverse = !mSkyControllerUaGamepad.getReversedAxes(droneModel).contains(
                SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL);
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetInverted(productId,
                Long.numberOfTrailingZeros(MASK_AXIS_1), reverse ? 1 : 0), true));
        mSkyControllerUaGamepad.reverseAxis(droneModel, SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL);
        mMockArsdkCore.assertNoExpectation();
    }

    private void testSetInverted(@NonNull SkyControllerUaGamepad.Axis axis, long axisMask) {
        //noinspection ConstantConditions
        boolean reverse = !mSkyControllerUaGamepad.getReversedAxes(Drone.Model.ANAFI_4K).contains(axis);
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetInverted(
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask), reverse ? 1 : 0), true));
        mSkyControllerUaGamepad.reverseAxis(Drone.Model.ANAFI_4K, axis);

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(2,
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask), 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperSetInverted(
                Drone.Model.ANAFI_4K.id(), Long.numberOfTrailingZeros(axisMask), reverse ? 0 : 1), true));
        mSkyControllerUaGamepad.reverseAxis(Drone.Model.ANAFI_4K, axis);
    }

    @Test
    public void testActions() {
        // validates that all ButtonsMappableAction values have a mapping defined in Actions class
        for (ButtonsMappableAction action : ButtonsMappableAction.values()) {
            assertThat(GamepadControllerBase.Actions.convert(action), notNullValue());
        }

        // validates that all AxisMappableAction values have a mapping defined in Actions class
        for (AxisMappableAction action : AxisMappableAction.values()) {
            assertThat(GamepadControllerBase.Actions.convert(action), notNullValue());
        }
    }

    @Test
    public void testButtons() {
        // validates that all ButtonEvent values have a mapping defined in Buttons class
        for (ButtonEvent event : ButtonEvent.values()) {
            assertThat(ScUaGamepad.ButtonEvents.maskFrom(Collections.singleton(event)), not(0L));
        }
    }

    @Test
    public void testAxes() {
        // validates that all AxisEvent values have a mapping defined in Axes class
        for (AxisEvent event : AxisEvent.values()) {
            assertThat(ScUaGamepad.AxisEvents.maskFrom(event), not(0L));
        }
    }

    @Test
    public void testInputInfo() {
        // validates that all kyController3Gamepad.Button values have a mapping defined in InputInfo class
        for (SkyControllerUaGamepad.Button button : SkyControllerUaGamepad.Button.values()) {
            assertThat(ScUaGamepad.InputMasks.of(button), notNullValue());
        }

        // validates that all SkyControllerUaGamepad.Axis values have a mapping defined in InputInfo class
        for (SkyControllerUaGamepad.Axis axis : SkyControllerUaGamepad.Axis.values()) {
            ScUaGamepad.InputMasks maskInfo = ScUaGamepad.InputMasks.of(axis);
            assertThat(maskInfo, notNullValue());
            // also validate that each axis mask can be converted back to an axis
            assertThat(ScUaGamepad.InputMasks.axisFrom(maskInfo.mAxes), notNullValue());
        }
    }

    @Test
    public void testVolatileMapping() {
        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperVolatileMappingState(0)));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(1));
        assertThat(mSkyControllerUaGamepad.volatileMapping(), allOf(
                optionalSettingIsAvailable(),
                optionalBooleanSettingIsDisabled()));

        // enable volatile mapping from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperEnterVolatileMapping()));
        mSkyControllerUaGamepad.volatileMapping().setEnabled(true);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(2));
        assertThat(mSkyControllerUaGamepad.volatileMapping(), optionalBooleanSettingIsEnabling());

        // update from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperVolatileMappingState(1));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(3));
        assertThat(mSkyControllerUaGamepad.volatileMapping(), optionalBooleanSettingIsEnabled());

        // disable volatile mapping from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.mapperExitVolatileMapping()));
        mSkyControllerUaGamepad.volatileMapping().setEnabled(false);
        assertThat(mSkyControllerUaGamepadChangeCnt, is(4));
        assertThat(mSkyControllerUaGamepad.volatileMapping(), optionalBooleanSettingIsDisabling());

        // update from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperVolatileMappingState(0));
        assertThat(mSkyControllerUaGamepadChangeCnt, is(5));
        assertThat(mSkyControllerUaGamepad.volatileMapping(), optionalBooleanSettingIsDisabled());
    }

    private void setSupportedDroneModels(Drone.Model... models) {
        // axis interpolators key set serves as the list of supported drone models
        // also initialize the map of inverted axes so that we can use the public API
        int previousChangeCnt = mSkyControllerUaGamepadChangeCnt;
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(0, 0, 0,
                ArsdkFeatureMapper.ExpoType.EXPO_0, ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.EMPTY)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(0, 0, 0, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));
        int uid = 1;
        for (Drone.Model model : models) {
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperExpoMapItem(uid, model.id(),
                    Long.numberOfTrailingZeros(MASK_AXIS_0), ArsdkFeatureMapper.ExpoType.LINEAR,
                    ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMapperInvertedMapItem(uid, model.id(),
                    Long.numberOfTrailingZeros(MASK_AXIS_0), 0,
                    ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
            uid++;
        }
        mSkyControllerUaGamepadChangeCnt = previousChangeCnt;
    }

    private static final class ButtonEventListener implements ButtonEvent.Listener {

        ButtonEvent mEvent;

        ButtonEvent.State mState;

        int mCnt;

        void reset() {
            mEvent = null;
            mState = null;
            mCnt = 0;
        }

        @Override
        public void onButtonEvent(@NonNull ButtonEvent event, @NonNull ButtonEvent.State state) {
            mCnt++;
            mEvent = event;
            mState = state;
        }
    }

    private static final class AxisEventListener implements AxisEvent.Listener {

        AxisEvent mEvent;

        int mValue;

        int mCnt;

        void reset() {
            mEvent = null;
            mValue = 0;
            mCnt = 0;
        }

        @Override
        public void onAxisEvent(@NonNull AxisEvent event, @IntRange(from = -100, to = 100) int value) {
            mCnt++;
            mEvent = event;
            mValue = value;
        }
    }

    private static final class NavigationListener implements VirtualGamepad.Event.Listener {

        VirtualGamepad.Event mEvent;

        VirtualGamepad.Event.State mState;

        final List<VirtualGamepad.Event> mAllEvents = new ArrayList<>();

        final List<VirtualGamepad.Event.State> mAllStates = new ArrayList<>();

        int mCnt;

        void reset() {
            mEvent = null;
            mState = null;
            mCnt = 0;
            mAllEvents.clear();
            mAllStates.clear();
        }

        @Override
        public void onEvent(@NonNull VirtualGamepad.Event event, @NonNull VirtualGamepad.Event.State state) {
            mCnt++;
            mEvent = event;
            mState = state;
            mAllEvents.add(event);
            mAllStates.add(state);
        }
    }

    private static final class GamepadEventReceiver extends ApplicationNotifier {

        ButtonsMappableAction mAction;

        void reset() {
            mAction = null;
        }

        @Override
        public void broadcastIntent(@NonNull Intent intent) {
            assertThat(intent.getAction(), is(VirtualGamepad.ACTION_GAMEPAD_APP_EVENT));
            assertThat(intent.hasExtra(VirtualGamepad.EXTRA_GAMEPAD_APP_EVENT_ACTION), is(true));
            mAction = ButtonsMappableAction.values()[intent.getIntExtra(
                    VirtualGamepad.EXTRA_GAMEPAD_APP_EVENT_ACTION, -1)];
        }
    }
}
