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

package com.parrot.drone.groundsdk.device;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;

/**
 * A generic remote control. This is the base class to manage a specific remote control.
 */
public abstract class RemoteControl implements Instrument.Provider, Peripheral.Provider {

    /**
     * Remote Control model.
     */
    @SuppressWarnings("NullableProblems") // name() in enum is virtually @NonNull
    public enum Model implements DeviceModel {

        /** Sky Controller 3 remote control. */
        SKY_CONTROLLER_3(0x0918),

        /** Sky Controller UA remote control. */
        SKY_CONTROLLER_UA(0x091c);

        /** Model id. */
        @Id
        private final int mId;

        /**
         * Constructor.
         *
         * @param id model unique identifier
         */
        Model(int id) {
            mId = id;
        }

        @Override
        public int id() {
            return mId;
        }
    }

    /**
     * Gets the remote control uid.
     * <p>
     * Remote control uid uniquely identifies a remote control, and is persistent between sessions.
     *
     * @return remote control uid
     */
    @NonNull
    public abstract String getUid();

    /**
     * Gets the remote control model.
     * <p>
     * Model is set when the remote control instance is created and never changes.
     *
     * @return remote control model
     */
    @NonNull
    public abstract Model getModel();

    /**
     * Gets remote control current name.
     * <p>
     * Remote control name can be changed. This method returns the name at the current time. Use
     * {@link #getName(Ref.Observer)} to get the current name and be notified when the name changes.
     *
     * @return remote control current name
     */
    @NonNull
    public abstract String getName();

    /**
     * Gets the remote control name and registers an observer notified each time it changes.
     * <p>
     * If the remote control is removed, the observer will be notified and the name returned by {@link Ref#get()} will
     * be {@code null}.
     *
     * @param observer observer to notify when the remote control name changes
     *
     * @return reference to remote control name
     *
     * @see #getName() to get current name without registring an observer
     */
    @NonNull
    public abstract Ref<String> getName(@NonNull Ref.Observer<String> observer);

    /**
     * Gets the remote control current state.
     * <p>
     * This method returns the current state without indicating when it changes. Use {@link #getState(Ref.Observer)} to
     * get the current state and be notified when the state changes.
     *
     * @return current remote control state
     */
    @NonNull
    public abstract DeviceState getState();

    /**
     * Gets the remote control state and registers an observer notified each time it changes.
     * <p>
     * If the remote control is removed, the observer will be notified and the state returned by {@link Ref#get()} will
     * be {@code null}.
     *
     * @param observer observer to notify when the remote control state changes
     *
     * @return reference to remote control state
     *
     * @see #getState() to get current state without registring an observer
     */
    @NonNull
    public abstract Ref<DeviceState> getState(@NonNull Ref.Observer<DeviceState> observer);

    /**
     * Forgets the remote control.
     * <p>
     * Persisted remote control data are deleted and the remote control is removed if it's not currently visible.
     *
     * @return {@code true} if the remote control has been forgotten, otherwise {@code false}
     */
    public abstract boolean forget();

    /**
     * Connects the remote control.
     * <p>
     * Chooses the best available connector, as follows: <ul>
     * <li>If there is only one available connector, then it is used to connect the remote control. Otherwise,</li>
     * <li>if there is only one available {@link DeviceConnector.Technology#USB USB} connector (note that there can
     * be two of those in case both the device USB and the USB Debug Bridge are used), then it is used to connect
     * the remote control.</li>
     * </ul>
     * If none of the aforementioned condition holds, then the connection fails and {@code false} is returned. <br>
     * {@link #connect(DeviceConnector)} method shall be used to select an appropriate connector instead.
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the remote
     *         control is not visible anymore
     */
    public abstract boolean connect();

    /**
     * Connects the remote control using the specified device connector.
     *
     * @param connector connector that should be used
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the remote
     *         control is not visible anymore
     */
    public abstract boolean connect(@NonNull DeviceConnector connector);

    /**
     * Connects the remote control.
     *
     * @param connector connector that should be used
     * @param password  password to use for authentication
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the remote
     *         control is not visible anymore
     */
    public abstract boolean connect(@NonNull DeviceConnector connector, @NonNull String password);

    /**
     * Disconnects the remote control.
     * <p>
     * This method can be used to disconnect the remote control when connected or to cancel the connection process
     * if the remote control is currently connecting.
     *
     * @return {@code true} if the disconnection process has started, otherwise {@code false}
     */
    public abstract boolean disconnect();
}
