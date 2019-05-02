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

package com.parrot.drone.groundsdk.internal.engine.flightdata;

import android.content.Context;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.FlightDataManager;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.MockTask;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.FlightDataStorage;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class FlightDataEngineTest {

    private MockComponentStore<Facility> mFacilityStore;

    private FlightDataManager mFlightDataManager;

    private int mFacilityChangeCnt;

    private FlightDataEngine mEngine;

    private MockTask<Collection<File>> mMockCollectTask;

    @BeforeClass
    public static void load() {
        TestExecutor.setup();
        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());
    }

    @AfterClass
    public static void unload() {
        ApplicationStorageProvider.setInstance(null);
        TestExecutor.teardown();
    }

    @Before
    public void setUp() {
        mMockCollectTask = spy(new MockTask<>());
        mFacilityStore = new MockComponentStore<>();
        mFacilityStore.registerObserver(FlightDataManager.class, () -> {
            mFlightDataManager = mFacilityStore.get(FlightDataManager.class);
            mFacilityChangeCnt++;
        });
        mFacilityChangeCnt = 0;

        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = mock(FlightDataEngine.class, withSettings()
                .useConstructor(MockEngineController.create(mock(Context.class), utilities, mFacilityStore))
                .defaultAnswer(CALLS_REAL_METHODS));

        // engine should publish its utility
        assertThat(utilities.getUtility(FlightDataStorage.class), notNullValue());

        doReturn(mMockCollectTask).when(mEngine).launchCollectFilesJob();
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testDirectories() {
        assertThat(mEngine.getEngineDirectory(), is(
                new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "flightdata")));
        assertThat(mEngine.getWorkDirectory().getParentFile(), is(mEngine.getEngineDirectory()));
    }

    @Test
    public void testStart() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mFlightDataManager, notNullValue());
        assertThat(mFlightDataManager.files(), empty());
        verify(mEngine, times(1)).launchCollectFilesJob();
    }

    @Test
    public void testStop() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        mEngine.requestStop(null);
        mEngine.stop();

        verify(mMockCollectTask, times(1)).cancel();
    }

    @Test
    public void testUpdateLocalFiles() {
        File flightDataA = mock(File.class);
        File flightDataB = mock(File.class);

        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mFlightDataManager, notNullValue());
        assertThat(mFlightDataManager.files(), empty());

        mEngine.addLocalFiles(Arrays.asList(flightDataA, flightDataB));

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mFlightDataManager, notNullValue());
        assertThat(mFlightDataManager.files(), containsInAnyOrder(flightDataA, flightDataB));

        // mock flightDataB is an existing file that can be deleted so we can test remove
        when(flightDataB.isFile()).thenReturn(true);
        when(flightDataB.exists()).thenReturn(true);
        when(flightDataB.delete()).thenReturn(true);

        mFlightDataManager.delete(flightDataB);

        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mFlightDataManager, notNullValue());
        assertThat(mFlightDataManager.files(), contains(flightDataA));

        mEngine.addLocalFiles(Collections.singleton(flightDataB));

        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mFlightDataManager, notNullValue());
        assertThat(mFlightDataManager.files(), containsInAnyOrder(flightDataA, flightDataB));
    }
}
