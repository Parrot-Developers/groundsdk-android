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

package com.parrot.drone.groundsdk.internal.engine.activation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.device.MockDrone;
import com.parrot.drone.groundsdk.internal.device.MockRC;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.http.HttpActivationClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

public class ActivationEngineTest {

    private static final Map<String, String> REGISTER_DEVICES;

    private static final Map<String, String> REGISTER_DEVICES_2;

    static {
        REGISTER_DEVICES = new HashMap<>();
        REGISTER_DEVICES.put("PI040384AH7J139040", "4.4.0");
        REGISTER_DEVICES.put("PI040409AC7J229109", "1.0.7");
        REGISTER_DEVICES_2 = new HashMap<>();
        REGISTER_DEVICES_2.put("PI00123456789", "1.2.3");
        REGISTER_DEVICES_2.put("PI09876543210", "3.2.1");
    }

    private ActivationEngine mEngine;

    private DroneStore mDroneStore;

    private RemoteControlStore mRCStore;

    private SystemConnectivity mMockConnectivity;

    private HttpRequest mMockDeviceRegisterRequest;

    private HttpActivationClient mMockHttpClient;

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, String>> mDevicesCaptor = ArgumentCaptor.forClass(Map.class);

    private final ArgumentCaptor<HttpRequest.StatusCallback> mCbCaptor =
            ArgumentCaptor.forClass(HttpRequest.StatusCallback.class);

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
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteSharedPreferences(Persistence.PREF_FILE);

        mMockConnectivity = mock(SystemConnectivity.class);
        mDroneStore = new DeviceStoreCore.Drone();
        mRCStore = new DeviceStoreCore.RemoteControl();

        mMockDeviceRegisterRequest = mock(HttpRequest.class);

        MockComponentStore<Facility> facilityStore = new MockComponentStore<>();
        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = mock(ActivationEngine.class, withSettings()
                .useConstructor(MockEngineController.create(context,
                        utilities.registerUtility(SystemConnectivity.class, mMockConnectivity)
                                 .registerUtility(DroneStore.class, mDroneStore)
                                 .registerUtility(RemoteControlStore.class, mRCStore), facilityStore))
                .defaultAnswer(CALLS_REAL_METHODS));

        mMockHttpClient = mock(HttpActivationClient.class);
        doReturn(mMockHttpClient).when(mEngine).createHttpClient();
        doReturn(mMockDeviceRegisterRequest).when(mMockHttpClient).register(any(), any());
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testStart() {
        mEngine.start();

        verify(mMockConnectivity, Mockito.times(1)).monitorWith(any());
    }

    @Test
    public void testStop() {
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);

        mEngine.start();

        verify(mMockConnectivity, Mockito.times(1)).monitorWith(monitorListenerCaptor.capture());

        // add a drone and mock internet available so that a device register is started
        addRC("PI040409AC7J229109", RemoteControl.Model.SKY_CONTROLLER_3, "1.0.7").mockPersisted();
        addDrone("PI040384AH7J139040", Drone.Model.ANAFI_4K, "4.4.0").mockPersisted();
        mockInternetAvailable();

        mEngine.requestStop(null);
        mEngine.stop();

        verify(mMockConnectivity, Mockito.times(1)).disposeMonitor(monitorListenerCaptor.getValue());

        verify(mMockDeviceRegisterRequest, Mockito.times(1)).cancel();
    }

    @Test
    public void testIgnoredDevices() {
        mEngine.start();

        // add default drone(s) and simulator drone, they should never be registered
        List<MockDrone> drones = Stream.of(Drone.Model.values())
                                       .map(it -> addDrone(it.defaultDeviceUid(), it, "1.0.0").mockPersisted())
                                       .collect(Collectors.toCollection(ArrayList::new));
        drones.add(addDrone(ArsdkDevice.SIMULATOR_UID, Drone.Model.ANAFI_THERMAL, "1.0.0").mockPersisted());

        // add a drone with no known board id (yet), should not be registered
        drones.add(addDrone("null-board-id", Drone.Model.ANAFI_4K, "1.0.0", null).mockPersisted());

        // add a drond with a board id for which registration is not required, should not be registered
        drones.add(addDrone("skip-board-id", Drone.Model.ANAFI_4K, "1.0.0", "0x1")
                .mockPersisted());

        drones.forEach(it -> assertThat(mEngine.deviceNeedRegister(it), is(false)));

        // make internet available
        mockInternetAvailable();

        // it should not trigger a device register
        verify(mMockHttpClient, Mockito.times(0)).register(any(), any());

        drones.forEach(it -> assertThat(mEngine.deviceNeedRegister(it), is(false)));
    }

    @Test
    public void testRegisterSuccess() {
        mEngine.start();

        // add some devices
        MockRC rc1 = addRC("PI040409AC7J229109", RemoteControl.Model.SKY_CONTROLLER_3, "1.0.7");
        MockDrone drone1 = addDrone("PI040384AH7J139040", Drone.Model.ANAFI_4K, "4.4.0");

        // devices not persisted, don't need to be registered
        assertThat(mEngine.deviceNeedRegister(drone1), is(false));
        assertThat(mEngine.deviceNeedRegister(rc1), is(false));

        // devices persisted, need to be registered
        rc1.mockPersisted();
        drone1.mockPersisted();
        assertThat(mEngine.deviceNeedRegister(drone1), is(true));
        assertThat(mEngine.deviceNeedRegister(rc1), is(true));

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect device register
        verify(mMockHttpClient, Mockito.times(1)).register(mDevicesCaptor.capture(), mCbCaptor.capture());
        assertThat(mDevicesCaptor.getValue(), is(REGISTER_DEVICES));

        // mock a successful register
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mEngine.deviceNeedRegister(drone1), is(false));
        assertThat(mEngine.deviceNeedRegister(rc1), is(false));

        // make internet unavailable
        mockInternetUnavailable();

        // make internet available
        mockInternetAvailable();

        // devices already registered, so it should not trigger another device register
        verify(mMockHttpClient, Mockito.times(1)).register(any(), any());

        // add another RC
        MockRC rc2 = addRC("PI00123456789", RemoteControl.Model.SKY_CONTROLLER_3, "1.2.3").mockPersisted();
        assertThat(mEngine.deviceNeedRegister(rc2), is(true));

        // device register not triggered when new RC added
        verify(mMockHttpClient, Mockito.times(1)).register(any(), any());

        // add another drone
        MockDrone drone2 = addDrone("PI09876543210", Drone.Model.ANAFI_4K, "3.2.1").mockPersisted();
        assertThat(mEngine.deviceNeedRegister(rc2), is(true));
        assertThat(mEngine.deviceNeedRegister(drone2), is(true));

        // since a new drone is added, expect device register
        verify(mMockHttpClient, Mockito.times(2)).register(mDevicesCaptor.capture(), mCbCaptor.capture());
        assertThat(mDevicesCaptor.getValue(), is(REGISTER_DEVICES_2));

        // mock a successful register
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mEngine.deviceNeedRegister(drone1), is(false));
        assertThat(mEngine.deviceNeedRegister(rc1), is(false));
        assertThat(mEngine.deviceNeedRegister(drone2), is(false));
        assertThat(mEngine.deviceNeedRegister(rc2), is(false));
    }

    @Test
    public void testRegisterFailure() {
        mEngine.start();

        // add some devices
        MockRC rc = addRC("PI040409AC7J229109", RemoteControl.Model.SKY_CONTROLLER_3, "1.0.7").mockPersisted();
        MockDrone drone = addDrone("PI040384AH7J139040", Drone.Model.ANAFI_4K, "4.4.0").mockPersisted();

        assertThat(mEngine.deviceNeedRegister(drone), is(true));
        assertThat(mEngine.deviceNeedRegister(rc), is(true));

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect device register
        verify(mMockHttpClient, Mockito.times(1)).register(mDevicesCaptor.capture(), mCbCaptor.capture());
        assertThat(mDevicesCaptor.getValue(), is(REGISTER_DEVICES));

        // mock a register failure, error on server side
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // try later to register these devices
        assertThat(mEngine.deviceNeedRegister(drone), is(true));
        assertThat(mEngine.deviceNeedRegister(rc), is(true));

        // make internet unavailable
        mockInternetUnavailable();

        // make internet available
        mockInternetAvailable();

        // since internet is available
        verify(mMockHttpClient, Mockito.times(2)).register(mDevicesCaptor.capture(), mCbCaptor.capture());
        assertThat(mDevicesCaptor.getValue(), is(REGISTER_DEVICES));

        // mock a register failure, error on application side
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 403);

        // devices should be marked as registered, to not try later
        assertThat(mEngine.deviceNeedRegister(drone), is(false));
        assertThat(mEngine.deviceNeedRegister(rc), is(false));
    }

    @Test
    public void testInternetBecomesUnavailable() {
        mEngine.start();

        // make internet available
        mockInternetAvailable();

        // add some devices
        MockRC rc = addRC("PI040409AC7J229109", RemoteControl.Model.SKY_CONTROLLER_3, "1.0.7").mockPersisted();
        MockDrone drone = addDrone("PI040384AH7J139040", Drone.Model.ANAFI_4K, "4.4.0").mockPersisted();

        assertThat(mEngine.deviceNeedRegister(drone), is(true));
        assertThat(mEngine.deviceNeedRegister(rc), is(true));

        // since internet is available, expect device register
        verify(mMockHttpClient, Mockito.times(1)).register(mDevicesCaptor.capture(), mCbCaptor.capture());
        assertThat(mDevicesCaptor.getValue(), is(REGISTER_DEVICES));

        // make internet unavailable
        mockInternetUnavailable();

        // task should have been canceled
        verify(mMockHttpClient).dispose();

        // mock a register cancel
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        // devices should still need to be registered
        assertThat(mEngine.deviceNeedRegister(drone), is(true));
        assertThat(mEngine.deviceNeedRegister(rc), is(true));
    }

    private void mockInternetAvailable() {
        mockInternetAvailabilityChange(true);
    }

    private void mockInternetUnavailable() {
        mockInternetAvailabilityChange(false);
    }

    private void mockInternetAvailabilityChange(boolean available) {
        Mockito.when(mMockConnectivity.isInternetAvailable()).thenReturn(available);
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);
        verify(mMockConnectivity, Mockito.times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onInternetAvailabilityChanged(available);
    }

    @NonNull
    private MockDrone addDrone(@NonNull String uid,
                               @NonNull Drone.Model model,
                               @NonNull String firmware) {
        return addDrone(uid, model, firmware, "");
    }

    @NonNull
    private MockDrone addDrone(@NonNull String uid,
                               @NonNull Drone.Model model,
                               @NonNull String firmware,
                               @Nullable String boardId) {
        MockDrone device = new MockDrone(uid, model);
        FirmwareVersion firmwareVersion = FirmwareVersion.parse(firmware);
        assert firmwareVersion != null;
        device.updateFirmwareVersion(firmwareVersion);
        if (boardId != null) {
            device.updateBoardId(boardId);
        }
        mDroneStore.add(device);
        return device;
    }

    @NonNull
    private MockRC addRC(@NonNull String uid,
                         @NonNull RemoteControl.Model model,
                         @NonNull String firmware) {
        MockRC device = new MockRC(uid, model);
        FirmwareVersion firmwareVersion = FirmwareVersion.parse(firmware);
        assert firmwareVersion != null;
        device.updateFirmwareVersion(firmwareVersion);
        device.updateBoardId("");
        mRCStore.add(device);
        return device;
    }
}
