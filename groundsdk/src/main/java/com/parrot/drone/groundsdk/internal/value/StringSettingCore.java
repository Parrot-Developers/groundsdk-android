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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.value.StringSetting;

/**
 * Implementation class for {@code StringSetting}.
 */
public final class StringSettingCore extends StringSetting {

    /**
     * Setting backend interface, used to delegate value change processing.
     */
    public interface Backend {

        /**
         * Processes a value change.
         * <p>
         * The return value of this method determines whether the setting should adopt the application requested value
         * and switch to the updating state. <br>
         * If the implementation returns {@code true}, care must be taken <strong>NOT</strong> to call back into
         * {@link #updateValue} from within this method, because the update will not be taken into account. <br>
         * If the implementation needs to update the value from this method, then it should do so using
         * {@code updateValue}, and return <strong>{@code false}</strong> from {@code sendValue}
         *
         * @param value new setting value
         *
         * @return {@code true} to make the setting update to the requested value and switch to the updating state now,
         *         otherwise {@code false}
         */
        boolean setValue(@NonNull String value);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Current setting value. */
    @NonNull
    private String mValue;

    /**
     * Constructor.
     * <p>
     * Default value is set to the empty string.
     *
     * @param controller setting controller, managing update, rollback and change notifications
     * @param backend    backend that will process value changes
     */
    public StringSettingCore(@NonNull SettingController controller, @NonNull Backend backend) {
        this("", controller, backend);
    }

    /**
     * Constructor.
     *
     * @param defaultValue setting default value
     * @param controller   setting controller, managing update, rollback and change notifications
     * @param backend      backend that will process value changes
     */
    public StringSettingCore(@NonNull String defaultValue, @NonNull SettingController controller,
                             @NonNull Backend backend) {
        mValue = defaultValue;
        mController = controller;
        mBackend = backend;
    }

    @NonNull
    @Override
    public String getValue() {
        return mValue;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public void setValue(@NonNull String value) {
        if (!mValue.equals(value) && mBackend.setValue(value)) {
            String rollbackValue = mValue;
            mValue = value;
            mController.postRollback(() -> mValue = rollbackValue);
        }
    }

    /**
     * Updates the current setting value.
     * <p>
     * Resets the updating flag in case it was set. <br>
     * Called from lower layer when the backend sends a new value for the setting.
     *
     * @param value new setting value
     *
     * @return {@code this}, to allow chained calls
     */
    public StringSettingCore updateValue(@NonNull String value) {
        if (mController.cancelRollback() || !mValue.equals(value)) {
            mValue = value;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Cancels any pending rollback.
     */
    public void cancelRollback() {
        if (mController.cancelRollback()) {
            mController.notifyChange(false);
        }
    }

}
