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

package com.parrot.drone.sdkcore.arsdk.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Manages ArsdkStream resources for a device.
 * <p>
 * This component primarily works around the fact that, under certain circumstances (for instance, only one PDRAW
 * instance may use the MUX), only one ArsdkStream instance may be open at a single time; in order to open another
 * instance, then the existing one must be requested to close (which may involve calling back to alive renderer
 * components so that they stop rendering, and thus take some time) and notify that the closing process is complete
 * before we may attempt to open the new instance.
 * <p>
 * However, from an API standpoint, we don't want to expose such a limitation. The API provides a method that always
 * creates a new stream instance, and that guarantees that this stream instance will be opened at some point in the
 * future, unless otherwise closed by client request or for internal reasons.
 * <p>
 * As a consequence, this component effectively ensures that a single ArsdkStream instance at most is open at all times
 * (which is a bit more restrictive that what the limitation really is, but is an acceptable trade-off for now). <br/>
 * Every time a client requests to open a new stream, then the currently open (or opening) one is requested to close
 * behind the scenes. Client will receive a specific {@link ArsdkStream.CloseReason#INTERRUPTED} close reason on the
 * current stream, so that it may be aware that the stream was closed because another stream had to be opened.
 */
public class ArsdkDeviceStreamController {

    /** ArsdkCore instance. */
    @NonNull
    final ArsdkCore mArsdkCore;

    /** Handle of the device which provides the stream. */
    final short mDeviceHandle;

    /** Currently opened or opening stream. {@code null} if no stream is currently open(ing). */
    @Nullable
    private ArsdkStream mCurrentStream;

    /** Stream waiting for {@link #mCurrentStream} to close in order to be opened. {@code null} if no pending stream. */
    @Nullable
    private ArsdkStream mPendingStream;

    /**
     * Constructor.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle handle of the device which provides the stream.
     */
    public ArsdkDeviceStreamController(@NonNull ArsdkCore arsdkCore, short deviceHandle) {
        mArsdkCore = arsdkCore;
        mDeviceHandle = deviceHandle;
    }

    /**
     * Closes all open and pending streams.
     */
    public void closeStreams() {
        if (mPendingStream != null) {
            mPendingStream.close();
        }
        if (mCurrentStream != null) {
            mCurrentStream.close();
        }
    }

    /**
     * Creates a new {@code ArsdkStream} instance and requests it to open.
     *
     * @param url    stream url
     * @param track  stream track to select, {@code null} to select default track, if any
     * @param client client notified of stream events
     *
     * @return a new {@code ArsdkStream} instance, in the process of being opened
     */
    @NonNull
    public SdkCoreStream openStream(@NonNull String url, @Nullable String track, @NonNull ArsdkStream.Client client) {
        ArsdkStream stream = new ArsdkStream(this, client, url, track);
        if (mCurrentStream == null) {
            mCurrentStream = stream;
            mCurrentStream.open();
        } else if (mPendingStream == null) {
            mPendingStream = stream;
            mCurrentStream.close(ArsdkStream.CloseReason.INTERRUPTED);
        } else {
            mPendingStream.close(ArsdkStream.CloseReason.INTERRUPTED);
            mPendingStream = stream;
        }
        return stream;
    }

    /**
     * Notifies that a managed stream did just completely close.
     *
     * @param closedStream stream that did close
     */
    final void onStreamClosed(@NonNull ArsdkStream closedStream) {
        if (closedStream == mCurrentStream) {
            mCurrentStream = mPendingStream;
            mPendingStream = null;
            if (mCurrentStream != null) {
                mCurrentStream.open();
            }
        } else if (closedStream == mPendingStream) {
            mPendingStream = null;
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write(prefix + "--stream: dumps stream info\n");
        } else if (args.contains("--stream") || args.contains("--all")) {
            writer.write(prefix + "current stream: " + mCurrentStream + "\n");
            writer.write(prefix + "pending stream: " + mPendingStream + "\n");
        }
    }
}
