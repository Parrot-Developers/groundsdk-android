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

package com.parrot.drone.groundsdk.arsdkengine.http;

import androidx.annotation.NonNull;

import java.io.IOException;

import okio.Buffer;
import okio.ForwardingSource;

final class BlockingBufferSource extends ForwardingSource {

    private final long mSize;

    private long mNextBytes;

    BlockingBufferSource(byte[] data) {
        super(new Buffer().write(data));
        mSize = data.length;
    }

    long size() {
        return mSize;
    }

    void unblockNextBytes(long byteCount) {
        synchronized (this) {
            mNextBytes = byteCount;
            notifyAll();
        }
    }

    void unblockCompletely() {
        synchronized (this) {
            mNextBytes = Long.MAX_VALUE;
            notifyAll();
        }
    }

    void unblockWithEof() {
        unblockNextBytes(-1);
    }

    @Override
    public long read(@NonNull Buffer sink, long byteCount) throws IOException {
        synchronized (this) {
            while (mNextBytes == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // be graceful on interruption
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
            byteCount = Math.min(mNextBytes, byteCount);
            if (mNextBytes != Long.MAX_VALUE) {
                mNextBytes = 0;
            }
            return byteCount == -1 ? -1 : super.read(sink, byteCount);
        }
    }
}
