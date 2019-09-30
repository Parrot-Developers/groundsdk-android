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

package com.parrot.drone.groundsdk.internal;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Component that forwards intents to the application.
 */
public abstract class ApplicationNotifier {

    /** Singleton instance. */
    private static ApplicationNotifier sInstance;

    /**
     * Sets the default implementation as the singleton instance of ApplicationNotifier.
     * <p>
     * The default implementation is backed by android's {@link LocalBroadcastManager} which allows to broadcast
     * intents local to the application process.
     * <p>
     * This method is called when GroundSdkCore singleton instance is created.
     *
     * @param appContext android application context.
     */
    static void setDefault(@NonNull Context appContext) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(appContext);
        synchronized (ApplicationNotifier.class) {
            sInstance = new ApplicationNotifier() {

                @Override
                public void broadcastIntent(@NonNull Intent intent) {
                    broadcastManager.sendBroadcast(intent);
                }
            };
        }
    }

    /**
     * Forces the provided ApplicationNotifier instance to be the singleton ApplicationNotifier instance.
     * <p>
     * This method is used by tests to bypass the broadcast manager and receive intents directly.
     *
     * @param instance the application notifier instance to set
     */
    @VisibleForTesting
    public static void setInstance(@Nullable ApplicationNotifier instance) {
        synchronized (ApplicationNotifier.class) {
            sInstance = instance;
        }
    }

    /**
     * Gets the ApplicationNotifier singleton.
     *
     * @return the ApplicationNotifier
     *
     * @throws IllegalStateException in case the application notifier instance has not been set
     */
    @NonNull
    public static ApplicationNotifier getInstance() {
        synchronized (ApplicationNotifier.class) {
            if (sInstance == null) {
                throw new IllegalStateException("No ApplicationNotifier instance");
            }
            return sInstance;
        }
    }

    /**
     * Broadcasts the provided intent to the application.
     *
     * @param intent intent to broadcast
     */
    public abstract void broadcastIntent(@NonNull Intent intent);
}
