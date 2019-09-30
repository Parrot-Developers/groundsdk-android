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

package com.parrot.drone.groundsdk.internal.device.peripheral.wifi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Implementation class for {@code SecuritySetting}. */
public final class SecuritySettingCore extends WifiAccessPoint.SecuritySetting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets the access point security mode.
         *
         * @param mode     new security mode
         * @param password password used to secure the access point, use {@code null} for
         *                 {@link WifiAccessPoint.SecuritySetting.Mode#OPEN}
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setSecurity(@NonNull Mode mode, @Nullable String password);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Current security mode. */
    @NonNull
    private Mode mMode;

    /** Set of supported modes. */
    @NonNull
    private final EnumSet<Mode> mSupportedModes;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    SecuritySettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mMode = Mode.OPEN;
        mSupportedModes = EnumSet.noneOf(Mode.class);
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public Set<Mode> getSupportedModes() {
        return Collections.unmodifiableSet(mSupportedModes);
    }

    @NonNull
    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public void open() {
        if (mMode != Mode.OPEN && mSupportedModes.contains(Mode.OPEN) && mBackend.setSecurity(Mode.OPEN, null)) {
            Mode rollbackMode = mMode;
            mMode = Mode.OPEN;
            mController.postRollback(() -> mMode = rollbackMode);
        }
    }

    @Override
    public boolean secureWithWPA2(@NonNull String password) {
        if (!isPasswordValid(password)) {
            return false;
        }
        if (mSupportedModes.contains(Mode.WPA2_SECURED) && mBackend.setSecurity(Mode.WPA2_SECURED, password)) {
            Mode rollbackMode = mMode;
            mMode = Mode.WPA2_SECURED;
            mController.postRollback(() -> mMode = rollbackMode);
        }
        return true;
    }

    /**
     * Updates supported modes.
     *
     * @param modes new supported modes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public SecuritySettingCore updateSupportedModes(@NonNull Set<Mode> modes) {
        if (mSupportedModes.retainAll(modes) | mSupportedModes.addAll(modes)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current security mode.
     *
     * @param mode new security mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public SecuritySettingCore updateMode(@NonNull Mode mode) {
        if (mController.cancelRollback() || mMode != mode) {
            mMode = mode;
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
