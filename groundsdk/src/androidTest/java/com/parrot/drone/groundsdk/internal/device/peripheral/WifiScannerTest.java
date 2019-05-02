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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiScanner;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.wifi.WifiScannerCore;

import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class WifiScannerTest {

    private MockComponentStore<Peripheral> mStore;

    private WifiScannerCore mWifiScannerCore;

    private WifiScanner mWifiScanner;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mWifiScannerCore = new WifiScannerCore(mStore, mBackend);
        mWifiScanner = mStore.get(WifiScanner.class);
        mStore.registerObserver(WifiScanner.class, () -> {
            mComponentChangeCnt++;
            mWifiScanner = mStore.get(WifiScanner.class);
        });
        mComponentChangeCnt = 0;
        mBackend.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mWifiScanner, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mWifiScannerCore.publish();
        assertThat(mWifiScanner, is(mWifiScannerCore));
        assertThat(mComponentChangeCnt, is(1));

        mWifiScannerCore.unpublish();
        assertThat(mWifiScanner, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testScanning() {
        mWifiScannerCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiScanner.isScanning(), is(false));

        mWifiScannerCore.updateScanningFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));

        mWifiScannerCore.updateScanningFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));

        mWifiScannerCore.updateScanningFlag(false).notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiScanner.isScanning(), is(false));

    }

    @Test
    public void testScanResults() {
        mWifiScannerCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        for (Channel channel : EnumSet.allOf(Channel.class)) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        Map<Channel, Integer> scanResult = new EnumMap<>(Channel.class);
        scanResult.put(Channel.BAND_2_4_CHANNEL_1, 1);
        scanResult.put(Channel.BAND_2_4_CHANNEL_2, 2);
        scanResult.put(Channel.BAND_5_CHANNEL_34, 3);
        mWifiScannerCore.updateScannedChannels(scanResult).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_1), is(1));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_2), is(2));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_5_CHANNEL_34), is(3));
        for (Channel channel : EnumSet.complementOf(EnumSet.copyOf(scanResult.keySet()))) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        mWifiScannerCore.updateScannedChannels(scanResult).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_1), is(1));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_2), is(2));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_5_CHANNEL_34), is(3));
        for (Channel channel : EnumSet.complementOf(EnumSet.copyOf(scanResult.keySet()))) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }

        scanResult.remove(Channel.BAND_2_4_CHANNEL_2);

        mWifiScannerCore.updateScannedChannels(scanResult).notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_2_4_CHANNEL_1), is(1));
        assertThat(mWifiScanner.getChannelOccupationRate(Channel.BAND_5_CHANNEL_34), is(3));
        for (Channel channel : EnumSet.complementOf(EnumSet.copyOf(scanResult.keySet()))) {
            assertThat(mWifiScanner.getChannelOccupationRate(channel), is(0));
        }
    }

    @Test
    public void testStartStopScan() {
        mWifiScannerCore.publish();

        assertThat(mBackend.mStartScanCnt, is(0));
        assertThat(mBackend.mStopScanCnt, is(0));
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mWifiScanner.isScanning(), is(false));

        mWifiScanner.startScan();
        assertThat(mBackend.mStartScanCnt, is(1));
        assertThat(mBackend.mStopScanCnt, is(0));

        mWifiScannerCore.updateScanningFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mWifiScanner.isScanning(), is(true));

        mWifiScanner.startScan();
        assertThat(mBackend.mStartScanCnt, is(1));
        assertThat(mBackend.mStopScanCnt, is(0));

        mWifiScanner.stopScan();
        assertThat(mBackend.mStartScanCnt, is(1));
        assertThat(mBackend.mStopScanCnt, is(1));

        mWifiScannerCore.updateScanningFlag(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mWifiScanner.isScanning(), is(false));

        mWifiScanner.stopScan();
        assertThat(mBackend.mStartScanCnt, is(1));
        assertThat(mBackend.mStopScanCnt, is(1));
    }

    private static final class Backend implements WifiScannerCore.Backend {

        int mStartScanCnt;

        int mStopScanCnt;

        void reset() {
            mStartScanCnt = mStopScanCnt = 0;
        }

        @Override
        public void startScan() {
            mStartScanCnt++;
        }

        @Override
        public void stopScan() {
            mStopScanCnt++;
        }
    }
}
