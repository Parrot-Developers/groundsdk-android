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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.flightplan;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/**
 * Implementation class for {@code ReturnHomeOnDisconnectSetting}.
 */
public final class ReturnHomeOnDisconnectSettingCore extends FlightPlanPilotingItf.ReturnHomeOnDisconnectSetting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets return home on disconnect behavior.
         *
         * @param enable {@code true} to enable return home on disconnect, {@code false} to disable it.
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setReturnHomeOnDisconnect(boolean enable);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** {@code true} when the user can mutate the setting, otherwise {@code false} (setting is locked). */
    private boolean mMutable;

    /** {@code true} when the drone is setup tro return home upon disconnection, otherwise {@code false}. */
    private boolean mEnabled;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    ReturnHomeOnDisconnectSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public boolean isMutable() {
        return mMutable;
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mMutable && mEnabled != enabled && mBackend.setReturnHomeOnDisconnect(enabled)) {
            mEnabled = enabled;
            mController.postRollback(() -> mEnabled = !enabled);
        }
    }

    @Override
    public void toggle() {
        setEnabled(!mEnabled);
    }

    /**
     * Updates the current setting enabled flag.
     * <p>
     * Resets the updating flag in case it was set. <br>
     * Called from lower layer when the backend sends a new value for the setting.
     *
     * @param enabled {@code true} if return home on disconnect is enabled, otherwise {@code false}
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomeOnDisconnectSettingCore updateEnabledFlag(boolean enabled) {
        if (mController.cancelRollback() || mEnabled != enabled) {
            mEnabled = enabled;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates the current setting mutable flag.
     *
     * @param mutable {@code true} if the user can mutate the setting, otherwise {@code false}
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ReturnHomeOnDisconnectSettingCore updateMutableFlag(boolean mutable) {
        if (mMutable != mutable) {
            mMutable = mutable;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Cancels any pending rollback.
     */
    void cancelRollback() {
        if (mController.cancelRollback()) {
            mController.notifyChange(false);
        }
    }
}
