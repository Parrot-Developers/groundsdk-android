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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.UpdaterCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * Abstract Updater implementation.
 */
public abstract class UpdaterController extends PeripheralController<DeviceController<?>> {

    /**
     * Creates an {@code UpdaterController} instance.
     *
     * @param deviceController device controller that manages this peripheral controller
     * @param protocolFactory  factory for creating protocol-dependent firmware updater service
     * @param <D>              type of {@code deviceController} instance
     *
     * @return a new {@code UpdaterController instance}, or {@code null} in case it could not be created, for instance
     *         if firmware engine is disabled in groundsdk {@link GroundSdkConfig config}
     */
    @Nullable
    public static <D extends DeviceController<?>> UpdaterController create(
            @NonNull D deviceController,
            @NonNull Function<D, FirmwareUpdaterProtocol> protocolFactory) {
        ArsdkEngine engine = deviceController.getEngine();
        FirmwareStore firmwareStore = engine.getUtility(FirmwareStore.class);
        FirmwareDownloader firmwareDownloader = engine.getUtility(FirmwareDownloader.class);
        if (firmwareStore != null && firmwareDownloader != null) {

            DeviceModel model = deviceController.getDevice().getModel();
            if (model instanceof Drone.Model) {
                return new DroneUpdaterController(deviceController, firmwareStore, firmwareDownloader,
                        protocolFactory.apply(deviceController));
            } else if (model instanceof RemoteControl.Model) {
                return new RcUpdaterController(deviceController, firmwareStore, firmwareDownloader,
                        protocolFactory.apply(deviceController));
            }
        }
        return null;
    }

    /** Updater peripheral for which this object is the backend. */
    @NonNull
    private final UpdaterCore mUpdater;

    /** Firmware store utility. */
    @NonNull
    private final FirmwareStore mFirmwareStore;

    /** Firmware downloader utility. */
    @NonNull
    private final FirmwareDownloader mFirmwareDownloader;

    /** Protocol-dependent firmware updater service. */
    @NonNull
    private final FirmwareUpdaterProtocol mUpdaterProtocol;

    /** System connectivity utility. */
    @NonNull
    private final SystemConnectivity mConnectivity;

    /**
     * Queue of firmwares that must be applied. Maintained across device reboot/reconnection to allow automated updating
     * with multiple firmware in sequence.
     */
    @NonNull
    private final Queue<FirmwareInfo> mUpdateQueue;

    /** Collects reasons why applying updates may currently be impossible. */
    @NonNull
    private final EnumSet<Updater.Update.UnavailabilityReason> mUpdateUnavailabilityReasons;

    /** Current firmware update request. {@code null} when no firmware update is being applied. */
    @Nullable
    private Cancelable mCurrentUpdate;

    /**
     * Constructor.
     *
     * @param deviceController   the device controller that owns this peripheral controller.
     * @param firmwareStore      firmware store providing remotely and locally available firmwares
     * @param firmwareDownloader firmware downloader allowing to download remote firmware to local storage
     * @param updaterProtocol    firmware updater service, used to apply firmware updates to the device
     */
    UpdaterController(@NonNull DeviceController deviceController, @NonNull FirmwareStore firmwareStore,
                      @NonNull FirmwareDownloader firmwareDownloader,
                      @NonNull FirmwareUpdaterProtocol updaterProtocol) {
        super(deviceController);
        mFirmwareStore = firmwareStore;
        mFirmwareDownloader = firmwareDownloader;
        mUpdaterProtocol = updaterProtocol;
        mUpdater = new UpdaterCore(mComponentStore, mBackend);
        mUpdateQueue = new LinkedList<>();
        mConnectivity = deviceController.getEngine().getUtilityOrThrow(SystemConnectivity.class);
        mUpdateUnavailabilityReasons = EnumSet.noneOf(Updater.Update.UnavailabilityReason.class);

        mConnectivity.monitorWith(mInternetMonitor);
        processInternetAvailability(mConnectivity.isInternetAvailable());

        mFirmwareStore.monitorWith(mFirmwaresMonitor);
        processFirmwareInfos();

        onUnavailabilityReason(Updater.Update.UnavailabilityReason.NOT_CONNECTED, true);

        if (!deviceController.getDeviceDict().isNew()) {
            mUpdater.publish();
        }
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        onUnavailabilityReason(Updater.Update.UnavailabilityReason.NOT_CONNECTED, false);
        // compute up-to-date update info (device firmware may have changed)
        processFirmwareInfos();

        FirmwareInfo expected = mUpdateQueue.poll();
        if (expected != null) {
            if (!mDeviceController.getDevice().getFirmwareVersion().equals(expected.getFirmware().getVersion())) {
                // inconsistent, mark update failed
                onUpdateEnd(Updater.Update.State.FAILED);
            } else if (mUpdateQueue.isEmpty()) {
                // all done, success
                onUpdateEnd(Updater.Update.State.SUCCESS);
            } else if (mUpdateUnavailabilityReasons.isEmpty()) {
                // continue update
                mUpdater.continueUpdate();
                assert mUpdateQueue.peek() != null;
                mCurrentUpdate = mUpdaterProtocol.updateWith(mUpdateQueue.peek().getFirmware(), mFirmwareStore,
                        mUpdaterCallback);
            } else {
                // cannot continue, fail
                onUpdateEnd(Updater.Update.State.FAILED);
            }
        }
        mUpdater.publish();
    }

    @Override
    protected void onDisconnected() {
        mUpdateUnavailabilityReasons.clear();
        onUnavailabilityReason(Updater.Update.UnavailabilityReason.NOT_CONNECTED, true);

        if (!mUpdateQueue.isEmpty()) {
            mUpdater.updateUpdateState(Updater.Update.State.WAITING_FOR_REBOOT);
        }
        mUpdater.notifyUpdated();

        super.onDisconnected();
    }

    @Override
    protected void onForgetting() {
        mUpdater.unpublish();
        onUpdateEnd(Updater.Update.State.CANCELED);
        super.onForgetting();
    }

    @Override
    protected void onDispose() {
        mFirmwareStore.disposeMonitor(mFirmwaresMonitor);
        mConnectivity.disposeMonitor(mInternetMonitor);
        super.onDispose();
    }

    /**
     * Called when some update availability reason changes.
     *
     * @param reason update unavailability reason
     * @param holds  when {@code true}, signals that the unavailability {@code reason} holds and should be set; when
     *               {@code false}, signals that the unavailability {@code reason} does not hold and should be cleared
     *               instead
     */
    final void onUnavailabilityReason(@NonNull Updater.Update.UnavailabilityReason reason, boolean holds) {
        if (holds) {
            mUpdateUnavailabilityReasons.add(reason);
        } else {
            mUpdateUnavailabilityReasons.remove(reason);
        }
        mUpdater.updateUpdateUnavailabilityReasons(mUpdateUnavailabilityReasons);

        if (!mUpdateUnavailabilityReasons.isEmpty() && mCurrentUpdate != null) {
            mCurrentUpdate.cancel();
        }
    }

    /**
     * Requests a change notification on the component.
     * <p>
     * This does nothing unless the controller is {@link #isConnected() connected}.
     */
    final void notifyComponentChange() {
        if (isConnected()) {
            mUpdater.notifyUpdated();
        }
    }

    /**
     * Called when the update process ends.
     * <p>
     * This method first notifies the final status transiently by updating the status and {@link
     * UpdaterCore#notifyUpdated() notifying} the change, then it signals the end of the update by clearing the update
     * state and sending a second change notification.
     *
     * @param status final update status, either {@link Updater.Update.State#SUCCESS}, {@link
     *               Updater.Update.State#FAILED} or {@link Updater.Update.State#CANCELED}
     */
    private void onUpdateEnd(@NonNull Updater.Update.State status) {
        mUpdater.updateUpdateState(status)
                .notifyUpdated();
        mUpdater.endUpdate()
                .notifyUpdated();
        mUpdateQueue.clear();
    }

    /**
     * Processes firmware store content and current device firmware version to update downloadable and applicable
     * firmwares info.
     * <p>
     * This method may update the component state, but does <strong>NOT</strong> call {@link
     * UpdaterCore#notifyUpdated()}.
     */
    private void processFirmwareInfos() {
        DeviceCore device = mDeviceController.getDevice();
        FirmwareIdentifier firmware = new FirmwareIdentifier(device.getModel(), device.getFirmwareVersion());
        FirmwareInfo idealUpdate = mFirmwareStore.idealUpdateFor(firmware);
        mUpdater.updateDownloadableFirmwares(mFirmwareStore.downloadableUpdatesFor(firmware))
                .updateApplicableFirmwares(mFirmwareStore.applicableUpdatesFor(firmware))
                .updateIdealVersion(idealUpdate == null ? null : idealUpdate.getFirmware().getVersion());
    }

    /**
     * Processes internet availability state to update firmware download availability accordingly.
     * <p>
     * This method may update the component state, but does <strong>NOT</strong> call {@link
     * UpdaterCore#notifyUpdated()}.
     *
     * @param available {@code true} if internet is currently available, otherwise {@code false}
     */
    private void processInternetAvailability(boolean available) {
        mUpdater.updateDownloadUnavailabilityReasons(available ?
                EnumSet.noneOf(Updater.Download.UnavailabilityReason.class) :
                EnumSet.of(Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE));
    }

    /** Backend of UpdaterCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final UpdaterCore.Backend mBackend = new UpdaterCore.Backend() {

        @Override
        public void download(@NonNull Collection<FirmwareInfo> firmwares,
                             @NonNull FirmwareDownloader.Task.Observer observer) {
            mFirmwareDownloader.download(firmwares, observer);
        }

        @Override
        public void updateWith(@NonNull Collection<FirmwareInfo> firmwares) {
            if (mCurrentUpdate != null || !mUpdateQueue.isEmpty() || firmwares.isEmpty()) {
                return;
            }
            Set<FirmwareInfo> uniqueFirmwares = new LinkedHashSet<>(firmwares);
            mUpdateQueue.addAll(uniqueFirmwares);
            mUpdater.beginUpdate(uniqueFirmwares);
            assert mUpdateQueue.peek() != null;
            mCurrentUpdate = mUpdaterProtocol.updateWith(mUpdateQueue.peek().getFirmware(), mFirmwareStore,
                    mUpdaterCallback);
            mUpdater.notifyUpdated();
        }

        @Override
        public void cancelUpdate() {
            if (mCurrentUpdate != null) {
                mCurrentUpdate.cancel();
            } else {
                onUpdateEnd(Updater.Update.State.CANCELED);
            }
        }
    };

    /** Receives ongoing update state change notifications. */
    private final FirmwareUpdaterProtocol.Callback mUpdaterCallback = new FirmwareUpdaterProtocol.Callback() {

        @Override
        public void onUploadProgress(int progress) {
            mUpdater.updateUploadProgress(progress);
            if (progress == 100) {
                mUpdater.updateUpdateState(Updater.Update.State.PROCESSING);
            }
            mUpdater.notifyUpdated();
        }

        @Override
        public void onUpdateEnd(@NonNull Status status) {
            mCurrentUpdate = null;
            switch (status) {
                case SUCCESS:
                    // nothing to do, just wait for reboot.
                    break;
                case FAILED:
                    UpdaterController.this.onUpdateEnd(Updater.Update.State.FAILED);
                    break;
                case CANCELED:
                    UpdaterController.this.onUpdateEnd(Updater.Update.State.CANCELED);
                    break;
            }
        }
    };

    /** Receives firmware store change notifications. */
    @NonNull
    private final FirmwareStore.Monitor mFirmwaresMonitor = new FirmwareStore.Monitor() {

        @Override
        public void onChange() {
            processFirmwareInfos();
            mUpdater.notifyUpdated();
        }
    };

    /** Receives internet connectivity state change notifications. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = new SystemConnectivity.Monitor() {

        @Override
        public void onInternetAvailabilityChanged(boolean availableNow) {
            processInternetAvailability(availableNow);
            mUpdater.notifyUpdated();
        }
    };
}
