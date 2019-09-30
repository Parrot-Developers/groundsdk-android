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

package com.parrot.drone.groundsdk.arsdkengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class to handle date/time ISO 8601 formatting.
 */
public final class Iso8601 {

    /**
     * Converts a date to ISO 8601 base string representation.
     *
     * @param date date to format
     *
     * @return formatted date and time string
     */
    @NonNull
    public static String toBaseDateAndTimeFormat(@NonNull Date date) {
        //noinspection ConstantConditions: INSTANCE has a default initial value
        return BASE_DATE_TIME_FORMAT.get().format(date);
    }

    /**
     * Converts an ISO 8601 base date string representation to its {@code Date} equivalent.
     *
     * @param formattedDate date string representation to parse
     *
     * @return corresponding {@code Date} if parsing was successful
     *
     * @throws ParseException if parsing failed
     */
    @Nullable
    public static Date fromBaseDateAndTimeFormat(@NonNull String formattedDate) throws ParseException {
        //noinspection ConstantConditions: INSTANCE has a default initial value
        return BASE_DATE_TIME_FORMAT.get().parse(formattedDate);
    }

    /** Base ISO 8601 date/time formatter, one per thread to encapsulate SimpleDateFormat in a thread-safe manner. */
    private static final ThreadLocal<SimpleDateFormat> BASE_DATE_TIME_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        /** Base date and time format. */
        private static final String FORMAT = "yyyyMMdd'T'HHmmssZZZ";

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(FORMAT, Locale.ROOT);
        }
    };

    /**
     * Private constructor for static utility class.
     */
    private Iso8601() {
    }
}
