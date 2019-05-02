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

import com.parrot.drone.groundsdk.device.peripheral.CrashReportDownloader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CrashReportDownloaderTest {

    private MockComponentStore<Peripheral> mStore;

    private CrashReportDownloaderCore mCrashReportDownloaderImpl;

    private CrashReportDownloader mCrashReportDownloader;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mCrashReportDownloaderImpl = new CrashReportDownloaderCore(mStore);
        mCrashReportDownloader = mStore.get(CrashReportDownloader.class);
        mStore.registerObserver(CrashReportDownloader.class, () -> {
            mCrashReportDownloader = mStore.get(CrashReportDownloader.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mCrashReportDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mCrashReportDownloaderImpl.publish();
        assertThat(mCrashReportDownloader, is(mCrashReportDownloaderImpl));
        assertThat(mComponentChangeCnt, is(1));

        mCrashReportDownloaderImpl.unpublish();
        assertThat(mCrashReportDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testDownload() {
        mCrashReportDownloaderImpl.publish();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCrashReportDownloader.isDownloading(), is(false));
        assertThat(mCrashReportDownloader, isIdle());

        // report download start
        mCrashReportDownloaderImpl.updateDownloadingFlag(true)
                                  .updateCompletionStatus(CrashReportDownloader.CompletionStatus.NONE)
                                  .updateDownloadedCount(0)
                                  .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        // check that changes are not reported for updating to the same values
        mCrashReportDownloaderImpl.updateDownloadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mCrashReportDownloaderImpl.updateDownloadedCount(0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mCrashReportDownloaderImpl.updateCompletionStatus(CrashReportDownloader.CompletionStatus.NONE)
                                  .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        // report progress
        mCrashReportDownloaderImpl.updateDownloadedCount(1).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        // report success
        mCrashReportDownloaderImpl.updateCompletionStatus(CrashReportDownloader.CompletionStatus.SUCCESS)
                                  .updateDownloadingFlag(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));

        // report download start
        mCrashReportDownloaderImpl.updateDownloadingFlag(true)
                                  .updateCompletionStatus(CrashReportDownloader.CompletionStatus.NONE)
                                  .updateDownloadedCount(0)
                                  .notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCrashReportDownloader, isDownloading(0));

        // report failure
        mCrashReportDownloaderImpl.updateCompletionStatus(CrashReportDownloader.CompletionStatus.INTERRUPTED)
                                  .updateDownloadingFlag(false)
                                  .notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(0));
    }
}
