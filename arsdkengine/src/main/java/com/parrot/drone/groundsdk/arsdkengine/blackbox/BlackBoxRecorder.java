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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.R;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.EnvironmentData;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.Event;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.FlightData;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.RemoteControlInfo;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.HashMap;
import java.util.Map;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_BLACKBOX;

/**
 * Black box recorder, allowing device controllers to create sessions to record black box data.
 */
public class BlackBoxRecorder {

    /** Blackbox storage utility to which blackbox data is forwarded when ready. */
    @NonNull
    private final BlackBoxStorage mStorage;

    /** Live black box contexts, by master device uid. */
    @NonNull
    private final Map<String, Context> mContexts;

    /** System location provider, {@code null} if not available. */
    @Nullable
    private final SystemLocation mSystemLocation;

    /** Blackbox circular buffers capacity, in seconds. */
    private final int mBlackBoxBufferCapacity;

    /**
     * Constructor.
     *
     * @param engine  arsdk engine
     * @param storage black box storage utility
     */
    public BlackBoxRecorder(@NonNull ArsdkEngine engine, @NonNull BlackBoxStorage storage) {
        mSystemLocation = engine.getUtility(SystemLocation.class);
        mBlackBoxBufferCapacity = engine.getContext().getResources().getInteger(R.integer.blackbox_buffer_capacity);
        mStorage = storage;
        mContexts = new HashMap<>();
    }

    /**
     * Opens a session to record drone black box data.
     * <p>
     * Depending on the current state, it may be impossible to open a session, in which case, this method returns {@code
     * null} and the provided close listener is not registered.
     * <p>
     * In case an appropriate session is already open, then it is returned and the provided close listener is not
     * registered.
     *
     * @param drone       drone to record black box data from
     * @param providerUid uid of the active device provider for that drone, if any (otherwise {@code null})
     * @param listener    listener notified when the session closes
     *
     * @return an open drone black box recording session, or {@code null} if it was not possible to open a session
     */
    @Nullable
    public BlackBoxDroneSession openDroneSession(@NonNull DroneCore drone, @Nullable String providerUid,
                                                 @NonNull BlackBoxSession.CloseListener listener) {
        return obtainContext(drone.getUid(), providerUid).openDroneSession(drone, listener);
    }

    /**
     * Opens a session to record remote control black box data.
     * <p>
     * Depending on the current state, it may be impossible to open a session, in which case, this method returns {@code
     * null} and the provided close listener is not registered.
     * <p>
     * In case an appropriate session is already open, then it is returned and the provided close listener is not
     * registered.
     *
     * @param rc       remote control to record black box data from
     * @param listener listener notified when the session closes
     *
     * @return an open remote control black box recording session, or {@code null} if it was not possible to open a
     *         session
     */
    @Nullable
    public BlackBoxRcSession openRemoteControlSession(@NonNull RemoteControlCore rc,
                                                      @NonNull BlackBoxSession.CloseListener listener) {
        return obtainContext(rc.getUid(), null).openRcSession(rc, listener);
    }

    /**
     * Obtains an appropriate recording context for the given device.
     * <p>
     * In case the {@code providerUid} is not {@code null}, then it is used in priority to reference or create the
     * recording context for the device.
     *
     * @param deviceUid   device to provide a context for
     * @param providerUid active provider of the device to provide a context for, {@code null} if none
     *
     * @return an existing or new context for the given device to record black box data within
     */
    @NonNull
    private Context obtainContext(@NonNull String deviceUid, @Nullable String providerUid) {
        String uid = providerUid == null ? deviceUid : providerUid;
        Context context = mContexts.get(uid);
        if (context == null) {
            context = new Context();
            mContexts.put(uid, context);
        }
        return context;
    }

    /**
     * A black box recording context, which aggregates both a drone and a remote control recording session.
     */
    final class Context {

        /** Latest {@link FlightData} info. */
        @NonNull
        final FlightData.Builder mFlightInfo;

        /** Latest {@link EnvironmentData} info. */
        @NonNull
        final EnvironmentData.Builder mEnvironmentInfo;

        /** Open drone session, {@code null} if none. */
        @Nullable
        private BlackBoxDroneSession mDroneSession;

        /** Open rc session, {@code null} if none. */
        @Nullable
        private BlackBoxRcSession mRcSession;

        /**
         * Constructor.
         */
        Context() {
            mFlightInfo = new FlightData.Builder();
            mEnvironmentInfo = new EnvironmentData.Builder();
            if (mSystemLocation != null) {
                mSystemLocation.monitorWith(mControllerLocationMonitor);
            }
        }

        /**
         * Adds an event to the black box being recorded.
         * <p>
         * The event is only added if a drone session is opened, which contains the actual black box.
         *
         * @param event event to add
         *
         * @return {@code this}, to allow call chaining
         */
        @NonNull
        Context addEvent(@NonNull Event event) {
            if (mDroneSession != null) {
                mDroneSession.mBlackBox.addEvent(event);
            }
            return this;
        }

        /**
         * Opens a drone session.
         *
         * @param drone    drone to open a session for
         * @param listener session close listener
         *
         * @return a new recording session for the drone, or the currently open session if exists.
         */
        @NonNull
        BlackBoxDroneSession openDroneSession(@NonNull DroneCore drone,
                                              @NonNull BlackBoxSession.CloseListener listener) {
            if (mDroneSession == null) {
                mDroneSession = new BlackBoxDroneSession(this, mBlackBoxBufferCapacity, drone, () -> {
                    if (mRcSession == null) {
                        closeSelf();
                    } else {
                        // inject a copy of rc info in blackbox
                        mDroneSession.mBlackBox.mHeader.setRcInfo(new RemoteControlInfo(mRcSession.mRcInfo));
                    }
                    // forward finalized blackbox
                    if (ULog.d(TAG_BLACKBOX)) {
                        ULog.d(TAG_BLACKBOX, "Finalized blackbox: "
                                             + System.identityHashCode(mDroneSession.mBlackBox));
                    }
                    mStorage.notifyBlackBoxReady(mDroneSession.mBlackBox);
                    mDroneSession = null;
                    listener.onBlackBoxSessionClosed();
                });
            }
            return mDroneSession;
        }

        /**
         * Opens a remote control session.
         *
         * @param rc       remote control to open a session for
         * @param listener session close listener
         *
         * @return a new recording session for the remote control, or the currently open session if exists.
         */
        @NonNull
        BlackBoxRcSession openRcSession(@NonNull RemoteControlCore rc,
                                        @NonNull BlackBoxSession.CloseListener listener) {
            if (mRcSession == null) {
                mRcSession = new BlackBoxRcSession(this, rc, () -> {
                    if (mDroneSession == null) {
                        closeSelf();
                    } else {
                        // move rc info to drone blackbox. No need to copy, as RC session is disposed
                        mDroneSession.mBlackBox.mHeader.setRcInfo(mRcSession.mRcInfo);
                    }
                    mRcSession = null;
                    listener.onBlackBoxSessionClosed();
                });
            }
            return mRcSession;
        }

        /**
         * Closes the context.
         * <p>
         * The context is closed when both drone and rc session are closed.
         */
        private void closeSelf() {
            if (mSystemLocation != null) {
                mSystemLocation.disposeMonitor(mControllerLocationMonitor);
            }
            mContexts.values().remove(this);
        }

        /** Monitors system location to inject last known location into recorded black box. */
        private final SystemLocation.Monitor mControllerLocationMonitor = new SystemLocation.Monitor() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                mEnvironmentInfo.setControllerLocation(location);
            }
        };
    }
}
