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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.SkyController3Gamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.MappingEntry;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.gamepad.SkyController3GamepadCore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.MapMatcher.mapHasSize;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsAvailable;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class SkyController3GamepadTest {

    private MockComponentStore<Peripheral> mStore;

    private SkyController3GamepadCore mSkyController3GamepadImpl;

    private SkyController3Gamepad mSkyController3Gamepad;

    private Backend mBackend;

    private ButtonEventListener mButtonEventListener;

    private AxisEventListener mAxisEventListener;

    private int mChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mButtonEventListener = new ButtonEventListener();
        mAxisEventListener = new AxisEventListener();
        mSkyController3GamepadImpl = new SkyController3GamepadCore(mStore, mBackend);
        mSkyController3Gamepad = mStore.get(SkyController3Gamepad.class);
        mStore.registerObserver(SkyController3Gamepad.class, () -> {
            mChangeCnt++;
            mSkyController3Gamepad = mStore.get(SkyController3Gamepad.class);
        });
        mChangeCnt = 0;
        mBackend.reset();
        mButtonEventListener.reset();
        mAxisEventListener.reset();
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mSkyController3Gamepad, nullValue());
        assertThat(mChangeCnt, is(0));

        mSkyController3GamepadImpl.publish();
        assertThat(mSkyController3Gamepad, is(mSkyController3GamepadImpl));
        assertThat(mChangeCnt, is(1));

        mSkyController3GamepadImpl.unpublish();
        assertThat(mSkyController3Gamepad, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testGrabInputs() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start, no input shall be grabbed
        assertThat(mSkyController3Gamepad.getGrabbedButtons(), empty());
        assertThat(mSkyController3Gamepad.getGrabbedAxes(), empty());

        // grab an input
        mSkyController3Gamepad.grabInputs(EnumSet.noneOf(SkyController3Gamepad.Button.class),
                EnumSet.of(SkyController3Gamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(1));
        assertThat(mBackend.mGrabbedButtons, empty());
        assertThat(mBackend.mGrabbedAxes, contains(SkyController3Gamepad.Axis.LEFT_SLIDER));

        // mock grabbed inputs update from low-level
        mSkyController3GamepadImpl.updateGrabbedInputs(
                EnumSet.noneOf(SkyController3Gamepad.Button.class),
                EnumSet.of(SkyController3Gamepad.Axis.LEFT_SLIDER)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getGrabbedButtons(), empty());
        assertThat(mSkyController3Gamepad.getGrabbedAxes(), contains(SkyController3Gamepad.Axis.LEFT_SLIDER));

        // grab same input, this should do nothing
        mSkyController3Gamepad.grabInputs(EnumSet.noneOf(SkyController3Gamepad.Button.class),
                EnumSet.of(SkyController3Gamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(1));

        // grab all inputs
        mSkyController3Gamepad.grabInputs(EnumSet.allOf(SkyController3Gamepad.Button.class),
                EnumSet.allOf(SkyController3Gamepad.Axis.class));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(2));
        assertThat(mBackend.mGrabbedButtons, containsInAnyOrder(SkyController3Gamepad.Button.values()));
        assertThat(mBackend.mGrabbedAxes, containsInAnyOrder(SkyController3Gamepad.Axis.values()));


        // mock grabbed inputs update from low-level
        mSkyController3GamepadImpl.updateGrabbedInputs(
                EnumSet.of(SkyController3Gamepad.Button.REAR_RIGHT_BUTTON),
                EnumSet.of(SkyController3Gamepad.Axis.LEFT_STICK_VERTICAL)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getGrabbedButtons(), containsInAnyOrder(
                SkyController3Gamepad.Button.REAR_RIGHT_BUTTON));
        assertThat(mSkyController3Gamepad.getGrabbedAxes(), containsInAnyOrder(
                SkyController3Gamepad.Axis.LEFT_STICK_VERTICAL));

        // release all inputs
        mSkyController3Gamepad.grabInputs(EnumSet.noneOf(SkyController3Gamepad.Button.class),
                EnumSet.noneOf(SkyController3Gamepad.Axis.class));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(3));
        assertThat(mBackend.mGrabbedButtons, empty());
        assertThat(mBackend.mGrabbedAxes, empty());

        // mock grabbed inputs update from low-level
        mSkyController3GamepadImpl.updateGrabbedInputs(
                EnumSet.noneOf(SkyController3Gamepad.Button.class),
                EnumSet.noneOf(SkyController3Gamepad.Axis.class)).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.getGrabbedButtons(), empty());
        assertThat(mSkyController3Gamepad.getGrabbedAxes(), empty());
    }

    @Test
    public void testGrabbedButtonEvents() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        mSkyController3Gamepad.setButtonEventListener(mButtonEventListener);

        // at start, no button event shall be grabbed
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), empty());

        EnumMap<ButtonEvent, ButtonEvent.State> grabState = new EnumMap<>(ButtonEvent.class);
        grabState.put(ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED);
        grabState.put(ButtonEvent.FRONT_BOTTOM_BUTTON, ButtonEvent.State.RELEASED);
        grabState.put(ButtonEvent.REAR_LEFT_BUTTON, ButtonEvent.State.RELEASED);
        grabState.put(ButtonEvent.REAR_RIGHT_BUTTON, ButtonEvent.State.PRESSED);

        // mock grab state update from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvents(grabState).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(4));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), allOf(
                hasEntry(ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED),
                hasEntry(ButtonEvent.FRONT_BOTTOM_BUTTON, ButtonEvent.State.RELEASED),
                hasEntry(ButtonEvent.REAR_LEFT_BUTTON, ButtonEvent.State.RELEASED),
                hasEntry(ButtonEvent.REAR_RIGHT_BUTTON, ButtonEvent.State.PRESSED)));

        // ensure listener gets called for pressed buttons
        assertThat(mButtonEventListener.mAllEvents, containsInAnyOrder(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.REAR_RIGHT_BUTTON));
        assertThat(mButtonEventListener.mAllStates, contains(
                ButtonEvent.State.PRESSED, ButtonEvent.State.PRESSED));

    }

    @Test
    public void testButtonEvents() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // ensure we don't receive events when no listener is set
        mSkyController3GamepadImpl.setButtonEventListener(null);

        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // set a listener
        mSkyController3GamepadImpl.setButtonEventListener(mButtonEventListener);

        // ensure we still don't receive any events for buttons that are not in the grabbed state
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // mock grab state update
        EnumMap<ButtonEvent, ButtonEvent.State> grabState = new EnumMap<>(ButtonEvent.class);
        grabState.put(ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.RELEASED);
        mSkyController3GamepadImpl.updateGrabbedButtonEvents(grabState).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.RELEASED));

        // ensure we did not receive an event for a released button state
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // notify button press from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure that the grab state changed
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED));

        // ensure that the event got forwarded to the listener
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.PRESSED));

        // repeat the exact same event
        mButtonEventListener.reset();
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure nothing changed
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED));

        // ensure no event got forwarded
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // notify button release from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.RELEASED).notifyUpdated();

        // ensure that the grab state changed
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.RELEASED));

        // ensure that the event got forwarded to the listener
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.RELEASED));

        // unregister listener
        mButtonEventListener.reset();
        mSkyController3Gamepad.setButtonEventListener(null);

        // notify button press from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        // ensure that the grab state changed
        assertThat(mChangeCnt, is(5));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mSkyController3Gamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED));

        // ensure listener was not called
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // put listener back
        mSkyController3Gamepad.setButtonEventListener(mButtonEventListener);

        // notify button release from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.RELEASED).notifyUpdated();

        // ensure listener is called
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.RELEASED));

        // unpublish
        mButtonEventListener.reset();
        mSkyController3GamepadImpl.unpublish();

        // notify button press from low level
        mSkyController3GamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure listener was not called
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());
    }

    @Test
    public void testAxisEvents() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // ensure we don't receive events when no listener is set
        mSkyController3GamepadImpl.setAxisEventListener(null);

        mSkyController3GamepadImpl.notifyAxisEvent(AxisEvent.LEFT_SLIDER, 42);
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));

        // set a listener
        mSkyController3GamepadImpl.setAxisEventListener(mAxisEventListener);

        // notify axis event from low level
        mSkyController3GamepadImpl.notifyAxisEvent(AxisEvent.LEFT_STICK_HORIZONTAL, 42);

        // ensure that the event got forwarded to the listener
        assertThat(mAxisEventListener.mEvent, is(AxisEvent.LEFT_STICK_HORIZONTAL));
        assertThat(mAxisEventListener.mValue, is(42));

        // unregister listener
        mAxisEventListener.reset();
        mSkyController3Gamepad.setAxisEventListener(null);

        // notify axis event from low level
        mSkyController3GamepadImpl.notifyAxisEvent(AxisEvent.LEFT_STICK_HORIZONTAL, 42);

        // ensure listener was not called
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));

        // put listener back
        mSkyController3Gamepad.setAxisEventListener(mAxisEventListener);

        // notify another axis event from low level
        mSkyController3GamepadImpl.notifyAxisEvent(AxisEvent.RIGHT_STICK_VERTICAL, -42);

        // ensure that the event got forwarded to the listener
        assertThat(mAxisEventListener.mEvent, is(AxisEvent.RIGHT_STICK_VERTICAL));
        assertThat(mAxisEventListener.mValue, is(-42));

        // unpublish
        mAxisEventListener.reset();
        mSkyController3GamepadImpl.unpublish();

        // notify axis event from low level
        mSkyController3GamepadImpl.notifyAxisEvent(AxisEvent.RIGHT_STICK_HORIZONTAL, 100);

        // ensure listener was not called
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));
    }

    @Test
    public void testMappings() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start, there should be no mappings
        assertThat(mSkyController3Gamepad.getMapping(Drone.Model.ANAFI_4K), nullValue());

        // register button mapping
        MappingEntry entry1 = new ButtonsMappingEntry(Drone.Model.ANAFI_4K, ButtonsMappableAction.FLIP_LEFT,
                EnumSet.of(ButtonEvent.LEFT_SLIDER_DOWN, ButtonEvent.LEFT_STICK_LEFT));

        mSkyController3Gamepad.registerMappingEntry(entry1);
        assertThat(mBackend.mSetupMappingEntryCnt, is(1));
        assertThat(mBackend.mMappingEntry, is(entry1));
        assertThat(mBackend.mRegister, is(true));

        // mock update from low-level
        mSkyController3GamepadImpl.updateSupportedDroneModels(EnumSet.of(Drone.Model.ANAFI_4K))
                                  .updateButtonsMappings(Collections.singleton(entry1)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getMapping(Drone.Model.ANAFI_4K), contains(entry1));

        // register axis mapping
        MappingEntry entry2 = new AxisMappingEntry(Drone.Model.ANAFI_4K, AxisMappableAction.PAN_CAMERA,
                AxisEvent.RIGHT_STICK_HORIZONTAL, EnumSet.of(ButtonEvent.RIGHT_SLIDER_UP));

        mSkyController3Gamepad.registerMappingEntry(entry2);
        assertThat(mBackend.mSetupMappingEntryCnt, is(2));
        assertThat(mBackend.mMappingEntry, is(entry2));
        assertThat(mBackend.mRegister, is(true));

        // mock update from low-level
        mSkyController3GamepadImpl.updateAxisMappings(Collections.singleton(entry2)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(entry1, entry2));

        // unregister button mapping
        mSkyController3Gamepad.unregisterMappingEntry(entry1);
        assertThat(mBackend.mSetupMappingEntryCnt, is(3));
        assertThat(mBackend.mMappingEntry, is(entry1));
        assertThat(mBackend.mRegister, is(false));

        // mock update from low-level
        mSkyController3GamepadImpl.updateButtonsMappings(Collections.emptySet()).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.getMapping(Drone.Model.ANAFI_4K), contains(entry2));

        // unregister axis mapping
        mSkyController3Gamepad.unregisterMappingEntry(entry2);
        assertThat(mBackend.mSetupMappingEntryCnt, is(4));
        assertThat(mBackend.mMappingEntry, is(entry2));
        assertThat(mBackend.mRegister, is(false));

        // mock update from low-level
        mSkyController3GamepadImpl.updateAxisMappings(Collections.emptySet()).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mSkyController3Gamepad.getMapping(Drone.Model.ANAFI_4K), empty());
    }

    @Test
    public void testResetMappings() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        mSkyController3Gamepad.resetDefaultMappings(Drone.Model.ANAFI_4K);
        assertThat(mBackend.mResetMappingsCnt, is(1));
        assertThat(mBackend.mResetMappingModel, is(Drone.Model.ANAFI_4K));

        mSkyController3Gamepad.resetAllDefaultMappings();
        assertThat(mBackend.mResetMappingsCnt, is(2));
        assertThat(mBackend.mResetMappingModel, nullValue());
    }

    @Test
    public void testAxisInterpolators() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at first, should return null since the list of supported drone models is not known
        assertThat(mSkyController3Gamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), nullValue());

        // adding an interpolator for an unsupported drone model should do nothing
        mSkyController3Gamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LIGHT_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(0));
        assertThat(mBackend.mAxisInterpolatorModel, nullValue());
        assertThat(mBackend.mAxisInterpolatorAxis, nullValue());
        assertThat(mBackend.mAxisInterpolator, nullValue());

        Set<SkyController3GamepadCore.AxisInterpolatorEntry> entries = new HashSet<>();
        entries.add(new SkyController3GamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR));
        // now declare that we support the drone model
        mSkyController3GamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        // this should trigger a change
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR)).and(mapHasSize(1)));

        // adding the exact same interpolator should do nothing
        mSkyController3Gamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(0));
        assertThat(mBackend.mAxisInterpolatorModel, nullValue());
        assertThat(mBackend.mAxisInterpolatorAxis, nullValue());
        assertThat(mBackend.mAxisInterpolator, nullValue());

        // set another interpolator
        mSkyController3Gamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(1));
        assertThat(mBackend.mAxisInterpolatorModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mAxisInterpolatorAxis, is(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mAxisInterpolator, is(AxisInterpolator.STRONGEST_EXPONENTIAL));

        // mock update from low-level
        entries.clear();
        entries.add(new SkyController3GamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL));
        mSkyController3GamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL)).and(
                mapHasSize(1)));

        // add an interpolator to a different axis
        mSkyController3Gamepad.setAxisInterpolator(Drone.Model.ANAFI_4K, SkyController3Gamepad.Axis.LEFT_SLIDER,
                AxisInterpolator.LIGHT_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(2));
        assertThat(mBackend.mAxisInterpolatorModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mAxisInterpolatorAxis, is(SkyController3Gamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mAxisInterpolator, is(AxisInterpolator.LIGHT_EXPONENTIAL));

        // mock update from low level
        entries.add(new SkyController3GamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_SLIDER, AxisInterpolator.LIGHT_EXPONENTIAL));
        mSkyController3GamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), allOf(
                hasEntry(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL),
                hasEntry(SkyController3Gamepad.Axis.LEFT_SLIDER, AxisInterpolator.LIGHT_EXPONENTIAL),
                mapHasSize(2)));
    }

    @Test
    public void testReversedAxes() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at first, should return null since the list of supported drone models is not known
        assertThat(mSkyController3Gamepad.getReversedAxes(Drone.Model.ANAFI_4K), nullValue());

        // adding a reversed axis for an unsupported drone model should do nothing
        mSkyController3Gamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(0));
        assertThat(mBackend.mReversedAxisModel, nullValue());
        assertThat(mBackend.mReversedAxis, nullValue());
        assertThat(mBackend.mReversed, is(false));

        Set<SkyController3GamepadCore.ReversedAxisEntry> entries = new HashSet<>();
        entries.add(new SkyController3GamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, false));
        // now declare that we support the drone model
        mSkyController3GamepadImpl.updateReversedAxes(entries).notifyUpdated();
        // this should trigger a change
        assertThat(mChangeCnt, is(2));
        // set should be empty (not null since the model is supported now)
        assertThat(mSkyController3Gamepad.getReversedAxes(Drone.Model.ANAFI_4K), empty());

        // reverse axis
        mSkyController3Gamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(1));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mReversed, is(true));

        // mock update from low-level
        entries.clear();
        entries.add(new SkyController3GamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, true));
        mSkyController3GamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getReversedAxes(Drone.Model.ANAFI_4K),
                contains(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL));

        // reverse same axis back to normal
        mSkyController3Gamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(2));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mReversed, is(false));

        // mock update from low level
        entries.clear();
        entries.add(new SkyController3GamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, false));
        mSkyController3GamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.getReversedAxes(Drone.Model.ANAFI_4K), empty());

        // reverse another axis
        mSkyController3Gamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyController3Gamepad.Axis.RIGHT_SLIDER);
        assertThat(mBackend.mSetReversedAxisCnt, is(3));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyController3Gamepad.Axis.RIGHT_SLIDER));
        assertThat(mBackend.mReversed, is(true));

        // mock update from low level
        entries.add(new SkyController3GamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyController3Gamepad.Axis.RIGHT_SLIDER, true));
        mSkyController3GamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mSkyController3Gamepad.getReversedAxes(Drone.Model.ANAFI_4K),
                contains(SkyController3Gamepad.Axis.RIGHT_SLIDER));
    }

    @Test
    public void testVolatileMapping() {
        mSkyController3GamepadImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mSkyController3Gamepad.volatileMapping(), allOf(
                optionalSettingIsUnavailable(),
                optionalBooleanSettingIsDisabled()));

        // mock update from low-level, volatile mapping is now supported
        mSkyController3GamepadImpl.volatileMapping().updateSupportedFlag(true);
        mSkyController3GamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.volatileMapping(), allOf(
                optionalSettingIsAvailable(),
                optionalBooleanSettingIsDisabled()));

        // enable volatile mapping
        mSkyController3Gamepad.volatileMapping().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsEnabling());
        assertThat(mBackend.mEnableVolatileMappingCnt, is(1));
        assertThat(mBackend.mVolatileMappingEnabled, is(true));

        // update from low-level
        mSkyController3GamepadImpl.volatileMapping().updateValue(true);
        mSkyController3GamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsEnabled());

        // disable volatile mapping
        mSkyController3Gamepad.volatileMapping().toggle();
        assertThat(mChangeCnt, is(5));
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsDisabling());
        assertThat(mBackend.mEnableVolatileMappingCnt, is(2));
        assertThat(mBackend.mVolatileMappingEnabled, is(false));

        // update from low-level
        mSkyController3GamepadImpl.volatileMapping().updateValue(false);
        mSkyController3GamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsDisabled());
    }

    @Test
    public void testActiveDroneModel() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start should be null -- no active model
        assertThat(mSkyController3Gamepad.getActiveDroneModel(), nullValue());

        // update from low level -- anafi 4k is active
        mSkyController3GamepadImpl.updateActiveDroneModel(Drone.Model.ANAFI_4K).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getActiveDroneModel(), is(Drone.Model.ANAFI_4K));
    }

    @Test
    public void testSupportedDroneModels() {
        mSkyController3GamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start should be empty -- no supported drones
        assertThat(mSkyController3Gamepad.getSupportedDroneModels(), empty());

        // update from low level -- declare anafi 4k support only
        mSkyController3GamepadImpl.updateSupportedDroneModels(EnumSet.of(Drone.Model.ANAFI_4K)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mSkyController3Gamepad.getSupportedDroneModels(), contains(Drone.Model.ANAFI_4K));

        // update from low level -- declare only anafi 4k / thermal support
        mSkyController3GamepadImpl.updateSupportedDroneModels(
                EnumSet.of(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mSkyController3Gamepad.getSupportedDroneModels(), containsInAnyOrder(
                Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL));
    }

    @Test
    public void testCancelRollbacks() {
        mSkyController3GamepadImpl.volatileMapping().updateSupportedFlag(true).updateValue(false);

        mSkyController3GamepadImpl.publish();

        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsDisabled());

        // mock user changes all settings
        mSkyController3Gamepad.volatileMapping().setEnabled(true);

        // cancel all rollbacks
        mSkyController3GamepadImpl.cancelSettingsRollbacks();

        // all setting should be updated to user values
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsEnabled());

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mSkyController3Gamepad.volatileMapping(), optionalBooleanSettingIsEnabled());
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements SkyController3GamepadCore.Backend {

        int mSetGrabbedInputsCnt;

        Set<SkyController3Gamepad.Button> mGrabbedButtons;

        Set<SkyController3Gamepad.Axis> mGrabbedAxes;

        int mResetMappingsCnt;

        Drone.Model mResetMappingModel;

        int mSetupMappingEntryCnt;

        MappingEntry mMappingEntry;

        boolean mRegister;

        int mSetAxisInterpolatorCnt;

        Drone.Model mAxisInterpolatorModel;

        SkyController3Gamepad.Axis mAxisInterpolatorAxis;

        AxisInterpolator mAxisInterpolator;

        int mSetReversedAxisCnt;

        Drone.Model mReversedAxisModel;

        SkyController3Gamepad.Axis mReversedAxis;

        boolean mReversed;

        int mEnableVolatileMappingCnt;

        boolean mVolatileMappingEnabled;

        void reset() {
            mSetGrabbedInputsCnt = mResetMappingsCnt = mSetupMappingEntryCnt = mSetAxisInterpolatorCnt = 0;
            mGrabbedButtons = null;
            mGrabbedAxes = null;
            mResetMappingModel = null;
            mMappingEntry = null;
            mRegister = false;
            mAxisInterpolatorModel = null;
            mAxisInterpolatorAxis = null;
            mAxisInterpolator = null;
        }

        @Override
        public void setupMappingEntry(@NonNull MappingEntry mappingEntry, boolean register) {
            mSetupMappingEntryCnt++;
            mMappingEntry = mappingEntry;
            mRegister = register;
        }

        @Override
        public void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull SkyController3Gamepad.Axis axis,
                                        @NonNull AxisInterpolator interpolator) {
            mSetAxisInterpolatorCnt++;
            mAxisInterpolatorModel = droneModel;
            mAxisInterpolatorAxis = axis;
            mAxisInterpolator = interpolator;
        }

        @Override
        public void setReversedAxis(@NonNull Drone.Model droneModel, @NonNull SkyController3Gamepad.Axis axis,
                                    boolean reversed) {
            mSetReversedAxisCnt++;
            mReversedAxisModel = droneModel;
            mReversedAxis = axis;
            mReversed = reversed;
        }

        @Override
        public void resetMappings(@Nullable Drone.Model model) {
            mResetMappingsCnt++;
            mResetMappingModel = model;
        }

        @Override
        public void setGrabbedInputs(@NonNull Set<SkyController3Gamepad.Button> buttons,
                                     @NonNull Set<SkyController3Gamepad.Axis> axes) {
            mSetGrabbedInputsCnt++;
            mGrabbedButtons = buttons;
            mGrabbedAxes = axes;
        }

        @Override
        public boolean setVolatileMapping(boolean enable) {
            mEnableVolatileMappingCnt++;
            mVolatileMappingEnabled = enable;
            return true;
        }
    }

    private static final class ButtonEventListener implements ButtonEvent.Listener {

        ButtonEvent mEvent;

        ButtonEvent.State mState;

        final List<ButtonEvent> mAllEvents = new ArrayList<>();

        final List<ButtonEvent.State> mAllStates = new ArrayList<>();

        void reset() {
            mEvent = null;
            mState = null;
            mAllEvents.clear();
            mAllStates.clear();
        }

        @Override
        public void onButtonEvent(@NonNull ButtonEvent event, @NonNull ButtonEvent.State state) {
            mEvent = event;
            mState = state;
            mAllEvents.add(event);
            mAllStates.add(state);
        }
    }

    private static final class AxisEventListener implements AxisEvent.Listener {

        AxisEvent mEvent;

        int mValue;

        void reset() {
            mEvent = null;
            mValue = 0;
        }

        @Override
        public void onAxisEvent(@NonNull AxisEvent event, @IntRange(from = -100, to = 100) int value) {
            mEvent = event;
            mValue = value;
        }
    }
}
