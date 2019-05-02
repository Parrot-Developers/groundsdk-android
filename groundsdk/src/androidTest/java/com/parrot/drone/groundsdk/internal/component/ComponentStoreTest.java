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
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests Component store.
 */
public class ComponentStoreTest {

    private ComponentStore<CompType> mStore;

    private MockSession mMockSession;

    private int mainChangeCnt;

    private int subChangeCnt;

    private int firstObserverCnt;

    private int noMoreObserverCnt;

    @Before
    public void setUp() {
        mStore = new ComponentStore<>();
        mMockSession = new MockSession();
        mainChangeCnt = 0;
        subChangeCnt = 0;
        firstObserverCnt = 0;
        noMoreObserverCnt = 0;
    }

    @After
    public void teardown() {
        mStore = null;
    }

    @Test
    public void testAddComponent() {
        ComponentStore.Observer observer = () -> mainChangeCnt++;
        mStore.registerObserver(MainComp.class, observer);
        assertThat(mainChangeCnt, is(0));

        // notify updated, check not notified as component is not in the store
        mStore.notifyUpdated(MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(0));

        // add component, check listener notified
        mStore.add(new MainCompImpl(), MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(1));

        // get component, check its expected class
        MainComp mainComp = mStore.get(mMockSession, MainComp.class);
        assertThat(mainComp, is(notNullValue()));
        assertThat(mainComp.mainFunc(), is("mainFunc"));

        // notify updated, check notified
        mStore.notifyUpdated(MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(2));

        // remove component, check notified
        mStore.remove(MainCompImpl.DESC);
        assertThat(mainChangeCnt, is(3));

        mStore.unregisterObserver(MainComp.class, observer);
    }

    @Test
    public void testAddSubComponent() {
        ComponentStore.Observer mainObserver = () -> mainChangeCnt++;
        mStore.registerObserver(MainComp.class, mainObserver);
        assertThat(mainChangeCnt, is(0));

        ComponentStore.Observer subObserver = () -> subChangeCnt++;
        mStore.registerObserver(SubComp.class, subObserver);
        assertThat(mainChangeCnt, is(0));

        // notify updated, check not notified as component is not in the store
        mStore.notifyUpdated(SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(0));
        assertThat(subChangeCnt, is(0));

        // add sub component, check main and sub listener notified
        mStore.add(new SubCompImpl(), SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(1));
        assertThat(subChangeCnt, is(1));

        // get as main component, check its expected class
        MainComp mainComp = mStore.get(mMockSession, MainComp.class);
        assertThat(mainComp, is(notNullValue()));
        assertThat(mainComp.mainFunc(), is("mainFunc on sub"));

        // get as sub component, check its expected class
        SubComp subComp = mStore.get(mMockSession, SubComp.class);
        assertThat(subComp, is(notNullValue()));
        assertThat(subComp.mainFunc(), is("mainFunc on sub"));
        assertThat(subComp.subFunc(), is("subFunc"));

        // notify updated, check both main and sub are notified
        mStore.notifyUpdated(SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(2));
        assertThat(subChangeCnt, is(2));

        // remove component, check notified
        mStore.remove(SubCompImpl.DESC);
        assertThat(mainChangeCnt, is(3));
        assertThat(subChangeCnt, is(3));

        // get as main component, check is nil
        mainComp = mStore.get(mMockSession, MainComp.class);
        assertThat(mainComp, is(nullValue()));

        // get as sub component, check is nil
        subComp = mStore.get(mMockSession, SubComp.class);
        assertThat(subComp, is(nullValue()));

        mStore.unregisterObserver(MainComp.class, mainObserver);
        mStore.unregisterObserver(SubComp.class, subObserver);
    }

    @Test
    public void testObservedListener() {
        SubCompImpl subComp = new SubCompImpl() {

            @Override
            public void onObserved() {
                firstObserverCnt++;
            }

            @Override
            public void onNoMoreObserved() {
                noMoreObserverCnt++;
            }
        };
        mStore.add(subComp, SubCompImpl.DESC);

        // no observer registered
        assertThat(firstObserverCnt, is(0));
        assertThat(noMoreObserverCnt, is(0));

        // register an observer, check notified for first observer
        ComponentStore.Observer observer1 = () -> {
        };
        mStore.registerObserver(SubComp.class, observer1);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(0));

        // register another observer, check not notified
        ComponentStore.Observer observer2 = () -> {
        };
        mStore.registerObserver(SubComp.class, observer2);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(0));

        // register another observer (for the parent), check not notified
        ComponentStore.Observer observer3 = () -> {
        };
        mStore.registerObserver(MainComp.class, observer3);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(0));

        // unregister all observers but one, check not notified
        mStore.unregisterObserver(SubComp.class, observer2);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(0));
        mStore.unregisterObserver(SubComp.class, observer1);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(0));

        // unregister the last observer, check notified there's no more observer
        mStore.unregisterObserver(MainComp.class, observer3);
        assertThat(firstObserverCnt, is(1));
        assertThat(noMoreObserverCnt, is(1));


        // register main component observer, check notified for first observer
        ComponentStore.Observer observer4 = () -> {
        };
        mStore.registerObserver(MainComp.class, observer4);
        assertThat(firstObserverCnt, is(2));
        assertThat(noMoreObserverCnt, is(1));

        // unregister observer, check notified there's no more observer
        mStore.unregisterObserver(MainComp.class, observer4);
        assertThat(firstObserverCnt, is(2));
        assertThat(noMoreObserverCnt, is(2));

        mStore.remove(SubCompImpl.DESC);
        assertThat(firstObserverCnt, is(2));
        assertThat(noMoreObserverCnt, is(2));


        // register observer before adding the component
        ComponentStore.Observer observer5 = () -> {
        };
        mStore.registerObserver(SubComp.class, observer5);
        assertThat(firstObserverCnt, is(2));
        assertThat(noMoreObserverCnt, is(2));
        mStore.add(subComp, SubCompImpl.DESC);
        assertThat(firstObserverCnt, is(3));
        assertThat(noMoreObserverCnt, is(2));

        mStore.remove(SubCompImpl.DESC);
        mStore.unregisterObserver(SubComp.class, observer5);

        // register observer on main component before adding sub component
        ComponentStore.Observer observer6 = () -> {
        };
        mStore.registerObserver(MainComp.class, observer6);
        assertThat(firstObserverCnt, is(3));
        assertThat(noMoreObserverCnt, is(2));
        mStore.add(subComp, SubCompImpl.DESC);
        assertThat(firstObserverCnt, is(4));
        assertThat(noMoreObserverCnt, is(2));

        mStore.remove(SubCompImpl.DESC);
        mStore.unregisterObserver(MainComp.class, observer6);
    }
}
