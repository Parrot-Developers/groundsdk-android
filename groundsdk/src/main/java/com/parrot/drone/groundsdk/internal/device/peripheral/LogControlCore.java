/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.LogControl;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for LogControl. */
public class LogControlCore extends SingletonComponentCore implements LogControl {

    /** Description of LogControl. */
    private static final ComponentDescriptor<Peripheral, LogControl> DESC =
            ComponentDescriptor.of(LogControl.class);

    /** Engine-specific backend for LogControl. */
    public interface Backend {

        /**
         * Deactivate the logs.
         *
         * @return {@code true} if the state could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean deactivateLogs();
    }

    /** Logs activation state. */
    private boolean mLogsEnabled;

    /**{ @code true} if connected drone supports logs deactivation. */
    private boolean mDeactivateLogsSupported;

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public LogControlCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mLogsEnabled = true;
        mBackend = backend;
    }

    @Override
    public boolean canDeactivateLogs() {
        return mDeactivateLogsSupported;
    }

    @Override
    public boolean areLogsEnabled() {
        return mLogsEnabled;
    }

    @Override
    public boolean deactivateLogs() {
        if (mLogsEnabled && mDeactivateLogsSupported) {
            return mBackend.deactivateLogs();
        } else {
            return false;
        }
    }

    /**
     * Updates current logs state.
     *
     * @param enabled {@code true} if the logs are enabled
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public LogControlCore updateLogsState(boolean enabled) {
        if (mLogsEnabled != enabled) {
            mLogsEnabled = enabled;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates whether logs can be disabled.
     *
     * @param supported {@code true} if the logs can be disabled
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public LogControlCore updateDeactivateLogsSupported(boolean supported) {
        if (mDeactivateLogsSupported != supported) {
            mDeactivateLogsSupported = supported;
            mChanged = true;
        }
        return this;
    }
}
