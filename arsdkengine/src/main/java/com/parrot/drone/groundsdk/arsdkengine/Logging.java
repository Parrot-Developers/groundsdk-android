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

package com.parrot.drone.groundsdk.arsdkengine;

import com.parrot.drone.sdkcore.ulog.ULogTag;

/**
 * Logging tags and conditions.
 */
public final class Logging {

    /** Main tag. */
    public static final ULogTag TAG = new ULogTag("arsdkengine");

    /** Tag for device controller logs. */
    public static final ULogTag TAG_CTRL = new ULogTag("arsdkengine.ctrl");

    /** Tag for virtual gamepad logs. */
    public static final ULogTag TAG_GAMEPAD = new ULogTag("arsdkengine.gamepad");

    /** Tag for flightplan piloting interface logs. */
    public static final ULogTag TAG_FLIGHTPLAN = new ULogTag("arsdkengine.flightplan");

    /** Tag for tracking piloting interface logs. */
    public static final ULogTag TAG_TRACKING_PITF = new ULogTag("arsdkengine.tracking");

    /** Tag for return home piloting interface logs. */
    public static final ULogTag TAG_RETURNHOME = new ULogTag("arsdkengine.returnhome");

    /** Tag for wifi peripheral(s) logs. */
    public static final ULogTag TAG_WIFI = new ULogTag("arsdkengine.wifi");

    /** Tag for piloting interface activation logs. */
    public static final ULogTag TAG_PITF = new ULogTag("arsdkengine.pitf");

    /** Tag for ephemeris logs. */
    public static final ULogTag TAG_EPHEMERIS = new ULogTag("arsdkengine.ephemeris");

    /** Tag for blackbox recorder logs. */
    public static final ULogTag TAG_BLACKBOX = new ULogTag("arsdkengine.blackbox");

    /** Tag for http logs. */
    public static final ULogTag TAG_HTTP = new ULogTag("arsdkengine.http");

    /** Tag for media logs. */
    public static final ULogTag TAG_MEDIA = new ULogTag("arsdkengine.media");

    /** Tag for persistence logs. */
    public static final ULogTag TAG_STORAGE = new ULogTag("arsdkengine.storage");

    /** Tag for camera logs. */
    public static final ULogTag TAG_CAMERA = new ULogTag("arsdkengine.camera");

    /**
     * Private constructor for static utility class.
     */
    private Logging() {
    }
}
