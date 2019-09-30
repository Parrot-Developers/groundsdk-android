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

package com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.SkyController3Gamepad;

/**
 * An event that may be produced by a gamepad {@link SkyController3Gamepad.Button button} or
 * {@link SkyController3Gamepad.Axis axis}.
 * <p>
 * The corresponding input has a button behavior, i.e. it can be either {@link State#PRESSED pressed} or
 * {@link State#RELEASED released}, and an event is sent each time that state changes, along with the current state.
 * <p>
 * Axes usually send a press event when they reach the start or end of their course, and send a release event when they
 * quit that position.
 */
public enum ButtonEvent {

    /**
     * Event sent when {@link SkyController3Gamepad.Button#FRONT_TOP_BUTTON front top button} is pressed or released.
     */
    FRONT_TOP_BUTTON,

    /**
     * Event sent when {@link SkyController3Gamepad.Button#FRONT_BOTTOM_BUTTON front bottom button} is pressed or
     * released.
     */
    FRONT_BOTTOM_BUTTON,

    /**
     * Event sent when {@link SkyController3Gamepad.Button#REAR_LEFT_BUTTON rear left button} is pressed or released.
     */
    REAR_LEFT_BUTTON,

    /**
     * Event sent when the {@link SkyController3Gamepad.Button#REAR_RIGHT_BUTTON rear right button} is pressed or
     * released.
     */
    REAR_RIGHT_BUTTON,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_STICK_HORIZONTAL horizontal axis on left stick}
     * reaches or quits the left start of its course.
     */
    LEFT_STICK_LEFT,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_STICK_HORIZONTAL horizontal axis on left stick}
     * reaches or quits the right stop of its course.
     */
    LEFT_STICK_RIGHT,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_STICK_VERTICAL vertical axis on left stick} reaches
     * or quits the top stop of its course.
     */
    LEFT_STICK_UP,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_STICK_VERTICAL vertical axis on left stick} reaches
     * or quits the bottom start of its course.
     */
    LEFT_STICK_DOWN,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_STICK_HORIZONTAL horizontal axis on right stick}
     * reaches or quits the left start of its course.
     */
    RIGHT_STICK_LEFT,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_STICK_HORIZONTAL horizontal axis on right stick}
     * reaches or quits the right stop of its course.
     */
    RIGHT_STICK_RIGHT,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_STICK_VERTICAL vertical axis on right stick} reaches
     * or quits the top stop of its course.
     */
    RIGHT_STICK_UP,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_STICK_VERTICAL vertical axis on right stick} reaches
     * or quits the bottom start of its course.
     */
    RIGHT_STICK_DOWN,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_SLIDER left slider} reaches or quits the left start
     * of its course.
     */
    LEFT_SLIDER_UP,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#LEFT_SLIDER left slider} reaches or quits the right stop
     * of its course.
     */
    LEFT_SLIDER_DOWN,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_SLIDER right slider} reaches or quits the left start
     * of its course.
     */
    RIGHT_SLIDER_UP,

    /**
     * Event sent when the {@link SkyController3Gamepad.Axis#RIGHT_SLIDER right slider} reaches or quits the right stop
     * of its course.
     */
    RIGHT_SLIDER_DOWN;

    /** State of the corresponding button. */
    public enum State {

        /** Button is pressed. */
        PRESSED,

        /** Button is released. */
        RELEASED
    }

    /**
     * Receives button events sent from the remote control when a corresponding input is grabbed.
     */
    public interface Listener {

        /**
         * Called back when a button event is received.
         *
         * @param event received button event
         * @param state current button state
         */
        void onButtonEvent(@NonNull ButtonEvent event, @NonNull State state);
    }
}
