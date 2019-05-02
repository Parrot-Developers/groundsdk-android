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

package com.parrot.drone.groundsdk.internal.facility;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.UserAccount;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserAccountTest {

    private MockComponentStore<Facility> mStore;

    private UserAccountCore.Backend mMockBackend;

    private UserAccountCore mUserAccountCore;

    private UserAccount mUserAccount;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mMockBackend = mock(UserAccountCore.Backend.class);
        mUserAccountCore = new UserAccountCore(mStore, mMockBackend);
        mUserAccount = mStore.get(UserAccount.class);
        mStore.registerObserver(UserAccount.class, () -> {
            mUserAccount = mStore.get(UserAccount.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mUserAccount, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mUserAccountCore.publish();
        assertThat(mUserAccount, is(mUserAccountCore));
        assertThat(mComponentChangeCnt, is(1));

        mUserAccountCore.unpublish();
        assertThat(mUserAccount, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testSetAccount() {
        mUserAccountCore.publish();

        assertThat(mComponentChangeCnt, is(1));

        mUserAccount.set("accountProvider", "accountId", UserAccount.AccountlessPersonalDataPolicy.DENY_UPLOAD);
        verify(mMockBackend).setUserAccount("accountProvider accountId",
                UserAccount.AccountlessPersonalDataPolicy.DENY_UPLOAD);

        mUserAccount.set("accountProvider", "accountId2", UserAccount.AccountlessPersonalDataPolicy.ALLOW_UPLOAD);
        verify(mMockBackend).setUserAccount("accountProvider accountId2",
                UserAccount.AccountlessPersonalDataPolicy.ALLOW_UPLOAD);
    }

    @Test
    public void testClearAccount() {
        mUserAccountCore.publish();

        assertThat(mComponentChangeCnt, is(1));

        mUserAccount.clear(UserAccount.AnonymousDataPolicy.ALLOW_UPLOAD);
        verify(mMockBackend).clearUserAccount(UserAccount.AnonymousDataPolicy.ALLOW_UPLOAD);

        mUserAccount.clear(UserAccount.AnonymousDataPolicy.DENY_UPLOAD);
        verify(mMockBackend).clearUserAccount(UserAccount.AnonymousDataPolicy.DENY_UPLOAD);
    }
}