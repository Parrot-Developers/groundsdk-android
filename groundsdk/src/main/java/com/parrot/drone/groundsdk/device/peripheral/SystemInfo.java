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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;

/**
 * SystemInfo peripheral interface for Drone and RemoteControl devices.
 * <p>
 * Allows to: <ul>
 * <li>Retrieve info about the device, such as firmware and hardware version, serial number, CPU identifier. </li>
 * <li> Perform system action on the device, such as factory reset, and resetting settings to their built-in
 * defaults.</li>
 * </ul>
 * This peripheral can be obtained from a {@link Peripheral.Provider peripheral providing device} (such as a drone or a
 * remote control) using:
 * <pre>{@code device.getPeripheral(SystemInfo.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface SystemInfo extends Peripheral {

    /**
     * Gets the device firmware version.
     *
     * @return device firmware version
     */
    @NonNull
    String getFirmwareVersion();

    /**
     * Tells whether the device firmware version is blacklisted.
     *
     * @return {@code true} if the device firmware version is blacklisted, {@code false} otherwise.
     */
    boolean isFirmwareBlacklisted();

    /**
     * Tells whether an update is required.
     *
     * @return {@code true} if an update is required, otherwise {@code false}.
     */
    boolean isUpdateRequired();

    /**
     * Gets the device hardware version.
     *
     * @return a string identifying the device hardware version
     */
    @NonNull
    String getHardwareVersion();

    /**
     * Gets the device serial number.
     * <p>
     * This identifier is unique over all devices of the same type and should usually be persistent. <br>
     * Note however that it may be changed by altering the factory partition of the device.
     *
     * @return the device serial number string
     */
    @NonNull
    String getSerialNumber();

    /**
     * Gets the identifier of the main CPU on the device.
     * <p>
     * This identifier provides a more secured identification method than the {@link #getSerialNumber() serial number}.
     *
     * @return device CPU identifier string
     */
    @NonNull
    String getCpuIdentifier();

    /**
     * Gets the device board identifier.
     *
     * @return device board identifier string
     */
    @NonNull
    String getBoardIdentifier();

    /**
     * Tells whether a factory reset is currently occurring on the device.
     *
     * @return {@code true} if a factory reset is in progress, otherwise {@code false}
     */
    boolean isFactoryResetInProgress();

    /**
     * Commands the device to perform a factory reset.
     * <p>
     * This will trigger a reboot of the device.
     *
     * @return {@code true} if the factory reset process could be initiated, otherwise {@code false}
     */
    boolean factoryReset();

    /**
     * Tells whether a settings reset operation is currently occurring on the device.
     *
     * @return {@code true} if a settings reset operation is in progress, otherwise {@code false}
     */
    boolean isResetSettingsInProgress();

    /**
     * Commands the device to reset its settings to their built-in defaults.
     *
     * @return {@code true} if the reset operation could be initiated, otherwise {@code false}
     */
    boolean resetSettings();
}
