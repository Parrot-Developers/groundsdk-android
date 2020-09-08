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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ReturnHomePilotingItfCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureRth;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;

/** ReturnHome piloting interface controller for Anafi family drones. */
public class AnafiReturnHomePilotingItf extends ActivablePilotingItfController {

    /** Key used to access preset and range dictionaries for this piloting interface settings. */
    private static final String SETTINGS_KEY = "returnHome";

    // preset store bindings

    /** Auto trigger preset entry. */
    private static final StorageEntry<Boolean> AUTO_TRIGGER_PRESET =
            StorageEntry.ofBoolean("autoTrigger");

    /** Preferred target preset entry. */
    private static final StorageEntry<ReturnHomePilotingItf.Target> PREFERRED_TARGET_PRESET =
            StorageEntry.ofEnum("preferredTarget", ReturnHomePilotingItf.Target.class);

    /** Ending behavior preset entry. */
    private static final StorageEntry<ReturnHomePilotingItf.EndingBehavior> ENDING_BEHAVIOR_PRESET =
            StorageEntry.ofEnum("endingBehavior", ReturnHomePilotingItf.EndingBehavior.class);

    /** Auto-start on disconnect delay preset entry. */
    private static final StorageEntry<Integer> AUTOSTART_ON_DISCONNECT_DELAY_PRESET =
            StorageEntry.ofInteger("autoStartOnDisconnectDelay");

    /** Ending hovering altitude preset entry. */
    private static final StorageEntry<Double> ENDING_HOVERING_ALTITUDE_PRESET =
            StorageEntry.ofDouble("endingHoveringAltitude");

    /** Minimum altitude preset entry. */
    private static final StorageEntry<Double> MIN_ALTITUDE_PRESET = StorageEntry.ofDouble("minAltitude");

    // device specific store bindings

    /** Auto-start on disconnect delay range device setting. */
    private static final StorageEntry<IntegerRange> AUTOSTART_ON_DISCONNECT_DELAY_RANGE_SETTING =
            StorageEntry.ofIntegerRange("autoStartOnDisconnectDelayRange");

    /** Ending hovering altitude range device setting. */
    private static final StorageEntry<DoubleRange> ENDING_HOVERING_ALTITUDE_RANGE_SETTING =
            StorageEntry.ofDoubleRange("endingHoveringAltitudeRange");

    /** Minimum altitude range device setting. */
    private static final StorageEntry<DoubleRange> MIN_ALTITUDE_RANGE_SETTING =
            StorageEntry.ofDoubleRange("minAltitudeRange");

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final ReturnHomePilotingItfCore mPilotingItf;

    /** Persists device specific values for this piloting interface, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Persists current preset values for this piloting interface. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Auto trigger. */
    @Nullable
    private Boolean mAutoTrigger;

    /** Preferred target. */
    @Nullable
    private ReturnHomePilotingItf.Target mPreferredTarget;

    /** Ending behavior. */
    @Nullable
    private ReturnHomePilotingItf.EndingBehavior mEndingBehavior;

    /** Auto-start on disconnect delay. */
    @Nullable
    private Integer mAutoStartOnDisconnectDelay;

    /** Ending hovering altitude. */
    @Nullable
    private Double mEndingHoveringAltitude;

    /** Minimum altitude. */
    @Nullable
    private Double mMinAltitude;

    /** Last estimation of possibility for the drone to reach its return point received from the drone. */
    @NonNull
    private ReturnHomePilotingItf.Reachability mReachability;

    /**
     * Delay before automatic safety return in seconds.
     * <p>
     * {@link ReturnHomePilotingItfCore#NO_DELAY} if there is no planned safety return.
     */
    private long mAutoTriggerDelay;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     */
    public AnafiReturnHomePilotingItf(@NonNull PilotingItfActivationController activationController) {
        super(activationController, false);
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mPilotingItf = new ReturnHomePilotingItfCore(mComponentStore, new Backend());
        mReachability = ReturnHomePilotingItf.Reachability.UNKNOWN;
        mAutoTriggerDelay = ReturnHomePilotingItfCore.NO_DELAY;
        loadPersistedData();
        if (isPersisted()) {
            mPilotingItf.publish();
        }
    }


    @Override
    public void requestActivation() {
        super.requestActivation();
        sendCommand(ArsdkFeatureArdrone3.Piloting.encodeNavigateHome(1));
    }

    @Override
    public void requestDeactivation() {
        super.requestDeactivation();
        sendCommand(ArsdkFeatureArdrone3.Piloting.encodeNavigateHome(0));
    }

    @Override
    @NonNull
    public final ReturnHomePilotingItfCore getPilotingItf() {
        return mPilotingItf;
    }

    @Override
    protected void onConnecting() {
        // The drone does not send the preferred target if it has not been set since last boot
        mPreferredTarget = null;
    }

    @Override
    protected final void onConnected() {
        super.onConnected();
        applyPresets();
        mPilotingItf.publish();
    }

    @Override
    protected final void onDisconnected() {
        // clear all non saved settings
        mPilotingItf.cancelSettingsRollbacks()
                    .resetLocation()
                    .updateCurrentTarget(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION)
                    .updateGpsFixedOnTakeOff(false);
        if (!isPersisted()) {
            mPilotingItf.unpublish();
        }
        super.onDisconnected();
    }

    @Override
    protected final void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mPilotingItf.notifyUpdated();
    }

    @Override
    protected final void onForgetting() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mPilotingItf.unpublish();
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureRth.UID) {
            ArsdkFeatureRth.decode(command, mRthCallback);
        }
    }

    /**
     * Tells whether device specific settings are persisted for this component.
     *
     * @return {@code true} if the component has persisted device settings, otherwise {@code false}
     */
    private boolean isPersisted() {
        return mDeviceDict != null && !mDeviceDict.isNew();
    }

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        IntegerRange storedAutoStartOnDisconnectDelayRange =
                AUTOSTART_ON_DISCONNECT_DELAY_RANGE_SETTING.load(mDeviceDict);
        if (storedAutoStartOnDisconnectDelayRange != null) {
            mPilotingItf.getAutoStartOnDisconnectDelay().updateBounds(storedAutoStartOnDisconnectDelayRange);
        }

        DoubleRange endingHoveringAltitudeRange = ENDING_HOVERING_ALTITUDE_RANGE_SETTING.load(mDeviceDict);
        if (endingHoveringAltitudeRange != null) {
            mPilotingItf.getEndingHoveringAltitude().updateBounds(endingHoveringAltitudeRange);
        }

        DoubleRange minAltitudeRange = MIN_ALTITUDE_RANGE_SETTING.load(mDeviceDict);
        if (minAltitudeRange != null) {
            mPilotingItf.getMinAltitude().updateBounds(minAltitudeRange);
        }

        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyAutoTrigger(AUTO_TRIGGER_PRESET.load(mPresetDict));
        applyPreferredTarget(PREFERRED_TARGET_PRESET.load(mPresetDict));
        applyEndingBehavior(ENDING_BEHAVIOR_PRESET.load(mPresetDict));
        applyAutoStartOnDisconnectDelay(AUTOSTART_ON_DISCONNECT_DELAY_PRESET.load(mPresetDict));
        applyEndingHoveringAltitude(ENDING_HOVERING_ALTITUDE_PRESET.load(mPresetDict));
        applyMinAltitude(MIN_ALTITUDE_PRESET.load(mPresetDict));
    }

    /**
     * Applies auto trigger.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param enabled value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyAutoTrigger(@Nullable Boolean enabled) {
        if (enabled == null) {
            if (mAutoTrigger == null) {
                return false;
            }
            enabled = mAutoTrigger;
        }

        boolean updating = mAutoTrigger != enabled
                           && sendCommand(ArsdkFeatureRth.encodeSetAutoTriggerMode(convert(enabled)));

        mAutoTrigger = enabled;
        mPilotingItf.autoTrigger().updateValue(enabled);

        return updating;
    }

    /**
     * Applies preferred target.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param preferredTarget value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyPreferredTarget(@Nullable ReturnHomePilotingItf.Target preferredTarget) {
        if (preferredTarget == null) {
            if (mPreferredTarget == null) {
                return false;
            }
            preferredTarget = mPreferredTarget;
        }

        boolean updating = mPreferredTarget != preferredTarget
                           && sendCommand(ArsdkFeatureRth.encodeSetPreferredHomeType(convert(preferredTarget)));

        mPreferredTarget = preferredTarget;
        mPilotingItf.getPreferredTarget()
                    .updateValue(preferredTarget);

        return updating;
    }

    /**
     * Applies ending behavior.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param endingBehavior value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyEndingBehavior(@Nullable ReturnHomePilotingItf.EndingBehavior endingBehavior) {
        if (endingBehavior == null) {
            if (mEndingBehavior == null) {
                return false;
            }
            endingBehavior = mEndingBehavior;
        }

        boolean updating = mEndingBehavior != endingBehavior
                           && sendCommand(ArsdkFeatureRth.encodeSetEndingBehavior(convert(endingBehavior)));

        mEndingBehavior = endingBehavior;
        mPilotingItf.getEndingBehavior()
                .updateValue(endingBehavior);

        return updating;
    }

    /**
     * Applies auto-start on disconnect delay.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param delay value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyAutoStartOnDisconnectDelay(@Nullable Integer delay) {
        if (delay == null) {
            if (mAutoStartOnDisconnectDelay == null) {
                return false;
            }
            delay = mAutoStartOnDisconnectDelay;
        }

        boolean updating = !delay.equals(mAutoStartOnDisconnectDelay)
                           && sendCommand(ArsdkFeatureRth.encodeSetDelay(delay));

        mAutoStartOnDisconnectDelay = delay;
        mPilotingItf.getAutoStartOnDisconnectDelay()
                    .updateValue(delay);

        return updating;
    }
    /**
     * Applies ending hovering altitude.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param altitude value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyEndingHoveringAltitude(@Nullable Double altitude) {
        if (altitude == null) {
            if (mEndingHoveringAltitude == null) {
                return false;
            }
            altitude = mEndingHoveringAltitude;
        }

        boolean updating = !altitude.equals(mEndingHoveringAltitude) && sendCommand(
                ArsdkFeatureRth.encodeSetEndingHoveringAltitude(altitude.floatValue()));

        mEndingHoveringAltitude = altitude;
        mPilotingItf.getEndingHoveringAltitude().updateValue(altitude);

        return updating;
    }

    /**
     * Applies minimum altitude.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param altitude value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMinAltitude(@Nullable Double altitude) {
        if (altitude == null) {
            if (mMinAltitude == null) {
                return false;
            }
            altitude = mMinAltitude;
        }

        boolean updating = !altitude.equals(mMinAltitude) && sendCommand(
                ArsdkFeatureRth.encodeSetMinAltitude(altitude.floatValue()));

        mMinAltitude = altitude;
        mPilotingItf.getMinAltitude().updateValue(altitude);

        return updating;
    }

    /**
     * Updates return home auto-trigger delay.
     *
     * @param delay new return home auto-trigger delay, in seconds
     */
    private void updateAutoTriggerDelay(long delay) {
        if (mAutoTriggerDelay != delay) {
            mAutoTriggerDelay = delay;
            updateReachabilityStatus();
        }
    }

    /**
     * Updates the home reachability status and the automatic safety return delay in the piloting interface.
     * <p>
     * If an automatic return is planned, this function sets the home reachability status to {@code WARNING}. Otherwise,
     * the home reachability status given by the drone is updated in the interface.
     */
    private void updateReachabilityStatus() {
        if (mAutoTriggerDelay == ReturnHomePilotingItfCore.NO_DELAY) {
            // no return home auto trigger
            mPilotingItf.updateHomeReachability(mReachability)
                        .updateAutoTriggerDelay(ReturnHomePilotingItfCore.NO_DELAY);
        } else {
            // return home auto trigger activated, we set reachability to WARNING
            mPilotingItf.updateHomeReachability(ReturnHomePilotingItf.Reachability.WARNING)
                        .updateAutoTriggerDelay(mAutoTriggerDelay);
        }
    }

    /**
     * Converts an auto trigger into its arsdk
     * {@link ArsdkFeatureRth.AutoTriggerMode representation}.
     *
     * @param enabled new return home auto trigger to convert
     *
     * @return arsdk representation of the specified target
     */
    @NonNull
    private static ArsdkFeatureRth.AutoTriggerMode convert(@NonNull Boolean enabled) {
        return enabled ? ArsdkFeatureRth.AutoTriggerMode.ON : ArsdkFeatureRth.AutoTriggerMode.OFF;
    }

    /**
     * Converts a groundsdk {@link ReturnHomePilotingItf.Target return home target} into its arsdk
     * {@link ArsdkFeatureRth.HomeType representation}.
     *
     * @param target groundsdk return home target to convert
     *
     * @return arsdk representation of the specified target
     */
    @NonNull
    private static ArsdkFeatureRth.HomeType convert(@NonNull ReturnHomePilotingItf.Target target) {
        switch (target) {
            case NONE:
                return ArsdkFeatureRth.HomeType.NONE;
            case TAKE_OFF_POSITION:
                return ArsdkFeatureRth.HomeType.TAKEOFF;
            case CUSTOM_LOCATION:
                return ArsdkFeatureRth.HomeType.CUSTOM;
            case CONTROLLER_POSITION:
                return ArsdkFeatureRth.HomeType.PILOT;
            case TRACKED_TARGET_POSITION:
                return ArsdkFeatureRth.HomeType.FOLLOWEE;
        }

        return null;
    }

    /**
     * Converts a groundsdk {@link ReturnHomePilotingItf.EndingBehavior return home ending behavior} into its arsdk
     * {@link ArsdkFeatureRth.EndingBehavior representation}.
     *
     * @param endingBehavior groundsdk return home ending behavior to convert
     *
     * @return arsdk representation of the specified ending behavior
     */
    @NonNull
    private static ArsdkFeatureRth.EndingBehavior convert(
            @NonNull ReturnHomePilotingItf.EndingBehavior endingBehavior) {
        switch (endingBehavior) {
            case HOVERING:
                return ArsdkFeatureRth.EndingBehavior.HOVERING;
            case LANDING:
                return ArsdkFeatureRth.EndingBehavior.LANDING;
        }

        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureRth.AutoTriggerMode auto trigger mode} into a boolean.
     *
     * @param autoTriggerMode arsdk auto trigger mode to convert
     *
     * @return {@code true} if the input is {@link ArsdkFeatureRth.AutoTriggerMode#ON}, otherwise {@code false}
     */
    private static boolean convert(@NonNull ArsdkFeatureRth.AutoTriggerMode autoTriggerMode) {
        return autoTriggerMode == ArsdkFeatureRth.AutoTriggerMode.ON;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureRth.EndingBehavior ending behavior} into its groundsdk
     * {@link ReturnHomePilotingItf.EndingBehavior representation}.
     *
     * @param endingBehavior arsdk ending behavior to convert
     *
     * @return groundsdk representation of the specified ending behavior
     */
    @NonNull
    private static ReturnHomePilotingItf.EndingBehavior convert(
            @NonNull ArsdkFeatureRth.EndingBehavior endingBehavior) {
        switch (endingBehavior) {
            case HOVERING:
                return ReturnHomePilotingItf.EndingBehavior.HOVERING;
            case LANDING:
                return ReturnHomePilotingItf.EndingBehavior.LANDING;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureRth.HomeReachability home reachability} into its groundsdk
     * {@link ReturnHomePilotingItf.Reachability representation}.
     *
     * @param reachability arsdk home reachability to convert
     *
     * @return groundsdk representation of the specified reachability
     */
    @NonNull
    private static ReturnHomePilotingItf.Reachability convert(@NonNull ArsdkFeatureRth.HomeReachability reachability) {
        switch (reachability) {
            case UNKNOWN:
                return ReturnHomePilotingItf.Reachability.UNKNOWN;
            case REACHABLE:
                return ReturnHomePilotingItf.Reachability.REACHABLE;
            case CRITICAL:
                return ReturnHomePilotingItf.Reachability.CRITICAL;
            case NOT_REACHABLE:
                return ReturnHomePilotingItf.Reachability.NOT_REACHABLE;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureRth.HomeType home type} into its
     * groundsdk {@link ReturnHomePilotingItf.Target representation}.
     *
     * @param type arsdk home type to convert
     *
     * @return groundsdk representation of the specified type
     */
    @NonNull
    private static ReturnHomePilotingItf.Target convert(
            @NonNull ArsdkFeatureRth.HomeType type) {
        switch (type) {
            case NONE:
                return ReturnHomePilotingItf.Target.NONE;
            case TAKEOFF:
                return ReturnHomePilotingItf.Target.TAKE_OFF_POSITION;
            case CUSTOM:
                return ReturnHomePilotingItf.Target.CUSTOM_LOCATION;
            case PILOT:
                return ReturnHomePilotingItf.Target.CONTROLLER_POSITION;
            case FOLLOWEE:
                return ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureRth.HomeType} to its equivalent set of
     * {@code ReturnHomePilotingItf.Target}.
     *
     * @param bitfield bitfield representation of RTH feature home type to convert
     *
     * @return the equivalent set of groundsdk targets
     */
    @NonNull
    private static EnumSet<ReturnHomePilotingItf.Target> from(int bitfield) {
        EnumSet<ReturnHomePilotingItf.Target> targets = EnumSet.noneOf(ReturnHomePilotingItf.Target.class);
        ArsdkFeatureRth.HomeType.each(bitfield, arsdk -> targets.add(convert(arsdk)));
        return targets;
    }

    /** Callbacks called when a command of the feature ArsdkFeatureRth is decoded. */
    private final ArsdkFeatureRth.Callback mRthCallback = new ArsdkFeatureRth.Callback() {

        /** Special value sent by the drone when either latitude or longitude is not known. */
        private static final double UNKNOWN_COORDINATE = 500;

        @Override
        public void onHomeReachability(@Nullable ArsdkFeatureRth.HomeReachability status) {
            if (status == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid home reachability status");
            }

            ReturnHomePilotingItf.Reachability reachability = convert(status);

            if (mReachability != reachability) {
                mReachability = reachability;
                updateReachabilityStatus();
                mPilotingItf.notifyUpdated();
            }
        }

        @Override
        public void onHomeTypeCapabilities(int valuesBitField) {
            EnumSet<ReturnHomePilotingItf.Target> supportedTargets = from(valuesBitField);
            mPilotingItf.getPreferredTarget().updateAvailableValues(supportedTargets);
            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onHomeType(@Nullable ArsdkFeatureRth.HomeType type) {
            if (type == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid return home type");
            }

            mPilotingItf.updateCurrentTarget(convert(type)).notifyUpdated();
        }

        @Override
        public void onPreferredHomeType(@Nullable ArsdkFeatureRth.HomeType type) {
            if (type == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid return home type");
            }

            mPreferredTarget = convert(type);
            if (isConnected()) {
                mPilotingItf.getPreferredTarget().updateValue(mPreferredTarget);
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onTakeoffLocation(double latitude, double longitude, float altitude, int fixedbeforetakeoff) {
            onLocation(latitude, longitude, altitude, fixedbeforetakeoff == 1);
            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onCustomLocation(double latitude, double longitude, float altitude) {
            onLocation(latitude, longitude, altitude);
            mPilotingItf.notifyUpdated();
        }


        @Override
        public void onFolloweeLocation(double latitude, double longitude, float altitude) {
            onLocation(latitude, longitude, altitude);
            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onDelay(int delay, int min, int max) {
            mAutoStartOnDisconnectDelay = delay;

            IntegerRange bounds = IntegerRange.of(min, max);
            AUTOSTART_ON_DISCONNECT_DELAY_RANGE_SETTING.save(mDeviceDict, bounds);
            mPilotingItf.getAutoStartOnDisconnectDelay()
                    .updateBounds(bounds);

            if (isConnected()) {
                mPilotingItf.getAutoStartOnDisconnectDelay()
                        .updateValue(mAutoStartOnDisconnectDelay);
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onState(@Nullable ArsdkFeatureRth.State state, @Nullable ArsdkFeatureRth.StateReason reason) {
            if (state != null) switch (state) {
                case AVAILABLE:
                    if (reason == ArsdkFeatureRth.StateReason.FINISHED) {
                        mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.FINISHED);
                    } else {
                        mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.NONE);
                    }
                    notifyIdle();
                    break;
                case PENDING:
                case IN_PROGRESS:
                    // reset the auto trigger if any
                    updateAutoTriggerDelay(ReturnHomePilotingItfCore.NO_DELAY);
                    if (reason != null) switch (reason) {
                        case USER_REQUEST:
                            mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.USER_REQUESTED);
                            break;
                        case CONNECTION_LOST:
                            mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.CONNECTION_LOST);
                            break;
                        case LOW_BATTERY:
                            mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.POWER_LOW);
                            break;
                        case FINISHED:
                        case STOPPED:
                        case DISABLED:
                        case ENABLED:
                            break;
                    }
                    notifyActive();
                    break;
                case UNAVAILABLE:
                    mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.NONE);
                    notifyUnavailable();
                    break;
            }
        }

        @Override
        public void onMinAltitude(float current, float min, float max) {
            if (min > max) {
                throw new ArsdkCommand.RejectedEventException(
                        "Invalid min altitude bounds [min: " + min + ", max: " + max + "]");
            }

            DoubleRange bounds = DoubleRange.of(min, max);
            MIN_ALTITUDE_RANGE_SETTING.save(mDeviceDict, bounds);
            mPilotingItf.getMinAltitude().updateBounds(bounds);

            mMinAltitude = (double) current;
            if (isConnected()) {
                mPilotingItf.getMinAltitude().updateValue(mMinAltitude);
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onRthAutoTrigger(@Nullable ArsdkFeatureRth.AutoTriggerReason reason, long delay) {
            if (reason == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid return home auto-trigger reason");
            }

            switch (reason) {
                case NONE:
                    updateAutoTriggerDelay(ReturnHomePilotingItfCore.NO_DELAY);
                    break;
                case BATTERY_CRITICAL_SOON:
                    updateAutoTriggerDelay(delay);
                    break;
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onAutoTriggerMode(@Nullable ArsdkFeatureRth.AutoTriggerMode autoTriggerMode) {
            if (autoTriggerMode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid return home auto-trigger mode");
            }

            mAutoTrigger = convert(autoTriggerMode);

            if (isConnected()) {
                mPilotingItf.autoTrigger().updateValue(mAutoTrigger);
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onEndingBehavior(@Nullable ArsdkFeatureRth.EndingBehavior endingBehavior) {
            if (endingBehavior == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid return home ending behavior");
            }

            mEndingBehavior = convert(endingBehavior);

            if (isConnected()) {
                mPilotingItf.getEndingBehavior().updateValue(mEndingBehavior);
            }

            mPilotingItf.notifyUpdated();
        }

        @Override
        public void onEndingHoveringAltitude(float current, float min, float max) {
            DoubleRange bounds = DoubleRange.of(min, max);
            ENDING_HOVERING_ALTITUDE_RANGE_SETTING.save(mDeviceDict, bounds);
            mPilotingItf.getEndingHoveringAltitude().updateBounds(bounds);

            mEndingHoveringAltitude = (double) current;
            if (isConnected()) {
                mPilotingItf.getEndingHoveringAltitude().updateValue(mEndingHoveringAltitude);
            }

            mPilotingItf.notifyUpdated();
        }

        /**
         * Update the new target location.
         *
         * @param latitude          new location latitude
         * @param longitude         new location longitude
         * @param altitude          new location altitude
         */
        private void onLocation(double latitude, double longitude, double altitude) {
           onLocation(latitude, longitude, altitude, null);
        }

        /**
         * Update the new target location and whether the gps was fixed on take off.
         *
         * @param latitude          new location latitude
         * @param longitude         new location longitude
         * @param altitude          new location altitude
         * @param gpsFixedOnTakeOff whether the gps was fixed on take off
         */
        private void onLocation(double latitude, double longitude, double altitude,
                                @Nullable Boolean gpsFixedOnTakeOff) {
            if (Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                    && Double.compare(longitude, UNKNOWN_COORDINATE) != 0) {
                mPilotingItf.updateLocation(latitude, longitude, altitude);
                if (gpsFixedOnTakeOff != null) {
                    mPilotingItf.updateGpsFixedOnTakeOff(gpsFixedOnTakeOff);
                }
            } else {
                mPilotingItf.resetLocation();
            }
        }
    };

    /** Backend of ReturnHomePilotingItfCore implementation. */
    private final class Backend extends ActivablePilotingItfController.Backend
            implements ReturnHomePilotingItfCore.Backend {

        @Override
        public boolean setAutoTrigger(boolean enabled) {
            boolean updating = applyAutoTrigger(enabled);
            AUTO_TRIGGER_PRESET.save(mPresetDict, enabled);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setPreferredTarget(@NonNull ReturnHomePilotingItf.Target preferredTarget) {
            boolean updating = applyPreferredTarget(preferredTarget);
            PREFERRED_TARGET_PRESET.save(mPresetDict, preferredTarget);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setEndingBehavior(@NonNull ReturnHomePilotingItf.EndingBehavior endingBehavior) {
            boolean updating = applyEndingBehavior(endingBehavior);
            ENDING_BEHAVIOR_PRESET.save(mPresetDict, endingBehavior);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setAutoStartOnDisconnectDelay(int delay) {
            boolean updating = applyAutoStartOnDisconnectDelay(delay);
            AUTOSTART_ON_DISCONNECT_DELAY_PRESET.save(mPresetDict, delay);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setEndingHoveringAltitude(double altitude) {
            boolean updating = applyEndingHoveringAltitude(altitude);
            ENDING_HOVERING_ALTITUDE_PRESET.save(mPresetDict, altitude);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setMinAltitude(double altitude) {
            boolean updating = applyMinAltitude(altitude);
            MIN_ALTITUDE_PRESET.save(mPresetDict, altitude);
            if (!updating) {
                mPilotingItf.notifyUpdated();
            }
            return updating;
        }

        @Override
        public void cancelAutoTrigger() {
            sendCommand(ArsdkFeatureRth.encodeCancelAutoTrigger());
        }

        @Override
        public void setCustomLocation(double latitude, double longitude, double altitude) {
            sendCommand(ArsdkFeatureRth.encodeSetCustomLocation(latitude, longitude, (float) altitude));
        }
    }
}
