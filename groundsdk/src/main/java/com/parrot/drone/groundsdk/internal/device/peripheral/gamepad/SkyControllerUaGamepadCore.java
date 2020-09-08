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

package com.parrot.drone.groundsdk.internal.device.peripheral.gamepad;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.SkyControllerUaGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua.MappingEntry;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalBooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Core class for the SkyControllerUaGamepad.
 */
public final class SkyControllerUaGamepadCore extends SingletonComponentCore implements SkyControllerUaGamepad {

    /** Description of SkyControllerUaGamepad. */
    private static final ComponentDescriptor<Peripheral, SkyControllerUaGamepad> DESC =
            ComponentDescriptor.of(SkyControllerUaGamepad.class);

    /** Engine-specific backend for the SkyControllerUaGamepad. */
    public interface Backend {

        /**
         * Registers or unregisters the given mapping entry.
         *
         * @param mappingEntry mapping entry to register or unregister
         * @param register     {@code true} to register the entry, {@code false} to unregister it
         */
        void setupMappingEntry(@NonNull MappingEntry mappingEntry, boolean register);

        /**
         * Sets an axis interpolator.
         *
         * @param droneModel   drone model onto which the interpolator must apply
         * @param axis         axis onto which the interpolator must apply
         * @param interpolator axis interpolator to apply
         */
        void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                                 @NonNull AxisInterpolator interpolator);

        /**
         * Sets an axis inversion state.
         *
         * @param droneModel drone model for which the axis inversion state must be set
         * @param axis       axis for which the inversion state must be set
         * @param reversed   {@code true} to reverse the axis, {@code false} to set it as normal
         */
        void setReversedAxis(@NonNull Drone.Model droneModel, @NonNull Axis axis, boolean reversed);

        /**
         * Resets the given drone model's mapping to its default.
         *
         * @param model the drone model whose mapping must be reset. Use {@code null} to reset all supported models'
         *              mappings
         */
        void resetMappings(@Nullable Drone.Model model);

        /**
         * Grabs the given set of inputs.
         *
         * @param buttons set of buttons to grab
         * @param axes    set of axes to grab
         */
        void setGrabbedInputs(@NonNull Set<Button> buttons, @NonNull Set<Axis> axes);

        /**
         * Sets volatile mapping setting.
         *
         * @param enable volatile mapping setting value to set
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setVolatileMapping(boolean enable);
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Mappings, by drone model. */
    @NonNull
    private final Map<Drone.Model, Set<MappingEntry>> mMappings;

    /** Axis interpolators maps (interpolator by axis), by drone model. */
    @NonNull
    private final Map<Drone.Model, EnumMap<Axis, AxisInterpolator>> mAxisInterpolators;

    /** Reversed axes, by drone model. */
    @NonNull
    private final Map<Drone.Model, EnumSet<Axis>> mReversedAxes;

    /** Supported drone models. */
    @NonNull
    private EnumSet<Drone.Model> mSupportedModels;

    /** Currently grabbed buttons. */
    @NonNull
    private EnumSet<Button> mGrabbedButtons;

    /** Currently grabbed buttons. */
    @NonNull
    private EnumSet<Axis> mGrabbedAxes;

    /** Map of grabbed input's button states, by associated button event. */
    @NonNull
    private EnumMap<ButtonEvent, ButtonEvent.State> mGrabbedButtonEvents;

    /** Volatile mapping setting. */
    @NonNull
    private final OptionalBooleanSettingCore mVolatileMapping;

    /** Application button event listener. */
    @Nullable
    private ButtonEvent.Listener mButtonEventListener;

    /** Application axis event listener. */
    @Nullable
    private AxisEvent.Listener mAxisEventListener;

    /** Currently active drone model. */
    @Nullable
    private Drone.Model mActiveDroneModel;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public SkyControllerUaGamepadCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mSupportedModels = EnumSet.noneOf(Drone.Model.class);
        mMappings = new EnumMap<>(Drone.Model.class);
        mAxisInterpolators = new EnumMap<>(Drone.Model.class);
        mGrabbedButtons = EnumSet.noneOf(Button.class);
        mGrabbedAxes = EnumSet.noneOf(Axis.class);
        mGrabbedButtonEvents = new EnumMap<>(ButtonEvent.class);
        mReversedAxes = new EnumMap<>(Drone.Model.class);
        mVolatileMapping = new OptionalBooleanSettingCore(new SettingController(this::onSettingChange),
                mBackend::setVolatileMapping);
    }

    @Override
    public void unpublish() {
        mButtonEventListener = null;
        mAxisEventListener = null;
        mSupportedModels.clear();
        mMappings.clear();
        mGrabbedButtons.clear();
        mGrabbedAxes.clear();
        mGrabbedButtonEvents.clear();
        cancelSettingsRollbacks();
        super.unpublish();
    }

    @NonNull
    @Override
    public SkyControllerUaGamepad setButtonEventListener(@Nullable ButtonEvent.Listener listener) {
        mButtonEventListener = listener;
        return this;
    }

    @NonNull
    @Override
    public SkyControllerUaGamepad setAxisEventListener(@Nullable AxisEvent.Listener listener) {
        mAxisEventListener = listener;
        return this;
    }

    @Override
    public void grabInputs(@NonNull Set<Button> buttons, @NonNull Set<Axis> axes) {
        if (!buttons.equals(mGrabbedButtons) || !axes.equals(mGrabbedAxes)) {
            mBackend.setGrabbedInputs(buttons, axes);
        }
    }

    @NonNull
    @Override
    public Map<ButtonEvent, ButtonEvent.State> getGrabbedButtonsState() {
        return new EnumMap<>(mGrabbedButtonEvents);
    }

    @NonNull
    @Override
    public Set<Button> getGrabbedButtons() {
        return mGrabbedButtons.isEmpty() ? EnumSet.noneOf(Button.class) : EnumSet.copyOf(mGrabbedButtons);
    }

    @NonNull
    @Override
    public Set<Axis> getGrabbedAxes() {
        return mGrabbedAxes.isEmpty() ? EnumSet.noneOf(Axis.class) : EnumSet.copyOf(mGrabbedAxes);
    }

    @Nullable
    @Override
    public Set<MappingEntry> getMapping(@NonNull Drone.Model model) {
        return mSupportedModels.contains(model) ? new HashSet<>(mMappings.get(model)) : null;
    }

    @Nullable
    @Override
    public Drone.Model getActiveDroneModel() {
        return mActiveDroneModel;
    }

    @NonNull
    @Override
    public Set<Drone.Model> getSupportedDroneModels() {
        return mSupportedModels.isEmpty() ? EnumSet.noneOf(Drone.Model.class) : EnumSet.copyOf(mSupportedModels);
    }

    @Override
    public void unregisterMappingEntry(@NonNull MappingEntry mappingEntry) {
        mBackend.setupMappingEntry(mappingEntry, false);
    }

    @Override
    public void registerMappingEntry(@NonNull MappingEntry mappingEntry) {
        mBackend.setupMappingEntry(mappingEntry, true);
    }

    @Override
    public void resetDefaultMappings(@NonNull Drone.Model droneModel) {
        mBackend.resetMappings(droneModel);
    }

    @Override
    public void resetAllDefaultMappings() {
        mBackend.resetMappings(null);
    }

    @Override
    public void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                                    @NonNull AxisInterpolator interpolator) {
        Map<Axis, AxisInterpolator> interpolators = mAxisInterpolators.get(droneModel);
        if (interpolators != null && interpolators.get(axis) != interpolator) {
            mBackend.setAxisInterpolator(droneModel, axis, interpolator);
        }
    }

    @Nullable
    @Override
    public Map<Axis, AxisInterpolator> getAxisInterpolators(@NonNull Drone.Model droneModel) {
        EnumMap<Axis, AxisInterpolator> axisInterpolators = mAxisInterpolators.get(droneModel);
        return axisInterpolators == null ? null : new EnumMap<>(axisInterpolators);
    }

    @Override
    public void reverseAxis(@NonNull Drone.Model droneModel, @NonNull Axis axis) {
        Set<Axis> reversedAxes = mReversedAxes.get(droneModel);
        if (reversedAxes != null) {
            mBackend.setReversedAxis(droneModel, axis, !reversedAxes.contains(axis));
        }
    }

    @Nullable
    @Override
    public Set<Axis> getReversedAxes(@NonNull Drone.Model droneModel) {
        EnumSet<Axis> reversedAxes = mReversedAxes.get(droneModel);
        return reversedAxes == null ? null : EnumSet.copyOf(reversedAxes);
    }

    @NonNull
    @Override
    public OptionalBooleanSettingCore volatileMapping() {
        return mVolatileMapping;
    }

    /**
     * Updates the set of supported drone models.
     *
     * @param supportedModels new set of supported drone models
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateSupportedDroneModels(@NonNull EnumSet<Drone.Model> supportedModels) {
        if (!mSupportedModels.equals(supportedModels)) {
            mSupportedModels = supportedModels;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates all current buttons mapping entries.
     * <p>
     * This replaces all buttons mapping entries for all drone models with the provided mapping entries.
     *
     * @param mappings new collection of buttons mapping entries
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateButtonsMappings(@NonNull Collection<MappingEntry> mappings) {
        return updateMappings(mappings, MappingEntry.Type.BUTTONS_MAPPING);
    }

    /**
     * Updates all current axis mapping entries.
     * <p>
     * This replaces all axis mapping entries for all drone models with the provided mapping entries.
     *
     * @param mappings new collection of axis mapping entries
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateAxisMappings(@NonNull Collection<MappingEntry> mappings) {
        return updateMappings(mappings, MappingEntry.Type.AXIS_MAPPING);
    }

    /**
     * Updates all axis interpolators.
     *
     * @param interpolators new collection of axis interpolator entries
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateAxisInterpolators(@NonNull Collection<AxisInterpolatorEntry> interpolators) {
        mAxisInterpolators.clear();
        for (AxisInterpolatorEntry entry : interpolators) {
            EnumMap<Axis, AxisInterpolator> droneInterpolators = mAxisInterpolators.get(entry.mDroneModel);
            if (droneInterpolators == null) {
                droneInterpolators = new EnumMap<>(Axis.class);
                mAxisInterpolators.put(entry.mDroneModel, droneInterpolators);
            }
            droneInterpolators.put(entry.mAxis, entry.mInterpolator);
        }
        mChanged = true; // we assume that the device notifies us with a real change
        return this;
    }

    /**
     * Updates all reversed axes.
     *
     * @param reversedAxes new collection of reversed axis entries
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateReversedAxes(@NonNull Collection<ReversedAxisEntry> reversedAxes) {
        mReversedAxes.clear();
        for (ReversedAxisEntry entry : reversedAxes) {
            EnumSet<Axis> droneReversedAxes = mReversedAxes.get(entry.mDroneModel);
            if (droneReversedAxes == null) {
                droneReversedAxes = EnumSet.noneOf(Axis.class);
                mReversedAxes.put(entry.mDroneModel, droneReversedAxes);
            }
            if (entry.mReversed) {
                droneReversedAxes.add(entry.mAxis);
            }
        }
        mChanged = true; // we assume that the device notifies us with a real change
        return this;
    }

    /**
     * Updates the set of currently grabbed inputs.
     *
     * @param buttons new set of grabbed buttons
     * @param axes    new set of grabbed axes
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateGrabbedInputs(@NonNull EnumSet<Button> buttons,
                                                          @NonNull EnumSet<Axis> axes) {
        if (!mGrabbedButtons.equals(buttons)) {
            mGrabbedButtons = buttons;
            mChanged = true;
        }
        if (!mGrabbedAxes.equals(axes)) {
            mGrabbedAxes = axes;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the map of currently grabbed button events and their associated states.
     * <p>
     * This will also forward button events to the application for all buttons that are in the
     * {@link ButtonEvent.State#PRESSED} state in the provided state map.
     *
     * @param buttonEvents new map of button states, by grabbed button event
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateGrabbedButtonEvents(
            @NonNull EnumMap<ButtonEvent, ButtonEvent.State> buttonEvents) {
        if (!mGrabbedButtonEvents.equals(buttonEvents)) {
            mGrabbedButtonEvents = buttonEvents;
            mChanged = true;
        }
        // also forward currently pressed buttons as events
        if (mButtonEventListener != null) {
            for (ButtonEvent event : buttonEvents.keySet()) {
                if (buttonEvents.get(event) == ButtonEvent.State.PRESSED) {
                    mButtonEventListener.onButtonEvent(event, ButtonEvent.State.PRESSED);
                }
            }
        }
        return this;
    }

    /**
     * Updates the active drone model.
     *
     * @param model new active drone model
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateActiveDroneModel(@NonNull Drone.Model model) {
        if (mActiveDroneModel != model) {
            mActiveDroneModel = model;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the button event state and forwards an event to the application.
     * <p>
     * The event is only forwarded if present in the current grabbed buttons state and the button state differs from
     * the current state.
     *
     * @param event button event to forward
     * @param state button state
     *
     * @return this, to allow call chaining
     */
    public SkyControllerUaGamepadCore updateGrabbedButtonEvent(@NonNull ButtonEvent event,
                                                               @NonNull ButtonEvent.State state) {
        if (mGrabbedButtonEvents.containsKey(event)) {
            if (mGrabbedButtonEvents.put(event, state) != state) {
                mChanged = true;
                if (mButtonEventListener != null) {
                    mButtonEventListener.onButtonEvent(event, state);
                }
            }
        }
        return this;
    }

    /**
     * Forwards an axis event to the application.
     *
     * @param event axis event to forward
     * @param value axis value
     */
    public void notifyAxisEvent(@NonNull AxisEvent event, @IntRange(from = -100, to = 100) int value) {
        if (mAxisEventListener != null) {
            mAxisEventListener.onAxisEvent(event, value);
        }
    }

    /**
     * Updates buttons or axis mapping entries.
     * <p>
     * All existing mapping entries of the target type are removed and replaced by the provided mapping entries.
     *
     * @param mappings mapping entries to update.
     * @param target   type of mapping entries to update
     *
     * @return this, to allow call chaining
     */
    private SkyControllerUaGamepadCore updateMappings(@NonNull Collection<MappingEntry> mappings,
                                                      @NonNull MappingEntry.Type target) {
        // remove all current mapping entries matching target type
        for (Set<MappingEntry> droneMapping : mMappings.values()) {
            Iterator<MappingEntry> iterator = droneMapping.iterator();
            while (iterator.hasNext()) {
                MappingEntry entry = iterator.next();
                if (target == entry.getType()) {
                    iterator.remove();
                    mChanged = true;
                }
            }
        }
        // add all provided mappings
        for (MappingEntry entry : mappings) {
            Drone.Model droneModel = entry.getDroneModel();
            Set<MappingEntry> droneMapping = mMappings.get(droneModel);
            if (droneMapping == null) {
                droneMapping = new HashSet<>();
                mMappings.put(droneModel, droneMapping);
            }
            mChanged |= droneMapping.add(entry);
        }
        return this;
    }

    /**
     * An axis interpolator entry. Mainly used as a struct holding fields of concern to represent an axis interpolator,
     * hence the package visible final fields.
     */
    public static final class AxisInterpolatorEntry {

        /** Drone model onto which the interpolator applies. */
        @NonNull
        final Drone.Model mDroneModel;

        /** Axis onto which the interpolator applies. */
        @NonNull
        final Axis mAxis;

        /** Axis interpolator. */
        @NonNull
        final AxisInterpolator mInterpolator;

        /**
         * Constructor.
         *
         * @param droneModel   drone model onto which the interpolator applies.
         * @param axis         axis onto which the interpolator applies.
         * @param interpolator axis interpolator
         */
        public AxisInterpolatorEntry(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                                     @NonNull AxisInterpolator interpolator) {
            mDroneModel = droneModel;
            mAxis = axis;
            mInterpolator = interpolator;
        }

        /**
         * Gets the entry's drone model.
         *
         * @return the drone model onto which the entry applies
         */
        @NonNull
        public Drone.Model getDroneModel() {
            return mDroneModel;
        }
    }

    /**
     * An reversed axis entry. Mainly used as a struct holding fields of concern to represent axis inversion info,
     * hence the package visible final fields.
     */
    public static final class ReversedAxisEntry {

        /** Drone model onto which the axis inversion applies. */
        @NonNull
        final Drone.Model mDroneModel;

        /** Axis onto which the inversion applies. */
        @NonNull
        final Axis mAxis;

        /** {@code true} for a reversed axis, {@code false} for a not reversed one. */
        final boolean mReversed;

        /**
         * Constructor.
         *
         * @param droneModel drone model onto which the axis inversion applies.
         * @param axis       axis onto which the inversion applies.
         * @param reversed   {@code true} for a reversed axis, {@code false} for a not reversed one.
         */
        public ReversedAxisEntry(@NonNull Drone.Model droneModel, @NonNull Axis axis, boolean reversed) {
            mDroneModel = droneModel;
            mAxis = axis;
            mReversed = reversed;
        }
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public SkyControllerUaGamepadCore cancelSettingsRollbacks() {
        mVolatileMapping.cancelRollback();
        return this;
    }

    /**
     * Notified when an user setting changes.
     * <p>
     * In case the change originates from the user modifying the setting value, updates the store to show the setting
     * is updating.
     *
     * @param fromUser {@code true} if the change originates from the user, otherwise {@code false}
     */
    private void onSettingChange(boolean fromUser) {
        mChanged = true;
        if (fromUser) {
            notifyUpdated();
        }
    }
}
