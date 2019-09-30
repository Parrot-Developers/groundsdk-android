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

package com.parrot.drone.groundsdk.internal.device.peripheral.tracking;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.internal.value.DoubleRangeCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for FramingSetting. */
final class FramingSettingCore extends TargetTracker.FramingSetting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets target framing position setting.
         *
         * @param horizontalPosition target horizontal position in frame
         * @param verticalPosition   target vertical position in frame
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setTargetPosition(double horizontalPosition, double verticalPosition);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Target horizontal position in frame (0 is left-most, 1 is right-most). */
    @FloatRange(from = 0, to = 1)
    private double mHorizontalPosition;

    /** Target vertical position in frame (0 is top-most, 1 is bottom-most). */
    @FloatRange(from = 0, to = 1)
    private double mVerticalPosition;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    FramingSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mHorizontalPosition = mVerticalPosition = TargetTrackerCore.DEFAULT_FRAMING_POSITION;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @Override
    public double getHorizontalPosition() {
        return mHorizontalPosition;
    }

    @Override
    public double getVerticalPosition() {
        return mVerticalPosition;
    }

    @Override
    public void setPosition(@FloatRange(from = 0, to = 1) double horizontalPosition,
                            @FloatRange(from = 0, to = 1) double verticalPosition) {
        horizontalPosition = DoubleRangeCore.RATIO.clamp(horizontalPosition);
        verticalPosition = DoubleRangeCore.RATIO.clamp(verticalPosition);
        if ((Double.compare(mHorizontalPosition, horizontalPosition) != 0
             || Double.compare(mVerticalPosition, verticalPosition) != 0)
            && mBackend.setTargetPosition(horizontalPosition, verticalPosition)) {
            double rollbackHorizontalPosition = mHorizontalPosition;
            double rollbackVerticalPosition = mVerticalPosition;
            mHorizontalPosition = horizontalPosition;
            mVerticalPosition = verticalPosition;
            mController.postRollback(() -> {
                mHorizontalPosition = rollbackHorizontalPosition;
                mVerticalPosition = rollbackVerticalPosition;
            });
        }
    }

    /**
     * Updates target position in frame.
     *
     * @param horizontalPosition target horizontal position in frame
     * @param verticalPosition   target vertical position in frame
     */
    void update(@FloatRange(from = 0, to = 1) double horizontalPosition,
                @FloatRange(from = 0, to = 1) double verticalPosition) {
        if (mController.cancelRollback()
            || Double.compare(mHorizontalPosition, horizontalPosition) != 0
            || Double.compare(mVerticalPosition, verticalPosition) != 0) {
            mHorizontalPosition = horizontalPosition;
            mVerticalPosition = verticalPosition;
            mController.notifyChange(false);
        }
    }

    /**
     * Cancels any pending rollback.
     */
    void cancelRollback() {
        if (mController.cancelRollback()) {
            mController.notifyChange(false);
        }
    }
}
