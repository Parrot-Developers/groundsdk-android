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

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ArsdkCommandDecoder {

    @NonNull
    private final ByteBuffer mBuf;

    public ArsdkCommandDecoder(@NonNull ArsdkCommand command) {
        mBuf = command.getData();
        mBuf.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int readSignedByte() {
        return mBuf.get();
    }

    public int readUnsignedByte() {
        return mBuf.get() & 0xff;
    }

    public int readSignedShort() {
        return mBuf.getShort();
    }

    public int readUnsignedShort() {
        return mBuf.getShort() & 0xffff;
    }

    public int readSignedInt() {
        return mBuf.getInt();
    }

    public long readUnsignedInt() {
        return mBuf.getInt() & 0xffffffffL;
    }

    public long readSignedLong() {
        return mBuf.getLong();
    }

    public long readUnsignedLong() {
        // to be correct this would need BigInteger return type. E.g:
        // long value = mBuf.getLong();
        // return new BigInteger(new byte[] {0x00,
        //         (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40), (byte) (value >> 32),
        //         (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value});
        return readSignedLong(); // ¯\_(ツ)_/¯
    }

    public float readFloat() {
        return mBuf.getFloat();
    }

    public double readDouble() {
        return mBuf.getDouble();
    }

    @NonNull
    public String readString() {
        StringBuilder sb = new StringBuilder();
        while (mBuf.remaining() > 0) {
            char c = (char) mBuf.get();
            if (c == '\0') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @NonNull
    public byte[] readBinary() {
        int size =  mBuf.getInt();
        if (size < 0) {
            throw new AssertionError("Binary buffer too large");
        }
        byte[] binary = new byte[size];
        mBuf.get(binary);
        return binary;
    }

    public void reset() {
        mBuf.clear();
    }
}
