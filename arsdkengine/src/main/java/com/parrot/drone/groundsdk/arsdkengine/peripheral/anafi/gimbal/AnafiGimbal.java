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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.gimbal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal.Axis;
import com.parrot.drone.groundsdk.internal.device.peripheral.GimbalCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal.FrameOfReference;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** Gimbal peripheral controller for Anafi family drones. */
public final class AnafiGimbal extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral settings. */
    private static final String SETTINGS_KEY = "gimbal";

    // preset store bindings

    /** Stabilized axes preset entry. */
    private static final StorageEntry<EnumSet<Axis>> STABILIZED_AXES_PRESET =
            StorageEntry.ofEnumSet("stabilizedAxes", Axis.class);

    /** Maximum speed preset entry. */
    private static final StorageEntry<EnumMap<Axis, Double>> MAX_SPEEDS_PRESET =
            StorageEntry.ofEnumToDoubleMap("maxSpeeds", Axis.class);

    // device specific store bindings

    /** Supported axes device setting. */
    private static final StorageEntry<EnumSet<Axis>> SUPPORTED_AXES_SETTING =
            StorageEntry.ofEnumSet("supportedAxes", Axis.class);

    /** Maximum speed setting range. */
    private static final StorageEntry<EnumMap<Axis, DoubleRange>> MAX_SPEEDS_RANGE_SETTING =
            StorageEntry.ofEnumToDoubleRangeMap("maxSpeedsRange", Axis.class);


    /** Gimbal peripheral for which this object is the backend. */
    @NonNull
    private final GimbalCore mGimbal;

    /** Gimbal control command encoder. */
    @NonNull
    private final GimbalControlCommandEncoder mGimbalControlEncoder;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Maximum speed setting value by axis. */
    @NonNull
    private EnumMap<Axis, Double> mMaxSpeeds;

    /**
     * Stabilization state by axis.
     * <p>
     * This field is {@code null} until stabilization is loaded or received from drone. We need to know whether it has
     * been initialized because stabilization command is non-acknowledged.
     */
    @Nullable
    private EnumSet<Axis> mStabilizedAxes;

    /** Pending stabilization change by axis. */
    @NonNull
    private final EnumSet<Axis> mPendingStabilizationChanges;

    /** Absolute attitude by axis. */
    @NonNull
    private final EnumMap<Axis, Double> mAbsoluteAttitude;

    /** Relative attitude by axis. */
    @NonNull
    private final EnumMap<Axis, Double> mRelativeAttitude;

    /** Absolute attitude bounds by axis. */
    @NonNull
    private final EnumMap<Axis, DoubleRange> mAbsoluteAttitudeBounds;

    /** Relative attitude bounds by axis. */
    @NonNull
    private final EnumMap<Axis, DoubleRange> mRelativeAttitudeBounds;

    /** Offset setting value by axis. */
    @NonNull
    private final EnumMap<Axis, Double> mOffsets;

    /** Whether attitude has been received from drone at least once. */
    private boolean mAttitudeReceived;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiGimbal(@NonNull DroneController droneController) {
        super(droneController);
        mGimbal = new GimbalCore(mComponentStore, mBackend);
        mGimbalControlEncoder = new GimbalControlCommandEncoder();
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;

        mMaxSpeeds = new EnumMap<>(Axis.class);
        mPendingStabilizationChanges = EnumSet.noneOf(Axis.class);
        mAbsoluteAttitude = new EnumMap<>(Axis.class);
        mRelativeAttitude = new EnumMap<>(Axis.class);
        mAbsoluteAttitudeBounds = new EnumMap<>(Axis.class);
        mRelativeAttitudeBounds = new EnumMap<>(Axis.class);
        mOffsets = new EnumMap<>(Axis.class);

        loadPersistedData();
        if (isPersisted()) {
            mGimbal.publish();
        }
    }

    @Override
    protected void onConnected() {
        applyPresets();

        DeviceController.Backend backend = mDeviceController.getProtocolBackend();
        if (backend != null) {
            backend.registerNoAckCommandEncoders(mGimbalControlEncoder);
        }

        mGimbal.publish();
    }

    @Override
    protected void onDisconnected() {
        // reset online-only settings
        mGimbal.cancelSettingsRollbacks()
               .updateLockedAxes(EnumSet.allOf(Axis.class))
               .updateCorrectableAxes(EnumSet.noneOf(Axis.class))
               .updateErrors(EnumSet.noneOf(Gimbal.Error.class))
               .updateCalibrationProcessState(Gimbal.CalibrationProcessState.NONE);

        for (Axis axis : EnumSet.allOf(Axis.class)) {
            mGimbal.updateAttitudeBounds(axis, null)
                   .updateAbsoluteAttitude(axis, 0)
                   .updateRelativeAttitude(axis, 0);
        }

        mPendingStabilizationChanges.clear();
        mAbsoluteAttitude.clear();
        mRelativeAttitude.clear();
        mRelativeAttitudeBounds.clear();
        mOffsets.clear();
        mAttitudeReceived = false;

        DeviceController.Backend backend = mDeviceController.getProtocolBackend();
        if (backend != null) {
            backend.unregisterNoAckCommandEncoders(mGimbalControlEncoder);
        }
        mGimbalControlEncoder.reset();

        mGimbal.updateOffsetCorrectionProcessState(false);

        if (isPersisted()) {
            mGimbal.notifyUpdated();
        } else {
            mGimbal.unpublish();
        }
    }

    @Override
    protected void onPresetChange() {
        // reload preset store
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mGimbal.notifyUpdated();
    }

    @Override
    protected void onForgetting() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mGimbal.unpublish();
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
        EnumSet<Axis> supportedAxes = SUPPORTED_AXES_SETTING.load(mDeviceDict);
        if (supportedAxes != null) {
            mGimbal.updateSupportedAxes(supportedAxes);
        }

        EnumMap<Axis, DoubleRange> maxSpeedRanges = MAX_SPEEDS_RANGE_SETTING.load(mDeviceDict);
        if (maxSpeedRanges != null) {
            mGimbal.updateMaxSpeedRanges(maxSpeedRanges);
        }

        EnumMap<Axis, Double> maxSpeeds = MAX_SPEEDS_PRESET.load(mPresetDict);
        if (maxSpeeds != null) {
            mMaxSpeeds = maxSpeeds;
            mGimbal.updateMaxSpeeds(maxSpeeds);
        }

        EnumSet<Axis> stabilizedAxes = STABILIZED_AXES_PRESET.load(mPresetDict);
        if (stabilizedAxes != null && supportedAxes != null) {
            mStabilizedAxes = stabilizedAxes;
            mGimbal.updateStabilization(stabilizedAxes);
        }
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyMaxSpeeds(MAX_SPEEDS_PRESET.load(mPresetDict), false);
        if (mAttitudeReceived) {
            applyStabilizationPreset();
        }
    }

    /**
     * Applies persisted gimbal presets.
     * <p>
     * This happens when both drone is connected and attitude has been received.
     */
    private void applyStabilizationPreset() {
        EnumSet<Axis> stabilizedAxes = STABILIZED_AXES_PRESET.load(mPresetDict);
        for (Axis axis : mGimbal.getSupportedAxes()) {
            applyStabilization(axis, stabilizedAxes != null ? stabilizedAxes.contains(axis) : null);
        }
    }

    /**
     * Applies maximum speed to all axes.
     * <ul>
     * <li>Gets the last received values if the given map is null;</li>
     * <li>Sends the obtained values to the drone in case they differ from the last received values or sending is
     * forced;</li>
     * <li>Saves the obtained values to the gimbal presets and updates the peripheral's settings accordingly.</li>
     * </ul>
     *
     * @param maxSpeeds           maximum speeds to apply
     * @param forceSendingCommand {@code true} to force sending values to the drone
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyMaxSpeeds(@Nullable EnumMap<Axis, Double> maxSpeeds, boolean forceSendingCommand) {
        // Validating given value
        if (maxSpeeds == null) {
            maxSpeeds = mMaxSpeeds;
        }

        boolean updating = false;
        if (forceSendingCommand || !maxSpeeds.equals(mMaxSpeeds)) {
            Double yaw = maxSpeeds.get(Axis.YAW);
            Double pitch = maxSpeeds.get(Axis.PITCH);
            Double roll = maxSpeeds.get(Axis.ROLL);
            updating = sendCommand(ArsdkFeatureGimbal.encodeSetMaxSpeed(0,
                    yaw != null ? (float) yaw.doubleValue() : 0f,
                    pitch != null ? (float) pitch.doubleValue() : 0f,
                    roll != null ? (float) roll.doubleValue() : 0f));
        }

        mMaxSpeeds = maxSpeeds;

        mGimbal.updateMaxSpeeds(maxSpeeds);
        return updating;
    }

    /**
     * Applies stabilization to the given axis.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Saves the obtained value to the camera presets and updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param axis       the axis
     * @param stabilized the stabilization to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyStabilization(@NonNull Axis axis, @Nullable Boolean stabilized) {
        if (mStabilizedAxes == null) {
            return false;
        }
        // Validating given value
        if (stabilized == null) {
            stabilized = mStabilizedAxes.contains(axis);
        }

        Double attitude = stabilized ? mAbsoluteAttitude.get(axis) : mRelativeAttitude.get(axis);

        boolean updating = false;
        if (stabilized != mStabilizedAxes.contains(axis) && isConnected() && mAttitudeReceived) {
            double targetAttitude;
            DoubleRange bounds = stabilized ? mAbsoluteAttitudeBounds.get(axis) : mRelativeAttitudeBounds.get(axis);
            if (bounds != null) {
                if (attitude != null) {
                    // if range and current attitude is known, the target attitude is the current attitude clamped into
                    // the range
                    targetAttitude = bounds.clamp(attitude);
                } else {
                    // if no current attitude, take the mid-range
                    targetAttitude = (bounds.getUpper() + bounds.getLower()) / 2.0;
                }
            } else {
                targetAttitude = 0;
            }
            mGimbalControlEncoder.setStabilization(axis, stabilized, targetAttitude);
            updating = true;
            mPendingStabilizationChanges.add(axis);
        }

        if (stabilized) {
            mStabilizedAxes.add(axis);
        } else {
            mStabilizedAxes.remove(axis);
        }

        // Update the attitude bounds to take the correct frame of reference according to the new stab
        mGimbal.updateStabilization(mStabilizedAxes)
               .updateAttitudeBounds(axis, stabilized ? mAbsoluteAttitudeBounds.get(axis)
                       : mRelativeAttitudeBounds.get(axis));

        return updating;
    }

    /**
     * Applies maximum speed to the given axis.
     * <ul>
     * <li>Sends the given value to the drone in case it differs from the last received value;</li>
     * <li>Saves it to the gimbal presets and updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param axis     the axis to which the new max speed will apply
     * @param maxSpeed maximum speed to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyMaxSpeed(@NonNull Axis axis, @NonNull Double maxSpeed) {
        if (!maxSpeed.equals(mMaxSpeeds.get(axis))) {
            mMaxSpeeds.put(axis, maxSpeed);
            return applyMaxSpeeds(null, true);
        }
        return false;
    }

    /**
     * Applies offset to the given axis.
     * <p>
     * Sends the offsets to the drone in case the given value differs from the last received value.
     *
     * @param axis   the axis to which the new offset will apply
     * @param offset offset to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyOffset(@NonNull Axis axis, @NonNull Double offset) {
        boolean updating = false;

        if (!offset.equals(mOffsets.get(axis))) {
            mOffsets.put(axis, offset);

            Double yaw = mOffsets.get(Axis.YAW);
            Double pitch = mOffsets.get(Axis.PITCH);
            Double roll = mOffsets.get(Axis.ROLL);

            updating = sendCommand(ArsdkFeatureGimbal.encodeSetOffsets(0,
                    yaw != null ? (float) yaw.doubleValue() : 0f,
                    pitch != null ? (float) pitch.doubleValue() : 0f,
                    roll != null ? (float) roll.doubleValue() : 0f));
        }
        return updating;
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureGimbal.UID) {
            ArsdkFeatureGimbal.decode(command, mGimbalCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureGimbal is decoded. */
    private final ArsdkFeatureGimbal.Callback mGimbalCallback = new ArsdkFeatureGimbal.Callback() {

        @Override
        public void onGimbalCapabilities(int gimbalId, @Nullable ArsdkFeatureGimbal.Model model, int axesBitField) {
            if (gimbalId != 0) {
                return;
            }

            EnumSet<Axis> supportedAxes = GimbalAxisAdapter.from(axesBitField);
            SUPPORTED_AXES_SETTING.save(mDeviceDict, supportedAxes);
            mGimbal.updateSupportedAxes(supportedAxes).notifyUpdated();
        }

        @Override
        public void onRelativeAttitudeBounds(int gimbalId, float minYaw, float maxYaw, float minPitch, float maxPitch,
                                             float minRoll, float maxRoll) {
            if (gimbalId != 0 || minYaw > maxYaw || minPitch > maxPitch || minRoll > maxRoll) {
                ULog.w(TAG, "Invalid gimbal relative attitude bounds parameters, skip this event");
                return;
            }

            // store the values as they may be used later (when axis stabilization changes)
            mRelativeAttitudeBounds.put(Axis.YAW, DoubleRange.of(minYaw, maxYaw));
            mRelativeAttitudeBounds.put(Axis.PITCH, DoubleRange.of(minPitch, maxPitch));
            mRelativeAttitudeBounds.put(Axis.ROLL, DoubleRange.of(minRoll, maxRoll));

            // update the bounds on the axes that are not stabilized (i.e.: frame of reference is relative)
            if (mStabilizedAxes != null) {
                for (Axis axis : EnumSet.complementOf(mStabilizedAxes)) {
                    mGimbal.updateAttitudeBounds(axis, mRelativeAttitudeBounds.get(axis));
                }
            }
            mGimbal.notifyUpdated();
        }

        @Override
        public void onAbsoluteAttitudeBounds(int gimbalId, float minYaw, float maxYaw, float minPitch, float maxPitch,
                                             float minRoll, float maxRoll) {
            if (gimbalId != 0 || minYaw > maxYaw || minPitch > maxPitch || minRoll > maxRoll) {
                ULog.w(TAG, "Invalid gimbal absolute attitude bounds parameters, skip this event");
                return;
            }

            // store the values as they may be used later (when axis stabilization changes)
            mAbsoluteAttitudeBounds.put(Axis.YAW, DoubleRange.of(minYaw, maxYaw));
            mAbsoluteAttitudeBounds.put(Axis.PITCH, DoubleRange.of(minPitch, maxPitch));
            mAbsoluteAttitudeBounds.put(Axis.ROLL, DoubleRange.of(minRoll, maxRoll));

            // update the bounds on the axes that are stabilized (i.e.: frame of reference is absolute)
            if (mStabilizedAxes != null) {
                for (Axis axis : mStabilizedAxes) {
                    mGimbal.updateAttitudeBounds(axis, mAbsoluteAttitudeBounds.get(axis));
                }
            }
            mGimbal.notifyUpdated();
        }

        /** Maximum speed ranges, used only in {@code onMaxSpeed()} method. */
        @NonNull
        private final EnumMap<Axis, DoubleRange> mMaxSpeedRanges = new EnumMap<>(Axis.class);

        @Override
        public void onMaxSpeed(int gimbalId, float minBoundYaw, float maxBoundYaw, float currentYaw,
                               float minBoundPitch, float maxBoundPitch, float currentPitch,
                               float minBoundRoll, float maxBoundRoll, float currentRoll) {
            if (gimbalId != 0
                || minBoundYaw > maxBoundYaw || minBoundPitch > maxBoundPitch || minBoundRoll > maxBoundRoll) {
                ULog.w(TAG, "Invalid gimbal max speed parameters, skip this event");
                return;
            }

            mMaxSpeedRanges.put(Axis.YAW, DoubleRange.of(minBoundYaw, maxBoundYaw));
            mMaxSpeedRanges.put(Axis.PITCH, DoubleRange.of(minBoundPitch, maxBoundPitch));
            mMaxSpeedRanges.put(Axis.ROLL, DoubleRange.of(minBoundRoll, maxBoundRoll));
            mMaxSpeeds.put(Axis.YAW, (double) currentYaw);
            mMaxSpeeds.put(Axis.PITCH, (double) currentPitch);
            mMaxSpeeds.put(Axis.ROLL, (double) currentRoll);

            MAX_SPEEDS_RANGE_SETTING.save(mDeviceDict, mMaxSpeedRanges);
            mGimbal.updateMaxSpeedRanges(mMaxSpeedRanges);

            if (isConnected()) {
                mGimbal.updateMaxSpeeds(mMaxSpeeds);
            }

            mGimbal.notifyUpdated();
        }

        /** Received stabilized axes, used only in {@code onAttitude()} method. */
        @NonNull
        private final EnumSet<Axis> mReceivedStabilizedAxes = EnumSet.noneOf(Axis.class);

        @Override
        public void onAttitude(int gimbalId, @Nullable FrameOfReference yawFrameOfReference,
                               @Nullable FrameOfReference pitchFrameOfReference,
                               @Nullable FrameOfReference rollFrameOfReference, float yawRelative,
                               float pitchRelative, float rollRelative, float yawAbsolute, float pitchAbsolute,
                               float rollAbsolute) {
            // This non-ack event may be received before the capabilities one. In this case we just ignore it.
            if (gimbalId != 0 || mGimbal.getSupportedAxes().isEmpty()) {
                return;
            }

            // Store internally the current attitude on each frame of reference
            mRelativeAttitude.put(Axis.YAW, (double) roundToSecondDecimal(yawRelative));
            mRelativeAttitude.put(Axis.PITCH, (double) roundToSecondDecimal(pitchRelative));
            mRelativeAttitude.put(Axis.ROLL, (double) roundToSecondDecimal(rollRelative));
            mAbsoluteAttitude.put(Axis.YAW, (double) roundToSecondDecimal(yawAbsolute));
            mAbsoluteAttitude.put(Axis.PITCH, (double) roundToSecondDecimal(pitchAbsolute));
            mAbsoluteAttitude.put(Axis.ROLL, (double) roundToSecondDecimal(rollAbsolute));

            mReceivedStabilizedAxes.clear();
            if (yawFrameOfReference == FrameOfReference.ABSOLUTE) {
                mReceivedStabilizedAxes.add(Axis.YAW);
            }
            if (pitchFrameOfReference == FrameOfReference.ABSOLUTE) {
                mReceivedStabilizedAxes.add(Axis.PITCH);
            }
            if (rollFrameOfReference == FrameOfReference.ABSOLUTE) {
                mReceivedStabilizedAxes.add(Axis.ROLL);
            }

            boolean settingChanged = false;

            if (!mAttitudeReceived) {
                mAttitudeReceived = true;
                mStabilizedAxes = mReceivedStabilizedAxes.clone();
                for (Axis axis : mGimbal.getSupportedAxes()) {
                    mGimbalControlEncoder.setStabilization(axis, mReceivedStabilizedAxes.contains(axis), null);
                }
                if (isConnected()) {
                    applyStabilizationPreset();
                }
                settingChanged = true;
            }

            for (Axis axis : mGimbal.getSupportedAxes()) {
                // Update the stabilization information according to the frame of reference on each axis if a
                // change has been previously requested and the received stabilization matches the desired one,
                // or if it has changed without being requested
                //noinspection ConstantConditions
                if (mPendingStabilizationChanges.contains(axis) ==
                    (mStabilizedAxes.contains(axis) == mReceivedStabilizedAxes.contains(axis))) {
                    if (mReceivedStabilizedAxes.contains(axis)) {
                        mStabilizedAxes.add(axis);
                    } else {
                        mStabilizedAxes.remove(axis);
                    }

                    if (!mPendingStabilizationChanges.remove(axis)) {
                        // If an unexpected stabilization change is received, we need to update the encoder
                        mGimbalControlEncoder.setStabilization(axis, mReceivedStabilizedAxes.contains(axis), null);
                    }
                    settingChanged = true;
                }

                // Update the attitude bounds according to the frame of reference that has been requested
                //noinspection ConstantConditions: validated by mStabilizedAxes.contains
                mGimbal.updateAbsoluteAttitude(axis, mAbsoluteAttitude.get(axis))
                       .updateRelativeAttitude(axis, mRelativeAttitude.get(axis))
                       .updateAttitudeBounds(axis, mStabilizedAxes.contains(axis) ?
                               mAbsoluteAttitudeBounds.get(axis) : mRelativeAttitudeBounds.get(axis));
            }

            if (settingChanged && isConnected()) {
                assert mStabilizedAxes != null;
                mGimbal.updateStabilization(mStabilizedAxes);
                mStabilizedAxes.retainAll(mGimbal.getSupportedAxes());
            }

            mGimbal.notifyUpdated();
        }

        @Override
        public void onAxisLockState(int gimbalId, int lockedBitField) {
            if (gimbalId != 0) {
                return;
            }

            mGimbal.updateLockedAxes(GimbalAxisAdapter.from(lockedBitField)).notifyUpdated();
        }

        /** Offset ranges, used only in {@code onOffsets()} method. */
        @NonNull
        private final EnumMap<Axis, DoubleRange> mOffsetsRanges = new EnumMap<>(Axis.class);

        @Override
        public void onOffsets(int gimbalId, @Nullable ArsdkFeatureGimbal.State updateState,
                              float minBoundYaw, float maxBoundYaw, float currentYaw,
                              float minBoundPitch, float maxBoundPitch, float currentPitch,
                              float minBoundRoll, float maxBoundRoll, float currentRoll) {
            if (gimbalId != 0
                || minBoundYaw > maxBoundYaw || minBoundPitch > maxBoundPitch || minBoundRoll > maxBoundRoll) {
                ULog.w(TAG, "Invalid gimbal offsets parameters, skip this event");
                return;
            }

            if (updateState == ArsdkFeatureGimbal.State.ACTIVE) {
                EnumSet<Axis> calibratableAxes = EnumSet.noneOf(Axis.class);
                if (Float.compare(minBoundYaw, maxBoundYaw) != 0) {
                    calibratableAxes.add(Axis.YAW);
                    mOffsetsRanges.put(Axis.YAW, DoubleRange.of(minBoundYaw, maxBoundYaw));
                    mOffsets.put(Axis.YAW, (double) currentYaw);
                }
                if (Float.compare(minBoundPitch, maxBoundPitch) != 0) {
                    calibratableAxes.add(Axis.PITCH);
                    mOffsetsRanges.put(Axis.PITCH, DoubleRange.of(minBoundPitch, maxBoundPitch));
                    mOffsets.put(Axis.PITCH, (double) currentPitch);
                }
                if (Double.compare(minBoundRoll, maxBoundRoll) != 0) {
                    calibratableAxes.add(Axis.ROLL);
                    mOffsetsRanges.put(Axis.ROLL, DoubleRange.of(minBoundRoll, maxBoundRoll));
                    mOffsets.put(Axis.ROLL, (double) currentRoll);
                }

                mGimbal.updateOffsetCorrectionProcessState(true)
                       .updateCorrectableAxes(calibratableAxes)
                       .updateOffsetsRanges(mOffsetsRanges)
                       .updateOffsets(mOffsets);
            } else {
                mGimbal.updateOffsetCorrectionProcessState(false);
            }

            mGimbal.notifyUpdated();
        }

        @Override
        public void onCalibrationState(@Nullable ArsdkFeatureGimbal.CalibrationState state, int gimbalId) {
            if (gimbalId != 0 || state == null) {
                return;
            }

            switch (state) {
                case OK:
                    mGimbal.updateIsCalibrated(true).notifyUpdated();
                    break;
                case REQUIRED:
                    mGimbal.updateIsCalibrated(false).notifyUpdated();
                    break;
                case IN_PROGRESS:
                    mGimbal.updateCalibrationProcessState(Gimbal.CalibrationProcessState.CALIBRATING).notifyUpdated();
                    break;
            }
        }

        @Override
        public void onCalibrationResult(int gimbalId, @Nullable ArsdkFeatureGimbal.CalibrationResult result) {
            if (gimbalId != 0 || result == null) {
                return;
            }

            switch (result) {
                case SUCCESS:
                    mGimbal.updateCalibrationProcessState(Gimbal.CalibrationProcessState.SUCCESS).notifyUpdated();
                    break;
                case FAILURE:
                    mGimbal.updateCalibrationProcessState(Gimbal.CalibrationProcessState.FAILURE).notifyUpdated();
                    break;
                case CANCELED:
                    mGimbal.updateCalibrationProcessState(Gimbal.CalibrationProcessState.CANCELED).notifyUpdated();
                    break;
            }
            // SUCCESS, FAILURE and CANCELED status are transient, reset calibration process state to NONE
            mGimbal.updateCalibrationProcessState(Gimbal.CalibrationProcessState.NONE).notifyUpdated();
        }

        @Override
        public void onAlert(int gimbalId, int errorBitField) {
            if (gimbalId != 0) {
                return;
            }
            mGimbal.updateErrors(ArsdkFeatureGimbal.Error
                    .fromBitfield(errorBitField)
                    .stream()
                    .map(AnafiGimbal::convertError)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Gimbal.Error.class))))
                   .notifyUpdated();
        }
    };

    /**
     * Converts an arsdk gimbal error to its gsdk {@code Error} equivalent.
     *
     * @param error arsdk error to convert
     *
     * @return gsdk representation of the specified error
     */
    @NonNull
    private static Gimbal.Error convertError(@NonNull ArsdkFeatureGimbal.Error error) {
        switch (error) {
            case CALIBRATION_ERROR:
                return Gimbal.Error.CALIBRATION;
            case OVERLOAD_ERROR:
                return Gimbal.Error.OVERLOAD;
            case COMM_ERROR:
                return Gimbal.Error.COMMUNICATION;
            case CRITICAL_ERROR:
                return Gimbal.Error.CRITICAL;
        }
        return null;
    }

    /**
     * Rounds the given value to the second decimal.
     *
     * @param value the value to round
     *
     * @return the rounded value
     */
    private static float roundToSecondDecimal(float value) {
        return Math.round(value * 100f) / 100f;
    }

    /** Backend of GimbalCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final GimbalCore.Backend mBackend = new GimbalCore.Backend() {

        @Override
        public boolean setMaxSpeed(@NonNull Axis axis, double speed) {
            boolean updating = applyMaxSpeed(axis, speed);
            MAX_SPEEDS_PRESET.save(mPresetDict, mMaxSpeeds);
            if (!updating) {
                mGimbal.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setStabilization(@NonNull Axis axis, boolean stabilized) {
            boolean updating = applyStabilization(axis, stabilized);
            STABILIZED_AXES_PRESET.save(mPresetDict, mStabilizedAxes);
            if (!updating) {
                mGimbal.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setOffset(@NonNull Axis axis, double offset) {
            if (applyOffset(axis, offset)) {
                return true;
            }

            mGimbal.notifyUpdated();

            return false;
        }

        @Override
        public void control(@NonNull Gimbal.ControlMode mode, @Nullable Double yaw, @Nullable Double pitch,
                            @Nullable Double roll) {
            if (mAttitudeReceived) {
                mGimbalControlEncoder.control(mode, yaw, pitch, roll);
            }
        }

        @Override
        public boolean startOffsetCorrectionProcess() {
            return sendCommand(ArsdkFeatureGimbal.encodeStartOffsetsUpdate(0));
        }

        @Override
        public boolean stopOffsetCorrectionProcess() {
            return sendCommand(ArsdkFeatureGimbal.encodeStopOffsetsUpdate(0));
        }

        @Override
        public boolean startCalibration() {
            return sendCommand(ArsdkFeatureGimbal.encodeCalibrate(0));
        }

        @Override
        public boolean cancelCalibration() {
            return sendCommand(ArsdkFeatureGimbal.encodeCancelCalibration(0));
        }
    };
}
