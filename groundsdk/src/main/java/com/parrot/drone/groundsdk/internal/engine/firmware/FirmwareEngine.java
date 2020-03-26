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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.facility.FirmwareManagerCore;
import com.parrot.drone.groundsdk.internal.http.HttpFirmwaresInfo;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.internal.utility.FirmwareBlackList;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FIRMWARE;

/**
 * Engine that manages device firmwares.
 * <p>
 * Maintains both locally downloaded firmwares and remote firmwares available for download from the update server. <br>
 * Downloads firmwares from the update server if required.
 */
public class FirmwareEngine extends EngineBase {

    /** Interval between automatic queries of update information on remote server. */
    private static final long AUTOMATIC_REMOTE_QUERY_INTERVAL = TimeUnit.DAYS.toMillis(7);

    /** Minimum interval between queries of update information on remote server. */
    private static final long MINIMUM_REMOTE_QUERY_INTERVAL = TimeUnit.HOURS.toMillis(1);

    /** Persistence layer. */
    @NonNull
    private final Persistence mPersistence;

    /** Firmware store. */
    @NonNull
    private final FirmwareStoreCore mFirmwareStore;

    /** Downloads remote firmware update files. */
    @NonNull
    private final FirmwareDownloaderCore mDownloader;

    /** HTTP update client, {@code null} unless internet is available. */
    @Nullable
    private HttpUpdateClient mHttpClient;

    /** Firmware blacklist. */
    @NonNull
    private final FirmwareBlackListCore mFirmwareBlackList;

    /** FirmwareManager facility for which this object is the backend. */
    @NonNull
    private final FirmwareManagerCore mFirmwareManager;

    /** Supported device models, from config. */
    @NonNull
    private final Set<DeviceModel> mSupportedModels;

    /** Current request of update information on remote server. */
    @Nullable
    private HttpRequest mCurrentRequest;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public FirmwareEngine(@NonNull Controller controller) {
        super(controller);
        Context context = getContext();
        mPersistence = new Persistence(context);
        mFirmwareStore = new FirmwareStoreCore(this);
        mFirmwareBlackList = new FirmwareBlackListCore(mPersistence);
        mFirmwareManager = new FirmwareManagerCore(getFacilityPublisher(), mBackend);
        mDownloader = new FirmwareDownloaderCore(this);
        mSupportedModels = GroundSdkConfig.get(context).getSupportedDevices();
        publishUtility(FirmwareStore.class, mFirmwareStore);
        publishUtility(FirmwareBlackList.class, mFirmwareBlackList);
        publishUtility(FirmwareDownloader.class, mDownloader);
    }

    @Override
    protected void onStart() {
        mFirmwareStore.monitorWith(mStoreMonitor);
        mStoreMonitor.onChange();
        getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);
    }

    @Override
    protected void onAllEnginesStarted() {
        mFirmwareStore.pruneObsoleteFirmwares();
        mFirmwareManager.publish();
    }

    @Override
    protected void onStopRequested() {
        getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);
        acknowledgeStopRequest();
    }

    @Override
    protected void onStop() {
        mFirmwareManager.unpublish();
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
        mFirmwareStore.disposeMonitor(mStoreMonitor);
    }

    /** Monitors firmware store changes. */
    private final FirmwareStore.Monitor mStoreMonitor = new FirmwareStore.Monitor() {

        @Override
        public void onChange() {
            mFirmwareManager.updateEntries(mFirmwareStore.allEntries().stream().map(entry -> {
                URI localUri = entry.getLocalUri();
                return mFirmwareManager.new EntryCore(entry.getFirmwareInfo(), localUri != null,
                        localUri != null && localUri.getScheme().equals(Schemes.FILE));
            }).collect(Collectors.toList())).notifyUpdated();
        }
    };

    /**
     * Gives access to firmware engine persistence layer.
     *
     * @return firmware engine persistence layer
     */
    @NonNull
    Persistence persistence() {
        return mPersistence;
    }

    /**
     * Gives access to firmware store.
     *
     * @return firmware store
     */
    @NonNull
    FirmwareStoreCore firmwareStore() {
        return mFirmwareStore;
    }

    /**
     * Gives access to HTTP update client.
     *
     * @return HTTP update client
     */
    @Nullable
    HttpUpdateClient httpClient() {
        return mHttpClient;
    }

    /**
     * Fetches fresh update information from remote server.
     * <p>
     * This method fetches such information unconditionally, callers are responsible to check for appropriate allowed
     * remote query intervals beforehand.
     */
    private void fetchRemoteUpdateInfo() {
        if (mHttpClient == null || mCurrentRequest != null) {
            return;
        }
        mFirmwareManager.updateRemoteQueryFlag(true).notifyUpdated();
        mCurrentRequest = mHttpClient.listAvailableFirmwares(mSupportedModels, (status, code, firmwaresInfo) -> {
            mCurrentRequest = null;
            mFirmwareManager.updateRemoteQueryFlag(false);
            if (status == HttpRequest.Status.SUCCESS) {
                mPersistence.saveLastRemoteUpdateTime(System.currentTimeMillis());
                assert firmwaresInfo != null;
                Map<FirmwareIdentifier, FirmwareStoreEntry> remoteEntries = new HashMap<>();
                int index = 0;
                for (HttpFirmwaresInfo.Firmware httpFirmware : firmwaresInfo.getFirmwares()) {
                    index++;
                    try {
                        FirmwareStoreEntry entry = FirmwareStoreEntry.from(httpFirmware);
                        remoteEntries.put(entry.getFirmwareInfo().getFirmware(), entry);
                    } catch (JsonParseException e) {
                        ULog.w(TAG_FIRMWARE, "Failed to parse received HTTP firmware info #" + index, e);
                    }
                }

                mFirmwareStore.mergeRemoteFirmwares(remoteEntries);

                Set<FirmwareIdentifier> remoteBlackList = new HashSet<>();
                index = 0;
                for (HttpFirmwaresInfo.BlackListEntry httpBlackListEntry : firmwaresInfo.getBlacklist()) {
                    index++;
                    try {
                        FirmwareBlackListAdapter.addVersions(remoteBlackList, httpBlackListEntry);
                    } catch (JsonParseException e) {
                        ULog.w(TAG_FIRMWARE, "Failed to parse received HTTP blacklist entry #" + index, e);
                    }
                }
                mFirmwareBlackList.addToBlackList(remoteBlackList);
            }
            mFirmwareManager.notifyUpdated();
        });
    }

    /**
     * Tells whether update information from remote server is older than a given time.
     *
     * @param time time interval to compare, in milliseconds
     *
     * @return {@code true} if time since last received update information from remote server is unknown or is bigger
     *         than {@code time}, {@code false} otherwise
     */
    private boolean remoteUpdateOlderThan(long time) {
        long timeDifference = System.currentTimeMillis() - mPersistence.loadLastRemoteUpdateTime();
        return (timeDifference < 0) || (timeDifference > time);
    }

    /** Backend of FirmwareManagerCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final FirmwareManagerCore.Backend mBackend = new FirmwareManagerCore.Backend() {

        @Override
        public void queryRemoteFirmwares() {
            if (remoteUpdateOlderThan(MINIMUM_REMOTE_QUERY_INTERVAL)) {
                fetchRemoteUpdateInfo();
            }
        }

        @Override
        public boolean delete(@NonNull FirmwareIdentifier firmware) {
            return mFirmwareStore.deleteLocalFirmware(firmware);
        }

        @Override
        public void download(@NonNull FirmwareInfo firmware, @NonNull FirmwareDownloader.Task.Observer observer) {
            mDownloader.download(Collections.singleton(firmware), observer);
        }
    };

    /** Listens to Internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = available -> {
        if (available) {
            mHttpClient = createHttpClient();
            if (remoteUpdateOlderThan(AUTOMATIC_REMOTE_QUERY_INTERVAL)) {
                fetchRemoteUpdateInfo();
            }
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    };

    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        mFirmwareStore.dump(writer, args);
    }

    /**
     * Creates the HTTP update client.
     * <p>
     * This method only exists for mocking purposes in test cases.
     *
     * @return a new HTTP update client instance
     */
    @VisibleForTesting
    HttpUpdateClient createHttpClient() {
        return new HttpUpdateClient(getContext());
    }
}
