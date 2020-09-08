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

import java.util.Objects;

/** Core class for DRI. */
public class DriCore extends SingletonComponentCore implements Dri {

    /** Description of DRI. */
    private static final ComponentDescriptor<Peripheral, Dri> DESC = ComponentDescriptor.of(Dri.class);

    /** Engine-specific backend for DRI. */
    public interface Backend {

        /**
         * Sets the DRI state.
         *
         * @param enabled {@code true} to enable DRI, otherwise {@code false}
         *
         * @return {@code true} if the state could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setState(boolean enabled);
    }

    /** DRI state setting. */
    @NonNull
    private final BooleanSettingCore mState;

    /** DRI DroneId. */
    @Nullable
    private Id mDroneId;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public DriCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mState = new BooleanSettingCore(new SettingController(this::onSettingChange), backend::setState);
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
}