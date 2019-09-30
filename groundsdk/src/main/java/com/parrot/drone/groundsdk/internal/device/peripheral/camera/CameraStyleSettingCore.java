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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.internal.value.IntSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;
import com.parrot.drone.groundsdk.value.IntegerRange;

import java.util.Collection;
import java.util.EnumSet;

/** Core class for CameraStyle.Setting. */
public final class CameraStyleSettingCore extends CameraStyle.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets style setting.
         *
         * @param style style to set
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state now,
         *         otherwise {@code false}
         */
        boolean setStyle(@NonNull CameraStyle.Style style);

        /**
         * Sets style parameters.
         *
         * @param saturation saturation to set
         * @param contrast   contrast to set
         * @param sharpness  sharpness to set
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state now,
         *         otherwise {@code false}
         */
        boolean setStyleParameters(int saturation, int contrast, int sharpness);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Supported styles. */
    @NonNull
    private final EnumSet<CameraStyle.Style> mSupportedStyles;

    /** Current style saturation. */
    @NonNull
    private final CameraStyleParameterCore mSaturation;

    /** Current style contrast. */
    @NonNull
    private CameraStyleParameterCore mContrast;

    /** Current style sharpness. */
    @NonNull
    private CameraStyleParameterCore mSharpness;

    /** Current style. */
    @NonNull
    private CameraStyle.Style mStyle;

    /** Default style. */
    private static final CameraStyle.Style DEFAULT_STYLE = CameraStyle.Style.STANDARD;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraStyleSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mSupportedStyles = EnumSet.noneOf(CameraStyle.Style.class);
        mSaturation = new CameraStyleParameterCore(saturation ->
                mBackend.setStyleParameters(saturation, mContrast.getValue(), mSharpness.getValue()));
        mContrast = new CameraStyleParameterCore(contrast ->
                mBackend.setStyleParameters(mSaturation.getValue(), contrast, mSharpness.getValue()));
        mSharpness = new CameraStyleParameterCore(sharpness ->
                mBackend.setStyleParameters(mSaturation.getValue(), mContrast.getValue(), sharpness));
        mStyle = DEFAULT_STYLE;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public EnumSet<CameraStyle.Style> supportedStyles() {
        return EnumSet.copyOf(mSupportedStyles);
    }

    @NonNull
    @Override
    public CameraStyle.Style style() {
        return mStyle;
    }

    @NonNull
    @Override
    public CameraStyle.Setting setStyle(@NonNull CameraStyle.Style style) {
        if (mStyle != style && mSupportedStyles.contains(style) && mBackend.setStyle(style)) {
            CameraStyle.Style rollbackStyle = mStyle;
            mStyle = style;
            mController.postRollback(() -> mStyle = rollbackStyle);
        }
        return this;
    }

    @NonNull
    @Override
    public CameraStyleParameterCore saturation() {
        return mSaturation;
    }

    @NonNull
    @Override
    public CameraStyleParameterCore contrast() {
        return mContrast;
    }

    @NonNull
    @Override
    public CameraStyleParameterCore sharpness() {
        return mSharpness;
    }

    /**
     * Updates supported styles.
     *
     * @param styles supported styles
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraStyleSettingCore updateSupportedStyles(@NonNull Collection<CameraStyle.Style> styles) {
        if (mSupportedStyles.retainAll(styles) | mSupportedStyles.addAll(styles)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current style.
     *
     * @param style exposure mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraStyleSettingCore updateStyle(@NonNull CameraStyle.Style style) {
        if (mController.cancelRollback() || mStyle != style) {
            mStyle = style;
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

    /** Core class for CameraStyle.StyleParameter. */
    public final class CameraStyleParameterCore implements CameraStyle.StyleParameter {

        /** Int setting backend. */
        @NonNull
        private final IntSettingCore mDelegate;

        /**
         * Constructor.
         *
         * @param backend backend that will process value changes
         */
        CameraStyleParameterCore(@NonNull IntSettingCore.Backend backend) {
            mDelegate = new IntSettingCore(mController, backend);
        }

        @Override
        public int getMin() {
            return mDelegate.getMin();
        }

        @Override
        public int getMax() {
            return mDelegate.getMax();
        }

        @Override
        public int getValue() {
            return mDelegate.getValue();
        }

        @Override
        public void setValue(int value) {
            mDelegate.setValue(value);
        }

        /**
         * Updates current parameter value.
         *
         * @param value parameter value
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public CameraStyleParameterCore updateValue(int value) {
            mDelegate.updateValue(value);
            return this;
        }

        /**
         * Updates parameter bounds.
         *
         * @param bounds range defining new parameter bounds
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public CameraStyleParameterCore updateBounds(@NonNull IntegerRange bounds) {
            mDelegate.updateBounds(bounds);
            return this;
        }
    }
}
