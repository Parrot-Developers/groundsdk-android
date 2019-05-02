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

package com.parrot.drone.groundsdk.device.peripheral.gamepad;

import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;

/**
 * An action that may be triggered when the gamepad inputs generate a specific set of button events.
 * <p>
 * Actions starting with {@code APP_ACTION_*} don't occur on the connected drone but are forwarded to the application
 * as local intent broadcasts (see {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}).
 * <p>
 * Other actions are predefined actions that are executed by the connected drone.
 */
public enum ButtonsMappableAction {

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of
     * APP_ACTION_SETTINGS.
     */
    APP_ACTION_SETTINGS,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_1.
     */
    APP_ACTION_1,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_2.
     */
    APP_ACTION_2,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_3.
     */
    APP_ACTION_3,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_4.
     */
    APP_ACTION_4,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_5.
     */
    APP_ACTION_5,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_6.
     */
    APP_ACTION_6,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_7.
     */
    APP_ACTION_7,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_8.
     */
    APP_ACTION_8,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_9.
     */
    APP_ACTION_9,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_10.
     */
    APP_ACTION_10,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_11.
     */
    APP_ACTION_11,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_12.
     */
    APP_ACTION_12,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_13.
     */
    APP_ACTION_13,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_14.
     */
    APP_ACTION_14,

    /**
     * Sends a local broadcast intent to the application.
     * <p>
     * Intent action is {@link VirtualGamepad#ACTION_GAMEPAD_APP_EVENT}, and the intent contains the
     * {@link VirtualGamepad#EXTRA_GAMEPAD_APP_EVENT_ACTION} extra which holds the ordinal value of APP_ACTION_15.
     */
    APP_ACTION_15,

    /** Commands the connected drone to return home. */
    RETURN_HOME,

    /** Commands the connected drone to take off or land (depending on its current state). */
    TAKEOFF_OR_LAND,

    /** Commands the connected drone to start recording a video. */
    RECORD_VIDEO,

    /** Commands the connected drone to take a picture. */
    TAKE_PICTURE,

    /**
     * Commands the connected drone to<ul>
     * <li>either take a photo, in case the camera is in picture mode</li>
     * <li>or start/stop video recording, in case the camera is in recording mode.</li>
     * </ul>
     */
    PHOTO_OR_VIDEO,

    /** Commands the connected drone to center its camera. */
    CENTER_CAMERA,

    /** Commands the connected drone to increase current camera exposition. */
    INCREASE_CAMERA_EXPOSITION,

    /** Commands the connected drone to decrease current camera exposition. */
    DECREASE_CAMERA_EXPOSITION,

    /** Commands the connected drone to perform a left flip. */
    FLIP_LEFT,

    /** Commands the connected drone to perform a right flip. */
    FLIP_RIGHT,

    /** Commands the connected drone to perform a front flip. */
    FLIP_FRONT,

    /** Commands the connected drone to perform a back flip. */
    FLIP_BACK,

    /** Commands the connected drone to perform an emergency motor cut-off. */
    EMERGENCY_CUTOFF,

    /**
     * Commands the controller to cycle between different configurations of the HUD on the external HDMI display (if
     * present and supported by the controller.)
     */
    CYCLE_HUD
}
