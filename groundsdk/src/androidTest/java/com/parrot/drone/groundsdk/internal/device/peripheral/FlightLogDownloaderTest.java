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

import com.parrot.drone.groundsdk.device.peripheral.FlightLogDownloader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FlightLogDownloaderTest {

    private MockComponentStore<Peripheral> mStore;

    private FlightLogDownloaderCore mFlightLogDownloaderImpl;

    private FlightLogDownloader mFlightLogDownloader;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mFlightLogDownloaderImpl = new FlightLogDownloaderCore(mStore);
        mFlightLogDownloader = mStore.get(FlightLogDownloader.class);
        mStore.registerObserver(FlightLogDownloader.class, () -> {
            mFlightLogDownloader = mStore.get(FlightLogDownloader.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mFlightLogDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mFlightLogDownloaderImpl.publish();
        assertThat(mFlightLogDownloader, is(mFlightLogDownloaderImpl));
        assertThat(mComponentChangeCnt, is(1));

        mFlightLogDownloaderImpl.unpublish();
        assertThat(mFlightLogDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testDownload() {
        mFlightLogDownloaderImpl.publish();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mFlightLogDownloader.isDownloading(), is(false));
        assertThat(mFlightLogDownloader, isIdle());

        // download start
        mFlightLogDownloaderImpl.updateDownloadingFlag(true)
                                .updateCompletionStatus(FlightLogDownloader.CompletionStatus.NONE)
                                .updateDownloadedCount(0)
                                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // check that changes are not reported for updating to the same values
        mFlightLogDownloaderImpl.updateDownloadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mFlightLogDownloaderImpl.updateDownloadedCount(0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mFlightLogDownloaderImpl.updateCompletionStatus(FlightLogDownloader.CompletionStatus.NONE)
                                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        // progress
        mFlightLogDownloaderImpl.updateDownloadedCount(1).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        // success
        mFlightLogDownloaderImpl.updateCompletionStatus(FlightLogDownloader.CompletionStatus.SUCCESS)
                                .updateDownloadingFlag(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(1));

        // download start
        mFlightLogDownloaderImpl.updateDownloadingFlag(true)
                                .updateCompletionStatus(FlightLogDownloader.CompletionStatus.NONE)
                                .updateDownloadedCount(0)
                                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // report failure
        mFlightLogDownloaderImpl.updateCompletionStatus(FlightLogDownloader.CompletionStatus.INTERRUPTED)
                                .updateDownloadingFlag(false)
                                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mFlightLogDownloader, wasInterruptedAfter(0));
    }
}
