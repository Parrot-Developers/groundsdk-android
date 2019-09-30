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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.flightdata;

import android.content.Context;

import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.parrot.drone.groundsdk.arsdkengine.test.R;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class PudAdapterTests {

    @Test
    public void testNonJsonHeader() {
        InputStream input = new ByteArrayInputStream("Not some JSON".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Malformed JSON header"));
        }
    }

    @Test
    public void testNonJsonObjectHeader() {
        InputStream input = new ByteArrayInputStream("[]".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Malformed JSON header"));
        }
    }

    @Test
    public void testMissingNullSeparator() {
        InputStream input = new ByteArrayInputStream("{foo: 1}\1".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Malformed JSON header"));
        }
    }

    @Test
    public void testInvalidColumnDescription() {
        InputStream input = new ByteArrayInputStream("{details_headers: {}}\0".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Malformed columns description"));
        }
        input = new ByteArrayInputStream("{details_headers: null}\0".getBytes());
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Empty columns description"));
        }
        input = new ByteArrayInputStream("{details_headers: []}\0".getBytes());
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Empty columns description"));
        }
    }

    @Test
    public void testNullColumnDescriptor() {
        InputStream input = new ByteArrayInputStream("{details_headers : [null]}\0".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Null column 0 descriptor"));
        }
    }

    @Test
    public void testInvalidColumnDescriptorSize() {
        InputStream input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: \"foo\",\n"
                + "    type: \"string\",\n"
                + "    size: -1\n"
                + "  }\n"
                + "]}\0").getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Invalid column 0 descriptor size"));
        }

        input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: \"foo\",\n"
                + "    type: \"string\"\n"
                + "  }\n"
                + "]}\0").getBytes());
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Invalid column 0 descriptor size"));
        }
    }

    @Test
    public void testUnnamedColumnDescriptor() {
        InputStream input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: null,\n"
                + "    type: \"string\",\n"
                + "    size: 4\n"
                + "  }\n"
                + "]}\0").getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Invalid column 0 descriptor name"));
        }

        input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: \"\",\n"
                + "    type: \"string\",\n"
                + "    size: 4\n"
                + "  }\n"
                + "]}\0").getBytes());
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Invalid column 0 descriptor name"));
        }

        input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    size: 4,\n"
                + "    type: \"string\"\n"
                + "  }\n"
                + "]}\0").getBytes());
        try {
            PudAdapter.adapt(input, output);
            throw new AssertionError();
        } catch (IOException e) {
            assertThat(e.getMessage(), is("Invalid column 0 descriptor name"));
        }
    }

    @Test
    public void testColumnDescriptorUnknownType() {
        InputStream input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: \"foo\",\n"
                + "    type: \"bar\",\n"
                + "    size: 4\n"
                + "  }\n"
                + "]}\0").getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            for (int i = 0; i < columns.length(); i++) {
                assertThat(columns.get(i), not("foo"));
            }
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }


    @Test
    public void testSpeedColumn() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"speed_vx\",\n"
                         + "    type: \"float\",\n"
                         + "    size: 4\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"speed_vy\",\n"
                         + "    type: \"float\",\n"
                         + "    size: 4\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"speed_vz\",\n"
                         + "    type: \"float\",\n"
                         + "    size: 4\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        float vx = 2;
        float vy = 3;
        float vz = 4;
        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 3 * 4])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header).putFloat(vx).putFloat(vy).putFloat(vz)
                          .array());

        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("speed_vx"));
            assertThat(columns.get(1), is("speed_vy"));
            assertThat(columns.get(2), is("speed_vz"));
            assertThat(columns.get(3), is("speed"));
            JSONArray dataLine0 = new JSONObject(output.toString()).getJSONArray("details_data").getJSONArray(0);
            assertThat((float) dataLine0.getDouble(0), is(vx));
            assertThat((float) dataLine0.getDouble(1), is(vy));
            assertThat((float) dataLine0.getDouble(2), is(vz));
            assertThat((float) dataLine0.getDouble(3),
                    is((float) Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2) + Math.pow(vz, 2))));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testTime() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"time\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 4\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        int time1 = 1000;
        int time2 = 2000;

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 2 * 4])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header).putInt(time1).putInt(time2)
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("time"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.getJSONArray(0).getInt(0), is(time1));
            assertThat(data.getJSONArray(1).getInt(0), is(time2));

            int time = new JSONObject(output.toString()).getInt("total_run_time");
            assertThat(time, is(time2));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testIncoherentTime() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"time\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 4\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        int time1 = 2000;
        int time2 = 1000; // second time 'before' first one

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 2 * 4])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header).putInt(time1).putInt(time2)
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("time"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(1)); // only one line, second should have been dropped
            assertThat(data.getJSONArray(0).getInt(0), is(time1));

            int time = new JSONObject(output.toString()).getInt("total_run_time");
            assertThat(time, is(time1));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testTooLargeTimeGap() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"time\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 4\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        int time1 = 1000;
        int time2 = time1 + (int) PudAdapter.MAX_TIME_INTERVAL + 1; // second time too long after first one

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 2 * 4])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header).putInt(time1).putInt(time2)
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("time"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(1)); // only one line, second should have been dropped
            assertThat(data.getJSONArray(0).getInt(0), is(time1));

            int time = new JSONObject(output.toString()).getInt("total_run_time");
            assertThat(time, is(time1));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testAlertCount() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"alert_state\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 1\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        byte[] alerts = new byte[] {
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.CUT_OUT.value, // counts 1
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.CRITICAL_BATTERY.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.TOO_MUCH_ANGLE.value, // counts 1
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.TOO_MUCH_ANGLE.value, // does not count
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.LOW_BATTERY.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE.value,
                (byte) 0x7f, // unknown value, does not count
                (byte) ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.USER.value // counts 1
        };
        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + alerts.length])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header).put(alerts)
                          .array());

        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("alert_state"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(alerts.length));
            for (int i = 0; i < alerts.length; i++) {
                assertThat(data.getJSONArray(i).getInt(0), is((int) alerts[i]));
            }
            int alertCount = new JSONObject(output.toString()).getInt("crash");
            assertThat(alertCount, is(3));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testFlyingTime() {
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"time\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 4\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"flying_state\",\n"
                         + "    type: \"integer\",\n"
                         + "    size: 1\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        byte[] flyingStates = new byte[] {
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.MOTOR_RAMPING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.TAKINGOFF.value, // flight1 start@3000
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING.value,
                (byte) 0x7f, // unknown value
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY_LANDING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED.value, // flight1 stop@12000
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.TAKINGOFF.value, // flight2 start@14000
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING.value,
                (byte) ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING.value,
                // flight2 stop never recorded, should use latest time as stop value: 16000
        };

        ByteBuffer binary = ByteBuffer.wrap(new byte[header.length + flyingStates.length * (4 + 1)])
                                      .order(ByteOrder.LITTLE_ENDIAN)
                                      .put(header);
        int time = 0;
        for (byte flyingState : flyingStates) {
            time += 1000;
            binary.putInt(time).put(flyingState);
        }

        InputStream input = new ByteArrayInputStream(binary.array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("time"));
            assertThat(columns.get(1), is("flying_state"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(flyingStates.length));
            int expectedTime = 0;
            for (int i = 0; i < flyingStates.length; i++) {
                expectedTime += 1000;
                assertThat(data.getJSONArray(i).getInt(0), is(expectedTime));
                assertThat(data.getJSONArray(i).getInt(1), is((int) flyingStates[i]));
            }
            int totalTime = new JSONObject(output.toString()).getInt("total_run_time");
            assertThat(totalTime, is(16000));
            int flightTime = new JSONObject(output.toString()).getInt("run_time");
            assertThat(flightTime, is((12000 - 3000) + (16000 - 14000)));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testGpsAvailable() {
        // first test that gps_available is false if the column is missing
        InputStream input = new ByteArrayInputStream((
                "{details_headers : [\n"
                + "  {\n"
                + "    name: foo,\n"
                + "    type: \"bar\",\n"
                + "    size: 1\n"
                + "  }\n"
                + "]}\0").getBytes());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            assertThat(new JSONObject(output.toString()).getBoolean("gps_available"), is(false));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        // test that when column is present but never true, then gps_available is false
        byte[] header = (
                "{details_headers : [\n"
                + "  {\n"
                + "    name: product_gps_available,\n"
                + "    type: \"boolean\",\n"
                + "    size: 1\n"
                + "  }\n"
                + "]}\0").getBytes();

        //noinspection PointlessArithmeticExpression
        input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 4 * 1])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          .put(new byte[] {0, 0, 0, 0}) // false, false, false, false
                          .array());
        output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            assertThat(new JSONObject(output.toString()).getBoolean("gps_available"), is(false));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        // test that one product_gps_available = true value suffice to make gps_available true
        header = (
                "{details_headers : [\n"
                + "  {\n"
                + "    name: product_gps_available,\n"
                + "    type: \"boolean\",\n"
                + "    size: 1\n"
                + "  }\n"
                + "]}\0").getBytes();

        //noinspection PointlessArithmeticExpression
        input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + 4 * 1])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          .put(new byte[] {0, 0, 1, 0}) // false, false, true, false
                          .array());
        output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            assertThat(new JSONObject(output.toString()).getBoolean("gps_available"), is(true));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testNoValidLocation() {
        // tests that reported 'gps_longitude' and 'gps_latitude' is 500, 500 if no valid location is found in data
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"product_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"product_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  }\n"
                         + "]}\0").getBytes();

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + (8 * 4 * 3)])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          // data line 1
                          .putDouble(500) // invalid product longitude
                          .putDouble(500) // invalid product latitude
                          .putDouble(500) // invalid controller longitude
                          .putDouble(500) // invalid controller latitude
                          // data line 2
                          .putDouble(10)  // valid product longitude
                          .putDouble(500) // invalid product latitude
                          .putDouble(500) // invalid controller longitude
                          .putDouble(20)  // valid controller latitude
                          // data line 3
                          .putDouble(500) // invalid product longitude
                          .putDouble(30)  // valid product latitude
                          .putDouble(40)  // valid controller longitude
                          .putDouble(500) // invalid controller latitude
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("product_gps_longitude"));
            assertThat(columns.get(1), is("product_gps_latitude"));
            assertThat(columns.get(2), is("controller_gps_longitude"));
            assertThat(columns.get(3), is("controller_gps_latitude"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(3)); // 3 lines of data
            assertThat(data.getJSONArray(0).getDouble(0), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(1), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(2), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(3), is(500.0));
            assertThat(data.getJSONArray(1).getDouble(0), is(10.0));
            assertThat(data.getJSONArray(1).getDouble(1), is(500.0));
            assertThat(data.getJSONArray(1).getDouble(2), is(500.0));
            assertThat(data.getJSONArray(1).getDouble(3), is(20.0));
            assertThat(data.getJSONArray(2).getDouble(0), is(500.0));
            assertThat(data.getJSONArray(2).getDouble(1), is(30.0));
            assertThat(data.getJSONArray(2).getDouble(2), is(40.0));
            assertThat(data.getJSONArray(2).getDouble(3), is(500.0));

            double longitude = new JSONObject(output.toString()).getDouble("gps_longitude");
            assertThat(longitude, is(500.0));
            double latitude = new JSONObject(output.toString()).getDouble("gps_latitude");
            assertThat(latitude, is(500.0));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testOnlyValidControllerLocation() {
        // tests that reported 'gps_longitude' and 'gps_latitude' is LATEST valid controller location, in case device
        // location is missing or never valid
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"product_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"product_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  }\n"
                         + "]}\0").getBytes();

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + (8 * 4 * 3)])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          // data line 1
                          .putDouble(500) // invalid product longitude
                          .putDouble(500) // invalid product latitude
                          .putDouble(10)  // valid controller longitude
                          .putDouble(20)  // valid controller latitude
                          // data line 2
                          .putDouble(30)  // valid product longitude
                          .putDouble(500) // invalid product latitude
                          .putDouble(40)  // valid controller longitude
                          .putDouble(50)  // valid controller latitude
                          // data line 3
                          .putDouble(500) // invalid product longitude
                          .putDouble(60)  // valid product latitude
                          .putDouble(70)  // valid controller longitude
                          .putDouble(500) // invalid controller latitude
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("product_gps_longitude"));
            assertThat(columns.get(1), is("product_gps_latitude"));
            assertThat(columns.get(2), is("controller_gps_longitude"));
            assertThat(columns.get(3), is("controller_gps_latitude"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(3)); // 3 lines of data
            assertThat(data.getJSONArray(0).getDouble(0), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(1), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(2), is(10.0));
            assertThat(data.getJSONArray(0).getDouble(3), is(20.0));
            assertThat(data.getJSONArray(1).getDouble(0), is(30.0));
            assertThat(data.getJSONArray(1).getDouble(1), is(500.0));
            assertThat(data.getJSONArray(1).getDouble(2), is(40.0));
            assertThat(data.getJSONArray(1).getDouble(3), is(50.0));
            assertThat(data.getJSONArray(2).getDouble(0), is(500.0));
            assertThat(data.getJSONArray(2).getDouble(1), is(60.0));
            assertThat(data.getJSONArray(2).getDouble(2), is(70.0));
            assertThat(data.getJSONArray(2).getDouble(3), is(500.0));

            double longitude = new JSONObject(output.toString()).getDouble("gps_longitude");
            assertThat(longitude, is(40.0));
            double latitude = new JSONObject(output.toString()).getDouble("gps_latitude");
            assertThat(latitude, is(50.0));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testValidDeviceLocation() {
        // tests that reported 'gps_longitude' and 'gps_latitude' is FIRST valid device location, whatever controller
        // location is
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"product_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"product_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_longitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"controller_gps_latitude\",\n"
                         + "    type: \"double\",\n"
                         + "    size: 8\n"
                         + "  }\n"
                         + "]}\0").getBytes();

        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + (8 * 4 * 3)])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          // data line 1
                          .putDouble(500) // invalid product longitude
                          .putDouble(0)   // invalid product latitude
                          .putDouble(10)  // valid controller longitude
                          .putDouble(20)  // valid controller latitude
                          // data line 2
                          .putDouble(30)  // valid product longitude
                          .putDouble(40)  // valid product latitude
                          .putDouble(50)  // valid controller longitude
                          .putDouble(60)  // valid controller latitude
                          // data line 3
                          .putDouble(70)  // valid product longitude
                          .putDouble(80)  // valid product latitude
                          .putDouble(90)  // valid controller longitude
                          .putDouble(100) // valid controller latitude
                          .array());
        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }
        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("product_gps_longitude"));
            assertThat(columns.get(1), is("product_gps_latitude"));
            assertThat(columns.get(2), is("controller_gps_longitude"));
            assertThat(columns.get(3), is("controller_gps_latitude"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(3)); // 3 lines of data
            assertThat(data.getJSONArray(0).getDouble(0), is(500.0));
            assertThat(data.getJSONArray(0).getDouble(1), is(0.0));
            assertThat(data.getJSONArray(0).getDouble(2), is(10.0));
            assertThat(data.getJSONArray(0).getDouble(3), is(20.0));
            assertThat(data.getJSONArray(1).getDouble(0), is(30.0));
            assertThat(data.getJSONArray(1).getDouble(1), is(40.0));
            assertThat(data.getJSONArray(1).getDouble(2), is(50.0));
            assertThat(data.getJSONArray(1).getDouble(3), is(60.0));
            assertThat(data.getJSONArray(2).getDouble(0), is(70.0));
            assertThat(data.getJSONArray(2).getDouble(1), is(80.0));
            assertThat(data.getJSONArray(2).getDouble(2), is(90.0));
            assertThat(data.getJSONArray(2).getDouble(3), is(100.0));

            double longitude = new JSONObject(output.toString()).getDouble("gps_longitude");
            assertThat(longitude, is(30.0));
            double latitude = new JSONObject(output.toString()).getDouble("gps_latitude");
            assertThat(latitude, is(40.0));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testEOFProcessingLine() {
        // test that EOF during line parsing cause this line to be discarded from output
        byte[] header = ("{details_headers : [\n"
                         + "  {\n"
                         + "    name: \"foo\",\n"
                         + "    type: \"boolean\",\n"
                         + "    size: 1\n"
                         + "  },\n"
                         + "  {\n"
                         + "    name: \"bar\",\n"
                         + "    type: \"boolean\",\n"
                         + "    size: 1\n"
                         + "  }\n"
                         + "]}\0").getBytes();
        //noinspection PointlessArithmeticExpression
        InputStream input = new ByteArrayInputStream(
                ByteBuffer.wrap(new byte[header.length + (1 * 2 + 1 * 1)])
                          .order(ByteOrder.LITTLE_ENDIAN)
                          .put(header)
                          .put(new byte[] {0, 1, 1 /* early eof */})
                          .array());

        OutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
        } catch (IOException e) {
            throw new AssertionError();
        }

        try {
            JSONArray columns = new JSONObject(output.toString()).getJSONArray("details_headers");
            assertThat(columns.get(0), is("foo"));
            assertThat(columns.get(1), is("bar"));
            JSONArray data = new JSONObject(output.toString()).getJSONArray("details_data");
            assertThat(data.length(), is(1)); // only one line, second line was incomplete and dropped
            assertThat(data.getJSONArray(0).getBoolean(0), is(false));
            assertThat(data.getJSONArray(0).getBoolean(1), is(true));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testRealValidPuds() {
        testRealValidPud(R.raw.anafi_binary_pud_1, R.raw.anafi_json_pud_1);
        // add more test files...
    }

    private static void testRealValidPud(@RawRes int binaryPudRes, @RawRes int jsonPudRes) {
        Context context = ApplicationProvider.getApplicationContext();

        JsonParser parser = new JsonParser();
        JsonElement expectedJson = parser.parse(new InputStreamReader(context.getResources().openRawResource(
                jsonPudRes)));

        InputStream input = context.getResources().openRawResource(binaryPudRes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PudAdapter.adapt(input, output);
            output.flush();
            output.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        JsonElement adaptedJson;
        try {
            adaptedJson = parser.parse(output.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }

        assertThat(adaptedJson.equals(expectedJson), is(true));
    }
}
