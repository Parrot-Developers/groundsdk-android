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

package com.parrot.drone.sdkcore.arsdk;

import com.parrot.drone.sdkcore.ulog.ULogTag;

/**
 * Logging tags and conditions.
 */
public final class Logging {

    /** Main tag. */
    public static final ULogTag TAG = new ULogTag("arsdk");

    /** Tag for BLE backend related logs. */
    public static final ULogTag TAG_BLE = new ULogTag("arsdk.ble");

    /** Tag for MUX backend related logs. */
    public static final ULogTag TAG_MUX = new ULogTag("arsdk.mux");

    /** Tag for NET backend related logs. */
    public static final ULogTag TAG_NET = new ULogTag("arsdk.net");

    /** Tag for mdns discovery. */
    public static final ULogTag TAG_MDNS = new ULogTag("arsdk.mdns");

    /** Tag for device related logs. */
    public static final ULogTag TAG_DEVICE = new ULogTag("arsdk.device");

    /** Tag for stream related logs. */
    public static final ULogTag TAG_STREAM = new ULogTag("arsdk.stream");

    /**
     * hide default constructor.
     */
    private Logging() {
    }
}
