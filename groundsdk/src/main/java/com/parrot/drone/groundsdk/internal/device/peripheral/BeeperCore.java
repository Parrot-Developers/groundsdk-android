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

import com.parrot.drone.groundsdk.device.peripheral.Beeper;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for Beeper. */
public class BeeperCore extends SingletonComponentCore implements Beeper {

    /** Description of Beeper. */
    private static final ComponentDescriptor<Peripheral, Beeper> DESC = ComponentDescriptor.of(Beeper.class);

    /** Engine-specific backend for Beeper. */
    public interface Backend {

        /**
         * Starts to play alert sound.
         * <p>
         * The alert sound shall be stopped with {@link #stopAlertSound()}.
         *
         * @return {@code true} if the start alert sound operation could be initiated, otherwise {@code false}
         */
        boolean startAlertSound();

        /**
         * Stops to play alert sound.
         *
         * @return {@code true} if the stop alert sound operation could be initiated, otherwise {@code false}
         */
        boolean stopAlertSound();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** {@code true} when device is currently playing alert sound. */
    private boolean mAlertSoundPlaying;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public BeeperCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
    }

    @Override
    public boolean startAlertSound() {
        return !mAlertSoundPlaying && mBackend.startAlertSound();
    }

    @Override
    public boolean stopAlertSound() {
        return mAlertSoundPlaying && mBackend.stopAlertSound();
    }

    @Override
    public boolean isAlertSoundPlaying() {
        return mAlertSoundPlaying;
    }

    /**
     * Updates the alert sound playing state.
     *
     * @param playing {@code true} if the device is currently playing alert sound, otherwise {@code false}
     *
     * @return this, to allow call chaining
     */
    public final BeeperCore updateAlertSoundPlaying(boolean playing) {
        if (playing != mAlertSoundPlaying) {
            mAlertSoundPlaying = playing;
            mChanged = true;
        }
        return this;
    }
}
