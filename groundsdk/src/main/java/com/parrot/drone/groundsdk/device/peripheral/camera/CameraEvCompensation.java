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

package com.parrot.drone.groundsdk.device.peripheral.camera;

/**
 * Camera exposure compensation value.
 */
public enum CameraEvCompensation {

    /** -3.00 EV. */
    EV_MINUS_3,

    /** -2.67 EV. */
    EV_MINUS_2_67,

    /** -2.33 EV. */
    EV_MINUS_2_33,

    /** -2.00 EV. */
    EV_MINUS_2,

    /** -1.67 EV. */
    EV_MINUS_1_67,

    /** -1.33 EV. */
    EV_MINUS_1_33,

    /** -1.00 EV. */
    EV_MINUS_1,

    /** -0.67 EV. */
    EV_MINUS_0_67,

    /** -0.33 EV. */
    EV_MINUS_0_33,

    /** 0.00 EV. */
    EV_0,

    /** +0.33 EV. */
    EV_0_33,

    /** +0.67 EV. */
    EV_0_67,

    /** +1.00 EV. */
    EV_1,

    /** +1.33 EV. */
    EV_1_33,

    /** +1.67 EV. */
    EV_1_67,

    /** +2.00 EV. */
    EV_2,

    /** +2.33 EV. */
    EV_2_33,

    /** +2.67 EV. */
    EV_2_67,

    /** +3.00 EV. */
    EV_3
}
