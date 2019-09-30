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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;

/**
 * WifiScanner peripheral interface for Drone devices.
 * <p>
 * Allows scanning the device's WIFI environment to obtain information about the current occupation of WIFI channels.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(WifiScanner.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface WifiScanner extends Peripheral {

    /**
     * Tells whether the peripheral is currently scanning WIFI networks environment.
     *
     * @return {@code true} if a scan operation is ongoing, otherwise {@code false}
     */
    boolean isScanning();

    /**
     * Requests the WIFI environment scanning process to start.
     * <p>
     * While scanning, the peripheral will report updates regularly; WIFI channels occupation can be obtained
     * using {@link #getChannelOccupationRate(Channel)} method.
     * <p>
     * This has no effect if scanning is already {@link #isScanning() ongoing}
     */
    void startScan();

    /**
     * Requests an ongoing scan operation to stop.
     * <p>
     * When scanning stops, internal scan results are cleared and the {@link #getChannelOccupationRate(Channel)}
     * method will report {@code 0} for any channel.
     * <p>
     * This has no effect if the peripheral is not currently {@link #isScanning() scanning}.
     */
    void stopScan();

    /**
     * Retrieves the amount of WIFI networks that are currently using a given WIFI channel.
     * <p>
     * The list of WIFI channels that are currently available to configure the device's access point may be obtained
     * from the {@link WifiAccessPoint} peripheral using {@link WifiAccessPoint.ChannelSetting#getAvailableChannels()}.
     *
     * @param channel the WIFI channel to query occupation information of
     *
     * @return the channel occupation rate
     */
    @IntRange(from = 0)
    int getChannelOccupationRate(@NonNull Channel channel);
}
