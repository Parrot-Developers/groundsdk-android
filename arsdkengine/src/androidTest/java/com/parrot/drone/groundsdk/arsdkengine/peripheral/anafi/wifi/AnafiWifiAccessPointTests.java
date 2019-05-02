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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.wifi;

import android.location.Address;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint.SecuritySetting;
import com.parrot.drone.groundsdk.device.peripheral.WifiScanner;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureWifi;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiWifiAccessPointTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private WifiAccessPoint mWifiAccessPoint;

    private WifiScanner mWifiScanner;

    private int mAccessPointChangeCnt, mScannerChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mWifiAccessPoint = mDrone.getPeripheralStore().get(mMockSession, WifiAccessPoint.class);
        mDrone.getPeripheralStore().registerObserver(WifiAccessPoint.class, () -> {
            mWifiAccessPoint = mDrone.getPeripheralStore().get(mMockSession, WifiAccessPoint.class);
            mAccessPointChangeCnt++;
        });

        mWifiScanner = mDrone.getPeripheralStore().get(mMockSession, WifiScanner.class);
        mDrone.getPeripheralStore().registerObserver(WifiScanner.class, () -> {
            mWifiScanner = mDrone.getPeripheralStore().get(mMockSession, WifiScanner.class);
            mScannerChangeCnt++;
        });

        mAccessPointChangeCnt = mScannerChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mWifiAccessPoint, nullValue());
        assertThat(mWifiScanner, nullValue());
        assertThat(mAccessPointChangeCnt, is(0));
        assertThat(mScannerChangeCnt, is(0));

        connectDrone(mDrone, 1);

        assertThat(mWifiAccessPoint, notNullValue());
        assertThat(mAccessPointChangeCnt, is(1));
        assertThat(mWifiScanner, notNullValue());
        assertThat(mScannerChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mWifiAccessPoint, nullValue());
        assertThat(mWifiScanner, nullValue());
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mScannerChangeCnt, is(2));
    }

    @Test
    public void testEnvironment() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // for drones, environment should be mutable so both INDOOR and OUTDOOR modes should be available
        assertThat(mWifiAccessPoint.environment(),
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)));

        // mock some available channels from low-level...
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.INDOOR,
                        ArsdkFeatureWifi.Environment.OUTDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.INDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 3,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.OUTDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // this should not trigger a change since the environment is unknown yet
        assertThat(mAccessPointChangeCnt, is(1));

        // mock initial environment from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(
                ArsdkFeatureWifi.Environment.INDOOR));

        // this should trigger a change, and available channels should come from the indoor list
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.environment(), enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(Channel.BAND_2_4_CHANNEL_1,
                Channel.BAND_2_4_CHANNEL_2));

        // user changes environment
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiSetEnvironment(ArsdkFeatureWifi.Environment.OUTDOOR), true));
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.OUTDOOR);
        assertThat(mAccessPointChangeCnt, is(3));

        // mock update from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(
                ArsdkFeatureWifi.Environment.OUTDOOR));

        // this should trigger a change, and available channels should come from the outdoor list
        assertThat(mAccessPointChangeCnt, is(4));
        assertThat(mWifiAccessPoint.environment(), enumSettingIsUpToDateAt(WifiAccessPoint.Environment.OUTDOOR));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(Channel.BAND_2_4_CHANNEL_1,
                Channel.BAND_2_4_CHANNEL_3));
    }

    @Test
    public void testCountry() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // there should be no available countries by default and the current country should be unknown
        assertThat(mAccessPointChangeCnt, is(1));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), empty());
        assertThat(mWifiAccessPoint.country().getCode(), is(""));
        assertThat(mWifiAccessPoint.country().isDefaultCountryUsed(), is(false));

        // mock available countries from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSupportedCountries("FR;US;DE;ES"));

        // this should trigger a change with the appropriate country list
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("FR", "US", "DE", "ES"));
        assertThat(mWifiAccessPoint.country().getCode(), is(""));
        assertThat(mWifiAccessPoint.country().isDefaultCountryUsed(), is(false));

        // mock country from low-level (should trigger an available channel request)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiUpdateAuthorizedChannels()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiCountryChanged(
                ArsdkFeatureWifi.CountrySelection.MANUAL, "FR"));

        // this should trigger a change with the appropriate current country
        assertThat(mAccessPointChangeCnt, is(3));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("FR", "US", "DE", "ES"));
        assertThat(mWifiAccessPoint.country().getCode(), is("FR"));
        assertThat(mWifiAccessPoint.country().isDefaultCountryUsed(), is(false));

        // user changes country
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetCountry(
                ArsdkFeatureWifi.CountrySelection.MANUAL, "DE"), true));
        mWifiAccessPoint.country().select("DE");
        assertThat(mAccessPointChangeCnt, is(4));

        // mock country update with auto selection from low-level (should trigger an available channel request)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiUpdateAuthorizedChannels()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiCountryChanged(
                ArsdkFeatureWifi.CountrySelection.AUTO, "US"));

        // this should trigger a change with the appropriate current country, and default country set to true
        assertThat(mAccessPointChangeCnt, is(5));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("FR", "US", "DE", "ES"));
        assertThat(mWifiAccessPoint.country().getCode(), is("US"));
        assertThat(mWifiAccessPoint.country().isDefaultCountryUsed(), is(true));

        // mock one available country (lock) from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSupportedCountries("US"));

        // default country should switch to false
        assertThat(mAccessPointChangeCnt, is(6));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("US"));
        assertThat(mWifiAccessPoint.country().getCode(), is("US"));
        assertThat(mWifiAccessPoint.country().isDefaultCountryUsed(), is(false));
    }

    @Test
    public void testAutoSelectCountry() {
        GroundSdkConfig.get().autoSelectWifiCountry(true);

        // mock country detected before connecting
        Address address = new Address(null);
        address.setCountryCode("FR");
        mReverseGeocoderUtility.updateAddress(address);

        // check that detected country and default environment are sent to the drone on connect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(ArsdkFeatureWifi.Environment.INDOOR))
                .expect(new Expectation.Command(1, ExpectedCmd.wifiUpdateAuthorizedChannels()))
                .commandReceived(1, ArsdkEncoder.encodeWifiCountryChanged(
                        ArsdkFeatureWifi.CountrySelection.MANUAL, "US"))
                .expect(new Expectation.Command(1, ExpectedCmd.wifiSetCountry(
                        ArsdkFeatureWifi.CountrySelection.MANUAL, "FR"), true))
                .expect(new Expectation.Command(1, ExpectedCmd.wifiSetEnvironment(
                        ArsdkFeatureWifi.Environment.OUTDOOR), true))
        );

        // available countries and environment have changed
        assertThat(mAccessPointChangeCnt, is(1));
        assertThat(mWifiAccessPoint.country().getCode(), is("US"));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("US"));
        assertThat(mWifiAccessPoint.environment().getAvailableValues(),
                containsInAnyOrder(WifiAccessPoint.Environment.OUTDOOR));

        // mock country from low-level (should trigger an available channel request)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiUpdateAuthorizedChannels()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiCountryChanged(
                ArsdkFeatureWifi.CountrySelection.MANUAL, "FR"));

        // this should trigger a change with the appropriate current country
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.country().getCode(), is("FR"));
        assertThat(mWifiAccessPoint.country().getAvailableCodes(), containsInAnyOrder("FR"));

        // mock new address decoded without country code, this should not trigger any change
        address = new Address(null);
        mReverseGeocoderUtility.updateAddress(address);

        // mock new address decoded with the current country code, this should not trigger any change
        address = new Address(null);
        address.setCountryCode("FR");
        mReverseGeocoderUtility.updateAddress(address);

        // mock new address decoded with new country code, this should trigger a country command
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetCountry(
                ArsdkFeatureWifi.CountrySelection.MANUAL, "DE"), true));

        address = new Address(null);
        address.setCountryCode("DE");
        mReverseGeocoderUtility.updateAddress(address);
    }

    @Test
    public void testChannel() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // mock some available channels from low-level...
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.values()),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.values()),
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 34,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.values()),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // then mock environment from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(
                ArsdkFeatureWifi.Environment.INDOOR));

        // this should trigger a change
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.environment(), enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), contains(Channel.BAND_2_4_CHANNEL_1,
                Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34));

        // user changes channel
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetApChannel(
                ArsdkFeatureWifi.SelectionType.MANUAL, ArsdkFeatureWifi.Band.E5_GHZ, 34), true));
        mWifiAccessPoint.channel().select(Channel.BAND_5_CHANNEL_34);
        assertThat(mAccessPointChangeCnt, is(3));

        // mock response from low level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiApChannelChanged(ArsdkFeatureWifi.SelectionType.MANUAL,
                ArsdkFeatureWifi.Band.E2_4_GHZ, 1));
        assertThat(mAccessPointChangeCnt, is(4));
        assertThat(mWifiAccessPoint.channel().get(), is(Channel.BAND_2_4_CHANNEL_1));
        assertThat(mWifiAccessPoint.channel().getSelectionMode(),
                is(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL));

        // user auto-selects channel on any band
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetApChannel
                (ArsdkFeatureWifi.SelectionType.AUTO_ALL, ArsdkFeatureWifi.Band.E2_4_GHZ, 0), true));
        mWifiAccessPoint.channel().autoSelect();
        assertThat(mAccessPointChangeCnt, is(5));

        // mock response from low level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiApChannelChanged(
                ArsdkFeatureWifi.SelectionType.AUTO_ALL,
                ArsdkFeatureWifi.Band.E2_4_GHZ, 2));
        assertThat(mAccessPointChangeCnt, is(6));
        assertThat(mWifiAccessPoint.channel().get(), is(Channel.BAND_2_4_CHANNEL_2));
        assertThat(mWifiAccessPoint.channel().getSelectionMode(),
                is(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND));

        // user auto-selects channel on 2.4 Ghz band
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetApChannel
                (ArsdkFeatureWifi.SelectionType.AUTO_2_4_GHZ, ArsdkFeatureWifi.Band.E2_4_GHZ, 0), true));
        mWifiAccessPoint.channel().autoSelect(Band.B_2_4_GHZ);
        assertThat(mAccessPointChangeCnt, is(7));

        // mock response from low level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiApChannelChanged(
                ArsdkFeatureWifi.SelectionType.AUTO_2_4_GHZ,
                ArsdkFeatureWifi.Band.E2_4_GHZ, 1));
        assertThat(mAccessPointChangeCnt, is(8));
        assertThat(mWifiAccessPoint.channel().get(), is(Channel.BAND_2_4_CHANNEL_1));
        assertThat(mWifiAccessPoint.channel().getSelectionMode(),
                is(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND));

        // user auto-selects channel on 5 Ghz band
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetApChannel
                (ArsdkFeatureWifi.SelectionType.AUTO_5_GHZ, ArsdkFeatureWifi.Band.E2_4_GHZ, 0), true));
        mWifiAccessPoint.channel().autoSelect(Band.B_5_GHZ);
        assertThat(mAccessPointChangeCnt, is(9));

        // mock response from low level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiApChannelChanged(
                ArsdkFeatureWifi.SelectionType.AUTO_5_GHZ,
                ArsdkFeatureWifi.Band.E5_GHZ, 34));
        assertThat(mAccessPointChangeCnt, is(10));
        assertThat(mWifiAccessPoint.channel().get(), is(Channel.BAND_5_CHANNEL_34));
        assertThat(mWifiAccessPoint.channel().getSelectionMode(),
                is(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND));
    }

    @Test
    public void testAvailableChannels() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // receive environment from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(
                ArsdkFeatureWifi.Environment.INDOOR));

        // this should trigger a change
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.environment(), enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR));

        // receive first channel
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.values()),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // ensure no update yet
        assertThat(mAccessPointChangeCnt, is(2));

        // receive other channels
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.OUTDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 34,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.values()),
                ArsdkFeatureGeneric.ListFlags.toBitField()));

        // ensure no update yet
        assertThat(mAccessPointChangeCnt, is(2));

        // receive last channel
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 36,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.INDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // this should trigger a change, channels should come from indoor list
        assertThat(mAccessPointChangeCnt, is(3));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(
                Channel.BAND_2_4_CHANNEL_1, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36));


        // receive a new available channel
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 38,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.INDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // this should trigger a change, channels should update
        assertThat(mAccessPointChangeCnt, is(4));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(
                Channel.BAND_2_4_CHANNEL_1, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36,
                Channel.BAND_5_CHANNEL_38));

        // receive a channel removal
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 38,
                ArsdkFeatureWifi.Environment.toBitField(ArsdkFeatureWifi.Environment.INDOOR),
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.REMOVE,
                        ArsdkFeatureGeneric.ListFlags.LAST)));

        // this should trigger a change, channel should go away
        assertThat(mAccessPointChangeCnt, is(5));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(
                Channel.BAND_2_4_CHANNEL_1, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36));

        // receive an environment change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiEnvironmentChanged(
                ArsdkFeatureWifi.Environment.OUTDOOR));

        // this should trigger a change, channels should come from the outdoor list now
        assertThat(mAccessPointChangeCnt, is(6));
        assertThat(mWifiAccessPoint.environment(), enumSettingIsUpToDateAt(WifiAccessPoint.Environment.OUTDOOR));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), containsInAnyOrder(
                Channel.BAND_2_4_CHANNEL_1, Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34));

        // receive a list clear
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiAuthorizedChannel(ArsdkFeatureWifi.Band.E5_GHZ, 0, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));

        // this should trigger a change, channels should contain only the current channel (groundsdk injects it)
        assertThat(mAccessPointChangeCnt, is(7));
        assertThat(mWifiAccessPoint.channel().getAvailableChannels(), contains(mWifiAccessPoint.channel().get()));
    }

    @Test
    public void testSsid() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // mock initial ssid from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductNameChanged("ssid"));
        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.ssid().getValue(), is("ssid"));

        // user changes ssid
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsProductName("ssid-user")));
        mWifiAccessPoint.ssid().setValue("ssid-user");
    }

    @Test
    public void testSecurity() {
        connectDrone(mDrone, 1);

        assertThat(mAccessPointChangeCnt, is(1));

        // mock supported modes
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSupportedSecurityTypes(
                ArsdkFeatureWifi.SecurityType.toBitField(ArsdkFeatureWifi.SecurityType.OPEN,
                        ArsdkFeatureWifi.SecurityType.WPA2)));

        assertThat(mAccessPointChangeCnt, is(2));
        assertThat(mWifiAccessPoint.security().getSupportedModes(),
                containsInAnyOrder(SecuritySetting.Mode.OPEN, SecuritySetting.Mode.WPA2_SECURED));

        // user changes security
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetSecurity(ArsdkFeatureWifi.SecurityType.WPA2,
                "password", ArsdkFeatureWifi.SecurityKeyType.PLAIN), true));

        assertThat(mWifiAccessPoint.security().secureWithWPA2("password"), is(true));
        assertThat(mAccessPointChangeCnt, is(3));

        // mock response from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSecurityChanged(ArsdkFeatureWifi.SecurityType.WPA2,
                "password", ArsdkFeatureWifi.SecurityKeyType.PLAIN));

        assertThat(mAccessPointChangeCnt, is(4));
        assertThat(mWifiAccessPoint.security().getMode(), is(SecuritySetting.Mode.WPA2_SECURED));

        // user disables security
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.wifiSetSecurity(ArsdkFeatureWifi.SecurityType.OPEN,
                "", ArsdkFeatureWifi.SecurityKeyType.PLAIN), true));

        mWifiAccessPoint.security().open();
        assertThat(mAccessPointChangeCnt, is(5));

        // mock response from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSecurityChanged(ArsdkFeatureWifi.SecurityType.OPEN,
                "", ArsdkFeatureWifi.SecurityKeyType.PLAIN));

        assertThat(mAccessPointChangeCnt, is(6));
        assertThat(mWifiAccessPoint.security().getMode(), is(SecuritySetting.Mode.OPEN));

        // mock supporting only WPA2
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSupportedSecurityTypes(
                ArsdkFeatureWifi.SecurityType.toBitField(ArsdkFeatureWifi.SecurityType.WPA2)));

        assertThat(mAccessPointChangeCnt, is(7));
        assertThat(mWifiAccessPoint.security().getSupportedModes(),
                containsInAnyOrder(SecuritySetting.Mode.WPA2_SECURED));

        // mock security enabled
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiSecurityChanged(ArsdkFeatureWifi.SecurityType.WPA2,
                "", ArsdkFeatureWifi.SecurityKeyType.PLAIN));

        assertThat(mAccessPointChangeCnt, is(8));
        assertThat(mWifiAccessPoint.security().getMode(), is(SecuritySetting.Mode.WPA2_SECURED));

        // mock user trying to disable security, but it's not supported
        mWifiAccessPoint.security().open();

        assertThat(mAccessPointChangeCnt, is(8));
        assertThat(mWifiAccessPoint.security().getMode(), is(SecuritySetting.Mode.WPA2_SECURED));
    }

    @Test
    public void testScan() {
        connectDrone(mDrone, 1);

        assertThat(mScannerChangeCnt, is(1));
        assertThat(mWifiScanner.isScanning(), is(false));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // user starts scan. This should trigger a scan request
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiScan(ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())), true));
        mWifiScanner.startScan();

        assertThat(mScannerChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // start scan while already scanning
        mWifiScanner.startScan();

        // ensure this is a no-op
        assertThat(mScannerChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock first scan result
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("A", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // user stop scan while we are receiving results
        mWifiScanner.stopScan();

        // ensure that change occurs and all is cleared
        assertThat(mWifiScanner.isScanning(), is(false));
        assertThat(mScannerChangeCnt, is(3));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock another scan result
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("B", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureGeneric.ListFlags.toBitField()));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(3));
        assertThat(mWifiScanner.isScanning(), is(false));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // user changes his mind and starts scanning again. This should trigger a scan request
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiScan(ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())), true));
        mWifiScanner.startScan();

        // ensure the scanner changes state, rates should be cleared
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // receive another result from the previous scan
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("C", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureGeneric.ListFlags.toBitField()));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // last result from the previous scan, should be ignored, but another scan should be requested.
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiScan(ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())), true));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("D", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock first scan result from second batch
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("A", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock other scan results
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("B", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("C", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField()));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock a removal (which should not happen in real life, but let's support it)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("D", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.REMOVE)));

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // ensure this does not trigger a change
        assertThat(mScannerChangeCnt, is(4));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // mock last result, this should trigger another scan since the user did not stop scan yet
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiScan(ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())), true));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("E", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 2,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // assert that a changed occur and the occupation rates match the scan results
        assertThat(mScannerChangeCnt, is(5));
        assertThat(mWifiScanner.isScanning(), is(true));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_1), is(2));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_2), is(1));
        for (Channel channel : EnumSet.complementOf(
                EnumSet.of(Channel.BAND_2_4_CHANNEL_1, Channel.BAND_2_4_CHANNEL_2))) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // scanning continues
        // mock empty scan results, this should trigger another scan since the user did not stop scan yet
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.wifiScan(ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())), true));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiScannedItem("A", 0, ArsdkFeatureWifi.Band.E2_4_GHZ, 1,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));

        // assert that change occurs and all is cleared
        assertThat(mScannerChangeCnt, is(6));
        assertThat(mWifiScanner.isScanning(), is(true));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // user stops scan
        mWifiScanner.stopScan();

        // ensure that change occurs and all is cleared
        assertThat(mWifiScanner.isScanning(), is(false));
        assertThat(mScannerChangeCnt, is(7));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        // user stops scan again.
        mWifiScanner.stopScan();

        // ensure nothing changes
        assertThat(mWifiScanner.isScanning(), is(false));
        assertThat(mScannerChangeCnt, is(7));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

    }
}
