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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.ulog.ULog;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_ENGINE;
import static com.parrot.drone.groundsdk.internal.engine.EngineBase.META_KEY;

/**
 * Provides load machinery for external engines that are declared in the application manifest.
 */
final class ExternalEngines {

    /**
     * Load all declared external engines.
     *
     * @param controller engine controller providing access to GroundSdk utilities
     *
     * @return a set collecting all dynamically created external engines
     */
    @NonNull
    static Set<EngineBase> load(@NonNull EngineBase.Controller controller) {
        Set<EngineBase> engines = new HashSet<>();

        Bundle metadata = null;
        try {
            metadata = controller.mContext.getPackageManager().getApplicationInfo(
                    controller.mContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            ULog.e(TAG_ENGINE, "Error getting out package info", e);
        }

        if (metadata != null) {
            for (String key : metadata.keySet()) {
                if (key.startsWith(META_KEY)) {
                    try {
                        //noinspection ConstantConditions: metadata value null catched by Exception below
                        Class<? extends EngineBase> engineClass =
                                Class.forName(metadata.getString(key)).asSubclass(EngineBase.class);
                        Constructor<? extends EngineBase> constructor =
                                engineClass.getConstructor(EngineBase.Controller.class);
                        if (ULog.i(TAG_ENGINE)) {
                            ULog.i(TAG_ENGINE, "Loading engine " + key.substring(META_KEY.length())
                                               + " [" + engineClass.getSimpleName() + "]");
                        }
                        EngineBase engine = constructor.newInstance(controller);
                        engines.add(engine);
                    } catch (Exception e) {
                        ULog.e(TAG_ENGINE, "Exception loading engine", e);
                    }
                }
            }
        }

        return engines;
    }

    /**
     * Private constructor for static utility class.
     */
    private ExternalEngines() {
    }
}
