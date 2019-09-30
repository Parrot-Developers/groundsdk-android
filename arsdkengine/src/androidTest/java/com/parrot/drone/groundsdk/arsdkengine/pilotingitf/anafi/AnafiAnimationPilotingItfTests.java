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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.AnimationItf;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Candle;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.DollySlide;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Dronie;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.GenericTwistUp;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Horizontal180PhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Parabola;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.SphericalPhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Spiral;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertical180PhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.VerticalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertigo;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureAnimation;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.AnimationMatcher.abortingAnimation;
import static com.parrot.drone.groundsdk.AnimationMatcher.animConfigMatches;
import static com.parrot.drone.groundsdk.AnimationMatcher.animProgressIs;
import static com.parrot.drone.groundsdk.AnimationMatcher.animating;
import static com.parrot.drone.groundsdk.AnimationMatcher.unidentifiedAnimation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AnafiAnimationPilotingItfTests extends ArsdkEngineTestBase {

    private static final Animation.Type[] KNOWN_ANIMATIONS = EnumSet.complementOf(
            EnumSet.of(Animation.Type.UNIDENTIFIED)).toArray(new Animation.Type[Animation.Type.values().length - 1]);

    private DroneCore mDrone;

    private AnimationItf mAnimationItf;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mAnimationItf = mDrone.getPilotingItfStore().get(mMockSession, AnimationItf.class);
        mDrone.getPilotingItfStore().registerObserver(AnimationItf.class, () -> {
            mAnimationItf = mDrone.getPilotingItfStore().get(mMockSession, AnimationItf.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testAvailableAnimations() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test that there is no available animation by default
        assertThat(mAnimationItf.getAvailableAnimations(), empty());

        // test all animation available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.values())));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), containsInAnyOrder(KNOWN_ANIMATIONS));

        // test each animation availability separately
        testAvailableAnimation(ArsdkFeatureAnimation.Type.CANDLE, Animation.Type.CANDLE);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.DOLLY_SLIDE, Animation.Type.DOLLY_SLIDE);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.DRONIE, Animation.Type.DRONIE);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.FLIP, Animation.Type.FLIP);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.HORIZONTAL_PANORAMA, Animation.Type.HORIZONTAL_PANORAMA);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.HORIZONTAL_REVEAL, Animation.Type.HORIZONTAL_REVEAL);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.PARABOLA, Animation.Type.PARABOLA);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.SPIRAL, Animation.Type.SPIRAL);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.VERTICAL_REVEAL, Animation.Type.VERTICAL_REVEAL);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.VERTIGO, Animation.Type.VERTIGO);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.TWIST_UP, Animation.Type.TWIST_UP);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.POSITION_TWIST_UP, Animation.Type.POSITION_TWIST_UP);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.HORIZONTAL_180_PHOTO_PANORAMA,
                Animation.Type.HORIZONTAL_180_PHOTO_PANORAMA);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.VERTICAL_180_PHOTO_PANORAMA,
                Animation.Type.VERTICAL_180_PHOTO_PANORAMA);
        testAvailableAnimation(ArsdkFeatureAnimation.Type.SPHERICAL_PHOTO_PANORAMA,
                Animation.Type.SPHERICAL_PHOTO_PANORAMA);

    }

    private void testAvailableAnimation(@NonNull ArsdkFeatureAnimation.Type arsdkType,
                                        @NonNull Animation.Type apiType) {
        int changeCnt = mChangeCnt;

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(arsdkType)));
        assertThat(mChangeCnt, is(changeCnt + 1));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(apiType));
    }

    @Test
    public void testGlobalAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test null type
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationState(null, 50));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating(),
                animProgressIs(50)));

        // test progress
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationState(ArsdkFeatureAnimation.Type.FLIP, 75));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), animProgressIs(75));

        // test none type
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationState(ArsdkFeatureAnimation.Type.NONE, 25));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), nullValue());
    }

    @Test
    public void testCandleAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Candle.class), animating(),
                animConfigMatches(new Candle.Config().withSpeed(1.0).withVerticalDistance(2.0)
                                                     .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Candle.class), animating(),
                animConfigMatches(new Candle.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.CANCELING,
                0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Candle.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.IDLE,
                0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Candle.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(null, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Candle.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.RUNNING,
                0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testDollySlideAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f,
                ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(DollySlide.class), animating(),
                animConfigMatches(new DollySlide.Config().withSpeed(1.0).withAngle(Math.toDegrees(2.0))
                                                         .withHorizontalDistance(3.0)
                                                         .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(DollySlide.class), animating(),
                animConfigMatches((new DollySlide.Config().withMode(Animation.Mode.ONCE)))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(DollySlide.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(ArsdkFeatureAnimation.State.IDLE,
                0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(DollySlide.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(null, 0, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(DollySlide.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDollySlideState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testDronieAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Dronie.class), animating(),
                animConfigMatches(new Dronie.Config().withSpeed(1.0).withDistance(2.0)
                                                     .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Dronie.class), animating(),
                animConfigMatches(new Dronie.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.CANCELING,
                0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Dronie.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.IDLE,
                0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Dronie.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(null, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Dronie.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.RUNNING,
                0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testFlipAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                ArsdkFeatureAnimation.FlipType.FRONT));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), animating(),
                animConfigMatches(new Flip.Config(Flip.Direction.FRONT))));

        // test back direction
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                ArsdkFeatureAnimation.FlipType.BACK));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), animating(),
                animConfigMatches(new Flip.Config(Flip.Direction.BACK))));

        // test right direction
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                ArsdkFeatureAnimation.FlipType.RIGHT));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), animating(),
                animConfigMatches(new Flip.Config(Flip.Direction.RIGHT))));

        // test left direction
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                ArsdkFeatureAnimation.FlipType.LEFT));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), animating(),
                animConfigMatches(new Flip.Config(Flip.Direction.LEFT))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.CANCELING,
                ArsdkFeatureAnimation.FlipType.FRONT));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.IDLE,
                ArsdkFeatureAnimation.FlipType.FRONT));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(null,
                ArsdkFeatureAnimation.FlipType.FRONT));
        assertThat(mChangeCnt, is(7));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Flip.class), animating()));

        // test that when direction is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                null));
        assertThat(mChangeCnt, is(8));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testHorizontalPanoramaAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalPanoramaState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalPanorama.class), animating(),
                animConfigMatches(new HorizontalPanorama.Config().withRotationAngle(Math.toDegrees(1.0))
                                                                 .withRotationSpeed(Math.toDegrees(2.0)))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalPanoramaState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalPanorama.class),
                abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalPanoramaState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalPanorama.class),
                abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalPanoramaState(null, 0, 0));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalPanorama.class), animating()));
    }

    @Test
    public void testHorizontalRevealAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(
                ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalReveal.class), animating(),
                animConfigMatches(new HorizontalReveal.Config().withSpeed(1.0).withDistance(2.0)
                                                               .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalReveal.class), animating(),
                animConfigMatches(new HorizontalReveal.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalReveal.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalReveal.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(null, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(HorizontalReveal.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontalRevealState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testParabolaAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(
                ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Parabola.class), animating(),
                animConfigMatches(new Parabola.Config().withSpeed(1.0).withVerticalDistance(2.0)
                                                       .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Parabola.class), animating(),
                animConfigMatches(new Parabola.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Parabola.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Parabola.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(null, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Parabola.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testSpiralAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, 3.0f, 4.0f, ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Spiral.class), animating(),
                animConfigMatches(new Spiral.Config().withSpeed(1.0).withRadiusVariation(2.0).withVerticalDistance(3.0)
                                                     .withRevolutionAmount(4.0)
                                                     .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.RUNNING,
                1.0f, 2.0f, 3.0f, 4.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Spiral.class), animating(),
                animConfigMatches(new Spiral.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.CANCELING,
                0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Spiral.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.IDLE,
                0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Spiral.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(null, 0, 0, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Spiral.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.RUNNING,
                0, 0, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testVerticalRevealAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f,
                ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(VerticalReveal.class), animating(),
                animConfigMatches(new VerticalReveal.Config().withVerticalSpeed(1.0).withVerticalDistance(2.0)
                                                             .withRotationAngle(Math.toDegrees(3.0))
                                                             .withRotationSpeed(Math.toDegrees(4.0))
                                                             .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(VerticalReveal.class), animating(),
                animConfigMatches(new VerticalReveal.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(VerticalReveal.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(VerticalReveal.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(null, 0, 0, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(VerticalReveal.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVerticalRevealState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testVertigoAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.RUNNING,
                60, 10.0f, ArsdkFeatureAnimation.VertigoFinishAction.UNZOOM,
                ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertigo.class), animating(),
                animConfigMatches(new Vertigo.Config().withDuration(60).withMaxZoomLevel(10.0)
                                                      .withFinishAction(Vertigo.FinishAction.UNZOOM)
                                                      .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.RUNNING,
                60, 10.0f, ArsdkFeatureAnimation.VertigoFinishAction.UNZOOM, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertigo.class), animating(),
                animConfigMatches(new Vertigo.Config().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.CANCELING,
                        0, 0, ArsdkFeatureAnimation.VertigoFinishAction.UNZOOM, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertigo.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.IDLE,
                0, 0, ArsdkFeatureAnimation.VertigoFinishAction.NONE, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertigo.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(null, 0, 0,
                ArsdkFeatureAnimation.VertigoFinishAction.NONE, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertigo.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.RUNNING,
                0, 0, ArsdkFeatureAnimation.VertigoFinishAction.NONE, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));

        // test that when finish animation is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.RUNNING,
                0, 0, null, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testTwistUpAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f,
                ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating(),
                animConfigMatches(new GenericTwistUp.TwistUpConfig().withSpeed(1.0).withVerticalDistance(2.0)
                                                                    .withRotationAngle(Math.toDegrees(3.0))
                                                                    .withRotationSpeed(Math.toDegrees(4.0))
                                                                    .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating(),
                animConfigMatches(new GenericTwistUp.TwistUpConfig().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(null, 0, 0, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testPositionTwistUpAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f,
                ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating(),
                animConfigMatches(new GenericTwistUp.PositionTwistUpConfig()
                        .withSpeed(1.0)
                        .withVerticalDistance(2.0)
                        .withRotationAngle(Math.toDegrees(3.0))
                        .withRotationSpeed(Math.toDegrees(4.0))
                        .withMode(Animation.Mode.ONCE_THEN_MIRRORED))));

        // test normal play mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 1.0f, 2.0f, 3.0f, 4.0f, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating(),
                animConfigMatches(new GenericTwistUp.PositionTwistUpConfig().withMode(Animation.Mode.ONCE))));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(
                ArsdkFeatureAnimation.State.CANCELING, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(
                ArsdkFeatureAnimation.State.IDLE, 0, 0, 0, 0, ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(null, 0, 0, 0, 0,
                ArsdkFeatureAnimation.PlayMode.NORMAL));
        assertThat(mChangeCnt, is(5));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(GenericTwistUp.class), animating()));

        // test that when mode is null, animation is UNIDENTIFIED
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationPositionTwistUpState(
                ArsdkFeatureAnimation.State.RUNNING, 0, 0, 0, 0, null));
        assertThat(mChangeCnt, is(6));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(unidentifiedAnimation(), animating()));
    }

    @Test
    public void testHorizontal180PhotoPanoramaAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontal180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.RUNNING));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Horizontal180PhotoPanorama.class),
                animating(), animConfigMatches(new Horizontal180PhotoPanorama.Config())));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontal180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.CANCELING));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Horizontal180PhotoPanorama.class),
                abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontal180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.IDLE));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Horizontal180PhotoPanorama.class),
                abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationHorizontal180PhotoPanoramaState(null));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Horizontal180PhotoPanorama.class),
                animating()));
    }

    @Test
    public void testVertical180PhotoPanoramaAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertical180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.RUNNING));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertical180PhotoPanorama.class),
                animating(), animConfigMatches(new Vertical180PhotoPanorama.Config())));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertical180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.CANCELING));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertical180PhotoPanorama.class),
                abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertical180PhotoPanoramaState(
                ArsdkFeatureAnimation.State.IDLE));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertical180PhotoPanorama.class),
                abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationVertical180PhotoPanoramaState(null));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(Vertical180PhotoPanorama.class),
                animating()));
    }

    @Test
    public void testSphericalPhotoPanoramaAnimationState() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // test running state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSphericalPhotoPanoramaState(
                ArsdkFeatureAnimation.State.RUNNING));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(SphericalPhotoPanorama.class),
                animating(), animConfigMatches(new SphericalPhotoPanorama.Config())));

        // test aborting state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSphericalPhotoPanoramaState(
                ArsdkFeatureAnimation.State.CANCELING));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(SphericalPhotoPanorama.class),
                abortingAnimation()));

        // test idle state -- should do nothing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSphericalPhotoPanoramaState(
                ArsdkFeatureAnimation.State.IDLE));
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(SphericalPhotoPanorama.class),
                abortingAnimation()));

        // test that when state is unsupported, status is ANIMATING
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSphericalPhotoPanoramaState(null));
        assertThat(mChangeCnt, is(4));
        assertThat(mAnimationItf.getCurrentAnimation(), allOf(instanceOf(SphericalPhotoPanorama.class),
                animating()));
    }

    @Test
    public void testAbortAnimation() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock animating state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationFlipState(ArsdkFeatureAnimation.State.RUNNING,
                ArsdkFeatureAnimation.FlipType.FRONT));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), animating());

        // abort animation
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.animationCancel(), false));
        mAnimationItf.abortCurrentAnimation();
    }

    @Test
    public void testStartCandle() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock CANDLE animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.CANDLE)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.CANDLE));

        // test default config
        Candle.Config config = new Candle.Config();
        testStartCandle(config, EnumSet.noneOf(ArsdkFeatureAnimation.CandleConfigParam.class));

        // test speed parameter
        config.withSpeed(10.0);
        testStartCandle(config, EnumSet.of(ArsdkFeatureAnimation.CandleConfigParam.SPEED));

        // test vertical distance parameter
        config.withVerticalDistance(5.0);
        testStartCandle(config, EnumSet.of(ArsdkFeatureAnimation.CandleConfigParam.SPEED,
                ArsdkFeatureAnimation.CandleConfigParam.VERTICAL_DISTANCE));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartCandle(config, EnumSet.of(ArsdkFeatureAnimation.CandleConfigParam.SPEED,
                ArsdkFeatureAnimation.CandleConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.CandleConfigParam.PLAY_MODE));
    }

    private void testStartCandle(@NonNull Candle.Config config,
                                 @NonNull EnumSet<ArsdkFeatureAnimation.CandleConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.CandleConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.CandleConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartCandle(paramsBitField, (float) config.getSpeed(),
                        (float) config.getVerticalDistance(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationCandleState(ArsdkFeatureAnimation.State.RUNNING,
                (float) config.getSpeed(), (float) config.getVerticalDistance(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartDollySlide() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock DOLLY_SLIDE animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.DOLLY_SLIDE)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.DOLLY_SLIDE));

        // test default config
        DollySlide.Config config = new DollySlide.Config();
        testStartDollySlide(config, EnumSet.noneOf(ArsdkFeatureAnimation.DollySlideConfigParam.class));

        // test speed parameter
        config.withSpeed(1.1234567);
        testStartDollySlide(config, EnumSet.of(ArsdkFeatureAnimation.DollySlideConfigParam.SPEED));

        // test angle parameter
        config.withAngle(90.1234567);
        testStartDollySlide(config, EnumSet.of(ArsdkFeatureAnimation.DollySlideConfigParam.SPEED,
                ArsdkFeatureAnimation.DollySlideConfigParam.ANGLE));

        // test horizontal distance parameter
        config.withHorizontalDistance(10.123456789);
        testStartDollySlide(config, EnumSet.of(ArsdkFeatureAnimation.DollySlideConfigParam.SPEED,
                ArsdkFeatureAnimation.DollySlideConfigParam.ANGLE,
                ArsdkFeatureAnimation.DollySlideConfigParam.HORIZONTAL_DISTANCE));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartDollySlide(config, EnumSet.of(ArsdkFeatureAnimation.DollySlideConfigParam.SPEED,
                ArsdkFeatureAnimation.DollySlideConfigParam.ANGLE,
                ArsdkFeatureAnimation.DollySlideConfigParam.HORIZONTAL_DISTANCE,
                ArsdkFeatureAnimation.DollySlideConfigParam.PLAY_MODE));
    }

    private void testStartDollySlide(@NonNull DollySlide.Config config,
                                     @NonNull EnumSet<ArsdkFeatureAnimation.DollySlideConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.DollySlideConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.DollySlideConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartDollySlide(paramsBitField, (float) config.getSpeed(),
                        (float) Math.toRadians(config.getAngle()),
                        (float) config.getHorizontalDistance(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationDollySlideState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getSpeed(), (float) Math.toRadians(config.getAngle()),
                        (float) config.getHorizontalDistance(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartDronie() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock DRONIE animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.DRONIE)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.DRONIE));

        // test default config
        Dronie.Config config = new Dronie.Config();
        testStartDronie(config, EnumSet.noneOf(ArsdkFeatureAnimation.DronieConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartDronie(config, EnumSet.of(ArsdkFeatureAnimation.DronieConfigParam.SPEED));

        // test distance parameter
        config.withDistance(10.123456789);
        testStartDronie(config, EnumSet.of(ArsdkFeatureAnimation.DronieConfigParam.SPEED,
                ArsdkFeatureAnimation.DronieConfigParam.DISTANCE));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartDronie(config, EnumSet.of(ArsdkFeatureAnimation.DronieConfigParam.SPEED,
                ArsdkFeatureAnimation.DronieConfigParam.DISTANCE,
                ArsdkFeatureAnimation.DronieConfigParam.PLAY_MODE));
    }

    private void testStartDronie(@NonNull Dronie.Config config,
                                 @NonNull EnumSet<ArsdkFeatureAnimation.DronieConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.DronieConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.DronieConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartDronie(paramsBitField, (float) config.getSpeed(),
                        (float) config.getDistance(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationDronieState(ArsdkFeatureAnimation.State.RUNNING,
                (float) config.getSpeed(), (float) config.getDistance(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartFlip() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock FLIP animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.FLIP)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.FLIP));

        // test front direction config
        Flip.Config config = new Flip.Config(Flip.Direction.FRONT);
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartFlip(ArsdkFeatureAnimation.FlipType.FRONT), true));
        mAnimationItf.startAnimation(config);

        // test back direction config
        config = new Flip.Config(Flip.Direction.BACK);
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartFlip(ArsdkFeatureAnimation.FlipType.BACK), true));
        mAnimationItf.startAnimation(config);

        // test right direction config
        config = new Flip.Config(Flip.Direction.RIGHT);
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartFlip(ArsdkFeatureAnimation.FlipType.RIGHT), true));
        mAnimationItf.startAnimation(config);

        // test left direction config
        config = new Flip.Config(Flip.Direction.LEFT);
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartFlip(ArsdkFeatureAnimation.FlipType.LEFT), true));
        mAnimationItf.startAnimation(config);
    }

    @Test
    public void testStartHorizontalPanorama() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock HORIZONTAL_PANORAMA animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.HORIZONTAL_PANORAMA)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.HORIZONTAL_PANORAMA));

        // test default config
        HorizontalPanorama.Config config = new HorizontalPanorama.Config();
        testStartHorizontalPanorama(config, EnumSet.noneOf(ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.class));

        // test rotation angle parameter
        config.withRotationAngle(-75.123456789);
        testStartHorizontalPanorama(config, EnumSet.of(
                ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.ROTATION_ANGLE));

        // test rotation speed parameter
        config.withRotationSpeed(10.123456789);
        testStartHorizontalPanorama(config, EnumSet.of(
                ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.ROTATION_SPEED));
    }

    private void testStartHorizontalPanorama(
            @NonNull HorizontalPanorama.Config config,
            @NonNull EnumSet<ArsdkFeatureAnimation.HorizontalPanoramaConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.HorizontalPanoramaConfigParam[0]));
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartHorizontalPanorama(paramsBitField,
                        (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed())), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationHorizontalPanoramaState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed())));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartHorizontalReveal() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock HORIZONTAL_REVEAL animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.HORIZONTAL_REVEAL)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.HORIZONTAL_REVEAL));

        // test default config
        HorizontalReveal.Config config = new HorizontalReveal.Config();
        testStartHorizontalReveal(config, EnumSet.noneOf(ArsdkFeatureAnimation.HorizontalRevealConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartHorizontalReveal(config, EnumSet.of(ArsdkFeatureAnimation.HorizontalRevealConfigParam.SPEED));

        // test distance parameter
        config.withDistance(10.123456789);
        testStartHorizontalReveal(config, EnumSet.of(ArsdkFeatureAnimation.HorizontalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.HorizontalRevealConfigParam.DISTANCE));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartHorizontalReveal(config, EnumSet.of(ArsdkFeatureAnimation.HorizontalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.HorizontalRevealConfigParam.DISTANCE,
                ArsdkFeatureAnimation.HorizontalRevealConfigParam.PLAY_MODE));
    }

    private void testStartHorizontalReveal(
            @NonNull HorizontalReveal.Config config,
            @NonNull EnumSet<ArsdkFeatureAnimation.HorizontalRevealConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.HorizontalRevealConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.HorizontalRevealConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartHorizontalReveal(paramsBitField, (float) config.getSpeed(),
                        (float) config.getDistance(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationHorizontalRevealState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getSpeed(), (float) config.getDistance(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartParabola() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock PARABOLA animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.PARABOLA)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.PARABOLA));

        // test default config
        Parabola.Config config = new Parabola.Config();
        testStartParabola(config, EnumSet.noneOf(ArsdkFeatureAnimation.ParabolaConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartParabola(config, EnumSet.of(ArsdkFeatureAnimation.ParabolaConfigParam.SPEED));

        // test vertical distance parameter
        config.withVerticalDistance(10.123456789);
        testStartParabola(config, EnumSet.of(ArsdkFeatureAnimation.ParabolaConfigParam.SPEED,
                ArsdkFeatureAnimation.ParabolaConfigParam.VERTICAL_DISTANCE));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartParabola(config, EnumSet.of(ArsdkFeatureAnimation.ParabolaConfigParam.SPEED,
                ArsdkFeatureAnimation.ParabolaConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.ParabolaConfigParam.PLAY_MODE));
    }

    private void testStartParabola(@NonNull Parabola.Config config,
                                   @NonNull EnumSet<ArsdkFeatureAnimation.ParabolaConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.ParabolaConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.ParabolaConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartParabola(paramsBitField, (float) config.getSpeed(),
                        (float) config.getVerticalDistance(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationParabolaState(ArsdkFeatureAnimation.State.RUNNING,
                (float) config.getSpeed(), (float) config.getVerticalDistance(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartSpiral() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock SPIRAL animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.SPIRAL)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.SPIRAL));

        // test default config
        Spiral.Config config = new Spiral.Config();
        testStartSpiral(config, EnumSet.noneOf(ArsdkFeatureAnimation.SpiralConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartSpiral(config, EnumSet.of(ArsdkFeatureAnimation.SpiralConfigParam.SPEED));

        // test radius variation parameter
        config.withRadiusVariation(2.123456789);
        testStartSpiral(config, EnumSet.of(ArsdkFeatureAnimation.SpiralConfigParam.SPEED,
                ArsdkFeatureAnimation.SpiralConfigParam.RADIUS_VARIATION));

        // test vertical distance parameter
        config.withVerticalDistance(10.123456789);
        testStartSpiral(config, EnumSet.of(ArsdkFeatureAnimation.SpiralConfigParam.SPEED,
                ArsdkFeatureAnimation.SpiralConfigParam.RADIUS_VARIATION,
                ArsdkFeatureAnimation.SpiralConfigParam.VERTICAL_DISTANCE));

        // test revolution amount parameter
        config.withRevolutionAmount(0.123456789);
        testStartSpiral(config, EnumSet.of(ArsdkFeatureAnimation.SpiralConfigParam.SPEED,
                ArsdkFeatureAnimation.SpiralConfigParam.RADIUS_VARIATION,
                ArsdkFeatureAnimation.SpiralConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.SpiralConfigParam.REVOLUTION_NB));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartSpiral(config, EnumSet.of(ArsdkFeatureAnimation.SpiralConfigParam.SPEED,
                ArsdkFeatureAnimation.SpiralConfigParam.RADIUS_VARIATION,
                ArsdkFeatureAnimation.SpiralConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.SpiralConfigParam.REVOLUTION_NB,
                ArsdkFeatureAnimation.SpiralConfigParam.PLAY_MODE));
    }

    private void testStartSpiral(@NonNull Spiral.Config config,
                                 @NonNull EnumSet<ArsdkFeatureAnimation.SpiralConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.SpiralConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartSpiral(paramsBitField, (float) config.getSpeed(),
                        (float) config.getRadiusVariation(), (float) config.getVerticalDistance(),
                        (float) config.getRevolutionAmount(), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationSpiralState(ArsdkFeatureAnimation.State.RUNNING,
                (float) config.getSpeed(), (float) config.getRadiusVariation(),
                (float) config.getVerticalDistance(), (float) config.getRevolutionAmount(), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartVerticalReveal() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock VERTICAL_REVEAL animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.VERTICAL_REVEAL)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.VERTICAL_REVEAL));

        // test default config
        VerticalReveal.Config config = new VerticalReveal.Config();
        testStartVerticalReveal(config, EnumSet.noneOf(ArsdkFeatureAnimation.VerticalRevealConfigParam.class));

        // test vertical speed parameter
        config.withVerticalSpeed(1.123456789);
        testStartVerticalReveal(config, EnumSet.of(ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED));

        // test vertical distance parameter
        config.withVerticalDistance(10.123456789);
        testStartVerticalReveal(config, EnumSet.of(ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.VERTICAL_DISTANCE));

        // test rotation angle parameter
        config.withRotationAngle(0.123456789);
        testStartVerticalReveal(config, EnumSet.of(ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_ANGLE));

        // test rotation speed parameter
        config.withRotationSpeed(5.123456789);
        testStartVerticalReveal(config, EnumSet.of(ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_SPEED));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartVerticalReveal(config, EnumSet.of(ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_SPEED,
                ArsdkFeatureAnimation.VerticalRevealConfigParam.PLAY_MODE));
    }

    private void testStartVerticalReveal(@NonNull VerticalReveal.Config config,
                                         @NonNull EnumSet<ArsdkFeatureAnimation.VerticalRevealConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.VerticalRevealConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartVerticalReveal(paramsBitField, (float) config.getVerticalSpeed(),
                        (float) config.getVerticalDistance(), (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationVerticalRevealState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getVerticalSpeed(), (float) config.getVerticalDistance(),
                        (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()),
                        mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartVertigo() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock Vertigo animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.VERTIGO)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.VERTIGO));

        // test default config
        Vertigo.Config config = new Vertigo.Config();
        testStartVertigo(config, EnumSet.noneOf(ArsdkFeatureAnimation.VertigoConfigParam.class));

        // test duration parameter
        config.withDuration(20.123456789);
        testStartVertigo(config, EnumSet.of(ArsdkFeatureAnimation.VertigoConfigParam.DURATION));

        // test maximum zoom level parameter
        config.withMaxZoomLevel(10.123456789);
        testStartVertigo(config, EnumSet.of(ArsdkFeatureAnimation.VertigoConfigParam.DURATION,
                ArsdkFeatureAnimation.VertigoConfigParam.MAX_ZOOM_LEVEL));

        // test finish action
        config.withFinishAction(Vertigo.FinishAction.UNZOOM);
        testStartVertigo(config, EnumSet.of(ArsdkFeatureAnimation.VertigoConfigParam.DURATION,
                ArsdkFeatureAnimation.VertigoConfigParam.MAX_ZOOM_LEVEL,
                ArsdkFeatureAnimation.VertigoConfigParam.FINISH_ACTION));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartVertigo(config, EnumSet.of(ArsdkFeatureAnimation.VertigoConfigParam.DURATION,
                ArsdkFeatureAnimation.VertigoConfigParam.MAX_ZOOM_LEVEL,
                ArsdkFeatureAnimation.VertigoConfigParam.FINISH_ACTION,
                ArsdkFeatureAnimation.VertigoConfigParam.PLAY_MODE));
    }

    private void testStartVertigo(@NonNull Vertigo.Config config,
                                  @NonNull EnumSet<ArsdkFeatureAnimation.VertigoConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.VertigoConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.VertigoConfigParam[0]));
        ArsdkFeatureAnimation.VertigoFinishAction action = ArsdkFeatureAnimation.VertigoFinishAction.NONE;
        if (config.getFinishAction() != null) {
            switch (config.getFinishAction()) {
                case NONE:
                    action = ArsdkFeatureAnimation.VertigoFinishAction.NONE;
                    break;
                case UNZOOM:
                    action = ArsdkFeatureAnimation.VertigoFinishAction.UNZOOM;
                    break;
            }
        }
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartVertigo(paramsBitField, (float) config.getDuration(),
                        (float) config.getMaxZoomLevel(), action, mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationVertigoState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getDuration(), (float) config.getMaxZoomLevel(), action, mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartTwistUp() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock TWIST_UP animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.TWIST_UP)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.TWIST_UP));

        // test default config
        GenericTwistUp.TwistUpConfig config = new GenericTwistUp.TwistUpConfig();
        testStartTwistUp(config, EnumSet.noneOf(ArsdkFeatureAnimation.TwistUpConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED));

        // test vertical distance parameter
        config.withVerticalDistance(10.123456789);
        testStartTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE));

        // test rotation angle parameter
        config.withRotationAngle(0.123456789);
        testStartTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE));

        // test rotation speed parameter
        config.withRotationSpeed(5.123456789);
        testStartTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_SPEED));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.PLAY_MODE));
    }

    private void testStartTwistUp(@NonNull GenericTwistUp.TwistUpConfig config,
                                  @NonNull EnumSet<ArsdkFeatureAnimation.TwistUpConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.TwistUpConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartTwistUp(paramsBitField, (float) config.getSpeed(),
                        (float) config.getVerticalDistance(), (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationTwistUpState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getSpeed(), (float) config.getVerticalDistance(),
                        (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartPositionTwistUp() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock POSITION_TWIST_UP animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.POSITION_TWIST_UP)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.POSITION_TWIST_UP));

        // test default config
        GenericTwistUp.PositionTwistUpConfig config = new GenericTwistUp.PositionTwistUpConfig();
        testStartPositionTwistUp(config, EnumSet.noneOf(ArsdkFeatureAnimation.TwistUpConfigParam.class));

        // test speed parameter
        config.withSpeed(1.123456789);
        testStartPositionTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED));

        // test vertical distance parameter
        config.withVerticalDistance(10.123456789);
        testStartPositionTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE));

        // test rotation angle parameter
        config.withRotationAngle(0.123456789);
        testStartPositionTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE));

        // test rotation speed parameter
        config.withRotationSpeed(5.123456789);
        testStartPositionTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_SPEED));

        // test play mode parameter
        config.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        testStartPositionTwistUp(config, EnumSet.of(ArsdkFeatureAnimation.TwistUpConfigParam.SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE,
                ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_SPEED,
                ArsdkFeatureAnimation.TwistUpConfigParam.PLAY_MODE));
    }

    private void testStartPositionTwistUp(@NonNull GenericTwistUp.PositionTwistUpConfig config,
                                          @NonNull EnumSet<ArsdkFeatureAnimation.TwistUpConfigParam> params) {
        int paramsBitField = ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                params.toArray(new ArsdkFeatureAnimation.TwistUpConfigParam[0]));
        ArsdkFeatureAnimation.PlayMode mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (config.getMode() != null) {
            switch (config.getMode()) {
                case ONCE:
                    mode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    mode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.animationStartPositionTwistUp(paramsBitField, (float) config.getSpeed(),
                        (float) config.getVerticalDistance(), (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()), mode), true));
        mAnimationItf.startAnimation(config);
        mMockArsdkCore.assertNoExpectation();

        // mock reception
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeAnimationPositionTwistUpState(ArsdkFeatureAnimation.State.RUNNING,
                        (float) config.getSpeed(), (float) config.getVerticalDistance(),
                        (float) Math.toRadians(config.getRotationAngle()),
                        (float) Math.toRadians(config.getRotationSpeed()), mode));

        assert mAnimationItf.getCurrentAnimation() != null;
        assertThat(mAnimationItf.getCurrentAnimation().matchesConfig(config), is(true));
    }

    @Test
    public void testStartHorizontal180PhotoPanorama() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock Horizontal180PhotoPanorama animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.HORIZONTAL_180_PHOTO_PANORAMA)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.HORIZONTAL_180_PHOTO_PANORAMA));

        // test Horizontal180PhotoPanorama config
        Horizontal180PhotoPanorama.Config config = new Horizontal180PhotoPanorama.Config();
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.animationStartHorizontal180PhotoPanorama(),
                true));
        mAnimationItf.startAnimation(config);
    }

    @Test
    public void testStartVertical180PhotoPanorama() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock Vertical180PhotoPanorama animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.VERTICAL_180_PHOTO_PANORAMA)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.VERTICAL_180_PHOTO_PANORAMA));

        // test Vertical180PhotoPanorama config
        Vertical180PhotoPanorama.Config config = new Vertical180PhotoPanorama.Config();
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.animationStartVertical180PhotoPanorama(),
                true));
        mAnimationItf.startAnimation(config);
    }

    @Test
    public void testStartSphericalPhotoPanorama() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // mock SphericalPhotoPanorama animations available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeAnimationAvailability(
                ArsdkFeatureAnimation.Type.toBitField(ArsdkFeatureAnimation.Type.SPHERICAL_PHOTO_PANORAMA)));
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), contains(Animation.Type.SPHERICAL_PHOTO_PANORAMA));

        // test SphericalPhotoPanorama config
        SphericalPhotoPanorama.Config config = new SphericalPhotoPanorama.Config();
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.animationStartSphericalPhotoPanorama(),
                true));
        mAnimationItf.startAnimation(config);
    }
}
