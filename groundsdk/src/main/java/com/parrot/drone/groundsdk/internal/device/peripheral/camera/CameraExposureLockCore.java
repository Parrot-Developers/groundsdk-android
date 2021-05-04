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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for CameraExposureLock. */
public final class CameraExposureLockCore implements CameraExposureLock {

    /** Exposure lock backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sends command to set exposure lock mode.
         *
         * @param mode    exposure mode
         * @param centerX horizontal center in the video (relative position, from left (0.0) to right (1.0)),
         *                if {@code mode} is {@link Mode#REGION}
         * @param centerY vertical center in the video (relative position, from bottom (0.0) to top (1.0)),
         *                if {@code mode} is {@link Mode#REGION}
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setExposureLock(@NonNull Mode mode,
                                @FloatRange(from = 0.0, to = 1.0) double centerX,
                                @FloatRange(from = 0.0, to = 1.0) double centerY);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Current exposure lock mode. */
    @NonNull
    private Mode mMode;

    /** Horizontal center of exposure lock region in the video (relative position, from left (0.0) to right (1.0). */
    @FloatRange(from = 0.0, to = 1.0)
    private double mRegionCenterX;

    /** Vertical center of exposure lock region in the video (relative position, from bottom (0.0) to top (1.0). */
    @FloatRange(from = 0.0, to = 1.0)
    private double mRegionCenterY;

    /** Width of exposure lock region (relative to the video width, from 0.0 to 1.0). */
    @FloatRange(from = 0.0, to = 1.0)
    private double mRegionWidth;

    /** Height of exposure lock region (relative to the video height, from 0.0 to 1.0). */
    @FloatRange(from = 0.0, to = 1.0)
    private double mRegionHeight;

    /** Default mode. */
    private static final Mode DEFAULT_MODE = Mode.NONE;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraExposureLockCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mMode = DEFAULT_MODE;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public Mode mode() {
        return mMode;
    }

    @Override
    public double getRegionCenterX() {
        return mRegionCenterX;
    }

    @Override
    public double getRegionCenterY() {
        return mRegionCenterY;
    }

    @Override
    public double getRegionWidth() {
        return mRegionWidth;
    }

    @Override
    public double getRegionHeight() {
        return mRegionHeight;
    }

    @Override
    public void lockCurrentValues() {
        if (mMode != Mode.CURRENT_VALUES) {
            sendMode(Mode.CURRENT_VALUES);
        }
    }

    @Override
    public void lockOnRegion(@FloatRange(from = 0.0, to = 1.0) double centerX,
                             @FloatRange(from = 0.0, to = 1.0) double centerY) {
        if (mMode != Mode.REGION
            || Double.compare(mRegionCenterX, centerX) != 0
            || Double.compare(mRegionCenterY, centerY) != 0) {
            sendMode(Mode.REGION, centerX, centerY);
        }
    }

    @Override
    public void unlock() {
        if (mMode != Mode.NONE) {
            sendMode(Mode.NONE);
        }
    }

    /**
     * Updates current exposure lock mode.
     *
     * @param mode    exposure lock mode
     * @param centerX horizontal center of the lock region in the video, when {@code mode} is {@link Mode#REGION}
     *                (relative position, from left (0.0) to right (1.0)
     * @param centerY vertical center in the video, when {@code mode} is {@link Mode#REGION}
     *                (relative position, from bottom (0.0) to top (1.0)
     * @param width   width of the region, when {@code mode} is {@link Mode#REGION}
     *                (relative to the video width, from 0.0 to 1.0)
     * @param height  height of the region, when {@code mode} is {@link Mode#REGION}
     *                (relative to the video height, from 0.0 to 1.0)
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureLockCore updateMode(@NonNull Mode mode,
                                             @FloatRange(from = 0.0, to = 1.0) double centerX,
                                             @FloatRange(from = 0.0, to = 1.0) double centerY,
                                             @FloatRange(from = 0.0, to = 1.0) double width,
                                             @FloatRange(from = 0.0, to = 1.0) double height) {
        if (mController.cancelRollback()
            || mMode != mode
            || Double.compare(mRegionCenterX, centerX) != 0
            || Double.compare(mRegionCenterY, centerY) != 0
            || Double.compare(mRegionWidth, width) != 0
            || Double.compare(mRegionHeight, height) != 0) {
            mMode = mode;
            mRegionCenterX = centerX;
            mRegionCenterY = centerY;
            mRegionWidth = width;
            mRegionHeight = height;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Cancels any pending rollback.
     */
    void cancelRollback() {
        if (mController.cancelRollback()) {
            mController.notifyChange(false);
        }
    }

    /**
     * Sends exposure lock mode to backend.
     *
     * @param mode exposure lock mode
     */
    private void sendMode(@NonNull Mode mode) {
        sendMode(mode, 0, 0);
    }

    /**
     * Sends exposure lock mode to backend.
     *
     * @param mode    exposure lock mode
     * @param centerX horizontal center of the lock region in the video (relative position, from left (0.0) to right
     *                (1.0)
     * @param centerY vertical center in the video (relative position, from bottom (0.0) to top (1.0)
     */
    private void sendMode(@NonNull Mode mode,
                          @FloatRange(from = 0.0, to = 1.0) double centerX,
                          @FloatRange(from = 0.0, to = 1.0) double centerY) {
        Mode rollbackMode = mMode;
        double rollbackCenterX = mRegionCenterX;
        double rollbackCenterY = mRegionCenterY;
        double rollbackWidth = mRegionWidth;
        double rollbackHeight = mRegionHeight;
        if (mBackend.setExposureLock(mode, centerX, centerY)) {
            mMode = mode;
            mRegionCenterX = centerX;
            mRegionCenterY = centerY;
            mRegionHeight = 0;
            mRegionWidth = 0;
            mController.postRollback(() -> {
                mMode = rollbackMode;
                mRegionCenterX = rollbackCenterX;
                mRegionCenterY = rollbackCenterY;
                mRegionWidth = rollbackWidth;
                mRegionHeight = rollbackHeight;
            });
        }
    }

    /**
     * Compares two exposure lock mode requests.
     *
     * @param mode1    first exposure lock mode to compare
     * @param centerX1 first exposure lock region horizontal center to compare
     * @param centerY1 first exposure lock region vertical center to compare
     * @param mode2    second exposure lock mode to compare
     * @param centerX2 second exposure lock region horizontal center to compare
     * @param centerY2 second exposure lock region vertical center to compare
     *
     * @return {@code true} is the two exposure lock modes are identical, otherwise {@code false}
     */
    public static boolean isSameRequest(@NonNull Mode mode1, double centerX1, double centerY1,
                                        @NonNull Mode mode2, double centerX2, double centerY2) {
        return (mode1 == Mode.NONE && mode2 == Mode.NONE)
               || (mode1 == Mode.CURRENT_VALUES && mode2 == Mode.CURRENT_VALUES)
               || (mode1 == Mode.REGION && mode2 == Mode.REGION
                   && (Math.abs(centerX1 - centerX2) < 0.1) && (Math.abs(centerY1 - centerY2) < 0.1));
    }
}
