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

package com.parrot.drone.groundsdk.internal.value;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.tasks.Executor;

import java.util.concurrent.TimeUnit;

/**
 * Delegate for setting implementation that takes charge of change notifications, updating state and timeout/rollback
 * management.
 */
public final class SettingController {

    /** Default setting update rollback timeout, in seconds. */
    private static final long TIMEOUT_DEFAULT = 5;

    /**
     * Callback interface notified when the controlled setting changes.
     */
    public interface ChangeListener {

        /**
         * Notifies that the controlled setting has changed.
         *
         * @param fromUser {@code true} if the change originates from the user, {@code false} otherwise (change
         *                 originates from the device)
         */
        void onChange(boolean fromUser);
    }

    /** Listener notified when the setting state changes. */
    @NonNull
    private final ChangeListener mListener;

    /** Default timeout for update rollback. */
    @IntRange(from = 0)
    private final long mDefaultTimeout;

    /** Current pending rollback. When non-{@code null}, the setting is considered 'updating'. */
    @Nullable
    private Runnable mUpdateRollback;

    /**
     * Constructor.
     * <p>
     * This constructor sets the default timeout to {@value #TIMEOUT_DEFAULT} seconds.
     *
     * @param listener listener notified when the setting state changes
     */
    public SettingController(@NonNull ChangeListener listener) {
        this(listener, TimeUnit.SECONDS.toMillis(TIMEOUT_DEFAULT));
    }

    /**
     * Constructor.
     *
     * @param listener       listener notified when the setting state changes
     * @param defaultTimeout default timeout for update rollback, {@code 0} for infinite timeout
     */
    public SettingController(@NonNull ChangeListener listener, @IntRange(from = 0) long defaultTimeout) {
        mListener = listener;
        mDefaultTimeout = defaultTimeout;
    }

    /**
     * Tells whether a rollback is pending on this setting.
     * <p>
     * When {@code true}, then the setting can be considered to be updating, otherwise it is up-to-date.
     *
     * @return {@code true} if some rollback is pending, otherwise {@code false}
     */
    public boolean hasPendingRollback() {
        return mUpdateRollback != null;
    }

    /**
     * Posts a rollback action.
     * <p>
     * This rollback will be executed after the configured default timeout elapses, in which case a user change
     * notification will be forwarded to the setting listener right after rollback execution.
     * <p>
     * Calling this method cancels any previously posted rollback beforehand, and forwards a user change notification
     * to the setting listener.
     *
     * @param rollback rollback action to execute upon timeout
     */
    public void postRollback(@NonNull Runnable rollback) {
        if (mUpdateRollback != null) {
            Executor.unschedule(mUpdateRollback);
        }
        mUpdateRollback = () -> {
            rollback.run();
            mUpdateRollback = null;
            mListener.onChange(true);
        };
        mListener.onChange(true);

        if (mDefaultTimeout > 0) {
            Executor.schedule(mUpdateRollback, mDefaultTimeout);
        }
    }

    /**
     * Cancels any pending rollback action.
     *
     * @return {@code true} if a rollback was pending and has been canceled, otherwise {@code false}
     */
    public boolean cancelRollback() {
        if (mUpdateRollback != null) {
            Executor.unschedule(mUpdateRollback);
            mUpdateRollback = null;
            return true;
        }
        return false;
    }

    /**
     * Forwards a change notification to the setting change listener.
     *
     * @param fromUser {@code true} if the change results from some user action, otherwise {@code false}
     */
    public void notifyChange(boolean fromUser) {
        mListener.onChange(fromUser);
    }
}
