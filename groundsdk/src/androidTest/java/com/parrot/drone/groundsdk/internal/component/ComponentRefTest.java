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

package com.parrot.drone.groundsdk.internal.component;

import com.parrot.drone.groundsdk.internal.session.MockSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test componentRef.
 */
public class ComponentRefTest {

    private ComponentStore<CompType> mStore;

    private int mainChangeCnt;

    private int subChangeCnt;

    private MockSession mMockSession;

    @Before
    public void setUp() {
        mStore = new ComponentStore<>();
        mMockSession = new MockSession();
        mainChangeCnt = 0;
        subChangeCnt = 0;
    }

    @After
    public void teardown() {
        mStore = null;
    }

    @Test
    public void testMainComponentRef() {
        ComponentRef<CompType, MainComp> mainCompRef =
                new ComponentRef<>(mMockSession, obj -> mainChangeCnt++, mStore, MainComp.class);

        // add main component, check notified
        mStore.add(new MainCompImpl(), MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(1));
        assertThat(mainCompRef.get(), is(notNullValue()));

        // update component, check notified
        mStore.notifyUpdated(MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(2));

        // remove component, check notified
        mStore.remove(MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(3));

        mainCompRef.release();
    }

    @Test
    public void testSubComponentRef() {
        ComponentRef<CompType, MainComp> mainCompRef =
                new ComponentRef<>(mMockSession, obj -> mainChangeCnt++, mStore, MainComp.class);

        ComponentRef<CompType, SubComp> subCompRef =
                new ComponentRef<>(mMockSession, obj -> subChangeCnt++, mStore, SubComp.class);

        // add sub component, check notified
        mStore.add(new SubCompImpl(), SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(1));
        assertThat(mainCompRef.get(), is(notNullValue()));
        assertThat(subChangeCnt, is(1));
        assertThat(subCompRef.get(), is(notNullValue()));

        // update component, check notified
        mStore.notifyUpdated(SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(2));
        assertThat(subChangeCnt, is(2));

        // remove component, check notified
        mStore.remove(SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(3));
        assertThat(subChangeCnt, is(3));

        mainCompRef.release();
        subCompRef.release();
    }
}

