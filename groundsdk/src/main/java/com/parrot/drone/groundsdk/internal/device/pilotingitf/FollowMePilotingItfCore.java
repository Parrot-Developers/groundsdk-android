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

import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/**
 * Core class for FollowMePilotingItf.
 */
public final class FollowMePilotingItfCore extends TrackingPilotingItfCore implements FollowMePilotingItf {

    /** Description of FollowMePilotingItf. */
    private static final ComponentDescriptor<PilotingItf, FollowMePilotingItf> DESC =
            ComponentDescriptor.of(FollowMePilotingItf.class);

    /** Engine-specific backend for FollowMePilotingItf. */
    public interface Backend extends TrackingPilotingItfCore.Backend {

        /**
         * Sets follow-me mode.
         *
         * @param mode follow-me mode to set
         *
         * @return {@code true} to make the setting update to the requested mode and switch to the updating state now,
         *         otherwise {@code false}
         */
        boolean setMode(@NonNull Mode mode);
    }

    /** Operating mode setting. */
    @NonNull
    private final EnumSettingCore<Mode> mModeSetting;

    /** Current behavior. */
    @NonNull
    private Behavior mBehavior;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs.
     * @param backend          backend used to forward actions to the engine
     */
    public FollowMePilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mModeSetting = new EnumSettingCore<>(Mode.GEOGRAPHIC, new SettingController(this::onSettingChange),
                backend::setMode);
        mBehavior = Behavior.INACTIVE;
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @NonNull
    @Override
    public EnumSettingCore<Mode> mode() {
        return mModeSetting;
    }

    @NonNull
    @Override
    public Behavior getCurrentBehavior() {
        return mBehavior;
    }

    /**
     * Updates follow-me behavior.
     *
     * @param behavior follow me behavior
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FollowMePilotingItfCore updateBehavior(@NonNull Behavior behavior) {
        if (mBehavior != behavior) {
            mBehavior = behavior;
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
    public FollowMePilotingItfCore cancelSettingsRollbacks() {
        mModeSetting.cancelRollback();
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
