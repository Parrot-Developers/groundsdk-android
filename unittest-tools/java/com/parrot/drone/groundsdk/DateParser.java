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

package com.parrot.drone.groundsdk;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to build date objects from a human readable string
 */
public final class DateParser {

    /** Formatters used to parse date/time string. */
    private static final SimpleDateFormat[] FORMATTERS = new SimpleDateFormat[] {
            new SimpleDateFormat("dd-MM-yyyy", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
            new SimpleDateFormat("HH:mm", Locale.ROOT),
            new SimpleDateFormat("HH:mm:ss", Locale.ROOT),
            new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ROOT),
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
            new SimpleDateFormat("yyyyMMdd'T'HHmmssZZZ", Locale.ROOT),
            };

    static {
        for (DateFormat format : FORMATTERS) {
            format.setLenient(false); // we want to strictly respect the specified format
        }
    }

    /**
     * Parses the given string and builds a date from it.
     * <p>
     * Accepted formats are: <ul>
     * <li>31-12-2001</li>
     * <li>2001-31-12</li>
     * <li>23:59</li>
     * <li>23:59:59</li>
     * <li>31-12-2001 23:59</li>
     * <li>31-12-2001 23:59:59</li>
     * <li>2001-31-12 23:59:59</li>
     * <li>2001-31-12 23:59:59</li>
     * <li>20013112T235959+0100</li>
     * </ul>
     *
     * @param dateStr date string to parse
     *
     * @return the corresponding Date object
     *
     * @throws AssertionError if the given string cannot be parsed successfully
     */
    @NonNull
    public static Date parse(@NonNull String dateStr) {
        for (SimpleDateFormat formatter : FORMATTERS) {
            try {
                return formatter.parse(dateStr);
            } catch (ParseException ignore) {
            }
        }
        throw new AssertionError("Unsupported date/time format: " + dateStr);
    }

    /**
     * Private constructor for static utility class.
     */
    private DateParser() {
    }
}
