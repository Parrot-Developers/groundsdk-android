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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Adapts PUD received from the drone in their JSON/binary format, to the JSON format suitable for upload on server.
 */
final class PudAdapter {

    /**
     * Adapts PUD in JSON/binary format to FlightData full JSON format.
     * <p>
     * Caller is responsible to close both streams and in particular to flush the output before consumption.
     *
     * @param from input stream to read PUD from
     * @param to   output stream to write FlightData to
     *
     * @throws IOException in case adapting failed
     */
    static void adapt(@NonNull InputStream from, @NonNull OutputStream to) throws IOException {
        new PudAdapter(from, to).adapt();
    }

    /** Interval between two 'time' infos, over which the rest of binary data is considered invalid and dropped. */
    @VisibleForTesting
    static final long MAX_TIME_INTERVAL = TimeUnit.SECONDS.toMillis(10);

    /** Marker value used when some time info is not known yet. */
    private static final long TIME_UNKNOWN = -1;

    /** Name for the columns description field in both input {@link #mHeader} and output JSON. */
    private static final String COLUMNS_DESCRIPTION_TAG = "details_headers";

    /** Type token for parsing JSON columns descriptor list from {@link #mHeader}. */
    private static final Type COLUMNS_DESCRIPTION_TYPE = new TypeToken<List<ColumnDescriptor>>() {}.getType();

    /** GSON instance used to parse and serialize JSON data. */
    @NonNull
    private final Gson mGson;

    /** Input stream to read PUD from. */
    @NonNull
    private final InputStream mInput;

    /** Writes JSON output. */
    @NonNull
    private final JsonWriter mWriter;

    /** PUD JSON header, as read from input. */
    @NonNull
    private final JsonObject mHeader;

    /** Columns type descriptors, parsed from {@link #mHeader}. */
    @NonNull
    private final List<ColumnDescriptor> mDescriptors;

    /** Latest alert state parsed from input binary data. {@code null} if none. Used to compute {@link #mAlertCount}. */
    @Nullable
    private ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState mLatestAlert;

    /** Latest time info parsed from input binary data. */
    private long mLatestTime;

    /** Time when the drone started flying, parsed from input binary data. {@link #TIME_UNKNOWN} if not known yet. */
    private long mFlightStartTime;

    /** Total time the drone spent flying. Computed from input binary data based on the drone flying state changes. */
    private long mFlyingTime;

    /** Counts alerts from input binary data. */
    private int mAlertCount;

    /** Device GPS availability, parsed from input binary data. */
    private boolean mGpsAvailable;

    /** First meaningful device location parsed from binary data. */
    @Nullable
    private Location mFirstDeviceLocation;

    /** Latest meaningful controller location parsed from binary data. */
    @NonNull
    private Location mLatestControllerLocation;

    /**
     * Constructor.
     *
     * @param input  input stream to read PUD from
     * @param output output stream to write FlightData to
     *
     * @throws IOException in case parsing input header failed
     */
    private PudAdapter(@NonNull InputStream input, @NonNull OutputStream output) throws IOException {
        mInput = input;
        mWriter = new JsonWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        mGson = new Gson();
        mDescriptors = new ArrayList<>();
        mLatestControllerLocation = new Location();
        mFlightStartTime = TIME_UNKNOWN;

        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int readByte;
        while ((readByte = input.read()) > 0) { // read until EOF or first null separator marking end of JSON header
            headerBuffer.write(readByte);
        }

        try {
            mHeader = new JsonParser().parse(headerBuffer.toString("UTF-8")).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Malformed JSON header", e);
        }
    }

    /**
     * Parses input PUD and adapts it to FlightData output.
     *
     * @throws IOException in case parsing failed
     */
    private void adapt() throws IOException {
        // being output JSON object
        mWriter.beginObject();

        adaptColumnDescriptors();

        adaptBinaryData();

        // write collected data to header
        mHeader.addProperty("crash", mAlertCount);
        mHeader.addProperty("total_run_time", mLatestTime);
        mHeader.addProperty("run_time", mFlyingTime);
        mHeader.addProperty("gps_available", mGpsAvailable);

        Location location = mFirstDeviceLocation == null ? mLatestControllerLocation : mFirstDeviceLocation;
        mHeader.addProperty("gps_latitude", location.latitude);
        mHeader.addProperty("gps_longitude", location.longitude);

        // remove original columns description
        mHeader.remove(COLUMNS_DESCRIPTION_TAG);

        // write all header properties
        for (Map.Entry<String, JsonElement> entry : mHeader.entrySet()) {
            mWriter.name(entry.getKey());
            mGson.toJson(entry.getValue(), mWriter);
        }

        // end output JSON object
        mWriter.endObject();
        mWriter.flush();
    }

    /**
     * Parses and validates column descriptors from header. Writes column names to output.
     * <p>
     * Note that descriptor without type pass validation, but they are not written to output and the corresponding field
     * in binary data won't be parsed and written to output.
     *
     * @throws IOException in case parsing failed
     */
    private void adaptColumnDescriptors() throws IOException {
        // begin output descriptor array
        mWriter.name(COLUMNS_DESCRIPTION_TAG).beginArray();

        List<ColumnDescriptor> descriptors;
        try {
            descriptors = mGson.fromJson(mHeader.get(COLUMNS_DESCRIPTION_TAG), COLUMNS_DESCRIPTION_TYPE);
        } catch (JsonParseException e) {
            throw new IOException("Malformed columns description", e);
        }

        if (descriptors == null || descriptors.isEmpty()) {
            throw new IOException("Empty columns description");
        }

        for (int i = 0, N = descriptors.size(); i < N; i++) {
            ColumnDescriptor descriptor = descriptors.get(i);
            if (descriptor == null) {
                throw new IOException("Null column " + i + " descriptor");
            }
            if (descriptor.getSize() <= 0) {
                throw new IOException("Invalid column " + i + " descriptor size");
            }
            if (TextUtils.isEmpty(descriptor.getName())) {
                throw new IOException("Invalid column " + i + " descriptor name");
            }
            if (descriptor.getType() != null) { // add to final column description only if the type is known
                mWriter.value(descriptor.getName());
            }
            // but keep track of it anyway in order to skip binary value during parsing
            mDescriptors.add(descriptor);
        }

        // write speed descriptor name
        mWriter.value("speed");

        // end output descriptor array
        mWriter.endArray();
    }

    /**
     * Parses input binary data and writes it, properly adapted, to JSON output.
     * <p>
     * Parsing stops in case of EOF (in which case, the line being currently parsed is dropped), or in case time parsed
     * from binary data appears inconsistent.
     *
     * @throws IOException in case parsing failed
     */
    private void adaptBinaryData() throws IOException {
        // begin output data lines array
        mWriter.name("details_data").beginArray();

        // parse all input line by line
        boolean parseNext;
        do {
            parseNext = adaptNextBinaryLine();
        } while (parseNext);

        // finalize flying time if necessary
        if (mFlightStartTime != TIME_UNKNOWN) {
            mFlyingTime += mLatestTime - mFlightStartTime;
        }

        // end output data array
        mWriter.endArray();
    }

    /**
     * Parses the next binary data line and writes it, properly adapted, to JSON output.
     * <p>
     * In case of EOF during line parsing, then the line is dropped, and this method returns {@code false} so that
     * parsing stops.
     *
     * @return {@code true} if parsing may proceed with the next line, otherwise {@code false}
     *
     * @throws IOException in case parsing failed.
     */
    private boolean adaptNextBinaryLine() throws IOException {
        // Keeps track of device latitude & longitude as read in line.
        Location deviceLocation = new Location();

        // Keeps track of controller latitude & longitude as read in line.
        Location controllerLocation = new Location();

        // Keeps track of time as read in line
        long time = TIME_UNKNOWN;

        // will contain the sum of squared speeds from all axis read in line.
        double speedSquare = 0;

        // JSON data line to write once parsing is successful.
        JsonArray lineToWrite = new JsonArray();

        try {
            for (ColumnDescriptor descriptor : mDescriptors) {
                byte[] fieldData = descriptor.readNextField(mInput);

                assert descriptor.getName() != null; // validated when mDescriptors is built
                switch (descriptor.getName()) {
                    case "time":
                        time = descriptor.parseAsInt(fieldData);
                        break;
                    case "speed_vx":
                    case "speed_vy":
                    case "speed_vz":
                        speedSquare += Math.pow(descriptor.parseAsNumber(fieldData).doubleValue(), 2);
                        break;
                    case "product_gps_available":
                        mGpsAvailable |= descriptor.parseAsBoolean(fieldData);
                        break;
                    case "product_gps_latitude":
                        deviceLocation.latitude = descriptor.parseAsDouble(fieldData);
                        break;
                    case "product_gps_longitude":
                        deviceLocation.longitude = descriptor.parseAsDouble(fieldData);
                        break;
                    case "controller_gps_latitude":
                        controllerLocation.latitude = descriptor.parseAsDouble(fieldData);
                        break;
                    case "controller_gps_longitude":
                        controllerLocation.longitude = descriptor.parseAsDouble(fieldData);
                        break;
                    case "alert_state":
                        ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState alert =
                                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.fromValue(
                                        descriptor.parseAsInt(fieldData));
                        if (alert != null) {
                            processAlertState(alert);
                        }
                        break;
                    case "flying_state":
                        ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state =
                                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.fromValue(
                                        descriptor.parseAsInt(fieldData));
                        if (state != null) {
                            processFlyingState(state);
                        }
                        break;
                }

                // update latest time
                if (time != TIME_UNKNOWN) {
                    if (time < mLatestTime || time > mLatestTime + MAX_TIME_INTERVAL) {
                        return false; // stop parsing if time is incoherent
                    }
                    mLatestTime = time;
                }

                // write data
                descriptor.appendData(fieldData, lineToWrite);
            }

            // update known device and controller location
            if (controllerLocation.isValid()) {
                mLatestControllerLocation = controllerLocation;
            }
            if (mFirstDeviceLocation == null && deviceLocation.isValid()) {
                mFirstDeviceLocation = deviceLocation;
            }

            // write computed speed value
            lineToWrite.add(Math.sqrt(speedSquare));

            // write line data array
            mGson.toJson(lineToWrite, mWriter);

            return true;
        } catch (EOFException e) {
            // could not parse line entirely, drop it and stop parsing
            return false;
        }
    }

    /**
     * Processes an alert state info read from binary data.
     * <p>
     * This allows to keep track of the alert count, which is written when the JSON output is finalized.
     *
     * @param alert alert state to process
     */
    private void processAlertState(@NonNull ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState alert) {
        if (mLatestAlert != alert) {
            mLatestAlert = alert;
            switch (mLatestAlert) {
                case NONE:
                case CRITICAL_BATTERY:
                case LOW_BATTERY:
                    break;
                case USER:
                case CUT_OUT:
                case TOO_MUCH_ANGLE:
                    mAlertCount++;
                    break;
            }
        }
    }

    /**
     * Processes a flying state info read from binary data.
     * <p>
     * This allows to keep track of the drone flying time, which is written when the JSON output is finalized.
     *
     * @param state flying state to process
     */
    private void processFlyingState(@NonNull ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
        switch (state) {
            case LANDED:
                if (mFlightStartTime != TIME_UNKNOWN) {
                    mFlyingTime += mLatestTime - mFlightStartTime;
                    mFlightStartTime = TIME_UNKNOWN;
                }
                break;
            case TAKINGOFF:
            case HOVERING:
            case FLYING:
                if (mFlightStartTime == TIME_UNKNOWN) {
                    mFlightStartTime = mLatestTime;
                }
                break;
            case LANDING:
            case EMERGENCY:
            case USERTAKEOFF:
            case MOTOR_RAMPING:
            case EMERGENCY_LANDING:
                break;
        }
    }

    /**
     * Parses data as an UTF-8 string.
     *
     * @param data data to parse
     *
     * @return string representation of the given data
     */
    @NonNull
    private static String parseString(@NonNull byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Parses data as an integer.
     *
     * @param data data to parse
     *
     * @return integer representation of the given data
     *
     * @throws IOException in case the given data cannot be parsed to an integer
     */
    private static int parseInt(@NonNull byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (data.length == 1) {
            return buffer.get();
        } else if (data.length == 2) {
            return buffer.getShort();
        } else if (data.length == 4) {
            return buffer.getInt();
        }
        throw new IOException("Cannot parse integer: " + Arrays.toString(data));
    }

    /**
     * Parses data as a boolean.
     *
     * @param data data to parse
     *
     * @return boolean representation of the given data
     *
     * @throws IOException in case the given data cannot be parsed to a boolean
     */
    private static boolean parseBoolean(@NonNull byte[] data) throws IOException {
        if (data.length > 0) {
            return data[0] != 0;
        }
        throw new IOException("Cannot parse boolean: " + Arrays.toString(data));
    }

    /**
     * Parses data as a float.
     *
     * @param data data to parse
     *
     * @return float representation of the given data
     *
     * @throws IOException in case the given data cannot be parsed to a float
     */
    private static float parseFloat(@NonNull byte[] data) throws IOException {
        try {
            return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        } catch (BufferOverflowException e) {
            throw new IOException(e);
        }
    }

    /**
     * Parses data as a double.
     *
     * @param data data to parse
     *
     * @return double representation of the given data
     *
     * @throws IOException in case the given data cannot be parsed to a double
     */
    private static double parseDouble(@NonNull byte[] data) throws IOException {
        try {
            return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getDouble();
        } catch (BufferOverflowException e) {
            throw new IOException(e);
        }
    }

    /**
     * Data class representing a Location with a latitude and a longitude.
     */
    private static final class Location {

        /** Marker value used when some coordinate is not known. */
        private static final double COORD_UNKNOWN = 500;

        /** Location latitude. */
        double latitude;

        /** Location longitude. */
        double longitude;

        /**
         * Constructor.
         */
        Location() {
            latitude = COORD_UNKNOWN;
            longitude = COORD_UNKNOWN;
        }

        /**
         * Tells whether the location data is valid, i.e. that both latitude and longitude have meaningful values
         *
         * @return {@code true} if the location is valid, otherwise {@code false}
         */
        boolean isValid() {
            return Double.compare(latitude, COORD_UNKNOWN) != 0 && Double.compare(longitude, COORD_UNKNOWN) != 0;
        }
    }

    /**
     * A column type descriptor, as parsed from JSON {@link #mHeader}.
     */
    private static class ColumnDescriptor {

        /** Column name. */
        @SuppressWarnings("unused")
        @SerializedName("name")
        @Nullable
        private String mName;

        /** Known column types. */
        enum Type {

            /** String column type. */
            @SerializedName("string")
            STRING,

            /** Integer column type. */
            @SerializedName("integer")
            INTEGER,

            /** Boolean column type. */
            @SerializedName("boolean")
            BOOLEAN,

            /** Float column type. */
            @SerializedName("float")
            FLOAT,

            /** Double column type. */
            @SerializedName("double")
            DOUBLE
        }

        /** Column type. */
        @SuppressWarnings("unused")
        @SerializedName("type")
        @Nullable
        private Type mType;

        /** Column size. */
        @SuppressWarnings("unused")
        @SerializedName("size")
        private int mSize;

        /**
         * Retrieves the column name.
         *
         * @return column name.
         */
        @Nullable
        String getName() {
            return mName;
        }

        /**
         * Retrieves the column type.
         *
         * @return column type
         */
        @Nullable
        Type getType() {
            return mType;
        }

        /**
         * Retrieves the column size.
         *
         * @return column size, in bytes.
         */
        int getSize() {
            return mSize;
        }

        /**
         * Reads the next field corresponding to this descriptor.
         *
         * @param input input stream to read next field from
         *
         * @return a byte array containing field data
         *
         * @throws IOException  in case reading failed
         * @throws EOFException in case the end of stream is reached
         */
        @NonNull
        byte[] readNextField(@NonNull InputStream input) throws IOException {
            // read column data
            byte[] data = new byte[mSize];
            int remaining = data.length;
            while (remaining > 0) {
                int read = input.read(data, data.length - remaining, remaining);
                if (read == -1) {
                    throw new EOFException("Binary data underflow");
                }
                remaining -= read;
            }
            return data;
        }

        /**
         * Parses data as an integer.
         *
         * @param data data to parse
         *
         * @return integer representation of the given data
         *
         * @throws IOException in case this descriptor type is not {@link Type#INTEGER}
         */
        int parseAsInt(@NonNull byte[] data) throws IOException {
            checkType(Type.INTEGER);
            return parseInt(data);
        }

        /**
         * Parses data as a boolean.
         *
         * @param data data to parse
         *
         * @return boolean representation of the given data
         *
         * @throws IOException in case this descriptor type is not {@link Type#BOOLEAN}
         */
        boolean parseAsBoolean(@NonNull byte[] data) throws IOException {
            checkType(Type.BOOLEAN);
            return parseBoolean(data);
        }

        /**
         * Parses data as a double.
         *
         * @param data data to parse
         *
         * @return double representation of the given data
         *
         * @throws IOException in case this descriptor type is not {@link Type#DOUBLE}
         */
        double parseAsDouble(@NonNull byte[] data) throws IOException {
            checkType(Type.DOUBLE);
            return parseDouble(data);
        }

        /**
         * Parses data as any kind of number.
         * <p>
         * This method supports parsing any of {@link Type#INTEGER}, {@link Type#FLOAT} or {@link Type#DOUBLE}.
         *
         * @param data data to parse
         *
         * @return Number representation of the given data
         *
         * @throws IOException in case this descriptor type is not {@link Type#FLOAT}
         */
        @NonNull
        Number parseAsNumber(@NonNull byte[] data) throws IOException {
            if (mType == Type.INTEGER) {
                return parseInt(data);
            } else if (mType == Type.FLOAT) {
                return parseFloat(data);
            } else if (mType == Type.DOUBLE) {
                return parseDouble(data);
            } else {
                throw new IOException("Cannot parse field [name: " + mName + ", size:" + mSize + ", type: " + mType
                                      + "] as number");
            }
        }

        /**
         * Checks this descriptor type against a specific type.
         *
         * @param type type to check against
         *
         * @throws IOException in case this descriptor is not of the specified type
         */
        private void checkType(@NonNull Type type) throws IOException {
            if (mType != type) {
                throw new IOException("Cannot parse field [name: " + mName + ", size:" + mSize + ", type: " + mType
                                      + "] as " + type.name());
            }
        }

        /**
         * Adapts raw data to this descriptor format and appends it to a JSON array.
         *
         * @param data  data to adapt
         * @param array JSON array to add adapted data to
         *
         * @throws IOException in case data cannot be adapted to this descriptor format
         */
        void appendData(@NonNull byte[] data, @NonNull JsonArray array) throws IOException {
            if (mType != null) { // otherwise field data is not written (column is ignored)
                switch (mType) {
                    case STRING:
                        array.add(parseString(data));
                        break;
                    case INTEGER:
                        array.add(parseInt(data));
                        break;
                    case BOOLEAN:
                        array.add(parseBoolean(data));
                        break;
                    case FLOAT:
                        array.add(parseFloat(data));
                        break;
                    case DOUBLE:
                        array.add(parseDouble(data));
                        break;
                }
            }
        }
    }
}
