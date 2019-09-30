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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.animation;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.AnimationItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Dronie;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.AnimationMatcher.abortingAnimation;
import static com.parrot.drone.groundsdk.AnimationMatcher.animProgressIs;
import static com.parrot.drone.groundsdk.AnimationMatcher.animating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AnimationItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private AnimationItf mAnimationItf;

    private AnimationItfCore mAnimationItfImpl;

    private Backend mBackend;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mStore.registerObserver(AnimationItf.class, () -> mChangeCnt++);
        mBackend = new Backend();
        mAnimationItfImpl = new AnimationItfCore(mStore, mBackend);
        mAnimationItf = mAnimationItfImpl;
    }

    @After
    public void teardown() {
        mAnimationItfImpl = null;
        mBackend = null;
        mStore = null;
    }

    @Test
    public void testAvailableAnimations() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getAvailableAnimations(), empty());

        // mock update from low-level
        mAnimationItfImpl.updateAvailableAnimations(EnumSet.allOf(Animation.Type.class)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), containsInAnyOrder(Animation.Type.values()));

        // mock same update from low-level -- no notification expected
        mAnimationItfImpl.updateAvailableAnimations(EnumSet.allOf(Animation.Type.class)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getAvailableAnimations(), containsInAnyOrder(Animation.Type.values()));

        // mock another update without notification
        mAnimationItfImpl.updateAvailableAnimations(EnumSet.of(Animation.Type.FLIP));
        assertThat(mChangeCnt, is(2));

        // another update and notify
        mAnimationItfImpl.updateAvailableAnimations(EnumSet.of(Animation.Type.CANDLE)).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getAvailableAnimations(), containsInAnyOrder(Animation.Type.CANDLE));
    }

    @Test
    public void testAnimationStatus() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getCurrentAnimation(), nullValue());

        // mock update from low-level
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), animating());

        // mock same update from low-level -- no notification expected
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), animating());

        // mock another update without notification
        mAnimationItfImpl.clearAnimation();
        assertThat(mChangeCnt, is(2));

        // another update and notify
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ABORTING).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), abortingAnimation());
    }

    @Test
    public void testAnimationProgress() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getCurrentAnimation(), nullValue());

        // mock update from low-level
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ANIMATING)
                         .updateCurrentAnimationProgress(50).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), animProgressIs(50));

        // mock same update from low-level -- no notification expected
        mAnimationItfImpl.updateCurrentAnimationProgress(50).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), animProgressIs(50));

        // mock another update without notification
        mAnimationItfImpl.updateCurrentAnimationProgress(75);
        assertThat(mChangeCnt, is(2));

        // another update and notify
        mAnimationItfImpl.updateCurrentAnimationProgress(100).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), animProgressIs(100));
    }

    @Test
    public void testCurrentAnimation() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getCurrentAnimation(), nullValue());

        // mock update from low-level
        AnimationCore anim = new SpiralCore(1.0, 2.0, 3.0, 4.0, Animation.Mode.ONCE);
        mAnimationItfImpl.updateAnimation(anim, Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), is(anim));

        // mock same update from low-level -- no notification expected
        mAnimationItfImpl.updateAnimation(anim, Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), is(anim));

        // mock update with a different object but equal values -- no notification expected
        mAnimationItfImpl.updateAnimation(new SpiralCore(1.0, 2.0, 3.0, 4.0, Animation.Mode.ONCE),
                Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.getCurrentAnimation(), is(anim));

        // mock another update without notification
        anim = new VerticalRevealCore(5.0, 6.0, 7.0, 8.0, Animation.Mode.ONCE_THEN_MIRRORED);
        mAnimationItfImpl.updateAnimation(anim, Animation.Status.ANIMATING);
        assertThat(mChangeCnt, is(2));

        // another update and notify
        anim = new FlipCore(Flip.Direction.RIGHT);
        mAnimationItfImpl.updateAnimation(anim, Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.getCurrentAnimation(), is(anim));
    }

    @Test
    public void testStartAnimation() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getAvailableAnimations(), empty());

        // test that starting an unavailable animation is forbidden
        Animation.Config animConfig = new Dronie.Config();
        assertThat(mAnimationItf.startAnimation(animConfig), is(false));
        assertThat(mBackend.mAnimationConfig, nullValue());

        // test that starting an available animation is permitted
        mAnimationItfImpl.updateAvailableAnimations(EnumSet.of(Animation.Type.DRONIE)).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.startAnimation(animConfig), is(true));
        assertThat(mBackend.mAnimationConfig, is(animConfig));
    }

    @Test
    public void testAbortAnimation() {
        mAnimationItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mAnimationItf.getCurrentAnimation(), nullValue());

        // test that aborting is forbidden when there is no current animation
        assertThat(mAnimationItf.abortCurrentAnimation(), is(false));
        assertThat(mBackend.mAnimationAbort, is(false));

        // test that aborting is forbidden when status is ABORTING
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ABORTING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mAnimationItf.abortCurrentAnimation(), is(false));
        assertThat(mBackend.mAnimationAbort, is(false));

        // test that aborting is permitted when status is ANIMATING
        mAnimationItfImpl.updateAnimation(AnimationCore.unidentified(), Animation.Status.ANIMATING).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mAnimationItf.abortCurrentAnimation(), is(true));
        assertThat(mBackend.mAnimationAbort, is(true));
    }

    private static final class Backend implements AnimationItfCore.Backend {

        Animation.Config mAnimationConfig;

        boolean mAnimationAbort;

        @Override
        public boolean startAnimation(@NonNull Animation.Config animationConfig) {
            mAnimationConfig = animationConfig;
            return true;
        }

        @Override
        public boolean abortCurrentAnimation() {
            mAnimationAbort = true;
            return true;
        }
    }
}
