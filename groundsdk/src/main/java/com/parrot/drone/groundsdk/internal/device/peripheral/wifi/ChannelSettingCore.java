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
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/** Implementation class for {@code ChannelSetting}. */
public final class ChannelSettingCore extends WifiAccessPoint.ChannelSetting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets the access point channel.
         *
         * @param channel new channel value
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean selectChannel(@NonNull Channel channel);

        /**
         * Requests auto-selection of the most appropriate access point channel.
         *
         * @param band frequency band to restrict auto-selection to, use {@code null} to allow any band
         *
         * @return {@code true} if the request could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean autoSelectChannel(@Nullable Band band);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Currently selected channel. */
    @NonNull
    private Channel mChannel;

    /** Current selection mode. */
    @NonNull
    private SelectionMode mMode;

    /** Currently available channels. */
    @NonNull
    private final Set<Channel> mAvailableChannels;

    /** Collects all bands for which a channel with this band is in the {@code mAvailableChannels} set. */
    private final Set<Band> mAvailableBands;

    /** {@code true} (by default) when any kind of auto-selection is unsupported. */
    private boolean mAutoSelectSupported;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    ChannelSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mChannel = Channel.BAND_2_4_CHANNEL_1;
        mMode = SelectionMode.MANUAL;
        mAvailableChannels = EnumSet.noneOf(Channel.class);
        mAvailableBands = EnumSet.noneOf(Band.class);
        mAutoSelectSupported = true;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public SelectionMode getSelectionMode() {
        return mMode;
    }

    @NonNull
    @Override
    public Set<Channel> getAvailableChannels() {
        Set<Channel> channels = EnumSet.of(mChannel);
        channels.addAll(mAvailableChannels);
        return Collections.unmodifiableSet(channels);
    }

    @NonNull
    @Override
    public Channel get() {
        return mChannel;
    }

    @Override
    public boolean canAutoSelect() {
        return mAutoSelectSupported && !mAvailableBands.isEmpty();
    }

    @Override
    public void select(@NonNull Channel channel) {
        if ((mChannel != channel || mMode != SelectionMode.MANUAL) && mAvailableChannels.contains(channel)
            && mBackend.selectChannel(channel)) {
            Channel rollbackChannel = mChannel;
            SelectionMode rollbackMode = mMode;
            mChannel = channel;
            mMode = SelectionMode.MANUAL;
            mController.postRollback(() -> {
                mChannel = rollbackChannel;
                mMode = rollbackMode;
            });
        }
    }

    @Override
    public void autoSelect() {
        if (canAutoSelect() && mBackend.autoSelectChannel(null)) {
            SelectionMode rollbackMode = mMode;
            mMode = SelectionMode.AUTO_ANY_BAND;
            mController.postRollback(() -> mMode = rollbackMode);
        }
    }

    @Override
    public void autoSelect(@NonNull Band band) {
        if (canAutoSelect(band) && mBackend.autoSelectChannel(band)) {
            SelectionMode rollbackMode = mMode;
            switch (band) {
                case B_2_4_GHZ:
                    mMode = SelectionMode.AUTO_2_4_GHZ_BAND;
                    break;
                case B_5_GHZ:
                    mMode = SelectionMode.AUTO_5_GHZ_BAND;
                    break;
            }
            mController.postRollback(() -> mMode = rollbackMode);
        }
    }

    @Override
    public boolean canAutoSelect(@NonNull Band band) {
        return mAutoSelectSupported && mAvailableBands.contains(band);
    }

    /**
     * Updates current channel and selection mode.
     *
     * @param mode    new selection mode
     * @param channel new Wifi channel
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ChannelSettingCore updateChannel(@NonNull SelectionMode mode, @NonNull Channel channel) {
        if (mController.cancelRollback() || mMode != mode || mChannel != channel) {
            mMode = mode;
            mChannel = channel;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates available channels for access point setup.
     *
     * @param availableChannels new collection of available channels
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ChannelSettingCore updateAvailableChannels(@NonNull Collection<Channel> availableChannels) {
        if (mAvailableChannels.addAll(availableChannels) | mAvailableChannels.retainAll(availableChannels)) {
            mAvailableBands.clear();
            // compute all available bands from those channels to know which selection modes are available
            int allBandsSize = Band.values().length;
            Iterator<Channel> iter = mAvailableChannels.iterator();
            while (iter.hasNext() && mAvailableBands.size() < allBandsSize) {
                mAvailableBands.add(iter.next().getBand());
            }
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates the auto-selection support flag.
     *
     * @param supported new auto-selection support value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ChannelSettingCore updateAutoSelectSupportFlag(boolean supported) {
        if (mAutoSelectSupported != supported) {
            mAutoSelectSupported = supported;
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
