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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalIntCore;
import com.parrot.drone.groundsdk.value.OptionalInt;

import java.util.Objects;

/** Core class for the BatteryInfo instrument. */
public final class BatteryInfoCore extends SingletonComponentCore implements BatteryInfo {

    /** Description of BatteryInfo. */
    private static final ComponentDescriptor<Instrument, BatteryInfo> DESC = ComponentDescriptor.of(BatteryInfo.class);

    /** Current battery charge percentage. */
    private int mLevel;

    /** Whether device is currently charging. */
    private boolean mCharging;

    /** Current battery health percentage. */
    @NonNull
    private final OptionalIntCore mHealth;

    /** Current battery cycle count. */
    @NonNull
    private final OptionalIntCore mCycleCount;

    /** Battery serial number. */
    @Nullable
    private String mSerial;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public BatteryInfoCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mHealth = new OptionalIntCore();
        mCycleCount = new OptionalIntCore();
    }

    @Override
    public int getBatteryLevel() {
        return mLevel;
    }

    @Override
    public boolean isCharging() {
        return mCharging;
    }

    @NonNull
    @Override
    public OptionalInt getBatteryHealth() {
        return mHealth;
    }

    @NonNull
    @Override
    public OptionalInt getBatteryCycleCount() {
        return mCycleCount;
    }

    @Nullable
    @Override
    public String getSerial() {
        return mSerial;
    }

    /**
     * Updates the current battery level, as an integer percentage of full charge.
     *
     * @param level new battery level
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public BatteryInfoCore updateLevel(int level) {
        if (level != mLevel) {
            mLevel = level;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates whether the device is currently charging.
     *
     * @param charging {@code true} to mark the device as charging, {@code false} otherwise
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public BatteryInfoCore updateCharging(boolean charging) {
        if (mCharging != charging) {
            mChanged = true;
            mCharging = charging;
        }
        return this;
    }

    /**
     * Updates the current battery health, as an integer percentage of full health.
     *
     * @param health new battery health
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public BatteryInfoCore updateHealth(int health) {
        mChanged |= mHealth.setValue(health);
        return this;
    }

    /**
     * Updates the current battery cycle count.
     *
     * @param cycleCount new battery cycle count
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public BatteryInfoCore updateCycleCount(int cycleCount) {
        mChanged |= mCycleCount.setValue(cycleCount);
        return this;
    }

    /**
     * Updates battery serial number.
     *
     * @param serial new battery serial
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public BatteryInfoCore updateSerial(@NonNull String serial) {
        if (!Objects.equals(mSerial, serial)) {
            mSerial = serial;
            mChanged = true;
        }
        return this;
    }
}
