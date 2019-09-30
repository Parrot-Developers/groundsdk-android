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

package com.parrot.drone.groundsdk.internal.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

import java.util.Date;

/**
 * Utility interface providing access to user account information.
 * <p>
 * This utility is always available and can be safely requested after engine startup using:
 * <pre>{@code UserAccountInfo accountInfo = getUtilityOrThrow(UserAccountInfo.class);}</pre>
 *
 * @see EngineBase#getUtilityOrThrow(Class)
 */
public interface UserAccountInfo extends Monitorable<UserAccountInfo.Monitor>, Utility {

    /**
     * Interface for listening to {@code UserAccountInfo} change notifications.
     */
    interface Monitor {

        /**
         * Called back when {@code UserAccountInfo} changes.
         *
         * @param userAccountInfo {@code UserAccountInfo} instance which changed
         */
        void onChange(@NonNull UserAccountInfo userAccountInfo);
    }

    /**
     * Retrieves currently registered user account identifier.
     *
     * @return user account identifier, or {@code null} if none is registered currently
     */
    @Nullable
    String getAccountIdentifier();

    /**
     * Tells when the user account identifier did change for the latest time.
     *
     * @return latest user account change date, or epoch if it did never change
     */
    @NonNull
    Date getLatestAccountChangeDate();

    /**
     * Tells the date starting from which collected personal data can be uploaded.
     * <p>
     * Personal data older than the returned date must not be uploaded and should be deleted instead.
     *
     * @return date starting from which collected personal data can be uploaded
     */
    @NonNull
    Date getPersonalDataAllowanceDate();

    /**
     * Tells whether anonymous data upload is allowed.
     * <p>
     * Returned value is only meaningful if {@link #getAccountIdentifier() user account} is {@code null}
     *
     * @return {@code true} if anonymous data upload is allowed, otherwise {@code false}
     */
    boolean isAnonymousDataUploadAllowed();
}
