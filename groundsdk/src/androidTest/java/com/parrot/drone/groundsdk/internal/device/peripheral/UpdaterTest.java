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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;

import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateCurrentFirmwareIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateCurrentProgressIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateIndexIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateStateIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateTotalCountIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateTotalProgressIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadCurrentFirmwareIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadCurrentProgressIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadIndexIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadStateIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadTotalCountIs;
import static com.parrot.drone.groundsdk.internal.device.peripheral.DownloadMatcher.firmwareDownloadTotalProgressIs;
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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

public class UpdaterTest {

    private MockComponentStore<Peripheral> mStore;

    private UpdaterCore mUpdaterImpl;

    private Updater mUpdater;

    private UpdaterCore.Backend mMockBackend;

    private int mComponentChangeCnt;

    private static final FirmwareInfo[] FIRMWARES = new FirmwareInfo[] {
            mock(FirmwareInfo.class),
            mock(FirmwareInfo.class)
    };

    private Queue<Runnable> mOnChangeRunnables;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = mock(UpdaterCore.Backend.class);
        mUpdaterImpl = new UpdaterCore(mStore, mMockBackend);
        mUpdater = mStore.get(Updater.class);
        mStore.registerObserver(Updater.class, () -> {
            mComponentChangeCnt++;
            mUpdater = mStore.get(Updater.class);
            Runnable r = mOnChangeRunnables.poll();
            if (r != null) {
                r.run();
            }
        });
        mComponentChangeCnt = 0;
        mOnChangeRunnables = new LinkedList<>();
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mUpdater, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mUpdaterImpl.publish();
        assertThat(mUpdater, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mUpdaterImpl.unpublish();
        assertThat(mUpdater, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testDownloadableFirmwares() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.downloadableFirmwares(), empty());

        // mock update from low-level
        mUpdaterImpl.updateDownloadableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.downloadableFirmwares(), contains(FIRMWARES[0]));

        // mock same update from low-level
        mUpdaterImpl.updateDownloadableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.downloadableFirmwares(), contains(FIRMWARES[0]));

        // mock update from low level
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // mock second update from low level
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[1], FIRMWARES[0]));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // notify
        mUpdaterImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdaterImpl.downloadableFirmwares(), contains(FIRMWARES[1], FIRMWARES[0]));
    }

    @Test
    public void testApplicableFirmwares() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.applicableFirmwares(), empty());

        // mock update from low-level
        mUpdaterImpl.updateApplicableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.applicableFirmwares(), contains(FIRMWARES[0]));

        // mock same update from low-level
        mUpdaterImpl.updateApplicableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.applicableFirmwares(), contains(FIRMWARES[0]));

        // mock update from low level
        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // mock second update from low level
        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[1], FIRMWARES[0]));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // notify
        mUpdaterImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdaterImpl.applicableFirmwares(), contains(FIRMWARES[1], FIRMWARES[0]));
    }

    @Test
    public void testDownloadUnavailabilityReasons() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.downloadUnavailabilityReasons(), empty());

        // mock update from low-level
        mUpdaterImpl.updateDownloadUnavailabilityReasons(EnumSet.of(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE))
                    .notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.downloadUnavailabilityReasons(), containsInAnyOrder(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE));

        // mock same update from low-level
        mUpdaterImpl.updateDownloadUnavailabilityReasons(EnumSet.of(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE))
                    .notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.downloadUnavailabilityReasons(), containsInAnyOrder(
                Updater.Download.UnavailabilityReason.INTERNET_UNAVAILABLE));

        // mock update from low level
        mUpdaterImpl.updateDownloadUnavailabilityReasons(EnumSet.noneOf(
                Updater.Download.UnavailabilityReason.class));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // mock second update from low level
        mUpdaterImpl.updateDownloadUnavailabilityReasons(EnumSet.allOf(
                Updater.Download.UnavailabilityReason.class));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // notify
        mUpdaterImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdaterImpl.downloadUnavailabilityReasons(), containsInAnyOrder(
                Updater.Download.UnavailabilityReason.values()));
    }

    @Test
    public void testUpdateUnavailabilityReasons() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.updateUnavailabilityReasons(), empty());

        // mock update from low-level
        mUpdaterImpl.updateUpdateUnavailabilityReasons(EnumSet.of(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED))
                    .notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.updateUnavailabilityReasons(), containsInAnyOrder(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED));

        // mock same update from low-level
        mUpdaterImpl.updateUpdateUnavailabilityReasons(EnumSet.of(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED))
                    .notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.updateUnavailabilityReasons(), containsInAnyOrder(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED));

        // mock update from low level
        mUpdaterImpl.updateUpdateUnavailabilityReasons(EnumSet.of(
                Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // mock second update from low level
        mUpdaterImpl.updateUpdateUnavailabilityReasons(EnumSet.of(
                Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY,
                Updater.Update.UnavailabilityReason.NOT_LANDED));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // notify
        mUpdaterImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdaterImpl.updateUnavailabilityReasons(), containsInAnyOrder(
                Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY,
                Updater.Update.UnavailabilityReason.NOT_LANDED));
    }

    @Test
    public void testIsUpToDate() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.isUpToDate(), is(true));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.applicableFirmwares(), empty());

        // mock downloadable firmwares from low-level
        mUpdaterImpl.updateDownloadableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test not up to date anymore
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.isUpToDate(), is(false));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[0]));
        assertThat(mUpdater.applicableFirmwares(), empty());

        // mock applicable firmwares from low-level
        mUpdaterImpl.updateApplicableFirmwares(Collections.singletonList(FIRMWARES[0]))
                    .notifyUpdated();

        // test still not up to date
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.isUpToDate(), is(false));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[0]));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[0]));

        // mock all firmwares downloaded from low-level
        mUpdaterImpl.updateDownloadableFirmwares(Collections.emptyList())
                    .notifyUpdated();

        // nothing should change
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.isUpToDate(), is(false));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[0]));


        // mock all firmwares applied from low-level
        mUpdaterImpl.updateApplicableFirmwares(Collections.emptyList())
                    .notifyUpdated();

        // test up to date
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mUpdater.isUpToDate(), is(true));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.applicableFirmwares(), empty());
    }

    @Test
    public void testIdealVersion() {
        FirmwareVersion v1 = FirmwareVersion.parse("1.0.0");
        FirmwareVersion v2 = FirmwareVersion.parse("2.0.0");

        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.idealVersion(), nullValue());

        // mock update from low-level
        mUpdaterImpl.updateIdealVersion(v1).notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.idealVersion(), is(v1));

        // mock same update from low-level
        mUpdaterImpl.updateIdealVersion(v1).notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdaterImpl.idealVersion(), is(v1));

        // mock update from low level
        mUpdaterImpl.updateIdealVersion(v2).notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdaterImpl.idealVersion(), is(v2));

        // mock update to null from low level
        mUpdaterImpl.updateIdealVersion(null).notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdaterImpl.idealVersion(), nullValue());
    }

    @Test
    public void testDownloadNextFirmware() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.currentDownload(), nullValue());

        // test cannot cancel since there is no ongoing download
        assertThat(mUpdater.cancelDownload(), is(false));

        // test cannot download since there are no downloadable firmwares
        assertThat(mUpdater.downloadNextFirmware(), is(false));
        verify(mMockBackend, never()).download(any(), any());

        // mock some downloadable firmwares
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[0], FIRMWARES[1]));
        assertThat(mUpdater.currentDownload(), nullValue());

        ArgumentCaptor<FirmwareDownloader.Task.Observer> taskObserverCaptor = ArgumentCaptor.forClass(
                FirmwareDownloader.Task.Observer.class);

        // mock download request
        assertThat(mUpdater.downloadNextFirmware(), is(true));
        verify(mMockBackend, times(1)).download(eq(Collections.singletonList(FIRMWARES[0])),
                taskObserverCaptor.capture());

        // current download should remain null until the task callbacks
        assertThat(mUpdater.currentDownload(), nullValue());

        FirmwareDownloader.Task task = newMockTask();
        doReturn(FirmwareDownloader.Task.State.DOWNLOADING).when(task).state();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).requested();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).remaining();
        doReturn(0).when(task).currentProgress();
        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        // mock task change
        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                firmwareDownloadIndexIs(1),
                firmwareDownloadTotalCountIs(1),
                firmwareDownloadCurrentProgressIs(0),
                firmwareDownloadTotalProgressIs(0)));

        // test another download request is rejected since a download is ongoing
        assertThat(mUpdater.downloadNextFirmware(), is(false));
        assertThat(mUpdater.downloadAllFirmwares(), is(false));
        verify(mMockBackend, times(1)).download(any(), any());

        // mock task progress
        doReturn(50).when(task).currentProgress();
        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                firmwareDownloadIndexIs(1),
                firmwareDownloadTotalCountIs(1),
                firmwareDownloadCurrentProgressIs(50),
                firmwareDownloadTotalProgressIs(50)));

        // mock task success
        doReturn(FirmwareDownloader.Task.State.SUCCESS).when(task).state();
        doReturn(Collections.emptyList()).when(task).remaining();
        doReturn(100).when(task).currentProgress();

        // test we receive a first transient change with state success
        onNextChange(() -> {
            assertThat(mComponentChangeCnt, is(5));
            assertThat(mUpdater.currentDownload(), allOf(
                    firmwareDownloadStateIs(Updater.Download.State.SUCCESS),
                    firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                    firmwareDownloadIndexIs(1),
                    firmwareDownloadTotalCountIs(1),
                    firmwareDownloadCurrentProgressIs(100),
                    firmwareDownloadTotalProgressIs(100)));
        });

        taskObserverCaptor.getValue().onChange(task);

        // then we receive another change where there is no current download
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testDownloadAllFirmwares() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.downloadableFirmwares(), empty());
        assertThat(mUpdater.currentDownload(), nullValue());

        // test cannot cancel since there is no ongoing download
        assertThat(mUpdater.cancelDownload(), is(false));

        // test cannot download since there are no downloadable firmwares
        assertThat(mUpdater.downloadAllFirmwares(), is(false));
        verify(mMockBackend, never()).download(any(), any());

        // mock some downloadable firmwares
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.downloadableFirmwares(), contains(FIRMWARES[0], FIRMWARES[1]));
        assertThat(mUpdater.currentDownload(), nullValue());

        ArgumentCaptor<FirmwareDownloader.Task.Observer> taskObserverCaptor = ArgumentCaptor.forClass(
                FirmwareDownloader.Task.Observer.class);

        // mock download request
        assertThat(mUpdater.downloadAllFirmwares(), is(true));
        verify(mMockBackend, times(1)).download(eq(Arrays.asList(FIRMWARES[0], FIRMWARES[1])),
                taskObserverCaptor.capture());

        // current download should remain null until the task callbacks
        assertThat(mUpdater.currentDownload(), nullValue());

        FirmwareDownloader.Task task = newMockTask();
        doReturn(FirmwareDownloader.Task.State.DOWNLOADING).when(task).state();
        doReturn(Arrays.asList(FIRMWARES[0], FIRMWARES[1])).when(task).requested();
        doReturn(Arrays.asList(FIRMWARES[0], FIRMWARES[1])).when(task).remaining();
        doReturn(0).when(task).currentProgress();
        // make our firmwares have some size
        doReturn(50L).when(FIRMWARES[0]).getSize();
        doReturn(50L).when(FIRMWARES[1]).getSize();

        // mock task change
        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                firmwareDownloadIndexIs(1),
                firmwareDownloadTotalCountIs(2),
                firmwareDownloadCurrentProgressIs(0),
                firmwareDownloadTotalProgressIs(0)));

        // test another download request is rejected since a download is ongoing
        assertThat(mUpdater.downloadNextFirmware(), is(false));
        assertThat(mUpdater.downloadAllFirmwares(), is(false));
        verify(mMockBackend, times(1)).download(any(), any());

        // mock task progress
        doReturn(50).when(task).currentProgress();
        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                firmwareDownloadIndexIs(1),
                firmwareDownloadTotalCountIs(2),
                firmwareDownloadCurrentProgressIs(50),
                firmwareDownloadTotalProgressIs(25)));

        // mock next firmware download starts
        doReturn(0).when(task).currentProgress();
        doReturn(Collections.singletonList(FIRMWARES[1])).when(task).remaining();
        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[1]),
                firmwareDownloadIndexIs(2),
                firmwareDownloadTotalCountIs(2),
                firmwareDownloadCurrentProgressIs(0),
                firmwareDownloadTotalProgressIs(50)));

        // mock task success
        doReturn(FirmwareDownloader.Task.State.SUCCESS).when(task).state();
        doReturn(Collections.emptyList()).when(task).remaining();
        doReturn(100).when(task).currentProgress();

        // test we receive a first transient change with state success
        onNextChange(() -> {
            assertThat(mComponentChangeCnt, is(6));
            assertThat(mUpdater.currentDownload(), allOf(
                    firmwareDownloadStateIs(Updater.Download.State.SUCCESS),
                    firmwareDownloadCurrentFirmwareIs(FIRMWARES[1]),
                    firmwareDownloadIndexIs(2),
                    firmwareDownloadTotalCountIs(2),
                    firmwareDownloadCurrentProgressIs(100),
                    firmwareDownloadTotalProgressIs(100)));
        });

        taskObserverCaptor.getValue().onChange(task);

        // then we receive another change where there is no current download
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testDownloadFailure() {
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();
        mUpdaterImpl.publish();

        assertThat(mComponentChangeCnt, is(1));

        ArgumentCaptor<FirmwareDownloader.Task.Observer> taskObserverCaptor = ArgumentCaptor.forClass(
                FirmwareDownloader.Task.Observer.class);

        // mock download request
        assertThat(mUpdater.downloadNextFirmware(), is(true));
        verify(mMockBackend, times(1)).download(eq(Collections.singletonList(FIRMWARES[0])),
                taskObserverCaptor.capture());

        // current download should remain null until the task callbacks
        assertThat(mUpdater.currentDownload(), nullValue());

        FirmwareDownloader.Task task = newMockTask();
        doReturn(FirmwareDownloader.Task.State.FAILED).when(task).state();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).requested();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).remaining();
        doReturn(0).when(task).currentProgress();
        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        // test we receive a first transient change with state failure
        onNextChange(() -> {
            assertThat(mComponentChangeCnt, is(2));
            assertThat(mUpdater.currentDownload(), allOf(
                    firmwareDownloadStateIs(Updater.Download.State.FAILED),
                    firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                    firmwareDownloadIndexIs(1),
                    firmwareDownloadTotalCountIs(1),
                    firmwareDownloadCurrentProgressIs(0),
                    firmwareDownloadTotalProgressIs(0)));
        });

        taskObserverCaptor.getValue().onChange(task);

        // then we receive another change where there is no current download
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testDownloadCancel() {
        mUpdaterImpl.updateDownloadableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();
        mUpdaterImpl.publish();

        assertThat(mComponentChangeCnt, is(1));

        ArgumentCaptor<FirmwareDownloader.Task.Observer> taskObserverCaptor = ArgumentCaptor.forClass(
                FirmwareDownloader.Task.Observer.class);

        // mock download request
        assertThat(mUpdater.downloadNextFirmware(), is(true));
        verify(mMockBackend, times(1)).download(eq(Collections.singletonList(FIRMWARES[0])),
                taskObserverCaptor.capture());

        // current download should remain null until the task callbacks
        assertThat(mUpdater.currentDownload(), nullValue());

        FirmwareDownloader.Task task = newMockTask();
        doReturn(FirmwareDownloader.Task.State.DOWNLOADING).when(task).state();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).requested();
        doReturn(Collections.singletonList(FIRMWARES[0])).when(task).remaining();
        doReturn(50).when(task).currentProgress();
        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        taskObserverCaptor.getValue().onChange(task);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.currentDownload(), allOf(
                firmwareDownloadStateIs(Updater.Download.State.DOWNLOADING),
                firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                firmwareDownloadIndexIs(1),
                firmwareDownloadTotalCountIs(1),
                firmwareDownloadCurrentProgressIs(50),
                firmwareDownloadTotalProgressIs(50)));

        // mock download cancel
        assertThat(mUpdater.cancelDownload(), is(true));
        verify(task).cancel();

        // mock task cancel
        doReturn(FirmwareDownloader.Task.State.CANCELED).when(task).state();

        // test we receive a first transient change with state canceled
        onNextChange(() -> {
            assertThat(mComponentChangeCnt, is(3));
            assertThat(mUpdater.currentDownload(), allOf(
                    firmwareDownloadStateIs(Updater.Download.State.CANCELED),
                    firmwareDownloadCurrentFirmwareIs(FIRMWARES[0]),
                    firmwareDownloadIndexIs(1),
                    firmwareDownloadTotalCountIs(1),
                    firmwareDownloadCurrentProgressIs(50),
                    firmwareDownloadTotalProgressIs(50)));
        });

        taskObserverCaptor.getValue().onChange(task);

        // then we receive another change where there is no current download
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testUpdateToNextFirmware() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.applicableFirmwares(), empty());
        assertThat(mUpdater.currentUpdate(), nullValue());

        // test cannot cancel since there is no ongoing update
        assertThat(mUpdater.cancelUpdate(), is(false));
        verify(mMockBackend, never()).cancelUpdate();

        // test cannot update since there are no applicable firmwares
        assertThat(mUpdater.updateToNextFirmware(), is(false));
        verify(mMockBackend, never()).updateWith(any());

        // mock some applicable firmwares
        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[0], FIRMWARES[1]));
        assertThat(mUpdater.currentUpdate(), nullValue());

        // mock update request
        assertThat(mUpdater.updateToNextFirmware(), is(true));
        verify(mMockBackend, times(1)).updateWith(eq(Collections.singletonList(FIRMWARES[0])));

        // current update should remain null until update from low-level
        assertThat(mUpdater.currentUpdate(), nullValue());

        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        // mock update begin from low-level
        mUpdaterImpl.beginUpdate(Collections.singleton(FIRMWARES[0]))
                    .notifyUpdated();

        // test that there is a current update now
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));

        // test another update request is rejected since an update is ongoing
        assertThat(mUpdater.updateToNextFirmware(), is(false));
        assertThat(mUpdater.updateToLatestFirmware(), is(false));
        verify(mMockBackend, times(1)).updateWith(any());

        // mock task progress
        mUpdaterImpl.updateUploadProgress(50)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(50),
                firmwareUpdateTotalProgressIs(50)));

        // mock processing firmware
        mUpdaterImpl.updateUploadProgress(100)
                    .updateUpdateState(Updater.Update.State.PROCESSING)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.PROCESSING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // mock waiting for reboot
        mUpdaterImpl.updateUpdateState(Updater.Update.State.WAITING_FOR_REBOOT)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // mock task success
        mUpdaterImpl.updateUploadProgress(100)
                    .updateUpdateState(Updater.Update.State.SUCCESS)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.SUCCESS),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // mock task end
        mUpdaterImpl.endUpdate()
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testUpdateToLatestFirmware() {
        mUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.applicableFirmwares(), empty());
        assertThat(mUpdater.currentUpdate(), nullValue());

        // test cannot cancel since there is no ongoing update
        assertThat(mUpdater.cancelUpdate(), is(false));
        verify(mMockBackend, never()).cancelUpdate();

        // test cannot update since there are no applicable firmwares
        assertThat(mUpdater.updateToLatestFirmware(), is(false));
        verify(mMockBackend, never()).updateWith(any());

        // mock some applicable firmwares
        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.applicableFirmwares(), contains(FIRMWARES[0], FIRMWARES[1]));
        assertThat(mUpdater.currentUpdate(), nullValue());

        // mock update request
        assertThat(mUpdater.updateToLatestFirmware(), is(true));
        verify(mMockBackend, times(1)).updateWith(Arrays.asList(FIRMWARES[0], FIRMWARES[1]));

        // current download should remain null until update from low-level
        assertThat(mUpdater.currentUpdate(), nullValue());

        // make our firmwares have some size
        doReturn(50L).when(FIRMWARES[0]).getSize();
        doReturn(50L).when(FIRMWARES[1]).getSize();

        // mock update begin from low-level
        mUpdaterImpl.beginUpdate(new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])))
                    .notifyUpdated();

        // test that there is a current update now
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));

        // test another update request is rejected since an update is ongoing
        assertThat(mUpdater.updateToNextFirmware(), is(false));
        assertThat(mUpdater.updateToLatestFirmware(), is(false));
        verify(mMockBackend, times(1)).updateWith(any());

        // mock task progress
        mUpdaterImpl.updateUploadProgress(50)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(50),
                firmwareUpdateTotalProgressIs(25)));

        // mock processing firmware
        mUpdaterImpl.updateUploadProgress(100)
                    .updateUpdateState(Updater.Update.State.PROCESSING)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.PROCESSING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(50)));

        // mock waiting for reboot
        mUpdaterImpl.updateUpdateState(Updater.Update.State.WAITING_FOR_REBOOT)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(50)));

        // mock next firmware update begins
        mUpdaterImpl.continueUpdate()
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(2),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(50)));

        // mock processing firmware
        mUpdaterImpl.updateUploadProgress(100)
                    .updateUpdateState(Updater.Update.State.PROCESSING)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.PROCESSING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(2),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // mock waiting for reboot
        mUpdaterImpl.updateUpdateState(Updater.Update.State.WAITING_FOR_REBOOT)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(2),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // mock task success
        mUpdaterImpl.updateUpdateState(Updater.Update.State.SUCCESS)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.SUCCESS),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[1]),
                firmwareUpdateIndexIs(2),
                firmwareUpdateTotalCountIs(2),
                firmwareUpdateCurrentProgressIs(100),
                firmwareUpdateTotalProgressIs(100)));

        // end task
        mUpdaterImpl.endUpdate()
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testUpdateFailure() {
        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .beginUpdate(Collections.singleton(FIRMWARES[0]))
                    .publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));


        // mock task failed from low-level
        mUpdaterImpl.updateUpdateState(Updater.Update.State.FAILED)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.FAILED),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));

        // mock task end
        mUpdaterImpl.endUpdate()
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    @Test
    public void testUpdateCancel() {
        // make our firmware have some size
        doReturn(100L).when(FIRMWARES[0]).getSize();

        mUpdaterImpl.updateApplicableFirmwares(Arrays.asList(FIRMWARES[0], FIRMWARES[1]))
                    .beginUpdate(Collections.singleton(FIRMWARES[0]))
                    .publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));

        // mock task cancel
        assertThat(mUpdater.cancelUpdate(), is(true));
        verify(mMockBackend).cancelUpdate();

        // mock task canceled from low-level
        mUpdaterImpl.updateUpdateState(Updater.Update.State.CANCELED)
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.CANCELED),
                firmwareUpdateCurrentFirmwareIs(FIRMWARES[0]),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0),
                firmwareUpdateTotalProgressIs(0)));

        // mock task end
        mUpdaterImpl.endUpdate()
                    .notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mUpdater.currentDownload(), nullValue());
    }

    private static FirmwareDownloader.Task newMockTask() {
        //noinspection AbstractClassNeverImplemented
        abstract class MockableTask implements FirmwareDownloader.Task {}
        // it seems we cannot directly mock an interface with default methods, this way, with an abstract anonymous
        // implementation, it works ok.
        return mock(MockableTask.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

}
