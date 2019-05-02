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

package com.parrot.drone.groundsdk.internal;

import android.os.Bundle;

import com.parrot.drone.groundsdk.GroundSdk;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
public class GroundSdkSessionsTest extends GroundSdkTestBase {

    private Bundle mSessionBundle1;

    private Bundle mSessionBundle2;

    @Override
    public void setUp() {
        super.setUp();
        mSessionBundle1 = new Bundle();
        mSessionBundle2 = new Bundle();
    }

    /**
     * Checks that engines are started and stopped
     */
    @Test
    public void testOpenClose() {
        GroundSdk gsdk1 = GroundSdk.newSession(mContext, mSessionBundle1);
        assertThat(mMockEngine.isStarted(), is(true));
        GroundSdk gsdk2 = GroundSdk.newSession(mContext, mSessionBundle2);
        assertThat(mMockEngine.isStarted(), is(true));
        gsdk1.close();
        assertThat(mMockEngine.isStarted(), is(true));
        gsdk2.close();
        assertThat(mMockEngine.isStarted(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void testGetDroneListGroundSdkClosed() {
        GroundSdk gsdk = GroundSdk.newSession(mContext, mSessionBundle1);
        gsdk.close();
        gsdk.getDroneList(null, null);
    }

    /**
     * Checks that engines are not stopped when a GroundSdk is retained
     */
    @Test
    public void testRetainClose() {
        GroundSdk gsdk1 = GroundSdk.newSession(mContext, mSessionBundle1);
        assertThat(mMockEngine.isStarted(), is(true));
        gsdk1.retain(mSessionBundle1);
        gsdk1.close();
        assertThat(mMockEngine.isStarted(), is(true));

        gsdk1 = GroundSdk.newSession(mContext, mSessionBundle1);
        gsdk1.close();
        assertThat(mMockEngine.isStarted(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void testGetDroneGroundSdkClosed() {
        GroundSdk gsdk = GroundSdk.newSession(mContext, mSessionBundle1);
        gsdk.close();
        gsdk.getDroneList(it -> true, obj -> {
            throw new AssertionError();
        });
    }
}

