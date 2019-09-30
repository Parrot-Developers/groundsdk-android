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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.facility.UserAccount;
import com.parrot.drone.groundsdk.internal.facility.UserAccountCore;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Engine that manages user account information.
 * <p>
 * Allows the application to set or reset user account information, manages persistence of such information and provides
 * access to such information, along with change notifications to other groundsdk internal component.
 */
public class UserAccountEngine extends EngineBase {

    /** Epoch date. */
    private static final Date EPOCH = new Date(0);

    /** Persistence layer. */
    @NonNull
    private final Persistence mPersistence;

    /** UserAccount facility for which this object is the backend. */
    @NonNull
    private final UserAccountCore mUserAccount;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    UserAccountEngine(@NonNull Controller controller) {
        super(controller);
        mPersistence = new Persistence(getContext());
        UserAccountInfoCore userAccountInfo = new UserAccountInfoCore();
        mUserAccount = new UserAccountCore(getFacilityPublisher(), userAccountInfo);
        publishUtility(UserAccountInfo.class, userAccountInfo);
    }

    @Override
    protected void onStart() {
        mUserAccount.publish();
    }

    @Override
    protected void onStop() {
        mUserAccount.unpublish();
    }

    /** {@link UserAccountInfo} utility implementation. */
    private final class UserAccountInfoCore implements UserAccountInfo, UserAccountCore.Backend {

        /** Registered monitors. */
        @NonNull
        private final Set<Monitor> mMonitors;

        /** Current user account identifier, {@code null} if none. */
        @Nullable
        private String mAccountId;

        /** Latest user account identifier change date, epoch if none. */
        @NonNull
        private Date mChangeDate;

        /** Personal data upload allowance date, epoch if none. */
        @NonNull
        private Date mPersonalDataAllowanceDate;

        /** {@code true} if anonymous data upload is currently allowed. */
        private boolean mAnonymousDataAllowed;

        /**
         * Constructor.
         */
        UserAccountInfoCore() {
            mMonitors = new CopyOnWriteArraySet<>();
            mAccountId = mPersistence.loadUserAccount();
            mChangeDate = mPersistence.loadLatestAccountChangeDate();
            mPersonalDataAllowanceDate = mPersistence.loadPersonalDataAllowanceDate();
            mAnonymousDataAllowed = mPersistence.loadAnonymousDataAllowance();
        }

        @Nullable
        @Override
        public String getAccountIdentifier() {
            return mAccountId;
        }

        @NonNull
        @Override
        public Date getLatestAccountChangeDate() {
            return mChangeDate;
        }

        @Override
        public boolean isAnonymousDataUploadAllowed() {
            return mAnonymousDataAllowed;
        }

        @NonNull
        @Override
        public Date getPersonalDataAllowanceDate() {
            return mPersonalDataAllowanceDate;
        }

        @Override
        public void monitorWith(@NonNull Monitor monitor) {
            mMonitors.add(monitor);
        }

        @Override
        public void disposeMonitor(@NonNull Monitor monitor) {
            mMonitors.remove(monitor);
        }

        @Override
        public void setUserAccount(@NonNull String accountId,
                                   @NonNull UserAccount.AccountlessPersonalDataPolicy policy) {
            boolean changed = false;
            if (!TextUtils.equals(mAccountId, accountId)) {
                mAnonymousDataAllowed = false;
                mAccountId = accountId;
                mChangeDate = mPersistence.saveUserAccount(mAccountId);
                changed = true;
            }
            Date personalDataAllowanceDate = policy == UserAccount.AccountlessPersonalDataPolicy.ALLOW_UPLOAD ?
                    EPOCH : new Date();
            if (!personalDataAllowanceDate.equals(mPersonalDataAllowanceDate)) {
                mPersonalDataAllowanceDate = personalDataAllowanceDate;
                mPersistence.savePersonalDataAllowanceDate(personalDataAllowanceDate);
                changed = true;
            }
            if (changed) {
                notifyChange();
            }
        }

        @Override
        public void clearUserAccount(@NonNull UserAccount.AnonymousDataPolicy policy) {
            boolean changed = false;
            if (mAccountId != null) {
                mAccountId = null;
                mChangeDate = mPersistence.saveUserAccount(null);
                mPersonalDataAllowanceDate = EPOCH;
                changed = true;
            }
            boolean allowAnonymousData = policy == UserAccount.AnonymousDataPolicy.ALLOW_UPLOAD;
            if (mAnonymousDataAllowed != allowAnonymousData) {
                mAnonymousDataAllowed = allowAnonymousData;
                mPersistence.saveAnonymousDataAllowance(mAnonymousDataAllowed);
                changed = true;
            }
            if (changed) {
                notifyChange();
            }
        }

        /**
         * Notifies all registered monitors that this {@code UserAccountInfo} did change.
         */
        private void notifyChange() {
            for (Monitor monitor : mMonitors) {
                monitor.onChange(this);
            }
        }
    }

    /**
     * Manages persistence of user account information.
     */
    @VisibleForTesting
    static final class Persistence {

        /** User account shared preferences file name. */
        @VisibleForTesting
        static final String PREF_FILE = "account";

        /** Key for accessing account shared preferences version. Value is int. */
        @VisibleForTesting
        static final String PREF_KEY_VERSION = "version";

        /** Key for accessing account identifier. Value is String. */
        @VisibleForTesting
        static final String PREF_KEY_ACCOUNT = "account";

        /** Key for accessing account change date. Value is long. */
        @VisibleForTesting
        static final String PREF_KEY_DATE = "date";

        /** Key for accessing personal data upload allowance date. Value is long. */
        @VisibleForTesting
        static final String PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE = "personal-data-allowance-date";

        /** Key for accessing anonymous data upload allowance setting. Value is boolean. */
        @VisibleForTesting
        static final String PREF_KEY_ANONYMOUS_DATA = "anonymous-data";

        /** Shared preference where user account data are stored. */
        @NonNull
        private final SharedPreferences mPrefs;

        /**
         * Constructor.
         *
         * @param context application context
         */
        Persistence(@NonNull Context context) {
            mPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            int version = mPrefs.getInt(PREF_KEY_VERSION, 0);
            if (version == 0) {
                version = 1;
                mPrefs.edit().putInt(PREF_KEY_VERSION, version).apply();
            }
        }

        /**
         * Loads stored user account identifier.
         *
         * @return stored user account identifier, or {@code null} if none
         */
        @Nullable
        String loadUserAccount() {
            return mPrefs.getString(PREF_KEY_ACCOUNT, null);
        }

        /**
         * Loads latest user account identifier change date.
         *
         * @return latest user account identifier change date, or epoch if none
         */
        @NonNull
        Date loadLatestAccountChangeDate() {
            return new Date(mPrefs.getLong(PREF_KEY_DATE, 0));
        }

        /**
         * Loads personal data upload allowance date.
         *
         * @return personal data allowance date, or epoch if none
         */
        @NonNull
        Date loadPersonalDataAllowanceDate() {
            return new Date(mPrefs.getLong(PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE, 0));
        }

        /**
         * Loads stored anonymous data upload allowance setting.
         *
         * @return stored anonymous data upload allowance setting, or {@code false} if none
         */
        boolean loadAnonymousDataAllowance() {
            return mPrefs.getBoolean(PREF_KEY_ANONYMOUS_DATA, false);
        }

        /**
         * Stores user account identifier.
         * <p>
         * Current date is stored as the latest user account identifier change date.
         * <p>
         * Anonymous data upload allowance is reset to false in case the provided user account is not {@code null}.
         * <p>
         * Personal data upload allowance date is reset in case the provided user account is {@code null}.
         *
         * @param accountId account identifier to store, may be {@code null}
         *
         * @return user account change date
         */
        @NonNull
        Date saveUserAccount(@Nullable String accountId) {
            long changeDate = System.currentTimeMillis();
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(PREF_KEY_ACCOUNT, accountId)
                  .putLong(PREF_KEY_DATE, changeDate);
            if (accountId != null) {
                editor.putBoolean(PREF_KEY_ANONYMOUS_DATA, false);
            } else {
                editor.putLong(PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE, 0);
            }
            editor.apply();
            return new Date(changeDate);
        }

        /**
         * Stores anonymous data upload allowance setting.
         *
         * @param allow anonymous data upload allowance setting
         */
        void saveAnonymousDataAllowance(boolean allow) {
            mPrefs.edit().putBoolean(PREF_KEY_ANONYMOUS_DATA, allow).apply();
        }

        /**
         * Stores collected personal data upload allowance date.
         *
         * @param date collected personal data upload allowance date
         */
        void savePersonalDataAllowanceDate(@NonNull Date date) {
            mPrefs.edit().putLong(PREF_KEY_PERSONAL_DATA_ALLOWANCE_DATE, date.getTime()).apply();
        }
    }
}
