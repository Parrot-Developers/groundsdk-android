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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.crashml;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.CrashReportDownloader;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.crashml.ArsdkCrashmlDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.crashml.MockArsdkCrashmlDownloadRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class FtpReportDownloaderTests extends ArsdkEngineTestBase {

    private static final String DOWNLOAD_DIR = "/tmp";

    private RemoteControlCore mRemoteControl;

    private CrashReportDownloader mCrashReportDownloader;

    private int mChangeCnt;

    @Mock
    private CrashReportStorage mStorage;

    @Override
    public void setUp() {
        super.setUp();

        doReturn(new File(DOWNLOAD_DIR)).when(mStorage).getWorkDir();
        mUtilities.registerUtility(CrashReportStorage.class, mStorage);

        mArsdkEngine.start();

        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("123");
        assert mRemoteControl != null;

        mCrashReportDownloader = mRemoteControl.getPeripheralStore().get(mMockSession, CrashReportDownloader.class);
        mRemoteControl.getPeripheralStore().registerObserver(CrashReportDownloader.class, () -> {
            mCrashReportDownloader = mRemoteControl.getPeripheralStore().get(mMockSession, CrashReportDownloader.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mCrashReportDownloader, nullValue());

        connectRemoteControl(mRemoteControl, 1, () -> {
            MockArsdkCrashmlDownloadRequest crashmlRequest = new MockArsdkCrashmlDownloadRequest();
            mMockArsdkCore.expect(Expectation.CrashmlReport.of(1, crashmlRequest, DOWNLOAD_DIR));
        });

        assertThat(mChangeCnt, is(2)); // +1 for publish, +1 for download start
        assertThat(mCrashReportDownloader, notNullValue());
        assertThat(mCrashReportDownloader, isDownloading(0));

        disconnectRemoteControl(mRemoteControl, 1);

        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, nullValue());
    }

    @Test
    public void testDownloadFailed() {
        MockArsdkCrashmlDownloadRequest crashmlRequest = new MockArsdkCrashmlDownloadRequest();

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.CrashmlReport.of(1, crashmlRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2)); // +1 for publish, +1 for download start
        assertThat(mCrashReportDownloader, isDownloading(0));

        crashmlRequest.mockReportDownloaded(new File(mStorage.getWorkDir(), "report_000"));

        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        crashmlRequest.mockCompletion(ArsdkCrashmlDownloadRequest.STATUS_ABORTED);

        assertThat(mChangeCnt, is(4));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDownloadCancel() {
        MockArsdkCrashmlDownloadRequest crashmlRequest = new MockArsdkCrashmlDownloadRequest();

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.CrashmlReport.of(1, crashmlRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2)); // +1 for publish, +1 for download start
        assertThat(mCrashReportDownloader, isDownloading(0));

        crashmlRequest.mockReportDownloaded(new File(mStorage.getWorkDir(), "report_000"));

        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        crashmlRequest.cancel();

        assertThat(mChangeCnt, is(4));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDownloadFromRc() {
        MockArsdkCrashmlDownloadRequest crashmlRequest = new MockArsdkCrashmlDownloadRequest();

        connectRemoteControl(mRemoteControl, 1,
                () -> mMockArsdkCore.expect(Expectation.CrashmlReport.of(2, crashmlRequest, DOWNLOAD_DIR)));

        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        crashmlRequest.mockReportDownloaded(new File(mStorage.getWorkDir(), "report_000"));

        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        crashmlRequest.mockCompletion(ArsdkCrashmlDownloadRequest.STATUS_OK);

        assertThat(mChangeCnt, is(4));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));
    }
}
