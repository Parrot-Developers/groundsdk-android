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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.DroneIdMatcher;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.DroneIdMatcher.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DriTest {
    private MockComponentStore<Peripheral> mStore;

    private DriCore mDriImpl;

    private Dri mDri;

    private Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = new Backend();
        mDriImpl = new DriCore(mStore, mMockBackend);
        mDri = mStore.get(Dri.class);
        mStore.registerObserver(Dri.class, () -> {
            mComponentChangeCnt++;
            mDri = mStore.get(Dri.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mDri, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mDriImpl.publish();

        assertThat(mDri, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mDriImpl.unpublish();

        assertThat(mDri, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testState() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.state(), booleanSettingIsDisabled());

        // change setting from the api
        mDri.state().toggle();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mMockBackend.mEnabled, is(true));
        assertThat(mDri.state(), booleanSettingIsEnabling());

        // mock update from backend
        mDriImpl.state().updateValue(true);
        mDriImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.state(), booleanSettingIsEnabled());
    }

    @Test
    public void testDroneId() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getDroneId(), nullValue());

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.ANSI_CTA_2063, "ANSIId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.ANSI_CTA_2063, "ANSIId"));

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "ANSIId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "ANSIId"));

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "MyFrId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyFrId"));

        // mock update from backend with same values does nothing
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "MyFrId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyFrId"));
    }

    @Test
    public void testSupportedTypes() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.supportedTypes(), empty());

        // mock update from backend
        mDriImpl.updateSupportedTypes(EnumSet.of(Dri.TypeConfig.Type.FRENCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.supportedTypes(), is(EnumSet.of(Dri.TypeConfig.Type.FRENCH)));

        // mock update from backend
        mDriImpl.updateSupportedTypes(EnumSet.allOf(Dri.TypeConfig.Type.class)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.supportedTypes(), is(EnumSet.allOf(Dri.TypeConfig.Type.class)));

        // mock update from backend with same values does nothing
        mDriImpl.updateSupportedTypes(EnumSet.allOf(Dri.TypeConfig.Type.class)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.supportedTypes(), is(EnumSet.allOf(Dri.TypeConfig.Type.class)));
    }

    @Test
    public void testTypeConfigState() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getTypeConfigState(), nullValue());

        // mock update from backend
        mDriImpl.updateTypeConfigState(new DriCore.TypeConfigStateCore(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofFrench())).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofFrench()));

        // mock update from backend
        mDriImpl.updateTypeConfigState(new DriCore.TypeConfigStateCore(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator1"))).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getTypeConfigState(),
                is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofEn4709002("operator1")));

        // mock update from backend
        mDriImpl.updateTypeConfigState(new DriCore.TypeConfigStateCore(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator2"))).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getTypeConfigState(),
                is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofEn4709002("operator2")));

        // mock update from backend with same values does nothing
        mDriImpl.updateTypeConfigState(new DriCore.TypeConfigStateCore(Dri.TypeConfigState.State.CONFIGURED,
                Dri.TypeConfig.ofEn4709002("operator2"))).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getTypeConfigState(),
                is(Dri.TypeConfigState.State.CONFIGURED, Dri.TypeConfig.ofEn4709002("operator2")));

        // mock update from backend
        mDriImpl.updateTypeConfigState(new DriCore.TypeConfigStateCore(Dri.TypeConfigState.State.FAILURE, null))
                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mDri.getTypeConfigState(), is(Dri.TypeConfigState.State.FAILURE));

        // mock update from backend
        mDriImpl.updateTypeConfigState(null).notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mDri.getTypeConfigState(), nullValue());
    }

    @Test
    public void testTypeConfig() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mMockBackend.mTypeConfig, nullValue());

        // change type config from api
        mDri.setTypeConfig(Dri.TypeConfig.ofFrench());
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());
        assertThat(mMockBackend.mTypeConfig, is(Dri.TypeConfig.ofFrench()));

        // mock update from backend
        mDriImpl.updateTypeConfig((DriCore.TypeConfigCore) Dri.TypeConfig.ofFrench()).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofFrench()));

        // change type config from api
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofFrench()));
        assertThat(mMockBackend.mTypeConfig, is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));

        // mock update from backend
        mDriImpl.updateTypeConfig((DriCore.TypeConfigCore) Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"))
                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mMockBackend.mTypeConfig, is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));

        // set type config to same value from api does nothing
        mMockBackend.mTypeConfig = null;
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mMockBackend.mTypeConfig, nullValue());

        // mock update from backend to same values does nothing
        mDriImpl.updateTypeConfig((DriCore.TypeConfigCore) Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz"))
                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mMockBackend.mTypeConfig, nullValue());

        // change type config from api
        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc"));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz")));
        assertThat(mMockBackend.mTypeConfig, is(Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc")));

        // mock update from backend
        mDriImpl.updateTypeConfig((DriCore.TypeConfigCore) Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc")));
        assertThat(mMockBackend.mTypeConfig, is(Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc")));

        // change type config from api
        mDri.setTypeConfig(null);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getTypeConfig(), is(Dri.TypeConfig.ofEn4709002("FRAgroundsdktstp-abc")));
        assertThat(mMockBackend.mTypeConfig, nullValue());

        // mock update from backend
        mDriImpl.updateTypeConfig(null).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mDri.getTypeConfig(), nullValue());
    }

    /**
     * Verify that setting a DRI type configuration with an invalid operator identifier triggers an exception.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testTypeConfigException() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getTypeConfig(), nullValue());

        mDri.setTypeConfig(Dri.TypeConfig.ofEn4709002("invalidOperator"));
    }

    @Test
    public void testTypeConfigValidation() {
        assertThat(Dri.TypeConfig.ofFrench().isValid(), is(true));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyz").isValid(), is(true));
        assertThat(Dri.TypeConfig.ofEn4709002("fin87astrdge12k8-xyz").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87Astrdge12k8-xyz").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87bstrdge12k8-xyz").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8.xyz").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k0-xyz").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8-xyy").isValid(), is(false));
        assertThat(Dri.TypeConfig.ofEn4709002("FIN87astrdge12k8").isValid(), is(false));
    }

    @Test
    public void testCancelRollbacks() {

        mDriImpl.state().setEnabled(false);
        mDriImpl.publish();

        assertThat(mDri.state(), booleanSettingValueIs(false));

        // mock user changes settings
        mDri.state().setEnabled(true);

        // cancel all rollbacks
        mDriImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mDri.state(), booleanSettingValueIs(true));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mDri.state(), booleanSettingValueIs(true));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements DriCore.Backend {

        private boolean mEnabled;

        private DriCore.TypeConfigCore mTypeConfig;

        @Override
        public boolean setState(boolean enabled) {
            mEnabled = enabled;
            return true;
        }

        @Override
        public void setTypeConfig(@Nullable DriCore.TypeConfigCore typeConfig) {
            mTypeConfig = typeConfig;
        }
    }
}