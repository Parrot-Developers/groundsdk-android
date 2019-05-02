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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.Context;
import android.content.SharedPreferences;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.UserAccount;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class UserAccountEngineTest {


    private UserAccountEngine mEngine;

    private UserAccountInfo mUserAccountInfo;

    private MockComponentStore<Facility> mFacilityStore;

    private UserAccount mUserAccount;

    private int mFacilityChangeCnt;

    private SharedPreferences.Editor mMockEditor;

    private static final Date EPOCH = new Date(0);

    @Before
    public void setUp() {
        mFacilityStore = new MockComponentStore<>();
        mFacilityStore.registerObserver(UserAccount.class, () -> {
            mUserAccount = mFacilityStore.get(UserAccount.class);
            mFacilityChangeCnt++;
        });
        mFacilityChangeCnt = 0;

        Context context = mock(Context.class);
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        mMockEditor = mock(SharedPreferences.Editor.class, RETURNS_SELF);
        doReturn(mMockEditor).when(mockPrefs).edit();
        doReturn(mockPrefs).when(context).getSharedPreferences(
                UserAccountEngine.Persistence.PREF_FILE, Context.MODE_PRIVATE);

        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = new UserAccountEngine(MockEngineController.create(context, utilities, mFacilityStore));

        // persisted data should be loaded
        verify(mockPrefs).getString(UserAccountEngine.Persistence.PREF_KEY_ACCOUNT, null);
        verify(mockPrefs).getLong(UserAccountEngine.Persistence.PREF_KEY_DATE, 0);
        verify(mockPrefs).getBoolean(UserAccountEngine.Persistence.PREF_KEY_ANONYMOUS_DATA, false);
        verify(mockPrefs).getLong(UserAccountEngine.Persistence.PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE, 0);
        // engine should publish its utility
        mUserAccountInfo = utilities.getUtility(UserAccountInfo.class);
        assertThat(mUserAccountInfo, notNullValue());

        clearInvocations(mMockEditor);
    }


    @Test
    public void testStart() {
        mEngine.start();

        // facility should be published
        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mUserAccount, notNullValue());
    }

    @Test
    public void testStop() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        mEngine.requestStop(null);
        mEngine.stop();

        // facility should be unpublished
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mUserAccount, nullValue());
    }

    @Test
    public void testSetUserAccount() {
        mEngine.start();

        UserAccountInfo.Monitor monitor = mock(UserAccountInfo.Monitor.class);
        mUserAccountInfo.monitorWith(monitor);

        assertThat(mFacilityChangeCnt, is(1));

        // test default values
        assertThat(mUserAccountInfo.getAccountIdentifier(), nullValue());
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(EPOCH));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(false));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(EPOCH));

        // set user account, allowing collected personal data upload
        mUserAccount.set("testProvider", "testAccount", UserAccount.AccountlessPersonalDataPolicy.ALLOW_UPLOAD);

        ArgumentCaptor<Long> dateCaptor = ArgumentCaptor.forClass(Long.class);

        // new account should be stored, anonymous data flag should be put to false,
        verify(mMockEditor).putString(UserAccountEngine.Persistence.PREF_KEY_ACCOUNT, "testProvider testAccount");
        verify(mMockEditor).putLong(eq(UserAccountEngine.Persistence.PREF_KEY_DATE), dateCaptor.capture());
        long accountChangeDate = dateCaptor.getValue();
        assertThat(accountChangeDate, not(0L));
        verify(mMockEditor).putBoolean(UserAccountEngine.Persistence.PREF_KEY_ANONYMOUS_DATA, false);
        verify(mMockEditor).apply();
        verifyNoMoreInteractions(mMockEditor);

        // account info should have changed
        verify(monitor).onChange(mUserAccountInfo);
        assertThat(mUserAccountInfo.getAccountIdentifier(), is("testProvider testAccount"));
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(new Date(accountChangeDate)));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(EPOCH));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(false));

        // set same account again, changing policy
        mUserAccount.set("testProvider", "testAccount", UserAccount.AccountlessPersonalDataPolicy.DENY_UPLOAD);

        // personal data allowance date should be stored
        verify(mMockEditor).putLong(eq(UserAccountEngine.Persistence.PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE),
                dateCaptor.capture());
        long personalDataAllowanceDate = dateCaptor.getValue();
        assertThat(personalDataAllowanceDate, not(0L));
        verify(mMockEditor, times(2)).apply();
        verifyNoMoreInteractions(mMockEditor);

        // account info should have changed
        verify(monitor, times(2)).onChange(mUserAccountInfo);
        assertThat(mUserAccountInfo.getAccountIdentifier(), is("testProvider testAccount"));
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(new Date(accountChangeDate)));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(new Date(personalDataAllowanceDate)));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(false));

        // clear user account, not allowing anonymous data
        mUserAccount.clear(UserAccount.AnonymousDataPolicy.DENY_UPLOAD);

        // cleared account should be stored, anonymous flag should not be stored since it did not change,
        // personal data allowance date should be reset to EPOCH (0)
        verify(mMockEditor).putString(UserAccountEngine.Persistence.PREF_KEY_ACCOUNT, null);
        verify(mMockEditor, times(2)).putLong(eq(UserAccountEngine.Persistence.PREF_KEY_DATE), dateCaptor.capture());
        accountChangeDate = dateCaptor.getValue();
        assertThat(accountChangeDate, not(0L));
        verify(mMockEditor).putLong(UserAccountEngine.Persistence.PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE, 0);
        verify(mMockEditor, times(3)).apply();
        verifyNoMoreInteractions(mMockEditor);

        // account info should have changed
        verify(monitor, times(3)).onChange(mUserAccountInfo);
        assertThat(mUserAccountInfo.getAccountIdentifier(), nullValue());
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(new Date(accountChangeDate)));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(EPOCH));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(false));

        // clear user account to allow anonymous data
        mUserAccount.clear(UserAccount.AnonymousDataPolicy.ALLOW_UPLOAD);

        // only anonymous data allowance should change
        verify(mMockEditor, times(1)).putBoolean(UserAccountEngine.Persistence.PREF_KEY_ANONYMOUS_DATA, true);
        verify(mMockEditor, times(4)).apply();
        verifyNoMoreInteractions(mMockEditor);

        // account info should have changed
        verify(monitor, times(4)).onChange(mUserAccountInfo);
        assertThat(mUserAccountInfo.getAccountIdentifier(), nullValue());
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(new Date(accountChangeDate)));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(EPOCH));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(true));

        // set user account, dropping collected personal data
        mUserAccount.set("testProvider", "testAccount", UserAccount.AccountlessPersonalDataPolicy.DENY_UPLOAD);

        // new account should be stored
        verify(mMockEditor, times(2)).putString(
                UserAccountEngine.Persistence.PREF_KEY_ACCOUNT, "testProvider testAccount");
        verify(mMockEditor, times(3)).putLong(eq(UserAccountEngine.Persistence.PREF_KEY_DATE), dateCaptor.capture());
        accountChangeDate = dateCaptor.getValue();
        assertThat(accountChangeDate, not(0L));
        verify(mMockEditor, times(3)).putLong(eq(
                UserAccountEngine.Persistence.PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE), dateCaptor.capture());
        personalDataAllowanceDate = dateCaptor.getValue();
        assertThat(accountChangeDate, not(0L));
        verify(mMockEditor, times(2)).putBoolean(UserAccountEngine.Persistence.PREF_KEY_ANONYMOUS_DATA, false);
        verify(mMockEditor, times(6)).apply(); // +1 for account, +1 for allowance date
        verifyNoMoreInteractions(mMockEditor);

        // account info should have changed, in particular, anonymous data allowance should now be false
        verify(monitor, times(5)).onChange(mUserAccountInfo);
        assertThat(mUserAccountInfo.getAccountIdentifier(), is("testProvider testAccount"));
        assertThat(mUserAccountInfo.getLatestAccountChangeDate(), is(new Date(accountChangeDate)));
        assertThat(mUserAccountInfo.getPersonalDataAllowanceDate(), is(new Date(personalDataAllowanceDate)));
        assertThat(mUserAccountInfo.isAnonymousDataUploadAllowed(), is(false));
    }
}
