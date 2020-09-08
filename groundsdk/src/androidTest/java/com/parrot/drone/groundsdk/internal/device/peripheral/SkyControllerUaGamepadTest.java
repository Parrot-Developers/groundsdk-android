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

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.SkyControllerUaGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.MappingEntry;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.gamepad.SkyControllerUaGamepadCore;
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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class SkyControllerUaGamepadTest {

    private MockComponentStore<Peripheral> mStore;

    private SkyControllerUaGamepadCore mGamepadImpl;

    private SkyControllerUaGamepad mGamepad;

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
        mGamepadImpl = new SkyControllerUaGamepadCore(mStore, mBackend);
        mGamepad = mStore.get(SkyControllerUaGamepad.class);
        mStore.registerObserver(SkyControllerUaGamepad.class, () -> {
            mChangeCnt++;
            mGamepad = mStore.get(SkyControllerUaGamepad.class);
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
        assertThat(mGamepad, nullValue());
        assertThat(mChangeCnt, is(0));

        mGamepadImpl.publish();
        assertThat(mGamepad, is(mGamepadImpl));
        assertThat(mChangeCnt, is(1));

        mGamepadImpl.unpublish();
        assertThat(mGamepad, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testGrabInputs() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start, no input shall be grabbed
        assertThat(mGamepad.getGrabbedButtons(), empty());
        assertThat(mGamepad.getGrabbedAxes(), empty());

        // grab an input
        mGamepad.grabInputs(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(1));
        assertThat(mBackend.mGrabbedButtons, empty());
        assertThat(mBackend.mGrabbedAxes, contains(SkyControllerUaGamepad.Axis.LEFT_SLIDER));

        // mock grabbed inputs update from low-level
        mGamepadImpl.updateGrabbedInputs(
                EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_SLIDER)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getGrabbedButtons(), empty());
        assertThat(mGamepad.getGrabbedAxes(), contains(SkyControllerUaGamepad.Axis.LEFT_SLIDER));

        // grab same input, this should do nothing
        mGamepad.grabInputs(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(1));

        // grab all inputs
        mGamepad.grabInputs(EnumSet.allOf(SkyControllerUaGamepad.Button.class),
                EnumSet.allOf(SkyControllerUaGamepad.Axis.class));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(2));
        assertThat(mBackend.mGrabbedButtons, containsInAnyOrder(SkyControllerUaGamepad.Button.values()));
        assertThat(mBackend.mGrabbedAxes, containsInAnyOrder(SkyControllerUaGamepad.Axis.values()));


        // mock grabbed inputs update from low-level
        mGamepadImpl.updateGrabbedInputs(
                EnumSet.of(SkyControllerUaGamepad.Button.REAR_RIGHT),
                EnumSet.of(SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getGrabbedButtons(), containsInAnyOrder(
                SkyControllerUaGamepad.Button.REAR_RIGHT));
        assertThat(mGamepad.getGrabbedAxes(), containsInAnyOrder(
                SkyControllerUaGamepad.Axis.LEFT_STICK_VERTICAL));

        // release all inputs
        mGamepad.grabInputs(EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class));
        assertThat(mBackend.mSetGrabbedInputsCnt, is(3));
        assertThat(mBackend.mGrabbedButtons, empty());
        assertThat(mBackend.mGrabbedAxes, empty());

        // mock grabbed inputs update from low-level
        mGamepadImpl.updateGrabbedInputs(
                EnumSet.noneOf(SkyControllerUaGamepad.Button.class),
                EnumSet.noneOf(SkyControllerUaGamepad.Axis.class)).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.getGrabbedButtons(), empty());
        assertThat(mGamepad.getGrabbedAxes(), empty());
    }

    @Test
    public void testGrabbedButtonEvents() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        mGamepad.setButtonEventListener(mButtonEventListener);

        // at start, no button event shall be grabbed
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), empty());

        EnumMap<ButtonEvent, ButtonEvent.State> grabState = new EnumMap<>(ButtonEvent.class);
        grabState.put(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED);
        grabState.put(ButtonEvent.FRONT_TOP_RIGHT_BUTTON, ButtonEvent.State.RELEASED);
        grabState.put(ButtonEvent.REAR_LEFT_BUTTON, ButtonEvent.State.RELEASED);
        grabState.put(ButtonEvent.REAR_RIGHT_BUTTON, ButtonEvent.State.PRESSED);

        // mock grab state update from low level
        mGamepadImpl.updateGrabbedButtonEvents(grabState).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(4));
        assertThat(mGamepad.getGrabbedButtonsState(), allOf(
                hasEntry(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED),
                hasEntry(ButtonEvent.FRONT_TOP_RIGHT_BUTTON, ButtonEvent.State.RELEASED),
                hasEntry(ButtonEvent.REAR_LEFT_BUTTON, ButtonEvent.State.RELEASED),
                hasEntry(ButtonEvent.REAR_RIGHT_BUTTON, ButtonEvent.State.PRESSED)));

        // ensure listener gets called for pressed buttons
        assertThat(mButtonEventListener.mAllEvents, containsInAnyOrder(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.REAR_RIGHT_BUTTON));
        assertThat(mButtonEventListener.mAllStates, contains(
                ButtonEvent.State.PRESSED, ButtonEvent.State.PRESSED));

    }

    @Test
    public void testButtonEvents() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // ensure we don't receive events when no listener is set
        mGamepadImpl.setButtonEventListener(null);

        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // set a listener
        mGamepadImpl.setButtonEventListener(mButtonEventListener);

        // ensure we still don't receive any events for buttons that are not in the grabbed state
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // mock grab state update
        EnumMap<ButtonEvent, ButtonEvent.State> grabState = new EnumMap<>(ButtonEvent.class);
        grabState.put(ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.RELEASED);
        mGamepadImpl.updateGrabbedButtonEvents(grabState).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mGamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.RELEASED));

        // ensure we did not receive an event for a released button state
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // notify button press from low level
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure that the grab state changed
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mGamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED));

        // ensure that the event got forwarded to the listener
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_LEFT_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.PRESSED));

        // repeat the exact same event
        mButtonEventListener.reset();
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure nothing changed
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mGamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED));

        // ensure no event got forwarded
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // notify button release from low level
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.RELEASED).notifyUpdated();

        // ensure that the grab state changed
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mGamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.RELEASED));

        // ensure that the event got forwarded to the listener
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_LEFT_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.RELEASED));

        // unregister listener
        mButtonEventListener.reset();
        mGamepad.setButtonEventListener(null);

        // notify button press from low level
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();
        // ensure that the grab state changed
        assertThat(mChangeCnt, is(5));
        assertThat(mGamepad.getGrabbedButtonsState().keySet(), hasSize(1));
        assertThat(mGamepad.getGrabbedButtonsState(), hasEntry(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED));

        // ensure listener was not called
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());

        // put listener back
        mGamepad.setButtonEventListener(mButtonEventListener);

        // notify button release from low level
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.RELEASED).notifyUpdated();

        // ensure listener is called
        assertThat(mButtonEventListener.mEvent, is(ButtonEvent.FRONT_TOP_LEFT_BUTTON));
        assertThat(mButtonEventListener.mState, is(ButtonEvent.State.RELEASED));

        // unpublish
        mButtonEventListener.reset();
        mGamepadImpl.unpublish();

        // notify button press from low level
        mGamepadImpl.updateGrabbedButtonEvent(
                ButtonEvent.FRONT_TOP_LEFT_BUTTON, ButtonEvent.State.PRESSED).notifyUpdated();

        // ensure listener was not called
        assertThat(mButtonEventListener.mEvent, nullValue());
        assertThat(mButtonEventListener.mState, nullValue());
    }

    @Test
    public void testAxisEvents() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // ensure we don't receive events when no listener is set
        mGamepadImpl.setAxisEventListener(null);

        mGamepadImpl.notifyAxisEvent(AxisEvent.LEFT_SLIDER, 42);
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));

        // set a listener
        mGamepadImpl.setAxisEventListener(mAxisEventListener);

        // notify axis event from low level
        mGamepadImpl.notifyAxisEvent(AxisEvent.LEFT_STICK_HORIZONTAL, 42);

        // ensure that the event got forwarded to the listener
        assertThat(mAxisEventListener.mEvent, is(AxisEvent.LEFT_STICK_HORIZONTAL));
        assertThat(mAxisEventListener.mValue, is(42));

        // unregister listener
        mAxisEventListener.reset();
        mGamepad.setAxisEventListener(null);

        // notify axis event from low level
        mGamepadImpl.notifyAxisEvent(AxisEvent.LEFT_STICK_HORIZONTAL, 42);

        // ensure listener was not called
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));

        // put listener back
        mGamepad.setAxisEventListener(mAxisEventListener);

        // notify another axis event from low level
        mGamepadImpl.notifyAxisEvent(AxisEvent.RIGHT_STICK_VERTICAL, -42);

        // ensure that the event got forwarded to the listener
        assertThat(mAxisEventListener.mEvent, is(AxisEvent.RIGHT_STICK_VERTICAL));
        assertThat(mAxisEventListener.mValue, is(-42));

        // unpublish
        mAxisEventListener.reset();
        mGamepadImpl.unpublish();

        // notify axis event from low level
        mGamepadImpl.notifyAxisEvent(AxisEvent.RIGHT_STICK_HORIZONTAL, 100);

        // ensure listener was not called
        assertThat(mAxisEventListener.mEvent, nullValue());
        assertThat(mAxisEventListener.mValue, is(0));
    }

    @Test
    public void testMappings() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start, there should be no mappings
        assertThat(mGamepad.getMapping(Drone.Model.ANAFI_4K), nullValue());

        // register button mapping
        MappingEntry entry1 = new ButtonsMappingEntry(Drone.Model.ANAFI_4K, ButtonsMappableAction.FLIP_LEFT,
                EnumSet.of(ButtonEvent.LEFT_SLIDER_DOWN, ButtonEvent.LEFT_STICK_LEFT));

        mGamepad.registerMappingEntry(entry1);
        assertThat(mBackend.mSetupMappingEntryCnt, is(1));
        assertThat(mBackend.mMappingEntry, is(entry1));
        assertThat(mBackend.mRegister, is(true));

        // mock update from low-level
        mGamepadImpl.updateSupportedDroneModels(EnumSet.of(Drone.Model.ANAFI_4K))
                    .updateButtonsMappings(Collections.singleton(entry1)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getMapping(Drone.Model.ANAFI_4K), contains(entry1));

        // register axis mapping
        MappingEntry entry2 = new AxisMappingEntry(Drone.Model.ANAFI_4K, AxisMappableAction.PAN_CAMERA,
                AxisEvent.RIGHT_STICK_HORIZONTAL, EnumSet.of(ButtonEvent.RIGHT_SLIDER_UP));

        mGamepad.registerMappingEntry(entry2);
        assertThat(mBackend.mSetupMappingEntryCnt, is(2));
        assertThat(mBackend.mMappingEntry, is(entry2));
        assertThat(mBackend.mRegister, is(true));

        // mock update from low-level
        mGamepadImpl.updateAxisMappings(Collections.singleton(entry2)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getMapping(Drone.Model.ANAFI_4K), containsInAnyOrder(entry1, entry2));

        // unregister button mapping
        mGamepad.unregisterMappingEntry(entry1);
        assertThat(mBackend.mSetupMappingEntryCnt, is(3));
        assertThat(mBackend.mMappingEntry, is(entry1));
        assertThat(mBackend.mRegister, is(false));

        // mock update from low-level
        mGamepadImpl.updateButtonsMappings(Collections.emptySet()).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.getMapping(Drone.Model.ANAFI_4K), contains(entry2));

        // unregister axis mapping
        mGamepad.unregisterMappingEntry(entry2);
        assertThat(mBackend.mSetupMappingEntryCnt, is(4));
        assertThat(mBackend.mMappingEntry, is(entry2));
        assertThat(mBackend.mRegister, is(false));

        // mock update from low-level
        mGamepadImpl.updateAxisMappings(Collections.emptySet()).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mGamepad.getMapping(Drone.Model.ANAFI_4K), empty());
    }

    @Test
    public void testResetMappings() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        mGamepad.resetDefaultMappings(Drone.Model.ANAFI_4K);
        assertThat(mBackend.mResetMappingsCnt, is(1));
        assertThat(mBackend.mResetMappingModel, is(Drone.Model.ANAFI_4K));

        mGamepad.resetAllDefaultMappings();
        assertThat(mBackend.mResetMappingsCnt, is(2));
        assertThat(mBackend.mResetMappingModel, nullValue());
    }

    @Test
    public void testAxisInterpolators() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at first, should return null since the list of supported drone models is not known
        assertThat(mGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), nullValue());

        // adding an interpolator for an unsupported drone model should do nothing
        mGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LIGHT_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(0));
        assertThat(mBackend.mAxisInterpolatorModel, nullValue());
        assertThat(mBackend.mAxisInterpolatorAxis, nullValue());
        assertThat(mBackend.mAxisInterpolator, nullValue());

        Set<SkyControllerUaGamepadCore.AxisInterpolatorEntry> entries = new HashSet<>();
        entries.add(new SkyControllerUaGamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR));
        // now declare that we support the drone model
        mGamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        // this should trigger a change
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR)).and(mapHasSize(1)));

        // adding the exact same interpolator should do nothing
        mGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.LINEAR);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(0));
        assertThat(mBackend.mAxisInterpolatorModel, nullValue());
        assertThat(mBackend.mAxisInterpolatorAxis, nullValue());
        assertThat(mBackend.mAxisInterpolator, nullValue());

        // set another interpolator
        mGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(1));
        assertThat(mBackend.mAxisInterpolatorModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mAxisInterpolatorAxis, is(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mAxisInterpolator, is(AxisInterpolator.STRONGEST_EXPONENTIAL));

        // mock update from low-level
        entries.clear();
        entries.add(new SkyControllerUaGamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL));
        mGamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), both(hasEntry(
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL)).and(
                mapHasSize(1)));

        // add an interpolator to a different axis
        mGamepad.setAxisInterpolator(Drone.Model.ANAFI_4K, SkyControllerUaGamepad.Axis.LEFT_SLIDER,
                AxisInterpolator.LIGHT_EXPONENTIAL);
        assertThat(mBackend.mSetAxisInterpolatorCnt, is(2));
        assertThat(mBackend.mAxisInterpolatorModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mAxisInterpolatorAxis, is(SkyControllerUaGamepad.Axis.LEFT_SLIDER));
        assertThat(mBackend.mAxisInterpolator, is(AxisInterpolator.LIGHT_EXPONENTIAL));

        // mock update from low level
        entries.add(new SkyControllerUaGamepadCore.AxisInterpolatorEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_SLIDER, AxisInterpolator.LIGHT_EXPONENTIAL));
        mGamepadImpl.updateAxisInterpolators(entries).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.getAxisInterpolators(Drone.Model.ANAFI_4K), allOf(
                hasEntry(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, AxisInterpolator.STRONGEST_EXPONENTIAL),
                hasEntry(SkyControllerUaGamepad.Axis.LEFT_SLIDER, AxisInterpolator.LIGHT_EXPONENTIAL),
                mapHasSize(2)));
    }

    @Test
    public void testReversedAxes() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at first, should return null since the list of supported drone models is not known
        assertThat(mGamepad.getReversedAxes(Drone.Model.ANAFI_4K), nullValue());

        // adding a reversed axis for an unsupported drone model should do nothing
        mGamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(0));
        assertThat(mBackend.mReversedAxisModel, nullValue());
        assertThat(mBackend.mReversedAxis, nullValue());
        assertThat(mBackend.mReversed, is(false));

        Set<SkyControllerUaGamepadCore.ReversedAxisEntry> entries = new HashSet<>();
        entries.add(new SkyControllerUaGamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, false));
        // now declare that we support the drone model
        mGamepadImpl.updateReversedAxes(entries).notifyUpdated();
        // this should trigger a change
        assertThat(mChangeCnt, is(2));
        // set should be empty (not null since the model is supported now)
        assertThat(mGamepad.getReversedAxes(Drone.Model.ANAFI_4K), empty());

        // reverse axis
        mGamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(1));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mReversed, is(true));

        // mock update from low-level
        entries.clear();
        entries.add(new SkyControllerUaGamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, true));
        mGamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getReversedAxes(Drone.Model.ANAFI_4K),
                contains(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL));

        // reverse same axis back to normal
        mGamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL);
        assertThat(mBackend.mSetReversedAxisCnt, is(2));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL));
        assertThat(mBackend.mReversed, is(false));

        // mock update from low level
        entries.clear();
        entries.add(new SkyControllerUaGamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.LEFT_STICK_HORIZONTAL, false));
        mGamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.getReversedAxes(Drone.Model.ANAFI_4K), empty());

        // reverse another axis
        mGamepad.reverseAxis(Drone.Model.ANAFI_4K, SkyControllerUaGamepad.Axis.RIGHT_SLIDER);
        assertThat(mBackend.mSetReversedAxisCnt, is(3));
        assertThat(mBackend.mReversedAxisModel, is(Drone.Model.ANAFI_4K));
        assertThat(mBackend.mReversedAxis, is(SkyControllerUaGamepad.Axis.RIGHT_SLIDER));
        assertThat(mBackend.mReversed, is(true));

        // mock update from low level
        entries.add(new SkyControllerUaGamepadCore.ReversedAxisEntry(Drone.Model.ANAFI_4K,
                SkyControllerUaGamepad.Axis.RIGHT_SLIDER, true));
        mGamepadImpl.updateReversedAxes(entries).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mGamepad.getReversedAxes(Drone.Model.ANAFI_4K),
                contains(SkyControllerUaGamepad.Axis.RIGHT_SLIDER));
    }

    @Test
    public void testVolatileMapping() {
        mGamepadImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGamepad.volatileMapping(), allOf(
                optionalSettingIsUnavailable(),
                optionalBooleanSettingIsDisabled()));

        // mock update from low-level, volatile mapping is now supported
        mGamepadImpl.volatileMapping().updateSupportedFlag(true);
        mGamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.volatileMapping(), allOf(
                optionalSettingIsAvailable(),
                optionalBooleanSettingIsDisabled()));

        // enable volatile mapping
        mGamepad.volatileMapping().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsEnabling());
        assertThat(mBackend.mEnableVolatileMappingCnt, is(1));
        assertThat(mBackend.mVolatileMappingEnabled, is(true));

        // update from low-level
        mGamepadImpl.volatileMapping().updateValue(true);
        mGamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsEnabled());

        // disable volatile mapping
        mGamepad.volatileMapping().toggle();
        assertThat(mChangeCnt, is(5));
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsDisabling());
        assertThat(mBackend.mEnableVolatileMappingCnt, is(2));
        assertThat(mBackend.mVolatileMappingEnabled, is(false));

        // update from low-level
        mGamepadImpl.volatileMapping().updateValue(false);
        mGamepadImpl.notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsDisabled());
    }

    @Test
    public void testActiveDroneModel() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start should be null -- no active model
        assertThat(mGamepad.getActiveDroneModel(), nullValue());

        // update from low level -- anafi 4k is active
        mGamepadImpl.updateActiveDroneModel(Drone.Model.ANAFI_4K).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getActiveDroneModel(), is(Drone.Model.ANAFI_4K));
    }

    @Test
    public void testSupportedDroneModels() {
        mGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start should be empty -- no supported drones
        assertThat(mGamepad.getSupportedDroneModels(), empty());

        // update from low level -- declare anafi 4k support only
        mGamepadImpl.updateSupportedDroneModels(EnumSet.of(Drone.Model.ANAFI_4K)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mGamepad.getSupportedDroneModels(), contains(Drone.Model.ANAFI_4K));

        // update from low level -- declare only anafi 4k / thermal support
        mGamepadImpl.updateSupportedDroneModels(
                EnumSet.of(Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mGamepad.getSupportedDroneModels(), containsInAnyOrder(
                Drone.Model.ANAFI_4K, Drone.Model.ANAFI_THERMAL));
    }

    @Test
    public void testCancelRollbacks() {
        mGamepadImpl.volatileMapping().updateSupportedFlag(true).updateValue(false);

        mGamepadImpl.publish();

        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsDisabled());

        // mock user changes all settings
        mGamepad.volatileMapping().setEnabled(true);

        // cancel all rollbacks
        mGamepadImpl.cancelSettingsRollbacks();

        // all setting should be updated to user values
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsEnabled());

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mGamepad.volatileMapping(), optionalBooleanSettingIsEnabled());
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements SkyControllerUaGamepadCore.Backend {

        int mSetGrabbedInputsCnt;

        Set<SkyControllerUaGamepad.Button> mGrabbedButtons;

        Set<SkyControllerUaGamepad.Axis> mGrabbedAxes;

        int mResetMappingsCnt;

        Drone.Model mResetMappingModel;

        int mSetupMappingEntryCnt;

        MappingEntry mMappingEntry;

        boolean mRegister;

        int mSetAxisInterpolatorCnt;

        Drone.Model mAxisInterpolatorModel;

        SkyControllerUaGamepad.Axis mAxisInterpolatorAxis;

        AxisInterpolator mAxisInterpolator;

        int mSetReversedAxisCnt;

        Drone.Model mReversedAxisModel;

        SkyControllerUaGamepad.Axis mReversedAxis;

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
        public void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull SkyControllerUaGamepad.Axis axis,
                                        @NonNull AxisInterpolator interpolator) {
            mSetAxisInterpolatorCnt++;
            mAxisInterpolatorModel = droneModel;
            mAxisInterpolatorAxis = axis;
            mAxisInterpolator = interpolator;
        }

        @Override
        public void setReversedAxis(@NonNull Drone.Model droneModel, @NonNull SkyControllerUaGamepad.Axis axis,
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
        public void setGrabbedInputs(@NonNull Set<SkyControllerUaGamepad.Button> buttons,
                                     @NonNull Set<SkyControllerUaGamepad.Axis> axes) {
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
