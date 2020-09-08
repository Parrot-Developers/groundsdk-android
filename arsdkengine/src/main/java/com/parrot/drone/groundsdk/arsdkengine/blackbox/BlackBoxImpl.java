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

package com.parrot.drone.groundsdk.arsdkengine.blackbox;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.EnvironmentData;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.Event;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.FlightData;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.HeaderInfo;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.data.TimeStampedData;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;

/**
 * Black box data.
 */
class BlackBoxImpl implements BlackBoxStorage.BlackBox {

    /** JSon serializer thread-safe singleton. */
    private static final Gson JSON_SERIALIZER = new GsonBuilder()
            .excludeFieldsWithModifiers(0)
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /** Black box header. */
    @Expose
    @SerializedName("header")
    @NonNull
    final HeaderInfo mHeader;

    /** Black box list of events. */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // read when serialized to json
    @Expose
    @SerializedName("datas")
    @NonNull
    private final LinkedList<Event> mEvents;

    /** Black box flight data sample buffer. */
    @Expose
    @SerializedName("datas_5Hz")
    @NonNull
    private final LinkedList<FlightData> mFlightInfos;

    /** Black box environment data sample buffer. */
    @Expose
    @SerializedName("datas_1Hz")
    @NonNull
    private final LinkedList<EnvironmentData> mEnvironmentInfos;

    /** Maximum amount of samples in the flight data circular buffer. */
    private final int mMaxFlightSamples;

    /** Maximum amount of samples in the environment data circular buffer. */
    private final int mMaxEnvironmentSamples;

    /**
     * Constructor.
     *
     * @param bufferCapacity circular buffers capacity, in seconds
     * @param drone          drone that this black box is recorded for
     */
    BlackBoxImpl(int bufferCapacity, @NonNull DroneCore drone) {
        mHeader = new HeaderInfo(drone);
        mEvents = new LinkedList<>();
        mFlightInfos = new LinkedList<>();
        mEnvironmentInfos = new LinkedList<>();
        mMaxFlightSamples = 5 * bufferCapacity;
        mMaxEnvironmentSamples = bufferCapacity;
    }

    /**
     * Records an event in the black box.
     *
     * @param event event to record
     */
    void addEvent(@NonNull Event event) {
        mEvents.add(event);
    }

    /**
     * Records a new flight data sample in the black box.
     *
     * @param info flight data sample to record
     */
    void addFlightInfo(@NonNull FlightData info) {
        addInfo(info, mFlightInfos, mMaxFlightSamples);
    }

    /**
     * Records a new environment data sample in the black box.
     *
     * @param info environment data sample to record
     */
    void addEnvironmentInfo(@NonNull EnvironmentData info) {
        addInfo(info, mEnvironmentInfos, mMaxEnvironmentSamples);
    }

    @Override
    public void writeTo(@NonNull OutputStream stream) throws IOException {
        try {
            Writer writer = new OutputStreamWriter(stream);
            JSON_SERIALIZER.toJson(this, writer);
            writer.flush();
        } catch (JsonIOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Records the given info sample to the given sample buffer.
     * <p>
     * This method compares the timestamps of the current buffer head and of the sample to be added; in case they are
     * the same, then the sample is discarded.
     * <p>
     * This method ensures that the destination buffer never grows past a given capacity. If adding a new sample would
     * exceed this capacity, then the eldest element is removed beforehand.
     *
     * @param info           sample to record
     * @param buffer         buffer to record to
     * @param bufferCapacity buffer capacity to respect
     * @param <T>            type of sample
     */
    private static <T extends TimeStampedData> void addInfo(@NonNull T info, @NonNull LinkedList<T> buffer,
                                                            int bufferCapacity) {
        TimeStampedData head = buffer.peekLast();
        if (head == null || head.timeStampDiffersFrom(info)) {
            if (buffer.size() >= bufferCapacity) {
                buffer.removeFirst();
            }
            buffer.addLast(info);
        }
    }
}