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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.FlightLogDownloader;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.flightlog.ArsdkFlightLogDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.MockArsdkFlightLogDownloadRequest;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class FtpFlightLogDownloaderTests extends ArsdkEngineTestBase {

    private static final String DOWNLOAD_DIR = "/tmp";

    private FlightLogStorage mMockStorage;

    private RemoteControlCore mRemoteControl;

    private FlightLogDownloader mFlightLogDownloader;

    private int mChangeCnt;

    private Queue<Runnable> mOnChangeRunnables;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Override
    public void setUp() {
        super.setUp();
        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());

        mMockStorage = Mockito.mock(FlightLogStorage.class);
        Mockito.doReturn(new File(DOWNLOAD_DIR)).when(mMockStorage).getWorkDir();
        mUtilities.registerUtility(FlightLogStorage.class, mMockStorage);

        mArsdkEngine.start();

        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("123");
        assert mRemoteControl != null;

        mFlightLogDownloader = mRemoteControl.getPeripheralStore().get(mMockSession, FlightLogDownloader.class);
        mRemoteControl.getPeripheralStore().registerObserver(FlightLogDownloader.class, () -> {
            mFlightLogDownloader = mRemoteControl.getPeripheralStore().get(mMockSession, FlightLogDownloader.class);
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
        assertThat(mOnChangeRunnables.isEmpty(), is(true));
        ApplicationStorageProvider.setInstance(null);
        super.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mFlightLogDownloader, nullValue());
        assertThat(mChangeCnt, is(0));

        onNextChange(() -> {
            assertThat(mFlightLogDownloader, notNullValue());
            assertThat(mChangeCnt, is(1));
        });

        connectRemoteControl(mRemoteControl, 1, () -> {
            MockArsdkFlightLogDownloadRequest flightLogRequest = new MockArsdkFlightLogDownloadRequest();
            mMockArsdkCore.expect(Expectation.FlightLog.of(1, flightLogRequest, DOWNLOAD_DIR));
        });

        assertThat(mFlightLogDownloader, isDownloading(0));
        assertThat(mChangeCnt, is(2));

        disconnectRemoteControl(mRemoteControl, 1);

        assertThat(mFlightLogDownloader, nullValue());
        assertThat(mChangeCnt, is(3));
    }

    @Test
    public void testDownloadFailed() {
        MockArsdkFlightLogDownloadRequest flightLogRequest = new MockArsdkFlightLogDownloadRequest();

        onNextChange(() -> {
            assertThat(mFlightLogDownloader, notNullValue());
            assertThat(mChangeCnt, is(1));
        });

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.FlightLog.of(1, flightLogRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        flightLogRequest.mockFlightLogDownloaded(new File(mMockStorage.getWorkDir(), "log-1.bin"));
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        flightLogRequest.mockCompletion(ArsdkFlightLogDownloadRequest.STATUS_ABORTED);
        assertThat(mChangeCnt, is(4));
        assertThat(mFlightLogDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDownloadCancel() {
        MockArsdkFlightLogDownloadRequest flightLogRequest = new MockArsdkFlightLogDownloadRequest();

        onNextChange(() -> {
            assertThat(mFlightLogDownloader, notNullValue());
            assertThat(mChangeCnt, is(1));
        });

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.FlightLog.of(1, flightLogRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        flightLogRequest.mockFlightLogDownloaded(new File(mMockStorage.getWorkDir(), "log-1.bin"));
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        flightLogRequest.cancel();

        assertThat(mChangeCnt, is(4));
        assertThat(mFlightLogDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDownload() {
        MockArsdkFlightLogDownloadRequest flightLogRequest = new MockArsdkFlightLogDownloadRequest();

        onNextChange(() -> {
            assertThat(mFlightLogDownloader, notNullValue());
            assertThat(mChangeCnt, is(1));
        });

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.FlightLog.of(1, flightLogRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        flightLogRequest.mockFlightLogDownloaded(new File(mMockStorage.getWorkDir(), "log-1.bin"));
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        flightLogRequest.mockCompletion(ArsdkFlightLogDownloadRequest.STATUS_OK);
        assertThat(mChangeCnt, is(4));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(1));
    }
}
