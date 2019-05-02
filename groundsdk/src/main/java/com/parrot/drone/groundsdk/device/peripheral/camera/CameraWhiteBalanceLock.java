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

package com.parrot.drone.groundsdk.device.peripheral.camera;

/**
 * Camera white balance lock sub-peripheral.
 * <p>
 * Allows to lock/unlock white balance value.
 */
public interface CameraWhiteBalanceLock {

    /**
     * Tells whether lock state is updating.
     *
     * @return {@code true} when current lock state has been changed locally and waiting for drone answer
     */
    boolean isUpdating();

    /**
     * Tells whether white balance is currently lockable.
     * <p>
     * White balance is lockable if current {@link CameraWhiteBalance.Setting#mode() mode} is
     * {@link CameraWhiteBalance.Mode#AUTOMATIC AUTOMATIC} and this feature is supported by the drone.
     *
     * @return {@code true} if white balance is currently lockable, otherwise {@code false}
     */
    boolean isLockable();

    /**
     * Tells whether white balance is currently locked.
     *
     * @return {@code true} if white balance is locked, otherwise {@code false}
     */
    boolean isLocked();

    /**
     * Locks (or unlocks) white balance to current value.
     * <p>
     * This method has no effect if white balance is not currently {@link #isLockable() lockable}.
     * White balance lock is automatically removed when the mode is changed to a non-automatic value.
     *
     * @param lock {@code true} to lock white balance to current value, {@code false} to unlock white balance
     */
    void lockCurrentValue(boolean lock);
}
