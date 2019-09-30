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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.MockDrone;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FirmwareStoreTest {

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private FirmwareEngine mEngine;

    private Persistence mPersistence;

    private DroneStore mDroneStore;

    private FirmwareStoreEntry mTrampoline, mIntermediate, mLatest;

    private static final FirmwareIdentifier V1 = anafi("1.0.1");

    private static final FirmwareIdentifier V2 = anafi("1.0.2");

    private static final FirmwareIdentifier V3 = anafi("1.0.3");

    private static final FirmwareIdentifier V4 = anafi("1.0.4");

    private static final FirmwareIdentifier V5 = anafi("1.0.5");

    private static final String REMOTE_URI = "http://remote/firmware";

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
        mTrampoline = new FirmwareStoreEntryBuilder().firmware(V3)
                                                     .minVersion(V1).maxVersion(V1)
                                                     .remoteUri(REMOTE_URI).build();
        mIntermediate = new FirmwareStoreEntryBuilder().firmware(V4)
                                                       .minVersion(V2)
                                                       .remoteUri(REMOTE_URI).build();
        mLatest = new FirmwareStoreEntryBuilder().firmware(V5)
                                                 .minVersion(V2)
                                                 .remoteUri(REMOTE_URI).build();

        mEngine = mock(FirmwareEngine.class);
        mPersistence = mock(Persistence.class);
        mDroneStore = new DeviceStoreCore.Drone();
        doReturn(mPersistence).when(mEngine).persistence();
        doReturn(mDroneStore).when(mEngine).internalGetUtility(DroneStore.class);
        doReturn(mock(RemoteControlStore.class)).when(mEngine).internalGetUtility(RemoteControlStore.class);
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testLocalFirmwaresWithLocalTrampoline() {
        // make trampoline and latest local
        mTrampoline.setUri(URI.create("file://trampoline"));
        mLatest.setUri(URI.create("file://latest"));

        doReturn(makeEntryMap(mTrampoline, mLatest)).when(mPersistence).loadFirmwares();
        FirmwareStoreCore store = new FirmwareStoreCore(mEngine);

        assertThat(store.getUpdateChain(V1, false), contains(mTrampoline, mLatest));
        assertThat(store.getUpdateChain(V2, false), contains(mLatest));
        assertThat(store.getUpdateChain(V3, false), contains(mLatest));
        assertThat(store.getUpdateChain(V4, false), contains(mLatest));
        assertThat(store.getUpdateChain(V5, false), empty());


        assertThat(store.getUpdateChain(V1, true), contains(mTrampoline, mLatest));
        assertThat(store.getUpdateChain(V2, true), contains(mLatest));
        assertThat(store.getUpdateChain(V3, true), contains(mLatest));
        assertThat(store.getUpdateChain(V4, true), contains(mLatest));
        assertThat(store.getUpdateChain(V5, true), empty());

        assertThat(store.applicableUpdatesFor(V1), contains(mTrampoline.getFirmwareInfo(), mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V2), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V3), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V4), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V5), empty());

        assertThat(store.downloadableUpdatesFor(V1), empty());
        assertThat(store.downloadableUpdatesFor(V2), empty());
        assertThat(store.downloadableUpdatesFor(V3), empty());
        assertThat(store.downloadableUpdatesFor(V4), empty());
        assertThat(store.downloadableUpdatesFor(V5), empty());

        assertThat(store.idealUpdateFor(V1), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V2), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V3), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V4), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V5), nullValue());
    }

    @Test
    public void testLocalFirmwaresWithRemoteTrampoline() {
        // make latest local
        mLatest.setUri(URI.create("file://latest"));
        doReturn(makeEntryMap(mTrampoline, mLatest)).when(mPersistence).loadFirmwares();
        FirmwareStoreCore store = new FirmwareStoreCore(mEngine);

        assertThat(store.getUpdateChain(V1, false), contains(mTrampoline, mLatest));
        assertThat(store.getUpdateChain(V2, false), contains(mLatest));
        assertThat(store.getUpdateChain(V3, false), contains(mLatest));
        assertThat(store.getUpdateChain(V4, false), contains(mLatest));
        assertThat(store.getUpdateChain(V5, false), empty());

        assertThat(store.getUpdateChain(V1, true), empty());
        assertThat(store.getUpdateChain(V2, true), contains(mLatest));
        assertThat(store.getUpdateChain(V3, true), contains(mLatest));
        assertThat(store.getUpdateChain(V4, true), contains(mLatest));
        assertThat(store.getUpdateChain(V5, true), empty());

        assertThat(store.applicableUpdatesFor(V1), empty());
        assertThat(store.applicableUpdatesFor(V2), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V3), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V4), contains(mLatest.getFirmwareInfo()));
        assertThat(store.applicableUpdatesFor(V5), empty());

        assertThat(store.downloadableUpdatesFor(V1), contains(mTrampoline.getFirmwareInfo()));
        assertThat(store.downloadableUpdatesFor(V2), empty());
        assertThat(store.downloadableUpdatesFor(V3), empty());
        assertThat(store.downloadableUpdatesFor(V4), empty());
        assertThat(store.downloadableUpdatesFor(V5), empty());

        assertThat(store.idealUpdateFor(V1), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V2), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V3), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V4), is(mLatest.getFirmwareInfo()));
        assertThat(store.idealUpdateFor(V5), nullValue());
    }

    @Test
    public void testRemoveUnnecessaryFirmwaresAfterDownload() throws IOException {
        // add to the store the drone we want to update
        DroneCore drone = new MockDrone("", Drone.Model.ANAFI_4K);
        drone.updateFirmwareVersion(V1.getVersion());
        mDroneStore.add(drone);

        // let the store contain the remote trampoline and the remote intermediate
        doReturn(makeEntryMap(mTrampoline, mIntermediate)).when(mPersistence).loadFirmwares();
        FirmwareStoreCore store = new FirmwareStoreCore(mEngine);

        assertThat(store.getUpdateChain(V1, false), contains(mTrampoline, mIntermediate));
        assertThat(store.getUpdateChain(V1, true), empty());

        // mock trampoline download
        File trampolineFile = mTemporaryFolder.newFile("trampoline");
        // make it 'old' enough so that it is also eligible for pruning
        //noinspection ResultOfMethodCallIgnored
        trampolineFile.setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        store.addLocalFirmware(V3, trampolineFile.toURI());

        assertThat(store.getUpdateChain(V1, false), contains(mTrampoline, mIntermediate));
        assertThat(store.getUpdateChain(V1, true), contains(mTrampoline));

        // trampoline should have a local uri now
        assertThat(mTrampoline.getLocalUri(), is(trampolineFile.toURI()));

        // mock drone update to trampoline version (trampoline no longer required)
        drone.updateFirmwareVersion(V3.getVersion());
        // explicitly trigger firmware pruning to mock an app reboot
        store.pruneObsoleteFirmwares();

        // local trampoline should have been deleted
        assertThat(store.getUpdateChain(V3, false), contains(mIntermediate));
        assertThat(store.getUpdateChain(V3, true), empty());

        assertThat(mTrampoline.getLocalUri(), nullValue());
        assertThat(trampolineFile.exists(), is(false));

        // mock intermediate firmware downloaded
        File intermediateFile = mTemporaryFolder.newFile("intermediate");
        // make it 'old' enough so that it is also eligible for pruning
        //noinspection ResultOfMethodCallIgnored
        intermediateFile.setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        store.addLocalFirmware(V4, intermediateFile.toURI());

        assertThat(store.getUpdateChain(V3, false), contains(mIntermediate));
        assertThat(store.getUpdateChain(V3, true), contains(mIntermediate));

        // intermediate should have a local uri now
        assertThat(mIntermediate.getLocalUri(), is(intermediateFile.toURI()));

        // mock latest remote firmware appears
        store.mergeRemoteFirmwares(makeEntryMap(mTrampoline, mIntermediate, mLatest));

        // latest should be proposed instead of intermediate now
        assertThat(store.getUpdateChain(V3, false), contains(mLatest));
        assertThat(store.getUpdateChain(V3, true), contains(mIntermediate));

        // mock latest downloaded
        File latestFile = mTemporaryFolder.newFile("latest");
        store.addLocalFirmware(V5, latestFile.toURI());

        assertThat(store.getUpdateChain(V3, false), contains(mLatest));
        assertThat(store.getUpdateChain(V3, true), contains(mLatest));

        // latest should have a local uri now
        assertThat(mLatest.getLocalUri(), is(latestFile.toURI()));
        // local intermediate should have been deleted
        assertThat(mIntermediate.getLocalUri(), nullValue());
        assertThat(intermediateFile.exists(), is(false));
    }

    @NonNull
    private static Map<FirmwareIdentifier, FirmwareStoreEntry> makeEntryMap(@NonNull FirmwareStoreEntry... entries) {
        Map<FirmwareIdentifier, FirmwareStoreEntry> map = new HashMap<>();
        for (FirmwareStoreEntry entry : entries) {
            map.put(entry.getFirmwareInfo().getFirmware(), entry);
        }
        return map;
    }

    @NonNull
    private static FirmwareIdentifier anafi(@NonNull String versionStr) {
        //noinspection ConstantConditions
        return new FirmwareIdentifier(Drone.Model.ANAFI_4K, FirmwareVersion.parse(versionStr));
    }
}
