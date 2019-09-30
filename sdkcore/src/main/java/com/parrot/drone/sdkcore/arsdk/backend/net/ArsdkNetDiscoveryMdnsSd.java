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

package com.parrot.drone.sdkcore.arsdk.backend.net;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.backend.net.mdnssdmin.MdnsSdMin;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG;

/**
 * Mdns-sd-min based discovery.
 */
final class ArsdkNetDiscoveryMdnsSd extends ArsdkNetDiscovery {

    /** device format. */
    private static final String FORMAT = "_arsdk-%04x._udp.local.";

    /** arsdk instance owning this discovery. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Mdns-sd-min instance. */
    @NonNull
    private final MdnsSdMin mMdnsSdMin;

    /** Service string to to type mapping. */
    @NonNull
    private final Map<String, Integer> mServices;

    /**
     * Constructor.
     *
     * @param arsdkCore arsdk ctrl instance owning this discovery
     * @param backend   discovery backend
     * @param types     requested device type
     * @param netConfig network configuration
     */
    ArsdkNetDiscoveryMdnsSd(@NonNull ArsdkCore arsdkCore, @NonNull ArsdkNetBackend backend,
                            @ArsdkDevice.Type int types[], @NonNull MdnsSdMin.NetConfig netConfig) {
        super(arsdkCore, backend, "mdns-sd");
        mArsdkCore = arsdkCore;
        mServices = new HashMap<>(types.length);
        for (@ArsdkDevice.Type int type : types) {
            mServices.put(String.format(FORMAT, type), type);

        }
        mMdnsSdMin = new MdnsSdMin(mServices.keySet().toArray(new String[0]),
                mMdnsSdMinListener, netConfig);
    }

    @Override
    protected void onStart() {
        mMdnsSdMin.start();
    }

    @Override
    protected void onStop() {
        mMdnsSdMin.stop();
    }

    @ArsdkDevice.Type
    private int getTypeFromService(String serviceType) {
        Integer typeId = mServices.get(serviceType);
        return typeId == null ? ArsdkDevice.TYPE_UNKNOWN : typeId;
    }

    private static String getSerialFromTxtRecords(@NonNull String[] txtRecords) {
        if (txtRecords.length > 0) {
            try {
                JSONObject json = new JSONObject(txtRecords[0]);
                return json.getString("device_id");
            } catch (JSONException e) {
                ULog.w(TAG, "Error decoding txt record", e);
            }
        }
        return "";
    }

    /**
     * Mdns-sd-mon listener.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MdnsSdMin.Listener mMdnsSdMinListener = new MdnsSdMin.Listener() {

        @Override
        public void onServiceAdded(@NonNull String name, @NonNull String serviceType,
                                   @NonNull String ipAddress, int port,
                                   @NonNull String[] txtRecords) {
            @ArsdkDevice.Type int type = getTypeFromService(serviceType);
            if (type != ArsdkDevice.TYPE_UNKNOWN) {
                mArsdkCore.dispatchToPomp(() -> addDevice(
                        name, type, ipAddress, port, getSerialFromTxtRecords(txtRecords)));
                mMdnsSdMin.cancelSendQueries();
            }
        }

        @Override
        public void onServiceRemoved(@NonNull String name, @NonNull String serviceType) {
            @ArsdkDevice.Type int type = getTypeFromService(serviceType);
            mArsdkCore.dispatchToPomp(() -> {
                if (type != ArsdkDevice.TYPE_UNKNOWN) {
                    removeDevice(name, type);
                }
            });
        }
    };
}
