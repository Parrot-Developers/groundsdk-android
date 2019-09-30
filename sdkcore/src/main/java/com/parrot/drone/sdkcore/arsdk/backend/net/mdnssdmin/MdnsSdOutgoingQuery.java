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

import java.io.ByteArrayOutputStream;

/**
 * A MDNS-SD service Query
 * <p>
 * Build a PTR query for a list of service name.
 */
final class MdnsSdOutgoingQuery {

    /** Array of service name mQuestions. */
    @NonNull
    private final String[] mQuestions;

    /**
     * Constructor.
     *
     * @param questions array of service name to query
     */
    MdnsSdOutgoingQuery(@NonNull String[] questions) {
        mQuestions = questions;
    }

    /**
     * Encode the query.
     *
     * @return query data ready to be send as UDP payload
     */
    byte[] encode() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // header
        // uid: always 0 in mdns
        writeU16(os, 0);
        // OP code. R code, flags : 0 for a query
        writeU16(os, 0);
        // Questions
        writeU16(os, mQuestions.length);
        // Answer RRs
        writeU16(os, 0);
        // Authority RRs
        writeU16(os, 0);
        // Additional RRs
        writeU16(os, 0);

        // mQuestions
        for (String question : mQuestions) {
            // name
            writeName(os, question);
            // type PTR
            writeU16(os, 12);
            // class IN
            writeU16(os, 1);
        }
        return os.toByteArray();
    }

    /**
     * Write an int as 16 bits unsigned, network byte order.
     *
     * @param outputStream the stream to write to
     * @param val          the value to write
     */
    private static void writeU16(@NonNull ByteArrayOutputStream outputStream, int val) {
        outputStream.write(val >> 8);
        outputStream.write(val);
    }

    /**
     * Write an encoded domain name.
     *
     * @param outputStream the stream to write to
     * @param name         the name to write
     */
    private static void writeName(@NonNull ByteArrayOutputStream outputStream, @NonNull String name) {
        String[] segments = name.split("\\.");
        for (String s : segments) {
            outputStream.write(s.length());
            // note: only supports US-ASCII char
            for (char ch : s.toCharArray()) {
                outputStream.write(ch);
            }
        }
        outputStream.write(0);
    }
}
