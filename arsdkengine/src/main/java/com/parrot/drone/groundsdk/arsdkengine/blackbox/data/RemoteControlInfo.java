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

package com.parrot.drone.groundsdk.arsdkengine.blackbox.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;

/**
 * Remote controller information.
 * <p>
 * Contains information such as current remote controller uid, model, software and hardware versions.
 */
public class RemoteControlInfo {

    /** Remote control uid. */
    @Expose
    @SerializedName("PI")
    @NonNull
    private final String mUid;

    /**
     * Remote control model.
     */
    @Expose
    @SerializedName("Model")
    @NonNull
    /*
     * TODO : in current blackboxes, this is a 'display name' of the model, such as 'SkyController 2'.
     * TODO   Here, we inject the model unique id as a string; this field should be an int unique model id.
     * TODO   Also, why is the first letter capitalized?
     */
    private final String mModel;

    /** Remote control software version. */
    @SuppressWarnings("unused") // read when serializing to json
    @Expose
    @SerializedName("software_version")
    @Nullable
    private String mSoftwareVersion;

    /** Remote control hardware version. */
    @SuppressWarnings("unused") // read when serializing to json
    @Expose
    @SerializedName("hardware_version")
    @Nullable
    private String mHardwareVersion;

    /**
     * Constructor.
     *
     * @param rc remote control to record info from
     */
    public RemoteControlInfo(@NonNull RemoteControlCore rc) {
        mUid = rc.getUid();
        mModel = Integer.toString(rc.getModel().id());
    }

    /**
     * Copy constructor.
     *
     * @param other remote controller info to copy data from
     */
    public RemoteControlInfo(@NonNull RemoteControlInfo other) {
        mUid = other.mUid;
        mModel = other.mModel;
        mSoftwareVersion = other.mSoftwareVersion;
        mHardwareVersion = other.mHardwareVersion;
    }

    /**
     * Updates remote controller version info.
     *
     * @param softwareVersion remote control software version
     * @param hardwareVersion remote control hardware version
     */
    public void setVersion(@NonNull String softwareVersion, @NonNull String hardwareVersion) {
        mSoftwareVersion = softwareVersion;
        mHardwareVersion = hardwareVersion;
    }
}
