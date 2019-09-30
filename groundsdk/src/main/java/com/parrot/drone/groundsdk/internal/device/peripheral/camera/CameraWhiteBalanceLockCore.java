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

package com.parrot.drone.groundsdk.internal.device.peripheral.camera;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalanceLock;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for CameraWhiteBalanceLock. */
public class CameraWhiteBalanceLockCore implements CameraWhiteBalanceLock {

    /** White balance lock backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets white balance lock.
         *
         * @param locked lock value to set
         *
         * @return {@code true} to make the setting update to the requested value and switch to the updating state now,
         *         otherwise {@code false}
         */
        boolean setWhiteBalanceLock(boolean locked);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Whether white balance is lockable. */
    private boolean mLockable;

    /** Whether white balance is currently locked. */
    private boolean mLocked;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraWhiteBalanceLockCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public boolean isLockable() {
        return mLockable;
    }

    @Override
    public boolean isLocked() {
        return mLocked;
    }

    @Override
    public void lockCurrentValue(boolean lock) {
        if (mLockable && mLocked != lock && mBackend.setWhiteBalanceLock(lock)) {
            boolean rollbackLocked = mLocked;
            mLocked = lock;
            mController.postRollback(() -> mLocked = rollbackLocked);
        }
    }

    /**
     * Updates current white balance lockable state.
     *
     * @param lockable white balance lockable value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceLockCore updateLockable(boolean lockable) {
        if (mLockable != lockable) {
            mLockable = lockable;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current white balance lock state.
     *
     * @param locked white balance lock value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceLockCore updateLocked(boolean locked) {
        if (mController.cancelRollback() || mLocked != locked) {
            mLocked = locked;
            mController.notifyChange(false);
        }
        return this;
    }
}
