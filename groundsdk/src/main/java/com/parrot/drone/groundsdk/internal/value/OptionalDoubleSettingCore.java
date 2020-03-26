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

import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;

/**
 * Implementation class for {@code OptionalDoubleSetting}.
 */
public final class OptionalDoubleSettingCore extends OptionalDoubleSetting {

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
        boolean setValue(double value);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Setting value bounds. */
    @NonNull
    private final DoubleRangeCore mBounds;

    /** Current setting value. */
    private double mValue;

    /** {@code true} if the setting is supported. */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param controller setting controller, managing update, rollback and change notifications
     * @param backend    backend that will process value changes
     */
    public OptionalDoubleSettingCore(@NonNull SettingController controller, @NonNull Backend backend) {
        mController = controller;
        mBackend = backend;
        mBounds = new DoubleRangeCore(0, 0);
    }

    @Override
    public double getMin() {
        return mBounds.getLower();
    }

    @Override
    public double getMax() {
        return mBounds.getUpper();
    }

    @Override
    public double getValue() {
        return mValue;
    }

    @Override
    public boolean isAvailable() {
        return mSupported;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public void setValue(double value) {
        value = mBounds.clamp(value);
        if (mSupported && Double.compare(mValue, value) != 0 && mBackend.setValue(value)) {
            double rollbackValue = mValue;
            mValue = value;
            mController.postRollback(() -> mValue = rollbackValue);
        }
    }

    /**
     * Update setting bounds.
     *
     * @param range range defining new setting bounds
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public OptionalDoubleSettingCore updateBounds(@NonNull DoubleRange range) {
        if (mBounds.updateBounds(range) || !mSupported) {
            mSupported = true;
            mValue = mBounds.clamp(mValue);
            mController.notifyChange(false);
        }
        return this;
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
    @NonNull
    public OptionalDoubleSettingCore updateValue(double value) {
        value = mBounds.clamp(value);
        if (mController.cancelRollback() || Double.compare(mValue, value) != 0) {
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
