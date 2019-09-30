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

package com.parrot.drone.groundsdk.internal.device.instrument;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.EnumMap;

/** Core class for the Alarms instrument. */
public final class AlarmsCore extends SingletonComponentCore implements Alarms {

    /** Description of Alarms. */
    private static final ComponentDescriptor<Instrument, Alarms> DESC = ComponentDescriptor.of(Alarms.class);

    /** Current alarms. */
    @NonNull
    private final EnumMap<Alarm.Kind, AlarmCore> mAlarms;

    /** Current delay before automatic landing, in seconds. {@code 0} when no automatic landing is scheduled. */
    @IntRange(from = 0)
    private int mAutoLandingDelay;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public AlarmsCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mAlarms = new EnumMap<>(Alarm.Kind.class);
        for (Alarm.Kind kind : Alarm.Kind.values()) {
            mAlarms.put(kind, new AlarmCore(kind, Alarm.Level.NOT_SUPPORTED));
        }
    }

    /**
     * Gets the alarm of a given kind.
     *
     * @param kind the kind of the alarm
     *
     * @return an alarm of the requested kind.
     */
    @Override
    @NonNull
    public Alarm getAlarm(@NonNull Alarm.Kind kind) {
        //noinspection ConstantConditions: all alarm kinds are present in map
        return mAlarms.get(kind);
    }

    @Override
    public int automaticLandingDelay() {
        return mAutoLandingDelay;
    }

    /**
     * Updates the level of a given alarm.
     *
     * @param kind  the kind of the alarm
     * @param level the new level
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public AlarmsCore updateAlarmLevel(@NonNull Alarm.Kind kind, @NonNull Alarm.Level level) {
        AlarmCore alarm = mAlarms.get(kind);
        assert alarm != null; // all alarm kinds are present in map
        if (alarm.getLevel() != level) {
            alarm.setLevel(level);
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the level of a multiple alarms.
     *
     * @param level the new level
     * @param kinds alarm kinds to set the level of
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public AlarmsCore updateAlarmsLevel(@NonNull Alarm.Level level, @NonNull Alarm.Kind... kinds) {
        for (Alarm.Kind kind : kinds) {
            AlarmCore alarm = mAlarms.get(kind);
            assert alarm != null; // all alarm kinds are present in map
            if (alarm.getLevel() != level) {
                alarm.setLevel(level);
                mChanged = true;
            }
        }
        return this;
    }

    /**
     * Updates the current automatic landing delay.
     *
     * @param delay automatic landing delay, in seconds
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public AlarmsCore updateAutoLandingDelay(@IntRange(from = 0) int delay) {
        if (mAutoLandingDelay != delay) {
            mAutoLandingDelay = delay;
            mChanged = true;
        }
        return this;
    }

    /** Core class for the alarm. */
    private static class AlarmCore extends Alarm {

        /** Kind of the alarm. */
        @NonNull
        private final Kind mKind;

        /** Level of the alarm. */
        @NonNull
        private Level mLevel;

        /**
         * Constructor.
         *
         * @param level the initial level of the alarm
         */
        AlarmCore(@NonNull Kind kind, @NonNull Level level) {
            mKind = kind;
            mLevel = level;
        }

        @Override
        @NonNull
        public Kind getKind() {
            return mKind;
        }

        @Override
        @NonNull
        public Level getLevel() {
            return mLevel;
        }

        /**
         * Sets the level of the alarm.
         *
         * @param level the level to set
         */
        void setLevel(@NonNull Level level) {
            mLevel = level;
        }
    }
}
