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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraAlignment;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;
import com.parrot.drone.groundsdk.value.DoubleRange;

/** Core class for CameraAlignment.Setting. */
public final class CameraAlignmentSettingCore extends CameraAlignment.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets alignment offset on each axis.
         *
         * @param yaw   offset to apply to the yaw axis
         * @param pitch offset to apply to the pitch axis
         * @param roll  offset to apply to the roll axis
         *
         * @return {@code true} if the setting could successfully be sent to the device, {@code false} otherwise
         */
        boolean setAlignment(double yaw, double pitch, double roll);

        /**
         * Resets alignment on all axes.
         *
         * @return {@code true} if the request could successfully be sent to the device, {@code false} otherwise
         */
        boolean resetAlignment();
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Yaw alignment setting. */
    @NonNull
    private final DoubleSettingCore mYaw;

    /** Pitch alignment setting. */
    @NonNull
    private DoubleSettingCore mPitch;

    /** Roll alignment setting. */
    @NonNull
    private DoubleSettingCore mRoll;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraAlignmentSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mYaw = new DoubleSettingCore(mController,
                yaw -> mBackend.setAlignment(yaw, mPitch.getValue(), mRoll.getValue()));
        mPitch = new DoubleSettingCore(mController,
                pitch -> mBackend.setAlignment(mYaw.getValue(), pitch, mRoll.getValue()));
        mRoll = new DoubleSettingCore(mController,
                roll -> mBackend.setAlignment(mYaw.getValue(), mPitch.getValue(), roll));
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public DoubleRange supportedYawRange() {
        return mYaw.getBounds();
    }

    @Override
    public double yaw() {
        return mYaw.getValue();
    }

    @NonNull
    @Override
    public CameraAlignment.Setting setYaw(double offset) {
        mYaw.setValue(offset);
        return this;
    }

    @NonNull
    @Override
    public DoubleRange supportedPitchRange() {
        return mPitch.getBounds();
    }

    @Override
    public double pitch() {
        return mPitch.getValue();
    }

    @NonNull
    @Override
    public CameraAlignment.Setting setPitch(double offset) {
        mPitch.setValue(offset);
        return this;
    }

    @NonNull
    @Override
    public DoubleRange supportedRollRange() {
        return mRoll.getBounds();
    }

    @Override
    public double roll() {
        return mRoll.getValue();
    }

    @NonNull
    @Override
    public CameraAlignment.Setting setRoll(double offset) {
        mRoll.setValue(offset);
        return this;
    }

    @Override
    public boolean reset() {
        return mBackend.resetAlignment();
    }

    /**
     * Updates supported yaw alignment offset range.
     *
     * @param range supported yaw range
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updateSupportedYawRange(@NonNull DoubleRange range) {
        mYaw.updateBounds(range);
        return this;
    }

    /**
     * Updates current yaw alignment offset.
     *
     * @param offset yaw offset
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updateYaw(double offset) {
        mYaw.updateValue(offset);
        return this;
    }

    /**
     * Updates supported pitch alignment offset range.
     *
     * @param range supported pitch range
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updateSupportedPitchRange(@NonNull DoubleRange range) {
        mPitch.updateBounds(range);
        return this;
    }

    /**
     * Updates current pitch alignment offset.
     *
     * @param offset pitch offset
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updatePitch(double offset) {
        mPitch.updateValue(offset);
        return this;
    }

    /**
     * Updates supported roll alignment offset range.
     *
     * @param range supported roll range
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updateSupportedRollRange(@NonNull DoubleRange range) {
        mRoll.updateBounds(range);
        return this;
    }

    /**
     * Updates current roll alignment offset.
     *
     * @param offset roll offset
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraAlignmentSettingCore updateRoll(double offset) {
        mRoll.updateValue(offset);
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
}
