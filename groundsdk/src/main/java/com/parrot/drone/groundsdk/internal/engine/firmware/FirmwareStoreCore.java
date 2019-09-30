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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FIRMWARE;

/**
 * Store for all known firmwares, either present locally or known to be available for download from the update server.
 * <p>
 * This class is also the implementation class for the {@code FirmwareStore} utility.
 */
public class FirmwareStoreCore implements FirmwareStore {

    /** Sorts {@link FirmwareStoreEntry} by version. Most recent versions last. */
    private static final Comparator<FirmwareStoreEntry> ASCENDING_VERSION = (lhs, rhs) ->
            lhs.getFirmwareInfo().getFirmware().getVersion().compareTo(
                    rhs.getFirmwareInfo().getFirmware().getVersion());

    /** Sorts {@link FirmwareStoreEntry} by version. Most recent versions first. */
    private static final Comparator<FirmwareStoreEntry> DESCENDING_VERSION = (lhs, rhs) ->
            rhs.getFirmwareInfo().getFirmware().getVersion().compareTo(
                    lhs.getFirmwareInfo().getFirmware().getVersion());

    /** Firmware engine. */
    @NonNull
    private final FirmwareEngine mEngine;

    /** All registered monitors of the store. */
    @NonNull
    private final Set<Monitor> mMonitors;

    /** All known firmwares. */
    private final Map<FirmwareIdentifier, FirmwareStoreEntry> mUpdates;


    /**
     * Constructor.
     *
     * @param engine firmware engine
     */
    FirmwareStoreCore(@NonNull FirmwareEngine engine) {
        mEngine = engine;
        mMonitors = new HashSet<>();
        mUpdates = mEngine.persistence().loadFirmwares();
    }

    @Override
    public void monitorWith(@NonNull Monitor monitor) {
        mMonitors.add(monitor);
    }

    @Override
    public void disposeMonitor(@NonNull Monitor monitor) {
        mMonitors.remove(monitor);
    }

    @Override
    @NonNull
    public List<FirmwareInfo> applicableUpdatesFor(@NonNull FirmwareIdentifier firmware) {
        return getUpdateChain(firmware, true)
                .stream()
                .map(FirmwareStoreEntry::getFirmwareInfo)
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<FirmwareInfo> downloadableUpdatesFor(@NonNull FirmwareIdentifier firmware) {
        return getUpdateChain(firmware, false)
                .stream().filter(entry -> entry.getLocalUri() == null)
                .map(FirmwareStoreEntry::getFirmwareInfo)
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public FirmwareInfo idealUpdateFor(@NonNull FirmwareIdentifier firmware) {
        SortedSet<FirmwareStoreEntry> chain = getUpdateChain(firmware, false);
        if (!chain.isEmpty()) {
            return chain.last().getFirmwareInfo();
        }
        return null;
    }

    @Nullable
    @Override
    public InputStream getFirmwareStream(@NonNull FirmwareIdentifier firmware) {
        FirmwareStoreEntry entry = mUpdates.get(firmware);
        if (entry == null) {
            return null;
        }
        URI uri = entry.getLocalUri();
        return uri == null ? null : mEngine.persistence().getFirmwareStream(uri);
    }

    @NonNull
    @Override
    public Task<File> getFirmwareFile(@NonNull FirmwareIdentifier firmware) {
        FirmwareStoreEntry entry = mUpdates.get(firmware);
        if (entry == null) {
            return Task.failure(new FileNotFoundException("Unknown firmware:" + firmware));
        }
        URI firmwareUri = entry.getLocalUri();
        if (firmwareUri == null) {
            return Task.failure(new FileNotFoundException("Firmware is not available locally: " + firmware));
        }
        if (firmwareUri.getScheme().equals(Schemes.FILE)) {
            return Task.success(new File(firmwareUri.getPath()));
        }
        Persistence persistence = mEngine.persistence();
        InputStream stream = persistence.getFirmwareStream(firmwareUri);
        if (stream == null) {
            return Task.failure(new FileNotFoundException("Firmware is not available locally" + firmware));
        }

        File dest = persistence.makeLocalFirmwarePath(firmware, firmwareUri);

        return Executor.runInBackground(() -> {
            // TODO maybe check if file already exists (we have length & md5 to check properly).
            Files.writeFile(stream, dest);
            return dest;
        }).whenComplete((result, error, canceled) -> {
            if (error == null && !canceled) {
                if (entry.setUri(dest.toURI())) {
                    storeChanged();
                }
            }
        });
    }

    /**
     * Prune all local firmwares that can be considered obsolete.
     */
    void pruneObsoleteFirmwares() {
        if (removeObsoleteFirmwares()) {
            storeChanged();
        }
    }

    /**
     * Retrieves an entry from the store.
     *
     * @param firmware firmware to get the corresponding entry of
     *
     * @return the firmware entry if exists, otherwise {@code null}
     */
    @Nullable
    FirmwareStoreEntry getEntry(@NonNull FirmwareIdentifier firmware) {
        return mUpdates.get(firmware);
    }

    /**
     * Retrieves all entries from the store.
     * <p>
     * Returned collection cannot be modified.
     *
     * @return a collection of all entries in the store
     */
    @NonNull
    Collection<FirmwareStoreEntry> allEntries() {
        return Collections.unmodifiableCollection(mUpdates.values());
    }

    /**
     * Retrieves all update entries that may be applied consecutively to update a given device firmware to the latest
     * known version.
     * <p>
     * The returned set contains the latest firmware entry that is available to update the given firmware , plus all
     * other firmware entries that are required to be applied before.
     * <p>
     * Entries in the set are sorted by application order, first entries should be applied before subsequent entries.
     * <p>
     * In case the {@code localOnly} parameter is set to {@code false}, entries in the set might be only remotely
     * available and corresponding firmware update files should be downloaded before application.
     *
     * @param firmware  firmware to update
     * @param localOnly {@code true} to disregard firmwares that are only remotely available when building the update
     *                  chain, {@code false} otherwise
     *
     * @return a set of all update entries that should be applied to update the firmware to the latest known version.
     *         Possibly empty
     */
    @NonNull
    SortedSet<FirmwareStoreEntry> getUpdateChain(@NonNull FirmwareIdentifier firmware, boolean localOnly) {
        SortedSet<FirmwareStoreEntry> entries = new TreeSet<>(ASCENDING_VERSION);
        while (firmware != null) {
            NavigableSet<FirmwareStoreEntry> suitable = listSuitableEntriesFrom(firmware);
            if (localOnly) {
                suitable.removeIf((entry) -> entry.getLocalUri() == null);
            }
            FirmwareStoreEntry entry = suitable.pollFirst();
            if (entry != null) {
                entries.add(entry);
                firmware = entry.getFirmwareInfo().getFirmware();
            } else {
                firmware = null;
            }
        }
        return entries;
    }

    /**
     * Attaches a local URI to a firmware entry, making it available for application.
     *
     * @param firmware identifies the firmware entry to update
     * @param localUri local URI to attach to the entry
     */
    void addLocalFirmware(@NonNull FirmwareIdentifier firmware, @NonNull URI localUri) {
        FirmwareStoreEntry addedEntry = mUpdates.get(firmware);
        boolean changed = addedEntry != null && addedEntry.setUri(localUri);
        if (changed) {
            removeObsoleteFirmwares();
            storeChanged();
        }
    }

    /**
     * Deletes local firmware file and detaches local file URI from a firmware entry.
     *
     * @param firmware identifies the firmware entry whose firmware file must be deleted
     *
     * @return {@code true} if an existing local file URI was detached from the identified entry, otherwise
     *         {@code false}
     */
    boolean deleteLocalFirmware(@NonNull FirmwareIdentifier firmware) {
        FirmwareStoreEntry toDelete = mUpdates.get(firmware);
        URI localUri = toDelete == null ? null : toDelete.getLocalUri();
        if (localUri == null || !localUri.getScheme().equals(Schemes.FILE)) {
            return false;
        }
        File file = new File(localUri);
        if (ULog.i(TAG_FIRMWARE)) {
            ULog.i(TAG_FIRMWARE, "Deleting local firmware file [firmware: " + firmware + ", file: " + file + "]");
        }
        if (deleteFirmwareFile(file)) {
            if (toDelete.clearLocalUri()) {
                mUpdates.remove(firmware);
            }
            storeChanged();
            return true;
        }
        return false;
    }

    /**
     * Merges remote firmware info to the store.
     *
     * @param remoteEntries remote entries to merge in the store
     */
    void mergeRemoteFirmwares(@NonNull Map<FirmwareIdentifier, FirmwareStoreEntry> remoteEntries) {
        boolean changed = false;
        for (Iterator<FirmwareIdentifier> storeEntryIter = mUpdates.keySet().iterator(); storeEntryIter.hasNext(); ) {
            FirmwareStoreEntry storeEntry = mUpdates.get(storeEntryIter.next());
            assert storeEntry != null;
            FirmwareStoreEntry matchingRemote = remoteEntries.remove(storeEntry.getFirmwareInfo().getFirmware());
            if (matchingRemote != null) {
                // merge http uris from remote
                URI remoteUri = matchingRemote.getRemoteUri();
                assert remoteUri != null;
                changed |= storeEntry.setUri(remoteUri);
            } else if (storeEntry.clearRemoteUri()) { // remove remote uri from store entry
                // no uris left for entry, remove it completely
                storeEntryIter.remove();
                changed = true;
            }
        }
        // what remains in remoteEntries is only new entries to be added
        changed |= !remoteEntries.isEmpty();
        mUpdates.putAll(remoteEntries);

        if (changed) {
            storeChanged();
        }
    }

    /**
     * Lists all entries which are suitable for updating a given firmware.
     * <p>
     * The returned set will only contain entries: <ul>
     * <li>that apply on the model of the specified {@code firmware},</li>
     * <li>whose {@link FirmwareIdentifier#getVersion() version} is strictly higher than the version of the
     * specified {@code firmware},</li>
     * <li>whose {@link FirmwareStoreEntry#getMaxApplicableVersion() max version} is higher than or equal to the
     * version of the specified {@code firmware},
     * <li>whose {@link FirmwareStoreEntry#getMinApplicableVersion() min version} is lower than or equal to the
     * version of the specified {@code firmware}.
     * </ul>
     * Note however that the returned set may contain entries that are not available
     * {@link FirmwareStoreEntry#getLocalUri() locally}.
     * <p>
     * Entries in the set are sorted by descending version order: first entries have higher versions than subsequent
     * entries.
     * <p>
     * The returned set can be modified.
     *
     * @param firmware firmware to list suitable updates of
     *
     * @return a set of all entries that are suitable for updating the specified firmware
     */
    @NonNull
    private NavigableSet<FirmwareStoreEntry> listSuitableEntriesFrom(@NonNull FirmwareIdentifier firmware) {
        FirmwareVersion version = firmware.getVersion();
        DeviceModel model = firmware.getDeviceModel();
        return mUpdates.values().stream().filter(entry -> {
            FirmwareIdentifier entryFirmware = entry.getFirmwareInfo().getFirmware();
            FirmwareVersion minVersion = entry.getMinApplicableVersion();
            FirmwareVersion maxVersion = entry.getMaxApplicableVersion();
            return entryFirmware.getDeviceModel().equals(model)
                   && entryFirmware.getVersion().compareTo(version) > 0
                   && (minVersion == null || minVersion.compareTo(version) <= 0)
                   && (maxVersion == null || maxVersion.compareTo(version) >= 0);
        }).collect(Collectors.toCollection(() -> new TreeSet<>(DESCENDING_VERSION)));
    }

    /**
     * Deletes obsolete local firmwares.
     * <p>
     * This removes from local storage all firmwares that are not needed anymore in order to update any drone known
     * to groundsdk.
     * <p>
     * Note that this method may perform changes on the store; caller has the responsibility to publish appropriate
     * update notifications after having called this method.
     *
     * @return {@code true} if the store did change after this operation, otherwise {@code false}
     */
    private boolean removeObsoleteFirmwares() {
        Set<FirmwareStoreEntry> toKeep = new HashSet<>();
        for (DeviceStore<?> store : Arrays.asList(
                mEngine.getUtilityOrThrow(DroneStore.class),
                mEngine.getUtilityOrThrow(RemoteControlStore.class))) {
            for (DeviceCore device : store.all()) {
                toKeep.addAll(getUpdateChain(
                        new FirmwareIdentifier(device.getModel(), device.getFirmwareVersion()), true));
            }
        }
        boolean storeChanged = false;
        for (Iterator<FirmwareStoreEntry> storeEntryIter = mUpdates.values().iterator(); storeEntryIter.hasNext(); ) {
            FirmwareStoreEntry entry = storeEntryIter.next();
            if (toKeep.contains(entry)) {
                continue;
            }
            URI localUri = entry.getLocalUri();
            if (localUri == null || !localUri.getScheme().equals(Schemes.FILE)) {
                continue;
            }
            File file = new File(localUri);
            if (file.lastModified() > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
                continue;
            }
            if (ULog.i(TAG_FIRMWARE)) {
                ULog.i(TAG_FIRMWARE, "Pruning obsolete local firmware file [firmware: "
                                     + entry.getFirmwareInfo().getFirmware() + ", file: " + file + "]");
            }
            if (deleteFirmwareFile(file)) {
                if (entry.clearLocalUri()) {
                    storeEntryIter.remove();
                }
                storeChanged = true;
            }
        }
        return storeChanged;
    }

    /**
     * Called when store data changes.
     * <p>
     * Persist store data and notifies all monitors.
     */
    private void storeChanged() {
        mEngine.persistence().saveFirmwares(mUpdates.values());
        for (Monitor monitor : mMonitors) {
            monitor.onChange();
        }
    }

    /**
     * Deletes a local firmware file from file system.
     *
     * @param firmware file that must be deleted
     *
     * @return {@code true} if an existing firmware file was properly deleted, otherwise {@code false}
     */
    private static boolean deleteFirmwareFile(@NonNull File firmware) {
        if (!firmware.exists() || firmware.delete()) {
            return true;
        } else if (ULog.w(TAG_FIRMWARE)) {
            ULog.w(TAG_FIRMWARE, "Could not delete firmware update file [path:" + firmware + "]");
        }
        return false;
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--firmwares: dumps firmware store\n");
        } else if (args.contains("--firmwares") || args.contains("--all")) {
            writer.write("Firmwares: " + mUpdates.size() + "\n");
            for (FirmwareStoreEntry entry : mUpdates.values()) {
                FirmwareInfoCore info = entry.getFirmwareInfo();
                writer.write("\t" + info.getFirmware() + "\n");
                writer.write("\t\tsize: " + info.getSize() + "\n");
                writer.write("\t\tchecksum: " + info.getChecksum() + "\n");
                if (entry.getRemoteUri() != null) {
                    writer.write("\t\tremote: " + entry.getRemoteUri() + "\n");
                }
                if (entry.getLocalUri() != null) {
                    writer.write("\t\t" + (entry.isPreset() ? "preset" : "local") + ": " + entry.getLocalUri() + "\n");
                }
                EnumSet<FirmwareInfo.Attribute> attributes = info.getAttributes();
                if (!attributes.isEmpty()) {
                    writer.write("\t\tattributes: " + attributes + "\n");
                }
                if (entry.getMinApplicableVersion() != null) {
                    writer.write("\t\tmin-version: " + entry.getMinApplicableVersion() + "\n");
                }
                if (entry.getMaxApplicableVersion() != null) {
                    writer.write("\t\tmax-version: " + entry.getMaxApplicableVersion() + "\n");
                }
            }
        }
    }
}
