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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.TimeProvider;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Black box header info.
 * <p>
 * Contains information such as the black box version, operating system version, operating device model, black box
 * recording date, black box monotonic timestamps base, drone uid, drone model, drone hardware, software and gps
 * versions, user academy identifier, remote control info.
 */
public final class HeaderInfo {

    /** Date time formatter, one instance per thread since SimpleDateFormat is not thread safe. */
    private static final ThreadLocal<Format> DATE_FORMATTER = new ThreadLocal<Format>() {

        @Override
        protected Format initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
        }
    };

    /** Black box version. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("blackbox_version")
    private static final String BLACKBOX_VERSION = "1.0.6";

    /** Android operating system version. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("device_os")
    private static final String OS_VERSION = "Android " + Build.VERSION.RELEASE;

    /** Android operating device model. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("device_model")
    private static final String OS_MODEL = Build.MODEL;

    /** Black box record date. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("date")
    @NonNull
    private final String mDate;

    /** Monotonic timestamps base. TODO : this field is new, check it is ok. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("timestamp_base")
    private final long mTimestampBase;

    /** Drone uid. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_serial")
    @NonNull
    private final String mUid;

    /** Drone model. TODO: this should be an int unique model id. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_id")
    @NonNull
    private final String mModel;

    /** Drone hardware version. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_fw_hard")
    @Nullable
    private String mHardwareVersion;

    /** Drone software version. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_fw_soft")
    @Nullable
    private String mSoftwareVersion;

    /** User academy id. TODO: defined here but needs academy integration. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("academy_id")
    @Nullable
    private String mAcademyId;

    /** Drone motor version. TODO: the providing arsdk callback is deprecated. Should we inject this field? */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_motor_version")
    @Nullable
    private String mMotorVersion;

    /** Drone GPS version. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("product_gps_version")
    @Nullable
    private String mGpsVersion;

    /** Drone boot id. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("boot_id")
    @Nullable
    private String mBootId;

    /** Remote controller info. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("remote_controller")
    @Nullable
    private RemoteControlInfo mRcInfo;

    /**
     * Constructor.
     *
     * @param drone drone that this black box header is recorded for
     */
    public HeaderInfo(@NonNull DroneCore drone) {
        //noinspection ConstantConditions: DATE_FORMATTER has a default initial value
        mDate = DATE_FORMATTER.get().format(new Date());
        mTimestampBase = TimeProvider.elapsedRealtime();
        mUid = drone.getUid();
        mModel = Integer.toString(drone.getModel().id());
    }

    /**
     * Updates drone version info.
     *
     * @param softwareVersion drone software version
     * @param hardwareVersion drone hardware version
     */
    public void setVersion(@NonNull String softwareVersion, @NonNull String hardwareVersion) {
        mSoftwareVersion = softwareVersion;
        mHardwareVersion = hardwareVersion;
    }

    /**
     * Updates drone motor version.
     *
     * @param motorVersion drone motor version
     */
    public void setMotorVersion(@NonNull String motorVersion) {
        mMotorVersion = motorVersion;
    }

    /**
     * Updates drone GPS version.
     *
     * @param gpsVersion drone GPS version
     */
    public void setGpsVersion(@NonNull String gpsVersion) {
        mGpsVersion = gpsVersion;
    }

    /**
     * Updates drone boot id.
     *
     * @param bootId drone boot id
     */
    public void setBootId(@NonNull String bootId) {
        mBootId = bootId;
    }

    /**
     * Updates remote controller information.
     *
     * @param info remote controller info
     */
    public void setRcInfo(@NonNull RemoteControlInfo info) {
        mRcInfo = info;
    }
}
