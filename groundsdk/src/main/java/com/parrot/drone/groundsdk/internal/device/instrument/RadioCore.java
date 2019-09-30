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

package com.parrot.drone.groundsdk.internal.device.instrument;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.instrument.Radio;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the Radio instrument. */
public final class RadioCore extends SingletonComponentCore implements Radio {

    /** Description of Radio. */
    private static final ComponentDescriptor<Instrument, Radio> DESC = ComponentDescriptor.of(Radio.class);

    /** Received Signal Strength Indication (dBm). */
    private int mRssi;

    /** The link signal quality, from 0 to 4 or -1 if unknown. */
    private int mLinkSignalQuality;

    /** {@code true} if the radio link is perturbed by external elements, otherwise {@code false}. */
    private boolean mLinkPerturbed;

    /** {@code true} if there is a probable 4G interference coming from the user phone, otherwise {@code false}. */
    private boolean m4GInterfering;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs
     */
    public RadioCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mLinkSignalQuality = -1;
    }

    @Override
    public int getRssi() {
        return mRssi;
    }

    @Override
    public int getLinkSignalQuality() {
        return mLinkSignalQuality;
    }

    @Override
    public boolean isLinkPerturbed() {
        return mLinkPerturbed;
    }

    @Override
    public boolean is4GInterfering() {
        return m4GInterfering;
    }

    /**
     * Updates the current RSSI value.
     *
     * @param rssi new RSSI value
     *
     * @return this, to allow call chaining
     */
    public RadioCore updateRssi(int rssi) {
        if (rssi != mRssi) {
            mRssi = rssi;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current link signal quality.
     *
     * @param linkSignalQuality new link signal quality from 0 to 4
     *
     * @return this, to allow call chaining
     */
    public RadioCore updateLinkSignalQuality(@IntRange(from = 0, to = 4) int linkSignalQuality) {
        if (linkSignalQuality != mLinkSignalQuality) {
            mLinkSignalQuality = linkSignalQuality;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current radio link perturbation state.
     *
     * @param linkPerturbed new link perturbation state
     *
     * @return this, to allow call chaining
     */
    public RadioCore updateLinkPerturbed(boolean linkPerturbed) {
        if (linkPerturbed != mLinkPerturbed) {
            mLinkPerturbed = linkPerturbed;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current 4G interference state.
     *
     * @param interfering new 4G interference state
     *
     * @return this, to allow call chaining
     */
    public RadioCore update4GInterfering(boolean interfering) {
        if (interfering != m4GInterfering) {
            m4GInterfering = interfering;
            mChanged = true;
        }
        return this;
    }
}
