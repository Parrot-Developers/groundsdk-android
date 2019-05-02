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
import com.parrot.drone.groundsdk.facility.FlightDataManager;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FlightDataManagerTest {

    private static final File FILE_1 = mock(File.class), FILE_2 = mock(File.class);

    private MockComponentStore<Facility> mStore;

    private FlightDataManagerCore.Backend mMockBackend;

    private FlightDataManagerCore mFlightDataManagerCore;

    private FlightDataManager mFlightDataManager;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mMockBackend = mock(FlightDataManagerCore.Backend.class);
        mFlightDataManagerCore = new FlightDataManagerCore(mStore, mMockBackend);
        mFlightDataManager = mStore.get(FlightDataManager.class);
        mStore.registerObserver(FlightDataManager.class, () -> {
            mFlightDataManager = mStore.get(FlightDataManager.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mFlightDataManager, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mFlightDataManagerCore.publish();
        assertThat(mFlightDataManager, is(mFlightDataManagerCore));
        assertThat(mComponentChangeCnt, is(1));

        mFlightDataManagerCore.unpublish();
        assertThat(mFlightDataManager, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testFiles() {
        mFlightDataManagerCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mFlightDataManager.files(), empty());


        mFlightDataManagerCore.updateFiles(Arrays.asList(FILE_1, FILE_2));

        assertThat(mComponentChangeCnt, is(1));

        mFlightDataManagerCore.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightDataManager.files(), containsInAnyOrder(FILE_1, FILE_2));

        mFlightDataManagerCore.updateFiles(Arrays.asList(FILE_2, FILE_1)).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mFlightDataManager.files(), containsInAnyOrder(FILE_1, FILE_2));


        mFlightDataManagerCore.updateFiles(Collections.singleton(FILE_1))
                              .updateFiles(Collections.singleton(FILE_2))
                              .notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mFlightDataManager.files(), contains(FILE_2));
    }

    @Test
    public void testDelete() {
        mFlightDataManagerCore.publish();

        assertThat(mComponentChangeCnt, is(1));
        verifyZeroInteractions(mMockBackend);

        // delete unlisted file, should not go to backend
        assertThat(mFlightDataManager.delete(FILE_1), is(false));
        verify(mMockBackend, never()).delete(FILE_1);

        // add file
        mFlightDataManagerCore.updateFiles(Collections.singleton(FILE_1)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        // delete listed file, mock backend refuses
        doReturn(false).when(mMockBackend).delete(FILE_1);
        assertThat(mFlightDataManager.delete(FILE_1), is(false));
        verify(mMockBackend, times(1)).delete(FILE_1);

        // delete listed file, mock backend accepts
        doReturn(true).when(mMockBackend).delete(FILE_1);
        assertThat(mFlightDataManager.delete(FILE_1), is(true));
        verify(mMockBackend, times(2)).delete(FILE_1);

        // mock deletion update
        mFlightDataManagerCore.updateFiles(Collections.emptySet()).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mFlightDataManager.files(), empty());
    }
}
