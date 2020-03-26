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

package com.parrot.drone.groundsdk.mavlink;

import android.util.SparseArray;

import com.parrot.drone.groundsdk.internal.Logging;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A MAVLink command.
 * <p>
 * Clients of this API cannot instantiate this class directly, and must use one of the subclasses defining a specific
 * MAVLink command.
 */
public abstract class MavlinkCommand {

    /** MAVLink command type. */
    enum Type {

        /** Navigate to waypoint. */
        NAVIGATE_TO_WAYPOINT(16),

        /** Return to home. */
        RETURN_TO_LAUNCH(20),

        /** Land. */
        LAND(21),

        /** Take off. */
        TAKE_OFF(22),

        /** Delay the next command. */
        DELAY(112),

        /** Change speed. */
        CHANGE_SPEED(178),

        /** Set Region Of Interest. */
        SET_ROI(201),

        /** Control the camera tilt. */
        MOUNT_CONTROL(205),

        /** Start photo capture. */
        START_PHOTO_CAPTURE(2000),

        /** Stop photo capture. */
        STOP_PHOTO_CAPTURE(2001),

        /** Start video recording. */
        START_VIDEO_CAPTURE(2500),

        /** Stop video recording. */
        STOP_VIDEO_CAPTURE(2501),

        /** Create a panorama. */
        CREATE_PANORAMA(2800),

        /** Set view mode. */
        SET_VIEW_MODE(50000),

        /** Set still capture mode. */
        SET_STILL_CAPTURE_MODE(50001);

        /** Command code. */
        private final int mCode;

        /**
         * Constructor.
         *
         * @param code command code
         */
        Type(int code) {
            mCode = code;
        }

        /**
         * Retrieves the command code.
         *
         * @return command code
         */
        int code() {
            return mCode;
        }

        /**
         * Retrieves a {@code Type} from its code.
         *
         * @param code command code
         *
         * @return {@code Type} matching code, or {@code null} if no one matches
         */
        @Nullable
        static Type fromCode(int code) {
            return MAP.get(code);
        }

        /** Map of Types, by their code. */
        private static final SparseArray<Type> MAP;

        static {
            MAP = new SparseArray<>();
            for (Type type : values()) {
                MAP.put(type.mCode, type);
            }
        }
    }

    /** Value always used for current waypoint; set to false. */
    private static final int CURRENT_WAYPOINT = 0;

    /** Value always used for coordinate frame; set to global coordinate frame, relative altitude over ground. */
    private static final int FRAME = 3;

    /** Value always used for auto-continue; set to true. */
    private static final int AUTO_CONTINUE = 1;

    /** The MAVLink command type. */
    @NonNull
    protected final Type mType;

    /**
     * Constructor.
     *
     * @param type the MAVLink command type
     */
    MavlinkCommand(@NonNull Type type) {
        mType = type;
    }

    /**
     * Writes the MAVLink command to the given writer.
     * <p>
     * This is the default implementation for commands with no parameter. Subclasses should override this method to add
     * command specific parameters.
     *
     * @param writer the writer to which the command is written
     * @param index  the index of the command
     *
     * @throws IOException if the command could not be written
     */
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Writes the MAVLink command to the given writer.
     *
     * @param writer    the writer to which the command is written
     * @param index     the index of the command
     * @param param1    first parameter of the command, type dependant
     * @param param2    second parameter of the command, type dependant
     * @param param3    third parameter of the command, type dependant
     * @param param4    fourth parameter of the command, type dependant
     * @param latitude  the latitude of the command
     * @param longitude the longitude of the command
     * @param altitude  the altitude of the command
     *
     * @throws IOException if the command could not be written
     */
    void write(@NonNull Writer writer, int index, double param1, double param2, double param3, double param4,
               double latitude, double longitude, double altitude) throws IOException {
        writer.write(String.format(Locale.US, "%d\t%d\t%d\t%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%d\n",
                index, CURRENT_WAYPOINT, FRAME, mType.code(), param1, param2, param3, param4, latitude,
                longitude, altitude, AUTO_CONTINUE));
    }

    /**
     * Parses a line of a MAVLink file.
     *
     * @param line line of MAVLink file
     *
     * @return MAVLink command, or {@code null} if the line could not be parsed
     */
    @Nullable
    static MavlinkCommand parse(@NonNull String line) {
        MavlinkCommand command = null;
        String[] tokens = line.split("\\t");
        if (tokens.length == 12) {
            try {
                Type type = Type.fromCode(Integer.parseInt(tokens[3]));
                if (type != null) {
                    double[] parameters = new double[7];
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = Double.parseDouble(tokens[i + 4]);
                    }
                    switch (type) {
                        case NAVIGATE_TO_WAYPOINT:
                            command = NavigateToWaypointCommand.create(parameters);
                            break;
                        case RETURN_TO_LAUNCH:
                            command = new ReturnToLaunchCommand();
                            break;
                        case LAND:
                            command = new LandCommand();
                            break;
                        case TAKE_OFF:
                            command = new TakeOffCommand();
                            break;
                        case DELAY:
                            command = DelayCommand.create(parameters);
                            break;
                        case CHANGE_SPEED:
                            command = ChangeSpeedCommand.create(parameters);
                            break;
                        case SET_ROI:
                            command = SetRoiCommand.create(parameters);
                            break;
                        case MOUNT_CONTROL:
                            command = MountControlCommand.create(parameters);
                            break;
                        case START_PHOTO_CAPTURE:
                            command = StartPhotoCaptureCommand.create(parameters);
                            break;
                        case STOP_PHOTO_CAPTURE:
                            command = new StopPhotoCaptureCommand();
                            break;
                        case START_VIDEO_CAPTURE:
                            command = new StartVideoCaptureCommand();
                            break;
                        case STOP_VIDEO_CAPTURE:
                            command = new StopVideoCaptureCommand();
                            break;
                        case CREATE_PANORAMA:
                            command = CreatePanoramaCommand.create(parameters);
                            break;
                        case SET_VIEW_MODE:
                            command = SetViewModeCommand.create(parameters);
                            break;
                        case SET_STILL_CAPTURE_MODE:
                            command = SetStillCaptureModeCommand.create(parameters);
                            break;
                    }
                }
            } catch (NumberFormatException e) {
                ULog.e(Logging.TAG_MAVLINK, "Error parsing MAVLink file, ignoring line", e);
                command = null;
            }
        }
        return command;
    }
}
