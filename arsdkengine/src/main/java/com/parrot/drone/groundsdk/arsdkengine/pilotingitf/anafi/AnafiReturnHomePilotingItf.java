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

/** ReturnHome piloting interface controller for Anafi family drones. */
public class AnafiReturnHomePilotingItf extends ActivablePilotingItfController {

    /** Key used to access preset and range dictionaries for this piloting interface settings. */
    private static final String SETTINGS_KEY = "returnHome";

    // preset store bindings

    /** Preferred target preset entry. */
    private static final StorageEntry<ReturnHomePilotingItf.Target> PREFERRED_TARGET_PRESET =
            StorageEntry.ofEnum("preferredTarget", ReturnHomePilotingItf.Target.class);

    /** Auto-start on disconnect delay preset entry. */
    private static final StorageEntry<Integer> AUTOSTART_ON_DISCONNECT_DELAY_PRESET =
            StorageEntry.ofInteger("autoStartOnDisconnectDelay");

    /** Minimum altitude preset entry. */
    private static final StorageEntry<Double> MIN_ALTITUDE_PRESET = StorageEntry.ofDouble("minAltitude");

    // device specific store bindings

    /** Auto-start on disconnect delay range device setting. */
    private static final StorageEntry<IntegerRange> AUTOSTART_ON_DISCONNECT_DELAY_RANGE_SETTING =
            StorageEntry.ofIntegerRange("autoStartOnDisconnectDelayRange");

    /** Minimum altitude range device setting. */
    private static final StorageEntry<DoubleRange> MIN_ALTITUDE_RANGE_SETTING =
            StorageEntry.ofDoubleRange("minAltitudeRange");

    /** Autostart on disconnect delay range, as hardcoded in the drone, in seconds. */
    private static final IntegerRange AUTOSTART_ON_DISCONNECT_DELAY_RANGE = IntegerRange.of(0, 120);

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final ReturnHomePilotingItfCore mPilotingItf;

    /** Persists device specific values for this piloting interface, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Persists current preset values for this piloting interface. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Preferred target. */
    @Nullable
    private ReturnHomePilotingItf.Target mPreferredTarget;

    /** Auto-start on disconnect delay. */
    @Nullable
    private Integer mAutoStartOnDisconnectDelay;

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
    protected final void onConnected() {
        super.onConnected();
        applyPresets();
        mPilotingItf.publish();
    }

    @Override
    protected final void onConnecting() {
        if (!isPersisted()) {
            // mock receiving auto-start on disconnect delay range, that the drone does not send
            AUTOSTART_ON_DISCONNECT_DELAY_RANGE_SETTING.save(mDeviceDict, AUTOSTART_ON_DISCONNECT_DELAY_RANGE);
            mPilotingItf.getAutoStartOnDisconnectDelay().updateBounds(AUTOSTART_ON_DISCONNECT_DELAY_RANGE);
        }
    }

    @Override
    protected final void onDisconnected() {
        // clear all non saved settings
        mPilotingItf.cancelSettingsRollbacks()
                    .resetLocation()
                    .updateCurrentTarget(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION, false);
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
        if (featureId == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
        } else if (featureId == ArsdkFeatureArdrone3.GPSSettingsState.UID) {
            ArsdkFeatureArdrone3.GPSSettingsState.decode(command, mGpsSettingsStateCallback);
        } else if (featureId == ArsdkFeatureArdrone3.GPSState.UID) {
            ArsdkFeatureArdrone3.GPSState.decode(command, mGpsStateCallback);
        } else if (featureId == ArsdkFeatureRth.UID) {
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
        applyPreferredTarget(PREFERRED_TARGET_PRESET.load(mPresetDict));
        applyAutoStartOnDisconnectDelay(AUTOSTART_ON_DISCONNECT_DELAY_PRESET.load(mPresetDict));
        applyMinAltitude(MIN_ALTITUDE_PRESET.load(mPresetDict));
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
                           && sendCommand(ArsdkFeatureArdrone3.GPSSettings.encodeHomeType(convert(preferredTarget)));

        mPreferredTarget = preferredTarget;
        mPilotingItf.getPreferredTarget()
                    .updateValue(preferredTarget);

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
                           && sendCommand(ArsdkFeatureArdrone3.GPSSettings.encodeReturnHomeDelay(delay));

        mAutoStartOnDisconnectDelay = delay;
        mPilotingItf.getAutoStartOnDisconnectDelay()
                    .updateValue(delay);

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
                ArsdkFeatureArdrone3.GPSSettings.encodeReturnHomeMinAltitude(altitude.floatValue()));

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
     * Converts a groundsdk {@link ReturnHomePilotingItf.Target return home target} into its arsdk
     * {@link ArsdkFeatureArdrone3.GpssettingsHometypeType representation}.
     *
     * @param target groundsdk return home target to convert
     *
     * @return arsdk representation of the specified target
     */
    @NonNull
    private static ArsdkFeatureArdrone3.GpssettingsHometypeType convert(@NonNull ReturnHomePilotingItf.Target target) {
        switch (target) {
            case TAKE_OFF_POSITION:
                return ArsdkFeatureArdrone3.GpssettingsHometypeType.TAKEOFF;
            case CONTROLLER_POSITION:
                return ArsdkFeatureArdrone3.GpssettingsHometypeType.PILOT;
            case TRACKED_TARGET_POSITION:
                return ArsdkFeatureArdrone3.GpssettingsHometypeType.FOLLOWEE;
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
     * Converts an arsdk {@link ArsdkFeatureArdrone3.GpssettingsstateHometypechangedType home type} into its
     * groundsdk {@link ReturnHomePilotingItf.Target representation}.
     *
     * @param type arsdk home type to convert
     *
     * @return groundsdk representation of the specified type
     */
    @NonNull
    private static ReturnHomePilotingItf.Target convert(
            @NonNull ArsdkFeatureArdrone3.GpssettingsstateHometypechangedType type) {
        switch (type) {
            case TAKEOFF:
                return ReturnHomePilotingItf.Target.TAKE_OFF_POSITION;
            case PILOT:
                return ReturnHomePilotingItf.Target.CONTROLLER_POSITION;
            case FOLLOWEE:
                return ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureArdrone3.GpsstateHometypechosenchangedType home type} into its
     * groundsdk {@link ReturnHomePilotingItf.Target representation}.
     *
     * @param type arsdk home type to convert
     *
     * @return groundsdk representation of the specified type
     */
    @NonNull
    private static ReturnHomePilotingItf.Target convert(
            @NonNull ArsdkFeatureArdrone3.GpsstateHometypechosenchangedType type) {
        switch (type) {
            case TAKEOFF:
            case FIRST_FIX:
                return ReturnHomePilotingItf.Target.TAKE_OFF_POSITION;
            case PILOT:
                return ReturnHomePilotingItf.Target.CONTROLLER_POSITION;
            case FOLLOWEE:
                return ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION;
        }
        return null;
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                @Override
                public void onNavigateHomeStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateNavigatehomestatechangedState state,
                        @Nullable ArsdkFeatureArdrone3.PilotingstateNavigatehomestatechangedReason reason) {

                    if (state != null) switch (state) {
                        case AVAILABLE:
                            if (reason == ArsdkFeatureArdrone3.PilotingstateNavigatehomestatechangedReason.FINISHED) {
                                mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.FINISHED);
                            } else {
                                mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.NONE);
                            }
                            notifyIdle();
                            break;
                        case PENDING:
                        case INPROGRESS:
                            // reset the auto trigger if any
                            updateAutoTriggerDelay(ReturnHomePilotingItfCore.NO_DELAY);
                            if (reason != null) switch (reason) {
                                case USERREQUEST:
                                    mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.USER_REQUESTED);
                                    break;
                                case CONNECTIONLOST:
                                    mPilotingItf.updateReason(ReturnHomePilotingItf.Reason.CONNECTION_LOST);
                                    break;
                                case LOWBATTERY:
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
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.GpsSettingsState is decoded. */
    private final ArsdkFeatureArdrone3.GPSSettingsState.Callback mGpsSettingsStateCallback =
            new ArsdkFeatureArdrone3.GPSSettingsState.Callback() {

                /** Special value sent by the drone when either latitude or longitude is not known. */
                private static final double UNKNOWN_COORDINATE = 500;

                @Override
                public void onHomeChanged(double latitude, double longitude, double altitude) {
                    if (Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(longitude, UNKNOWN_COORDINATE) != 0) {
                        mPilotingItf.updateLocation(latitude, longitude, altitude).notifyUpdated();
                    } else {
                        mPilotingItf.resetLocation().notifyUpdated();
                    }
                }

                @Override
                public void onHomeTypeChanged(@Nullable ArsdkFeatureArdrone3.GpssettingsstateHometypechangedType type) {
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
                public void onReturnHomeDelayChanged(int delay) {
                    // Min and max delay are hardcoded on drone side
                    mAutoStartOnDisconnectDelay = delay;
                    if (isConnected()) {
                        mPilotingItf.getAutoStartOnDisconnectDelay().updateValue(mAutoStartOnDisconnectDelay);
                    }

                    mPilotingItf.notifyUpdated();
                }

                @Override
                public void onReturnHomeMinAltitudeChanged(float value, float min, float max) {
                    if (min > max) {
                        throw new ArsdkCommand.RejectedEventException(
                                "Invalid min altitude bounds [min: " + min + ", max: " + max + "]");
                    }

                    DoubleRange bounds = DoubleRange.of(min, max);
                    MIN_ALTITUDE_RANGE_SETTING.save(mDeviceDict, bounds);
                    mPilotingItf.getMinAltitude().updateBounds(bounds);

                    mMinAltitude = (double) value;
                    if (isConnected()) {
                        mPilotingItf.getMinAltitude().updateValue(mMinAltitude);
                    }

                    mPilotingItf.notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.GpsState is decoded. */
    private final ArsdkFeatureArdrone3.GPSState.Callback mGpsStateCallback =
            new ArsdkFeatureArdrone3.GPSState.Callback() {

                @Override
                public void onHomeTypeChosenChanged(
                        @Nullable ArsdkFeatureArdrone3.GpsstateHometypechosenchangedType type) {
                    if (type == null) {
                        throw new ArsdkCommand.RejectedEventException("Invalid return home type");
                    }

                    mPilotingItf.updateCurrentTarget(convert(type),
                            type != ArsdkFeatureArdrone3.GpsstateHometypechosenchangedType.FIRST_FIX)
                                .notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureRth is decoded. */
    private final ArsdkFeatureRth.Callback mRthCallback = new ArsdkFeatureRth.Callback() {

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
    };

    /** Backend of ReturnHomePilotingItfCore implementation. */
    private final class Backend extends ActivablePilotingItfController.Backend
            implements ReturnHomePilotingItfCore.Backend {

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
        public boolean setAutoStartOnDisconnectDelay(int delay) {
            boolean updating = applyAutoStartOnDisconnectDelay(delay);
            AUTOSTART_ON_DISCONNECT_DELAY_PRESET.save(mPresetDict, delay);
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
    }
}
