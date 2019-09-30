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

package com.parrot.drone.sdkcore.arsdk.backend.net.mdnssdmin;

import androidx.annotation.NonNull;

/**
 * Store mdns SRV data.
 */
final class MdnsSrvData {

    /** Service mPort. */
    private final int mPort;

    /** Service dns address. */
    @NonNull
    private final String mTarget;

    /** Service time to live. */
    private final long mTtl;

    /**
     * Constructor.
     *
     * @param port   Service port
     * @param target Service dns address
     * @param ttl    Service time to live
     */
    MdnsSrvData(int port, @NonNull String target, long ttl) {
        mPort = port;
        mTarget = target;
        mTtl = ttl;
    }

    /**
     * Gets the service port.
     *
     * @return the service port
     */
    int getPort() {
        return mPort;
    }

    /**
     * Gets the service target name.
     *
     * @return the service target name
     */
    @NonNull
    String getTarget() {
        return mTarget;
    }

    /**
     * Gets the time to live.
     *
     * @return the time to live
     */
    long getTtl() {
        return mTtl;
    }
}
