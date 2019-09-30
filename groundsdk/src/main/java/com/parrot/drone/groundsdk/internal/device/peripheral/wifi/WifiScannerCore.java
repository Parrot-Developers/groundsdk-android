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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiScanner;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.EnumMap;
import java.util.Map;

/** Core class for WifiScanner. */
public final class WifiScannerCore extends SingletonComponentCore implements WifiScanner {

    /** Description of WifiScanner. */
    private static final ComponentDescriptor<Peripheral, WifiScanner> DESC =
            ComponentDescriptor.of(WifiScanner.class);

    /** Engine-specific backend for WifiScanner. */
    public interface Backend {

        /**
         * Starts scanning channels occupation rate.
         */
        void startScan();

        /**
         * Stops ongoing channels occupation rate scan.
         */
        void stopScan();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Map of occupation rate (amount of scanned networks), by wifi channel. */
    @NonNull
    private final Map<Channel, Integer> mScannedChannels;

    /** {@code true} when scanning, otherwise {@code false}. */
    private boolean mScanning;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public WifiScannerCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mScannedChannels = new EnumMap<>(Channel.class);
    }

    @Override
    public void unpublish() {
        mScannedChannels.clear();
        mScanning = false;
        super.unpublish();
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public void startScan() {
        if (!mScanning) {
            mBackend.startScan();
        }
    }

    @Override
    public void stopScan() {
        if (mScanning) {
            mBackend.stopScan();
        }
    }

    @Override
    public int getChannelOccupationRate(@NonNull Channel channel) {
        Integer rate = mScannedChannels.get(channel);
        return rate == null ? 0 : rate;
    }

    /**
     * Updates channels occupation rate.
     *
     * @param scanResult new map of occupation rate (amount of wifi networks) by channel
     *
     * @return this, to allow call chaining
     */
    public WifiScannerCore updateScannedChannels(@NonNull Map<Channel, Integer> scanResult) {
        for (Map.Entry<Channel, Integer> entry : scanResult.entrySet()) {
            Integer rate = entry.getValue();
            mChanged |= !rate.equals(mScannedChannels.put(entry.getKey(), rate));
        }
        mChanged |= mScannedChannels.keySet().retainAll(scanResult.keySet());
        return this;
    }

    /**
     * Updates the scanning flag.
     *
     * @param scanning new scanning flag value
     *
     * @return this, to allow call chaining
     */
    public WifiScannerCore updateScanningFlag(boolean scanning) {
        if (scanning != mScanning) {
            mScanning = scanning;
            mChanged = true;
        }
        return this;
    }
}
