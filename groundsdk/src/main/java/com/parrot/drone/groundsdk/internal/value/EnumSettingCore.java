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

import com.parrot.drone.groundsdk.value.EnumSetting;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Implementation class for {@code EnumSetting}.
 */
public final class EnumSettingCore<E extends Enum<E>> extends EnumSetting<E> {

    /**
     * Setting backend interface, used to delegate value change processing.
     *
     * @param <E> type of managed enum
     */
    public interface Backend<E> {

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
        boolean setValue(E value);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend<E> mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Default available values. */
    @NonNull
    private final EnumSet<E> mDefaultAvailableValues;

    /** Default value. */
    @NonNull
    private final E mDefaultValue;

    /** Current available values. */
    @NonNull
    private final EnumSet<E> mAvailableValues;

    /** Current setting value. */
    @NonNull
    private E mValue;

    /**
     * Constructor.
     *
     * @param defaultValue                default setting value
     * @param defaultOtherAvailableValues default available values (other than {@code defaultValue}
     * @param controller                  setting controller, managing update, rollback and change notifications
     * @param backend                     backend that will process value changes
     */
    public EnumSettingCore(@NonNull E defaultValue, @NonNull EnumSet<E> defaultOtherAvailableValues,
                           @NonNull SettingController controller, @NonNull Backend<E> backend) {
        mDefaultAvailableValues = EnumSet.copyOf(defaultOtherAvailableValues);
        mDefaultAvailableValues.add(defaultValue);
        mDefaultValue = defaultValue;
        mController = controller;
        mBackend = backend;
        mAvailableValues = EnumSet.copyOf(mDefaultAvailableValues);
        mValue = mDefaultValue;
    }

    /**
     * Constructor.
     * <p>
     * This constructor sets the initial available values to the whole set of values defined in the corresponding enum.
     *
     * @param defaultValue initial setting value
     * @param controller   setting controller, managing update, rollback and change notifications
     * @param backend      backend that will process value changes
     */
    public EnumSettingCore(@NonNull E defaultValue, @NonNull SettingController controller,
                           @NonNull Backend<E> backend) {
        mDefaultAvailableValues = EnumSet.allOf(defaultValue.getDeclaringClass());
        mDefaultValue = defaultValue;
        mController = controller;
        mBackend = backend;
        mAvailableValues = EnumSet.copyOf(mDefaultAvailableValues);
        mValue = mDefaultValue;
    }

    /**
     * Constructor.
     * <p>
     * This constructor sets the initial available values to none of the values defined in the corresponding enum. <br>
     * Default value, although meaningless as long as the available values set is empty, is initialized to the first
     * value in the corresponding enum, ordinal-wise.
     *
     * @param type       enum type
     * @param controller setting controller, managing update, rollback and change notifications
     * @param backend    backend that will process value changes
     */
    public EnumSettingCore(@NonNull Class<E> type, @NonNull SettingController controller, @NonNull Backend<E> backend) {
        mDefaultAvailableValues = EnumSet.noneOf(type);
        mDefaultValue = type.getEnumConstants()[0];
        mController = controller;
        mBackend = backend;
        mAvailableValues = EnumSet.copyOf(mDefaultAvailableValues);
        mValue = mDefaultValue;
    }

    @NonNull
    @Override
    public E getValue() {
        return mValue;
    }

    @NonNull
    @Override
    public EnumSet<E> getAvailableValues() {
        return EnumSet.copyOf(mAvailableValues);
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public void setValue(@NonNull E value) {
        if (mValue != value && mAvailableValues.contains(value) && mBackend.setValue(value)) {
            E rollbackValue = mValue;
            mValue = value;
            mController.postRollback(() -> mValue = rollbackValue);
        }
    }

    /**
     * Updates the current setting value.
     * <p>
     * Resets the updating flag in case it was set. <br>
     * Called from lower layer when the backend sends a new value for the setting. <br>
     * Provided value is always added to the set of currently available values, if not present already.
     *
     * @param value new setting value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public EnumSettingCore<E> updateValue(@NonNull E value) {
        if (mController.cancelRollback() || mValue != value) {
            mValue = value;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates currently available setting values.
     * <p>
     * Current setting value is also added in case it is not present in the provided values.
     *
     * @param values new available values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public EnumSettingCore<E> updateAvailableValues(@NonNull Collection<E> values) {
        if (mAvailableValues.retainAll(values) | mAvailableValues.addAll(values)) {
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