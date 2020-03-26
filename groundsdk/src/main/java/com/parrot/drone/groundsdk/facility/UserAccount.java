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

package com.parrot.drone.groundsdk.facility;

import androidx.annotation.NonNull;

/**
 * Facility that allows the application to register some user account identifier.
 * <p>
 * The application may register a user account in order to allow GroundSdk to upload data that may disclose
 * user-personal information to the configured remote server. This includes: <ul>
 * <li>flight blackboxes,</li>
 * <li>flight logs,</li>
 * <li>full crash reports.</li>
 * </ul>
 * All HTTP requests that upload such data will include the registered user account identifier.
 * <p>
 * In the absence of such a user account, then <strong>by default</strong> GroundSdk is not allowed to upload any data
 * to the configured remote server.
 * <p>
 * However, the application may authorize GroundSdk to upload anonymous data (which do not disclose any user-personal
 * information) to the configured remote server. This includes: <ul>
 * <li>anonymous crash reports</li>
 * </ul>
 * The application can opt-in anonymous data upload upon {@link #clear clearing} any registered user account, by
 * specifying the desired {@link AnonymousDataPolicy policy} to observe.
 * <p>
 * Note that GroundSdk may <strong>always</strong> collect both anonymous and personal data from connected devices
 * and will store them on the user's device, regardless of the presence of any user account. <br>
 * When the application eventually {@link #set(String, String, AccountlessPersonalDataPolicy) registers} a user
 * account, it may at that point indicate what to do with personal data that were collected beforehand, by
 * specifying the desired {@link AccountlessPersonalDataPolicy policy} to observe.
 * </p>
 * User account identifiers, or absence thereof, as well as whether anonymous data upload is allowed (when no user
 * account is registered), are persisted by GroundSdk across application restarts.
 * <p>
 * By default, there is no registered user account (so personal data upload is denied) and anonymous data upload is
 * also denied.
 */
public interface UserAccount extends Facility {

    /**
     * Policy to observe with regard to personal user data that were collected in the absence of a registered user
     * account, upon registration of such an account.
     */
    enum AccountlessPersonalDataPolicy {

        /** Already collected data must not be uploaded and should be deleted. */
        DENY_UPLOAD,

        /** Already collected data may be uploaded. */
        ALLOW_UPLOAD,
    }

    /**
     * Registers a user account.
     * <p>
     * Only one user account may be registered, calling this method erase any previously set user account. <br>
     * <p>
     * In case no user account was set beforehand, the specified policy informs GroundSdk about what to do with
     * personal user data that have already been collected on the user's device.
     * <p>
     * By default, there is no user account registered.
     *
     * @param accountProvider identifies the account provider
     * @param accountId       identifies the account
     * @param policy          policy to observe with regard to personal data that were collected in the absence of
     *                        any registered user account
     */
    void set(@NonNull String accountProvider, @NonNull String accountId, @NonNull AccountlessPersonalDataPolicy policy);

    /** Anonymous data upload policy. */
    enum AnonymousDataPolicy {

        /** Uploading anonymous data is authorized. */
        ALLOW_UPLOAD,

        /** Uploading anonymous is forbidden. */
        DENY_UPLOAD
    }

    /**
     * Clears any registered user account.
     * <p>
     * In the absence of any registered user account, the application may furthermore specify a policy to observe
     * with regard to anonymous data upload.
     * <p>
     * By default, anonymous data collection is denied.
     *
     * @param policy policy to observe with regard to anonymous data upload
     */
    void clear(@NonNull AnonymousDataPolicy policy);
}
