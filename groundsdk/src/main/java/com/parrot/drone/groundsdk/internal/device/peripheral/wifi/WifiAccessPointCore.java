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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;
import com.parrot.drone.groundsdk.internal.value.StringSettingCore;

/** Core class for WifiAccessPoint. */
public final class WifiAccessPointCore extends SingletonComponentCore implements WifiAccessPoint {

    /** Description of WifiAccessPoint. */
    private static final ComponentDescriptor<Peripheral, WifiAccessPoint> DESC =
            ComponentDescriptor.of(WifiAccessPoint.class);

    /** Engine-specific backend for WifiAccessPoint. */
    public interface Backend extends ChannelSettingCore.Backend {

        /**
         * Sets the access point environment.
         *
         * @param environment new environment value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setEnvironment(@NonNull Environment environment);

        /**
         * Sets the access point country.
         *
         * @param code new country code value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setCountry(@NonNull String code);

        /**
         * Sets the access point SSID.
         *
         * @param ssid new SSID value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setSsid(@NonNull String ssid);

        /**
         * Sets the access point security mode.
         *
         * @param mode     new security mode
         * @param password password used to secure the access point, use {@code null} for
         *                 {@link SecuritySetting.Mode#OPEN}
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setSecurity(@NonNull SecuritySetting.Mode mode, @Nullable String password);
    }

    /** Access point country setting. */
    private final CountrySettingCore mCountry;

    /** Access point environment setting. */
    @NonNull
    private final EnumSettingCore<Environment> mEnvironment;

    /** Access point channel setting. */
    @NonNull
    private final ChannelSettingCore mChannel;

    /** Access point SSID setting. */
    @NonNull
    private final StringSettingCore mSsid;

    /** Access point security setting. */
    @NonNull
    private final SecuritySettingCore mSecurity;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public WifiAccessPointCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mEnvironment = new EnumSettingCore<>(Environment.OUTDOOR, new SettingController(this::onSettingChange),
                backend::setEnvironment);
        mChannel = new ChannelSettingCore(this::onSettingChange, backend);
        mSecurity = new SecuritySettingCore(this::onSettingChange, backend::setSecurity);
        mSsid = new StringSettingCore(new SettingController(this::onSettingChange), backend::setSsid);
        mCountry = new CountrySettingCore(this::onSettingChange, backend::setCountry);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @NonNull
    @Override
    public EnumSettingCore<Environment> environment() {
        return mEnvironment;
    }

    @NonNull
    @Override
    public CountrySettingCore country() {
        return mCountry;
    }

    @NonNull
    @Override
    public ChannelSettingCore channel() {
        return mChannel;
    }

    @NonNull
    @Override
    public StringSettingCore ssid() {
        return mSsid;
    }

    @NonNull
    @Override
    public SecuritySettingCore security() {
        return mSecurity;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public WifiAccessPointCore cancelSettingsRollbacks() {
        mEnvironment.cancelRollback();
        mChannel.cancelRollback();
        mSecurity.cancelRollback();
        mSsid.cancelRollback();
        mCountry.cancelRollback();
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
