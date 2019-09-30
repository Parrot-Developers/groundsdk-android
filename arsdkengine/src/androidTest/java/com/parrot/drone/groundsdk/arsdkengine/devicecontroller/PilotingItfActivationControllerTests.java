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

package com.parrot.drone.groundsdk.arsdkengine.devicecontroller;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.ephemeris.EphemerisUploadProtocol;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PilotingItfActivationControllerTests extends ArsdkEngineTestBase {

    private MockDroneController mDroneController;

    private MockPilotingItfController mDefaultPilotingItf;

    private MockPilotingItfController mMockPilotingItfController;

    private MockPilotingItfController mPilotingItf2;

    @Override
    public void setUp() {
        super.setUp();
        mDroneController = new MockDroneController(mArsdkEngine, "123", Drone.Model.ANAFI_4K, "anafi",
                activationController -> mDefaultPilotingItf = new MockPilotingItfController(activationController));

        mMockPilotingItfController = new MockPilotingItfController(mDroneController.mActivationController);
        mPilotingItf2 = new MockPilotingItfController(mDroneController.mActivationController);
        MockPilotingItfController.reset();
        mArsdkEngine.start();
    }

    @Test
    public void testInitialState() {
        // check initial state
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
    }

    @Test
    public void testDefaultInterfaceFallback() {
        // before connection, everything should be unavailable
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // and no activation request should have been made
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, nullValue());
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // mock an interface reporting any inactive state
        mMockPilotingItfController.setState(Activable.State.IDLE);

        // the interface should be idle
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // and still no activation request should occur
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, nullValue());
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // default piloting interface activation should have been requested
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mDefaultPilotingItf));
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // mock default activation from low-level
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);

        // should be the only one active now
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
    }

    @Test
    public void testNonDefaultActivationDuringConnection() {
        // before connection, everything should be unavailable
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // and no activation request should have been made
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, nullValue());
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // mock an interface reporting any inactive state
        mPilotingItf2.setState(Activable.State.IDLE);

        // the interface should be idle
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));

        // and still no activation request should occur
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, nullValue());
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // mock non-default interface spontaneous activation
        mMockPilotingItfController.setState(Activable.State.ACTIVE);

        // should be the only one active now
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));

        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // no activation request (in particular for the default interface) should have been made
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
        assertThat(MockPilotingItfController.activatedPilotingItf, nullValue());
        assertThat(MockPilotingItfController.deactivatedPilotingItf, nullValue());

        // previously active interface should remain
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
    }

    @Test
    public void testActivateActiveItf() {
        // make default piloting itf active
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);

        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // can not activate an active piloting itf
        boolean result = mDefaultPilotingItf.getPilotingItf().activate();
        assertThat(result, is(false));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
    }

    @Test
    public void testActivateUnavailableItf() {
        // initial state: all unavailable
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // can not activate an unavailable piloting itf
        boolean result = mMockPilotingItfController.getPilotingItf().activate();
        assertThat(result, is(false));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
    }

    @Test
    public void testDeactivateDefaultItf() {
        // initial state: default is active
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // can not deactivate a default piloting itf
        boolean result = mDefaultPilotingItf.getPilotingItf().deactivate();
        assertThat(result, is(false));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
    }

    @Test
    public void testActivation() {
        // mock drone connection
        mDroneController.onProtocolConnected();
        // mock default activation from low-level
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);
        // reset counters
        MockPilotingItfController.reset();

        // default interface should be active
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // set the piloting itf 1 to idle in order to be able to activate it
        mMockPilotingItfController.setState(Activable.State.IDLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // activate piloting itf 1
        boolean result = mMockPilotingItfController.getPilotingItf().activate();
        // nothing should change for the moment
        assertThat(result, is(true));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));
        assertThat(MockPilotingItfController.deactivatedPilotingItf, is(mDefaultPilotingItf));

        // mock deactivation answer from low-level
        mDefaultPilotingItf.setState(Activable.State.IDLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mMockPilotingItfController));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));

        // mock activation answer from low-level
        mMockPilotingItfController.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));
    }

    @Test
    public void testDeactivateNonDefaultItf() {
        // mock non-default activation from low-level
        mDefaultPilotingItf.setState(Activable.State.IDLE);
        mMockPilotingItfController.setState(Activable.State.ACTIVE);
        // mock drone connection
        mDroneController.onProtocolConnected();
        // reset counters
        MockPilotingItfController.reset();

        // initial state: default is idle, pilotingItf1 is active
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // deactivate a non-default piloting itf should activate the default one
        boolean result = mMockPilotingItfController.getPilotingItf().deactivate();
        assertThat(result, is(true));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));
        assertThat(MockPilotingItfController.deactivatedPilotingItf, is(mMockPilotingItfController));

        // mock deactivation answer from low-level
        mMockPilotingItfController.setState(Activable.State.IDLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mDefaultPilotingItf));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));

        // mock activation answer from low-level
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));
    }

    @Test
    public void testActivateNext() {
        // try to go from a non-default piloting itf to another
        mMockPilotingItfController.setState(Activable.State.IDLE);
        mPilotingItf2.setState(Activable.State.IDLE);
        // mock drone connection
        mDroneController.onProtocolConnected();
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);
        // reset counters
        MockPilotingItfController.reset();

        // initial state: default is active, pilotingItf1 is idle
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // activate piloting itf 1
        boolean result = mMockPilotingItfController.getPilotingItf().activate();
        assertThat(result, is(true));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));
        assertThat(MockPilotingItfController.deactivatedPilotingItf, is(mDefaultPilotingItf));

        // mock deactivation answer from low-level
        mDefaultPilotingItf.setState(Activable.State.IDLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mMockPilotingItfController));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));

        // mock activation answer from low-level
        mMockPilotingItfController.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(1));

        // activate piloting itf 2
        result = mPilotingItf2.getPilotingItf().activate();
        assertThat(result, is(true));
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(2));
        assertThat(MockPilotingItfController.deactivatedPilotingItf, is(mMockPilotingItfController));

        // mock deactivation from low level
        mMockPilotingItfController.setState(Activable.State.IDLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(MockPilotingItfController.activationCnt, is(2));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mPilotingItf2));
        assertThat(MockPilotingItfController.deactivationCnt, is(2));

        mPilotingItf2.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(MockPilotingItfController.activationCnt, is(2));
        assertThat(MockPilotingItfController.deactivationCnt, is(2));
    }

    @Test
    public void testActivePilotingItfGoesUnavailable() {
        // mock non-default activation from low-level
        mDefaultPilotingItf.setState(Activable.State.IDLE);
        mMockPilotingItfController.setState(Activable.State.ACTIVE);
        // mock drone connection
        mDroneController.onProtocolConnected();
        // reset counters
        MockPilotingItfController.reset();

        // initial state: default is idle, pilotingItf1 is active
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(0));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // Mock MockPilotingItfController goes unavailable
        mMockPilotingItfController.setState(Activable.State.UNAVAILABLE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.activatedPilotingItf, is(mDefaultPilotingItf));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));

        // mock activation answer
        mDefaultPilotingItf.setState(Activable.State.ACTIVE);
        assertThat(mDefaultPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mMockPilotingItfController.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf2.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(MockPilotingItfController.activationCnt, is(1));
        assertThat(MockPilotingItfController.deactivationCnt, is(0));
    }

    private static final class MockDroneController extends DroneController {


        MockDroneController(@NonNull ArsdkEngine engine, @NonNull String droneUid, @NonNull Drone.Model model,
                            @NonNull String name,
                            @NonNull ActivablePilotingItfController.Factory defaultPilotingItfFactory) {
            super(engine, droneUid, model, name, new PilotingCommand.Encoder.Anafi(), defaultPilotingItfFactory,
                    EphemerisUploadProtocol::httpUpload);
        }

        @Override
        void sendDate(@NonNull Date currentDate) {
        }
    }

    private static final class IPilotingItfCore extends ActivablePilotingItfCore {

        private static final ComponentDescriptor<PilotingItf, IPilotingItfCore> DESC =
                ComponentDescriptor.of(IPilotingItfCore.class);

        @NonNull
        private final ActivablePilotingItfCore.Backend mBackend;

        IPilotingItfCore(@NonNull ActivablePilotingItfCore.Backend backend) {
            super(DESC, new ComponentStore<>(), backend);
            mBackend = backend;
        }

        boolean activate() {
            return getState() == State.IDLE && mBackend.activate();
        }
    }

    private static final class MockPilotingItfController extends ActivablePilotingItfController {

        private static MockPilotingItfController activatedPilotingItf;

        private static int activationCnt;

        private static MockPilotingItfController deactivatedPilotingItf;

        private static int deactivationCnt;

        @NonNull
        private final IPilotingItfCore mPilotingItf;

        MockPilotingItfController(@NonNull PilotingItfActivationController activationController) {
            super(activationController, true);
            mPilotingItf = new IPilotingItfCore(new ActivablePilotingItfController.Backend() {});
        }

        @NonNull
        @Override
        public IPilotingItfCore getPilotingItf() {
            return mPilotingItf;
        }

        @Override
        public void requestActivation() {
            super.requestActivation();
            activationCnt++;
            activatedPilotingItf = this;
        }

        @Override
        public void requestDeactivation() {
            super.requestDeactivation();
            deactivationCnt++;
            deactivatedPilotingItf = this;
        }

        /**
         * Sets the state of the piloting itf
         *
         * @param state the new state to apply
         */
        void setState(Activable.State state) {
            switch (state) {
                case IDLE:
                    notifyIdle();
                    break;
                case ACTIVE:
                    notifyActive();
                    break;
                case UNAVAILABLE:
                    notifyUnavailable();
                    break;
            }
        }

        static void reset() {
            activatedPilotingItf = null;
            activationCnt = 0;
            deactivatedPilotingItf = null;
            deactivationCnt = 0;
        }
    }
}
