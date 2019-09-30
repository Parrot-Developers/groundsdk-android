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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateCurrentFirmwareIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateIndexIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateStateIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateTotalCountIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdaterControllerTests extends ArsdkEngineTestBase {

    private static final DeviceModel MODEL = Drone.Model.ANAFI_4K;

    @SuppressWarnings("ConstantConditions")
    private static final FirmwareIdentifier[] VERSIONS = new FirmwareIdentifier[] {
            new FirmwareIdentifier(MODEL, FirmwareVersion.parse("1.0.0")),
            new FirmwareIdentifier(MODEL, FirmwareVersion.parse("2.0.0")),
            new FirmwareIdentifier(MODEL, FirmwareVersion.parse("3.0.0")),
            new FirmwareIdentifier(MODEL, FirmwareVersion.parse("4.0.0"))};

    private static final FirmwareInfo[] FIRMWARES = new FirmwareInfo[] {
            when(mock(FirmwareInfo.class).getFirmware()).thenReturn(VERSIONS[0]).getMock(),
            when(mock(FirmwareInfo.class).getFirmware()).thenReturn(VERSIONS[1]).getMock(),
            when(mock(FirmwareInfo.class).getFirmware()).thenReturn(VERSIONS[2]).getMock(),
            when(mock(FirmwareInfo.class).getFirmware()).thenReturn(VERSIONS[3]).getMock()};

    private FirmwareStore mMockFirmwareStore;

    private FirmwareDownloader mMockFirmwareDownloader;

    private HttpUpdateClient mMockUpdateClient;

    private DroneCore mDrone;

    private Updater mUpdater;

    private int mChangeCnt;

    private Queue<Runnable> mOnChangeRunnables;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Override
    public void setUp() {
        super.setUp();

        mMockFirmwareStore = mock(FirmwareStore.class);
        mMockFirmwareDownloader = mock(FirmwareDownloader.class);
        mUtilities.registerUtility(FirmwareStore.class, mMockFirmwareStore)
                  .registerUtility(FirmwareDownloader.class, mMockFirmwareDownloader);

        mMockUpdateClient = mock(HttpUpdateClient.class);
        MockHttpSession.registerOnly(mMockUpdateClient);

        mArsdkEngine.start();
        clearInvocations(mMockConnectivity); // otherwise we get polluted since engine registers a monitor on start.
        mMockArsdkCore.addDevice("123", MODEL.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mUpdater = mDrone.getPeripheralStore().get(mMockSession, Updater.class);
        mDrone.getPeripheralStore().registerObserver(Updater.class, () -> {
            mUpdater = mDrone.getPeripheralStore().get(mMockSession, Updater.class);
            mChangeCnt++;
            Runnable r = mOnChangeRunnables.poll();
            if (r != null) {
                r.run();
            }
        });

        mOnChangeRunnables = new LinkedList<>();
        mChangeCnt = 0;
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        super.teardown();
    }

    @Test
    public void testPublication() {
        // test that updater is not available when not connected and not known
        assertThat(mChangeCnt, is(0));
        assertThat(mUpdater, nullValue());

        // test that updater becomes available after connection
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater, notNullValue());
        assertThat(mUpdater.updateUnavailabilityReasons(), empty());

        // test that updater is still available after disconnection
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater, notNullValue());
        assertThat(mUpdater.updateUnavailabilityReasons(), containsInAnyOrder(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED));

        // test that updater becomes unavailable after forget
        mDrone.forget();

        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater, nullValue());
    }

    @Test
    public void testInternetAvailability() {
        ArgumentCaptor<SystemConnectivity.Monitor> connectivityMonitorCaptor = ArgumentCaptor.forClass(
                SystemConnectivity.Monitor.class);

        // monitor should be registered
        verify(mMockConnectivity).monitorWith(connectivityMonitorCaptor.capture());

        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater.downloadUnavailabilityReasons(), containsInAnyOrder(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE));

        // mock internet becomes available
        doReturn(true).when(mMockConnectivity).isInternetAvailable();
        connectivityMonitorCaptor.getValue().onInternetAvailabilityChanged(true);

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.downloadUnavailabilityReasons(), empty());

        // mock internet becomes unavailable
        doReturn(false).when(mMockConnectivity).isInternetAvailable();
        connectivityMonitorCaptor.getValue().onInternetAvailabilityChanged(false);

        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.downloadUnavailabilityReasons(), containsInAnyOrder(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE));

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // internet should still be monitored
        verify(mMockConnectivity, never()).disposeMonitor(connectivityMonitorCaptor.getValue());

        // dispose drone
        mMockArsdkCore.removeDevice(1);
        mDrone.forget();

        // monitor should be unregistered
        verify(mMockConnectivity).disposeMonitor(connectivityMonitorCaptor.getValue());
    }

    @Test
    public void testDeviceFirmwareAndStoreUpdates() {
        ArgumentCaptor<FirmwareStore.Monitor> storeMonitorCaptor = ArgumentCaptor.forClass(
                FirmwareStore.Monitor.class);

        // monitor should be registered
        verify(mMockFirmwareStore).monitorWith(storeMonitorCaptor.capture());

        // mock receiving firmware version
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(VERSIONS[0].getVersion().toString(), "")));

        // everything should be empty since the store is empty
        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.applicableFirmwares(), empty());
        assertThat(mUpdater.idealVersion(), nullValue());

        // mock downloadable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[2], FIRMWARES[3]))
                .when(mMockFirmwareStore)
                .downloadableUpdatesFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // downloadable firmwares should update
        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[2], FIRMWARES[3]));
        assertThat(mUpdater.applicableFirmwares(), empty());
        assertThat(mUpdater.idealVersion(), nullValue());

        // mock applicable firmwares in store
        doReturn(Collections.singletonList(FIRMWARES[1]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // applicable firmwares should update
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[2], FIRMWARES[3]));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[1]));
        assertThat(mUpdater.idealVersion(), nullValue());

        // mock ideal firmware in store
        doReturn(FIRMWARES[3])
                .when(mMockFirmwareStore)
                .idealUpdateFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // ideal firmware should update
        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[2], FIRMWARES[3]));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[1]));
        assertThat(mUpdater.idealVersion(), is(FIRMWARES[3].getFirmware().getVersion()));

        // mock downloadable firmwares change in store
        doReturn(Collections.singletonList(FIRMWARES[3]))
                .when(mMockFirmwareStore)
                .downloadableUpdatesFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // downloadable firmwares should update
        assertThat(mChangeCnt, is(5));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[3]));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[1]));
        assertThat(mUpdater.idealVersion(), is(FIRMWARES[3].getFirmware().getVersion()));

        // mock applicable firmwares change in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // applicable firmwares should update
        assertThat(mChangeCnt, is(6));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[3]));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[1], FIRMWARES[2]));
        assertThat(mUpdater.idealVersion(), is(FIRMWARES[3].getFirmware().getVersion()));

        // mock both downloadable and applicable firmwares change in store
        doReturn(Collections.emptyList())
                .when(mMockFirmwareStore)
                .downloadableUpdatesFor(VERSIONS[0]);
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2], FIRMWARES[3]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(VERSIONS[0]);
        storeMonitorCaptor.getValue().onChange();

        // downloadable and applicable firmwares should update
        assertThat(mChangeCnt, is(7));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[1], FIRMWARES[2], FIRMWARES[3]));
        assertThat(mUpdater.idealVersion(), is(FIRMWARES[3].getFirmware().getVersion()));

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // firmware store should still be monitored
        verify(mMockFirmwareStore, never()).disposeMonitor(storeMonitorCaptor.getValue());

        // dispose component
        mMockArsdkCore.removeDevice(1);
        mDrone.forget();

        // monitor should be unregistered
        verify(mMockFirmwareStore).disposeMonitor(storeMonitorCaptor.getValue());
    }

    @Test
    public void testDownload() {
        ArgumentCaptor<SystemConnectivity.Monitor> connectivityMonitorCaptor = ArgumentCaptor.forClass(
                SystemConnectivity.Monitor.class);
        verify(mMockConnectivity).monitorWith(connectivityMonitorCaptor.capture());

        ArgumentCaptor<FirmwareStore.Monitor> storeMonitorCaptor = ArgumentCaptor.forClass(
                FirmwareStore.Monitor.class);

        verify(mMockFirmwareStore).monitorWith(storeMonitorCaptor.capture());

        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock internet is available
        doReturn(true).when(mMockConnectivity).isInternetAvailable();
        connectivityMonitorCaptor.getValue().onInternetAvailabilityChanged(true);

        assertThat(mChangeCnt, is(2));

        // mock downloadable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .downloadableUpdatesFor(any());
        storeMonitorCaptor.getValue().onChange();

        assertThat(mChangeCnt, is(3));

        // now request a download
        mUpdater.downloadAllFirmwares();

        verify(mMockFirmwareDownloader).download(eq(Arrays.asList(FIRMWARES[1], FIRMWARES[2])), any());
        // firmware downloader has its own test cases.
        // firmware download task state/updates is tested in groundsdk tests
    }

    @Test
    public void testStartUpdate() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        // now request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2)));
    }

    @Test
    public void testOngoingUpdateCancel() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // cancel update
        mUpdater.cancelUpdate();

        verify(request).cancel();

        onNextChange(() -> {
            // whole update task should be canceled
            assertThat(mChangeCnt, is(3));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.CANCELED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(2)));
        });

        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testWaitingUpdateCancel() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // cancel update
        onNextChange(() -> {
            // whole update task should be canceled
            assertThat(mChangeCnt, is(3));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.CANCELED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(2)));
        });

        mUpdater.cancelUpdate();

        verify(request, never()).cancel(); // should not be canceled, since already finished

        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testWaitingUpdateSuccessAtReconnection() {
        assertThat(mChangeCnt, is(0));
        // mock a single applicable firmwares in store
        doReturn(Collections.singletonList(FIRMWARES[1]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // status should switch to WAITING_FOR_REBOOT
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1)));

        // mock reconnection, with expected FIRMWARE[1]
        onNextChange(() -> {
            // whole update task should be SUCCESS
            assertThat(mChangeCnt, is(4));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.SUCCESS),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(1)));
        });

        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(VERSIONS[1].getVersion().toString(), "")));

        assertThat(mChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testWaitingUpdateFirmwareMismatchAtReconnection() {
        assertThat(mChangeCnt, is(0));
        // mock a single applicable firmwares in store
        doReturn(Collections.singletonList(FIRMWARES[1]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // status should switch to WAITING_FOR_REBOOT
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1)));

        // mock reconnection, with unexpected FIRMWARE[2]
        onNextChange(() -> {
            // whole update task should be FAILED
            assertThat(mChangeCnt, is(4));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.FAILED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(1)));
        });

        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(VERSIONS[2].getVersion().toString(), "")));

        assertThat(mChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testWaitingUpdateContinuesAtReconnection() {
        assertThat(mChangeCnt, is(0));
        // mock two applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // status should switch to WAITING_FOR_REBOOT
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2)));

        // mock reconnection, with expected FIRMWARE[1]
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(VERSIONS[1].getVersion().toString(), "")));

        // update should proceed with next firmware
        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[2]),
                firmwareUpdateIndexIs(2),
                firmwareUpdateTotalCountIs(2)));
    }

    @Test
    public void testOngoingUpdateIsCancelOnDisconnect() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), any());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // update should have been canceled
        verify(request).cancel();
    }

    @Test
    public void testOngoingUpdateFailure() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update failure
        onNextChange(() -> {
            // whole update task should be canceled
            assertThat(mChangeCnt, is(3));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.FAILED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(2)));
        });

        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testUnavailabilityReasonCancelsOngoingUpdate() {
        assertThat(mChangeCnt, is(0));
        // mock applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), any());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock reception of some update unavailability reason
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(39));

        // update should have been canceled
        verify(request).cancel();
    }

    @Test
    public void testWaitingUpdateFailsIfUnavailabilityReasonsAtConnection() {
        assertThat(mChangeCnt, is(0));
        // mock two applicable firmwares in store
        doReturn(Arrays.asList(FIRMWARES[1], FIRMWARES[2]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // status should switch to WAITING_FOR_REBOOT
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2)));

        // mock reconnection, with expected FIRMWARE[1], but unavailability reasons
        onNextChange(() -> {
            // update should be successful
            assertThat(mChangeCnt, is(4));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.FAILED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(2)));
        });

        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(
                        VERSIONS[1].getVersion().toString(), ""))
                .commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(39)));

        // update should end
        assertThat(mChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testFinishedUpdateSucceedsEvenIfUnavailabilityReasonsAtConnection() {
        assertThat(mChangeCnt, is(0));
        // mock one applicable firmwares in store
        doReturn(Collections.singletonList(FIRMWARES[1]))
                .when(mMockFirmwareStore)
                .applicableUpdatesFor(any());

        // mock firmware data
        doReturn(mock(InputStream.class)).when(mMockFirmwareStore).getFirmwareStream(any());

        // connect the drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));

        Cancelable request = spy(HttpRequest.class);
        ArgumentCaptor<HttpRequest.ProgressStatusCallback> callbackCaptor = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);
        doReturn(request).when(mMockUpdateClient).uploadFirmware(any(), callbackCaptor.capture());

        // request an update
        mUpdater.updateToLatestFirmware();

        assertThat(mChangeCnt, is(2));

        // mock update success
        callbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2)); // nothing should change, state WAITING_FOR_REBOOT happens at disconnection

        // mock disconnection
        disconnectDrone(mDrone, 1);

        // status should switch to WAITING_FOR_REBOOT
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1)));

        // mock reconnection, with expected FIRMWARE[1], but unavailability reasons
        onNextChange(() -> {
            // update should be successful
            assertThat(mChangeCnt, is(4));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.SUCCESS),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(1)));
        });

        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged(
                        VERSIONS[1].getVersion().toString(), ""))
                .commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(39)));

        // update should end
        assertThat(mChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }
}
