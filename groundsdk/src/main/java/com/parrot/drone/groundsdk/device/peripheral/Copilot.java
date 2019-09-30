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
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.value.EnumSetting;

/**
 * Copilot peripheral interface for RemoteControl devices.
 * <p>
 * Copilot allows to select the source of piloting commands, either the remote control (default) or the application.
 * Selecting a source prevents the other one from sending any piloting command.<br>
 * The piloting source is automatically reset to {@link Source#REMOTE_CONTROL remote control} when this one is
 * disconnected from the phone.
 * <p>
 * This peripheral can be obtained from a {@link RemoteControl remote control} using:
 * <pre>{@code remoteControl.getPeripheral(Copilot.class)}</pre>
 *
 * @see RemoteControl#getPeripheral(Class)
 * @see RemoteControl#getPeripheral(Class, Ref.Observer)
 */
public interface Copilot extends Peripheral {

    /**
     * Piloting source.
     */
    enum Source {

        /** Remote control joysticks are used to pilot the drone. */
        REMOTE_CONTROL,

        /** Application controls are used to pilot the drone. */
        APPLICATION
    }

    /**
     * Gives access to the piloting source setting.
     *
     * @return piloting source setting
     */
    @NonNull
    EnumSetting<Source> source();
}
