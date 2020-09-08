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

package com.parrot.drone.groundsdk.device.peripheral;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;

/**
 * LogControl peripheral interface.
 *
 * This peripheral allows to deactivate logs on the drone
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(LogControl.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface LogControl extends Peripheral {

    /**
     * Tells if the logs can be disabled.
     *
     * @return {@code true} if log deactivation is supported, {@code false} otherwise
     */
    boolean canDeactivateLogs();

    /**
     * Whether the logs are enabled.
     *
     * @return {@code true} if logs are enabled, {@code false} otherwise
     */
    boolean areLogsEnabled();

    /**
     * Deactivate logs on drone.
     * <p>
     * <strong>Note:</strong> the logs stay disabled for the session, and will be enabled again at the next restart.
     * This method has no effect if {@link #canDeactivateLogs()} is {@code false}.
     *
     * @return {@code true} if the command has been successfully sent to the drone, {@code false} otherwise
     */
    boolean deactivateLogs();
}
