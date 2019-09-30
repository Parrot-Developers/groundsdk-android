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

package com.parrot.drone.groundsdk.arsdkengine.blackbox.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Piloting command information.
 */
class PilotingCommandInfo {

    /** Piloting command roll. */
    @Expose
    @SerializedName("roll")
    private int mRoll;

    /** Piloting command pitch. */
    @Expose
    @SerializedName("pitch")
    private int mPitch;

    /** Piloting command yaw. */
    @Expose
    @SerializedName("yaw")
    private int mYaw;

    /** Piloting command gaz. */
    @Expose
    @SerializedName("gaz")
    private int mGaz;

    /**
     * Default constructor.
     */
    PilotingCommandInfo() {
    }

    /**
     * Copy constructor.
     *
     * @param other piloting command info to copy data from
     */
    PilotingCommandInfo(@NonNull PilotingCommandInfo other) {
        mRoll = other.mRoll;
        mPitch = other.mPitch;
        mYaw = other.mYaw;
        mGaz = other.mGaz;
    }

    /**
     * Updates piloting command information.
     *
     * @param roll  piloting command roll value
     * @param pitch piloting command pitch value
     * @param yaw   piloting command yaw value
     * @param gaz   piloting command gaz value
     *
     * @return {@code true} if piloting command information changed, otherwise {@code false}
     */
    boolean update(int roll, int pitch, int yaw, int gaz) {
        boolean changed = false;
        if (mRoll != roll) {
            mRoll = roll;
            changed = true;
        }
        if (mPitch != pitch) {
            mPitch = pitch;
            changed = true;
        }
        if (mYaw != yaw) {
            mYaw = yaw;
            changed = true;
        }
        if (mGaz != gaz) {
            mGaz = gaz;
            changed = true;
        }
        return changed;
    }
}
