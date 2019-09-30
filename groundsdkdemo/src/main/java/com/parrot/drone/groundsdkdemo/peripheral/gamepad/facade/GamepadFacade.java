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

package com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GamepadFacade implements Peripheral {

    public static final class Mapping {

        public abstract static class Entry {

            public enum Type {
                BUTTONS_MAPPING,
                AXIS_MAPPING
            }

            @NonNull
            private final Type mType;

            Entry(@NonNull Type type) {
                mType = type;
            }

            @NonNull
            public final Type getType() {
                return mType;
            }

            @SuppressWarnings("ConstantConditions")
            @NonNull
            public final <ENTRY extends Entry> ENTRY as(@NonNull Class<ENTRY> entryClass) {
                try {
                    return entryClass.cast(this);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Mapping entry is not a " + entryClass.getSimpleName()
                                                       + " [type: " + mType + "]");
                }
            }

            @NonNull
            public abstract Drone.Model getDroneModel();
        }
    }

    public static class Button {

        Button() {
        }

        public enum State {
            PRESSED,
            RELEASED
        }

        public static class Event {

            Event() {
            }

            public interface Listener {

                void onButtonEvent(@NonNull Button.Event event, @NonNull Button.State state);
            }
        }

        public abstract static class MappingEntry extends Mapping.Entry {

            MappingEntry() {
                super(Type.BUTTONS_MAPPING);
            }

            @NonNull
            public abstract ButtonsMappableAction getAction();

            @NonNull
            public abstract Set<Button.Event> getButtonEvents();
        }
    }

    public static class Axis {

        Axis() {
        }

        public static class Event {

            Event() {
            }

            public interface Listener {

                void onAxisEvent(@NonNull Axis.Event event, @IntRange(from = -100, to = 100) int value);
            }
        }

        public abstract static class MappingEntry extends Mapping.Entry {

            MappingEntry() {
                super(Type.AXIS_MAPPING);
            }

            @NonNull
            public abstract AxisMappableAction getAction();

            @NonNull
            public abstract Axis.Event getAxisEvent();

            @NonNull
            public abstract Set<Button.Event> getButtonEvents();
        }
    }

    @NonNull
    public abstract List<Button> allButtons();

    @NonNull
    public abstract List<Axis> allAxes();

    @NonNull
    public abstract Button.MappingEntry newButtonMappingEntry(@NonNull Drone.Model droneModel,
                                                              @NonNull ButtonsMappableAction action,
                                                              @NonNull Set<Button.Event> buttonEvents);

    @NonNull
    public abstract Axis.MappingEntry newAxisMappingEntry(@NonNull Drone.Model droneModel,
                                                          @NonNull AxisMappableAction action,
                                                          @NonNull Axis.Event axisEvent,
                                                          @NonNull Set<Button.Event> buttonEvents);

    @NonNull
    public abstract GamepadFacade setButtonEventListener(@Nullable Button.Event.Listener listener);

    @NonNull
    public abstract GamepadFacade setAxisEventListener(@Nullable Axis.Event.Listener listener);

    public abstract void grabInputs(@NonNull Collection<Button> buttons, @NonNull Collection<Axis> axes);

    @NonNull
    public abstract Set<Button> getGrabbedButtons();

    @NonNull
    public abstract Set<Axis> getGrabbedAxes();

    @NonNull
    public abstract Map<Button.Event, Button.State> getGrabbedButtonsState();

    @NonNull
    public abstract Set<Drone.Model> getSupportedDroneModels();

    @Nullable
    public abstract Drone.Model getActiveDroneModel();

    @Nullable
    public abstract Set<Mapping.Entry> getMapping(@NonNull Drone.Model droneModel);

    public abstract void registerMappingEntry(@NonNull Mapping.Entry mappingEntry);

    public abstract void unregisterMappingEntry(@NonNull Mapping.Entry mappingEntry);

    public abstract void resetDefaultMappings(@NonNull Drone.Model droneModel);

    @SuppressWarnings("unused")
    public abstract void resetAllDefaultMappings();

    public abstract void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                                             @NonNull AxisInterpolator interpolator);

    @Nullable
    public abstract Map<Axis, AxisInterpolator> getAxisInterpolators(@NonNull Drone.Model droneModel);

    public abstract void reverseAxis(@NonNull Drone.Model droneModel, @NonNull Axis axis);

    @Nullable
    public abstract Set<Axis> getReversedAxes(@NonNull Drone.Model droneModel);

    @NonNull
    public abstract OptionalBooleanSetting getVolatileMapping();

    GamepadFacade() {
    }

    abstract static class Creator<GAMEPAD_PERIPHERAL_IMPL extends Peripheral> {

        @NonNull
        private final Class<GAMEPAD_PERIPHERAL_IMPL> mGamepadClass;

        Creator(@NonNull Class<GAMEPAD_PERIPHERAL_IMPL> gamepadClass) {
            mGamepadClass = gamepadClass;
        }

        @NonNull
        final Class<GAMEPAD_PERIPHERAL_IMPL> getGamepadClass() {
            return mGamepadClass;
        }

        abstract GamepadFacade create(@NonNull GAMEPAD_PERIPHERAL_IMPL gamepad);
    }
}
