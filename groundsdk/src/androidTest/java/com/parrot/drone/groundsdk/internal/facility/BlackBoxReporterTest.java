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

import com.parrot.drone.groundsdk.facility.BlackBoxReporter;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class BlackBoxReporterTest {

    private MockComponentStore<Facility> mStore;

    private BlackBoxReporterCore mBlackBoxReporterCore;

    private BlackBoxReporter mBlackBoxReporter;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mBlackBoxReporterCore = new BlackBoxReporterCore(mStore);
        mBlackBoxReporter = mStore.get(BlackBoxReporter.class);
        mStore.registerObserver(BlackBoxReporter.class, () -> {
            mComponentChangeCnt++;
            mBlackBoxReporter = mStore.get(BlackBoxReporter.class);
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mBlackBoxReporter, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mBlackBoxReporterCore.publish();
        assertThat(mBlackBoxReporter, is(mBlackBoxReporterCore));
        assertThat(mComponentChangeCnt, is(1));

        mBlackBoxReporterCore.unpublish();
        assertThat(mBlackBoxReporter, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testPendingCount() {
        mBlackBoxReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mBlackBoxReporter.getPendingCount(), is(0));

        mBlackBoxReporterCore.updatePendingCount(1);

        assertThat(mComponentChangeCnt, is(1));

        mBlackBoxReporterCore.updatePendingCount(2).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBlackBoxReporterCore.getPendingCount(), is(2));

        mBlackBoxReporterCore.updatePendingCount(2).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testUploading() {
        mBlackBoxReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mBlackBoxReporter.isUploading(), is(false));

        mBlackBoxReporterCore.updateUploadingFlag(true);

        assertThat(mComponentChangeCnt, is(1));

        mBlackBoxReporterCore.updateUploadingFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBlackBoxReporterCore.isUploading(), is(true));

        mBlackBoxReporterCore.updateUploadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }
}
