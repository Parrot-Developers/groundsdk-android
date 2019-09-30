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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.instrument.PhotoProgressIndicator;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleCore;

/** Core class for PhotoProgressIndicator. */
public class PhotoProgressIndicatorCore extends SingletonComponentCore implements PhotoProgressIndicator {

    /** Description of PhotoProgressIndicator. */
    private static final ComponentDescriptor<Instrument, PhotoProgressIndicator> DESC =
            ComponentDescriptor.of(PhotoProgressIndicator.class);

    /** Remaining time before next photo, in seconds. */
    @NonNull
    private final OptionalDoubleCore mRemainingTime;

    /** Remaining distance before next photo, in meters. */
    @NonNull
    private final OptionalDoubleCore mRemainingDistance;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs
     */
    public PhotoProgressIndicatorCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mRemainingTime = new OptionalDoubleCore();
        mRemainingDistance = new OptionalDoubleCore();
    }

    @NonNull
    @Override
    public OptionalDoubleCore getRemainingTime() {
        return mRemainingTime;
    }

    @NonNull
    @Override
    public OptionalDoubleCore getRemainingDistance() {
        return mRemainingDistance;
    }

    /**
     * Updates the remaining time before next photo.
     *
     * @param time new remaining time, in seconds
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public PhotoProgressIndicatorCore updateRemainingTime(double time) {
        mChanged |= mRemainingTime.setValue(time);
        return this;
    }

    /**
     * Resets the remaining time before next photo, marking it invalid.
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public PhotoProgressIndicatorCore resetRemainingTime() {
        mChanged |= mRemainingTime.resetValue();
        return this;
    }

    /**
     * Updates the remaining distance before next photo.
     *
     * @param distance new remaining distance, in meters
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public PhotoProgressIndicatorCore updateRemainingDistance(double distance) {
        mChanged |= mRemainingDistance.setValue(distance);
        return this;
    }

    /**
     * Resets the remaining distance before next photo, marking it invalid.
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public PhotoProgressIndicatorCore resetRemainingDistance() {
        mChanged |= mRemainingDistance.resetValue();
        return this;
    }
}
