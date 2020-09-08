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

import java.util.Date;
import java.util.EnumSet;

/**
 * Scoping class for camera recording related types and settings.
 */
public final class CameraRecording {

    /** Camera recording mode. */
    public enum Mode {

        /** Standard recording mode. */
        STANDARD,

        /** Records accelerated videos. Records 1 of n frames. */
        HYPERLAPSE,

        /** Records slowed down videos. */
        SLOW_MOTION,

        /** Records high-framerate videos (playback speed remains x1). */
        HIGH_FRAMERATE
    }

    /** Camera recording resolution. */
    public enum Resolution {

        /** 4096x2160 pixels (4k cinema). */
        RES_DCI_4K,

        /** 3840x2160 pixels (UHD). */
        RES_UHD_4K,

        /** 2740x1524 pixels. */
        RES_2_7K,

        /** 1920x1080 pixels (Full HD). */
        RES_1080P,

        /** 1440x1080 pixels (Full HD, 4:3 aspect ratio). */
        RES_1080P_4_3,

        /** 1280x720 pixels (HD). */
        RES_720P,

        /** 960x720 pixels (HD, 4:3 aspect ratio). */
        RES_720P_4_3,

        /** 856x480 pixels. */
        RES_480P,

        /** 7680x4320 pixels (UHD). */
        RES_UHD_8K,

        /** 5120x2880 pixels. */
        RES_5K
    }

    /** Camera recording framerate. */
    public enum Framerate {

        /** 239.76 frames per second. */
        FPS_240,

        /** 200 frames per second. */
        FPS_200,

        /** 191.81 frames per second. */
        FPS_192,

        /** 119.88 frames per second. */
        FPS_120,

        /** 100 frames per second. */
        FPS_100,

        /** 95.88 frames per second. */
        FPS_96,

        /** 59.94 frames per second. */
        FPS_60,

        /** 50 frames per second. */
        FPS_50,

        /** 47.952 frames per second. */
        FPS_48,

        /** 29.97 frames per second. */
        FPS_30,

        /** 25 frames per second. */
        FPS_25,

        /** 23.97 frames per second. */
        FPS_24,

        /** 20 frames per second. */
        FPS_20,

        /** 15 frames per second. */
        FPS_15,

        /** 10 frames per second. */
        FPS_10,

        /** 9 frames per second.  */
        FPS_9,

        /** 8.57 frames per second. */
        FPS_8_6
    }

    /** Camera recording hyperlapse value for {@link Mode#HYPERLAPSE hyperlapse mode}. */
    public enum HyperlapseValue {

        /** Records a frame every 15 frames. */
        RATIO_15,

        /** Records a frame every 30 frames. */
        RATIO_30,

        /** Records a frame every 60 frames. */
        RATIO_60,

        /** Records a frame every 120 frames. */
        RATIO_120,

        /** Records a frame every 240 frames. */
        RATIO_240,
    }

    /**
     * Camera recording setting.
     * <p>
     * Allows to configure the camera recording mode and parameters, such as: <ul>
     * <li>Recording resolution,</li>
     * <li>Recording framerate,</li>
     * <li>Hyperlapse value (for {@link Mode#HYPERLAPSE hyperlapse mode}.</li>
     * </ul>
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the currently supported recording modes.
         * <p>
         * An empty set means that the whole setting is currently unsupported. <br>
         * A set containing a single value means that the setting is supported, yet the application is not allowed to
         * change the recording mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported recording modes
         */
        @NonNull
        public abstract EnumSet<Mode> supportedModes();

        /**
         * Retrieves the current recording mode.
         * <p>
         * Return value should be considered meaningless in case the set of {@link #supportedModes() supported modes}
         * is empty.
         *
         * @return current recording mode
         */
        @NonNull
        public abstract Mode mode();

        /**
         * Sets the recording mode.
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
         * Retrieves the recording resolutions supported <i>in the current mode</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported recording resolutions
         *
         * @see #supportedResolutionsFor(CameraRecording.Mode)
         */
        @NonNull
        public abstract EnumSet<Resolution> supportedResolutions();

        /**
         * Retrieves the recording resolutions supported <i>in a given mode</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param mode mode to query supported resolutions of
         *
         * @return supported recording resolutions for that mode
         *
         * @see #supportedResolutions()
         */
        @NonNull
        public abstract EnumSet<Resolution> supportedResolutionsFor(@NonNull Mode mode);

        /**
         * Retrieves the currently applied recording resolution.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedResolutions() supported resolutions for the current mode} is empty.
         *
         * @return recording resolution
         */
        @NonNull
        public abstract Resolution resolution();

        /**
         * Sets the recording resolution to be applied in the current mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedResolutions() supported resolutions for the current mode}, otherwise this method does
         * nothing.
         *
         * @param resolution recording resolution to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setResolution(@NonNull Resolution resolution);

        /**
         * Retrieves the recording framerates supported <i>in the current mode and resolution</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported recording framerates
         *
         * @see #supportedFrameratesFor(CameraRecording.Resolution)
         * @see #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution)
         */
        @NonNull
        public abstract EnumSet<Framerate> supportedFramerates();

        /**
         * Retrieves the recording framerates supported <i>in the current mode and a given resolution</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param resolution resolution to query supported framerates of
         *
         * @return supported recording framerates for this resolution in the current mode
         *
         * @see #supportedFramerates()
         * @see #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution)
         */
        @NonNull
        public abstract EnumSet<Framerate> supportedFrameratesFor(@NonNull Resolution resolution);

        /**
         * Retrieves the recording framerates supported <i>in a given mode and a given resolution</i>.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @param mode       mode to query supported framerates of
         * @param resolution resolution to query supported framerates of
         *
         * @return supported recording framerates for those mode and resolution
         *
         * @see #supportedFramerates()
         * @see #supportedFrameratesFor(CameraRecording.Resolution)
         */
        @NonNull
        public abstract EnumSet<Framerate> supportedFrameratesFor(@NonNull Mode mode, @NonNull Resolution resolution);

        /**
         * Retrieves the currently applied recording resolution.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedFramerates() supported framerates for the current mode and resolution} is empty.
         *
         * @return recording framerate
         */
        @NonNull
        public abstract Framerate framerate();

        /**
         * Sets the recording framerate to be applied in the current mode and resolution.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedFramerates() supported framerates for the current mode and resolution}, otherwise this
         * method does nothing.
         *
         * @param framerate recording framerate to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setFramerate(@NonNull Framerate framerate);

        /**
         * Tells whether HDR is available in the current mode, resolution and framerate.
         *
         * @return {@code true} if HDR is available in the current mode, resolution and framerate,
         *         otherwise {@code false}
         */
        public abstract boolean isHdrAvailable();

        /**
         * Tells whether HDR is available for specific recording mode, resolution and framerate.
         *
         * @param mode       recording mode to test
         * @param resolution recording resolution to test
         * @param framerate  recording framerate to test
         *
         * @return {@code true} if HDR is available in the given recording mode, resolution and framerate,
         *         otherwise {@code false}
         */
        public abstract boolean isHdrAvailable(@NonNull CameraRecording.Mode mode,
                                               @NonNull CameraRecording.Resolution resolution,
                                               @NonNull CameraRecording.Framerate framerate);

        /**
         * Retrieves the currently supported hyperlapse values for use in {@link Mode#HYPERLAPSE} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported hyperlapse values
         */
        @NonNull
        public abstract EnumSet<HyperlapseValue> supportedHyperlapseValues();

        /**
         * Retrieves the hyperlapse value applied in {@link Mode#HYPERLAPSE} mode.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedHyperlapseValues() supported hyperlapse values} is empty.
         *
         * @return hyperlapse value
         */
        @NonNull
        public abstract HyperlapseValue hyperlapseValue();

        /**
         * Retrieves the recording bitrate of the current configuration.
         * <p>
         *
         * @return recording bitrate in bit/s, zero if unknown.
         */
        @IntRange(from = 0)
        public abstract int bitrate();

        /**
         * Sets the hyperlapse value to be applied in {@link Mode#HYPERLAPSE} mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedHyperlapseValues()}  supported hyperlapse values}, otherwise this method does nothing.
         *
         * @param hyperlapse hyperlapse value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setHyperlapseValue(@NonNull HyperlapseValue hyperlapse);

        /**
         * Switches to {@link Mode#STANDARD standard} mode and applies the given recording resolution and framerate at
         * the same time.
         * <p>
         * {@link Mode#STANDARD} mode must be present in the set of {@link #supportedModes() supported modes}; provided
         * resolution must be present in the set of
         * {@link #supportedResolutionsFor(CameraRecording.Mode) supported resolution for that mode}; provided
         * framerate must be present in the set of
         * {@link #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution) supported framerate for
         * those mode and resolution}, otherwise this method does nothing.
         *
         * @param resolution recording resolution to set
         * @param framerate  recording framerate to set
         */
        public abstract void setStandardMode(@NonNull Resolution resolution, @NonNull Framerate framerate);

        /**
         * Switches to {@link Mode#HYPERLAPSE hyperlapes} mode and applies the given recording resolution, framerate
         * and hyperlapse value at the same time.
         * <p>
         * {@link Mode#HYPERLAPSE} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided resolution must be present in the set of
         * {@link #supportedResolutionsFor(CameraRecording.Mode) supported resolution for that mode}; provided
         * framerate must be present in the set of
         * {@link #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution) supported framerate for
         * those mode and resolution}; provided hyperlapse value must be present in the set of
         * {@link #supportedHyperlapseValues() supported hyperlapse values}, otherwise this method does nothing.
         *
         * @param resolution recording resolution to set
         * @param framerate  recording framerate to set
         * @param hyperlapse hyperlapse value to set
         */
        public abstract void setHyperlapseMode(@NonNull Resolution resolution, @NonNull Framerate framerate,
                                               @NonNull HyperlapseValue hyperlapse);

        /**
         * Switches to {@link Mode#SLOW_MOTION slow motion} mode and applies the given recording resolution and
         * framerate at the same time.
         * <p>
         * {@link Mode#SLOW_MOTION} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided resolution must be present in the set of
         * {@link #supportedResolutionsFor(CameraRecording.Mode) supported resolution for that mode}; provided
         * framerate must be present in the set of
         * {@link #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution) supported framerate for
         * those mode and resolution}, otherwise this method does nothing.
         *
         * @param resolution recording resolution to set
         * @param framerate  recording framerate to set
         */
        public abstract void setSlowMotionMode(@NonNull Resolution resolution, @NonNull Framerate framerate);

        /**
         * Switches to {@link Mode#HIGH_FRAMERATE high framerate} mode and applies the given recording resolution and
         * framerate at the same time.
         * <p>
         * {@link Mode#HIGH_FRAMERATE} mode must be present in the set of {@link #supportedModes() supported modes};
         * provided resolution must be present in the set of
         * {@link #supportedResolutionsFor(CameraRecording.Mode) supported resolution for that mode}; provided
         * framerate must be present in the set of
         * {@link #supportedFrameratesFor(CameraRecording.Mode, CameraRecording.Resolution) supported framerate for
         * those mode and resolution}, otherwise this method does nothing.
         *
         * @param resolution recording resolution to set
         * @param framerate  recording framerate to set
         */
        public abstract void setHighFramerateMode(@NonNull Resolution resolution, @NonNull Framerate framerate);
    }

    /**
     * Provides the state of the camera video record function, access to the identifier of the latest saved video
     * media, and allows to keep track of the start time and duration of a video record while the camera is in the
     * process of  recording a video.
     */
    public interface State {

        /** Camera video record function state. */
        enum FunctionState {

            /**
             * Camera video record function is inoperable at present.
             * <p>
             * This state is entered from any other state: <ul>
             * <li>after disconnection from the drone,</li>
             * <li>while connected, if the video record function becomes inoperable for some reason.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()}  latest saved media id} is reset to {@code null},</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>after connection to the drone, if the video record function is operable currently; state will
             * change to either {@link #STOPPED} if the camera is not currently recording a video, or
             * {@link #STARTED} if the camera is currently recording a video,</li>
             * <li>while connected, if the video record function becomes operable; state will change to
             * {@link #STOPPED}.</li>
             * </ul>
             */
            UNAVAILABLE,

            /**
             * Video record is stopped and ready to be started.
             * <p>
             * This state is entered: <ul>
             * <li>after connection to the drone, in case the video record function is ready to be started,</li>
             * <li>from {@link #UNAVAILABLE} state, when the video record function becomes operable,</li>
             * <li>from {@link #STOPPING} state, when the camera is done recording and saving the video to drone
             * storage,</li>
             * <li>from {@code #ERROR_*} states, immediately after any of those changes has been notified.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} contains the identifier of the latest saved
             * video record, iff available and transiting from either{@link #STOPPING},
             * {@link #ERROR_INSUFFICIENT_STORAGE_SPACE} or {@link #ERROR_INSUFFICIENT_STORAGE_SPEED} states;
             * otherwise it is {@code null},</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>after a call to {@link Camera#startRecording()}; state will change to {@link #STARTING},</li>
             * <li>when the video record function becomes inoperable; state will change to
             * {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STOPPED,

            /**
             * Video record is starting.
             * <p>
             * This state is entered from {@link #STOPPED} after a call to {@link Camera#startRecording()}.
             * <p>
             * In this state: <ul>
             * <li>{@link #latestMediaId()}  latest saved media id} is reset to {@code null},</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>when the video eventually starts being recorded; state will change to {@link #STARTED},</li>
             * <li>after a call to {@link Camera#stopRecording()}; state will change to {@link #STOPPING},</li>
             * <li>if recording could not start for some reason; state will change to some {@code ERROR_*} state,
             * depending on the reason,</li>
             * <li>when the video record function becomes inoperable; state will change to
             * {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STARTING,

            /**
             * Camera is currently recording a video.
             * <p>
             * This state is entered: <ul>
             * <li>after connection to the drone, if the camera is currently recording a video,</li>
             * <li>from {@link #STARTING}, when the video eventually starts being recorded.</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()}  latest saved media id} is reset to {@code null},</li>
             * <li>{@link #recordStartTime()} reports the date and time at which the recording started,</li>
             * <li>calling {@link #recordDuration()} reports the current record duration.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>after a call to {@link Camera#stopRecording()}; state will change to {@link #STOPPING},</li>
             * <li>if recording must stop for some reason; state will change to some {@code ERROR_*} state,
             * depending on the reason,</li>
             * <li>when the video record function becomes inoperable; state will change to
             * {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STARTED,

            /**
             * Video record is stopping.
             * <p>
             * This state is entered from {@link #STARTED} or {@link #STARTING}, after a call to
             * {@link Camera#stopRecording()}.
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited: <ul>
             * <li>when the video has stopped being recorded and has been saved to drone storage; state will
             * change to {@link #STOPPED},</li>
             * <li>if an error occurs while the recording process is being stopped; state will change to
             * {@link #ERROR_INTERNAL},</li>
             * <li>when the video record function becomes inoperable; state will change to
             * {@link #UNAVAILABLE}.</li>
             * </ul>
             */
            STOPPING,

            /**
             * Video record is about to stop because some video settings changed.
             * <p>
             * This state is entered from {@link #STARTED}, if the application modifies video settings that require
             * video record to stop so that camera configuration change can be applied.
             * <p>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} contains the identifier of the saved
             * video record,</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited immediately after having been notified; state will change to either {@link #STOPPED}
             * or {@link #UNAVAILABLE}
             */
            CONFIGURATION_CHANGE,

            /**
             * Video record has stopped due to insufficient storage space on the drone.
             * <p>
             * This state is entered: <ul>
             * <li>from {@link #STARTING} if there is not enough available storage space on the drone to
             * start recording a video,</li>
             * <li>from {@link #STARTED}, if there is not enough available storage space on the drone to
             * further record the ongoing video,</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} contains the identifier of the saved
             * video record,</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited immediately after having been notified; state will change to either {@link #STOPPED}
             * or {@link #UNAVAILABLE}.
             */
            ERROR_INSUFFICIENT_STORAGE_SPACE,

            /**
             * Video record has stopped due to insufficient storage speed on the drone.
             * <p>
             * This state is entered: <ul>
             * <li>from {@link #STARTING} if drone storage is too slow to start recording a video,</li>
             * <li>from {@link #STARTED}, if drone storage became too slow to further record the ongoing video,</li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} contains the identifier of the saved
             * video record,</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * This state is exited immediately after having been notified; state will change to either {@link #STOPPED}
             * or {@link #UNAVAILABLE}.
             */
            ERROR_INSUFFICIENT_STORAGE_SPEED,

            /**
             * Video record has stopped due to an internal, undocumented error
             * <p>
             * This state is entered: <ul>
             * <li>from {@link #STARTING} if the video record could not start due to an internal error, </li>
             * <li>from {@link #STARTED}, if the video stopped being recorded due to an internal error, </li>
             * <li>from {@link #STOPPING}, if the video could not be recorded and saved due to an internal
             * error. </li>
             * </ul>
             * In this state: <ul>
             * <li>{@link #latestMediaId()} latest saved media id} is reset to {@code null},</li>
             * <li>{@link #recordStartTime()} is reset to {@code null},</li>
             * <li>{@link #recordDuration()} reports {@code 0}.</li>
             * </ul>
             * Warning: this state can be temporary, and can be quickly followed by the state {@link #STOPPED} or
             * {@link #UNAVAILABLE}.
             */
            ERROR_INTERNAL
        }

        /**
         * Retrieves the current camera video record function state.
         *
         * @return current camera video record function state
         */
        @NonNull
        FunctionState get();

        /**
         * Retrieves the identifier of the latest saved video media since connection.
         * <p>
         * {@code null} unless {@link #get()} state} is either {@link FunctionState#STOPPED},
         * {@link FunctionState#ERROR_INSUFFICIENT_STORAGE_SPACE} or
         * {@link FunctionState#ERROR_INSUFFICIENT_STORAGE_SPEED} and some video was recorded beforehand since the
         * application is connected to the drone.
         *
         * @return saved media identifier if available, otherwise {@code null}
         */
        @Nullable
        String latestMediaId();

        /**
         * Retrieves the date and time at which the video record started.
         * <p>
         * {@code null} unless {@link #get()} state} is {@link FunctionState#STARTED}.
         *
         * @return current video record start date and time
         */
        @Nullable
        Date recordStartTime();

        /**
         * Computes current video record duration, in milliseconds
         * <p>
         * This is only a helper method that computes the duration based on the record start time
         * and the current system time, it is not updated regularly and subject to change notifications on the
         * camera component.
         *
         * @return the current record duration, iff {@link #recordStartTime() record start time} is not {@code null},
         *         otherwise {@code 0}
         */
        @IntRange(from = 0)
        long recordDuration();
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraRecording() {
    }
}
