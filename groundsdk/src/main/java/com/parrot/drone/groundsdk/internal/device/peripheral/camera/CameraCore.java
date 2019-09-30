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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.OptionalBooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Abstract core base for Camera. */
public abstract class CameraCore extends SingletonComponentCore implements Camera {

    /** Engine-specific backend for Camera. */
    public interface Backend extends CameraExposureSettingCore.Backend, CameraWhiteBalanceSettingCore.Backend,
                                     CameraPhotoSettingCore.Backend, CameraRecordingSettingCore.Backend,
                                     CameraZoomCore.Backend, CameraStyleSettingCore.Backend,
                                     CameraExposureLockCore.Backend, CameraWhiteBalanceLockCore.Backend,
                                     CameraAlignmentSettingCore.Backend {

        /**
         * Sets camera mode.
         *
         * @param mode camera mode to set
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMode(@NonNull Mode mode);

        /**
         * Sets EV compensation.
         *
         * @param ev exposure compensation value to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setEvCompensation(@NonNull CameraEvCompensation ev);

        /**
         * Sets automatic HDR setting.
         *
         * @param enable automatic HDR setting value to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setAutoHdr(boolean enable);

        /**
         * Sets auto-record setting.
         *
         * @param enable auto-record setting value to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setAutoRecord(boolean enable);

        /**
         * Requests photo capture to start.
         *
         * @return {@code true} if the request could successfully be sent to the device, {@code false} otherwise
         */
        boolean startPhotoCapture();

        /**
         * Requests photo capture to stop.
         *
         * @return {@code true} if the request could successfully be sent to the device, {@code false} otherwise
         */
        boolean stopPhotoCapture();

        /**
         * Requests video recording to start.
         *
         * @return {@code true} if the request could successfully be sent to the device, {@code false} otherwise
         */
        boolean startRecording();

        /**
         * Requests video recording to stop.
         *
         * @return {@code true} if the request could successfully be sent to the device, {@code false} otherwise
         */
        boolean stopRecording();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Camera mode setting. */
    @NonNull
    private final EnumSettingCore<Mode> mModeSetting;

    /** Camera exposure setting. */
    @NonNull
    private final CameraExposureSettingCore mExposureSetting;

    /** Camera exposure lock. */
    @Nullable
    private CameraExposureLockCore mExposureLock;

    /** Camera EV compensation setting. */
    @NonNull
    private final EnumSettingCore<CameraEvCompensation> mExposureCompensationSetting;

    /** Camera white balance setting. */
    @NonNull
    private final CameraWhiteBalanceSettingCore mWhiteBalanceSetting;

    /** Camera white balance lock. */
    @Nullable
    private CameraWhiteBalanceLockCore mWhiteBalanceLock;

    /** Camera automatic HDR setting. */
    @NonNull
    private final OptionalBooleanSettingCore mAutoHdr;

    /** Camera image style setting. */
    @NonNull
    private final CameraStyleSettingCore mStyleSetting;

    /** Camera alignment setting. */
    @Nullable
    private CameraAlignmentSettingCore mAlignmentSetting;

    /** Camera zoom. */
    @Nullable
    private CameraZoomCore mZoom;

    /** Camera photo mode setting. */
    @NonNull
    private final CameraPhotoSettingCore mPhotoSetting;

    /** Camera recording mode setting. */
    @NonNull
    private final CameraRecordingSettingCore mRecordingSetting;

    /** Auto-record setting. */
    @NonNull
    private final OptionalBooleanSettingCore mAutoRecord;

    /** Current take photo function state. */
    @NonNull
    private final CameraPhotoStateCore mPhotoState;

    /** Current video recording function state. */
    @NonNull
    private final CameraRecordingStateCore mRecordingState;

    /** {@code true} if HDR is currently active, otherwise {@code false}. */
    private boolean mHdrActive;

    /** {@code true} when the camera is active, otherwise {@code false}. */
    private boolean mActive;

    /**
     * Constructor.
     *
     * @param descriptor      specific descriptor of the provided camera
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    CameraCore(@NonNull ComponentDescriptor<Peripheral, ? extends Camera> descriptor,
               @NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(descriptor, peripheralStore);
        mBackend = backend;
        mModeSetting = new EnumSettingCore<>(Mode.class, new SettingController(this::onSettingChange),
                mBackend::setMode);
        mExposureSetting = new CameraExposureSettingCore(this::onSettingChange, mBackend);
        mWhiteBalanceSetting = new CameraWhiteBalanceSettingCore(this::onSettingChange, mBackend);
        mAutoHdr = new OptionalBooleanSettingCore(new SettingController(this::onSettingChange), mBackend::setAutoHdr);
        mStyleSetting = new CameraStyleSettingCore(this::onSettingChange, mBackend);
        mExposureCompensationSetting = new EnumSettingCore<>(CameraEvCompensation.class,
                new SettingController(this::onSettingChange), mBackend::setEvCompensation);
        mPhotoSetting = new CameraPhotoSettingCore(this::onSettingChange, mBackend);
        mRecordingSetting = new CameraRecordingSettingCore(this::onSettingChange, mBackend);
        mAutoRecord = new OptionalBooleanSettingCore(new SettingController(this::onSettingChange),
                mBackend::setAutoRecord);
        mPhotoState = new CameraPhotoStateCore(this::onChange);
        mRecordingState = new CameraRecordingStateCore(this::onChange);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        mActive = false;
        mWhiteBalanceLock = null;
        mExposureLock = null;
        mAlignmentSetting = null;
        mZoom = null;
        cancelSettingsRollbacks();
    }

    @Override
    public boolean isActive() {
        return mActive;
    }

    @NonNull
    @Override
    public EnumSettingCore<Mode> mode() {
        return mModeSetting;
    }

    @NonNull
    @Override
    public CameraExposureSettingCore exposure() {
        return mExposureSetting;
    }

    @Nullable
    @Override
    public CameraExposureLockCore exposureLock() {
        return mActive ? mExposureLock : null;
    }

    @NonNull
    @Override
    public EnumSettingCore<CameraEvCompensation> exposureCompensation() {
        return mExposureCompensationSetting;
    }

    @NonNull
    @Override
    public CameraWhiteBalanceSettingCore whiteBalance() {
        return mWhiteBalanceSetting;
    }

    @Nullable
    @Override
    public CameraWhiteBalanceLockCore whiteBalanceLock() {
        return mActive ? mWhiteBalanceLock : null;
    }

    @NonNull
    @Override
    public OptionalBooleanSettingCore autoHdr() {
        return mAutoHdr;
    }

    @NonNull
    @Override
    public CameraStyleSettingCore style() {
        return mStyleSetting;
    }

    @Nullable
    @Override
    public CameraAlignmentSettingCore alignment() {
        return mActive ? mAlignmentSetting : null;
    }

    @Nullable
    @Override
    public CameraZoomCore zoom() {
        return mZoom;
    }

    @NonNull
    @Override
    public CameraPhotoSettingCore photo() {
        return mPhotoSetting;
    }

    @NonNull
    @Override
    public CameraPhotoStateCore photoState() {
        return mPhotoState;
    }

    @NonNull
    @Override
    public CameraRecordingSettingCore recording() {
        return mRecordingSetting;
    }

    @NonNull
    @Override
    public OptionalBooleanSettingCore autoRecord() {
        return mAutoRecord;
    }

    @NonNull
    @Override
    public CameraRecordingStateCore recordingState() {
        return mRecordingState;
    }

    @Override
    public boolean isHdrActive() {
        return mHdrActive;
    }

    @Override
    public boolean isHdrAvailable() {
        boolean hdrAvailable = false;
        switch (mModeSetting.getValue()) {
            case RECORDING:
                hdrAvailable = mRecordingSetting.isHdrAvailable();
                break;
            case PHOTO:
                hdrAvailable = mPhotoSetting.isHdrAvailable();
                break;
        }
        return hdrAvailable;
    }

    @Override
    public boolean canStartPhotoCapture() {
        return mPhotoState.get() == CameraPhoto.State.FunctionState.STOPPED;
    }

    @Override
    public void startPhotoCapture() {
        if (canStartPhotoCapture() && mBackend.startPhotoCapture()) {
            mPhotoState.updateState(CameraPhoto.State.FunctionState.STARTED);
            mChanged = true;
            notifyUpdated();
        }
    }

    @Override
    public boolean canStopPhotoCapture() {
        CameraPhoto.Mode mode = mPhotoSetting.mode();
        return mPhotoState.get() == CameraPhoto.State.FunctionState.STARTED
               && (mode == CameraPhoto.Mode.TIME_LAPSE || mode == CameraPhoto.Mode.GPS_LAPSE);
    }

    @Override
    public void stopPhotoCapture() {
        if (canStopPhotoCapture() && mBackend.stopPhotoCapture()) {
            mPhotoState.updateState(CameraPhoto.State.FunctionState.STOPPING);
            mChanged = true;
            notifyUpdated();
        }
    }

    @Override
    public boolean canStartRecording() {
        return mRecordingState.get() == CameraRecording.State.FunctionState.STOPPED;
    }

    @Override
    public void startRecording() {
        if (canStartRecording() && mBackend.startRecording()) {
            mRecordingState.updateState(CameraRecording.State.FunctionState.STARTING);
            mChanged = true;
            notifyUpdated();
        }
    }

    @Override
    public boolean canStopRecording() {
        CameraRecording.State.FunctionState recordingState = mRecordingState.get();
        return recordingState == CameraRecording.State.FunctionState.STARTED
               || recordingState == CameraRecording.State.FunctionState.STARTING;
    }

    @Override
    public void stopRecording() {
        if (canStopRecording() && mBackend.stopRecording()) {
            mRecordingState.updateState(CameraRecording.State.FunctionState.STOPPING);
            mChanged = true;
            notifyUpdated();
        }
    }

    /**
     * Updates global camera activity flag.
     *
     * @param active {@code true} to mark the camera active, {@code false} to mark it inactive
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public final CameraCore updateActiveFlag(boolean active) {
        if (mActive != active) {
            mActive = active;
            mChanged = true;
        }
        return this;
    }

    /**
     * Creates the camera zoom if it doesn't exist yet and returns it.
     *
     * @return the camera zoom
     */
    @NonNull
    public final CameraZoomCore createZoomIfNeeded() {
        if (mZoom == null) {
            mZoom = new CameraZoomCore(this::onSettingChange, mBackend);
            mChanged = true;
        }
        return mZoom;
    }

    /**
     * Updates current exposure lock mode.
     *
     * @param mode    exposure lock mode
     * @param centerX horizontal center of the lock region in the video, when {@code mode} is
     *                {@link CameraExposureLock.Mode#REGION} (relative position, from left (0.0) to right (1.0)
     * @param centerY vertical center in the video, when {@code mode} is {@link CameraExposureLock.Mode#REGION}
     *                (relative position, from bottom (0.0) to top (1.0)
     * @param width   width of the region, when {@code mode} is {@link CameraExposureLock.Mode#REGION}
     *                (relative to the video width, from 0.0 to 1.0)
     * @param height  height of the region, when {@code mode} is {@link CameraExposureLock.Mode#REGION}
     *                (relative to the video height, from 0.0 to 1.0)
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraCore updateExposureLock(@NonNull CameraExposureLock.Mode mode,
                                         double centerX, double centerY, double width, double height) {
        if (mExposureLock == null) {
            mExposureLock = new CameraExposureLockCore((fromUser) -> {
                if (mActive || fromUser) {
                    onSettingChange(fromUser);
                }
            }, mBackend);
            mChanged |= mActive;
        }
        mExposureLock.updateMode(mode, centerX, centerY, width, height);
        return this;
    }

    /**
     * Creates the white balance lock sub-peripheral if it doesn't exist yet and returns it.
     *
     * @return the white balance lock
     */
    @NonNull
    public final CameraWhiteBalanceLockCore createWhiteBalanceLockIfNeeded() {
        if (mWhiteBalanceLock == null) {
            mWhiteBalanceLock = new CameraWhiteBalanceLockCore((fromUser) -> {
                if (mActive || fromUser) {
                    onSettingChange(fromUser);
                }
            }, mBackend);
            mChanged |= mActive;
        }
        return mWhiteBalanceLock;
    }

    /**
     * Creates the alignment setting if it doesn't exist yet and returns it.
     *
     * @return the alignment setting
     */
    @NonNull
    public CameraAlignmentSettingCore createAlignmentIfNeeded() {
        if (mAlignmentSetting == null) {
            mAlignmentSetting = new CameraAlignmentSettingCore((fromUser) -> {
                if (mActive || fromUser) {
                    onSettingChange(fromUser);
                }
            }, mBackend);
            mChanged |= mActive;
        }
        return mAlignmentSetting;
    }

    /**
     * Updates current HDR state.
     *
     * @param active new current state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraCore updateHdrActive(boolean active) {
        if (mHdrActive != active) {
            mHdrActive = active;
            mChanged |= mActive;
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraCore cancelSettingsRollbacks() {
        mModeSetting.cancelRollback();
        mExposureSetting.cancelRollback();
        if (mExposureLock != null) {
            mExposureLock.cancelRollback();
        }
        mWhiteBalanceSetting.cancelRollback();
        mAutoHdr.cancelRollback();
        mStyleSetting.cancelRollback();
        if (mAlignmentSetting != null) {
            mAlignmentSetting.cancelRollback();
        }
        mExposureCompensationSetting.cancelRollback();
        mPhotoSetting.cancelRollback();
        mRecordingSetting.cancelRollback();
        mAutoRecord.cancelRollback();
        if (mZoom != null) {
            mZoom.cancelRollback();
        }
        return this;
    }

    /**
     * Notified when an user setting changes.
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
