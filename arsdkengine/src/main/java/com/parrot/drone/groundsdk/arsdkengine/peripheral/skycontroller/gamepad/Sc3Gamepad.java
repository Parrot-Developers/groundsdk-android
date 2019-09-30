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

import android.annotation.SuppressLint;
import android.util.LongSparseArray;
import android.util.SparseArray;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.RCController;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.SkyController3Gamepad;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.MappingEntry;
import com.parrot.drone.groundsdk.internal.device.peripheral.gamepad.SkyController3GamepadCore;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_GAMEPAD;

/**
 * SkyController3Gamepad peripheral controller for SkyController3 remote control.
 */
public final class Sc3Gamepad extends GamepadControllerBase {

    /** The kyController3Gamepad peripheral for which this object is the backend. */
    @NonNull
    private final SkyController3GamepadCore mGamepad;

    /** Currently known buttons mapping entries, by entry uid. */
    @NonNull
    private final HashMap<Long, MappingEntry> mButtonMappings;

    /** Currently known axis mapping entries, by entry uid. */
    @NonNull
    private final HashMap<Long, MappingEntry> mAxisMappings;

    /** Currently known axis interpolator entries, by entry uid. */
    @NonNull
    private final HashMap<Long, SkyController3GamepadCore.AxisInterpolatorEntry> mAxisInterpolators;

    /** Currently known axis inversion entries, by entry uid. */
    @NonNull
    private final HashMap<Long, SkyController3GamepadCore.ReversedAxisEntry> mReversedAxes;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     */
    @SuppressLint("UseSparseArrays") // SparseArray has no values() method
    public Sc3Gamepad(@NonNull RCController deviceController) {
        super(deviceController, new Translator());
        mGamepad = new SkyController3GamepadCore(mComponentStore, mBackend);

        mButtonMappings = new HashMap<>();
        mAxisMappings = new HashMap<>();
        mAxisInterpolators = new HashMap<>();
        mReversedAxes = new HashMap<>();
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        mGamepad.publish();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
        mGamepad.unpublish();
        mButtonMappings.clear();
        mAxisMappings.clear();
    }

    @Override
    void clearAllButtonsMappings() {
        mButtonMappings.clear();
    }

    @Override
    void clearAllAxisMappings() {
        mAxisMappings.clear();
    }

    @Override
    void removeButtonsMappingEntry(long uid) {
        mButtonMappings.remove(uid);
    }

    @Override
    void removeAxisMappingEntry(long uid) {
        mAxisMappings.remove(uid);
    }

    @Override
    void addButtonsMappingEntry(long uid, @NonNull Drone.Model droneModel, @NonNull ButtonsMappableAction action,
                                @ButtonMask long buttons) {
        EnumSet<ButtonEvent> buttonEvents = ButtonEvents.eventsFrom(buttons);
        if (buttonEvents != null && !buttonEvents.isEmpty()) {
            mButtonMappings.put(uid, new ButtonsMappingEntry(droneModel, action, buttonEvents));
        } else {
            ULog.w(TAG_GAMEPAD, "Discarding mapping [uid: " + uid + ", model: " + droneModel + ", action: " + action
                                + ", buttons: " + Long.toBinaryString(buttons) + "]");
        }
    }

    @Override
    void addAxisMappingEntry(long uid, @NonNull Drone.Model droneModel, @NonNull AxisMappableAction action,
                             @AxisMask long axis, @ButtonMask long buttons) {
        AxisEvent axisEvent = AxisEvents.eventFrom(axis);
        EnumSet<ButtonEvent> buttonEvents = ButtonEvents.eventsFrom(buttons);
        if (axisEvent != null && buttonEvents != null) {
            mAxisMappings.put(uid, new AxisMappingEntry(droneModel, action, axisEvent, buttonEvents));
        } else {
            ULog.w(TAG_GAMEPAD, "Discarding mapping [uid: " + uid + ", model: " + droneModel + ", action:" + action
                                + ", axis: " + Long.numberOfTrailingZeros(axis)
                                + ", buttons: " + Long.toBinaryString(buttons) + "]");
        }
    }

    @Override
    void updateButtonsMappings() {
        mGamepad.updateButtonsMappings(mButtonMappings.values()).notifyUpdated();
    }

    @Override
    void updateAxisMappings() {
        mGamepad.updateAxisMappings(mAxisMappings.values()).notifyUpdated();
    }

    @Override
    void clearAllAxisInterpolators() {
        mAxisInterpolators.clear();
    }

    @Override
    void removeAxisInterpolatorEntry(long uid) {
        mAxisInterpolators.remove(uid);
    }

    @Override
    void addAxisInterpolatorEntry(long uid, @NonNull Drone.Model droneModel, @AxisMask long axisMask,
                                  @NonNull AxisInterpolator interpolator) {
        SkyController3Gamepad.Axis axis = InputMasks.axisFrom(axisMask);
        if (axis != null) {
            mAxisInterpolators.put(uid, new SkyController3GamepadCore.AxisInterpolatorEntry(
                    droneModel, axis, interpolator));
        }
    }

    @Override
    void updateAxisInterpolators() {
        // axis interpolators also serve to provide the set of supported drone models
        EnumSet<Drone.Model> supportedModels = EnumSet.noneOf(Drone.Model.class);
        for (SkyController3GamepadCore.AxisInterpolatorEntry entry : mAxisInterpolators.values()) {
            supportedModels.add(entry.getDroneModel());
        }
        mGamepad.updateSupportedDroneModels(supportedModels)
                .updateAxisInterpolators(mAxisInterpolators.values())
                .notifyUpdated();
    }

    @Override
    void clearAllReversedAxes() {
        mReversedAxes.clear();
    }

    @Override
    void removeReversedAxisEntry(long uid) {
        mReversedAxes.remove(uid);
    }

    @Override
    void addReversedAxisEntry(long uid, @NonNull Drone.Model droneModel, @AxisMask long axisMask, boolean reversed) {
        SkyController3Gamepad.Axis axis = InputMasks.axisFrom(axisMask);
        if (axis != null) {
            mReversedAxes.put(uid, new SkyController3GamepadCore.ReversedAxisEntry(droneModel, axis, reversed));
        }
    }

    @Override
    void updateReversedAxes() {
        mGamepad.updateReversedAxes(mReversedAxes.values()).notifyUpdated();
    }

    @Override
    void onGrabState(@ButtonMask long buttonsMask, @AxisMask long axesMask, @ButtonMask long buttonStates) {
        // collect grabbed buttons
        EnumSet<SkyController3Gamepad.Button> buttons = EnumSet.noneOf(SkyController3Gamepad.Button.class);
        for (SkyController3Gamepad.Button button : SkyController3Gamepad.Button.values()) {
            InputMasks info = InputMasks.of(button);
            if ((buttonsMask & info.mButtons) != 0 || (axesMask & info.mAxes) != 0) {
                // some of the input's buttons and/or axes are selected, so we consider the input grabbed
                buttons.add(button);
                // however warn if the complete set of buttons/axes is not present
                if ((buttonsMask & info.mButtons) != info.mButtons || (axesMask & info.mAxes) != info.mAxes) {
                    ULog.w(TAG_GAMEPAD, "Missing grabbed buttons/axes for input " + button
                                        + " [buttons: " + Long.toBinaryString(buttonsMask)
                                        + " , axes: " + Long.toBinaryString(axesMask) + "]");
                }
            }
        }
        // collect grabbed axes
        EnumSet<SkyController3Gamepad.Axis> axes = EnumSet.noneOf(SkyController3Gamepad.Axis.class);
        for (SkyController3Gamepad.Axis axis : SkyController3Gamepad.Axis.values()) {
            InputMasks info = InputMasks.of(axis);
            if ((buttonsMask & info.mButtons) != 0 || (axesMask & info.mAxes) != 0) {
                // some of the input's buttons and/or axes are selected, so we consider the input grabbed
                axes.add(axis);
                // however warn if the complete set of buttons/axes is not present
                if ((buttonsMask & info.mButtons) != info.mButtons || (axesMask & info.mAxes) != info.mAxes) {
                    ULog.w(TAG_GAMEPAD, "Missing grabbed buttons/axes for input " + axis
                                        + " [buttons: " + Long.toBinaryString(buttonsMask)
                                        + " , axes: " + Long.toBinaryString(axesMask) + "]");
                }
            }
        }
        // publish state to gamepad
        mGamepad.updateGrabbedInputs(buttons, axes)
                .updateGrabbedButtonEvents(ButtonEvents.statesFrom(buttonsMask, buttonStates))
                .notifyUpdated();
    }

    @Override
    void onButtonEvent(@ButtonMask long button, boolean pressed) {
        ButtonEvent buttonEvent = ButtonEvents.eventFrom(button);
        if (buttonEvent != null) {
            mGamepad.updateGrabbedButtonEvent(buttonEvent,
                    pressed ? ButtonEvent.State.PRESSED : ButtonEvent.State.RELEASED).notifyUpdated();
        }
    }

    @Override
    void onAxisEvent(@AxisMask long axis, @IntRange(from = -100, to = 100) int value) {
        AxisEvent axisEvent = AxisEvents.eventFrom(axis);
        if (axisEvent != null) {
            mGamepad.notifyAxisEvent(axisEvent, value);
        }
    }

    @Override
    void onVolatileMapping(boolean enabled) {
        mGamepad.volatileMapping()
                .updateSupportedFlag(true)
                .updateValue(enabled);
        mGamepad.notifyUpdated();
    }

    @Override
    void processActiveDroneModelChange(@NonNull Drone.Model droneModel) {
        mGamepad.updateActiveDroneModel(droneModel).notifyUpdated();
    }

    /** Backend of SkyController3GamepadCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final SkyController3GamepadCore.Backend mBackend = new SkyController3GamepadCore.Backend() {

        @Override
        public void setupMappingEntry(@NonNull MappingEntry mappingEntry, boolean register) {
            Drone.Model droneModel = mappingEntry.getDroneModel();
            @ButtonMask long buttonsMask = 0;
            switch (mappingEntry.getType()) {
                case BUTTONS_MAPPING:
                    ButtonsMappingEntry buttonsEntry = mappingEntry.as(ButtonsMappingEntry.class);
                    ButtonsMappableAction buttonsAction = buttonsEntry.getAction();
                    if (register) {
                        buttonsMask = ButtonEvents.maskFrom(buttonsEntry.getButtonEvents());
                    }
                    setupButtonsMappingEntry(droneModel, buttonsAction, buttonsMask);
                    break;
                case AXIS_MAPPING:
                    AxisMappingEntry axisEntry = mappingEntry.as(AxisMappingEntry.class);
                    AxisMappableAction axisAction = axisEntry.getAction();
                    @AxisMask long axisMask = 0;
                    if (register) {
                        axisMask = AxisEvents.maskFrom(axisEntry.getAxisEvent());
                        buttonsMask = ButtonEvents.maskFrom(axisEntry.getButtonEvents());
                    }
                    setupAxisMapping(droneModel, axisAction, axisMask, buttonsMask);
                    break;
            }
        }

        @Override
        public void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull SkyController3Gamepad.Axis axis,
                                        @NonNull AxisInterpolator interpolator) {
            Sc3Gamepad.this.setAxisInterpolator(droneModel, InputMasks.of(axis).mAxes, interpolator);
        }

        @Override
        public void setReversedAxis(@NonNull Drone.Model droneModel, @NonNull SkyController3Gamepad.Axis axis,
                                    boolean reversed) {
            Sc3Gamepad.this.setReversedAxis(droneModel, InputMasks.of(axis).mAxes, reversed);
        }

        @Override
        public void resetMappings(@Nullable Drone.Model model) {
            Sc3Gamepad.this.resetMappings(model);
        }

        @Override
        public void setGrabbedInputs(@NonNull Set<SkyController3Gamepad.Button> buttons,
                                     @NonNull Set<SkyController3Gamepad.Axis> axes) {
            InputMasks info = InputMasks.collect(buttons, axes);
            grab(info.mButtons, info.mAxes);
        }

        @Override
        public boolean setVolatileMapping(boolean enable) {
            enableVolatileMapping(enable);
            return true;
        }
    };

    /** Translates mapper buttons/axes masks to navigation events for SkyController3 VirtualGamepad implementation. */
    private static final class Translator implements NavigationEventTranslator {

        /**
         * Mask of buttons to use to grab navigation.
         * <p>
         * These are the left stick left/right/up/down buttons (nav), plus right buttons 2 & 3 (cancel/ok).
         */
        @ButtonMask
        private static final long BUTTONS_MASK =
                MASK_BUTTON_4 | MASK_BUTTON_5 | MASK_BUTTON_6 | MASK_BUTTON_7 | MASK_BUTTON_2 | MASK_BUTTON_3;

        /**
         * Mask of axes to use to grab navigation.
         * <p>
         * These are the left stick horizontal & vertical axes.
         */
        @AxisMask
        private static final long AXES_MASK = MASK_AXIS_0 | MASK_AXIS_1;

        @ButtonMask
        @Override
        public long getNavigationGrabButtonsMask() {
            return BUTTONS_MASK;
        }

        @AxisMask
        @Override
        public long getNavigationGrabAxesMask() {
            return AXES_MASK;
        }

        @Override
        @Nullable
        public VirtualGamepad.Event eventFrom(@ButtonMask long buttonMask) {
            VirtualGamepad.Event event = EVENTS.get(buttonMask);
            if (event == null) {
                ULog.w(TAG_GAMEPAD, "Not a navigation button: " + Long.toBinaryString(buttonMask));
            }
            return event;
        }

        /** VirtualGamepad navigation events, by Mapper button mask. */
        private static final LongSparseArray<VirtualGamepad.Event> EVENTS = new LongSparseArray<>();

        static {
            EVENTS.put(MASK_BUTTON_4, VirtualGamepad.Event.LEFT);
            EVENTS.put(MASK_BUTTON_5, VirtualGamepad.Event.RIGHT);
            EVENTS.put(MASK_BUTTON_6, VirtualGamepad.Event.UP);
            EVENTS.put(MASK_BUTTON_7, VirtualGamepad.Event.DOWN);
            EVENTS.put(MASK_BUTTON_2, VirtualGamepad.Event.CANCEL);
            EVENTS.put(MASK_BUTTON_3, VirtualGamepad.Event.OK);
        }
    }

    /** Converts mapper buttons to/from ButtonEvent. */
    @VisibleForTesting
    static final class ButtonEvents {

        /**
         * Translates a Mapper button to its gamepad button event equivalent.
         *
         * @param buttonMask mask of the Mapper button to convert
         *
         * @return the corresponding button event, or {@code null} if the provided mapper button cannot be translated
         */
        @Nullable
        static ButtonEvent eventFrom(@ButtonMask long buttonMask) {
            ButtonEvent buttonEvent = GSDK_BUTTON_EVENTS.get(buttonMask);
            if (buttonEvent == null) {
                ULog.w(TAG_GAMEPAD, "Unsupported button " + Long.toBinaryString(buttonMask));
            }
            return buttonEvent;
        }

        /**
         * Translates Mapper buttons to their gamepad button events equivalent.
         *
         * @param buttonsMask mask of Mapper buttons to convert
         *
         * @return a new set containing the corresponding button events, or {@code null} if <strong>ANY</strong> of the
         *         provided mapper buttons cannot be translated
         */
        @Nullable
        static EnumSet<ButtonEvent> eventsFrom(@ButtonMask long buttonsMask) {
            EnumSet<ButtonEvent> buttonEvents = EnumSet.noneOf(ButtonEvent.class);
            while (buttonsMask != 0) {
                @ButtonMask long buttonMask = Long.lowestOneBit(buttonsMask);
                ButtonEvent buttonEvent = eventFrom(buttonMask);
                if (buttonEvent == null) {
                    return null;
                }
                buttonEvents.add(buttonEvent);
                buttonsMask ^= buttonMask;
            }
            return buttonEvents;
        }

        /**
         * Translates Mapper buttons and their press states to their gamepad button events equivalent (with their button
         * event state).
         * <p>
         * Note that unlike {@link #eventsFrom(long)} method, this does not return null if one of the provided mapper
         * buttons cannot be translated. Instead, there will simply not be any entry for that button in the returned
         * map.
         *
         * @param buttonsMask mask of Mapper buttons to convert
         * @param statesMask  mask of pressed Mapper buttons
         *
         * @return a new map of the corresponding button event states, by button event.
         */
        @NonNull
        static EnumMap<ButtonEvent, ButtonEvent.State> statesFrom(@ButtonMask long buttonsMask,
                                                                  @ButtonMask long statesMask) {
            EnumMap<ButtonEvent, ButtonEvent.State> states = new EnumMap<>(ButtonEvent.class);
            while (buttonsMask != 0) {
                @ButtonMask long buttonMask = Long.lowestOneBit(buttonsMask);
                ButtonEvent buttonEvent = eventFrom(buttonMask);
                if (buttonEvent != null) {
                    states.put(buttonEvent,
                            (statesMask & buttonMask) == 0 ? ButtonEvent.State.RELEASED : ButtonEvent.State.PRESSED);
                }
                buttonsMask ^= buttonMask;
            }
            return states;
        }

        /**
         * Translates gamepad ButtonEvents to their Mapper buttons equivalent.
         *
         * @param buttonEvents set of gamepad ButtonEvents to convert
         *
         * @return a mask of the corresponding Mapper buttons
         */
        @ButtonMask
        static long maskFrom(@NonNull Set<ButtonEvent> buttonEvents) {
            @ButtonMask long buttonsMask = 0;
            for (ButtonEvent buttonEvent : buttonEvents) {
                @ButtonMask long buttonMask = ARSDK_BUTTON_MASKS.get(buttonEvent.ordinal());
                buttonsMask |= buttonMask;
            }
            return buttonsMask;
        }

        /**
         * Private constructor for static utility class.
         */
        private ButtonEvents() {
        }

        /** ARSDK button mask, by GSDK ButtonEvent ordinal. */
        private static final SparseArray<Long> ARSDK_BUTTON_MASKS = new SparseArray<>();

        /** GSDK ButtonEvent, by ARSDK button mask. */
        private static final LongSparseArray<ButtonEvent> GSDK_BUTTON_EVENTS = new LongSparseArray<>();

        /**
         * Maps an ARSDK button and a GSDK ButtonEvent together, both ways.
         *
         * @param buttonMask  mask of ARSDK button to map
         * @param buttonEvent GSDK ButtonEvent to map
         */
        private static void map(@ButtonMask long buttonMask, @NonNull ButtonEvent buttonEvent) {
            GSDK_BUTTON_EVENTS.put(buttonMask, buttonEvent);
            ARSDK_BUTTON_MASKS.put(buttonEvent.ordinal(), buttonMask);
        }

        static {
            map(MASK_BUTTON_0, ButtonEvent.FRONT_TOP_BUTTON);
            map(MASK_BUTTON_1, ButtonEvent.FRONT_BOTTOM_BUTTON);
            map(MASK_BUTTON_2, ButtonEvent.REAR_LEFT_BUTTON);
            map(MASK_BUTTON_3, ButtonEvent.REAR_RIGHT_BUTTON);
            map(MASK_BUTTON_4, ButtonEvent.LEFT_STICK_LEFT);
            map(MASK_BUTTON_5, ButtonEvent.LEFT_STICK_RIGHT);
            map(MASK_BUTTON_6, ButtonEvent.LEFT_STICK_UP);
            map(MASK_BUTTON_7, ButtonEvent.LEFT_STICK_DOWN);
            map(MASK_BUTTON_8, ButtonEvent.RIGHT_STICK_LEFT);
            map(MASK_BUTTON_9, ButtonEvent.RIGHT_STICK_RIGHT);
            map(MASK_BUTTON_10, ButtonEvent.RIGHT_STICK_UP);
            map(MASK_BUTTON_11, ButtonEvent.RIGHT_STICK_DOWN);
            map(MASK_BUTTON_12, ButtonEvent.LEFT_SLIDER_DOWN);
            map(MASK_BUTTON_13, ButtonEvent.LEFT_SLIDER_UP);
            map(MASK_BUTTON_14, ButtonEvent.RIGHT_SLIDER_UP);
            map(MASK_BUTTON_15, ButtonEvent.RIGHT_SLIDER_DOWN);
        }
    }

    /** Converts mapper axis to/from AxisEvent. */
    @VisibleForTesting
    static final class AxisEvents {

        /**
         * Translates a Mapper axis to its gamepad axis event equivalent.
         *
         * @param axisMask mask of the Mapper axis to convert
         *
         * @return the corresponding axis event, or {@code null} if the provided mapper axis cannot be translated
         */
        @Nullable
        static AxisEvent eventFrom(@AxisMask long axisMask) {
            AxisEvent axisEvent = GSDK_AXIS_EVENTS.get(axisMask);
            if (axisEvent == null) {
                ULog.w(TAG_GAMEPAD, "Unsupported axis " + Long.toBinaryString(axisMask));
            }
            return axisEvent;
        }

        /**
         * Translates a gamepad AxisEvent to its Mapper axis equivalent.
         *
         * @param axisEvent gamepad AxisEvent to convert
         *
         * @return a mask of the corresponding Mapper axis
         */
        @AxisMask
        static long maskFrom(@NonNull AxisEvent axisEvent) {
            @AxisMask long axisMask = ARSDK_AXIS_MASKS.get(axisEvent.ordinal());
            return axisMask;
        }

        /**
         * Private constructor for static utility class.
         */
        private AxisEvents() {
        }

        /** ARSDK axis mask, by GSDK AxisEvent ordinal. */
        private static final SparseArray<Long> ARSDK_AXIS_MASKS = new SparseArray<>();

        /** GSDK AxisEvent, by ARSDK axis mask. */
        private static final LongSparseArray<AxisEvent> GSDK_AXIS_EVENTS = new LongSparseArray<>();

        /**
         * Maps an ARSDK axis and a GSDK AxisEvent together, both ways.
         *
         * @param axisMask  mask of ARSDK axis to map
         * @param axisEvent GSDK AxisEvent to map
         */
        private static void map(@AxisMask long axisMask, @NonNull AxisEvent axisEvent) {
            GSDK_AXIS_EVENTS.put(axisMask, axisEvent);
            ARSDK_AXIS_MASKS.put(axisEvent.ordinal(), axisMask);
        }

        static {
            map(MASK_AXIS_0, AxisEvent.LEFT_STICK_HORIZONTAL);
            map(MASK_AXIS_1, AxisEvent.LEFT_STICK_VERTICAL);
            map(MASK_AXIS_2, AxisEvent.RIGHT_STICK_HORIZONTAL);
            map(MASK_AXIS_3, AxisEvent.RIGHT_STICK_VERTICAL);
            map(MASK_AXIS_4, AxisEvent.LEFT_SLIDER);
            map(MASK_AXIS_5, AxisEvent.RIGHT_SLIDER);
        }
    }

    /** Converts mapper buttons/axes to/from gamepad Input. */
    @VisibleForTesting
    static final class InputMasks {

        /** Mask of all Mapper buttons the associated Input represents. */
        @ButtonMask
        final long mButtons;

        /** Mask of all Mapper axes the associated Input represents. */
        @AxisMask
        final long mAxes;

        /**
         * Gets Mapper buttons/axes associated to a gamepad button.
         *
         * @param button gamepad button to get mask info for
         *
         * @return the button's info structure, containing the corresponding Mapper buttons and/or axes masks
         */
        @NonNull
        static InputMasks of(@NonNull SkyController3Gamepad.Button button) {
            //noinspection ConstantConditions: map is complete
            return ARSDK_BUTTONS_MASKS.get(button);
        }

        /**
         * Gets Mapper buttons/axes associated to a gamepad axis.
         *
         * @param axis gamepad axis to get mask info for
         *
         * @return the axis' info structure, containing the corresponding Mapper buttons and/or axes masks
         */
        @NonNull
        static InputMasks of(@NonNull SkyController3Gamepad.Axis axis) {
            //noinspection ConstantConditions: map is complete
            return ARSDK_AXES_MASKS.get(axis);
        }

        /**
         * Gets a SkyController3 gamepad axis from its ARSDK Mapper mask representation.
         *
         * @param axisMask mask of the axis to retrieve.
         *
         * @return the corresponding gamepad axis, or {@code null} if the mask does not correspond to any known axis
         */
        @Nullable
        static SkyController3Gamepad.Axis axisFrom(@AxisMask long axisMask) {
            SkyController3Gamepad.Axis axis = GSDK_AXES.get(axisMask);
            if (axis == null) {
                ULog.w(TAG_GAMEPAD, "Unsupported axis " + Long.toBinaryString(axisMask));
            }
            return axis;
        }

        /**
         * Collects all Mapper buttons/axes associated to multiple gamepad inputs.
         *
         * @param buttons set of gamepad buttons to get mask info for
         * @param axes    set of gamepad axes to get mask info for
         *
         * @return an info structure containing buttons and/or axes masks corresponding to all the provided inputs
         */
        static InputMasks collect(@NonNull Set<SkyController3Gamepad.Button> buttons,
                                  @NonNull Set<SkyController3Gamepad.Axis> axes) {
            @ButtonMask long buttonsMask = 0;
            @AxisMask long axesMask = 0;

            for (SkyController3Gamepad.Button button : buttons) {
                InputMasks info = of(button);
                buttonsMask |= info.mButtons;
                axesMask |= info.mAxes;
            }

            for (SkyController3Gamepad.Axis axis : axes) {
                InputMasks info = of(axis);
                buttonsMask |= info.mButtons;
                axesMask |= info.mAxes;
            }

            return new InputMasks(buttonsMask, axesMask);
        }

        /**
         * Constructor.
         *
         * @param buttons mask of Mapper buttons
         * @param axes    mask of Mapper axes
         */
        private InputMasks(@ButtonMask long buttons, @AxisMask long axes) {
            mButtons = buttons;
            mAxes = axes;
        }

        /** InputInfo, by gamepad Button. */
        private static final EnumMap<SkyController3Gamepad.Button, InputMasks> ARSDK_BUTTONS_MASKS =
                new EnumMap<>(SkyController3Gamepad.Button.class);

        /** InputInfo, by gamepad Axis. */
        private static final EnumMap<SkyController3Gamepad.Axis, InputMasks> ARSDK_AXES_MASKS =
                new EnumMap<>(SkyController3Gamepad.Axis.class);

        /** GSDK Axis, by ARSDK axis mask. */
        private static final LongSparseArray<SkyController3Gamepad.Axis> GSDK_AXES = new LongSparseArray<>();

        /**
         * Maps a GSDK gamepad button to a mask of ARSDK Mapper buttons.
         *
         * @param button      gamepad button to map
         * @param buttonsMask mask of ARSDK Mapper buttons to associate with the input
         */
        private static void map(@NonNull SkyController3Gamepad.Button button, @ButtonMask long buttonsMask) {
            ARSDK_BUTTONS_MASKS.put(button, new InputMasks(buttonsMask, 0));
        }

        /**
         * Maps a GSDK gamepad axis to a mask of ARSDK Mapper buttons and a mask of ARSDK Mapper axes.
         *
         * @param axis        gamepad axis to map
         * @param buttonsMask mask of ARSDK Mapper buttons to associate with the input
         * @param axesMask    mask of ARSDK Mapper axes to associate with the input
         */
        private static void map(@NonNull SkyController3Gamepad.Axis axis, @ButtonMask long buttonsMask,
                                @AxisMask long axesMask) {
            ARSDK_AXES_MASKS.put(axis, new InputMasks(buttonsMask, axesMask));
            GSDK_AXES.put(axesMask, axis);
        }

        static {
            map(SkyController3Gamepad.Button.FRONT_TOP_BUTTON, MASK_BUTTON_0);
            map(SkyController3Gamepad.Button.FRONT_BOTTOM_BUTTON, MASK_BUTTON_1);
            map(SkyController3Gamepad.Button.REAR_LEFT_BUTTON, MASK_BUTTON_2);
            map(SkyController3Gamepad.Button.REAR_RIGHT_BUTTON, MASK_BUTTON_3);
            map(SkyController3Gamepad.Axis.LEFT_STICK_HORIZONTAL, MASK_BUTTON_4 | MASK_BUTTON_5, MASK_AXIS_0);
            map(SkyController3Gamepad.Axis.LEFT_STICK_VERTICAL, MASK_BUTTON_6 | MASK_BUTTON_7, MASK_AXIS_1);
            map(SkyController3Gamepad.Axis.RIGHT_STICK_HORIZONTAL, MASK_BUTTON_8 | MASK_BUTTON_9, MASK_AXIS_2);
            map(SkyController3Gamepad.Axis.RIGHT_STICK_VERTICAL, MASK_BUTTON_10 | MASK_BUTTON_11, MASK_AXIS_3);
            map(SkyController3Gamepad.Axis.LEFT_SLIDER, MASK_BUTTON_12 | MASK_BUTTON_13, MASK_AXIS_4);
            map(SkyController3Gamepad.Axis.RIGHT_SLIDER, MASK_BUTTON_14 | MASK_BUTTON_15, MASK_AXIS_5);
        }
    }
}
