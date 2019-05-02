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

import com.parrot.drone.groundsdk.device.peripheral.FlightDataDownloader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FlightDataDownloaderTest {

    private MockComponentStore<Peripheral> mStore;

    private FlightDataDownloaderCore mFlightDataDownloaderImpl;

    private FlightDataDownloader mFlightDataDownloader;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mFlightDataDownloaderImpl = new FlightDataDownloaderCore(mStore);
        mFlightDataDownloader = mStore.get(FlightDataDownloader.class);
        mStore.registerObserver(FlightDataDownloader.class, () -> {
            mFlightDataDownloader = mStore.get(FlightDataDownloader.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mFlightDataDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mFlightDataDownloaderImpl.publish();
        assertThat(mFlightDataDownloader, is(mFlightDataDownloaderImpl));
        assertThat(mComponentChangeCnt, is(1));

        mFlightDataDownloaderImpl.unpublish();
        assertThat(mFlightDataDownloader, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testDownload() {
        mFlightDataDownloaderImpl.publish();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mFlightDataDownloader.isDownloading(), is(false));
        assertThat(mFlightDataDownloader, isIdle());

        // report download start
        mFlightDataDownloaderImpl.updateDownloadingFlag(true)
                                 .updateCompletionStatus(FlightDataDownloader.CompletionStatus.NONE)
                                 .updateDownloadedCount(0)
                                 .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        // check that changes are not reported for updating to the same values
        mFlightDataDownloaderImpl.updateDownloadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mFlightDataDownloaderImpl.updateDownloadedCount(0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        mFlightDataDownloaderImpl.updateCompletionStatus(FlightDataDownloader.CompletionStatus.NONE)
                                 .notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        // report progress
        mFlightDataDownloaderImpl.updateDownloadedCount(1).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mFlightDataDownloader, isDownloading(1));

        // report success
        mFlightDataDownloaderImpl.updateCompletionStatus(FlightDataDownloader.CompletionStatus.SUCCESS)
                                 .updateDownloadingFlag(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(1));

        // report download start
        mFlightDataDownloaderImpl.updateDownloadingFlag(true)
                                 .updateCompletionStatus(FlightDataDownloader.CompletionStatus.NONE)
                                 .updateDownloadedCount(0)
                                 .notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mFlightDataDownloader, isDownloading(0));

        // report failure
        mFlightDataDownloaderImpl.updateCompletionStatus(FlightDataDownloader.CompletionStatus.INTERRUPTED)
                                 .updateDownloadingFlag(false)
                                 .notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mFlightDataDownloader, wasInterruptedAfter(0));
    }
}
