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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/** Core class for CameraRecording.Setting. */
public final class CameraRecordingSettingCore extends CameraRecording.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets camera recording mode settings.
         *
         * @param mode       recording mode to set
         * @param resolution recording resolution to set, {@code null} to let the backend automatically pick an
         *                   appropriate value, if feasible
         * @param framerate  recording framerate to set, {@code null} to let the backend automatically pick an
         *                   appropriate value, if feasible
         * @param hyperlapse hyperlapse value to set, {@code null} to let the backend automatically pick an appropriate
         *                   value, if feasible
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setRecording(@NonNull CameraRecording.Mode mode, @Nullable CameraRecording.Resolution resolution,
                             @Nullable CameraRecording.Framerate framerate,
                             @Nullable CameraRecording.HyperlapseValue hyperlapse);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Supported hyperlapse values. */
    @NonNull
    private final EnumSet<CameraRecording.HyperlapseValue> mSupportedHyperlapseValues;

    /**
     * Supported capabilities.
     * <p>
     * Map each supported recording mode to supported resolutions in this mode, which in turn are mapped to supported
     * framerates in this resolution (and mode) associated with a flag telling whether HDR is supported in this
     * configuration.
     */
    @NonNull
    private EnumMap<CameraRecording.Mode, EnumMap<CameraRecording.Resolution, EnumMap<CameraRecording.Framerate,
            Boolean>>> mCapabilities;

    /** Current recording mode. */
    @NonNull
    private CameraRecording.Mode mMode;

    /** Current recording resolution. */
    @NonNull
    private CameraRecording.Resolution mResolution;

    /** Current recording framerate. */
    @NonNull
    private CameraRecording.Framerate mFramerate;

    /** Current hyperlapse value. */
    @NonNull
    private CameraRecording.HyperlapseValue mHyperlapse;

    /** Recording bitrate for the current configuration. */
    private int mBitrate;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraRecordingSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mSupportedHyperlapseValues = EnumSet.noneOf(CameraRecording.HyperlapseValue.class);
        mCapabilities = new EnumMap<>(CameraRecording.Mode.class);
        mMode = CameraRecording.Mode.STANDARD;
        mResolution = CameraRecording.Resolution.RES_DCI_4K;
        mFramerate = CameraRecording.Framerate.FPS_30;
        mHyperlapse = CameraRecording.HyperlapseValue.values()[0];
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Mode> supportedModes() {
        Set<CameraRecording.Mode> modes = mCapabilities.keySet();
        return modes.isEmpty() ? EnumSet.noneOf(CameraRecording.Mode.class) : EnumSet.copyOf(modes);
    }

    @NonNull
    @Override
    public CameraRecording.Mode mode() {
        return mMode;
    }

    @NonNull
    @Override
    public CameraRecording.Setting setMode(@NonNull CameraRecording.Mode mode) {
        if (mMode != mode && isSupported(mode)) {
            sendSettings(mode, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Resolution> supportedResolutions() {
        return supportedResolutionsFor(mMode);
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Resolution> supportedResolutionsFor(@NonNull CameraRecording.Mode mode) {
        EnumMap<CameraRecording.Resolution, ?> resolutions = mCapabilities.get(mode);
        return resolutions == null || resolutions.isEmpty() ? EnumSet.noneOf(CameraRecording.Resolution.class)
                : EnumSet.copyOf(resolutions.keySet());
    }

    @NonNull
    @Override
    public CameraRecording.Resolution resolution() {
        return mResolution;
    }

    @NonNull
    @Override
    public CameraRecording.Setting setResolution(@NonNull CameraRecording.Resolution resolution) {
        if (mResolution != resolution && isSupported(mMode, resolution)) {
            sendSettings(null, resolution, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Framerate> supportedFramerates() {
        return supportedFrameratesFor(mResolution);
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Framerate> supportedFrameratesFor(@NonNull CameraRecording.Resolution resolution) {
        return supportedFrameratesFor(mMode, resolution);
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.Framerate> supportedFrameratesFor(@NonNull CameraRecording.Mode mode,
                                                                     @NonNull CameraRecording.Resolution resolution) {
        EnumMap<CameraRecording.Resolution, EnumMap<CameraRecording.Framerate, Boolean>> resolutions
                = mCapabilities.get(mode);
        if (resolutions == null) {
            return EnumSet.noneOf(CameraRecording.Framerate.class);
        }
        EnumMap<CameraRecording.Framerate, Boolean> framerates = resolutions.get(resolution);
        return framerates == null || framerates.isEmpty() ? EnumSet.noneOf(CameraRecording.Framerate.class)
                : EnumSet.copyOf(framerates.keySet());
    }

    @NonNull
    @Override
    public CameraRecording.Framerate framerate() {
        return mFramerate;
    }

    @NonNull
    @Override
    public CameraRecording.Setting setFramerate(@NonNull CameraRecording.Framerate framerate) {
        if (mFramerate != framerate && isSupported(mMode, mResolution, framerate)) {
            sendSettings(null, null, framerate, null);
        }
        return this;
    }

    @Override
    public boolean isHdrAvailable() {
        return isHdrAvailable(mMode, mResolution, mFramerate);
    }

    @Override
    public boolean isHdrAvailable(@NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution,
                                  @NonNull CameraRecording.Framerate framerate) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode, resolution, framerate) && mCapabilities.get(mode).get(resolution).get(framerate);
    }

    @NonNull
    @Override
    public EnumSet<CameraRecording.HyperlapseValue> supportedHyperlapseValues() {
        return EnumSet.copyOf(mSupportedHyperlapseValues);
    }

    @NonNull
    @Override
    public CameraRecording.HyperlapseValue hyperlapseValue() {
        return mHyperlapse;
    }

    @Override
    public int bitrate() {
        return mBitrate;
    }

    @NonNull
    @Override
    public CameraRecording.Setting setHyperlapseValue(@NonNull CameraRecording.HyperlapseValue hyperlapse) {
        if (mHyperlapse != hyperlapse && mSupportedHyperlapseValues.contains(hyperlapse)) {
            sendSettings(null, null, null, hyperlapse);
        }
        return this;
    }

    @Override
    public void setStandardMode(@NonNull CameraRecording.Resolution resolution,
                                @NonNull CameraRecording.Framerate framerate) {
        if (isSupported(CameraRecording.Mode.STANDARD, resolution, framerate)
            && (mMode != CameraRecording.Mode.STANDARD || mResolution != resolution || mFramerate != framerate)) {
            sendSettings(CameraRecording.Mode.STANDARD, resolution, framerate, null);
        }
    }

    @Override
    public void setHyperlapseMode(@NonNull CameraRecording.Resolution resolution,
                                  @NonNull CameraRecording.Framerate framerate,
                                  @NonNull CameraRecording.HyperlapseValue hyperlapse) {
        if (isSupported(CameraRecording.Mode.HYPERLAPSE, resolution, framerate)
            && mSupportedHyperlapseValues.contains(hyperlapse)
            && (mMode != CameraRecording.Mode.HYPERLAPSE || mResolution != resolution || mFramerate != framerate
                || mHyperlapse != hyperlapse)) {
            sendSettings(CameraRecording.Mode.HYPERLAPSE, resolution, framerate, hyperlapse);
        }
    }

    @Override
    public void setSlowMotionMode(@NonNull CameraRecording.Resolution resolution,
                                  @NonNull CameraRecording.Framerate framerate) {
        if (isSupported(CameraRecording.Mode.SLOW_MOTION, resolution, framerate)
            && (mMode != CameraRecording.Mode.SLOW_MOTION || mResolution != resolution || mFramerate != framerate)) {
            sendSettings(CameraRecording.Mode.SLOW_MOTION, resolution, framerate, null);
        }
    }

    @Override
    public void setHighFramerateMode(@NonNull CameraRecording.Resolution resolution,
                                     @NonNull CameraRecording.Framerate framerate) {
        if (isSupported(CameraRecording.Mode.HIGH_FRAMERATE, resolution, framerate)
            && (mMode != CameraRecording.Mode.HIGH_FRAMERATE || mResolution != resolution || mFramerate != framerate)) {
            sendSettings(CameraRecording.Mode.HIGH_FRAMERATE, resolution, framerate, null);
        }
    }

    /**
     * Data class representing a set of recording capabilities.
     * <p>
     * This links a set of supported recording modes, to a set of recording resolutions supported in this mode, and to
     * a set of recording framerates supported in this modes and resolutions.
     * <p>
     * Fields are publicly accessible, but contain final immutable collections, so no modification is possible.
     */
    public static final class Capability {

        /**
         * Builds a new {@code Capability}.
         *
         * @param modes        supported recording modes
         * @param resolutions  recording resolutions supported in those {@code modes}
         * @param framerates   recording framerates supported in those {@code modes} and {@code resolutions}
         * @param hdrAvailable availability of HDR
         *
         * @return a new {@code Capability} representing the given support constraints
         */
        @NonNull
        public static Capability of(@NonNull Set<CameraRecording.Mode> modes,
                                    @NonNull Set<CameraRecording.Resolution> resolutions,
                                    @NonNull Set<CameraRecording.Framerate> framerates,
                                    boolean hdrAvailable) {
            return new Capability(modes, resolutions, framerates, hdrAvailable);
        }

        /** Supported recording modes. */
        @NonNull
        public final Set<CameraRecording.Mode> mModes;

        /** Supported recording resolutions with respect to the supported recording modes. */
        @NonNull
        public final Set<CameraRecording.Resolution> mResolutions;

        /** Supported recording framerates with respect to the supported recording modes and resolutions. */
        @NonNull
        public final Set<CameraRecording.Framerate> mFramerates;

        /** Availability of HDR. */
        public final boolean mHdrAvailable;

        /**
         * Constructor.
         *
         * @param modes        supported recording modes
         * @param resolutions  recording resolutions supported in those {@code modes}
         * @param framerates   recording framerates supported in those {@code modes} and {@code resolutions}
         * @param hdrAvailable availability of HDR
         */
        private Capability(@NonNull Set<CameraRecording.Mode> modes,
                           @NonNull Set<CameraRecording.Resolution> resolutions,
                           @NonNull Set<CameraRecording.Framerate> framerates,
                           boolean hdrAvailable) {
            mModes = Collections.unmodifiableSet(modes);
            mResolutions = Collections.unmodifiableSet(resolutions);
            mFramerates = Collections.unmodifiableSet(framerates);
            mHdrAvailable = hdrAvailable;
        }
    }

    /**
     * Updates camera recording mode capabilities.
     * <p>
     * Processes the given collection of capabilities in order and merge them together to form the overall set of
     * current mode/resolution/framerate/HDR capabilities for the camera recording mode.
     *
     * @param capabilities collection of capabilities
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateCapabilities(@NonNull Collection<Capability> capabilities) {
        EnumMap<CameraRecording.Mode, EnumMap<CameraRecording.Resolution, EnumMap<CameraRecording.Framerate, Boolean>>>
                newCaps = new EnumMap<>(CameraRecording.Mode.class);

        for (Capability capability : capabilities) {
            for (CameraRecording.Mode mode : capability.mModes) {
                EnumMap<CameraRecording.Resolution, EnumMap<CameraRecording.Framerate, Boolean>> resolutions
                        = newCaps.get(mode);
                if (resolutions == null) {
                    resolutions = new EnumMap<>(CameraRecording.Resolution.class);
                    newCaps.put(mode, resolutions);
                }
                for (CameraRecording.Resolution resolution : capability.mResolutions) {
                    EnumMap<CameraRecording.Framerate, Boolean> framerates = resolutions.get(resolution);
                    if (framerates == null) {
                        framerates = new EnumMap<>(CameraRecording.Framerate.class);
                        resolutions.put(resolution, framerates);
                    }
                    for (CameraRecording.Framerate framerate : capability.mFramerates) {
                        Boolean hdrAvailable = framerates.get(framerate);
                        if (hdrAvailable == null) {
                            hdrAvailable = capability.mHdrAvailable;
                            framerates.put(framerate, hdrAvailable);
                        }
                    }
                }
            }
        }

        if (!newCaps.equals(mCapabilities)) {
            mCapabilities = newCaps;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported hyperlapse values.
     *
     * @param hyperlapseValues supported hyperlapse values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateSupportedHyperlapseValues(
            @NonNull Collection<CameraRecording.HyperlapseValue> hyperlapseValues) {
        if (mSupportedHyperlapseValues.retainAll(hyperlapseValues)
            | mSupportedHyperlapseValues.addAll(hyperlapseValues)) {
            if (!mSupportedHyperlapseValues.isEmpty() && !mSupportedHyperlapseValues.contains(mHyperlapse)) {
                mHyperlapse = mSupportedHyperlapseValues.iterator().next();
            }
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current recording mode.
     *
     * @param mode recording mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateMode(@NonNull CameraRecording.Mode mode) {
        if (mController.cancelRollback() || mMode != mode) {
            mMode = mode;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current recording resolution.
     *
     * @param resolution recording resolution
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateResolution(@NonNull CameraRecording.Resolution resolution) {
        if (mController.cancelRollback() || mResolution != resolution) {
            mResolution = resolution;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current recording framerate.
     *
     * @param framerate recording framerate
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateFramerate(@NonNull CameraRecording.Framerate framerate) {
        if (mController.cancelRollback() || mFramerate != framerate) {
            mFramerate = framerate;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current hyperlapse value.
     *
     * @param hyperlapse hyperlapse value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateHyperlapseValue(@NonNull CameraRecording.HyperlapseValue hyperlapse) {
        if (!mSupportedHyperlapseValues.contains(hyperlapse)) {
            hyperlapse = mSupportedHyperlapseValues.isEmpty() ?
                    mHyperlapse : mSupportedHyperlapseValues.iterator().next();
        }
        if (mController.cancelRollback() || mHyperlapse != hyperlapse) {
            mHyperlapse = hyperlapse;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates recording bitrate.
     *
     * @param bitrate new recording bitrate.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraRecordingSettingCore updateBitrate(int bitrate) {
        if (mBitrate != bitrate) {
            mBitrate = bitrate;
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
     * Sends recording settings to backend.
     *
     * @param mode       mode to set, {@code null} to use {@link #mMode current mode}
     * @param resolution resolution to set, {@code null} to let the backend automatically pick an appropriate value, if
     *                   feasible
     * @param framerate  framerate to set, {@code null} to let the backend automatically pick an appropriate
     *                   value, if feasible
     * @param hyperlapse hyperlapse value to set, {@code null} to let the backend automatically pick an appropriate
     *                   value, if feasible
     */
    private void sendSettings(@Nullable CameraRecording.Mode mode,
                              @Nullable CameraRecording.Resolution resolution,
                              @Nullable CameraRecording.Framerate framerate,
                              @Nullable CameraRecording.HyperlapseValue hyperlapse) {
        CameraRecording.Mode rollbackMode = mMode;
        CameraRecording.Resolution rollbackResolution = mResolution;
        CameraRecording.Framerate rollbackFramerate = mFramerate;
        CameraRecording.HyperlapseValue rollbackHyperlapse = mHyperlapse;
        if (mBackend.setRecording(mode == null ? mMode : mode, resolution, framerate, hyperlapse)) {
            if (mode != null) {
                mMode = mode;
            }
            if (resolution != null) {
                mResolution = resolution;
            }
            if (framerate != null) {
                mFramerate = framerate;
            }
            if (hyperlapse != null) {
                mHyperlapse = hyperlapse;
            }
            mController.postRollback(() -> {
                mMode = rollbackMode;
                mResolution = rollbackResolution;
                mFramerate = rollbackFramerate;
                mHyperlapse = rollbackHyperlapse;
            });
        }
    }

    /**
     * Tells whether some recording mode is currently supported.
     *
     * @param mode recording mode to test
     *
     * @return {@code true} if the given recording mode is currently supported, otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraRecording.Mode mode) {
        return mCapabilities.containsKey(mode);
    }

    /**
     * Tells whether some recording mode and resolution pair is currently supported.
     *
     * @param mode       recording mode to test
     * @param resolution recording resolution to test
     *
     * @return {@code true} if the given recording mode is currently supported and the given recording resolution is
     *         supported in the given mode, otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode) && mCapabilities.get(mode).containsKey(resolution);
    }

    /**
     * Tells whether some recording mode, resolution and framerate triplet is currently supported.
     *
     * @param mode       recording mode to test
     * @param resolution recording resolution to test
     * @param framerate  recording framerate to test
     *
     * @return {@code true} if the given recording mode is currently supported and the given recording resolution is
     *         supported in the given mode, and the given recording framerate is supported in the given mode and
     *         resolution, otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution,
                                @NonNull CameraRecording.Framerate framerate) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode, resolution) && mCapabilities.get(mode).get(resolution).containsKey(framerate);
    }
}
