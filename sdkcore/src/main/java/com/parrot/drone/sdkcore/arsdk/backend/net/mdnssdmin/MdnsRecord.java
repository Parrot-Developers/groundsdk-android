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
 * A Mdns Record identified by a name and a type.
 * <p>
 * Correspond to an entry in dns response or additional data.
 */
final class MdnsRecord {

    /**
     * Record types. Contains only the subset of types managed by mdns-sd-min
     */
    enum Type {
        /** Address record: data is a String containing an IP Address. */
        A((byte) 1),
        /** PTR record: data is a String containing a host name. */
        PTR((byte) 12),
        /** TXT record: data is a String array containing TXTs. */
        TXT((byte) 16),
        /** SRV record: data is a MdnsSrv containing the service port and target. */
        SRV((byte) 33);

        /** Type numerical value. */
        private final byte mVal;

        /**
         * Constructor.
         *
         * @param val type numerical value
         */
        Type(byte val) {
            mVal = val;
        }

        /**
         * Gets a Type by its numerical value.
         *
         * @param val type numerical value
         *
         * @return corresponding type, or null
         */
        static Type get(int val) {
            for (Type type : values()) {
                if (type.mVal == val) {
                    return type;
                }
            }
            return null;
        }
    }

    /** Record name. */
    @NonNull
    private final String mName;

    /** Record type. */
    @NonNull
    private final Type mType;

    /**
     * Constructor.
     *
     * @param name record name
     * @param type record type
     */
    MdnsRecord(@NonNull String name, @NonNull Type type) {
        mName = name;
        mType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MdnsRecord)) {
            return false;
        }

        MdnsRecord mdnsEntry = (MdnsRecord) o;

        return mName.equals(mdnsEntry.mName) && mType == mdnsEntry.mType;
    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + mType.hashCode();
        return result;
    }
}
