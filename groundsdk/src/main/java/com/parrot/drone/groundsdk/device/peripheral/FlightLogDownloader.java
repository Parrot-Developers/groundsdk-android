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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;

/**
 * FlightLogDownloader peripheral interface for Drone and RemoteControl devices.
 * <p>
 * This peripheral informs about automated download of flight logs from connected devices.
 * <p>
 * This peripheral is unavailable if flight log support is disabled in config.
 * <p>
 * This peripheral can be obtained from a {@link Provider peripheral providing device} (such as a drone or a
 * remote control) using:
 * <br><pre>    {@code device.getPeripheral(FlightLogDownloader.class)}</pre>
 *
 * @see Provider#getPeripheral(Class)
 * @see Provider#getPeripheral(Class, Ref.Observer)
 */
public interface FlightLogDownloader extends Peripheral {

    /**
     * Tells whether flight logs are currently being downloaded from the device.
     *
     * @return {@code true} when flight log download is ongoing, otherwise {@code false}
     */
    boolean isDownloading();

    /**
     * Download completion status.
     */
    enum CompletionStatus {

        /**
         * No known completion status.
         * <p>
         * Either a download is {@link #isDownloading() ongoing} but has not completed yet, or no flight log
         * download has ever been started yet.
         */
        NONE,

        /**
         * Latest flight logs download was successful.
         * <p>
         * {@link #getLatestDownloadCount()} informs about the total count of downloaded flight logs.
         */
        SUCCESS,

        /**
         * Latest flight logs download was aborted before successful completion.
         * <p>
         * {@link #getLatestDownloadCount()} informs about the count of successfully downloaded flight logs before
         * interruption.
         */
        INTERRUPTED
    }

    /**
     * Retrieves latest flight logs download completion status.
     *
     * @return latest completion status
     */
    @NonNull
    CompletionStatus getCompletionStatus();

    /**
     * Retrieves latest count of successfully downloaded flight logs.
     * <p>
     * While downloading, this counter is incremented for each successfully downloaded flight log. Once download
     * is over (either {@link CompletionStatus#SUCCESS successfully} or because of
     * {@link CompletionStatus#INTERRUPTED interruption}, then it will keep its latest value, until flight logs
     * download starts again, where it will be reset to {@code 0}.
     *
     * @return latest successfully downloaded flight logs count
     */
    @IntRange(from = 0)
    int getLatestDownloadCount();
}
