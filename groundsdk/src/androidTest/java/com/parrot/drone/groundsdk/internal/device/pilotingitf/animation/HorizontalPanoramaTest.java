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
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalPanorama;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HorizontalPanoramaTest {

    private HorizontalPanorama.Config mAnimConfig;

    private HorizontalPanoramaCore mAnimCore;

    @Before
    public void setUp() {
        mAnimConfig = new HorizontalPanorama.Config();
        mAnimCore = new HorizontalPanoramaCore(-3.0, 5.0);
    }

    @Test
    public void testConfig() {
        assertThat(mAnimConfig.getAnimationType(), is(Animation.Type.HORIZONTAL_PANORAMA));

        assertThat(mAnimConfig.usesCustomRotationAngle(), is(false));
        assertThat(mAnimConfig.usesCustomRotationSpeed(), is(false));

        assertThat(mAnimConfig.withRotationAngle(-3.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomRotationAngle(), is(true));
        assertThat(mAnimConfig.getRotationAngle(), is(-3.0));

        assertThat(mAnimConfig.withRotationSpeed(5.0), is(mAnimConfig));
        assertThat(mAnimConfig.usesCustomRotationSpeed(), is(true));
        assertThat(mAnimConfig.getRotationSpeed(), is(5.0));
    }

    @Test
    public void testAnimation() {
        assertThat(mAnimCore.getType(), is(Animation.Type.HORIZONTAL_PANORAMA));
        assertThat(mAnimCore.getRotationAngle(), is(-3.0));
        assertThat(mAnimCore.getRotationSpeed(), is(5.0));
    }

    @Test
    public void testMatchesConfig() {
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withRotationAngle(-2.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withRotationAngle(-3.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));

        mAnimConfig.withRotationSpeed(4.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(false));
        mAnimConfig.withRotationSpeed(5.0);
        assertThat(mAnimCore.matchesConfig(mAnimConfig), is(true));
    }
}
