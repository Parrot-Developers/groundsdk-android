/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.BooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Core class for DRI. */
public class DriCore extends SingletonComponentCore implements Dri {

    /** Description of DRI. */
    private static final ComponentDescriptor<Peripheral, Dri> DESC = ComponentDescriptor.of(Dri.class);

    /** Engine-specific backend for DRI. */
    public interface Backend {

        /**
         * Sets DRI state.
         *
         * @param enabled {@code true} to enable DRI, otherwise {@code false}
         *
         * @return {@code true} if the state could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setState(boolean enabled);

        /**
         * Sets DRI type configuration.
         *
         * @param config DRI type configuration or {@code null} to disable DRI type configuration
         */
        void setTypeConfig(@Nullable TypeConfigCore config);
    }

    /** Core class for DroneId. */
    public static final class Id implements DroneId {

        /** DRI ID. */
        @NonNull
        private final String mId;

        /** ID Type. */
        @NonNull
        private final IdType mType;

        /**
         * Constructor.
         *
         * @param type ID type
         * @param id   ID
         */
        public Id(@NonNull IdType type, @NonNull String id) {
            mType = type;
            mId = id;
        }

        @NonNull
        @Override
        public String getId() {
            return mId;
        }

        @NonNull
        @Override
        public IdType getType() {
            return mType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return mId.equals(that.mId) && mType == that.mType;
        }

        @Override
        public int hashCode() {
            int result = mId.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }
    }

    /** Core class for DRI type configuration. */
    public static class TypeConfigCore implements TypeConfig {

        /** DRI type. */
        @NonNull
        protected final Type mType;

        /** Operator identifier, only relevant with {@link Type#EN4709_002} type. */
        @NonNull
        private final String mOperatorId;

        /**
         * Constructor.
         *
         * @param type       DRI type
         * @param operatorId Operator identifier, only relevant with {@link Type#EN4709_002} type
         */
        public TypeConfigCore(@NonNull Type type, @NonNull String operatorId) {
            mType = type;
            mOperatorId = operatorId;
        }

        @NonNull
        @Override
        public Type getType() {
            return mType;
        }

        @NonNull
        @Override
        public String getOperatorId() {
            return mOperatorId;
        }

        @Override
        public boolean isValid() {
            if (mType == Type.EN4709_002) {
                return validateEn4709UasOperator(mOperatorId);
            } else {
                return true;
            }
        }

        /**
         * Validates a string with Luhn mod 36 algorithm.
         *
         * @param input string to validate
         *
         * @return {@code true} is the string is valid, {@code false} otherwise
         */
        private static boolean validateLuhn(@NonNull String input) {
            int base = 36;
            int factor = 1;
            int sum = 0;
            for (int i = input.length() - 1; i >= 0; i--) {
                int codePoint = Integer.parseInt(input.substring(i, i+1), base);
                int addend = factor * codePoint;
                factor = (factor == 2) ? 1 : 2;
                addend = (addend / base) + (addend % base);
                sum += addend;
            }
            return sum % base == 0;
        }

        /**
         * Tells if an operator identifier conforms to EN4709 standard.
         * <p>
         * Per EN4709, operator string contains 19 characters composed of:
         * <ul>
         * <li> 3 characters for ISO 3166 Alpha-3 code of country
         * <li> 12 characters for operator identifier
         * <li> 1 character for checksum
         * <li> 1 hyphen
         * <li> 3 secret characters used to check checksum
         * </ul>
         *
         * @param operator operator identifier to verify
         *
         * @return {@code true} is the operator identifier is valid, {@code false} otherwise
         */
        private static boolean validateEn4709UasOperator(@NonNull String operator) {
            if (operator.length() != 20) {
                // invalid operator length
                return false;
            }
            String country = operator.substring(0, 3);
            String operatorId = operator.substring(3, 15);
            String checksum = operator.substring(15, 16);
            String hyphen = operator.substring(16, 17);
            String secret = operator.substring(17, 20);
            // verify country code is in upper case
            if (!country.toUpperCase(Locale.ENGLISH).equals(country)) {
                return false;
            }
            // verify hyphen
            if (!hyphen.equals("-")) {
                return false;
            }
            String secretOperatorId = operatorId + secret + checksum;
            // verify operator identifier is not in lower case
            if (!secretOperatorId.toLowerCase(Locale.ENGLISH).equals(secretOperatorId)) {
                return false;
            }
            // verify operator identifier with Luhn mod 36 algorithm
            return validateLuhn(secretOperatorId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeConfigCore that = (TypeConfigCore) o;
            return mType == that.mType
                    && mOperatorId.equals(that.mOperatorId);
        }

        @Override
        public int hashCode() {
            int result = mType.hashCode();
            result = 31 * result + mOperatorId.hashCode();
            return result;
        }
    }

    /** Core class for DRI type configuration state. */
    public static class TypeConfigStateCore implements TypeConfigState {

        /** DRI type configuration state. */
        @NonNull
        private final State mState;

        /** DRI type configuration if available. */
        @Nullable
        private final TypeConfig mTypeConfig;

        /**
         * Constructor.
         *
         * @param state      DRI type configuration state
         * @param typeConfig DRI type configuration
         */
        public TypeConfigStateCore(@NonNull State state, @Nullable TypeConfig typeConfig) {
            mState = state;
            mTypeConfig = typeConfig;
        }

        @NonNull
        @Override
        public State getState() {
            return mState;
        }

        @Nullable
        @Override
        public TypeConfig getConfig() {
            return mTypeConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeConfigStateCore that = (TypeConfigStateCore) o;
            return mState == that.mState
                   && Objects.equals(mTypeConfig, that.mTypeConfig);
        }

        @Override
        public int hashCode() {
            int result = mState.hashCode();
            if (mTypeConfig != null) {
                result = 31 * result + mTypeConfig.hashCode();
            }
            return result;
        }
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** DRI state setting. */
    @NonNull
    private final BooleanSettingCore mState;

    /** DRI DroneId. */
    @Nullable
    private Id mDroneId;

    /** Supported DRI types. */
    @NonNull
    private final EnumSet<TypeConfig.Type> mSupportedTypes;

    /** DRI type configuration state. */
    @Nullable
    private TypeConfigState mTypeConfigState;

    /** DRI type configuration. */
    @Nullable
    private TypeConfigCore mTypeConfig;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public DriCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mState = new BooleanSettingCore(new SettingController(this::onSettingChange), backend::setState);
        mSupportedTypes = EnumSet.noneOf(TypeConfig.Type.class);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @Nullable
    @Override
    public DroneId getDroneId() {
        return mDroneId;
    }

    /** DRI state setting. */
    @NonNull
    @Override
    public BooleanSettingCore state() {
        return mState;
    }

    @NonNull
    @Override
    public Set<TypeConfig.Type> supportedTypes() {
        return mSupportedTypes;
    }

    @Nullable
    @Override
    public TypeConfigState getTypeConfigState() {
        return mTypeConfigState;
    }

    @Nullable
    @Override
    public TypeConfig getTypeConfig() {
        return mTypeConfig;
    }

    @Override
    public void setTypeConfig(@Nullable TypeConfig typeConfig) throws IllegalArgumentException {
        TypeConfigCore configCore = typeConfig instanceof TypeConfigCore ? (TypeConfigCore) typeConfig : null;
        if (configCore != null && configCore.mType == TypeConfig.Type.EN4709_002
            && !TypeConfigCore.validateEn4709UasOperator(configCore.mOperatorId)) {
            throw new IllegalArgumentException("Invalid operator identifier");
        }
        if (!Objects.equals(mTypeConfig, typeConfig)) {
            mBackend.setTypeConfig(configCore);
        }
    }

    /**
     * Updates the remote identifier.
     *
     * @param id new identifier
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DriCore updateDroneId(@NonNull Id id) {
        if (!Objects.equals(mDroneId, id)) {
            mDroneId = id;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates supported DRI types.
     *
     * @param types supported DRI types
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public DriCore updateSupportedTypes(@NonNull Collection<TypeConfig.Type> types) {
        if (mSupportedTypes.retainAll(types) | mSupportedTypes.addAll(types)) {
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates DRI type configuration state.
     *
     * @param typeConfigState DRI type configuration state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public DriCore updateTypeConfigState(@Nullable TypeConfigStateCore typeConfigState) {
        if (!Objects.equals(mTypeConfigState, typeConfigState)) {
            mTypeConfigState = typeConfigState;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates DRI type configuration .
     *
     * @param typeConfig DRI type configuration
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public DriCore updateTypeConfig(@Nullable TypeConfigCore typeConfig) {
        if (!Objects.equals(mTypeConfig, typeConfig)) {
            mTypeConfig = typeConfig;
            mChanged = true;
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public DriCore cancelSettingsRollbacks() {
        mState.cancelRollback();
        return this;
    }

    /**
     * Notified when a user setting changes.
     * <p>
     * In case the change originates from the user modifying the setting value, updates the store to show the setting
     * is updating.
     *
     * @param fromUser {@code true} if the change originates from the user, otherwise {@code false}
     */
    private void onSettingChange(boolean fromUser) {
        mChanged = true;
        if (fromUser) {
            notifyUpdated();
        }
    }
}