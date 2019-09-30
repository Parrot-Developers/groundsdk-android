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

package com.parrot.drone.groundsdk.internal.device.peripheral.tracking;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/**
 * Core class for TargetTracker.
 */
public class TargetTrackerCore extends SingletonComponentCore implements TargetTracker {

    /** Default value for both horizontal and vertical framing position. */
    public static final double DEFAULT_FRAMING_POSITION = 0.5;

    /** Description of TargetTracker. */
    private static final ComponentDescriptor<Peripheral, TargetTracker> DESC =
            ComponentDescriptor.of(TargetTracker.class);

    /** Engine-specific backend for TargetTracker. */
    public interface Backend {

        /**
         * Enables or disables controller tracking.
         *
         * @param enable {@code true} to enable controller tracking, {@code false} to disable it
         */
        void enableControllerTracking(boolean enable);

        /**
         * Sets target framing position setting.
         *
         * @param horizontalPosition target horizontal position in frame
         * @param verticalPosition   target vertical position in frame
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setTargetPosition(@FloatRange(from = 0, to = 1) double horizontalPosition,
                                  @FloatRange(from = 0, to = 1) double verticalPosition);

        /**
         * Sends target detection info to the drone.
         *
         * @param info target detection info to send
         */
        void sendTargetDetectionInfo(@NonNull TargetDetectionInfo info);
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Framing setting. */
    @NonNull
    private final FramingSettingCore mFraming;

    /** {@code true} when controller tracking is enabled. */
    private boolean mControllerTracking;

    /** Tracked target current trajectory, {@code null} if unknown. */
    @Nullable
    private TargetTrajectoryCore mTargetTrajectory;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public TargetTrackerCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mFraming = new FramingSettingCore(this::onSettingChange, mBackend::setTargetPosition);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @NonNull
    @Override
    public FramingSettingCore framing() {
        return mFraming;
    }

    @Override
    public void enableControllerTracking() {
        if (!mControllerTracking) {
            mBackend.enableControllerTracking(true);
        }
    }

    @Override
    public void disableControllerTracking() {
        if (mControllerTracking) {
            mBackend.enableControllerTracking(false);
        }
    }

    @Override
    public boolean isControllerTrackingEnabled() {
        return mControllerTracking;
    }

    @Override
    public void sendTargetDetectionInfo(@NonNull TargetDetectionInfo info) {
        mBackend.sendTargetDetectionInfo(info);
    }

    @Nullable
    @Override
    public TargetTrajectory getTargetTrajectory() {
        return mTargetTrajectory;
    }

    /**
     * Updates target position in frame.
     *
     * @param horizontalPosition target horizontal position in frame
     * @param verticalPosition   target vertical position in frame
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public TargetTrackerCore updateTargetPosition(@FloatRange(from = 0, to = 1) double horizontalPosition,
                                                  @FloatRange(from = 0, to = 1) double verticalPosition) {
        mFraming.update(horizontalPosition, verticalPosition);
        return this;
    }

    /**
     * Updates controller tracking flag.
     *
     * @param tracking {@code true} to indicate controller tracking is enabled, otherwise {@code false}
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public TargetTrackerCore updateControllerTrackingFlag(boolean tracking) {
        if (mControllerTracking != tracking) {
            mControllerTracking = tracking;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current target trajectory.
     *
     * @param latitude   target latitude
     * @param longitude  target longitude
     * @param altitude   target altitude
     * @param northSpeed target north speed
     * @param eastSpeed  target east speed
     * @param downSpeed  target down speed
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public TargetTrackerCore updateTargetTrajectory(double latitude, double longitude, double altitude,
                                                    double northSpeed, double eastSpeed, double downSpeed) {
        if (mTargetTrajectory == null) {
            mTargetTrajectory = new TargetTrajectoryCore();
            mChanged = true;
        }
        mChanged |= mTargetTrajectory.update(latitude, longitude, altitude, northSpeed, eastSpeed, downSpeed);
        return this;
    }

    /**
     * Clears current target trajectory.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public TargetTrackerCore clearTargetTrajectory() {
        if (mTargetTrajectory != null) {
            mTargetTrajectory = null;
            mChanged = true;
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public TargetTrackerCore cancelSettingsRollbacks() {
        mFraming.cancelRollback();
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
