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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import android.location.Location;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.value.BooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.IntSettingCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/**
 * Core class for ReturnHomePilotingItf.
 */
public final class ReturnHomePilotingItfCore extends ActivablePilotingItfCore implements ReturnHomePilotingItf {

    /** Description of ReturnHomePilotingItf. */
    private static final ComponentDescriptor<PilotingItf, ReturnHomePilotingItf> DESC =
            ComponentDescriptor.of(ReturnHomePilotingItf.class);

    /** Value used to mark that the a delay is invalid (either reset or never set). */
    public static final int NO_DELAY = -1;

    /** Backend of a ReturnHomePilotingItfCore which handles the messages. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Updates the auto trigger setting.
         *
         * @param enabled the auto trigger to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setAutoTrigger(boolean enabled);

        /**
         * Updates the preferred target setting.
         *
         * @param preferredTarget the preferred target value to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setPreferredTarget(@NonNull Target preferredTarget);

        /**
         * Updates the ending behavior setting.
         *
         * @param endingBehavior the ending behavior value to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setEndingBehavior(@NonNull EndingBehavior endingBehavior);

        /**
         * Updates the auto-start on disconnect delay setting.
         *
         * @param delay the delay value to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setAutoStartOnDisconnectDelay(int delay);

        /**
         * Updates the ending hovering altitude setting.
         *
         * @param altitude the altitude value to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setEndingHoveringAltitude(double altitude);

        /**
         * Updates the minimum altitude setting.
         *
         * @param altitude the altitude value to send to the drone
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMinAltitude(double altitude);

        /**
         * Cancels return home auto trigger.
         */
        void cancelAutoTrigger();

        /**
         * Updates the return home custom location.
         *
         * @param latitude the custom location latitude to send to the drone
         * @param longitude the custom location longitude to send to the drone
         * @param altitude the custom location altitude to send to the drone
         */
        void setCustomLocation(double latitude, double longitude, double altitude);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Auto trigger setting. */
    @NonNull
    private final BooleanSettingCore mAutoTriggerSetting;

    /** Preferred target setting. */
    @NonNull
    private final EnumSettingCore<Target> mPreferredTargetSetting;

    /** Ending behavior setting. */
    @NonNull
    private final EnumSettingCore<EndingBehavior> mEndingBehaviorSetting;

    /** Auto-start on disconnect delay setting. */
    @NonNull
    private final IntSettingCore mAutoStartOnDisconnectDelaySetting;

    /** Ending hovering altitude setting. */
    @NonNull
    private final OptionalDoubleSettingCore mEndingHoveringAltitudeSetting;

    /** Minimum altitude setting. */
    @NonNull
    private final OptionalDoubleSettingCore mMinAltitudeSetting;

    /**
     * Indicates whether first GPS fix was made at or after take-off. Irrelevant in other modes than
     * TAKE_OFF_POSITION.
     */
    private boolean mGpsFixedOnTakeOff;

    /** Current return home latitude. */
    private double mLatitude;

    /** Current return home longitude. */
    private double mLongitude;

    /** Current return home altitude. */
    private double mAltitude;

    /**
     * Time of the latest return home location update. {@link #NO_TIMESTAMP}  if location has been reset or never been
     * updated.
     */
    private long mLocationTimeStamp;

    /** Current reason for return home to be active. {@link Reason#NONE} if return home is inactive. */
    @NonNull
    private Reason mReason;

    /** Current return home target. */
    @NonNull
    private Target mCurrentTarget;

    /** Estimation of possibility for the drone to reach its return point. */
    @NonNull
    private Reachability mReachability;

    /**
     * Delay before planned automatic safety return.
     * <p>
     * {@link #NO_DELAY} if there is no planned safety return.
     */
    private long mAutoTriggerDelay;

    /** Value used to mark that the a timestamp is invalid (either reset or never set). */
    private static final int NO_TIMESTAMP = -1;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    public ReturnHomePilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
        mCurrentTarget = Target.TAKE_OFF_POSITION;
        mReason = Reason.NONE;
        mReachability = Reachability.UNKNOWN;
        mAutoTriggerDelay = NO_DELAY;
        mLocationTimeStamp = NO_TIMESTAMP;
        mAutoTriggerSetting = new BooleanSettingCore(new SettingController(this::onSettingChange),
                mBackend::setAutoTrigger);
        mPreferredTargetSetting = new EnumSettingCore<>(Target.TAKE_OFF_POSITION,
                new SettingController(this::onSettingChange), mBackend::setPreferredTarget);
        mEndingBehaviorSetting = new EnumSettingCore<>(EndingBehavior.HOVERING,
                new SettingController(this::onSettingChange), mBackend::setEndingBehavior);
        mAutoStartOnDisconnectDelaySetting = new IntSettingCore(new SettingController(this::onSettingChange),
                mBackend::setAutoStartOnDisconnectDelay);
        mEndingHoveringAltitudeSetting = new OptionalDoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setEndingHoveringAltitude);
        mMinAltitudeSetting = new OptionalDoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setMinAltitude);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @Override
    public boolean activate() {
        return getState() == State.IDLE && mBackend.activate();
    }

    @NonNull
    @Override
    public Reason getReason() {
        return mReason;
    }

    @Nullable
    @Override
    public Location getHomeLocation() {
        Location location = null;
        if (mLocationTimeStamp != NO_TIMESTAMP) {
            location = new Location((String) null);
            location.setLatitude(mLatitude);
            location.setLongitude(mLongitude);
            location.setAltitude(mAltitude);
            location.setTime(mLocationTimeStamp);
        }
        return location;
    }

    @NonNull
    @Override
    public Target getCurrentTarget() {
        return mCurrentTarget;
    }

    @Override
    public boolean gpsWasFixedOnTakeOff() {
        return mGpsFixedOnTakeOff;
    }

    @NonNull
    @Override
    public EnumSettingCore<Target> getPreferredTarget() {
        return mPreferredTargetSetting;
    }

    @NonNull
    @Override
    public EnumSettingCore<EndingBehavior> getEndingBehavior() {
        return mEndingBehaviorSetting;
    }

    @NonNull
    @Override
    public IntSettingCore getAutoStartOnDisconnectDelay() {
        return mAutoStartOnDisconnectDelaySetting;
    }

    @NonNull
    @Override
    public OptionalDoubleSettingCore getEndingHoveringAltitude() {
        return mEndingHoveringAltitudeSetting;
    }

    @NonNull
    @Override
    public OptionalDoubleSettingCore getMinAltitude() {
        return mMinAltitudeSetting;
    }

    @NonNull
    @Override
    public Reachability getHomeReachability() {
        return mReachability;
    }

    @Override
    @IntRange(from = 0)
    public long getAutoTriggerDelay() {
        if (mAutoTriggerDelay != NO_DELAY) {
            return mAutoTriggerDelay;
        }
        return 0;
    }

    @Override
    public void cancelAutoTrigger() {
        if (mReachability == Reachability.WARNING) {
            mBackend.cancelAutoTrigger();
        }
    }

    @NonNull
    @Override
    public BooleanSettingCore autoTrigger() {
        return mAutoTriggerSetting;
    }

    @Override
    public void setCustomLocation(double latitude, double longitude, double altitude) {
        if (mPreferredTargetSetting.getValue() == Target.CUSTOM_LOCATION) {
            mBackend.setCustomLocation(latitude, longitude, altitude);
        }
    }

    /**
     * Updates the return home reason after a change in the backend.
     *
     * @param newReason the new reason.
     *
     * @return {@code this}, to allow chained calls.
     */
    @NonNull
    public ReturnHomePilotingItfCore updateReason(@NonNull Reason newReason) {
        if (mReason != newReason) {
            mReason = newReason;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current return home target after a change in the backend.
     *
     * @param newTarget the new target
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomePilotingItfCore updateCurrentTarget(@NonNull Target newTarget) {
        if (newTarget != mCurrentTarget) {
            mCurrentTarget = newTarget;
            mChanged = true;
        }
        return this;
    }
    /**
     * Updates if the gps was fixed on take off after a change in the backend.
     *
     * @param gpsFixedOnTakeOff {@code true} if GPS was fixed upon take-off, {@code false} otherwise. Only relevant
     *                          when target is {@link Target#TAKE_OFF_POSITION}.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomePilotingItfCore updateGpsFixedOnTakeOff(boolean gpsFixedOnTakeOff) {
        if (gpsFixedOnTakeOff != mGpsFixedOnTakeOff) {
            mGpsFixedOnTakeOff = gpsFixedOnTakeOff;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the return home location after a change in the backend.
     *
     * @param latitude  the new latitude
     * @param longitude the new longitude
     * @param altitude  the new altitude
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomePilotingItfCore updateLocation(double latitude, double longitude, double altitude) {
        if (mLocationTimeStamp == NO_TIMESTAMP
            || Double.compare(mLatitude, latitude) != 0
            || Double.compare(mLongitude, longitude) != 0
            || Double.compare(mAltitude, altitude) != 0) {
            mLocationTimeStamp = System.currentTimeMillis();
            mLatitude = latitude;
            mLongitude = longitude;
            mAltitude = altitude;
            mChanged = true;
        }
        return this;
    }

    /**
     * Resets the current home location.
     *
     * @return {@code this}, to allow chained calls.
     */
    @NonNull
    public ReturnHomePilotingItfCore resetLocation() {
        if (mLocationTimeStamp != NO_TIMESTAMP) {
            mLocationTimeStamp = NO_TIMESTAMP;
            mLatitude = mLongitude = mAltitude = 0;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the reachability of return home location.
     *
     * @param reachability the new reachability
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomePilotingItfCore updateHomeReachability(@NonNull Reachability reachability) {
        if (mReachability != reachability) {
            mReachability = reachability;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the delay before planned automatic safety return.
     *
     * @param autoTriggerDelay new delay before of planned automatic safety return in seconds, or
     *                         {@link #NO_DELAY} if there is no planned safety return
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomePilotingItfCore updateAutoTriggerDelay(long autoTriggerDelay) {
        if (mAutoTriggerDelay != autoTriggerDelay) {
            mAutoTriggerDelay = autoTriggerDelay;
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
    public ReturnHomePilotingItfCore cancelSettingsRollbacks() {
        mAutoTriggerSetting.cancelRollback();
        mPreferredTargetSetting.cancelRollback();
        mEndingBehaviorSetting.cancelRollback();
        mAutoStartOnDisconnectDelaySetting.cancelRollback();
        mEndingHoveringAltitudeSetting.cancelRollback();
        mMinAltitudeSetting.cancelRollback();
        return this;
    }

    /**
     * Notified when a user setting changes.
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
