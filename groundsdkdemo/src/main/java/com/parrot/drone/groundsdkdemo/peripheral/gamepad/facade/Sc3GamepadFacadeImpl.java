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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.SkyController3Gamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.MappingEntry;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Sc3GamepadFacadeImpl extends GamepadFacade {

    static final Creator<SkyController3Gamepad> CREATOR =
            new Creator<SkyController3Gamepad>(SkyController3Gamepad.class) {

                @Override
                GamepadFacade create(@NonNull SkyController3Gamepad gamepad) {
                    return new Sc3GamepadFacadeImpl(gamepad);
                }
            };

    private static final Adapter.Button<SkyController3Gamepad.Button> BUTTON_ADAPTER =
            new Adapter.Button<>(SkyController3Gamepad.Button.class);

    private static final Adapter.ButtonEvent<ButtonEvent> BUTTON_EVENT_ADAPTER =
            new Adapter.ButtonEvent<>(ButtonEvent.class);

    private static final Adapter.ButtonState<ButtonEvent.State> BUTTON_STATE_ADAPTER =
            new Adapter.ButtonState<>(ButtonEvent.State.class);

    private static final Adapter.Axis<SkyController3Gamepad.Axis> AXIS_ADAPTER =
            new Adapter.Axis<>(SkyController3Gamepad.Axis.class);

    private static final Adapter.AxisEvent<AxisEvent> AXIS_EVENT_ADAPTER = new Adapter.AxisEvent<>(AxisEvent.class);

    private static final class FacadeButtonMappingEntry extends Button.MappingEntry {

        @NonNull
        private final ButtonsMappingEntry mImpl;

        FacadeButtonMappingEntry(@NonNull ButtonsMappingEntry impl) {
            mImpl = impl;
        }

        @NonNull
        @Override
        public Drone.Model getDroneModel() {
            return mImpl.getDroneModel();
        }

        @NonNull
        @Override
        public ButtonsMappableAction getAction() {
            return mImpl.getAction();
        }

        @NonNull
        @Override
        public Set<Button.Event> getButtonEvents() {
            return BUTTON_EVENT_ADAPTER.fromImpl(mImpl.getButtonEvents());
        }
    }

    private static final class FacadeAxisMappingEntry extends Axis.MappingEntry {

        @NonNull
        private final AxisMappingEntry mImpl;

        FacadeAxisMappingEntry(@NonNull AxisMappingEntry impl) {
            mImpl = impl;
        }

        @NonNull
        @Override
        public Drone.Model getDroneModel() {
            return mImpl.getDroneModel();
        }

        @NonNull
        @Override
        public AxisMappableAction getAction() {
            return mImpl.getAction();
        }

        @NonNull
        @Override
        public Set<Button.Event> getButtonEvents() {
            return BUTTON_EVENT_ADAPTER.fromImpl(mImpl.getButtonEvents());
        }

        @NonNull
        @Override
        public Axis.Event getAxisEvent() {
            return AXIS_EVENT_ADAPTER.fromImpl(mImpl.getAxisEvent());
        }
    }

    private static final Adapter<Mapping.Entry, MappingEntry> MAPPING_ENTRY_ADAPTER =
            new Adapter<Mapping.Entry, MappingEntry>() {

                @NonNull
                @Override
                Mapping.Entry fromImpl(@NonNull MappingEntry impl) {
                    switch (impl.getType()) {
                        case BUTTONS_MAPPING:
                            return new FacadeButtonMappingEntry(impl.as(ButtonsMappingEntry.class));
                        case AXIS_MAPPING:
                            return new FacadeAxisMappingEntry(impl.as(AxisMappingEntry.class));
                    }
                    throw new AssertionError();
                }

                @NonNull
                @Override
                MappingEntry toImpl(@NonNull Mapping.Entry facade) {
                    if (facade instanceof FacadeButtonMappingEntry) {
                        return ((FacadeButtonMappingEntry) facade).mImpl;
                    } else if (facade instanceof FacadeAxisMappingEntry) {
                        return ((FacadeAxisMappingEntry) facade).mImpl;
                    }
                    throw new AssertionError();
                }
            };

    @NonNull
    private final SkyController3Gamepad mImpl;

    private Sc3GamepadFacadeImpl(@NonNull SkyController3Gamepad impl) {
        mImpl = impl;
    }

    @NonNull
    @Override
    public List<Button> allButtons() {
        return BUTTON_ADAPTER.facades();
    }

    @NonNull
    @Override
    public List<Axis> allAxes() {
        return AXIS_ADAPTER.facades();
    }

    @NonNull
    @Override
    public Button.MappingEntry newButtonMappingEntry(@NonNull Drone.Model droneModel,
                                                     @NonNull ButtonsMappableAction action,
                                                     @NonNull Set<Button.Event> buttonEvents) {
        return new FacadeButtonMappingEntry(new ButtonsMappingEntry(droneModel, action,
                BUTTON_EVENT_ADAPTER.toImpl(buttonEvents)));
    }

    @NonNull
    @Override
    public Axis.MappingEntry newAxisMappingEntry(@NonNull Drone.Model droneModel, @NonNull AxisMappableAction action,
                                                 @NonNull Axis.Event axisEvent,
                                                 @NonNull Set<Button.Event> buttonEvents) {
        return new FacadeAxisMappingEntry(new AxisMappingEntry(droneModel, action, AXIS_EVENT_ADAPTER.toImpl(axisEvent),
                BUTTON_EVENT_ADAPTER.toImpl(buttonEvents)));
    }

    @NonNull
    @Override
    public GamepadFacade setButtonEventListener(@Nullable Button.Event.Listener listener) {
        mImpl.setButtonEventListener(listener == null ? null : (ButtonEvent.Listener) (event, state) ->
                listener.onButtonEvent(BUTTON_EVENT_ADAPTER.fromImpl(event), BUTTON_STATE_ADAPTER.fromImpl(state)));
        return this;
    }

    @NonNull
    @Override
    public GamepadFacade setAxisEventListener(@Nullable Axis.Event.Listener listener) {
        mImpl.setAxisEventListener(listener == null ? null : (AxisEvent.Listener) (event, value) ->
                listener.onAxisEvent(AXIS_EVENT_ADAPTER.fromImpl(event), value));
        return this;
    }

    @Override
    public void grabInputs(@NonNull Collection<Button> buttons, @NonNull Collection<Axis> axes) {
        mImpl.grabInputs(BUTTON_ADAPTER.toImpl(buttons), AXIS_ADAPTER.toImpl(axes));
    }

    @NonNull
    @Override
    public Set<Button> getGrabbedButtons() {
        return BUTTON_ADAPTER.fromImpl(mImpl.getGrabbedButtons());
    }

    @NonNull
    @Override
    public Set<Axis> getGrabbedAxes() {
        return AXIS_ADAPTER.fromImpl(mImpl.getGrabbedAxes());
    }

    @NonNull
    @Override
    public Map<Button.Event, Button.State> getGrabbedButtonsState() {
        return Adapter.fromImpl(mImpl.getGrabbedButtonsState(), BUTTON_EVENT_ADAPTER, BUTTON_STATE_ADAPTER);
    }

    @NonNull
    @Override
    public Set<Drone.Model> getSupportedDroneModels() {
        return mImpl.getSupportedDroneModels();
    }

    @Nullable
    @Override
    public Drone.Model getActiveDroneModel() {
        return mImpl.getActiveDroneModel();
    }

    @Nullable
    @Override
    public Set<Mapping.Entry> getMapping(@NonNull Drone.Model droneModel) {
        Set<MappingEntry> impl = mImpl.getMapping(droneModel);
        return impl == null ? null : MAPPING_ENTRY_ADAPTER.fromImpl(impl);
    }

    @Override
    public void registerMappingEntry(@NonNull Mapping.Entry mappingEntry) {
        mImpl.registerMappingEntry(MAPPING_ENTRY_ADAPTER.toImpl(mappingEntry));
    }

    @Override
    public void unregisterMappingEntry(@NonNull Mapping.Entry mappingEntry) {
        mImpl.unregisterMappingEntry(MAPPING_ENTRY_ADAPTER.toImpl(mappingEntry));
    }

    @Override
    public void resetDefaultMappings(@NonNull Drone.Model droneModel) {
        mImpl.resetDefaultMappings(droneModel);
    }

    @Override
    public void resetAllDefaultMappings() {
        mImpl.resetAllDefaultMappings();
    }

    @Override
    public void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                                    @NonNull AxisInterpolator interpolator) {
        mImpl.setAxisInterpolator(droneModel, AXIS_ADAPTER.toImpl(axis), interpolator);
    }

    @Nullable
    @Override
    public Map<Axis, AxisInterpolator> getAxisInterpolators(@NonNull Drone.Model droneModel) {
        Map<SkyController3Gamepad.Axis, AxisInterpolator> impls = mImpl.getAxisInterpolators(droneModel);
        return impls == null ? null : AXIS_ADAPTER.fromImpl(impls);
    }

    @Override
    public void reverseAxis(@NonNull Drone.Model droneModel, @NonNull Axis axis) {
        mImpl.reverseAxis(droneModel, AXIS_ADAPTER.toImpl(axis));
    }

    @Nullable
    @Override
    public Set<Axis> getReversedAxes(@NonNull Drone.Model droneModel) {
        Set<SkyController3Gamepad.Axis> impls = mImpl.getReversedAxes(droneModel);
        return impls == null ? null : AXIS_ADAPTER.fromImpl(impls);
    }

    @NonNull
    @Override
    public OptionalBooleanSetting getVolatileMapping() {
        return mImpl.volatileMapping();
    }
}
