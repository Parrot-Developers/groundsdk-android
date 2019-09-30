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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.session.Session;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ComponentCoreTest {

    private ComponentCore mComponent;

    private int mChangeCnt;

    @Before
    public void setUp() {
        ComponentStore<CompType> store = new ComponentStore<>();
        store.registerObserver(CompType.class, () -> mChangeCnt++);

        mComponent = new ComponentCore(ComponentDescriptor.of(CompType.class), store) {

            @NonNull
            @Override
            protected Object getProxy(@NonNull Session session) {
                return this;
            }
        };

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // component should not be published at first
        assertThat(mComponent.isPublished(), is(false));
        assertThat(mChangeCnt, is(0));

        // publish component
        mComponent.publish();

        // should now be published and have triggered a change notification
        assertThat(mComponent.isPublished(), is(true));
        assertThat(mChangeCnt, is(1));

        // try to publish component again
        mComponent.publish();

        // should neither change publication state nor trigger a change
        assertThat(mComponent.isPublished(), is(true));
        assertThat(mChangeCnt, is(1));

        // unpublish component
        mComponent.unpublish();

        // should now be unpublished and have triggered a change notification
        assertThat(mComponent.isPublished(), is(false));
        assertThat(mChangeCnt, is(2));

        // try to publish component again
        mComponent.unpublish();

        // should neither change publication state nor trigger a change
        assertThat(mComponent.isPublished(), is(false));
        assertThat(mChangeCnt, is(2));
    }
}
