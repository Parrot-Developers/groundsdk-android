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

package com.parrot.drone.groundsdk.internal.engine.activation;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the persistence of list of registered devices.
 */
class Persistence {

    /** Activation shared preferences file name. */
    @VisibleForTesting
    static final String PREF_FILE = "activation";

    /** Key for accessing list of registered devices. */
    private static final String PREF_KEY_DEVICES = "devices";

    /** Shared preference where firmware meta data are stored. */
    @NonNull
    private final SharedPreferences mPrefs;

    /**
     * Constructor.
     *
     * @param context android application context
     */
    Persistence(@NonNull Context context) {
        mPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Loads registered devices.
     *
     * @return registered devices
     */
    @NonNull
    Set<String> loadRegisteredDevices() {
        Set<String> devices = mPrefs.getStringSet(PREF_KEY_DEVICES, null);
        return devices == null ? new HashSet<>() : devices;
    }

    /**
     * Persists registered devices.
     *
     * @param devices registered devices to persist
     */
    void saveRegisteredDevices(@NonNull Set<String> devices) {
        mPrefs.edit().putStringSet(PREF_KEY_DEVICES, devices).apply();
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--activation: dumps registered devices\n");
        } else if (args.contains("--activation") || args.contains("--all")) {
            Set<String> devices = loadRegisteredDevices();
            writer.write("Registered devices: " + devices.size() + "\n");
            for (String device : devices) {
                writer.write("\t" + device + "\n");
            }
        }
    }
}
