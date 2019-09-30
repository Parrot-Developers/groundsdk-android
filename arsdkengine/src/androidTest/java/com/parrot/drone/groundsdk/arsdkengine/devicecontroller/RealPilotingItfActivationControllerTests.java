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
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiFollowMePilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiLookAtPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiManualPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiReturnHomePilotingItf;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RealPilotingItfActivationControllerTests extends ArsdkEngineTestBase {

    private MockDroneController mDroneController;

    private MockAnafiManualPilotingItfItf mManualPilotingItf;

    private MockAnafiReturnHomePilotingItf mReturnHomePilotingItf;

    private MockAnafiLookAtPilotingItf mLookAtPilotingItf;

    private MockAnafiFollowMePilotingItf mFollowMePilotingItf;

    @Override
    public void setUp() {
        super.setUp();
        mDroneController = new MockDroneController(mArsdkEngine, "123", Drone.Model.ANAFI_4K, "anafi",
                activationController -> mManualPilotingItf = new MockAnafiManualPilotingItfItf(activationController));

        mReturnHomePilotingItf = new MockAnafiReturnHomePilotingItf(mDroneController.mActivationController);
        mLookAtPilotingItf = new MockAnafiLookAtPilotingItf(mDroneController.mActivationController);
        mFollowMePilotingItf = new MockAnafiFollowMePilotingItf(mDroneController.mActivationController);
        mArsdkEngine.start();
    }

    @Test
    public void testDefaultActivationDuringConnection() {
        // before connection, everything should be unavailable
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // and no activation request should have been made
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));

        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // the default interface should be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // default piloting interface activation should have been requested
        assertThat(mManualPilotingItf.activationCnt, is(1));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
    }

    @Test
    public void testNonDefaultActivationDuringConnection() {
        // before connection, everything should be unavailable
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // mock non-default interface spontaneous activation
        mReturnHomePilotingItf.setState(Activable.State.ACTIVE);

        // should be the only one active now
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // previously active interface should remain
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.UNAVAILABLE));

        // and no activation request should have been made
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
    }

    @Test
    public void testActivation() {
        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // initialize piloting interface states
        mReturnHomePilotingItf.setState(Activable.State.IDLE);
        mLookAtPilotingItf.setState(Activable.State.IDLE);

        mManualPilotingItf.reset();

        // default interface should be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));

        /////////////////////////////////
        // activate return home interface
        mReturnHomePilotingItf.getPilotingItf().activate();

        // manual interface should be immediately deactivated
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));

        // return home interface activation should be requested
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(1));
        assertThat(mReturnHomePilotingItf.activationCnt, is(1));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(0));

        // mock activation answer from low-level
        mReturnHomePilotingItf.setState(Activable.State.ACTIVE);

        // return home interface should now be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.activationCnt, is(1));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(0));

        /////////////////////////////
        // activate look at interface
        mLookAtPilotingItf.getPilotingItf().activate();

        // return home interface deactivation should be requested
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.activationCnt, is(1));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(1));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));

        // mock deactivation answer from low-level
        mReturnHomePilotingItf.setState(Activable.State.IDLE);

        // return home interface should now be idle, and look at interface activation should be requested
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.activationCnt, is(1));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(1));
        assertThat(mLookAtPilotingItf.activationCnt, is(1));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));

        // mock activation answer from low-level
        mLookAtPilotingItf.setState(Activable.State.ACTIVE);

        // look at interface should now be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mReturnHomePilotingItf.activationCnt, is(1));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(1));
        assertThat(mLookAtPilotingItf.activationCnt, is(1));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));

        ////////////////////////////
        // activate manual interface
        mManualPilotingItf.getPilotingItf().activate();

        // look at interface deactivation should be requested
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.activationCnt, is(1));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(1));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(1));

        // mock deactivation answer from low-level
        mLookAtPilotingItf.setState(Activable.State.IDLE);

        // manual piloting interface should be immediately active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.activationCnt, is(1));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(1));
        assertThat(mManualPilotingItf.activationCnt, is(1));
        assertThat(mManualPilotingItf.deactivationCnt, is(1));
    }

    @Test
    public void testDeactivation() {
        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // initialize piloting interface states
        mReturnHomePilotingItf.setState(Activable.State.ACTIVE);

        mManualPilotingItf.reset();
        mReturnHomePilotingItf.reset();

        // return home interface should be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mReturnHomePilotingItf.activationCnt, is(0));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(0));

        ///////////////////////////////////
        // deactivate return home interface
        mReturnHomePilotingItf.getPilotingItf().deactivate();

        // return interface deactivation should be requested
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mReturnHomePilotingItf.activationCnt, is(0));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(1));

        // mock deactivation answer from low-level
        mReturnHomePilotingItf.setState(Activable.State.IDLE);

        // manual piloting interface should be immediately active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mReturnHomePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(1));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mReturnHomePilotingItf.activationCnt, is(0));
        assertThat(mReturnHomePilotingItf.deactivationCnt, is(1));
    }

    @Test
    public void testPilotingItfGoesAvailable() {
        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // initialize piloting interface states
        mLookAtPilotingItf.setState(Activable.State.IDLE);
        mFollowMePilotingItf.setState(Activable.State.IDLE);

        mManualPilotingItf.reset();

        // the default interface should be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));

        // mock look at interface activation from low-level
        mLookAtPilotingItf.setState(Activable.State.ACTIVE);

        // look at interface becomes immediately active, and manual interface idle
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));
        assertThat(mFollowMePilotingItf.activationCnt, is(0));
        assertThat(mFollowMePilotingItf.deactivationCnt, is(0));

        // mock follow me interface activation from low-level
        mFollowMePilotingItf.setState(Activable.State.ACTIVE);

        // follow me interface becomes immediately active, and look at interface idle
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));
        assertThat(mFollowMePilotingItf.activationCnt, is(0));
        assertThat(mFollowMePilotingItf.deactivationCnt, is(0));

        // mock manual interface activation from low-level
        mManualPilotingItf.setState(Activable.State.ACTIVE);

        // manual interface becomes immediately active, and follow me interface idle
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mFollowMePilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));
        assertThat(mFollowMePilotingItf.activationCnt, is(0));
        assertThat(mFollowMePilotingItf.deactivationCnt, is(0));
    }

    @Test
    public void testPilotingItfGoesUnavailable() {
        // mock that connection is complete
        mDroneController.onProtocolConnected();

        // initialize piloting interface states
        mLookAtPilotingItf.setState(Activable.State.ACTIVE);

        mManualPilotingItf.reset();
        mLookAtPilotingItf.reset();

        // the look at interface should be active
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mManualPilotingItf.activationCnt, is(0));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));

        // mock look at interface deactivation from low-level
        mLookAtPilotingItf.setState(Activable.State.IDLE);

        // manual interface becomes immediately active, and look at interface idle
        assertThat(mManualPilotingItf.getPilotingItf().getState(), is(Activable.State.ACTIVE));
        assertThat(mLookAtPilotingItf.getPilotingItf().getState(), is(Activable.State.IDLE));
        assertThat(mManualPilotingItf.activationCnt, is(1));
        assertThat(mManualPilotingItf.deactivationCnt, is(0));
        assertThat(mLookAtPilotingItf.activationCnt, is(0));
        assertThat(mLookAtPilotingItf.deactivationCnt, is(0));
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

    private static final class MockAnafiManualPilotingItfItf extends AnafiManualPilotingItf {

        private int activationCnt;

        private int deactivationCnt;

        MockAnafiManualPilotingItfItf(@NonNull PilotingItfActivationController activationController) {
            super(activationController);
        }

        @Override
        public void requestActivation() {
            super.requestActivation();
            activationCnt++;
        }

        @Override
        public void requestDeactivation() {
            super.requestDeactivation();
            deactivationCnt++;
        }

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

        void reset() {
            activationCnt = 0;
            deactivationCnt = 0;
        }
    }

    private static final class MockAnafiReturnHomePilotingItf extends AnafiReturnHomePilotingItf {

        private int activationCnt;

        private int deactivationCnt;

        MockAnafiReturnHomePilotingItf(@NonNull PilotingItfActivationController activationController) {
            super(activationController);
        }

        @Override
        public void requestActivation() {
            super.requestActivation();
            activationCnt++;
        }

        @Override
        public void requestDeactivation() {
            super.requestDeactivation();
            deactivationCnt++;
        }

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

        void reset() {
            activationCnt = 0;
            deactivationCnt = 0;
        }
    }

    private static final class MockAnafiLookAtPilotingItf extends AnafiLookAtPilotingItf {

        private int activationCnt;

        private int deactivationCnt;

        MockAnafiLookAtPilotingItf(@NonNull PilotingItfActivationController activationController) {
            super(activationController);
        }

        @Override
        public void requestActivation() {
            super.requestActivation();
            activationCnt++;
        }

        @Override
        public void requestDeactivation() {
            super.requestDeactivation();
            deactivationCnt++;
        }

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

        void reset() {
            activationCnt = 0;
            deactivationCnt = 0;
        }
    }

    private static final class MockAnafiFollowMePilotingItf extends AnafiFollowMePilotingItf {

        private int activationCnt;

        private int deactivationCnt;

        MockAnafiFollowMePilotingItf(@NonNull PilotingItfActivationController activationController) {
            super(activationController);
        }

        @Override
        public void requestActivation() {
            super.requestActivation();
            activationCnt++;
        }

        @Override
        public void requestDeactivation() {
            super.requestDeactivation();
            deactivationCnt++;
        }

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

        public void reset() {
            activationCnt = 0;
            deactivationCnt = 0;
        }
    }
}
