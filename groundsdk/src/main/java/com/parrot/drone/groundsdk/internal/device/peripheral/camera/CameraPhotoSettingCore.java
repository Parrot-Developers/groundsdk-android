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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.internal.value.DoubleRangeCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;
import com.parrot.drone.groundsdk.value.DoubleRange;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/** Core class for CameraPhoto.Setting. */
public final class CameraPhotoSettingCore extends CameraPhoto.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets camera photo mode settings.
         *
         * @param mode              photo mode to set
         * @param format            photo format to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         * @param fileFormat        photo file format to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         * @param burst             burst value to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         * @param bracketing        bracketing value to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         * @param timelapseInterval time-lapse interval to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         * @param gpslapseInterval  GPS-lapse interval to set, {@code null} to let the backend automatically pick an
         *                          appropriate value, if feasible
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setPhoto(@NonNull CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format,
                         @Nullable CameraPhoto.FileFormat fileFormat, @Nullable CameraPhoto.BurstValue burst,
                         @Nullable CameraPhoto.BracketingValue bracketing, @Nullable Double timelapseInterval,
                         @Nullable Double gpslapseInterval);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Supported burst values. */
    @NonNull
    private final EnumSet<CameraPhoto.BurstValue> mSupportedBurstValues;

    /** Supported bracketing values. */
    @NonNull
    private final EnumSet<CameraPhoto.BracketingValue> mSupportedBracketingValues;

    /** Time-lapse interval range, in seconds. */
    @NonNull
    private final DoubleRangeCore mTimelapseIntervalRange;

    /** GPS-lapse interval range, in meters. */
    @NonNull
    private final DoubleRangeCore mGpslapseIntervalRange;

    /**
     * Supported capabilities.
     * <p>
     * Map each supported photo mode to supported formats in this mode, which in turn are mapped to supported file
     * formats in this format (and mode) associated with a flag telling whether HDR is supported in this configuration.
     */
    @NonNull
    private EnumMap<CameraPhoto.Mode, EnumMap<CameraPhoto.Format, EnumMap<CameraPhoto.FileFormat, Boolean>>>
            mCapabilities;

    /** Current photo mode. */
    @NonNull
    private CameraPhoto.Mode mMode;

    /** Current photo format. */
    @NonNull
    private CameraPhoto.Format mFormat;

    /** Current photo file format. */
    @NonNull
    private CameraPhoto.FileFormat mFileFormat;

    /** Current burst value. */
    @NonNull
    private CameraPhoto.BurstValue mBurst;

    /** Current bracketing value. */
    @NonNull
    private CameraPhoto.BracketingValue mBracketing;

    /** Current time-lapse interval, in seconds. */
    private double mTimelapseInterval;

    /** Current GPS-lapse interval, in meters. */
    private double mGpslapseInterval;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraPhotoSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mCapabilities = new EnumMap<>(CameraPhoto.Mode.class);
        mSupportedBurstValues = EnumSet.noneOf(CameraPhoto.BurstValue.class);
        mSupportedBracketingValues = EnumSet.noneOf(CameraPhoto.BracketingValue.class);
        mTimelapseIntervalRange = new DoubleRangeCore(1, 1);
        mGpslapseIntervalRange = new DoubleRangeCore(1, 1);
        mMode = CameraPhoto.Mode.SINGLE;
        mFormat = CameraPhoto.Format.RECTILINEAR;
        mFileFormat = CameraPhoto.FileFormat.JPEG;
        mBurst = CameraPhoto.BurstValue.values()[0];
        mBracketing = CameraPhoto.BracketingValue.values()[0];
        mTimelapseInterval = 1;
        mGpslapseInterval = 1;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.Mode> supportedModes() {
        Set<CameraPhoto.Mode> modes = mCapabilities.keySet();
        return modes.isEmpty() ? EnumSet.noneOf(CameraPhoto.Mode.class) : EnumSet.copyOf(modes);
    }

    @NonNull
    @Override
    public CameraPhoto.Mode mode() {
        return mMode;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setMode(@NonNull CameraPhoto.Mode mode) {
        if (mMode != mode && isSupported(mode)) {
            sendSettings(mode, null, null, null, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.Format> supportedFormats() {
        return supportedFormatsFor(mMode);
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.Format> supportedFormatsFor(@NonNull CameraPhoto.Mode mode) {
        EnumMap<CameraPhoto.Format, ?> formats = mCapabilities.get(mode);
        return formats == null || formats.isEmpty() ? EnumSet.noneOf(CameraPhoto.Format.class)
                : EnumSet.copyOf(formats.keySet());
    }

    @NonNull
    @Override
    public CameraPhoto.Format format() {
        return mFormat;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setFormat(@NonNull CameraPhoto.Format format) {
        if (mFormat != format && isSupported(mMode, format)) {
            sendSettings(null, format, null, null, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.FileFormat> supportedFileFormats() {
        return supportedFileFormatsFor(mFormat);
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.FileFormat> supportedFileFormatsFor(@NonNull CameraPhoto.Format format) {
        return supportedFileFormatsFor(mMode, format);
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.FileFormat> supportedFileFormatsFor(@NonNull CameraPhoto.Mode mode,
                                                                   @NonNull CameraPhoto.Format format) {
        EnumMap<CameraPhoto.Format, EnumMap<CameraPhoto.FileFormat, Boolean>> formats = mCapabilities.get(mode);
        if (formats == null) {
            return EnumSet.noneOf(CameraPhoto.FileFormat.class);
        }
        EnumMap<CameraPhoto.FileFormat, Boolean> fileFormats = formats.get(format);
        return fileFormats == null || fileFormats.isEmpty() ? EnumSet.noneOf(CameraPhoto.FileFormat.class)
                : EnumSet.copyOf(fileFormats.keySet());
    }

    @NonNull
    @Override
    public CameraPhoto.FileFormat fileFormat() {
        return mFileFormat;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setFileFormat(@NonNull CameraPhoto.FileFormat fileFormat) {
        if (mFileFormat != fileFormat && isSupported(mMode, mFormat, fileFormat)) {
            sendSettings(null, null, fileFormat, null, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.BurstValue> supportedBurstValues() {
        return EnumSet.copyOf(mSupportedBurstValues);
    }

    @NonNull
    @Override
    public CameraPhoto.BurstValue burstValue() {
        return mBurst;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setBurstValue(@NonNull CameraPhoto.BurstValue burst) {
        if (mBurst != burst && mSupportedBurstValues.contains(burst)) {
            sendSettings(null, null, null, burst, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraPhoto.BracketingValue> supportedBracketingValues() {
        return EnumSet.copyOf(mSupportedBracketingValues);
    }

    @NonNull
    @Override
    public CameraPhoto.BracketingValue bracketingValue() {
        return mBracketing;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setBracketingValue(@NonNull CameraPhoto.BracketingValue bracketing) {
        if (mBracketing != bracketing && mSupportedBracketingValues.contains(bracketing)) {
            sendSettings(null, null, null, null, bracketing, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public DoubleRange timelapseIntervalRange() {
        return mTimelapseIntervalRange;
    }

    @Override
    public double timelapseInterval() {
        return mTimelapseInterval;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setTimelapseInterval(double interval) {
        interval = mTimelapseIntervalRange.clamp(interval);
        if (Double.compare(mTimelapseInterval, interval) != 0) {
            sendSettings(null, null, null, null, null, interval, null);
        }
        return this;
    }

    @NonNull
    @Override
    public DoubleRange gpslapseIntervalRange() {
        return mGpslapseIntervalRange;
    }

    @Override
    public double gpslapseInterval() {
        return mGpslapseInterval;
    }

    @NonNull
    @Override
    public CameraPhoto.Setting setGpslapseInterval(double interval) {
        interval = mGpslapseIntervalRange.clamp(interval);
        if (Double.compare(mGpslapseInterval, interval) != 0) {
            sendSettings(null, null, null, null, null, null, interval);
        }
        return this;
    }

    @Override
    public boolean isHdrAvailable() {
        return isHdrAvailable(mMode, mFormat, mFileFormat);
    }

    @Override
    public boolean isHdrAvailable(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                                  @NonNull CameraPhoto.FileFormat fileFormat) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode, format, fileFormat) && mCapabilities.get(mode).get(format).get(fileFormat);
    }

    @Override
    public void setSingleMode(@NonNull CameraPhoto.Format format, @NonNull CameraPhoto.FileFormat fileFormat) {
        if (isSupported(CameraPhoto.Mode.SINGLE, format, fileFormat)
            && (mMode != CameraPhoto.Mode.SINGLE || mFormat != format || mFileFormat != fileFormat)) {
            sendSettings(CameraPhoto.Mode.SINGLE, format, fileFormat, null, null, null, null);
        }
    }

    @Override
    public void setBurstMode(@NonNull CameraPhoto.Format format, @NonNull CameraPhoto.FileFormat fileFormat,
                             @NonNull CameraPhoto.BurstValue burst) {
        if (isSupported(CameraPhoto.Mode.BURST, format, fileFormat) && mSupportedBurstValues.contains(burst)
            && (mMode != CameraPhoto.Mode.BURST || mFormat != format || mFileFormat != fileFormat || mBurst != burst)) {
            sendSettings(CameraPhoto.Mode.BURST, format, fileFormat, burst, null, null, null);
        }
    }

    @Override
    public void setBracketingMode(@NonNull CameraPhoto.Format format, @NonNull CameraPhoto.FileFormat fileFormat,
                                  @NonNull CameraPhoto.BracketingValue bracketing) {
        if (isSupported(CameraPhoto.Mode.BRACKETING, format, fileFormat)
            && mSupportedBracketingValues.contains(bracketing)
            && (mMode != CameraPhoto.Mode.BRACKETING || mFormat != format || mFileFormat != fileFormat
                || mBracketing != bracketing)) {
            sendSettings(CameraPhoto.Mode.BRACKETING, format, fileFormat, null, bracketing, null, null);
        }
    }

    @Override
    public void setTimelapseMode(@NonNull CameraPhoto.Format format, @NonNull CameraPhoto.FileFormat fileFormat,
                                 double interval) {
        interval = mTimelapseIntervalRange.clamp(interval);
        if (isSupported(CameraPhoto.Mode.TIME_LAPSE, format, fileFormat)
            && (mMode != CameraPhoto.Mode.TIME_LAPSE || mFormat != format || mFileFormat != fileFormat
                || Double.compare(mTimelapseInterval, interval) != 0)) {
            sendSettings(CameraPhoto.Mode.TIME_LAPSE, format, fileFormat, null, null, interval, null);
        }
    }

    @Override
    public void setGpslapseMode(@NonNull CameraPhoto.Format format, @NonNull CameraPhoto.FileFormat fileFormat,
                                double interval) {
        interval = mGpslapseIntervalRange.clamp(interval);
        if (isSupported(CameraPhoto.Mode.GPS_LAPSE, format, fileFormat)
            && (mMode != CameraPhoto.Mode.GPS_LAPSE || mFormat != format || mFileFormat != fileFormat
                || Double.compare(mGpslapseInterval, interval) != 0)) {
            sendSettings(CameraPhoto.Mode.GPS_LAPSE, format, fileFormat, null, null, null, interval);
        }
    }

    /**
     * Data class representing a set of photo capabilities.
     * <p>
     * This links a set of supported photo modes, to a set of photo formats supported in this mode, and to a set of
     * photo file formats supported in this modes and formats.
     * <p>
     * Fields are publicly accessible, but contain final immutable collections, so no modification is possible.
     */
    public static final class Capability {

        /**
         * Builds a new {@code Capability}.
         *
         * @param modes        supported photo modes
         * @param formats      photo formats supported in those {@code modes}
         * @param fileFormats  photo file formats supported in those {@code modes} and {@code formats}
         * @param hdrAvailable availability of HDR
         *
         * @return a new {@code Capability} representing the given support constraints
         */
        @NonNull
        public static Capability of(@NonNull Set<CameraPhoto.Mode> modes,
                                    @NonNull Set<CameraPhoto.Format> formats,
                                    @NonNull Set<CameraPhoto.FileFormat> fileFormats,
                                    boolean hdrAvailable) {
            return new Capability(modes, formats, fileFormats, hdrAvailable);
        }

        /** Supported photo modes. */
        @NonNull
        public final Set<CameraPhoto.Mode> mModes;

        /** Supported photo formats with respect to the supported photo modes. */
        @NonNull
        public final Set<CameraPhoto.Format> mFormats;

        /** Supported photo file formats with respect to the supported photo modes and formats. */
        @NonNull
        public final Set<CameraPhoto.FileFormat> mFileFormats;

        /** Availability of HDR. */
        public final boolean mHdrAvailable;

        /**
         * Constructor.
         *
         * @param modes        supported photo modes
         * @param formats      photo formats supported in those {@code modes}
         * @param fileFormats  photo file formats supported in those {@code modes} and {@code formats}
         * @param hdrAvailable availability of HDR
         */
        private Capability(@NonNull Set<CameraPhoto.Mode> modes,
                           @NonNull Set<CameraPhoto.Format> formats,
                           @NonNull Set<CameraPhoto.FileFormat> fileFormats,
                           boolean hdrAvailable) {
            mModes = Collections.unmodifiableSet(modes);
            mFormats = Collections.unmodifiableSet(formats);
            mFileFormats = Collections.unmodifiableSet(fileFormats);
            mHdrAvailable = hdrAvailable;
        }
    }

    /**
     * Updates camera photo mode capabilities.
     * <p>
     * Processes the given collection of capabilities in order and merge them together to form the overall set of
     * current mode/format/file format/HDR capabilities for the camera photo mode.
     *
     * @param capabilities collection of capabilities
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateCapabilities(@NonNull Collection<Capability> capabilities) {
        EnumMap<CameraPhoto.Mode, EnumMap<CameraPhoto.Format, EnumMap<CameraPhoto.FileFormat, Boolean>>> newCaps =
                new EnumMap<>(CameraPhoto.Mode.class);

        for (Capability capability : capabilities) {
            for (CameraPhoto.Mode mode : capability.mModes) {
                EnumMap<CameraPhoto.Format, EnumMap<CameraPhoto.FileFormat, Boolean>> formats = newCaps.get(mode);
                if (formats == null) {
                    formats = new EnumMap<>(CameraPhoto.Format.class);
                    newCaps.put(mode, formats);
                }
                for (CameraPhoto.Format format : capability.mFormats) {
                    EnumMap<CameraPhoto.FileFormat, Boolean> fileFormats = formats.get(format);
                    if (fileFormats == null) {
                        fileFormats = new EnumMap<>(CameraPhoto.FileFormat.class);
                        formats.put(format, fileFormats);
                    }
                    for (CameraPhoto.FileFormat fileFormat : capability.mFileFormats) {
                        Boolean hdrAvailable = fileFormats.get(fileFormat);
                        if (hdrAvailable == null) {
                            hdrAvailable = capability.mHdrAvailable;
                            fileFormats.put(fileFormat, hdrAvailable);
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
     * Updates supported burst values.
     *
     * @param burstValues supported burst values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateSupportedBurstValues(@NonNull Collection<CameraPhoto.BurstValue> burstValues) {
        if (mSupportedBurstValues.retainAll(burstValues) | mSupportedBurstValues.addAll(burstValues)) {
            if (!mSupportedBurstValues.isEmpty() && !mSupportedBurstValues.contains(mBurst)) {
                mBurst = mSupportedBurstValues.iterator().next();
            }
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported bracketing values.
     *
     * @param bracketingValues supported bracketing values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateSupportedBracketingValues(
            @NonNull Collection<CameraPhoto.BracketingValue> bracketingValues) {
        if (mSupportedBracketingValues.retainAll(bracketingValues)
            | mSupportedBracketingValues.addAll(bracketingValues)) {
            if (!mSupportedBracketingValues.isEmpty() && !mSupportedBracketingValues.contains(mBracketing)) {
                mBracketing = mSupportedBracketingValues.iterator().next();
            }
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported time-lapse interval range.
     *
     * @param range new time-lapse interval range, in seconds
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateTimelapseIntervalRange(@NonNull DoubleRange range) {
        if (!mTimelapseIntervalRange.equals(range)) {
            mTimelapseIntervalRange.updateBounds(range);
            mTimelapseInterval = mTimelapseIntervalRange.clamp(mTimelapseInterval);
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported GPS-lapse interval range.
     *
     * @param range new GPS-lapse interval range, in meters
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateGpslapseIntervalRange(@NonNull DoubleRange range) {
        if (!mGpslapseIntervalRange.equals(range)) {
            mGpslapseIntervalRange.updateBounds(range);
            mGpslapseInterval = mGpslapseIntervalRange.clamp(mGpslapseInterval);
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current photo mode.
     *
     * @param mode photo mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateMode(@NonNull CameraPhoto.Mode mode) {
        if (mController.cancelRollback() || mMode != mode) {
            mMode = mode;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current photo format.
     *
     * @param format photo format
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateFormat(@NonNull CameraPhoto.Format format) {
        if (mController.cancelRollback() || mFormat != format) {
            mFormat = format;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current photo file format.
     *
     * @param fileFormat photo file format
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateFileFormat(@NonNull CameraPhoto.FileFormat fileFormat) {
        if (mController.cancelRollback() || mFileFormat != fileFormat) {
            mFileFormat = fileFormat;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current burst value.
     *
     * @param burst burst value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateBurstValue(@NonNull CameraPhoto.BurstValue burst) {
        if (!mSupportedBurstValues.contains(burst)) {
            burst = mSupportedBurstValues.isEmpty() ? mBurst : mSupportedBurstValues.iterator().next();
        }
        if (mController.cancelRollback() || mBurst != burst) {
            mBurst = burst;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current bracketing value.
     *
     * @param bracketing bracketing value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateBracketingValue(@NonNull CameraPhoto.BracketingValue bracketing) {
        if (!mSupportedBracketingValues.contains(bracketing)) {
            bracketing = mSupportedBracketingValues.isEmpty() ?
                    mBracketing : mSupportedBracketingValues.iterator().next();
        }
        if (mController.cancelRollback() || mBracketing != bracketing) {
            mBracketing = bracketing;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current time-lapse interval.
     *
     * @param interval new time-lapse interval, in seconds
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateTimelapseInterval(double interval) {
        if (mController.cancelRollback() || Double.compare(mTimelapseInterval, interval) != 0) {
            mTimelapseInterval = interval;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current GPS-lapse interval.
     *
     * @param interval new GPS-lapse interval, in meters
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraPhotoSettingCore updateGpslapseInterval(double interval) {
        if (mController.cancelRollback() || Double.compare(mGpslapseInterval, interval) != 0) {
            mGpslapseInterval = interval;
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
     * Sends photo settings to backend.
     *
     * @param mode              mode to set, {@code null} to use {@link #mMode current mode}
     * @param format            format to set, {@code null} to let the backend automatically pick an appropriate value,
     *                          if feasible
     * @param fileFormat        file format to set, {@code null} to let the backend automatically pick an appropriate
     *                          value, if feasible
     * @param burst             burst value to set, {@code null} to let the backend automatically pick an appropriate
     *                          value, if feasible
     * @param bracketing        bracketing value to set, {@code null} to let the backend automatically pick an
     *                          appropriate value, if feasible
     * @param timelapseInterval time-lapse interval to set, {@code null} to let the backend automatically pick an
     *                          appropriate value, if feasible
     * @param gpslapseInterval  GPS-lapse interval to set, {@code null} to let the backend automatically pick an
     *                          appropriate value, if feasible
     */
    private void sendSettings(@Nullable CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format,
                              @Nullable CameraPhoto.FileFormat fileFormat, @Nullable CameraPhoto.BurstValue burst,
                              @Nullable CameraPhoto.BracketingValue bracketing, @Nullable Double timelapseInterval,
                              @Nullable Double gpslapseInterval) {
        CameraPhoto.Mode rollbackMode = mMode;
        CameraPhoto.Format rollbackFormat = mFormat;
        CameraPhoto.FileFormat rollbackFileFormat = mFileFormat;
        CameraPhoto.BurstValue rollbackBurst = mBurst;
        CameraPhoto.BracketingValue rollbackBracketing = mBracketing;
        double rollbackTimelapseInterval = mTimelapseInterval;
        double rollbackGpslapseInterval = mGpslapseInterval;
        if (mBackend.setPhoto(mode == null ? mMode : mode, format, fileFormat, burst, bracketing, timelapseInterval,
                gpslapseInterval)) {
            if (mode != null) {
                mMode = mode;
            }
            if (format != null) {
                mFormat = format;
            }
            if (fileFormat != null) {
                mFileFormat = fileFormat;
            }
            if (burst != null) {
                mBurst = burst;
            }
            if (bracketing != null) {
                mBracketing = bracketing;
            }
            if (timelapseInterval != null) {
                mTimelapseInterval = timelapseInterval;
            }
            if (gpslapseInterval != null) {
                mGpslapseInterval = gpslapseInterval;
            }
            mController.postRollback(() -> {
                mMode = rollbackMode;
                mFormat = rollbackFormat;
                mFileFormat = rollbackFileFormat;
                mBurst = rollbackBurst;
                mBracketing = rollbackBracketing;
                mTimelapseInterval = rollbackTimelapseInterval;
                mGpslapseInterval = rollbackGpslapseInterval;
            });
        }
    }

    /**
     * Tells whether some photo mode is currently supported.
     *
     * @param mode photo mode to test
     *
     * @return {@code true} if the given photo mode is currently supported, otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraPhoto.Mode mode) {
        return mCapabilities.containsKey(mode);
    }

    /**
     * Tells whether some photo mode and format pair is currently supported.
     *
     * @param mode   photo mode to test
     * @param format photo format to test
     *
     * @return {@code true} if the given photo mode is currently supported and the given photo format is
     *         supported in the given mode, otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode) && mCapabilities.get(mode).containsKey(format);
    }

    /**
     * Tells whether some photo mode, format and file format triplet is currently supported.
     *
     * @param mode       photo mode to test
     * @param format     photo format to test
     * @param fileFormat photo file format to test
     *
     * @return {@code true} if the given photo mode is currently supported and the given photo format is
     *         supported in the given mode, and the given photo file format is supported in the given mode and format,
     *         otherwise {@code false}
     */
    private boolean isSupported(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                                @NonNull CameraPhoto.FileFormat fileFormat) {
        //noinspection ConstantConditions: asserted by isSupported
        return isSupported(mode, format) && mCapabilities.get(mode).get(format).containsKey(fileFormat);
    }
}
