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

package com.parrot.drone.sdkcore.ulog;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.SdkCore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ULog API.
 * <p>
 * Allows to send logs.
 */
public final class ULog {

    /** Environment variable used to set default ULog log level. */
    public static final String DEFAULT_LEVEL_ENV_VAR = "ULOG_LEVEL";

    /** Log levels. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ULOG_CRIT, ULOG_ERR, ULOG_WARN, ULOG_NOTICE, ULOG_INFO, ULOG_DEBUG})
    public @interface Level {
    }

    /* Numerical ULog levels MUST be kept in sync with C ulog priority levels in ulog.h */

    /** Critical conditions. */
    public static final int ULOG_CRIT = 2;

    /** Error conditions. */
    public static final int ULOG_ERR = 3;

    /** Warning conditions. */
    public static final int ULOG_WARN = 4;

    /** Normal but significant condition. */
    public static final int ULOG_NOTICE = 5;

    /** Informational message. */
    public static final int ULOG_INFO = 6;

    /** Debug-level message. */
    public static final int ULOG_DEBUG = 7;

    /**
     * Send a critical log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void c(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_CRIT, tag, msg);
    }

    /**
     * Send an critical log, with an exception to be dumped.
     *
     * @param tag       tag use to log.
     * @param msg       message to log.
     * @param throwable exception to log
     */
    public static void c(@NonNull ULogTag tag, @NonNull String msg, @NonNull Throwable throwable) {
        logWithException(ULOG_CRIT, tag, msg, throwable);
    }

    /**
     * Send an error log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void e(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_ERR, tag, msg);
    }

    /**
     * Send an error log, with an exception to be dumped.
     *
     * @param tag       tag use to log.
     * @param msg       message to log.
     * @param throwable exception to log
     */
    public static void e(@NonNull ULogTag tag, @NonNull String msg, @NonNull Throwable throwable) {
        logWithException(ULOG_ERR, tag, msg, throwable);
    }

    /**
     * Send a warning log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void w(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_WARN, tag, msg);
    }

    /**
     * Send an warning log, with an exception to be dumped.
     *
     * @param tag       tag use to log.
     * @param msg       message to log.
     * @param throwable exception to log
     */
    public static void w(@NonNull ULogTag tag, @NonNull String msg, @NonNull Throwable throwable) {
        logWithException(ULOG_WARN, tag, msg, throwable);
    }

    /**
     * Send a notice log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void n(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_NOTICE, tag, msg);
    }

    /**
     * Send an info log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void i(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_INFO, tag, msg);
    }

    /**
     * Send a debug log.
     *
     * @param tag tag use to log.
     * @param msg message to log.
     */
    public static void d(@NonNull ULogTag tag, @NonNull String msg) {
        log(ULOG_DEBUG, tag, msg);
    }

    /**
     * Check if the critical log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if critical log will be logged for this tag.
     */
    public static boolean c(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_CRIT;
    }

    /**
     * Check if the error log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if error log will be logged for this tag.
     */
    public static boolean e(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_ERR;
    }

    /**
     * Check if the warning log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if warning log will be logged for this tag.
     */
    public static boolean w(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_WARN;
    }

    /**
     * Check if the notice log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if notice log will be logged for this tag.
     */
    public static boolean n(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_NOTICE;
    }

    /**
     * Check if the info log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if info log will be logged for this tag.
     */
    public static boolean i(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_INFO;
    }

    /**
     * Check if the debug log will be logged for this tag.
     *
     * @param tag tag to check.
     *
     * @return true if debug log will be logged for this tag.
     */
    public static boolean d(@NonNull ULogTag tag) {
        return tag.getMinLevel() >= ULOG_DEBUG;
    }

    /**
     * Set the minimum level to log for a tag.
     *
     * @param tagName tag name.
     * @param level   the minimum level.
     */
    public static void setTagMinLevel(@NonNull String tagName, @Level int level) {
        nativeSetTagLevel(tagName, level);
    }

    /**
     * Sends a log message.
     *
     * @param level log level
     * @param tag   log tag
     * @param msg   log message
     */
    private static void log(@Level int level, @NonNull ULogTag tag, @NonNull String msg) {
        //check if enable
        if (level <= tag.getMinLevel()) {
            nativeLog(level, tag.getNativePtr(), msg);
        }
    }

    /**
     * Sends a log message, plus an exception to be dumped.
     *
     * @param level     log level
     * @param tag       log tag
     * @param msg       log message
     * @param throwable exception to log
     */
    private static void logWithException(@Level int level, @NonNull ULogTag tag, @NonNull String msg,
                                         @NonNull Throwable throwable) {
        // check if enabled
        if (level <= tag.getMinLevel()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println(msg);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            nativeLog(level, tag.getNativePtr(), stringWriter.toString());
        }
    }

    /**
     * Disabled constructor for static utility class.
     */
    private ULog() {
    }

    /* JNI declarations and setup */
    private static native void nativeLog(int priority, long nativeCookie, String msg);

    private static native void nativeSetTagLevel(String tag, int level);

    static {
        SdkCore.init();
    }
}


