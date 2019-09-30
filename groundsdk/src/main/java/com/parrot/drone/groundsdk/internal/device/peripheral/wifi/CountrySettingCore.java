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

import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Implementation class for {@code CountrySetting}. */
public final class CountrySettingCore extends WifiAccessPoint.CountrySetting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets the access point country.
         *
         * @param code new country code value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setCountry(@NonNull String code);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** {@code true} if the current country is the default one and can be modified, otherwise {@code false}. */
    private boolean mDefaultCountryUsed;

    /** Currently selected country code. */
    @NonNull
    private String mCode;

    /** Available country codes. */
    @NonNull
    private final Set<String> mAvailableCodes;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CountrySettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mCode = "";
        mAvailableCodes = new HashSet<>();
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public boolean isDefaultCountryUsed() {
        return mDefaultCountryUsed;
    }

    @NonNull
    @Override
    public Set<String> getAvailableCodes() {
        return Collections.unmodifiableSet(mAvailableCodes);
    }

    @NonNull
    @Override
    public String getCode() {
        return mCode;
    }

    @Override
    public void select(@NonNull String code) {
        if (!mCode.equals(code) && mAvailableCodes.contains(code) && mBackend.setCountry(code)) {
            String rollbackCode = mCode;
            mCode = code;
            mController.postRollback(() -> mCode = rollbackCode);
        }
    }

    /**
     * Updates default country used flag.
     *
     * @param defaultCountryUsed new default country used value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CountrySettingCore updateDefaultCountryUsed(boolean defaultCountryUsed) {
        if (mDefaultCountryUsed != defaultCountryUsed) {
            mDefaultCountryUsed = defaultCountryUsed;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current country code.
     *
     * @param code new country code
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CountrySettingCore updateCode(@NonNull String code) {
        if (mController.cancelRollback() || !mCode.equals(code)) {
            mCode = code;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates available country codes for access point setup.
     *
     * @param codes new set of available country codes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CountrySettingCore updateAvailableCodes(@NonNull Set<String> codes) {
        if (mAvailableCodes.retainAll(codes) | mAvailableCodes.addAll(codes)) {
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
