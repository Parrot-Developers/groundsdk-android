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

import com.parrot.drone.sdkcore.ulog.ULogTag;

/**
 * Logging tags and conditions.
 */
public final class Logging {

    /** Tag for API package. */
    public static final ULogTag TAG_API = new ULogTag("gsdk.api");

    /** Tag for internal package. */
    public static final ULogTag TAG_INTERNAL = new ULogTag("gsdk.internal");

    /** Tag for engine package. */
    public static final ULogTag TAG_ENGINE = new ULogTag("gsdk.engine");

    /** Tag for session related logs. */
    public static final ULogTag TAG_SESSION = new ULogTag("gsdk.session");

    /** Tag for executor logs. */
    public static final ULogTag TAG_EXECUTOR = new ULogTag("gsdk.exec");

    /** Tag for monitor logs. */
    public static final ULogTag TAG_MONITOR = new ULogTag("gsdk.monitor");

    /** Tag for crash report engine logs. */
    public static final ULogTag TAG_CRASH = new ULogTag("gsdk.crash");

    /** Tag for flight data engine logs. */
    public static final ULogTag TAG_FLIGHTDATA = new ULogTag("gsdk.flightdata");

    /** Tag for GUTMA logs engine logs. */
    public static final ULogTag TAG_GUTMALOG = new ULogTag("gsdk.gutmalog");

    /** Tag for flight log engine logs. */
    public static final ULogTag TAG_FLIGHTLOG = new ULogTag("gsdk.flightlog");

    /** Tag for firmware engine logs. */
    public static final ULogTag TAG_FIRMWARE = new ULogTag("gsdk.firmware");

    /** Tag for blackbox engine logs. */
    public static final ULogTag TAG_BLACKBOX = new ULogTag("gsdk.blackbox");

    /** Tag for http logs. */
    public static final ULogTag TAG_HTTP = new ULogTag("gsdk.http");

    /** Tag for MAVLink logs. */
    public static final ULogTag TAG_MAVLINK = new ULogTag("gsdk.mavlink");

    /** Tag for HMD logs. */
    public static final ULogTag TAG_HMD = new ULogTag("gsdk.hmd");

    /**
     * Private constructor for static utility class.
     */
    private Logging() {
    }
}
