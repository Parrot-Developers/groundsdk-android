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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.gutmalog;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog.HttpFlightLogDownloader;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.utility.GutmaLogStorage;
import com.parrot.drone.sdkcore.flightlogconverter.FlightLogConverter;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_GUTMALOG;

/**
 * Producer of GUTMA log files.
 * <p>
 * Listens to flight log downloader, converts flight log files to GUTMA files and notifies GUTMA logs storage.
 */
public final class GutmaLogProducer implements HttpFlightLogDownloader.Converter {

    /** Extension of GUTMA log files. */
    private static final String GUTMA_EXTENSION = ".gutma";

    /** Extension of flight log files. */
    private static final String FDR_EXTENSION = ".bin";

    /** GUTMA logs storage. */
    @NonNull
    private final GutmaLogStorage mStorage;

    /**
     * Creates a new {@code GutmaLogProducer} instance.
     *
     * @param controller device controller
     *
     * @return a new {@code GutmaLogProducer} instance if a {@link GutmaLogStorage GUTMA logs storage utility} exists,
     *         otherwise {@code null}
     */
    @Nullable
    public static GutmaLogProducer create(@NonNull DeviceController controller) {
        GutmaLogStorage storage = controller.getEngine().getUtility(GutmaLogStorage.class);
        return storage == null ? null : new GutmaLogProducer(storage);
    }

    /**
     * Constructor.
     *
     * @param storage GUTMA logs storage interface
     */
    private GutmaLogProducer(@NonNull GutmaLogStorage storage) {
        mStorage = storage;
    }

    @Override
    public void onFlightLogDownloaded(@NonNull File flightLog) {
        try {
            Files.makeDirectories(mStorage.getWorkDir());
        } catch (IOException e) {
            ULog.w(TAG_GUTMALOG, "Failed to create directory for GUTMA log files " + mStorage.getWorkDir());
            return;
        }
        String gutmaFileName = flightLog.getName().replace(FDR_EXTENSION, "") + GUTMA_EXTENSION;
        File gutmaFile = new File(mStorage.getWorkDir(), gutmaFileName);
        boolean success = FlightLogConverter.toGutma(flightLog, gutmaFile);
        if (success) {
            ULog.d(TAG_GUTMALOG, "GUTMA log file created: " + gutmaFile);
            Executor.postOnMainThread(() -> mStorage.notifyGutmaLogFileReady(gutmaFile));
        } else {
            ULog.w(TAG_GUTMALOG, "Failed to convert flight log file to GUTMA log file: " + flightLog);
        }
    }
}
