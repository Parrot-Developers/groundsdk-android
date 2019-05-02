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

package com.parrot.drone.groundsdk.internal.facility;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.FlightLogReporter;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FlightLogReporterTest {

    private MockComponentStore<Facility> mStore;

    private FlightLogReporterCore mFlightLogReporterCore;

    private FlightLogReporter mFlightLogReporter;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mFlightLogReporterCore = new FlightLogReporterCore(mStore);
        mFlightLogReporter = mStore.get(FlightLogReporter.class);
        mStore.registerObserver(FlightLogReporter.class, () -> {
            mComponentChangeCnt++;
            mFlightLogReporter = mStore.get(FlightLogReporter.class);
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mFlightLogReporter, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mFlightLogReporterCore.publish();
        assertThat(mFlightLogReporter, is(mFlightLogReporterCore));
        assertThat(mComponentChangeCnt, is(1));

        mFlightLogReporterCore.unpublish();
        assertThat(mFlightLogReporter, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testPendingCount() {
        mFlightLogReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mFlightLogReporter.getPendingCount(), is(0));

        mFlightLogReporterCore.updatePendingCount(1);

        assertThat(mComponentChangeCnt, is(1));

        mFlightLogReporterCore.updatePendingCount(2).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightLogReporterCore.getPendingCount(), is(2));

        mFlightLogReporterCore.updatePendingCount(2).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testUploading() {
        mFlightLogReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mFlightLogReporter.isUploading(), is(false));

        mFlightLogReporterCore.updateUploadingFlag(true);

        assertThat(mComponentChangeCnt, is(1));

        mFlightLogReporterCore.updateUploadingFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightLogReporterCore.isUploading(), is(true));

        mFlightLogReporterCore.updateUploadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }
}
