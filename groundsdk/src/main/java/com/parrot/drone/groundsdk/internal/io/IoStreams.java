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

package com.parrot.drone.groundsdk.internal.io;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides utilities to manipulate {@link InputStream} and {@link OutputStream}.
 */
public final class IoStreams {

    /** Default size for a transfer chunk. */
    static final int DEFAULT_TRANSFER_CHUNK_SIZE = 4096;

    /**
     * Transfers content of an input stream to an output stream.
     * <p>
     * This method reads chunks of {@value #DEFAULT_TRANSFER_CHUNK_SIZE} bytes from {@code src} and writes them to
     * {@code dst}.
     *
     * @param src input stream to read data from
     * @param dst output stream to write data to
     *
     * @throws IOException          in case reading or writing failed
     * @throws InterruptedException if the current thread is interrupted while this method executes. Interruption
     *                              status is checked in between each chunk read and write.
     */
    public static void transfer(@NonNull InputStream src, @NonNull OutputStream dst)
            throws IOException, InterruptedException {
        transfer(src, dst, DEFAULT_TRANSFER_CHUNK_SIZE);
    }

    /**
     * Transfers content of an input stream to an output stream.
     * <p>
     * This method reads chunks of {@code chunkSize} bytes from {@code src} and writes them to {@code dst}.
     *
     * @param src       input stream to read data from
     * @param dst       output stream to write data to
     * @param chunkSize size of chunks read from {@code src}, in bytes
     *
     * @throws IOException          in case reading or writing failed
     * @throws InterruptedException if the current thread is interrupted while this method executes. Interruption
     *                              status is checked in between each chunk read and write.
     */
    public static void transfer(@NonNull InputStream src, @NonNull OutputStream dst, @IntRange(from = 1) int chunkSize)
            throws IOException, InterruptedException {
        byte[] buffer = new byte[chunkSize];
        int len;
        while (!Thread.currentThread().isInterrupted()
               && (len = src.read(buffer)) > 0) {
            if (!Thread.currentThread().isInterrupted()) {
                dst.write(buffer, 0, len);
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * An interface that allows to open an {@code InputStream}.
     */
    public interface LazyInputStream {

        /**
         * Opens the input stream.
         *
         * @return the opened {@code InputStream}
         *
         * @throws IOException in case opening the stream failed
         */
        @NonNull
        InputStream open() throws IOException;
    }

    /**
     * Creates a lazy input stream that is not open until first access.
     * <p>
     * This stream can also be reopened after having been closed, by calling any of the access methods.
     *
     * @param lazyStream interface that provides the concrete input stream to lazily open
     *
     * @return a lazily opened {@code InputStream}
     */
    @NonNull
    public static InputStream lazy(@NonNull LazyInputStream lazyStream) {
        return new InputStream() {

            /** Target stream, {@code null} when closed. */
            @Nullable
            private InputStream mStream;

            @NonNull
            private InputStream stream() throws IOException {
                if (mStream == null) {
                    mStream = lazyStream.open();
                }
                return mStream;
            }

            @Override
            public int read() throws IOException {
                return stream().read();
            }

            @Override
            public int read(@NonNull byte[] b) throws IOException {
                return stream().read(b);
            }

            @Override
            public int read(@NonNull byte[] b, int off, int len) throws IOException {
                return stream().read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return stream().skip(n);
            }

            @Override
            public int available() throws IOException {
                return stream().available();
            }

            @Override
            public void close() throws IOException {
                if (mStream != null) {
                    mStream.close();
                    mStream = null; // allows the stream to be re-opened
                }
            }

            @Override
            public synchronized void mark(int readLimit) {
                try {
                    stream().mark(readLimit);
                } catch (IOException ignored) {
                }
            }

            @Override
            public synchronized void reset() throws IOException {
                stream().reset();
            }

            @Override
            public boolean markSupported() {
                try {
                    return stream().markSupported();
                } catch (IOException ignored) {
                    return false;
                }
            }
        };
    }

    /**
     * Private constructor for static utility class.
     */
    private IoStreams() {
    }
}
