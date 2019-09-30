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

package com.parrot.drone.groundsdk.device.peripheral.wifi;

import androidx.annotation.NonNull;

/**
 * Represents a Wifi channel.
 */
@SuppressWarnings("JavaDoc") // channel names are self-explanatory enough
public enum Channel {

    BAND_2_4_CHANNEL_1(Band.B_2_4_GHZ, 1),
    BAND_2_4_CHANNEL_2(Band.B_2_4_GHZ, 2),
    BAND_2_4_CHANNEL_3(Band.B_2_4_GHZ, 3),
    BAND_2_4_CHANNEL_4(Band.B_2_4_GHZ, 4),
    BAND_2_4_CHANNEL_5(Band.B_2_4_GHZ, 5),
    BAND_2_4_CHANNEL_6(Band.B_2_4_GHZ, 6),
    BAND_2_4_CHANNEL_7(Band.B_2_4_GHZ, 7),
    BAND_2_4_CHANNEL_8(Band.B_2_4_GHZ, 8),
    BAND_2_4_CHANNEL_9(Band.B_2_4_GHZ, 9),
    BAND_2_4_CHANNEL_10(Band.B_2_4_GHZ, 10),
    BAND_2_4_CHANNEL_11(Band.B_2_4_GHZ, 11),
    BAND_2_4_CHANNEL_12(Band.B_2_4_GHZ, 12),
    BAND_2_4_CHANNEL_13(Band.B_2_4_GHZ, 13),
    BAND_2_4_CHANNEL_14(Band.B_2_4_GHZ, 14),

    BAND_5_CHANNEL_34(Band.B_5_GHZ, 34),
    BAND_5_CHANNEL_36(Band.B_5_GHZ, 36),
    BAND_5_CHANNEL_38(Band.B_5_GHZ, 38),
    BAND_5_CHANNEL_40(Band.B_5_GHZ, 40),
    BAND_5_CHANNEL_42(Band.B_5_GHZ, 42),
    BAND_5_CHANNEL_44(Band.B_5_GHZ, 44),
    BAND_5_CHANNEL_46(Band.B_5_GHZ, 46),
    BAND_5_CHANNEL_48(Band.B_5_GHZ, 48),
    BAND_5_CHANNEL_50(Band.B_5_GHZ, 50),
    BAND_5_CHANNEL_52(Band.B_5_GHZ, 52),
    BAND_5_CHANNEL_54(Band.B_5_GHZ, 54),
    BAND_5_CHANNEL_56(Band.B_5_GHZ, 56),
    BAND_5_CHANNEL_58(Band.B_5_GHZ, 58),
    BAND_5_CHANNEL_60(Band.B_5_GHZ, 60),
    BAND_5_CHANNEL_62(Band.B_5_GHZ, 62),
    BAND_5_CHANNEL_64(Band.B_5_GHZ, 64),
    BAND_5_CHANNEL_100(Band.B_5_GHZ, 100),
    BAND_5_CHANNEL_102(Band.B_5_GHZ, 102),
    BAND_5_CHANNEL_104(Band.B_5_GHZ, 104),
    BAND_5_CHANNEL_106(Band.B_5_GHZ, 106),
    BAND_5_CHANNEL_108(Band.B_5_GHZ, 108),
    BAND_5_CHANNEL_110(Band.B_5_GHZ, 110),
    BAND_5_CHANNEL_112(Band.B_5_GHZ, 112),
    BAND_5_CHANNEL_114(Band.B_5_GHZ, 114),
    BAND_5_CHANNEL_116(Band.B_5_GHZ, 116),
    BAND_5_CHANNEL_118(Band.B_5_GHZ, 118),
    BAND_5_CHANNEL_120(Band.B_5_GHZ, 120),
    BAND_5_CHANNEL_122(Band.B_5_GHZ, 122),
    BAND_5_CHANNEL_124(Band.B_5_GHZ, 124),
    BAND_5_CHANNEL_126(Band.B_5_GHZ, 126),
    BAND_5_CHANNEL_128(Band.B_5_GHZ, 128),
    BAND_5_CHANNEL_132(Band.B_5_GHZ, 132),
    BAND_5_CHANNEL_134(Band.B_5_GHZ, 134),
    BAND_5_CHANNEL_136(Band.B_5_GHZ, 136),
    BAND_5_CHANNEL_138(Band.B_5_GHZ, 138),
    BAND_5_CHANNEL_140(Band.B_5_GHZ, 140),
    BAND_5_CHANNEL_142(Band.B_5_GHZ, 142),
    BAND_5_CHANNEL_144(Band.B_5_GHZ, 144),
    BAND_5_CHANNEL_149(Band.B_5_GHZ, 149),
    BAND_5_CHANNEL_151(Band.B_5_GHZ, 151),
    BAND_5_CHANNEL_153(Band.B_5_GHZ, 153),
    BAND_5_CHANNEL_155(Band.B_5_GHZ, 155),
    BAND_5_CHANNEL_157(Band.B_5_GHZ, 157),
    BAND_5_CHANNEL_159(Band.B_5_GHZ, 159),
    BAND_5_CHANNEL_161(Band.B_5_GHZ, 161),
    BAND_5_CHANNEL_165(Band.B_5_GHZ, 165);

    /**
     * Retrieves the frequency band where this channel operates.
     *
     * @return channel frequency band
     */
    @NonNull
    public Band getBand() {
        return mBand;
    }

    /**
     * Retrieves the channel identifier.
     *
     * @return channel identifier
     */
    public final int getChannelId() {
        return mChannelId;
    }

    /** Channel frequency band. */
    @NonNull
    private final Band mBand;

    /** Channel identifier. */
    private final int mChannelId;

    /**
     * Constructor.
     *
     * @param band      channel frequency band
     * @param channelId channel identifier
     */
    Channel(@NonNull Band band, int channelId) {
        mBand = band;
        mChannelId = channelId;
    }
}
