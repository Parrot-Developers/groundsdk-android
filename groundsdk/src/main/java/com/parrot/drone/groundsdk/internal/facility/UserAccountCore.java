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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.UserAccount;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/**
 * Core class for the {@code UserAccount} facility.
 */
public class UserAccountCore extends SingletonComponentCore implements UserAccount {

    /** Description of UserAccount. */
    private static final ComponentDescriptor<Facility, UserAccount> DESC = ComponentDescriptor.of(UserAccount.class);

    /** Engine-specific backend for the UserAccount. */
    public interface Backend {

        /**
         * Sets the user account.
         *
         * @param accountId account identifier
         * @param policy    already collected personal data policy to observe
         */
        void setUserAccount(@NonNull String accountId, @NonNull AccountlessPersonalDataPolicy policy);

        /**
         * Clears the user account.
         *
         * @param policy anonymous data upload policy to observe
         */
        void clearUserAccount(@NonNull AnonymousDataPolicy policy);
    }

    /** Engine facility backend. */
    @NonNull
    private final Backend mBackend;

    /**
     * Constructor.
     *
     * @param facilityStore store where this facility belongs
     * @param backend       backend used to forward actions to the engine
     */
    public UserAccountCore(@NonNull ComponentStore<Facility> facilityStore, @NonNull Backend backend) {
        super(DESC, facilityStore);
        mBackend = backend;
    }

    @Override
    public void set(@NonNull String accountProvider, @NonNull String accountId,
                    @NonNull AccountlessPersonalDataPolicy policy) {
        mBackend.setUserAccount(accountProvider + " " + accountId, policy);
    }

    @Override
    public void clear(@NonNull AnonymousDataPolicy policy) {
        mBackend.clearUserAccount(policy);
    }
}
