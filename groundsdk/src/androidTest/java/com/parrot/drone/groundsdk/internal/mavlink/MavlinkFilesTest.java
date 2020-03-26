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

package com.parrot.drone.groundsdk.internal.mavlink;

import com.parrot.drone.groundsdk.mavlink.ChangeSpeedCommand;
import com.parrot.drone.groundsdk.mavlink.CreatePanoramaCommand;
import com.parrot.drone.groundsdk.mavlink.DelayCommand;
import com.parrot.drone.groundsdk.mavlink.LandCommand;
import com.parrot.drone.groundsdk.mavlink.MavlinkCommand;
import com.parrot.drone.groundsdk.mavlink.MavlinkFiles;
import com.parrot.drone.groundsdk.mavlink.MountControlCommand;
import com.parrot.drone.groundsdk.mavlink.NavigateToWaypointCommand;
import com.parrot.drone.groundsdk.mavlink.ReturnToLaunchCommand;
import com.parrot.drone.groundsdk.mavlink.SetRoiCommand;
import com.parrot.drone.groundsdk.mavlink.SetStillCaptureModeCommand;
import com.parrot.drone.groundsdk.mavlink.SetViewModeCommand;
import com.parrot.drone.groundsdk.mavlink.StartPhotoCaptureCommand;
import com.parrot.drone.groundsdk.mavlink.StartVideoCaptureCommand;
import com.parrot.drone.groundsdk.mavlink.StopPhotoCaptureCommand;
import com.parrot.drone.groundsdk.mavlink.StopVideoCaptureCommand;
import com.parrot.drone.groundsdk.mavlink.TakeOffCommand;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.parrot.drone.groundsdk.mavlink.ChangeSpeedCommandMatcher.changeSpeedCommandIs;
import static com.parrot.drone.groundsdk.mavlink.CreatePanoramaCommandMatcher.createPanoramaCommandIs;
import static com.parrot.drone.groundsdk.mavlink.DelayCommandMatcher.delayCommandIs;
import static com.parrot.drone.groundsdk.mavlink.MountControlCommandMatcher.mountControlCommandIs;
import static com.parrot.drone.groundsdk.mavlink.NavigateToWaypointCommandMatcher.navigateToWaypointCommandIs;
import static com.parrot.drone.groundsdk.mavlink.SetRoiCommandMatcher.setRoiCommandIs;
import static com.parrot.drone.groundsdk.mavlink.SetStillCaptureModeCommandMatcher.setStillCaptureModeCommandIs;
import static com.parrot.drone.groundsdk.mavlink.SetViewModeCommandMatcher.setViewModeCommandIs;
import static com.parrot.drone.groundsdk.mavlink.StartPhotoCaptureCommandMatcher.startPhotoCaptureCommandIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class MavlinkFilesTest {

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Test
    public void testGenerate() {
        File file = new File(mTemporaryFolder.getRoot(), "mavlink.txt");

        List<MavlinkCommand> commands = new ArrayList<>();
        commands.add(new NavigateToWaypointCommand(48.8, 2.3, 3, 45, 0, 5));
        commands.add(new ReturnToLaunchCommand());
        commands.add(new LandCommand());
        commands.add(new TakeOffCommand());
        commands.add(new DelayCommand(3.5));
        commands.add(new ChangeSpeedCommand(ChangeSpeedCommand.SpeedType.GROUND_SPEED, 7.5));
        commands.add(new SetRoiCommand(48.9, 2.4, 5.3));
        commands.add(new MountControlCommand(45.0));
        commands.add(new StartPhotoCaptureCommand(3.5, 5, StartPhotoCaptureCommand.Format.RECTILINEAR));
        commands.add(new StopPhotoCaptureCommand());
        commands.add(new StartVideoCaptureCommand());
        commands.add(new StopVideoCaptureCommand());
        commands.add(new CreatePanoramaCommand(2, 3, 4, 6));
        commands.add(new SetViewModeCommand(SetViewModeCommand.Mode.ROI, 7));
        commands.add(new SetStillCaptureModeCommand(SetStillCaptureModeCommand.Mode.GPSLAPSE, 4.5));

        MavlinkFiles.generate(file, commands);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            assertThat(reader.readLine(), equalTo("QGC WPL 120"));
            assertThat(reader.readLine(),
                    equalTo("0\t0\t3\t16\t0.000000\t5.000000\t0.000000\t45.000000\t48.800000\t2.300000\t3.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("1\t0\t3\t20\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("2\t0\t3\t21\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("3\t0\t3\t22\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("4\t0\t3\t112\t3.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("5\t0\t3\t178\t1.000000\t7.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("6\t0\t3\t201\t3.000000\t0.000000\t0.000000\t0.000000\t48.900000\t2.400000\t5.300000\t1"));
            assertThat(reader.readLine(),
                    equalTo("7\t0\t3\t205\t45.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t2.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("8\t0\t3\t2000\t3.500000\t5.000000\t12.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("9\t0\t3\t2001\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("10\t0\t3\t2500\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("11\t0\t3\t2501\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("12\t0\t3\t2800\t2.000000\t4.000000\t3.000000\t6.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("13\t0\t3\t50000\t2.000000\t7.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
            assertThat(reader.readLine(),
                    equalTo("14\t0\t3\t50001\t1.000000\t4.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1"));
        } catch (IOException e) {
            throw new AssertionError("Error reading generated file", e);
        }
    }

    @Test
    public void testParse() {
        File file = new File(mTemporaryFolder.getRoot(), "mavlink.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("QGC WPL 120\n");
            writer.write("0\t0\t3\t16\t0.000000\t5.000000\t0.000000\t45.000000\t48.800000\t2.300000\t3.000000\t1\n");
            writer.write("1\t0\t3\t20\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("2\t0\t3\t21\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("3\t0\t3\t22\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("4\t0\t3\t112\t3.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("5\t0\t3\t178\t1.000000\t7.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("6\t0\t3\t201\t3.000000\t0.000000\t0.000000\t0.000000\t48.900000\t2.400000\t5.300000\t1\n");
            writer.write("7\t0\t3\t205\t45.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t2.000000\t1\n");
            writer.write("8\t0\t3\t2000\t3.500000\t5.000000\t12.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("9\t0\t3\t2001\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("10\t0\t3\t2500\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("11\t0\t3\t2501\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("12\t0\t3\t2800\t2.000000\t4.000000\t3.000000\t6.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("13\t0\t3\t50000\t2.000000\t7.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
            writer.write("14\t0\t3\t50001\t1.000000\t4.500000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n");
        } catch (IOException e) {
            throw new AssertionError("Error writing MAVLink file", e);
        }

        List<MavlinkCommand> commands = MavlinkFiles.parse(file);

        assertThat(commands.get(0), instanceOf(NavigateToWaypointCommand.class));
        assertThat((NavigateToWaypointCommand) commands.get(0), navigateToWaypointCommandIs(48.8, 2.3, 3, 45, 0, 5));
        assertThat(commands.get(1), instanceOf(ReturnToLaunchCommand.class));
        assertThat(commands.get(2), instanceOf(LandCommand.class));
        assertThat(commands.get(3), instanceOf(TakeOffCommand.class));
        assertThat(commands.get(4), instanceOf(DelayCommand.class));
        assertThat((DelayCommand) commands.get(4), delayCommandIs(3.5));
        assertThat(commands.get(5), instanceOf(ChangeSpeedCommand.class));
        assertThat((ChangeSpeedCommand) commands.get(5),
                changeSpeedCommandIs(ChangeSpeedCommand.SpeedType.GROUND_SPEED, 7.5));
        assertThat(commands.get(6), instanceOf(SetRoiCommand.class));
        assertThat((SetRoiCommand) commands.get(6), setRoiCommandIs(48.9, 2.4, 5.3));
        assertThat(commands.get(7), instanceOf(MountControlCommand.class));
        assertThat((MountControlCommand) commands.get(7), mountControlCommandIs(45));
        assertThat(commands.get(8), instanceOf(StartPhotoCaptureCommand.class));
        assertThat((StartPhotoCaptureCommand) commands.get(8),
                startPhotoCaptureCommandIs(3.5, 5, StartPhotoCaptureCommand.Format.RECTILINEAR));
        assertThat(commands.get(9), instanceOf(StopPhotoCaptureCommand.class));
        assertThat(commands.get(10), instanceOf(StartVideoCaptureCommand.class));
        assertThat(commands.get(11), instanceOf(StopVideoCaptureCommand.class));
        assertThat(commands.get(12), instanceOf(CreatePanoramaCommand.class));
        assertThat((CreatePanoramaCommand) commands.get(12), createPanoramaCommandIs(2, 3, 4, 6));
        assertThat(commands.get(13), instanceOf(SetViewModeCommand.class));
        assertThat((SetViewModeCommand) commands.get(13), setViewModeCommandIs(SetViewModeCommand.Mode.ROI, 7));
        assertThat(commands.get(14), instanceOf(SetStillCaptureModeCommand.class));
        assertThat((SetStillCaptureModeCommand) commands.get(14),
                setStillCaptureModeCommandIs(SetStillCaptureModeCommand.Mode.GPSLAPSE, 4.5));
    }
}
