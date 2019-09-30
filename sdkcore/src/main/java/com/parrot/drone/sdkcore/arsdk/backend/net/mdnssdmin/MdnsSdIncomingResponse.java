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
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A mdns incoming response message.
 */
final class MdnsSdIncomingResponse {

    /** All parsed mEntries, by record. Class of an entry value depends of its type */
    @NonNull
    private final Map<MdnsRecord, Object> mEntries;

    /**
     * Constructor.
     *
     * @param data udp payload received
     */
    MdnsSdIncomingResponse(@NonNull byte[] data) {
        mEntries = new HashMap<>();
        new Decoder(data).decode();
    }

    /**
     * Gets an A entry by name.
     *
     * @param name the name to lookup
     *
     * @return A value (an ip address) or null if not found
     */
    String getAddress(@NonNull String name) {
        return (String) mEntries.get(new MdnsRecord(name, MdnsRecord.Type.A));
    }

    /**
     * Gets an PTR entry by name.
     *
     * @param name the name to lookup
     *
     * @return PTR value (an domain name) or null if not found
     */
    String getPtr(@NonNull String name) {
        return (String) mEntries.get(new MdnsRecord(name, MdnsRecord.Type.PTR));
    }

    /**
     * Gets an SRV entry by name.
     *
     * @param name the name to lookup
     *
     * @return SRV values (ip, name, ttl) or null if not found
     */
    MdnsSrvData getService(@NonNull String name) {
        return (MdnsSrvData) mEntries.get(new MdnsRecord(name, MdnsRecord.Type.SRV));

    }

    /**
     * Gets an TXT entry by name.
     *
     * @param name the name to lookup
     *
     * @return TXT values (an array of strings) or null if not found
     */
    @Nullable
    String[] getTexts(@NonNull String name) {
        return (String[]) mEntries.get(new MdnsRecord(name, MdnsRecord.Type.TXT));
    }

    /**
     * DNS message decoder.
     */
    private class Decoder {

        /** current pos in the buffer. */
        private int mPos;

        /** Udp received bytes. */
        @NonNull
        private final byte[] mBuffer;

        /**
         * Constructor.
         *
         * @param data Udp received data
         */
        Decoder(@NonNull byte[] data) {
            mBuffer = data;
            mPos = 0;
        }

        /**
         * Decode.
         */
        void decode() {
            // header
            int id = readU16();
            @SuppressWarnings("unused")
            int flags = readU16();
            int questionsCnt = readU16();
            int answersCnt = readU16();
            int authoritiesCnt = readU16();
            int additionalRRsCnt = readU16();

            // sanity check
            if (id == 0 && questionsCnt >= 0 && answersCnt >= 0 && authoritiesCnt >= 0 && additionalRRsCnt >= 0) {
                // skip questions if any
                for (int cnt = 0; cnt < questionsCnt; cnt++) {
                    @SuppressWarnings("unused")
                    String name = readName();
                    @SuppressWarnings("unused")
                    int type = readU16();
                    @SuppressWarnings("unused")
                    int cls = readU16();
                }

                // read answers
                for (int cnt = 0; cnt < answersCnt; cnt++) {
                    readResourceRecord();
                }

                // read authorities
                for (int cnt = 0; cnt < authoritiesCnt; cnt++) {
                    readResourceRecord();
                }

                // read additional records
                for (int cnt = 0; cnt < additionalRRsCnt; cnt++) {
                    readResourceRecord();
                }
            }
        }

        /**
         * Read a received resource record.
         */
        private void readResourceRecord() {
            String name = readName();
            MdnsRecord.Type type = MdnsRecord.Type.get(readU16());
            @SuppressWarnings("unused")
            int cls = readU16();
            long ttl = readU32();
            int dataLen = readU16();
            if (type != null) {
                switch (type) {
                    case A:
                        if (dataLen == 4) {
                            String address =
                                    String.format(Locale.US, "%d.%d.%d.%d", readU8(), readU8(), readU8(), readU8());
                            mEntries.put(new MdnsRecord(name, type), address);
                        }
                        break;

                    case PTR:
                        String domainName = readName();
                        mEntries.put(new MdnsRecord(name, type), domainName);
                        break;

                    case TXT:
                        int end = mPos + dataLen;
                        List<String> txtRecords = new ArrayList<>();
                        while (mPos < end) {
                            txtRecords.add(readString());
                        }
                        mEntries.put(new MdnsRecord(name, type), txtRecords.toArray(new String[0]));
                        break;

                    case SRV:
                        @SuppressWarnings("unused")
                        int priority = readU16();
                        @SuppressWarnings("unused")
                        int weight = readU16();
                        int port = readU16();
                        String target = readName();
                        mEntries.put(new MdnsRecord(name, type), new MdnsSrvData(port, target, ttl));
                        break;
                    default:
                }
            } else {
                // unhandled types: skip data
                mPos += dataLen;
            }
        }

        /**
         * Read an unsigned 8 bits int.
         *
         * @return unsigned 8 bits int at current position
         */
        private int readU8() {
            return mBuffer[mPos++] & 0xFF;
        }

        /**
         * Read an unsigned 16 bits int.
         *
         * @return unsigned 16 bits int at current position
         */
        private int readU16() {
            return ((readU8() << 8) & 0xFFFF) | ((readU8()) & 0xFF);
        }

        /**
         * Read an unsigned 32 bits int.
         *
         * @return unsigned 32 bits int at current position
         */
        private long readU32() {
            return (((long) readU16()) << 16) | readU16();
        }

        /**
         * Read a dns name.
         *
         * @return dns name at current position
         */
        @NonNull
        private String readName() {
            StringBuilder sb = new StringBuilder();
            readNameSegment(sb);
            return sb.toString();
        }

        /**
         * Read a dns name fragment and append it to a string builder.
         *
         * @param sb string builder to append the fragment to
         */
        private void readNameSegment(StringBuilder sb) {
            int len = readU8();
            while (len != 0) {
                if ((len & 0xC0) == 0) {
                    for (int cnt = 0; cnt < len; cnt++) {
                        sb.append((char) mBuffer[mPos++]);
                    }
                    sb.append('.');
                    len = readU8();
                } else {
                    // reference to other string
                    int offset = (len & 0x003F) << 8 | readU8() & 0xFF;
                    int savedPos = mPos;
                    mPos = offset;
                    readNameSegment(sb);
                    mPos = savedPos;
                    len = 0;
                }
            }
        }

        /**
         * Read a dns string.
         *
         * @return string at current position
         */
        @NonNull
        private String readString() {
            StringBuilder sb = new StringBuilder();
            int len = readU8();
            for (int cnt = 0; cnt < len; cnt++) {
                sb.append((char) mBuffer[mPos++]);
            }
            return sb.toString();
        }

    }
}
