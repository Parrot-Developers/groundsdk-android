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
import android.util.SparseArray;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.RCController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.RCPeripheralController;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.groundsdk.internal.device.peripheral.gamepad.VirtualGamepadCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMapper;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_GAMEPAD;

/**
 * Abstract VirtualGamePad base implementation for SkyController family remote controls.
 * <p>
 * Handles mapper command and callbacks. Publishes and manages the VirtualGamepad component.
 * <p>
 * Must be subclassed by a specific gamepad implementation that translates to and from mapper events.
 */
abstract class GamepadControllerBase extends RCPeripheralController {

    /**
     * Long definition for all Mapper button mask values.
     */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, value = {
            MASK_BUTTON_0, MASK_BUTTON_1, MASK_BUTTON_2, MASK_BUTTON_3, MASK_BUTTON_4, MASK_BUTTON_5, MASK_BUTTON_6,
            MASK_BUTTON_7, MASK_BUTTON_8, MASK_BUTTON_9, MASK_BUTTON_10, MASK_BUTTON_11, MASK_BUTTON_12, MASK_BUTTON_13,
            MASK_BUTTON_14, MASK_BUTTON_15, MASK_BUTTON_16, MASK_BUTTON_17, MASK_BUTTON_18, MASK_BUTTON_19,
            MASK_BUTTON_20, MASK_BUTTON_21, MASK_BUTTON_22, MASK_BUTTON_23})
    @interface ButtonMask {}

    /** Button 0. */
    @SuppressWarnings("PointlessBitwiseExpression")
    static final long MASK_BUTTON_0 = 1 << 0;

    /** Button 1. */
    static final long MASK_BUTTON_1 = 1 << 1;

    /** Button 2. */
    static final long MASK_BUTTON_2 = 1 << 2;

    /** Button 3. */
    static final long MASK_BUTTON_3 = 1 << 3;

    /** Button 4. */
    static final long MASK_BUTTON_4 = 1 << 4;

    /** Button 5. */
    static final long MASK_BUTTON_5 = 1 << 5;

    /** Button 6. */
    static final long MASK_BUTTON_6 = 1 << 6;

    /** Button 7. */
    static final long MASK_BUTTON_7 = 1 << 7;

    /** Button 8. */
    static final long MASK_BUTTON_8 = 1 << 8;

    /** Button 9. */
    static final long MASK_BUTTON_9 = 1 << 9;

    /** Button 10. */
    static final long MASK_BUTTON_10 = 1 << 10;

    /** Button 11. */
    static final long MASK_BUTTON_11 = 1 << 11;

    /** Button 12. */
    static final long MASK_BUTTON_12 = 1 << 12;

    /** Button 13. */
    static final long MASK_BUTTON_13 = 1 << 13;

    /** Button 14. */
    static final long MASK_BUTTON_14 = 1 << 14;

    /** Button 15. */
    static final long MASK_BUTTON_15 = 1 << 15;

    /** Button 16. */
    static final long MASK_BUTTON_16 = 1 << 16;

    /** Button 17. */
    static final long MASK_BUTTON_17 = 1 << 17;

    /** Button 18. */
    static final long MASK_BUTTON_18 = 1 << 18;

    /** Button 19. */
    static final long MASK_BUTTON_19 = 1 << 19;

    /** Button 20. */
    static final long MASK_BUTTON_20 = 1 << 20;

    /** Button 21. */
    static final long MASK_BUTTON_21 = 1 << 21;

    /** Button 22. */
    static final long MASK_BUTTON_22 = 1 << 22;

    /** Button 23. */
    static final long MASK_BUTTON_23 = 1 << 23;

    /**
     * Long definition for all Mapper axis mask values.
     */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, value = {
            MASK_AXIS_0, MASK_AXIS_1, MASK_AXIS_2, MASK_AXIS_3, MASK_AXIS_4, MASK_AXIS_5, MASK_AXIS_6, MASK_AXIS_7
    })
    @interface AxisMask {}

    /** Axis 0. */
    @SuppressWarnings("PointlessBitwiseExpression")
    static final long MASK_AXIS_0 = 1 << 0;

    /** Axis 1. */
    static final long MASK_AXIS_1 = 1 << 1;

    /** Axis 2. */
    static final long MASK_AXIS_2 = 1 << 2;

    /** Axis 3. */
    static final long MASK_AXIS_3 = 1 << 3;

    /** Axis 4. */
    static final long MASK_AXIS_4 = 1 << 4;

    /** Axis 4. */
    static final long MASK_AXIS_5 = 1 << 5;

    /** Axis 4. */
    static final long MASK_AXIS_6 = 1 << 6;

    /** Axis 4. */
    static final long MASK_AXIS_7 = 1 << 7;

    /**
     * Translates mapper buttons/axes masks to/from VirtualGamepad navigation events.
     */
    interface NavigationEventTranslator {

        /**
         * Gets the buttons mask to be used to grab navigation.
         *
         * @return navigation buttons mask
         */
        @ButtonMask
        long getNavigationGrabButtonsMask();

        /**
         * Gets the axes mask to be used to grab navigation.
         *
         * @return navigation axes mask
         */
        @AxisMask
        long getNavigationGrabAxesMask();

        /**
         * Convert the given button mask to a VirtualGamepad navigation event.
         *
         * @param buttonMask button mask to convert
         *
         * @return translated navigation event
         */
        @Nullable
        VirtualGamepad.Event eventFrom(@ButtonMask long buttonMask);
    }

    /** The VirtualGamepad peripheral for which this object is the backend. */
    @NonNull
    private final VirtualGamepadCore mVirtualGamepad;

    /** Translates navigation events to/from mapper buttons/axes masks. */
    @NonNull
    private final NavigationEventTranslator mNavEventTranslator;

    /** {@code true} when the latest grab request was performed on behalf of VirtualGamepad. */
    private boolean mGrabRequestFromVirtualGamepad;

    /** {@code true} when the VirtualGamepad is currently preempted. */
    private boolean mVirtualGamepadPreempted;

    /** {@code true} when the VirtualGamepad is currently grabbed, or would be grabbed if not preempted. */
    private boolean mVirtualGamepadGrabbed;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     * @param translator       the translator used to convert navigation events to mapper masks
     */
    GamepadControllerBase(@NonNull RCController deviceController, @NonNull NavigationEventTranslator translator) {
        super(deviceController);
        mNavEventTranslator = translator;
        mVirtualGamepad = new VirtualGamepadCore(mComponentStore, mBackend);
    }

    @CallSuper
    @Override
    protected void onConnected() {
        mVirtualGamepad.publish();
    }

    @CallSuper
    @Override
    protected void onDisconnected() {
        mVirtualGamepad.unpublish();
        mGrabRequestFromVirtualGamepad = mVirtualGamepadGrabbed = mVirtualGamepadPreempted = false;
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureMapper.UID) {
            ArsdkFeatureMapper.decode(command, mMapperCallbacks);
        }
    }

    /**
     * Registers a buttons mapping entry in the remote control.
     *
     * @param droneModel drone model the mapping entry applies on
     * @param action     buttons action the mapping entry triggers
     * @param buttons    mask of buttons that triggers the action, or 0 to unregister the mapping instead
     */
    final void setupButtonsMappingEntry(@NonNull Drone.Model droneModel, @NonNull ButtonsMappableAction action,
                                        @ButtonMask long buttons) {
        sendCommand(ArsdkFeatureMapper.encodeMapButtonAction(droneModel.id(), Actions.convert(action), buttons));
    }

    /**
     * Registers an axis mapping entry in the remote control.
     *
     * @param droneModel drone model the mapping entry applies on
     * @param action     axis action the mapping entry triggers
     * @param axis       mask of the axis that triggers the action, or 0 to unregister the mapping instead
     * @param buttons    mask of buttons that triggers the action
     */
    final void setupAxisMapping(@NonNull Drone.Model droneModel, @NonNull AxisMappableAction action,
                                @AxisMask long axis, @ButtonMask long buttons) {
        sendCommand(ArsdkFeatureMapper.encodeMapAxisAction(droneModel.id(), Actions.convert(action),
                axis == 0 ? -1 : Long.numberOfTrailingZeros(axis), buttons));
    }

    /**
     * Requests buttons and/or axes to be grabbed.
     * <p>
     * To be used when the request is on behalf of a specialized gamepad interface (versus VirtualGamepad interface).
     *
     * @param buttons mask of buttons to grab
     * @param axes    mask of axes to grab
     */
    final void grab(@ButtonMask long buttons, @AxisMask long axes) {
        grab(buttons, axes, false);
    }

    /**
     * Configures an axis interpolator.
     *
     * @param droneModel   drone model onto which the interpolator applies
     * @param axisMask     mask of the axis onto which the interpolator applies
     * @param interpolator interpolator to set for the given axis and drone model
     */
    final void setAxisInterpolator(@NonNull Drone.Model droneModel, @AxisMask long axisMask,
                                   @NonNull AxisInterpolator interpolator) {
        ArsdkFeatureMapper.ExpoType expoType = null;
        switch (interpolator) {
            case LINEAR:
                expoType = ArsdkFeatureMapper.ExpoType.LINEAR;
                break;
            case LIGHT_EXPONENTIAL:
                expoType = ArsdkFeatureMapper.ExpoType.EXPO_0;
                break;
            case MEDIUM_EXPONENTIAL:
                expoType = ArsdkFeatureMapper.ExpoType.EXPO_1;
                break;
            case STRONG_EXPONENTIAL:
                expoType = ArsdkFeatureMapper.ExpoType.EXPO_2;
                break;
            case STRONGEST_EXPONENTIAL:
                expoType = ArsdkFeatureMapper.ExpoType.EXPO_4;
                break;
        }
        sendCommand(ArsdkFeatureMapper.encodeSetExpo(droneModel.id(), Long.numberOfTrailingZeros(axisMask), expoType));
    }

    /**
     * Configures axis inversion.
     *
     * @param droneModel drone model onto which the axis inversion applies
     * @param axisMask   mask of the axis onto which the inversion applies
     * @param reversed   {@code true} to make the axis reversed, {@code false} to make it operate normally
     */
    final void setReversedAxis(@NonNull Drone.Model droneModel, @AxisMask long axisMask, boolean reversed) {
        sendCommand(ArsdkFeatureMapper.encodeSetInverted(droneModel.id(), Long.numberOfTrailingZeros(axisMask),
                reversed ? 1 : 0));
    }

    /**
     * Resets mappings to defaults.
     *
     * @param model drone model for which the mapping should be reset, or {@code null} to reset all models' mappings
     */
    final void resetMappings(@Nullable Drone.Model model) {
        sendCommand(ArsdkFeatureMapper.encodeResetMapping(model == null ? 0 : model.id()));
    }

    /**
     * Enables or disables volatile mapping.
     *
     * @param enable {@code true} to enable volatile mapping, {@code false} to disable it
     */
    final void enableVolatileMapping(boolean enable) {
        if (enable) {
            sendCommand(ArsdkFeatureMapper.encodeEnterVolatileMapping());
        } else {
            sendCommand(ArsdkFeatureMapper.encodeExitVolatileMapping());
        }
    }

    /**
     * Processes a change of the active drone model.
     *
     * @param droneModel new active drone model
     */
    abstract void processActiveDroneModelChange(@NonNull Drone.Model droneModel);

    /**
     * Clears all known buttons mappings.
     */
    abstract void clearAllButtonsMappings();

    /**
     * Clears all known axes mappings.
     */
    abstract void clearAllAxisMappings();

    /**
     * Removes a buttons mapping entry.
     *
     * @param uid uid of the buttons mapping entry to be removed
     */
    abstract void removeButtonsMappingEntry(long uid);

    /**
     * Removes an axis mapping entry.
     *
     * @param uid uid of the axis mapping entry to be removed
     */
    abstract void removeAxisMappingEntry(long uid);

    /**
     * Adds a buttons mapping entry.
     *
     * @param uid        uid of the mapping entry
     * @param droneModel drone model the entry applies on
     * @param action     buttons action the mapping entry triggers
     * @param buttons    mask of buttons that triggers the action
     */
    abstract void addButtonsMappingEntry(long uid, @NonNull Drone.Model droneModel,
                                         @NonNull ButtonsMappableAction action, @ButtonMask long buttons);

    /**
     * Adds an axis mapping entry.
     *
     * @param uid        uid of the mapping entry
     * @param droneModel drone model the entry applies on
     * @param action     axis action the mapping entry triggers
     * @param axis       mask of the axis that triggers the action
     * @param buttons    mask of buttons that triggers the action
     */
    abstract void addAxisMappingEntry(long uid, @NonNull Drone.Model droneModel, @NonNull AxisMappableAction action,
                                      @AxisMask long axis, @ButtonMask long buttons);

    /**
     * Synchronizes all known button mappings with the public gamepad interface.
     */
    abstract void updateButtonsMappings();

    /**
     * Synchronizes all known axes mappings with the public gamepad interface.
     */
    abstract void updateAxisMappings();

    /**
     * Clears all known axis interpolators.
     */
    abstract void clearAllAxisInterpolators();

    /**
     * Removes an axis interpolator entry.
     *
     * @param uid uid of the axis interpolator entry to be removed
     */
    abstract void removeAxisInterpolatorEntry(long uid);

    /**
     * Adds an axis interpolator entry.
     *
     * @param uid          uid of the axis interpolator entry
     * @param droneModel   drone model the entry applies on
     * @param axisMask     mask of the axis the entry applies on
     * @param interpolator interpolator to set for this entry
     */
    abstract void addAxisInterpolatorEntry(long uid, @NonNull Drone.Model droneModel, @AxisMask long axisMask,
                                           @NonNull AxisInterpolator interpolator);

    /**
     * Synchronizes all known axis interpolators with the public gamepad interface.
     */
    abstract void updateAxisInterpolators();

    /**
     * Clears all known reversed axes.
     */
    abstract void clearAllReversedAxes();

    /**
     * Removes a reversed axis entry.
     *
     * @param uid uid of the reversed axis entry to be removed
     */
    abstract void removeReversedAxisEntry(long uid);

    /**
     * Adds a reversed axis entry.
     *
     * @param uid        uid of the reversed axis entry
     * @param droneModel drone model the entry applies on
     * @param axisMask   mask of the axis the entry applies on
     * @param reversed   {@code true} for an inverted axis, {@code false} for a non-inverted axis
     */
    abstract void addReversedAxisEntry(long uid, @NonNull Drone.Model droneModel, @AxisMask long axisMask,
                                       boolean reversed);

    /**
     * Synchronizes all known reversed axes with the public gamepad interface.
     */
    abstract void updateReversedAxes();

    /**
     * Processes a new grab state.
     *
     * @param buttonsMask  mask of all grabbed buttons
     * @param axesMask     mask of all grabbed axes
     * @param buttonStates mask of all pressed buttons
     */
    abstract void onGrabState(@ButtonMask long buttonsMask, @AxisMask long axesMask, @ButtonMask long buttonStates);

    /**
     * Processes a button event.
     *
     * @param button  mask of the button that triggered an event
     * @param pressed {@code true} when the button is pressed, otherwise {@code false}
     */
    abstract void onButtonEvent(@ButtonMask long button, boolean pressed);

    /**
     * Processes an axis event.
     *
     * @param axis  mask of the axis that triggered an event
     * @param value current axis value
     */
    abstract void onAxisEvent(@AxisMask long axis, @IntRange(from = -100, to = 100) int value);

    /**
     * Notifies reception of volatile mapping state.
     *
     * @param enabled {@code true} when volatile mapping is enabled, otherwise {@code false}
     */
    abstract void onVolatileMapping(boolean enabled);

    /** Backend of VirtualGamepadCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final VirtualGamepadCore.Backend mBackend = new VirtualGamepadCore.Backend() {

        @Override
        public boolean grabNavigation() {
            if (!mVirtualGamepadGrabbed && !mVirtualGamepadPreempted) {
                grab(mNavEventTranslator.getNavigationGrabButtonsMask(),
                        mNavEventTranslator.getNavigationGrabAxesMask(), true);
                return true;
            }
            return false;
        }

        @Override
        public void releaseNavigation() {
            if (mVirtualGamepadGrabbed && !mVirtualGamepadPreempted) {
                grab(0, 0, true);
            }
            mVirtualGamepadGrabbed = false;
            mVirtualGamepad.updateGrabbed(false).notifyUpdated();
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureMapper is decoded. */
    private final ArsdkFeatureMapper.Callback mMapperCallbacks = new ArsdkFeatureMapper.Callback() {

        @Override
        public void onGrabState(@ButtonMask long buttons, @AxisMask long axes, @ButtonMask long buttonsState) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onGrabState [buttons: " + Long.toBinaryString(buttons)
                                    + ", axes: " + Long.toBinaryString(axes)
                                    + ", buttonsState: " + Long.toBinaryString(buttonsState) + "]");
            }
            processGrabState(buttons, axes, buttonsState);
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onGrabButtonEvent(long buttonCode, @Nullable ArsdkFeatureMapper.ButtonEvent event) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onGrabButtonEvent [buttonCode: " + buttonCode + ", event: " + event + "]");
            }
            if (event != null) {
                processButtonEvent(1 << buttonCode, event == ArsdkFeatureMapper.ButtonEvent.PRESS);
            }
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onGrabAxisEvent(long axisCode, int value) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onGrabAxisEvent [axisCode: " + axisCode + ", value: " + value + "]");
            }
            processAxisEvent(1 << axisCode, value);
        }

        @Override
        public void onButtonMappingItem(long uid, @DeviceModel.Id int product,
                                        @Nullable ArsdkFeatureMapper.ButtonAction action, @ButtonMask long buttons,
                                        int listFlagsBitField) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onButtonMappingItem [uid: " + uid + ", product: " + product
                                    + ", action: " + action + ", buttons: " + Long.toBinaryString(buttons)
                                    + ", listFlags: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlagsBitField)
                                    + "]");
            }

            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField);
            if (clearList) {
                clearAllButtonsMappings();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                removeButtonsMappingEntry(uid);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    clearAllButtonsMappings();
                }
                Drone.Model droneModel = DeviceModels.droneModel(product);
                ButtonsMappableAction buttonsAction = Actions.convert(action);
                if (droneModel == null || buttonsAction == null) {
                    ULog.w(TAG_GAMEPAD, "Invalid product or action, dropping mapping [uid: " + uid
                                        + ", product: " + product + ", action: " + action + "]");
                } else {
                    addButtonsMappingEntry(uid, droneModel, buttonsAction, buttons);
                }
            }

            if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                updateButtonsMappings();
            }
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onAxisMappingItem(long uid, @DeviceModel.Id int product,
                                      @Nullable ArsdkFeatureMapper.AxisAction action, int axisCode,
                                      @ButtonMask long buttons, int listFlagsBitField) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onAxisMappingItem [uid: " + uid + ", product: " + product
                                    + ", axis: " + axisCode + ", action: " + action
                                    + ", buttons: " + Long.toBinaryString(buttons)
                                    + "listFlags: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlagsBitField)
                                    + "]");
            }

            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField);
            if (clearList) {
                clearAllAxisMappings();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                removeAxisMappingEntry(uid);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    clearAllAxisMappings();
                }
                Drone.Model droneModel = DeviceModels.droneModel(product);
                AxisMappableAction axisAction = Actions.convert(action);
                if (droneModel == null || axisAction == null) {
                    ULog.w(TAG_GAMEPAD, "Invalid product or action, dropping mapping [uid: " + uid
                                        + ", product: " + product + ", action: " + action + "]");
                } else {
                    addAxisMappingEntry(uid, droneModel, axisAction, 1 << axisCode, buttons);
                }
            }

            if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                updateAxisMappings();
            }
        }

        @Override
        public void onApplicationAxisEvent(@Nullable ArsdkFeatureMapper.AxisAction action, int value) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onApplicationAxisEvent [action: " + action + ", value: " + value + "]");
            }
        }

        @Override
        public void onApplicationButtonEvent(@Nullable ArsdkFeatureMapper.ButtonAction action) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onApplicationButtonEvent [action: " + action + "]");
            }

            ButtonsMappableAction appAction = Actions.convert(action);
            if (appAction == null) {
                ULog.w(TAG_GAMEPAD, "Invalid application action " + action + ", dropping event");
            } else {
                VirtualGamepadCore.notifyAppEvent(appAction);
            }
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onExpoMapItem(long uid, @DeviceModel.Id int product, int axisCode,
                                  @Nullable ArsdkFeatureMapper.ExpoType expo, int listFlagsBitField) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onExpoMapItem [uid: " + uid + ", product: " + product
                                    + ", axis: " + axisCode + ", expo: " + expo
                                    + "listFlags: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlagsBitField)
                                    + "]");
            }

            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField);
            if (clearList) {
                clearAllAxisInterpolators();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                removeAxisInterpolatorEntry(uid);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    clearAllAxisInterpolators();
                }
                Drone.Model droneModel = DeviceModels.droneModel(product);
                if (droneModel == null || expo == null) {
                    ULog.w(TAG_GAMEPAD, "Invalid product or expo, dropping axis interpolator [uid: " + uid
                                        + ", product: " + product + ", axis: " + axisCode + ", expo: " + expo + "]");
                } else {
                    AxisInterpolator interpolator = null;
                    switch (expo) {
                        case LINEAR:
                            interpolator = AxisInterpolator.LINEAR;
                            break;
                        case EXPO_0:
                            interpolator = AxisInterpolator.LIGHT_EXPONENTIAL;
                            break;
                        case EXPO_1:
                            interpolator = AxisInterpolator.MEDIUM_EXPONENTIAL;
                            break;
                        case EXPO_2:
                            interpolator = AxisInterpolator.STRONG_EXPONENTIAL;
                            break;
                        case EXPO_4:
                            interpolator = AxisInterpolator.STRONGEST_EXPONENTIAL;
                            break;
                    }
                    addAxisInterpolatorEntry(uid, droneModel, 1 << axisCode, interpolator);
                }
            }

            if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                updateAxisInterpolators();
            }
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onInvertedMapItem(long uid, @DeviceModel.Id int product, int axisCode, int inverted,
                                      int listFlagsBitField) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onInvertedMapItem [uid: " + uid + ", product: " + product
                                    + ", axis: " + axisCode + ", inverted: " + inverted
                                    + "listFlags: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlagsBitField)
                                    + "]");
            }

            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField);
            if (clearList) {
                clearAllReversedAxes();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                removeReversedAxisEntry(uid);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    clearAllReversedAxes();
                }
                Drone.Model droneModel = DeviceModels.droneModel(product);
                if (droneModel == null) {
                    ULog.w(TAG_GAMEPAD, "Invalid product, dropping axis inversion [uid: " + uid
                                        + ", product: " + product + ", axis: " + axisCode + ", inverted: " + inverted + "]");
                } else {
                    addReversedAxisEntry(uid, droneModel, 1 << axisCode, inverted == 1);
                }
            }

            if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                updateReversedAxes();
            }
        }

        @Override
        public void onActiveProduct(@DeviceModel.Id int product) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onActiveProduct [product: " + product + "]");
            }
            Drone.Model droneModel = DeviceModels.droneModel(product);
            if (droneModel == null) {
                ULog.w(TAG_GAMEPAD, "Unsupported product " + product);
            } else {
                processActiveDroneModelChange(droneModel);
            }
        }

        @Override
        public void onVolatileMappingState(int active) {
            if (ULog.d(TAG_GAMEPAD)) {
                ULog.d(TAG_GAMEPAD, "onVolatileMappingState [active: " + active + "]");
            }

            onVolatileMapping(active == 1);
        }
    };

    /**
     * Grabs buttons and/or axes.
     *
     * @param buttons            mask of all buttons to be grabbed
     * @param axes               mask of all axes to be grabbed
     * @param fromVirtualGamepad {@code true} when the request has to be performed on behalf of VirtualGamepad
     */
    private void grab(@ButtonMask long buttons, @AxisMask long axes, boolean fromVirtualGamepad) {
        mGrabRequestFromVirtualGamepad = fromVirtualGamepad;
        if (buttons == 0 && axes == 0 && !mGrabRequestFromVirtualGamepad && mVirtualGamepadPreempted) {
            // if it is a release (buttons = 0, axes = 0) request from a specialized gamepad, and the virtual
            // gamepad was currently preempted, then virtual gamepad becomes not preempted anymore
            mVirtualGamepadPreempted = false;
            if (mVirtualGamepadGrabbed) {
                // and in case it was also grabbed, forward a 'release' state directly to specialized gamepad and send
                // a grab request instead on behalf of the virtual gamepad to restore navigation.
                onGrabState(0, 0, 0);
                buttons = mNavEventTranslator.getNavigationGrabButtonsMask();
                axes = mNavEventTranslator.getNavigationGrabAxesMask();
                mGrabRequestFromVirtualGamepad = true;
            }
        }
        sendCommand(ArsdkFeatureMapper.encodeGrab(buttons, axes));
    }

    /**
     * Processes a new grab state.
     *
     * @param buttons      mask of all grabbed buttons
     * @param axes         mask of all grabbed axes
     * @param buttonStates mask of all pressed buttons
     */
    private void processGrabState(@ButtonMask long buttons, @AxisMask long axes, @ButtonMask long buttonStates) {
        boolean stateHandled = false;
        if (buttons == 0 && axes == 0) {
            mVirtualGamepadGrabbed = mVirtualGamepadPreempted = false;
            mVirtualGamepad.updateGrabbed(false).updatePreempted(false).notifyUpdated();
        } else {
            if (mGrabRequestFromVirtualGamepad) {
                // check grab masks, forward state to virtual gamepad
                @ButtonMask long expectedButtons = mNavEventTranslator.getNavigationGrabButtonsMask();
                @AxisMask long expectedAxes = mNavEventTranslator.getNavigationGrabAxesMask();
                if ((buttons & expectedButtons) != 0 || (axes & expectedAxes) != 0) {
                    // considered grabbed
                    mVirtualGamepadGrabbed = true;
                    mVirtualGamepad.updateGrabbed(true).updatePreempted(false).notifyUpdated();
                    // however warn if the complete set of buttons/axes is not present
                    if ((buttons & expectedButtons) != expectedButtons || (axes & expectedAxes) != expectedAxes) {
                        ULog.w(TAG_GAMEPAD, "Missing grabbed buttons/axes for navigation "
                                            + " [buttons: " + Long.toBinaryString(buttons)
                                            + " , axes: " + Long.toBinaryString(axes) + "]");
                    }
                    // also notify events for pressed buttons
                    while (buttons != 0) {
                        @ButtonMask long buttonMask = Long.lowestOneBit(buttons);
                        VirtualGamepad.Event event = mNavEventTranslator.eventFrom(buttonMask);
                        if (event != null && (buttonStates & buttonMask) != 0) {
                            mVirtualGamepad.notifyNavigationEvent(event, VirtualGamepad.Event.State.PRESSED);
                        }
                        buttons ^= buttonMask;
                    }
                }
                stateHandled = true;
            } else {
                mVirtualGamepadPreempted = true;
                mVirtualGamepad.updatePreempted(true).notifyUpdated();
            }
        }

        if (!stateHandled) {
            onGrabState(buttons, axes, buttonStates);
        }
    }

    /**
     * Processes a button event.
     *
     * @param button  mask of the button that triggered an event
     * @param pressed {@code true} when the button is pressed, otherwise {@code false}
     */
    private void processButtonEvent(@ButtonMask long button, boolean pressed) {
        if (!mVirtualGamepadPreempted && mVirtualGamepadGrabbed) {
            VirtualGamepad.Event event = mNavEventTranslator.eventFrom(button);
            if (event != null) {
                mVirtualGamepad.notifyNavigationEvent(event,
                        pressed ? VirtualGamepad.Event.State.PRESSED : VirtualGamepad.Event.State.RELEASED);
            }
        } else {
            onButtonEvent(button, pressed);
        }
    }

    /**
     * Processes an axis event.
     *
     * @param axis  mask of the axis that triggered an event
     * @param value current axis value
     */
    private void processAxisEvent(@AxisMask long axis, @IntRange(from = -100, to = 100) int value) {
        if (mVirtualGamepadPreempted || !mVirtualGamepadGrabbed) {
            onAxisEvent(axis, value);
        }
    }

    /** Converts mapper (button/axis) action to/from Gamepad mappable (buttons/axes) actions. */
    @VisibleForTesting
    static final class Actions {

        /**
         * Translates a Gamepad buttons mappable action to its Mapper equivalent.
         *
         * @param action gamepad action to convert
         *
         * @return the corresponding Mapper action
         */
        @NonNull
        static ArsdkFeatureMapper.ButtonAction convert(@NonNull ButtonsMappableAction action) {
            return ARSDK_BUTTON_ACTIONS.get(action.ordinal());
        }

        /**
         * Translates a Mapper button action to its Gamepad equivalent.
         *
         * @param action mapper action to convert
         *
         * @return the corresponding Gamepad action, or {@code null} if the provided action cannot be translated
         */
        @Nullable
        static ButtonsMappableAction convert(@Nullable ArsdkFeatureMapper.ButtonAction action) {
            ButtonsMappableAction buttonAction = null;
            if (action != null) {
                buttonAction = GSDK_BUTTON_ACTIONS.get(action.ordinal());
                if (buttonAction == null) {
                    ULog.w(TAG_GAMEPAD, "Unsupported ButtonAction " + action);
                }
            }
            return buttonAction;
        }

        /**
         * Translates a Gamepad axis mappable action to its Mapper equivalent.
         *
         * @param action gamepad action to convert
         *
         * @return the corresponding Mapper action
         */
        @NonNull
        static ArsdkFeatureMapper.AxisAction convert(@NonNull AxisMappableAction action) {
            return ARSDK_AXIS_ACTIONS.get(action.ordinal());
        }

        /**
         * Translates a Mapper axis action to its Gamepad equivalent.
         *
         * @param action mapper action to convert
         *
         * @return the corresponding Gamepad action, or {@code null} if the provided action cannot be translated
         */
        @Nullable
        static AxisMappableAction convert(@Nullable ArsdkFeatureMapper.AxisAction action) {
            AxisMappableAction axisAction = null;
            if (action != null) {
                axisAction = GSDK_AXIS_ACTIONS.get(action.ordinal());
                if (axisAction == null) {
                    ULog.w(TAG_GAMEPAD, "Unsupported AxisAction " + action);
                }
            }
            return axisAction;
        }

        /**
         * Private constructor for static utility class.
         */
        private Actions() {
        }

        /** ARSDK Mapper button actions, by GSDK ButtonsMappableAction ordinal. */
        private static final SparseArray<ArsdkFeatureMapper.ButtonAction> ARSDK_BUTTON_ACTIONS = new SparseArray<>();

        /** GSDK ButtonsMappableAction, by ARSDK mapper button action ordinal. */
        private static final SparseArray<ButtonsMappableAction> GSDK_BUTTON_ACTIONS = new SparseArray<>();

        /** ARSDK Mapper axis actions, by GSDK AxisMappableAction ordinal. */
        private static final SparseArray<ArsdkFeatureMapper.AxisAction> ARSDK_AXIS_ACTIONS = new SparseArray<>();

        /** GSDK AxisMappableAction, by ARSDK mapper axis action ordinal. */
        private static final SparseArray<AxisMappableAction> GSDK_AXIS_ACTIONS = new SparseArray<>();

        /**
         * Maps an ARSDK button action and a GSDK ButtonMappableAction together, both ways.
         *
         * @param arsdkAction ARSDK button action to map
         * @param gsdkAction  GSDK ButtonMappableAction to map
         */
        private static void map(@NonNull ArsdkFeatureMapper.ButtonAction arsdkAction,
                                @NonNull ButtonsMappableAction gsdkAction) {
            ARSDK_BUTTON_ACTIONS.put(gsdkAction.ordinal(), arsdkAction);
            GSDK_BUTTON_ACTIONS.put(arsdkAction.ordinal(), gsdkAction);
        }

        /**
         * Maps an ARSDK axis action and a GSDK AxisMappableAction together, both ways.
         *
         * @param arsdkAction ARSDK axis action to map
         * @param gsdkAction  GSDK AxisMappableAction to amp
         */
        private static void map(@NonNull ArsdkFeatureMapper.AxisAction arsdkAction,
                                @NonNull AxisMappableAction gsdkAction) {
            ARSDK_AXIS_ACTIONS.put(gsdkAction.ordinal(), arsdkAction);
            GSDK_AXIS_ACTIONS.put(arsdkAction.ordinal(), gsdkAction);
        }

        static {
            // application buttons actions
            map(ArsdkFeatureMapper.ButtonAction.APP_0, ButtonsMappableAction.APP_ACTION_SETTINGS);
            map(ArsdkFeatureMapper.ButtonAction.APP_1, ButtonsMappableAction.APP_ACTION_1);
            map(ArsdkFeatureMapper.ButtonAction.APP_2, ButtonsMappableAction.APP_ACTION_2);
            map(ArsdkFeatureMapper.ButtonAction.APP_3, ButtonsMappableAction.APP_ACTION_3);
            map(ArsdkFeatureMapper.ButtonAction.APP_4, ButtonsMappableAction.APP_ACTION_4);
            map(ArsdkFeatureMapper.ButtonAction.APP_5, ButtonsMappableAction.APP_ACTION_5);
            map(ArsdkFeatureMapper.ButtonAction.APP_6, ButtonsMappableAction.APP_ACTION_6);
            map(ArsdkFeatureMapper.ButtonAction.APP_7, ButtonsMappableAction.APP_ACTION_7);
            map(ArsdkFeatureMapper.ButtonAction.APP_8, ButtonsMappableAction.APP_ACTION_8);
            map(ArsdkFeatureMapper.ButtonAction.APP_9, ButtonsMappableAction.APP_ACTION_9);
            map(ArsdkFeatureMapper.ButtonAction.APP_10, ButtonsMappableAction.APP_ACTION_10);
            map(ArsdkFeatureMapper.ButtonAction.APP_11, ButtonsMappableAction.APP_ACTION_11);
            map(ArsdkFeatureMapper.ButtonAction.APP_12, ButtonsMappableAction.APP_ACTION_12);
            map(ArsdkFeatureMapper.ButtonAction.APP_13, ButtonsMappableAction.APP_ACTION_13);
            map(ArsdkFeatureMapper.ButtonAction.APP_14, ButtonsMappableAction.APP_ACTION_14);
            map(ArsdkFeatureMapper.ButtonAction.APP_15, ButtonsMappableAction.APP_ACTION_15);
            // predefined buttons actions
            map(ArsdkFeatureMapper.ButtonAction.RETURN_HOME, ButtonsMappableAction.RETURN_HOME);
            map(ArsdkFeatureMapper.ButtonAction.TAKEOFF_LAND, ButtonsMappableAction.TAKEOFF_OR_LAND);
            map(ArsdkFeatureMapper.ButtonAction.VIDEO_RECORD, ButtonsMappableAction.RECORD_VIDEO);
            map(ArsdkFeatureMapper.ButtonAction.TAKE_PICTURE, ButtonsMappableAction.TAKE_PICTURE);
            map(ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_INC,
                    ButtonsMappableAction.INCREASE_CAMERA_EXPOSITION);
            map(ArsdkFeatureMapper.ButtonAction.CAMERA_EXPOSITION_DEC,
                    ButtonsMappableAction.DECREASE_CAMERA_EXPOSITION);
            map(ArsdkFeatureMapper.ButtonAction.FLIP_LEFT, ButtonsMappableAction.FLIP_LEFT);
            map(ArsdkFeatureMapper.ButtonAction.FLIP_RIGHT, ButtonsMappableAction.FLIP_RIGHT);
            map(ArsdkFeatureMapper.ButtonAction.FLIP_FRONT, ButtonsMappableAction.FLIP_FRONT);
            map(ArsdkFeatureMapper.ButtonAction.FLIP_BACK, ButtonsMappableAction.FLIP_BACK);
            map(ArsdkFeatureMapper.ButtonAction.EMERGENCY, ButtonsMappableAction.EMERGENCY_CUTOFF);
            map(ArsdkFeatureMapper.ButtonAction.CENTER_CAMERA, ButtonsMappableAction.CENTER_CAMERA);
            map(ArsdkFeatureMapper.ButtonAction.CYCLE_HUD, ButtonsMappableAction.CYCLE_HUD);
            map(ArsdkFeatureMapper.ButtonAction.CAMERA_AUTO, ButtonsMappableAction.PHOTO_OR_VIDEO);
            // predefined axis actions
            map(ArsdkFeatureMapper.AxisAction.ROLL, AxisMappableAction.CONTROL_ROLL);
            map(ArsdkFeatureMapper.AxisAction.PITCH, AxisMappableAction.CONTROL_PITCH);
            map(ArsdkFeatureMapper.AxisAction.YAW, AxisMappableAction.CONTROL_YAW_ROTATION_SPEED);
            map(ArsdkFeatureMapper.AxisAction.GAZ, AxisMappableAction.CONTROL_THROTTLE);
            map(ArsdkFeatureMapper.AxisAction.CAMERA_PAN, AxisMappableAction.PAN_CAMERA);
            map(ArsdkFeatureMapper.AxisAction.CAMERA_TILT, AxisMappableAction.TILT_CAMERA);
            map(ArsdkFeatureMapper.AxisAction.CAMERA_ZOOM, AxisMappableAction.ZOOM_CAMERA);
        }
    }
}
