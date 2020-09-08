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

package com.parrot.drone.groundsdk;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.MappingEntry;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Set;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings({"unused", "UtilityClassWithoutPrivateConstructor"})
public final class ScUaMappingEntryMatcher {

    public static Matcher<MappingEntry> hasModel(Drone.Model model) {
        return new FeatureMatcher<MappingEntry, Drone.Model>(equalTo(model), "Model", "Model") {

            @Override
            protected Drone.Model featureValueOf(MappingEntry actual) {
                return actual.getDroneModel();
            }
        };
    }

    public static Matcher<MappingEntry> hasType(MappingEntry.Type type) {
        return new FeatureMatcher<MappingEntry, MappingEntry.Type>(equalTo(type), "Type", "Type") {

            @Override
            protected MappingEntry.Type featureValueOf(MappingEntry actual) {
                return actual.getType();
            }
        };
    }

    public static Matcher<MappingEntry> hasAction(ButtonsMappableAction action) {
        return both(hasType(MappingEntry.Type.BUTTONS_MAPPING)).and(
                new FeatureMatcher<MappingEntry, ButtonsMappableAction>(equalTo(action), "Action", "Action") {

                    @Override
                    protected ButtonsMappableAction featureValueOf(MappingEntry actual) {
                        return actual.as(ButtonsMappingEntry.class).getAction();
                    }
                }
        );
    }

    public static Matcher<MappingEntry> hasAction(AxisMappableAction action) {
        return both(hasType(MappingEntry.Type.AXIS_MAPPING)).and(
                new FeatureMatcher<MappingEntry, AxisMappableAction>(equalTo(action), "Action", "Action") {

                    @Override
                    protected AxisMappableAction featureValueOf(MappingEntry actual) {
                        return actual.as(AxisMappingEntry.class).getAction();
                    }
                }
        );
    }

    public static Matcher<MappingEntry> hasAxis(AxisEvent axis) {
        return both(hasType(MappingEntry.Type.AXIS_MAPPING)).and(
                new FeatureMatcher<MappingEntry, AxisEvent>(equalTo(axis), "Axis", "Axis") {

                    @Override
                    protected AxisEvent featureValueOf(MappingEntry actual) {
                        return actual.as(AxisMappingEntry.class).getAxisEvent();
                    }
                }
        );
    }

    public static Matcher<MappingEntry> hasButtons(ButtonEvent... buttons) {
        return new FeatureMatcher<MappingEntry, Set<ButtonEvent>>(containsInAnyOrder(buttons), "Buttons", "Buttons") {

            @Override
            protected Set<ButtonEvent> featureValueOf(MappingEntry actual) {
                switch (actual.getType()) {
                    case AXIS_MAPPING:
                        return actual.as(AxisMappingEntry.class).getButtonEvents();
                    case BUTTONS_MAPPING:
                        return actual.as(ButtonsMappingEntry.class).getButtonEvents();
                }
                throw new AssertionError();
            }
        };
    }

    public static Matcher<MappingEntry> noButtons() {
        return new FeatureMatcher<MappingEntry, Set<ButtonEvent>>(empty(), "Buttons", "Buttons") {

            @Override
            protected Set<ButtonEvent> featureValueOf(MappingEntry actual) {
                switch (actual.getType()) {
                    case AXIS_MAPPING:
                        return actual.as(AxisMappingEntry.class).getButtonEvents();
                    case BUTTONS_MAPPING:
                        return actual.as(ButtonsMappingEntry.class).getButtonEvents();
                }
                throw new AssertionError();
            }
        };
    }
}
