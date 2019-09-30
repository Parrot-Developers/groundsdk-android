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

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureWifi;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_WIFI;

/**
 * Channels Arsdk conversion utility class.
 * <p>
 * The {@code obtain} methods always return a Wifi {@link Channel} instance, based on the given information, possibly
 * using fallback policies when the information is erroneous or incomplete.
 * <p>
 * The {@code get} methods return a valid non-{@code null} {@code Channel} instance only if the provided information
 * allows to find exactly the requested channel.
 */
final class Channels {

    /**
     * Retrieves the wifi channel corresponding to a given frequency band/channel id pair.
     *
     * @param band      frequency band where the channel operates. If {@code null}, search in all bands for the given
     *                  channel identifier
     * @param channelId identifier of the channel
     *
     * @return the corresponding wifi {@code Channel}, if known, otherwise {@link Channel#BAND_5_CHANNEL_34} if the
     *         specified band is 5 GHz, otherwise {@link Channel#BAND_2_4_CHANNEL_1}
     */
    @NonNull
    static Channel obtain(@Nullable ArsdkFeatureWifi.Band band, int channelId) {
        Channel channel = null;
        Channel defaultChannel = Channel.BAND_2_4_CHANNEL_1;
        if (band != null) {
            switch (band) {
                case E2_4_GHZ:
                    channel = CHANNELS_2_4_GHZ.get(channelId);
                    break;
                case E5_GHZ:
                    channel = CHANNELS_5_GHZ.get(channelId);
                    defaultChannel = Channel.BAND_5_CHANNEL_34;
                    break;
            }
        } else {
            channel = CHANNELS_2_4_GHZ.get(channelId);
            if (channel == null) {
                channel = CHANNELS_5_GHZ.get(channelId);
            }
        }
        if (channel == null) {
            ULog.e(TAG_WIFI, "Unsupported channel [band: " + band + ", id: " + channelId + "]");
            channel = defaultChannel;
        }
        return channel;
    }

    /**
     * Retrieves the wifi channel corresponding to a given frequency band/channel id pair.
     *
     * @param band      frequency band where the channel operates
     * @param channelId identifier of the channel
     *
     * @return the corresponding wifi {@code Channel}, if known, otherwise {@code null}
     */
    @Nullable
    static Channel get(@Nullable ArsdkFeatureWifi.Band band, int channelId) {
        Channel channel = null;
        if (band != null) {
            switch (band) {
                case E2_4_GHZ:
                    channel = CHANNELS_2_4_GHZ.get(channelId);
                    break;
                case E5_GHZ:
                    channel = CHANNELS_5_GHZ.get(channelId);
                    break;
            }
            if (channel == null) {
                ULog.e(TAG_WIFI, "Unsupported channel [band: " + band + ", id: " + channelId + "]");
            }
        }
        return channel;
    }

    /** Map of 2.4 GHz band channels, by channel id. */
    private static final SparseArray<Channel> CHANNELS_2_4_GHZ = new SparseArray<>();

    /** Map of 5 GHz band channels, by channel id. */
    private static final SparseArray<Channel> CHANNELS_5_GHZ = new SparseArray<>();

    static {
        for (Channel channel : Channel.values()) {
            int channelId = channel.getChannelId();
            switch (channel.getBand()) {
                case B_2_4_GHZ:
                    CHANNELS_2_4_GHZ.put(channelId, channel);
                    break;
                case B_5_GHZ:
                    CHANNELS_5_GHZ.put(channelId, channel);
                    break;
            }
        }
    }

    /** Private constructor for static utility class. */
    private Channels() {
    }
}
