/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.DroneIdMatcher;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDri;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.DroneIdMatcher.is;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiDriTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Dri mDri;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupDrone();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupDrone();
    }

    private void setupDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mDri = mDrone.getPeripheralStore().get(mMockSession, Dri.class);
        mDrone.getPeripheralStore().registerObserver(Dri.class, () -> {
            mDri = mDrone.getPeripheralStore().get(mMockSession, Dri.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mDri, nullValue());

        // Connect drone with no capabilities received
        // connect drone, mocking receiving online only parameters, so something changes on disconnection
        connectDrone(mDrone, 1);

        // component should not be published if the drone does not send supported capabilities
        assertThat(mChangeCnt, is(0));
        assertThat(mDri, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(0));
        assertThat(mDri, nullValue());

        // Connect drone with capabilities received

        // connect drone, receiving supported capabilities
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(
                1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(ArsdkFeatureDri.SupportedCapabilities.values())
                ))
        );

        // component should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mDri, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(1));
        assertThat(mDri, notNullValue());

        // Forgetting drone

        // forget drone
        mDrone.forget();

        // component should be unpublished
        assertThat(mChangeCnt, is(2));
        assertThat(mDri, nullValue());
    }

    @Test
    public void testState() {
        // Connecting drone supporting DRI deactivation
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values())),
                ArsdkEncoder.encodeDriDriState(ArsdkFeatureDri.Mode.DISABLED)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.state(), booleanSettingIsDisabled());

        // change switch state
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.driDriMode(ArsdkFeatureDri.Mode.ENABLED)));
        mDri.state().toggle();

        assertThat(mChangeCnt, is(2));
        assertThat(mDri.state(), booleanSettingIsEnabling());

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDriDriState(ArsdkFeatureDri.Mode.ENABLED));

        assertThat(mChangeCnt, is(3));
        assertThat(mDri.state(), booleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still enabled
        assertThat(mChangeCnt, is(0));
        assertThat(mDri.state(), booleanSettingIsEnabled());

        // change mode offline
        mDri.state().toggle();

        assertThat(mChangeCnt, is(1));
        assertThat(mDri.state(), booleanSettingIsDisabled());

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodeDriCapabilities(
                                ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                        ArsdkFeatureDri.SupportedCapabilities.values())),
                        ArsdkEncoder.encodeDriDriState(ArsdkFeatureDri.Mode.ENABLED))
                // DRI deactivation should be sent to drone
                .expect(new Expectation.Command(1, ExpectedCmd.driDriMode(ArsdkFeatureDri.Mode.DISABLED))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.state(), booleanSettingIsDisabled());
    }

    @Test
    public void testDroneId() {
        // Connecting drone supporting DRI deactivation
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values())),
                ArsdkEncoder.encodeDriDriState(ArsdkFeatureDri.Mode.ENABLED)));

        assertThat(mDri.getDroneId(), nullValue());
        assertThat(mChangeCnt, is(1));

        // IdInfo received
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.ANSI_CTA_2063, "MyANSIId"));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.ANSI_CTA_2063, "MyANSIId"));
        assertThat(mChangeCnt, is(2));

        // Same IdInfo received, no changes should be notified
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.ANSI_CTA_2063, "MyANSIId"));
        assertThat(mChangeCnt, is(2));

        // another id type received, changes should be notified
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.FR_30_OCTETS, "MyANSIId"));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyANSIId"));
        assertThat(mChangeCnt, is(3));

        // another id received, changes should be notified
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.FR_30_OCTETS, "MyFrId"));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyFrId"));
        assertThat(mChangeCnt, is(4));
    }

    @Test
    public void testIdPersistence() {
        // Connect drone receiving identifier
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values())),
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.ANSI_CTA_2063, "testAnsiId")));

        // disconnect & reset engine to fetch everything from persistence layer
        disconnectDrone(mDrone, 1);
        resetEngine();

        // identifier should still be available
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.ANSI_CTA_2063, "testAnsiId"));

        // Connect drone receiving another identifier
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values())),
                ArsdkEncoder.encodeDriDroneId(ArsdkFeatureDri.IdType.FR_30_OCTETS, "testFrId")));

        // disconnect & reset engine to fetch everything from persistence layer
        disconnectDrone(mDrone, 1);
        resetEngine();

        // identifier should still be available
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "testFrId"));


    }
    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(ArsdkFeatureDri.SupportedCapabilities.toBitField(
                        ArsdkFeatureDri.SupportedCapabilities.values())),
                ArsdkEncoder.encodeDriDriState(ArsdkFeatureDri.Mode.ENABLED)));

        assertThat(mChangeCnt, is(1));
        assertThat(mDri.state(), allOf(
                booleanSettingIsEnabled(),
                settingIsUpToDate()));


        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.driDriMode(ArsdkFeatureDri.Mode.DISABLED)));
        mDri.state().toggle();

        assertThat(mChangeCnt, is(2));
        assertThat(mDri.state(), booleanSettingIsDisabling());

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.driDriMode(ArsdkFeatureDri.Mode.ENABLED)));
        mDri.state().toggle();

        assertThat(mChangeCnt, is(3));
        assertThat(mDri.state(), booleanSettingIsEnabling());

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(4));

        assertThat(mDri.state(), allOf(
                booleanSettingIsEnabled(),
                settingIsUpToDate()));
    }

    @Test
    public void testSupportedTypes() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.ON_OFF))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.supportedTypes(), empty());

        // receive supported types
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(ArsdkFeatureDri.SupportedCapabilities.toBitField(
                        ArsdkFeatureDri.SupportedCapabilities.FRENCH_REGULATION)));
        assertThat(mChangeCnt, is(2));
        assertThat(mDri.supportedTypes(), is(EnumSet.of(Dri.TypeConfig.Type.FRENCH)));

        // receive supported types
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(ArsdkFeatureDri.SupportedCapabilities.toBitField(
                        ArsdkFeatureDri.SupportedCapabilities.FRENCH_REGULATION,
                        ArsdkFeatureDri.SupportedCapabilities.EN4709_002_REGULATION)));
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.supportedTypes(), is(EnumSet.of(Dri.TypeConfig.Type.FRENCH, Dri.TypeConfig.Type.EN4709_002)));

        // receive same supported types, should change nothing
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(ArsdkFeatureDri.SupportedCapabilities.toBitField(
                        ArsdkFeatureDri.SupportedCapabilities.FRENCH_REGULATION,
                        ArsdkFeatureDri.SupportedCapabilities.EN4709_002_REGULATION)));
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.supportedTypes(), is(EnumSet.of(Dri.TypeConfig.Type.FRENCH, Dri.TypeConfig.Type.EN4709_002)));
    }

    @Test
    public void testTypeConfigState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.ON_OFF))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.getTypeConfigState(), nullValue());

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("", ArsdkFeatureDri.DriType.FRENCH, ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(2));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofFrench()));

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("operator1", ArsdkFeatureDri.DriType.EN4709_002,
                        ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator1")));

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("operator2", ArsdkFeatureDri.DriType.EN4709_002,
                        ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(4));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator2")));

        // receive same type, should change nothing
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("operator2", ArsdkFeatureDri.DriType.EN4709_002,
                        ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(4));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator2")));

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("", ArsdkFeatureDri.DriType.FRENCH,
                        ArsdkFeatureDri.Status.FAILURE));
        assertThat(mChangeCnt, is(5));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.FAILURE, Dri.TypeConfig.ofFrench()));
    }

    @Test
    public void testTypeConfig() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.ON_OFF))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mDri.getTypeConfigState(), nullValue());

        // try to change to an unsupported type, should do nothing
        mDri.setTypeConfig(Dri.TypeConfig.ofFrench());
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mDri.getTypeConfigState(), nullValue());

        // receive supported types
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(ArsdkFeatureDri.SupportedCapabilities.toBitField(
                        ArsdkFeatureDri.SupportedCapabilities.FRENCH_REGULATION,
                        ArsdkFeatureDri.SupportedCapabilities.EN4709_002_REGULATION)));
        assertThat(mChangeCnt, is(2));
        assertThat(mDri.supportedTypes(), is(EnumSet.of(Dri.TypeConfig.Type.FRENCH, Dri.TypeConfig.Type.EN4709_002)));

        // change type from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.driSetDriType(ArsdkFeatureDri.DriType.FRENCH, "")));
        mDri.setTypeConfig(Dri.TypeConfig.ofFrench());
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofFrench()));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.UPDATING, Dri.TypeConfig.ofFrench()));

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("", ArsdkFeatureDri.DriType.FRENCH, ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(4));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofFrench()));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofFrench()));

        // change type from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.driSetDriType(ArsdkFeatureDri.DriType.EN4709_002, "FIN87astrdge12k8-xyz")));
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"));
        assertThat(mChangeCnt, is(5));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.UPDATING,
                Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));

        // receive type change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriDriType("FIN87astrdge12k8", ArsdkFeatureDri.DriType.EN4709_002,
                        ArsdkFeatureDri.Status.SUCCESS));
        assertThat(mChangeCnt, is(6));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8")));

        // set to same type from api, should do nothing
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"));
        assertThat(mChangeCnt, is(6));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8")));

        // reset type from api, should not send a command
        mDri.setTypeConfig(null);
        assertThat(mChangeCnt, is(7));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8")));

        // disconnect drone, type config state should be reset
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(8));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mDri.getTypeConfigState(), nullValue());
    }

    @Test
    public void testTypeConfigPersistence() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values()))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mDri.getTypeConfigState(), nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // change type from api
        mDri.setTypeConfig(Dri.TypeConfig.ofFrench());
        assertThat(mChangeCnt, is(2));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofFrench()));
        assertThat(mDri.getTypeConfigState(), nullValue());

        // change type from api
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"));
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mDri.getTypeConfigState(), nullValue());

        // connect drone, type configuration should be sent
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDriCapabilities(
                        ArsdkFeatureDri.SupportedCapabilities.toBitField(
                                ArsdkFeatureDri.SupportedCapabilities.values())))
                .expect(new Expectation.Command(1, ExpectedCmd.driSetDriType(ArsdkFeatureDri.DriType.EN4709_002,
                        "FIN87astrdge12k8-xyz"))));
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mDri.getTypeConfigState(), nullValue());

        // disconnect drone, type configuration should not change
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));

        // reset engine, type configuration should be loaded from persisted data
        resetEngine();
        assertThat(mChangeCnt, is(0));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
    }
}
