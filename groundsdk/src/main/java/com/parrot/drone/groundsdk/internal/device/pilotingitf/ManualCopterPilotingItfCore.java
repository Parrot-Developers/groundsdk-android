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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.OptionalBooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import static com.parrot.drone.groundsdk.internal.value.IntegerRangeCore.SIGNED_PERCENTAGE;

/**
 * Core class for ManualCopterPilotingItf.
 */
public final class ManualCopterPilotingItfCore extends ActivablePilotingItfCore implements ManualCopterPilotingItf {

    /** Description of ManualCopterPilotingItf. */
    private static final ComponentDescriptor<PilotingItf, ManualCopterPilotingItf> DESC =
            ComponentDescriptor.of(ManualCopterPilotingItf.class);

    /** Backend of a ManualCopterPilotingItfCore which handles the messages. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Asks to the copter to take off.
         */
        void takeOff();

        /**
         * Asks to the copter to get prepared for a thrown take-off.
         */
        void thrownTakeOff();

        /**
         * Asks to the drone to land.
         */
        void land();

        /**
         * Asks the copter to cut out the motors.
         */
        void emergencyCutOut();

        /**
         * Updates the max pitch/roll value.
         *
         * @param maxPitchRoll the max pitch/roll value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxPitchRoll(double maxPitchRoll);

        /**
         * Updates the max pitch/roll velocity value.
         *
         * @param maxPitchRollVelocity the max pitch/roll velocity value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxPitchRollVelocity(double maxPitchRollVelocity);

        /**
         * Updates the max vertical speed value.
         *
         * @param maxVerticalSpeed the max vertical speed value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxVerticalSpeed(double maxVerticalSpeed);

        /**
         * Updates the max yaw rotation speed value.
         *
         * @param maxYawRotationSpeed the max yaw rotation speed value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxYawRotationSpeed(double maxYawRotationSpeed);

        /**
         * Enables or disables the banked-turn mode.
         *
         * @param enable {@code true} to enable banked-turn mode, {@code false} to disable it.
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setBankedTurnMode(boolean enable);

        /**
         * Configures whether {@link #smartTakeOffLand()} may trigger a thrown take-off.
         *
         * @param enable {@code true} to allow thrown take-off, {@code false} to disallow thrown take-off
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean useThrownTakeOffForSmartTakeOff(boolean enable);

        /**
         * Sets the piloting command pitch value.
         *
         * @param pitch piloting command pitch
         */
        void setPitch(int pitch);

        /**
         * Sets the piloting command roll value.
         *
         * @param roll piloting command roll
         */
        void setRoll(int roll);

        /**
         * Sets the piloting command yaw rotation speed value.
         *
         * @param yawRotationSpeed piloting command yaw rotation speed
         */
        void setYawRotationSpeed(int yawRotationSpeed);

        /**
         * Sets the piloting command vertical speed value.
         *
         * @param verticalSpeed piloting command vertical speed
         */
        void setVerticalSpeed(int verticalSpeed);

        /**
         * Asks the drone to hover.
         */
        void hover();
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Can perform a take off. */
    private boolean mCanTakeOff;

    /** Can land. */
    private boolean mCanLand;

    /** Max pitch/roll setting. */
    @NonNull
    private final DoubleSettingCore mMaxPitchRollSetting;

    /** Max pitch/roll velocity setting. */
    @NonNull
    private final OptionalDoubleSettingCore mMaxPitchRollVelocitySetting;

    /** Max vertical speed setting. */
    @NonNull
    private final DoubleSettingCore mMaxVerticalSpeedSetting;

    /** Max yaw speed setting. */
    @NonNull
    private final DoubleSettingCore mMaxYawSpeedSetting;

    /** Banked-turn mode setting. */
    @NonNull
    private final OptionalBooleanSettingCore mBankedTurnSetting;

    /** Thrown take-off setting. */
    @NonNull
    private final OptionalBooleanSettingCore mThrownTakeOffSetting;

    /**
     * Action performed by {@link #smartTakeOffLand()}, the next time it will be called.
     */
    @NonNull
    private SmartTakeOffLandAction mSmartTakeOffLandAction;

    /**
     * Indicates if {@link #smartTakeOffLand()} should request or not a thrown take-off,
     * the next time it will request a take-off.
     *
     * @see #updateSmartTakeOffLandAction()
     */
    private boolean mSmartWillThrownTakeOff;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs.
     * @param backend          backend used to forward actions to the engine
     */
    public ManualCopterPilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore,
                                       @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
        mSmartTakeOffLandAction = SmartTakeOffLandAction.NONE;
        mMaxPitchRollSetting = new DoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setMaxPitchRoll);
        mMaxPitchRollVelocitySetting = new OptionalDoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setMaxPitchRollVelocity);
        mMaxVerticalSpeedSetting = new DoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setMaxVerticalSpeed);
        mMaxYawSpeedSetting = new DoubleSettingCore(new SettingController(this::onSettingChange),
                mBackend::setMaxYawRotationSpeed);
        mBankedTurnSetting = new OptionalBooleanSettingCore(new SettingController(this::onSettingChange),
                mBackend::setBankedTurnMode);
        mThrownTakeOffSetting = new OptionalBooleanSettingCore(new SettingController(this::onSettingChange),
                mBackend::useThrownTakeOffForSmartTakeOff);
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

    @Override
    public void setPitch(int value) {
        mBackend.setPitch(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void setRoll(int value) {
        mBackend.setRoll(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void setYawRotationSpeed(int value) {
        mBackend.setYawRotationSpeed(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void setVerticalSpeed(int value) {
        mBackend.setVerticalSpeed(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void hover() {
        mBackend.hover();
    }

    @Override
    public boolean canTakeOff() {
        return mCanTakeOff;
    }

    @Override
    public boolean canLand() {
        return mCanLand;
    }

    @Override
    public void takeOff() {
        mBackend.takeOff();
    }

    @Override
    public void thrownTakeOff() {
        mBackend.thrownTakeOff();
    }

    @Override
    public void smartTakeOffLand() {
        switch (mSmartTakeOffLandAction) {
            case NONE:
                break;
            case TAKE_OFF:
                takeOff();
                break;
            case THROWN_TAKE_OFF:
                thrownTakeOff();
                break;
            case LAND:
                land();
                break;
        }
    }

    @NonNull
    @Override
    public SmartTakeOffLandAction getSmartTakeOffLandAction() {
        return mSmartTakeOffLandAction;
    }

    @Override
    public void land() {
        mBackend.land();
    }

    @Override
    public void emergencyCutOut() {
        mBackend.emergencyCutOut();
    }

    @NonNull
    @Override
    public DoubleSettingCore getMaxPitchRoll() {
        return mMaxPitchRollSetting;
    }

    @NonNull
    @Override
    public OptionalDoubleSettingCore getMaxPitchRollVelocity() {
        return mMaxPitchRollVelocitySetting;
    }

    @NonNull
    @Override
    public DoubleSettingCore getMaxVerticalSpeed() {
        return mMaxVerticalSpeedSetting;
    }

    @NonNull
    @Override
    public DoubleSettingCore getMaxYawRotationSpeed() {
        return mMaxYawSpeedSetting;
    }

    @NonNull
    @Override
    public OptionalBooleanSettingCore getBankedTurnMode() {
        return mBankedTurnSetting;
    }

    @NonNull
    @Override
    public OptionalBooleanSettingCore getThrownTakeOffMode() {
        return mThrownTakeOffSetting;
    }

    /**
     * Updates the ability to land.
     *
     * @param canLand true if the land action can be called
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ManualCopterPilotingItfCore updateCanLand(boolean canLand) {
        if (mCanLand != canLand) {
            mCanLand = canLand;
            updateSmartTakeOffLandAction();
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the ability to take off.
     *
     * @param canTakeOff true if the take off action can be called
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ManualCopterPilotingItfCore updateCanTakeOff(boolean canTakeOff) {
        if (mCanTakeOff != canTakeOff) {
            mCanTakeOff = canTakeOff;
            updateSmartTakeOffLandAction();
            mChanged = true;
        }
        return this;
    }

    /**
     * Tells that {@link #smartTakeOffLand()} should trigger or not a thrown take-off
     * the next time it will request a take-off.
     * <p>
     * Called from the lower layer when the copter has detected if it's moving or not.
     * Also called when usage of thrown take-off is disabled.
     *
     * @param smartWillThrownTakeOff {@code true} if {@link #smartTakeOffLand()} should request
     *                               a thrown take-off, {@code false} if it should request a normal take-off.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ManualCopterPilotingItfCore updateSmartWillDoThrownTakeOff(boolean smartWillThrownTakeOff) {
        if (mSmartWillThrownTakeOff != smartWillThrownTakeOff) {
            mSmartWillThrownTakeOff = smartWillThrownTakeOff;
            mChanged |= updateSmartTakeOffLandAction();
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ManualCopterPilotingItfCore cancelSettingsRollbacks() {
        mMaxPitchRollSetting.cancelRollback();
        mMaxPitchRollVelocitySetting.cancelRollback();
        mMaxVerticalSpeedSetting.cancelRollback();
        mMaxYawSpeedSetting.cancelRollback();
        mBankedTurnSetting.cancelRollback();
        mThrownTakeOffSetting.cancelRollback();
        return this;
    }

    /**
     * Updates the action that will be performed by {@link #smartTakeOffLand()}.
     */
    private boolean updateSmartTakeOffLandAction() {
        SmartTakeOffLandAction newAction;
        if (mCanLand) {
            newAction = SmartTakeOffLandAction.LAND;
        } else if (mCanTakeOff) {
            newAction = mSmartWillThrownTakeOff ?
                    SmartTakeOffLandAction.THROWN_TAKE_OFF : SmartTakeOffLandAction.TAKE_OFF;
        } else {
            newAction = SmartTakeOffLandAction.NONE;
        }
        if (newAction != mSmartTakeOffLandAction) {
            mSmartTakeOffLandAction = newAction;
            return true;
        }
        return false;
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
