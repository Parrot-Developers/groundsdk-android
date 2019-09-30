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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint.SecuritySetting;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.wifi.WifiAccessPointCore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.ChannelSettingMatcher.channelSettingAvailableChannelsAre;
import static com.parrot.drone.groundsdk.ChannelSettingMatcher.channelSettingCanAutoSelect;
import static com.parrot.drone.groundsdk.ChannelSettingMatcher.channelSettingChannelIs;
import static com.parrot.drone.groundsdk.ChannelSettingMatcher.channelSettingSelectionModeIs;
import static com.parrot.drone.groundsdk.CountrySettingMatcher.countrySettingCodeIs;
import static com.parrot.drone.groundsdk.CountrySettingMatcher.countrySettingSupportsCodes;
import static com.parrot.drone.groundsdk.CountrySettingMatcher.countrySettingUsesDefault;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SecuritySettingMatcher.securitySettingModeIs;
import static com.parrot.drone.groundsdk.SecuritySettingMatcher.securitySettingSupportsModes;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingValueIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class WifiAccessPointTest {

    private MockComponentStore<Peripheral> mStore;

    private WifiAccessPointCore mWifiAccessPointCore;

    private WifiAccessPoint mWifiAccessPoint;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mWifiAccessPointCore = new WifiAccessPointCore(mStore, mBackend);
        mWifiAccessPoint = mStore.get(WifiAccessPoint.class);
        mStore.registerObserver(WifiAccessPoint.class, () -> {
            mComponentChangeCnt++;
            mWifiAccessPoint = mStore.get(WifiAccessPoint.class);
        });
        mComponentChangeCnt = 0;
        mBackend.reset();
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mWifiAccessPoint, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mWifiAccessPointCore.publish();
        assertThat(mWifiAccessPoint, is(mWifiAccessPointCore));
        assertThat(mComponentChangeCnt, is(1));

        mWifiAccessPointCore.unpublish();
        assertThat(mWifiAccessPoint, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testEnvironment() {
        mWifiAccessPointCore.publish();

        // test default value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.OUTDOOR)));

        // change supported values to make it immutable
        mWifiAccessPointCore.environment().updateAvailableValues(EnumSet.of(WifiAccessPoint.Environment.OUTDOOR));

        // user may not change setting until notified mutable from low-level
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.OUTDOOR);

        assertThat(mBackend.mEnvironment, nullValue());
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.of(WifiAccessPoint.Environment.OUTDOOR)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.OUTDOOR)));

        // mock update from low-level
        mWifiAccessPointCore.environment().updateAvailableValues(EnumSet.allOf(WifiAccessPoint.Environment.class))
                            .updateValue(WifiAccessPoint.Environment.INDOOR);
        mWifiAccessPointCore.notifyUpdated();

        // setting should be available and up to date now
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR)));

        // test nothing changes if low-level denies
        mBackend.mAccept = false;
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.OUTDOOR);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mEnvironment, is(WifiAccessPoint.Environment.OUTDOOR));
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR)));

        // mock low-level accepting changes again
        mBackend.reset();

        // changing to the current value should not trigger any change
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.INDOOR);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mEnvironment, nullValue());
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.INDOOR)));

        // user changes to a different value
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.OUTDOOR);

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mEnvironment, is(WifiAccessPoint.Environment.OUTDOOR));
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpdatingTo(WifiAccessPoint.Environment.OUTDOOR)));

        // mock update from low-level
        mWifiAccessPointCore.environment().updateValue(WifiAccessPoint.Environment.OUTDOOR);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingSupports(EnumSet.allOf(WifiAccessPoint.Environment.class)),
                enumSettingIsUpToDateAt(WifiAccessPoint.Environment.OUTDOOR)));
    }

    @Test
    public void testCountry() {
        // add some available countries we can work with
        mWifiAccessPointCore.country().updateAvailableCodes(new HashSet<>(Arrays.asList("FR", "DE", "ES")));

        mWifiAccessPointCore.publish();

        // test default value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs(""),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // mock update from low-level
        mWifiAccessPointCore.country().updateCode("FR");
        mWifiAccessPointCore.notifyUpdated();

        // setting should be available and up to date now
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("FR"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        mBackend.mAccept = false;
        mWifiAccessPoint.country().select("DE");

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mCountryCode, is("DE"));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("FR"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // mock low-level accepting changes again
        mBackend.reset();

        // changing to the current value should not trigger any change
        mWifiAccessPoint.country().select("FR");

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mCountryCode, nullValue());
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("FR"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // changing to a not available country should not trigger any change
        mWifiAccessPoint.country().select("US");

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mCountryCode, nullValue());
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("FR"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // user changes to a different value
        mWifiAccessPoint.country().select("ES");

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mCountryCode, is("ES"));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("ES"),
                countrySettingUsesDefault(false),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.country().updateCode("ES");
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("FR", "DE", "ES"),
                countrySettingCodeIs("ES"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // mock an available country update
        mWifiAccessPointCore.country().updateAvailableCodes(new HashSet<>(Arrays.asList("US", "DE", "GB", "ES")));
        mWifiAccessPointCore.notifyUpdated();

        // check that the list changes.
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("US", "DE", "GB", "ES"),
                countrySettingCodeIs("ES"),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // mock update default country used
        mWifiAccessPointCore.country().updateDefaultCountryUsed(true);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("US", "DE", "GB", "ES"),
                countrySettingCodeIs("ES"),
                countrySettingUsesDefault(true),
                settingIsUpToDate()));

        // changing to the current value should not trigger any change
        mWifiAccessPointCore.country().updateDefaultCountryUsed(true);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingSupportsCodes("US", "DE", "GB", "ES"),
                countrySettingCodeIs("ES"),
                countrySettingUsesDefault(true),
                settingIsUpToDate()));
    }

    @Test
    public void testCountrySettingTimeouts() {
        mWifiAccessPointCore.country().updateAvailableCodes(new HashSet<>(Arrays.asList("FR", "DE", "ES")));
        mWifiAccessPointCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs(""),
                countrySettingUsesDefault(false),
                settingIsUpToDate()));

        // mock user sets value
        mWifiAccessPoint.country().select("FR");

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("FR"),
                settingIsUpdating()));

        // mock backend updates value
        mWifiAccessPointCore.country().updateCode("FR");
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("FR"),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("FR"),
                settingIsUpToDate()));

        // mock user sets value
        mWifiAccessPoint.country().select("DE");

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("DE"),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("FR"),
                settingIsUpToDate()));
    }

    @Test
    public void testChannel() {
        // add some available channels we can work with
        mWifiAccessPointCore.channel().updateAvailableChannels(EnumSet.of(Channel.BAND_2_4_CHANNEL_1,
                Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36));

        mWifiAccessPointCore.publish();

        // test default value
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // test nothing changes if low-level denies manual selection
        mBackend.mAccept = false;
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_2);

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mChannel, is(Channel.BAND_2_4_CHANNEL_2));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));


        // test nothing changes if low-level denies auto selection (with 2.4Ghz band restriction)
        mWifiAccessPoint.channel().autoSelect(Band.B_2_4_GHZ);

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mAutoBand, is(Band.B_2_4_GHZ));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // test nothing changes if low-level denies auto selection (with 5Ghz band restriction)
        mWifiAccessPoint.channel().autoSelect(Band.B_5_GHZ);

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mAutoBand, is(Band.B_5_GHZ));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // test nothing changes if low-level denies auto selection (without band restriction)
        mWifiAccessPoint.channel().autoSelect();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mAutoBand, nullValue());
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // mock low-level accepting changes again
        mBackend.reset();

        // selecting the same channel when already in manual mode should not trigger a change
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_1);

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mChannel, is(nullValue()));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // selecting an unavailable channel should not trigger a change
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_3);

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mChannel, is(nullValue()));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // user selects an available, different channel
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_2);

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mChannel, is(Channel.BAND_2_4_CHANNEL_2));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL,
                Channel.BAND_5_CHANNEL_34);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // user requests auto-selection on 2.4 Ghz Band
        mWifiAccessPoint.channel().autoSelect(Band.B_2_4_GHZ);

        // setting should report change and be updating to the user selected mode. Channel should not change
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mAutoBand, is(Band.B_2_4_GHZ));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND,
                Channel.BAND_2_4_CHANNEL_1);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // user requests auto-selection on 5 Ghz Band
        mWifiAccessPoint.channel().autoSelect(Band.B_5_GHZ);

        // setting should report change and be updating to the user selected mode. Channel should not change
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mBackend.mAutoBand, is(Band.B_5_GHZ));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND,
                Channel.BAND_5_CHANNEL_36);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_36),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // user requests auto-selection on any band
        mWifiAccessPoint.channel().autoSelect();

        // setting should report change and be updating to the user selected mode. Channel should not change
        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mAutoBand, nullValue());
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_36),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND,
                Channel.BAND_5_CHANNEL_34);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // test that user can still switch back to manual mode with the current channel
        mWifiAccessPoint.channel().select(Channel.BAND_5_CHANNEL_34);

        // setting should report change and be updating to the user selected mode. Channel should not change
        assertThat(mComponentChangeCnt, is(10));
        assertThat(mBackend.mChannel, is(Channel.BAND_5_CHANNEL_34));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL,
                Channel.BAND_5_CHANNEL_34);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_1,
                        Channel.BAND_2_4_CHANNEL_2, Channel.BAND_5_CHANNEL_34, Channel.BAND_5_CHANNEL_36),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // mock an available channel update
        mWifiAccessPointCore.channel().updateAvailableChannels(EnumSet.of(Channel.BAND_2_4_CHANNEL_3,
                Channel.BAND_5_CHANNEL_38));
        mWifiAccessPointCore.notifyUpdated();

        // check that the list changes. Current channel should also be in the list
        assertThat(mComponentChangeCnt, is(12));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_3,
                        Channel.BAND_5_CHANNEL_38, Channel.BAND_5_CHANNEL_34),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // mock no auto-selection support
        mWifiAccessPointCore.channel().updateAutoSelectSupportFlag(false);
        mWifiAccessPointCore.notifyUpdated();

        // no auto-selection mode should be available
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_3,
                        Channel.BAND_5_CHANNEL_38, Channel.BAND_5_CHANNEL_34),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(false),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, false),
                channelSettingCanAutoSelect(Band.B_5_GHZ, false),
                settingIsUpToDate()));

        // re-enable auto-selection and mock an available channel update with only 2.4 Ghz band
        mWifiAccessPointCore.channel().updateAutoSelectSupportFlag(true).updateAvailableChannels(EnumSet.of(
                Channel.BAND_2_4_CHANNEL_5));
        mWifiAccessPointCore.notifyUpdated();

        // auto-selection should be denied on 2.4 Ghz band now
        assertThat(mComponentChangeCnt, is(14));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_2_4_CHANNEL_5, Channel.BAND_5_CHANNEL_34),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, true),
                channelSettingCanAutoSelect(Band.B_5_GHZ, false),
                settingIsUpToDate()));

        // mock an available channel update with only 5 Ghz band
        mWifiAccessPointCore.channel().updateAvailableChannels(EnumSet.of(Channel.BAND_5_CHANNEL_42));
        mWifiAccessPointCore.notifyUpdated();

        // auto-selection should be denied on 5 Ghz band now
        assertThat(mComponentChangeCnt, is(15));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_5_CHANNEL_42, Channel.BAND_5_CHANNEL_34),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(true),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, false),
                channelSettingCanAutoSelect(Band.B_5_GHZ, true),
                settingIsUpToDate()));

        // mock no channels available
        mWifiAccessPointCore.channel().updateAvailableChannels(EnumSet.noneOf(Channel.class));
        mWifiAccessPointCore.notifyUpdated();

        // auto-selection should be denied on 5 Ghz band now
        assertThat(mComponentChangeCnt, is(16));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingAvailableChannelsAre(Channel.BAND_5_CHANNEL_34),
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                channelSettingCanAutoSelect(false),
                channelSettingCanAutoSelect(Band.B_2_4_GHZ, false),
                channelSettingCanAutoSelect(Band.B_5_GHZ, false),
                settingIsUpToDate()));
    }

    @Test
    public void testChannelSettingTimeouts() {
        mWifiAccessPointCore.channel()
                            .updateAvailableChannels(EnumSet.allOf(Channel.class))
                            .updateAutoSelectSupportFlag(true);
        mWifiAccessPointCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpToDate()));

        // --- select ---

        // mock user sets value
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_2);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpdating()));

        // mock backend updates value
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL,
                Channel.BAND_2_4_CHANNEL_2);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpToDate()));

        // mock different selection mode to ensure it is properly rolled back to
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND,
                Channel.BAND_2_4_CHANNEL_2);

        // mock user sets value
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_3);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_3),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                settingIsUpToDate()));

        // --- autoselect (any band) ---

        // mock user sets value
        mWifiAccessPoint.channel().autoSelect();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                settingIsUpdating()));

        // mock backend updates value
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND,
                Channel.BAND_5_CHANNEL_34);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                settingIsUpToDate()));

        // mock different selection mode to ensure it is properly rolled back to
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL,
                Channel.BAND_5_CHANNEL_34);

        // mock user sets value
        mWifiAccessPoint.channel().autoSelect();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL),
                settingIsUpToDate()));

        // --- autoselect (restrict to band) ---

        // mock user sets value
        mWifiAccessPoint.channel().autoSelect(Band.B_2_4_GHZ);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_5_CHANNEL_34),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                settingIsUpdating()));

        // mock backend updates value
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND,
                Channel.BAND_2_4_CHANNEL_3);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_3),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_3),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND),
                settingIsUpToDate()));

        // mock different selection mode to ensure it is properly rolled back to
        mWifiAccessPointCore.channel().updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND,
                Channel.BAND_2_4_CHANNEL_3);

        // mock user sets value
        mWifiAccessPoint.channel().autoSelect(Band.B_5_GHZ);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_3),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_3),
                channelSettingSelectionModeIs(WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND),
                settingIsUpToDate()));
    }

    @Test
    public void testSsid() {
        mWifiAccessPointCore.publish();

        // test default value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.ssid(), stringSettingIsUpToDateAt(""));

        // test nothing changes if low-level denies
        mBackend.mAccept = false;
        mWifiAccessPoint.ssid().setValue("denied");

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mSsid, is("denied"));
        assertThat(mWifiAccessPoint.ssid(), stringSettingIsUpToDateAt(""));

        // mock low-level accepting changes again
        mBackend.reset();

        // changing to the current value should not trigger any change
        mWifiAccessPoint.ssid().setValue("");

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mSsid, nullValue());
        assertThat(mWifiAccessPoint.ssid(), stringSettingIsUpToDateAt(""));

        // user changes to a different value
        mWifiAccessPoint.ssid().setValue("new");

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mSsid, is("new"));
        assertThat(mWifiAccessPoint.ssid(), stringSettingIsUpdatingTo("new"));

        // mock update from low-level
        mWifiAccessPointCore.ssid().updateValue("low-level");
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.ssid(), stringSettingIsUpToDateAt("low-level"));
    }

    @Test
    public void testSecurity() {
        mWifiAccessPointCore.security().updateSupportedModes(EnumSet.of(SecuritySetting.Mode.OPEN));
        mWifiAccessPointCore.publish();

        // test default value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(SecuritySetting.Mode.OPEN),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // test enabling WPA2 security whereas it's not supported
        assertThat(mWifiAccessPoint.security().secureWithWPA2("not-supported"), is(true));

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mSecurityMode, nullValue());
        assertThat(mBackend.mPassword, nullValue());
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(SecuritySetting.Mode.OPEN),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // supporting all modes
        mWifiAccessPointCore.security().updateSupportedModes(EnumSet.allOf(SecuritySetting.Mode.class));
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // test invalid password (too short)
        assertThat(mWifiAccessPoint.security().secureWithWPA2("invalid"), is(false));

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mSecurityMode, nullValue());
        assertThat(mBackend.mPassword, nullValue());
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        mBackend.mAccept = false;
        assertThat(mWifiAccessPoint.security().secureWithWPA2("backend-denied"), is(true));

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.WPA2_SECURED));
        assertThat(mBackend.mPassword, is("backend-denied"));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // mock low-level accepting changes again
        mBackend.reset();

        // disabling security if already open should not trigger any change
        mWifiAccessPoint.security().open();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mSecurityMode, nullValue());
        assertThat(mBackend.mPassword, nullValue());
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // user enables WPA2 security
        assertThat(mWifiAccessPoint.security().secureWithWPA2("password"), is(true));

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.WPA2_SECURED));
        assertThat(mBackend.mPassword, is("password"));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.security().updateMode(SecuritySetting.Mode.WPA2_SECURED);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date now
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));


        // securing with WPA2 again should trigger a change, even with the same password
        mBackend.reset();
        assertThat(mWifiAccessPoint.security().secureWithWPA2("password"), is(true));

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.WPA2_SECURED));
        assertThat(mBackend.mPassword, is("password"));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.security().updateMode(SecuritySetting.Mode.WPA2_SECURED);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // supporting only WPA2 mode
        mWifiAccessPointCore.security().updateSupportedModes(EnumSet.of(SecuritySetting.Mode.WPA2_SECURED));
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(SecuritySetting.Mode.WPA2_SECURED),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // test disabling security whereas it's not supported
        mWifiAccessPoint.security().open();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.WPA2_SECURED));
        assertThat(mBackend.mPassword, is("password"));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(SecuritySetting.Mode.WPA2_SECURED),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // supporting again all modes
        mWifiAccessPointCore.security().updateSupportedModes(EnumSet.allOf(SecuritySetting.Mode.class));
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));


        // test low-level denial for open() call
        mBackend.mAccept = false;
        mWifiAccessPoint.security().open();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.OPEN));
        assertThat(mBackend.mPassword, nullValue());
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // user disables security
        mBackend.reset();
        mBackend.mPassword = "not null";
        mWifiAccessPoint.security().open();

        // setting should report change and be updating to the user selected value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mSecurityMode, is(SecuritySetting.Mode.OPEN));
        assertThat(mBackend.mPassword, nullValue());
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpdating()));

        // mock update from low-level
        mWifiAccessPointCore.security().updateMode(SecuritySetting.Mode.OPEN);
        mWifiAccessPointCore.notifyUpdated();

        // setting should report change and be up to date
        assertThat(mComponentChangeCnt, is(10));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingSupportsModes(EnumSet.allOf(SecuritySetting.Mode.class)),
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));
    }

    @Test
    public void testSecuritySettingTimeouts() {
        mWifiAccessPointCore.security().updateSupportedModes(EnumSet.allOf(SecuritySetting.Mode.class));
        mWifiAccessPointCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        // mock user sets value
        mWifiAccessPoint.security().secureWithWPA2("password");

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpdating()));

        // mock backend updates value
        mWifiAccessPointCore.security().updateMode(SecuritySetting.Mode.WPA2_SECURED);
        mWifiAccessPointCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        // mock user sets value
        mWifiAccessPoint.security().open();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));
    }

    @Test
    public void testPasswordValidity() {
        // too short
        assertThat(SecuritySetting.isPasswordValid("abcdefg"), is(false));
        // minimal size
        assertThat(SecuritySetting.isPasswordValid("abcdefgh"), is(true));
        // maximal size
        assertThat(SecuritySetting.isPasswordValid(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!"), is(true));
        // too long
        assertThat(SecuritySetting.isPasswordValid(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?"), is(false));
        // special characters and space
        assertThat(SecuritySetting.isPasswordValid("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ "), is(true));
        // non printable ASCII characters
        assertThat(SecuritySetting.isPasswordValid("abcdefgh\u0019"), is(false));
        assertThat(SecuritySetting.isPasswordValid("abcdefgh\u007F"), is(false));
        assertThat(SecuritySetting.isPasswordValid("abcdefghé"), is(false));
    }

    @Test
    public void testCancelRollbacks() {
        mWifiAccessPointCore.country()
                            .updateAvailableCodes(new HashSet<>(Arrays.asList("FR", "ES")))
                            .updateCode("FR");
        mWifiAccessPointCore.channel()
                            .updateAvailableChannels(Arrays.asList(
                                    Channel.BAND_2_4_CHANNEL_1,
                                    Channel.BAND_2_4_CHANNEL_2))
                            .updateChannel(WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL,
                                    Channel.BAND_2_4_CHANNEL_1);
        mWifiAccessPointCore.security()
                            .updateSupportedModes(EnumSet.allOf(SecuritySetting.Mode.class))
                            .updateMode(SecuritySetting.Mode.OPEN);
        mWifiAccessPointCore.publish();

        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingValueIs(WifiAccessPoint.Environment.OUTDOOR),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("FR"),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_1),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.OPEN),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.ssid(), allOf(
                stringSettingValueIs(""),
                settingIsUpToDate()));

        // mock user changes settings
        mWifiAccessPoint.environment().setValue(WifiAccessPoint.Environment.INDOOR);
        mWifiAccessPoint.country().select("ES");
        mWifiAccessPoint.channel().select(Channel.BAND_2_4_CHANNEL_2);
        mWifiAccessPoint.security().secureWithWPA2("password");
        mWifiAccessPoint.ssid().setValue("ssid");


        // cancel all rollbacks
        mWifiAccessPointCore.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingValueIs(WifiAccessPoint.Environment.INDOOR),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("ES"),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.ssid(), allOf(
                stringSettingValueIs("ssid"),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mWifiAccessPoint.environment(), allOf(
                enumSettingValueIs(WifiAccessPoint.Environment.INDOOR),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.country(), allOf(
                countrySettingCodeIs("ES"),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.channel(), allOf(
                channelSettingChannelIs(Channel.BAND_2_4_CHANNEL_2),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.security(), allOf(
                securitySettingModeIs(SecuritySetting.Mode.WPA2_SECURED),
                settingIsUpToDate()));

        assertThat(mWifiAccessPoint.ssid(), allOf(
                stringSettingValueIs("ssid"),
                settingIsUpToDate()));
    }

    private static final class Backend implements WifiAccessPointCore.Backend {

        WifiAccessPoint.Environment mEnvironment;

        String mCountryCode;

        String mSsid;

        Channel mChannel;

        Band mAutoBand;

        SecuritySetting.Mode mSecurityMode;

        String mPassword;

        boolean mAccept;

        void reset() {
            mAccept = true;
            mEnvironment = null;
            mCountryCode = null;
            mSsid = null;
            mChannel = null;
            mAutoBand = null;
            mSecurityMode = null;
            mPassword = null;
        }

        @Override
        public boolean setEnvironment(@NonNull WifiAccessPoint.Environment environment) {
            mEnvironment = environment;
            return mAccept;
        }

        @Override
        public boolean setCountry(@NonNull String code) {
            mCountryCode = code;
            return mAccept;
        }

        @Override
        public boolean setSsid(@NonNull String ssid) {
            mSsid = ssid;
            return mAccept;
        }

        @Override
        public boolean selectChannel(@NonNull Channel channel) {
            mChannel = channel;
            return mAccept;
        }

        @Override
        public boolean autoSelectChannel(@Nullable Band band) {
            mAutoBand = band;
            return mAccept;
        }

        @Override
        public boolean setSecurity(@NonNull SecuritySetting.Mode mode, @Nullable String password) {
            mSecurityMode = mode;
            mPassword = password;
            return mAccept;
        }
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
