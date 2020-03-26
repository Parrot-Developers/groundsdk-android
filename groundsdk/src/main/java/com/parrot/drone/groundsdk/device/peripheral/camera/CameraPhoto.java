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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.value.DoubleRange;

import java.util.EnumSet;

/**
 * Scoping class for camera photo related types and settings.
 */
public final class CameraPhoto {

    /** Camera photo mode. */
    public enum Mode {

        /** Photo mode that allows to take a single photo. */
        SINGLE,

        /** Photo mode that allows to take a burst of multiple photos, each using different EV compensation values. */
        BRACKETING,

        /** Photo mode that allows to take a burst of multiple photos. */
        BURST,

        /** Photo mode that allows to take photos at regular time intervals. */
        TIME_LAPSE,

        /** Photo mode that allows to take photos at regular GPS position intervals. */
        GPS_LAPSE
    }

    /** Camera photo format. */
    public enum Format {

        /** Uses a rectilinear projection, de-warped. */
        RECTILINEAR,

        /** Uses full sensor resolution, not de-warped. */
        FULL_FRAME,

        /** Uses a large projection, partially de-warped. */
        LARGE
    }

    /** Camera photo file format. */
    public enum FileFormat {

        /** Photo stored in JPEG format. */
        JPEG,

        /** Photo stored in DNG format. */
        DNG,

        /** Photo stored in both DNG and JPEG formats. */
        DNG_AND_JPEG
    }

    /** Camera photo burst value for {@link Mode#BURST burst mode}. */
    public enum BurstValue {

        /** Takes 14 different photos regularly over 4 seconds. */
        BURST_14_OVER_4S,

        /** Takes 14 different photos regularly over 2 seconds. */
        BURST_14_OVER_2S,

        /** Takes 14 different photos regularly over 1 seconds. */
        BURST_14_OVER_1S,

        /** Takes 10 different photos regularly over 4 seconds. */
        BURST_10_OVER_4S,

        /** Takes 10 different photos regularly over 2 seconds. */
        BURST_10_OVER_2S,

        /** Takes 10 different photos regularly over 1 seconds. */
        BURST_10_OVER_1S,

        /** Takes 4 different photos regularly over 4 seconds. */
        BURST_4_OVER_4S,

        /** Takes 4 different photos regularly over 2 seconds. */
        BURST_4_OVER_2S,

        /** Takes 4 different photos regularly over 1 seconds. */
        BURST_4_OVER_1S
    }

    /** Camera photo bracketing value for {@link Mode#BRACKETING bracketing mode}. */
    public enum BracketingValue {

        /** Takes 3 pictures applying, in order, -1 EV, 0 EV and +1 EV exposure compensation values. */
        EV_1,

        /** Takes 3 pictures applying, in order, -2 EV, 0 EV and +2 EV exposure compensation values. */
        EV_2,

        /** Takes 3 pictures applying, in order, -3 EV, 0 EV and +3 EV exposure compensation values. */
        EV_3,

        /** Takes 5 pictures applying, in order, -2 EV, -1 EV, 0 EV, +1 EV, and +2 EV exposure compensation values. */
        EV_1_2,

        /** Takes 5 pictures applying, in order, -3 EV, -1 EV, 0 EV, +1 EV, and +3 EV exposure compensation values. */
        EV_1_3,

        /** Takes 5 pictures applying, in order, -3 EV, -2 EV, 0 EV, +2 EV, and +3 EV exposure compensation values. */
        EV_2_3,

        /**
         * Takes 7 pictures applying, in order, -3 EV, -2 EV, -1 EV, 0 EV, +1 EV, +2 EV, and +3 EV exposure
         * compensation values.
         */
        EV_1_2_3
    }

    /**
     * Camera photo setting.
     * <p>
     * Allows to configure the camera photo mode and parameters, such as: <ul>
     * <li>Photo format,</li>
     * <li>Photo file format,</li>
     * <li>Burst value (for {@link Mode#BURST burst mode}),</li>
     * <li>Bracketing value (for {@link Mode#BRACKETING bracketing mode}),</li>
     * <li>Time interval (for {@link Mode#TIME_LAPSE}),</li>
     * <li>Distance interval (for {@link Mode#GPS_LAPSE}).</li>
     * </ul>
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the currently supported photo modes.
         * <p>
         * An empty set means that the whole setting is currently unsupported. <br>
         * A set containing a single value means that the setting is supported, yet the application is not allowed to
         * change the photo mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported photo modes
         */
        @NonNull
        public abstract EnumSet<Mode> supportedModes();

        /**
         * Retrieves the current photo mode.
         * <p>
         * Return value should be considered meaningless in case the set of {@link #supportedModes() supported modes}
         * is empty.
         *
         * @return current photo mode
         */
        @NonNull
        public abstract Mode mode();

        /**
         * Sets the photo mode.
         * <p>
         * The provided value must be present in the set of {@link #supportedModes() supported modes}, otherwise
         * this method does nothing.
         *
         * @param mode mode value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setMode(@NonNull Mode mode);

        /**
         * Retrieves the photo formats supported <i>in the current mode</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported photo formats
         *
         * @see #supportedFormatsFor(CameraPhoto.Mode)
         */
        @NonNull
        public abstract EnumSet<Format> supportedFormats();

        /**
         * Retrieves the photo formats supported <i>in a given mode</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param mode mode to query supported formats of
         *
         * @return supported photo formats for that mode
         *
         * @see #supportedFormats()
         */
        @NonNull
        public abstract EnumSet<Format> supportedFormatsFor(@NonNull Mode mode);

        /**
         * Retrieves the currently applied photo format.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedFormats() supported formats for the current mode} is empty.
         *
         * @return photo format
         */
        @NonNull
        public abstract Format format();

        /**
         * Sets the photo format to be applied in the current mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedFormats() supported formats for the current mode}, otherwise this method does nothing.
         *
         * @param format photo format to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setFormat(@NonNull Format format);

        /**
         * Retrieves the photo file formats supported <i>in the current mode and format</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported photo file formats
         *
         * @see #supportedFileFormatsFor(CameraPhoto.Format)
         * @see #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format)
         */
        @NonNull
        public abstract EnumSet<FileFormat> supportedFileFormats();

        /**
         * Retrieves the photo file formats supported <i>in the current mode and a given format</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param format format to query supported file formats of
         *
         * @return supported photo file formats for this format in the current mode
         *
         * @see #supportedFileFormats()
         * @see #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format)
         */
        @NonNull
        public abstract EnumSet<FileFormat> supportedFileFormatsFor(@NonNull Format format);

        /**
         * Retrieves the photo file formats supported <i>in a given mode and a given format</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param mode   mode to query supported formats of
         * @param format format to query supported file formats of
         *
         * @return supported photo file formats for those mode and format
         *
         * @see #supportedFileFormats()
         * @see #supportedFileFormatsFor(CameraPhoto.Format)
         */
        @NonNull
        public abstract EnumSet<FileFormat> supportedFileFormatsFor(@NonNull Mode mode, @NonNull Format format);

        /**
         * Retrieves the currently applied photo file format.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedFileFormats() supported file formats for the current mode and format} is empty.
         *
         * @return photo file format
         */
        @NonNull
        public abstract FileFormat fileFormat();

        /**
         * Sets the photo file format to be applied in the current mode and format.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedFileFormats() supported file formats for the current mode and format}, otherwise this
         * method does nothing.
         *
         * @param fileFormat photo file format to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setFileFormat(@NonNull FileFormat fileFormat);

        /**
         * Retrieves the currently supported burst values for use in {@link Mode#BURST} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported burst values
         */
        @NonNull
        public abstract EnumSet<BurstValue> supportedBurstValues();

        /**
         * Retrieves the burst value applied in {@link Mode#BURST} mode.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedBurstValues() supported burst values} is empty.
         *
         * @return burst value
         */
        @NonNull
        public abstract BurstValue burstValue();

        /**
         * Sets the burst value to be applied in {@link Mode#BURST} mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedBurstValues() supported burst values}, otherwise this method does nothing.
         *
         * @param burst burst value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setBurstValue(@NonNull BurstValue burst);

        /**
         * Retrieves the currently supported bracketing values for use in {@link Mode#BRACKETING} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported bracketing values
         */
        @NonNull
        public abstract EnumSet<BracketingValue> supportedBracketingValues();

        /**
         * Retrieves the bracketing value applied in {@link Mode#BRACKETING} mode.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedBracketingValues() supported bracketing values} is empty.
         *
         * @return bracketing value
         */
        @NonNull
        public abstract BracketingValue bracketingValue();

        /**
         * Sets the bracketing value to be applied in {@link Mode#BRACKETING} mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedBracketingValues() supported bracketing values}, otherwise this method does nothing.
         *
         * @param bracketing bracketing value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setBracketingValue(@NonNull BracketingValue bracketing);

        /**
         * Retrieves the range of currently supported time-lapse intervals for use in {@link Mode#TIME_LAPSE} mode.
         *
         * @return supported time-lapse interval range
         */
        @NonNull
        public abstract DoubleRange timelapseIntervalRange();

        /**
         * Retrieves the time-lapse interval applied in {@link Mode#TIME_LAPSE} mode, in seconds.
         *
         * @return the time-lapse interval
         */
        public abstract double timelapseInterval();

        /**
         * Sets the time-lapse interval to be applied in {@link Mode#TIME_LAPSE} mode.
         * <p>
         * The given value may be clamped to the {@link #timelapseIntervalRange() supported range} if necessary.
         *
         * @param interval the time-lapse interval to set, in seconds
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setTimelapseInterval(double interval);

        /**
         * Retrieves the range of currently supported GPS-lapse intervals for use in {@link Mode#GPS_LAPSE} mode.
         *
         * @return supported GPS-lapse interval range
         */
        @NonNull
        public abstract DoubleRange gpslapseIntervalRange();

        /**
         * Retrieves the GPS-lapse interval applied in {@link Mode#GPS_LAPSE} mode, in meters.
         *
         * @return the GPS-lapse interval
         */
        public abstract double gpslapseInterval();

        /**
         * Sets the GPS-lapse interval to be applied in {@link Mode#GPS_LAPSE} mode.
         * <p>
         * The given value may be clamped to the {@link #gpslapseIntervalRange() supported range} if necessary.
         *
         * @param interval the GPS-lapse interval to set, in meters
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setGpslapseInterval(double interval);

        /**
         * Tells whether HDR is available in the current mode, format and file format.
         *
         * @return {@code true} if HDR is available in the current mode, format and file format, otherwise {@code false}
         */
        public abstract boolean isHdrAvailable();

        /**
         * Tells whether HDR is available for specific photo mode, format and file format.
         *
         * @param mode       photo mode to test
         * @param format     photo format to test
         * @param fileFormat photo file format to test
         *
         * @return {@code true} if HDR is available in the given photo mode, format and file format,
         *         otherwise {@code false}
         */
        public abstract boolean isHdrAvailable(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                                               @NonNull CameraPhoto.FileFormat fileFormat);

        /**
         * Switches to {@link Mode#SINGLE single} mode and applies the given photo format and file format at the same
         * time.
         * <p>
         * {@link Mode#SINGLE} mode must be present in the set of {@link #supportedModes() supported modes}; provided
         * format must be present in the set of {@link #supportedFormatsFor(CameraPhoto.Mode) supported formats for
         * that mode}; provided file format must be present in the set of
         * {@link #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format) supported file format for those mode
         * and format}, otherwise this method does nothing.
         *
         * @param format     photo format to set
         * @param fileFormat photo file format to set
         */
        public abstract void setSingleMode(@NonNull Format format, @NonNull FileFormat fileFormat);

        /**
         * Switches to {@link Mode#BURST burst} mode and applies the given photo format, file format and burst value at
         * the same time.
         * <p>
         * {@link Mode#BURST} mode must be present in the set of {@link #supportedModes() supported modes}; provided
         * format must be present in the set of {@link #supportedFormatsFor(CameraPhoto.Mode) supported formats for
         * that mode}; provided file format must be present in the set of
         * {@link #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format) supported file format for those mode
         * and format}; provided burst value must be present in the set of {@link #supportedBurstValues() supported
         * burst values}, otherwise this method does nothing.
         *
         * @param format     photo format to set
         * @param fileFormat photo file format to set
         * @param burst      burst value to set
         */
        public abstract void setBurstMode(@NonNull Format format, @NonNull FileFormat fileFormat,
                                          @NonNull BurstValue burst);

        /**
         * Switches to {@link Mode#BRACKETING bracketing} mode and applies the given photo format, file format and
         * bracketing value at the same time.
         * <p>
         * {@link Mode#BRACKETING} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided format must be present in the set of
         * {@link #supportedFormatsFor(CameraPhoto.Mode) supported formats for that mode}; provided file format must be
         * present in the set of {@link #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format) supported file
         * formats for those mode and format}; provided bracketing value must be present in the set of
         * {@link #supportedBracketingValues() supported bracketing values}, otherwise this method does nothing.
         *
         * @param format     photo format to set
         * @param fileFormat photo file format to set
         * @param bracketing bracketing value to set
         */
        public abstract void setBracketingMode(@NonNull Format format, @NonNull FileFormat fileFormat,
                                               @NonNull BracketingValue bracketing);

        /**
         * Switches to {@link Mode#TIME_LAPSE time-lapse} mode and applies the given photo format, file format and
         * time interval at the same time.
         * <p>
         * {@link Mode#TIME_LAPSE} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided format must be present in the set of
         * {@link #supportedFormatsFor(CameraPhoto.Mode) supported formats for that mode}; provided file format must be
         * present in the set of {@link #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format) supported file
         * formats for those mode and format}, otherwise this method does nothing.
         * <p>
         * The given interval value may be clamped to the {@link #timelapseIntervalRange()} supported range} if
         * necessary.
         *
         * @param format     photo format to set
         * @param fileFormat photo file format to set
         * @param interval   time-lapse interval to set, in seconds
         */
        public abstract void setTimelapseMode(@NonNull Format format, @NonNull FileFormat fileFormat, double interval);

        /**
         * Switches to {@link Mode#GPS_LAPSE GPS-lapse} mode and applies the given photo format, file format and
         * distance interval at the same time.
         * <p>
         * {@link Mode#GPS_LAPSE} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided format must be present in the set of
         * {@link #supportedFormatsFor(CameraPhoto.Mode) supported formats for that mode}; provided file format must be
         * present in the set of {@link #supportedFileFormatsFor(CameraPhoto.Mode, CameraPhoto.Format) supported file
         * formats for those mode and format}, otherwise this method does nothing.
         * <p>
         * The given value may be clamped to the {@link #gpslapseIntervalRange() supported range} if necessary.
         *
         * @param format     photo format to set
         * @param fileFormat photo file format to set
         * @param interval   GPS-lapse interval to set, in meters
         */
        public abstract void setGpslapseMode(@NonNull Format format, @NonNull FileFormat fileFormat, double interval);
    }

    /**
     * Provides the state of the camera photo function, access to the identifier of the latest saved photo(s) media,
     * and allows to keep track of the count of photos being taken while the camera is in the process of taking
     * photos.
     */
    public interface State {

        /** Camera photo function state. */
        enum FunctionState {

            /**
             * Camera photo function is inoperable at present.
             * <p>
             * This state is entered from any other state: <ul>
             * <li>after disconnection from the drone,</li>
             * <li>while connected, if the photo function becomes inoperable for some reason.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #photoCount()} is reset to {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>after connection to the drone, if the photo function is operable currently; state will
             * change to either {@link #STOPPED} if the camera is not currently taking photo(s), or
             * {@link #STARTED} if the camera is currently taking photo(s),</li>
             * <li>while connected, if the photo function becomes operable; state will change to
             * {@link #STOPPED}.</li>
             * </ul>
             */
            UNAVAILABLE,

            /**
             * Photo capture is stopped and ready to be started.
             * <p>
             * This state is entered: <ul>
             * <li>after connection to the drone, in case the photo function is ready to be operated,</li>
             * <li>from {@link #UNAVAILABLE} state, when the photo function becomes operable,</li>
             * <li>from {@link #STARTED} and {@link #STOPPING} state, when the camera is done taking and
             * saving photo(s) to drone storage,</li>
             * <li>from {@link #ERROR_INSUFFICIENT_STORAGE} and {@link #ERROR_INTERNAL} states, immediately after
             * any of those changes has been notified.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} contains the identifier of the latest saved
             * photo media, if available and transiting from {@link #STARTED} or
             * {@link #ERROR_INSUFFICIENT_STORAGE} states; otherwise it is {@code null},</li>
             * <li>{@link #photoCount()} is reset to {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>after a call to {@link Camera#startPhotoCapture()}; state will change to {@link #STARTED},
             * </li>
             * <li>when the photo function becomes inoperable; state will change to {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STOPPED,

            /**
             * Camera is currently taking a (some) photo(s).
             * <p>
             * This state is entered: <ul>
             * <li>after connection to the drone, if the camera is currently taking a photo,</li>
             * <li>from {@link #STOPPED} after a call to {@link Camera#startPhotoCapture()}.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #photoCount()} increments repeatedly each time a new photo has been taken.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>when the camera is done taking requested photo(s); state will change to {@link #STOPPED},</li>
             * <li>when the user stops a time-lapse or GPS-lapse; state will change to {@link #STOPPING},</li>
             * <li>if there is an error while taking photo(s); state will change to some {@code ERROR_*} state,</li>
             * <li>when the photo function becomes inoperable; state will change to {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STARTED,

            /**
             * Photo capture is stopping.
             * <p>
             * This state is entered from {@link #STARTED}, after a call to {@link Camera#stopPhotoCapture()}.
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #photoCount()} is reset to {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>when the camera is done taking requested photo(s); state will change to {@link #STOPPED},</li>
             * <li>if an error occurs while the photo capturing process is being stopped; state will change to
             * {@link #ERROR_INTERNAL},</li>
             * <li>when the photo function becomes inoperable; state will change to {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STOPPING,

            /**
             * Photo could not be saved due to insufficient storage space on the drone.
             * <p>
             * This state is entered from {@link #STARTED} if there is not enough available storage space on the
             * drone to save a photo.
             * <p>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #photoCount()} is reset to {@code 0}.</li>
             * </ul>
             * This state is exited immediately after having been notified; state will change to either {@link #STOPPED}
             * or {@link #UNAVAILABLE}.
             */
            ERROR_INSUFFICIENT_STORAGE,

            /**
             * Photo(s) could not be saved due to an internal, undocumented error
             * <p>
             * This state is entered from {@link #STARTED} if taking photo(s) failed due to an internal error.
             * <p>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #photoCount()} is reset to {@code 0}.</li>
             * </ul>
             * Warning: this state can be temporary, and can be quickly followed by the state {@link #STOPPED} or
             * {@link #UNAVAILABLE}.
             */
            ERROR_INTERNAL
        }

        /**
         * Retrieves the current camera photo function state.
         *
         * @return current camera photo function state
         */
        @NonNull
        FunctionState get();

        /**
         * Retrieves the identifier of the latest saved photo media since connection.
         * <p>
         * {@code null} unless {@link #get()} state} is {@link FunctionState#STOPPED} and some photo was taken
         * beforehand since the application is connected to the drone.
         *
         * @return saved media identifier if available, otherwise {@code null}
         */
        @Nullable
        String latestMediaId();

        /**
         * Retrieves the current count of taken photos.
         * <p>
         * {@code 0} unless {@link #get()} state} is {@link FunctionState#STARTED}, in which case the
         * count increments repeatedly each time one or more photo(s) have been taken.
         * <p>
         * This mainly applies to {@link Mode#BRACKETING}, {@link Mode#BURST}, {@link Mode#TIME_LAPSE} and
         * {@link Mode#GPS_LAPSE} modes, where multiple photos are taken. <br>
         * For {@link Mode#SINGLE} mode, this value will be {@code 1} after the photo has been taken.
         *
         * @return current taken photos count
         */
        @IntRange(from = 0)
        int photoCount();
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraPhoto() {
    }
}
