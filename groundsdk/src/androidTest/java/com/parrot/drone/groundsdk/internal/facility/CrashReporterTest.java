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

import com.parrot.drone.groundsdk.facility.CrashReporter;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CrashReporterTest {

    private MockComponentStore<Facility> mStore;

    private CrashReporterCore mCrashReporterCore;

    private CrashReporter mCrashReporter;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mCrashReporterCore = new CrashReporterCore(mStore);
        mCrashReporter = mStore.get(CrashReporter.class);
        mStore.registerObserver(CrashReporter.class, () -> {
            mComponentChangeCnt++;
            mCrashReporter = mStore.get(CrashReporter.class);
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mCrashReporter, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mCrashReporterCore.publish();
        assertThat(mCrashReporter, is(mCrashReporterCore));
        assertThat(mComponentChangeCnt, is(1));

        mCrashReporterCore.unpublish();
        assertThat(mCrashReporter, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testPendingCount() {
        mCrashReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mCrashReporter.getPendingCount(), is(0));

        mCrashReporterCore.updatePendingCount(1);

        assertThat(mComponentChangeCnt, is(1));

        mCrashReporterCore.updatePendingCount(2).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCrashReporterCore.getPendingCount(), is(2));

        mCrashReporterCore.updatePendingCount(2).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testUploading() {
        mCrashReporterCore.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mCrashReporter.isUploading(), is(false));

        mCrashReporterCore.updateUploadingFlag(true);

        assertThat(mComponentChangeCnt, is(1));

        mCrashReporterCore.updateUploadingFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCrashReporterCore.isUploading(), is(true));

        mCrashReporterCore.updateUploadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }
}
