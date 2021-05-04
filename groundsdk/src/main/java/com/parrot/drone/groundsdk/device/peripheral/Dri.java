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

package com.parrot.drone.groundsdk.device.peripheral;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.internal.device.peripheral.DriCore;
import com.parrot.drone.groundsdk.value.BooleanSetting;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * DRI peripheral interface.
 *
 * The DRI or Drone Remote ID is a protocol that sends periodic broadcasts of some identification data
 * during the flight for safety, security, and compliance purposes.
 * <p>
 * This peripheral allows to enable or disable the DRI.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(Dri.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface Dri extends Peripheral {

    /**
     * Type of ID that can be sent in the DRI.
     */
    enum IdType {
        /** French 30-byte format. */
        FR_30_OCTETS,

        /** ANSI CTA 2063 format on 40 bytes. */
        ANSI_CTA_2063
    }

    /**
     * DRI ID.
     */
    interface DroneId {

        /**
         * Gets the type of the ID.
         *
         * @return ID type
         */
        @NonNull
        IdType getType();

        /**
         * Gets the ID.
         *
         * @return ID
         */
        @NonNull
        String getId();
    }

    /**
     * DRI type configuration.
     */
    interface TypeConfig {

        /**
         * DRI type.
         */
        enum Type {
            /** DRI wifi beacon respects the EN4709-002 european regulation. */
            EN4709_002,

            /** DRI wifi beacon respects the french regulation. */
            FRENCH
        }

        /**
         * Creates a DRI type configuration for EN4709-002 european regulation.
         *
         * @param operatorId operator identifier as defined by EN4709-002 european regulation
         *
         * @return a new {@code TypeConfig}
         */
        @NonNull
        static TypeConfig ofEn4709002(@NonNull String operatorId) {
            return new DriCore.TypeConfigCore(Type.EN4709_002, operatorId);
        }

        /**
         * Creates a DRI type configuration for french regulation.
         *
         * @return a new {@code TypeConfig}
         */
        @NonNull
        static TypeConfig ofFrench() {
            return new DriCore.TypeConfigCore(Type.FRENCH, "");
        }

        /**
         * Gets DRI type.
         *
         * @return DRI type
         */
        @NonNull
        Type getType();

        /**
         * Gets operator identifier, only relevant with {@link Type#EN4709_002} type.
         *
         * @return operator identifier.
         */
        @NonNull
        String getOperatorId();

        /**
         * Tells whether the configuration is valid.
         * <p>
         * For {@link Type#EN4709_002}, the configuration is considered invalid if the operator identifier does not
         * conform to EN4709-002 standard.
         *
         * @return {@code true} if the configuration is valid, {@code false} otherwise
         */
        boolean isValid();
    }

    /**
     * DRI type configuration state.
     */
    interface TypeConfigState {

        /**
         * Configuration state.
         */
        enum State {
            /** DRI type has been sent to the drone and change confirmation is awaited. */
            UPDATING,

            /** DRI type is configured on the drone. */
            CONFIGURED,

            /** DRI type configuration failed for an unknown reason. */
            FAILURE,

            /**
             * DRI type configuration failed due to an invalid operator identifier.
             *
             * It may occur with {@link TypeConfig.Type#EN4709_002} type when the operator identifier is not conform
             * to the EN4709-002 european regulation.
             */
            INVALID_OPERATOR_ID
        }

        /**
         * Gets configuration state.
         *
         * @return configuration state
         */
        @NonNull
        State getState();

        /**
         * Gets configuration.
         * <p>
         * This configuration may differ from the one returned by {@link #getTypeConfig()}. Especially with
         * {@link TypeConfig.Type#EN4709_002} type, as the operator identifier returned by the drone is a substring of
         * the one defined with {@link #setTypeConfig(TypeConfig)}, for security purpose.
         *
         * @return configuration if available, {@code null} otherwise
         */
        @Nullable
        TypeConfig getConfig();
    }

    /**
     * Gets information about the ID.
     *
     * @return information about the ID if available, {@code null} otherwise
     */
    @Nullable
    DroneId getDroneId();

    /**
     * Gives access to the DRI state setting.
     * <p>
     * This setting allows to enable or disable the drone DRI.
     *
     * @return the DRI state setting
     */
    @NonNull
    BooleanSetting state();

    /**
     * Gets DRI types supported by the drone.
     *
     * @return supported DRI types
     */
    @NonNull
    Set<TypeConfig.Type> supportedTypes();

    /**
     * Gets current DRI type configuration state.
     *
     * @return DRI configuration state if available, {@code null} otherwise
     */
    @Nullable
    TypeConfigState getTypeConfigState();

    /**
     * Gets DRI type configuration as defined by the user.
     *
     * @return DRI configuration as defined by the user if available, {@code null} otherwise
     */
    @Nullable
    TypeConfig getTypeConfig();

    /**
     * Sets DRI type configuration.
     * <p>
     * If {@link TypeConfig#isValid() valid}, this configuration is sent to the drone when it's changed by the user and
     * at every connection to the drone.
     * When {@code null} is passed, nothing is sent to the drone.
     *
     * @param config DRI type configuration or {@code null} to disable DRI type configuration
     *
     * @throws IllegalArgumentException if {@code config} is not a {@link TypeConfig#isValid() valid configuration}
     */
    void setTypeConfig(@Nullable TypeConfig config) throws IllegalArgumentException;
}
