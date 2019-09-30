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

package com.parrot.drone.groundsdk.arsdkengine.blackbox;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.Event;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.RemoteControlInfo;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_BLACKBOX;

/**
 * Remote control black box recording session.
 */
public final class BlackBoxRcSession extends BlackBoxSession {

    /** Remote control info being recorded. */
    final RemoteControlInfo mRcInfo;

    /**
     * Constructor.
     *
     * @param context  context that manages the session
     * @param rc       remote control to record info from
     * @param listener listener to notify when the session closes
     */
    BlackBoxRcSession(@NonNull BlackBoxRecorder.Context context, @NonNull RemoteControlCore rc,
                      @NonNull CloseListener listener) {
        super(context, listener);
        mRcInfo = new RemoteControlInfo(rc);
        if (ULog.d(TAG_BLACKBOX)) {
            ULog.d(TAG_BLACKBOX, "Opened new RC blackbox session [rc: " + rc.getUid()
                                 + ", session: " + System.identityHashCode(this)
                                 + ", context: " + System.identityHashCode(context) + "]");
        }
    }

    /**
     * Dispatches a button action event to the session for processing.
     *
     * @param action button action code
     */
    public void onButtonAction(int action) {
        mContext.addEvent(Event.rcButtonAction(action));
    }

    /**
     * Dispatches piloting command info to the session for processing.
     *
     * @param roll   piloting command roll
     * @param pitch  piloting command pitch
     * @param yaw    piloting command yaw
     * @param gaz    piloting command gaz
     * @param source piloting command source
     */
    public void onPilotingInfo(int roll, int pitch, int yaw, int gaz, int source) {
        mContext.mEnvironmentInfo.setRemotePilotingCommand(roll, pitch, yaw, gaz, source);
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureSkyctrl.SettingsState.UID) {
            ArsdkFeatureSkyctrl.SettingsState.decode(command, mSettingsStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureSkyctrl.SettingsState is decoded. */
    private final ArsdkFeatureSkyctrl.SettingsState.Callback mSettingsStateCallback =
            new ArsdkFeatureSkyctrl.SettingsState.Callback() {

                @Override
                public void onProductVersionChanged(String software, String hardware) {
                    mRcInfo.setVersion(software, hardware);
                }
            };
}
