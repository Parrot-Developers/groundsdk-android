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

package com.parrot.drone.groundsdk.arsdkengine.blackbox.data;

import androidx.annotation.NonNull;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.parrot.drone.sdkcore.TimeProvider;

import java.io.IOException;

/**
 * Base class for timestamped data samples, such as {@link EnvironmentData} and {@link FlightData}.
 * <p>
 * This class holds the timestamp of such samples and provide an update method to update such timestamp, which should be
 * called by subclasses whenever data is actually modified.
 */
public class TimeStampedData {

    /** Timestamp, in milliseconds. */
    @Expose
    @SerializedName("timestamp")
    @JsonAdapter(TimeStampAdapter.class)
    private long mTimeStamp;

    /**
     * Tells whether this sample's timestamp differs from another one.
     *
     * @param other sample to compare the timestamp of
     *
     * @return {@code true} if timestamps differs, otherwise {@code false}
     */
    public final boolean timeStampDiffersFrom(@NonNull TimeStampedData other) {
        return mTimeStamp != other.mTimeStamp;
    }

    /**
     * Default constructor.
     */
    TimeStampedData() {
        stamp();
    }

    /**
     * Copy constructor.
     *
     * @param other sample to copy data from
     */
    TimeStampedData(@NonNull TimeStampedData other) {
        mTimeStamp = other.mTimeStamp;
    }

    /**
     * Updates current timestamp.
     */
    void stamp() {
        mTimeStamp = TimeProvider.elapsedRealtime();
    }

    /**
     * A GSON adapter that allows to serialize the timestamp as a double in seconds.
     */
    // TODO: this should be changed in black boxes.
    private static final class TimeStampAdapter extends TypeAdapter<Long> {

        @Override
        public void write(JsonWriter out, Long value) throws IOException {
            out.value(value / 1000.0);
        }

        @Override
        public Long read(JsonReader in) {
            throw new UnsupportedOperationException();
        }
    }
}
