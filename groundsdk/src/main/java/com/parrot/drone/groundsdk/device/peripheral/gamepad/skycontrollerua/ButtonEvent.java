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

package com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua;

import com.parrot.drone.groundsdk.device.peripheral.SkyControllerUaGamepad;

import androidx.annotation.NonNull;

/**
 * An event that may be produced by a gamepad {@link SkyControllerUaGamepad.Button button} or
 * {@link SkyControllerUaGamepad.Axis axis}.
 * <p>
 * The corresponding input has a button behavior, i.e. it can be either {@link State#PRESSED pressed} or
 * {@link State#RELEASED released}, and an event is sent each time that state changes, along with the current state.
 * <p>
 * Axes usually send a press event when they reach the start or end of their course, and send a release event when they
 * quit that position.
 */
public enum ButtonEvent {

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_TOP_LEFT front top left button} is pressed or
     * released.
     */
    FRONT_TOP_LEFT_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_TOP_RIGHT front top right button} is pressed or
     * released.
     */
    FRONT_TOP_RIGHT_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_BOTTOM_LEFT_1 first front bottom left button} is
     * pressed or released.
     */
    FRONT_BOTTOM_LEFT_1_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_BOTTOM_LEFT_2 second front bottom left button} is
     * pressed or released.
     */
    FRONT_BOTTOM_LEFT_2_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_BOTTOM_RIGHT_1 first front bottom right button} is
     * pressed or released.
     */
    FRONT_BOTTOM_RIGHT_1_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_BOTTOM_RIGHT_2 second front bottom right button} is
     * pressed or released.
     */
    FRONT_BOTTOM_RIGHT_2_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#FRONT_BOTTOM_RIGHT_3 third front bottom right button} is
     * pressed or released.
     */
    FRONT_BOTTOM_RIGHT_3_BUTTON,

    /**
     * Event sent when {@link SkyControllerUaGamepad.Button#REAR_LEFT rear left button} is pressed or released.
     */
    REAR_LEFT_BUTTON,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Button#REAR_RIGHT rear right button} is pressed or
     * released.
     */
    REAR_RIGHT_BUTTON,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_STICK_HORIZONTAL horizontal axis on left stick}
     * reaches or quits the left start of its course.
     */
    LEFT_STICK_LEFT,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_STICK_HORIZONTAL horizontal axis on left stick}
     * reaches or quits the right stop of its course.
     */
    LEFT_STICK_RIGHT,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_STICK_VERTICAL vertical axis on left stick} reaches
     * or quits the top stop of its course.
     */
    LEFT_STICK_UP,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_STICK_VERTICAL vertical axis on left stick} reaches
     * or quits the bottom start of its course.
     */
    LEFT_STICK_DOWN,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_STICK_HORIZONTAL horizontal axis on right stick}
     * reaches or quits the left start of its course.
     */
    RIGHT_STICK_LEFT,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_STICK_HORIZONTAL horizontal axis on right stick}
     * reaches or quits the right stop of its course.
     */
    RIGHT_STICK_RIGHT,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_STICK_VERTICAL vertical axis on right stick} reaches
     * or quits the top stop of its course.
     */
    RIGHT_STICK_UP,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_STICK_VERTICAL vertical axis on right stick} reaches
     * or quits the bottom start of its course.
     */
    RIGHT_STICK_DOWN,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_SLIDER left slider} reaches or quits the left start
     * of its course.
     */
    LEFT_SLIDER_UP,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#LEFT_SLIDER left slider} reaches or quits the right stop
     * of its course.
     */
    LEFT_SLIDER_DOWN,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_SLIDER right slider} reaches or quits the left start
     * of its course.
     */
    RIGHT_SLIDER_UP,

    /**
     * Event sent when the {@link SkyControllerUaGamepad.Axis#RIGHT_SLIDER right slider} reaches or quits the right stop
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
        void onButtonEvent(
                @NonNull ButtonEvent event,
                @NonNull State state);
    }
}
