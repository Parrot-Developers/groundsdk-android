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

import com.parrot.drone.groundsdk.internal.Logging;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Utility class that provides methods to generate a MAVLink file from a list of
 * {@link MavlinkCommand MAVLink commands}, and conversely, parse a MAVLink file.
 * <p>
 * A MAVLink file contains a list of commands in a plain-text format, which form a mission script. Note that supported
 * MAVLink commands differ from official
 * <a href="https://mavlink.io/en/messages/common.html">MAVLink common message set</a>.
 * For further information about supported MAVLink commands, please refer to
 * <a href="https://developer.parrot.com/docs/mavlink-flightplan">Parrot FlightPlan MAVLink documentation</a>.
 */
public final class MavlinkFiles {

    /**
     * Generates a MAVLink file from the given list of commands.
     *
     * @param file     destination file path
     * @param commands iterable over MAVLink commands
     */
    public static void generate(@NonNull File file, @NonNull Iterable<MavlinkCommand> commands) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("QGC WPL 120\n");
            int index = 0;
            for (MavlinkCommand item : commands) {
                item.write(writer, index);
                index++;
            }
        } catch (IOException e) {
            ULog.e(Logging.TAG_MAVLINK, "Could not generate MAVLink file", e);
        }
    }

    /**
     * Parses a MAVLink file into a list of commands.
     * <p>
     * Any malformed command is simply ignored. If the given file is not properly formatted, this method returns an
     * empty list.
     *
     * @param file source file path
     *
     * @return the command list extracted from the file
     */
    @NonNull
    public static List<MavlinkCommand> parse(@NonNull File file) {
        List<MavlinkCommand> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && line.matches("QGC WPL \\d+")) {
                while ((line = reader.readLine()) != null) {
                    MavlinkCommand command = MavlinkCommand.parse(line);
                    if (command != null) {
                        commands.add(command);
                    }
                }
            }
        } catch (IOException e) {
            ULog.e(Logging.TAG_MAVLINK, "Could not parse MAVLink file", e);
        }
        return commands;
    }

    /**
     * Private constructor for static utility class.
     */
    private MavlinkFiles() {
    }
}
