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

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.VerticalReveal;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class VerticalRevealTest {

    private VerticalReveal.Config mAnimConfig;

    private VerticalRevealCore mAnimCore;

    @Before
    public void setUp() {
        mAnimConfig = new VerticalReveal.Config();
        mAnimCore = new VerticalRevealCore(3.0, 5.0, -7.0, 9.0,
                Animation.Mode.ONCE_THEN_MIRRORED);
    }

    @Test
    public void testConfig() {
        assertThat(mAnimConfig.getAnimationType(), is(Animation.Type.VERTICAL_REVEAL));

        assertThat(mAnimConfig.usesCustomVerticalSpeed(), is(false));
        assertThat(mAnimConfig.usesCustomVerticalDistance(), is(false));
        assertThat(mAnimConfig.usesCustomRotationAngle(), is(false));
        assertThat(mAnimConfig.usesCustomRotationSpeed(), is(false));
        assertThat(mAnimConfig.getMode(), nullValue());

        assertThat(mAnimConfig.withVerticalSpeed(3.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomVerticalSpeed(), is(true));
        assertThat(mAnimConfig.getVerticalSpeed(), is(3.0));

        assertThat(mAnimConfig.withVerticalDistance(5.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomVerticalDistance(), is(true));
        assertThat(mAnimConfig.getVerticalDistance(), is(5.0));

        assertThat(mAnimConfig.withRotationAngle(-7.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomRotationAngle(), is(true));
        assertThat(mAnimConfig.getRotationAngle(), is(-7.0));

        assertThat(mAnimConfig.withRotationSpeed(9.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomRotationSpeed(), is(true));
        assertThat(mAnimConfig.getRotationSpeed(), is(9.0));

        assertThat(mAnimConfig.withMode(Animation.Mode.ONCE_THEN_MIRRORED), is(mAnimConfig));
        assertThat(mAnimConfig.getMode(), is(Animation.Mode.ONCE_THEN_MIRRORED));
    }

    @Test
    public void testAnimation() {
        assertThat(mAnimCore.getType(), is(Animation.Type.VERTICAL_REVEAL));
        assertThat(mAnimCore.getVerticalSpeed(), is(3.0));
        assertThat(mAnimCore.getVerticalDistance(), is(5.0));
        assertThat(mAnimCore.getRotationAngle(), is(-7.0));
        assertThat(mAnimCore.getRotationSpeed(), is(9.0));
        assertThat(mAnimCore.getMode(), is(Animation.Mode.ONCE_THEN_MIRRORED));
    }

    @Test
    public void testMatchesConfig() {
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withVerticalSpeed(2.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withVerticalSpeed(3.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withVerticalDistance(4.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withVerticalDistance(5.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withRotationAngle(-6.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withRotationAngle(-7.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withRotationSpeed(8.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withRotationSpeed(9.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withMode(Animation.Mode.ONCE);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withMode(Animation.Mode.ONCE_THEN_MIRRORED);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));
    }
}
