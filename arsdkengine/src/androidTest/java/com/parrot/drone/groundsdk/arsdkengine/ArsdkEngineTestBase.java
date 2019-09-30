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

package com.parrot.drone.groundsdk.arsdkengine;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MockSharedPreferences;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.engine.reversegeocoder.ReverseGeocoderUtilityCore;
import com.parrot.drone.groundsdk.internal.session.MockSession;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.ReverseGeocoderUtility;
import com.parrot.drone.groundsdk.internal.utility.SystemBarometer;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;
import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDroneManager;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;
import com.parrot.drone.sdkcore.arsdk.MockArsdkCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Base class for tests using mock arsdk ctrl
 */
public class ArsdkEngineTestBase {

    protected MockArsdkCore mMockArsdkCore;

    protected DroneStore mDroneStore;

    protected RemoteControlStore mRCStore;

    protected SystemConnectivity mMockConnectivity;

    protected SystemBarometer mMockBarometer;

    protected SystemLocation mMockLocation;

    protected ReverseGeocoderUtilityCore mReverseGeocoderUtility;

    protected final UtilityRegistry mUtilities = new UtilityRegistry();

    protected ArsdkEngine mArsdkEngine;

    protected MockSession mMockSession;

    final Context mContext = mock(Context.class);

    @Before
    @CallSuper
    public void setUp() {
        doReturn(new MockSharedPreferences()).when(mContext).getSharedPreferences(any(), anyInt());
        doReturn(ApplicationProvider.getApplicationContext().getResources()).when(mContext).getResources();
        TestExecutor.setup();
        GroundSdkConfig.loadDefaults();
        mDroneStore = new DeviceStoreCore.Drone();
        mRCStore = new DeviceStoreCore.RemoteControl();
        mMockSession = new MockSession();

        mMockConnectivity = mock(SystemConnectivity.class);
        mMockBarometer = mock(SystemBarometer.class);
        mMockLocation = mock(SystemLocation.class);
        mReverseGeocoderUtility = new ReverseGeocoderUtilityCore();

        mUtilities.registerUtility(DroneStore.class, mDroneStore)
                  .registerUtility(RemoteControlStore.class, mRCStore)
                  .registerUtility(SystemConnectivity.class, mMockConnectivity)
                  .registerUtility(SystemBarometer.class, mMockBarometer)
                  .registerUtility(SystemLocation.class, mMockLocation)
                  .registerUtility(ReverseGeocoderUtility.class, mReverseGeocoderUtility);

        mArsdkEngine = new ArsdkEngine(MockEngineController.create(mContext, mUtilities, new ComponentStore<>())) {

            @NonNull
            @Override
            protected ArsdkCore createArsdkCore(@NonNull ArsdkCore.Listener arsdkListener) {
                mMockArsdkCore = MockArsdkCore.create(arsdkListener);
                return mMockArsdkCore;
            }
        };
    }

    @Rule
    public final TestWatcher mTestWatcher = new TestWatcher() {

        @Override
        protected void succeeded(Description description) {
            mMockArsdkCore.assertNoExpectation();
        }
    };

    @After
    @CallSuper
    public void teardown() {
        TestExecutor.teardown();
    }

    protected void resetEngine() {
        mArsdkEngine.requestStop(null);
        mArsdkEngine.stop();
        mArsdkEngine.start();
    }

    protected final void connectDrone(@NonNull DroneCore drone, int handle) {
        connectDrone(drone, handle, null);
    }

    void expectDateAccordingToDrone(@NonNull DroneCore drone, int handle) {
        if (drone.getModel() == Drone.Model.ANAFI_4K) {
            mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.commonCommonCurrentDateTime(""), false));
        } else {
            mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.commonCommonCurrentDate(""), false));
            mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.commonCommonCurrentTime(""), false));
        }
    }

    protected void connectDrone(@NonNull DroneCore drone, int handle, @Nullable Runnable runBeforeConnect) {
        mMockArsdkCore.expect(new Expectation.Connect(handle));
        drone.connect(null, null);
        mMockArsdkCore.deviceConnecting(handle);
        // after link-level connection, we expect to send a date, time and get all settings
        expectDateAccordingToDrone(drone, handle);
        mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.commonSettingsAllSettings()));
        mMockArsdkCore.deviceConnected(handle);
        // after receiving the all settings ended, we expect to send the get all states
        mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.commonCommonAllStates()));
        mMockArsdkCore.commandReceived(handle, ArsdkEncoder.encodeCommonSettingsStateAllSettingsChanged());
        // run the provided runnable that the test expect to be run before transiting to the connected state
        if (runBeforeConnect != null) {
            runBeforeConnect.run();
        }
        // after receiving the all states ended, we expect the state to be connected
        mMockArsdkCore.commandReceived(handle, ArsdkEncoder.encodeCommonCommonStateAllStatesChanged());
    }

    protected void disconnectDrone(@NonNull DroneCore drone, int handle) {
        mMockArsdkCore.expect(new Expectation.Disconnect(handle));
        drone.disconnect();
        mMockArsdkCore.deviceDisconnected(handle, false);
    }

    protected final void connectRemoteControl(@NonNull RemoteControlCore remoteControl, int handle) {
        connectRemoteControl(remoteControl, handle, null);
    }

    @SuppressWarnings("WeakerAccess")
    protected void connectRemoteControl(@NonNull RemoteControlCore remoteControl, int handle,
                                        @Nullable Runnable runBeforeConnect) {
        mMockArsdkCore.expect(new Expectation.Connect(handle));
        remoteControl.connect(null, null);
        mMockArsdkCore.deviceConnecting(handle);
        // after link-level connection, we expect to send a date, time and get all settings
        mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.skyctrlCommonCurrentDateTime(""), false));
        mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.skyctrlSettingsAllSettings()));
        mMockArsdkCore.deviceConnected(handle);
        // after receiving the all settings ended, we expect to send the get all states
        mMockArsdkCore.expect(new Expectation.Command(handle, ExpectedCmd.skyctrlCommonAllStates()));
        mMockArsdkCore.commandReceived(handle, ArsdkEncoder.encodeSkyctrlSettingsStateAllSettingsChanged());
        // run the provided runnable that the test expect to be run before transiting to the connected state
        if (runBeforeConnect != null) {
            runBeforeConnect.run();
        }
        // after receiving the all states ended, we expect the state to be connected
        mMockArsdkCore.commandReceived(handle, ArsdkEncoder.encodeSkyctrlCommonStateAllStatesChanged());
    }

    protected void disconnectRemoteControl(@NonNull RemoteControlCore remoteControl, int handle) {
        mMockArsdkCore.expect(new Expectation.Disconnect(handle));
        remoteControl.disconnect();
        mMockArsdkCore.deviceDisconnected(handle, false);
    }

    protected void discoveredDroneConnecting(int remoteHandle, DroneFinder.DiscoveredDrone discoveredDrone) {
        mMockArsdkCore.commandReceived(remoteHandle, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.CONNECTING, discoveredDrone.getUid(),
                discoveredDrone.getModel().id(), discoveredDrone.getName()));
    }

    protected void discoveredDroneDisconnected(int remoteHandle, DroneFinder.DiscoveredDrone discoveredDrone) {
        mMockArsdkCore.commandReceived(remoteHandle, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.IDLE, discoveredDrone.getUid(),
                discoveredDrone.getModel().id(), discoveredDrone.getName()));
    }
}
