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

package com.parrot.drone.groundsdk.internal.utility;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

import java.util.Collection;
import java.util.List;

/**
 * Utility interface for downloading remote firmwares.
 * <p>
 * This utility may be unavailable if firmware support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code FirmwareDownloader store = getUtility(FirmwareDownloader.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isFirmwareEnabled()
 */
public interface FirmwareDownloader extends Utility {

    /** A firmware download task. */
    interface Task {

        /** Observer notified when a {@link Task} state changes. */
        interface Observer {

            /**
             * Called back when an observed task's state changes.
             *
             * @param task task that did change
             */
            void onChange(@NonNull Task task);
        }

        /** Task state. */
        enum State {

            /** Task has pending firmwares to be downloaded, none of which are being downloaded currently. */
            QUEUED,

            /** Next firmware in task queue is being downloaded. */
            DOWNLOADING,

            /** All firmwares for this task have been successfully downloaded. */
            SUCCESS,

            /** Task failed to download some firmware. */
            FAILED,

            /** Task was canceled by client request. */
            CANCELED
        }

        /**
         * Retrieves current task's state.
         *
         * @return current task state
         */
        @NonNull
        State state();

        /**
         * Gives the list of firmwares that have been requested to be downloaded for this task.
         * <p>
         * The returned list is in the same order as what was requested by client upon
         * {@link #download(Collection, Observer) task creation}. It thus does not contain duplicate elements.
         * <p>
         * The returned list does never change and cannot be modified.
         *
         * @return requested firmwares
         */
        @NonNull
        List<FirmwareInfo> requested();

        /**
         * Gives the list of firmwares that have not been completely downloaded for this task, so far.
         * <p>
         * The returned list is in the same order as what was requested by client upon
         * {@link #download(Collection, Observer) task creation}.
         * <p>
         * The returned list cannot be modified.
         *
         * @return remaining firmwares to be downloaded
         */
        @NonNull
        List<FirmwareInfo> remaining();

        /**
         * Gives the list of firmwares that have been successfully downloaded for this task, so far.
         * <p>
         * The returned list is in the same order as what was requested by client upon
         * {@link #download(Collection, Observer) task creation}.
         * <p>
         * The returned list cannot be modified.
         *
         * @return downloaded firmwares
         */
        @NonNull
        default List<FirmwareInfo> downloaded() {
            return requested().subList(0, currentCount() - (state() == State.SUCCESS ? 0 : 1));
        }

        /**
         * Current progress of the ongoing firmware download.
         * <p>
         * This is {@code 0} when the task is {@link State#QUEUED queued}, {@code 100} when the task is
         * {@link State#SUCCESS successful}. <br>
         * Otherwise, this is current progress of the ongoing download if the task is {@link State#DOWNLOADING}, or the
         * latest reached progress when the task {@link State#FAILED failed} or was {@link State#CANCELED}.
         *
         * @return current download progress
         */
        @IntRange(from = 0, to = 100)
        int currentProgress();

        /**
         * Gives the index of the latest firmware being or having been processed so far.
         * <p>
         * This gives the index of the current firmware being {@link State#QUEUED queued} or
         * {@link State#DOWNLOADING downloaded}, or the firmware that was being downloaded when the task completed,
         * either {@link State#SUCCESS successfully}, {@link State#FAILED with error} or because of
         * {@link State#CANCELED cancelation}.
         * <p>
         * Value in in range [1, {@link #totalCount() totalCount}].
         *
         * @return current firmware index
         */
        @IntRange(from = 1)
        default int currentCount() {
            int total = totalCount();
            return state() == State.SUCCESS ? total : total - remaining().size() + 1;
        }

        /**
         * Gives the latest firmware being or having been processed so far.
         * <p>
         * This gives the current firmware being {@link State#QUEUED queued} or {@link State#DOWNLOADING downloaded},
         * or the firmware that was being downloaded when the task completed, either {@link State#SUCCESS successfully},
         * {@link State#FAILED with error} or because of {@link State#CANCELED cancelation}.
         *
         * @return current firmware
         */
        @NonNull
        default FirmwareInfo current() {
            return requested().get(currentCount() - 1);
        }

        /**
         * Gives the total count of firmwares this task should download.
         *
         * @return total firmwares count
         */
        @IntRange(from = 1)
        default int totalCount() {
            return requested().size();
        }

        /**
         * Gives the overall task progress, in percent.
         *
         * @return overall task progress
         */
        @IntRange(from = 0, to = 100)
        default int overallProgress() {
            long totalSize = 0;
            for (FirmwareInfo info : requested()) {
                totalSize += info.getSize();
            }
            long downloadedSize = 0;
            for (FirmwareInfo info : downloaded()) {
                downloadedSize += info.getSize();
            }
            if (state() != State.SUCCESS) {
                long currentSize = current().getSize(); // use a local var to circumvent a lint Range false-negative.
                downloadedSize += currentProgress() * currentSize / 100;
            }
            return totalSize == 0 ? 0 : Math.round(downloadedSize * 100 / (float) totalSize);
        }

        /**
         * Cancels the task.
         * <p>
         * When canceled, all queued firmware download requests are discarded. <br>
         * In case some firmware is currently being downloaded for this task, then, provided no other existing task
         * requested that particular firmware to be downloaded too, the download is canceled.
         * <p>
         * This operation has no effect if the task is already {@link State#CANCELED canceled}, has
         * {@link State#FAILED failed}, or completed {@link State#SUCCESS successfully}.
         */
        void cancel();
    }

    /**
     * Downloads firmwares.
     * <p>
     * The requested collection of firmwares will be downloaded as part of a single download operation, which the
     * returned {@link Task task} represents. <br>
     * In case downloading any of the specified firmwares fails, for whatever reason, then the whole task fails, and
     * any pending firmware downloads for this operation are discarded. <br>
     * <p>
     * The requested firmwares will be downloaded in the iteration order of the provided collection; duplicate elements
     * will be ignored.
     *
     * @param firmwares set of firmwares to be downloaded.
     * @param observer  observer to notify when the download task's state changes
     *
     * @return a new task providing progress and status information about the overall download operation
     */
    @NonNull
    Task download(@NonNull Collection<FirmwareInfo> firmwares, @NonNull Task.Observer observer);
}
