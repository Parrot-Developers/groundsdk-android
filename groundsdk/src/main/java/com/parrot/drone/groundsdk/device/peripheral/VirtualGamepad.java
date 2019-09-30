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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;

/**
 * Virtual Gamepad peripheral interface for {@link RemoteControl} devices.
 * <p>
 * Allows to receive navigation events when some predefined inputs on the device are triggered.
 * <p>
 * The mapping between physical inputs (buttons, axes, etc.) on the device and received navigation events is specific
 * to the remote control in use: please refer to the remote control documentation for further information.
 * <p>
 * This peripheral is provided by all remote control devices, unless explicitly stated otherwise in the specific
 * remote control documentation.
 * <p>
 * To start receiving navigation events, the virtual gamepad peripheral must be grabbed and a listener (that will
 * receive all events) must be provided. <br>
 * When the virtual gamepad is grabbed, the remote control will stop forwarding events associated to its navigation
 * inputs to the connected drone (if any) and instead forward those events to the application-provided listener.
 * <p>
 * To stop receiving events and having the device forward navigation input events back to the connected drone, the
 * virtual gamepad must be released.
 * <p>
 * Most remote control devices also provide a more specialized gamepad interface (please refer to the remote control
 * documentation for further information), which usually allows to listen to finer-grained remote control input events.
 * <br>
 * However, when inputs are grabbed using such a specialized interface, the virtual gamepad cannot be used anymore.
 * <br>
 * In case it is also currently grabbed, it will switch to a {@link VirtualGamepad.State#PREEMPTED preempted} state
 * where navigation events won't be forwarded to the provided listener anymore, until all inputs on the specialized
 * gamepad interface are released. At that point, the virtual gamepad will grab the navigation inputs again and resume
 * forwarding events to the application listener.
 * <p>
 * The application can also subscribe to gamepad application events that are forwarded through a
 * {@link LocalBroadcastManager local broadcast} {@link Intent intent} when a combination of some physical inputs is
 * triggered on the remote control. <br>
 * Those events are the {@code APP_ACTION_*} values defined in {@link ButtonsMappableAction} enum. <br>
 * How those events are mapped to physical inputs and how those mappings can be configured is specific to the remote
 * control device in use, please refer to the remote control documentation for further information.
 * <p>
 * This peripheral can be obtained from a {@code RemoteControl} using:
 * <pre>{@code remoteControl.getPeripheral(VirtualGamepad.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface VirtualGamepad extends Peripheral {

    /**
     * Intent action sent in a local broadcast when a gamepad application event has been triggered from the remote
     * control device.
     * <p>
     * The broadcast intent also contains the extra {@link #EXTRA_GAMEPAD_APP_EVENT_ACTION}, which identifies the
     * application event that was triggered.
     */
    String ACTION_GAMEPAD_APP_EVENT = "com.parrot.drone.groundsdk.device.peripheral.ACTION_GAMEPAD_APP_EVENT";

    /**
     * Intent extra sent along {@link #ACTION_GAMEPAD_APP_EVENT} intent when a gamepad application event has been
     * triggered.
     * <p>
     * Identifies which application event was triggered and is an int {@link ButtonsMappableAction#ordinal() ordinal}
     * of one of the {@code APP_ACTION_*} in {@link ButtonsMappableAction}.
     */
    String EXTRA_GAMEPAD_APP_EVENT_ACTION =
            "com.parrot.drone.groundsdk.device.peripheral.EXTRA_GAMEPAD_APP_EVENT_ACTION";

    /**
     * A navigation event sent when the appropriate remote control input is triggered.
     */
    enum Event {

        /** Input used to validate an action was triggered. */
        OK,

        /** Input used to cancel an action was triggered. */
        CANCEL,

        /** Input used to navigate left was triggered. */
        LEFT,

        /** Input used to navigate right was triggered. */
        RIGHT,

        /** Input used to navigate up was triggered. */
        UP,

        /** Input used to navigate down was triggered. */
        DOWN;

        /** State of the input associated to the event that was sent. */
        public enum State {

            /** Input was pressed. */
            PRESSED,

            /** Input was released. */
            RELEASED
        }

        /**
         * Receives navigation events sent from the remote control when the virtual gamepad peripheral is grabbed.
         */
        public interface Listener {

            /**
             * Called back when a navigation event is received.
             *
             * @param event received navigation event
             * @param state corresponding input state
             */
            void onEvent(@NonNull Event event, @NonNull State state);
        }
    }

    /**
     * State of the virtual gamepad peripheral.
     */
    enum State {

        /**
         * Virtual gamepad is released.
         * <p>
         * Navigation events are forwarded to the connected drone (unless a more specialized gamepad interface is
         * currently grabbing navigation inputs).
         * <p>
         * In this state, gamepad can be {@link #grab grabbed} to start forwarding events to the application instead
         * of the drone, unless a more specialized gamepad interface is currently grabbing any input (see
         * {@link #canGrab()}).
         */
        RELEASED,

        /**
         * Virtual gamepad is grabbed.
         * <p>
         * Navigation events are forwarded to the application-provided listener instead of the connected drone.
         * <p>
         * In this state, gamepad can be {@link #release() released} to stop forwarding events to the listener and
         * resume forwarding events to the drone.
         */
        GRABBED,

        /**
         * Virtual Gamepad is preempted.
         * <p>
         * Navigation events cannot be forwarded to the application-provided listener since a more specialized gamepad
         * interface is currently grabbing some inputs.
         * <p>
         * Once all grabbed inputs are released on the specialized gamepad interface, the virtual gamepad with switch
         * back to the {@link #GRABBED} state and resume forwarding events to the application listener.
         */
        PREEMPTED
    }

    /**
     * Gets the current virtual gamepad state.
     *
     * @return virtual gamepad current state
     */
    @NonNull
    State getState();

    /**
     * Tells whether the virtual gamepad can be grabbed at the moment.
     * <p>
     * Virtual gamepad can be grabbed <strong>only if</strong> in the {@link State#RELEASED released} state,
     * <strong>and unless</strong> a more specialized gamepad interface is currently grabbing any input.
     *
     * @return {@code true} if the virtual gamepad can be grabbed, otherwise {@code false}
     */
    boolean canGrab();

    /**
     * Grabs the remote control navigation inputs.
     * <p>
     * Stops forwarding navigation events to the connected drone (if any), and starts forwarding them to the
     * provided listener instead.
     *
     * @param listener application listener to forward navigation events to
     *
     * @return {@code true} if navigation inputs could be grabbed, otherwise {@code false}
     *
     * @see #canGrab()
     * @see #release()
     */
    boolean grab(@NonNull Event.Listener listener);

    /**
     * Release the remote control navigation inputs.
     * <p>
     * Stops forwarding navigation events to the application listener and resumes forwarding them to the connected
     * drone.
     * <p>
     * Note that navigation inputs are automatically released, and the application listener is unregistered as soon
     * as the gamepad peripheral disappears.
     */
    void release();
}
