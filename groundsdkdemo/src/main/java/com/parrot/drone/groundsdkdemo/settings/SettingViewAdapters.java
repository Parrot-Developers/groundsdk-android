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

package com.parrot.drone.groundsdkdemo.settings;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.value.BooleanSetting;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdk.value.EnumSetting;
import com.parrot.drone.groundsdk.value.IntSetting;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;
import com.parrot.drone.groundsdk.value.StringSetting;

public final class SettingViewAdapters {

    public static void updateSetting(@NonNull ToggleSettingView view, @NonNull OptionalBooleanSetting setting) {
        view.setListener(setting::toggle)
            .setToggled(setting.isEnabled())
            .setAvailable(setting.isAvailable())
            .setUpdating(setting.isUpdating());
    }

    public static void updateSetting(@NonNull RangedSettingView view, @NonNull OptionalDoubleSetting setting) {
        view.setListener(setting::setValue)
            .setValue(setting.getMin(), setting.getValue(), setting.getMax())
            .setAvailable(setting.isAvailable())
            .setUpdating(setting.isUpdating());
    }

    public static void updateSetting(@NonNull ToggleSettingView view, @NonNull BooleanSetting setting) {
        view.setListener(setting::toggle)
            .setToggled(setting.isEnabled())
            .setAvailable(true)
            .setUpdating(setting.isUpdating());
    }

    public static void updateSetting(@NonNull RangedSettingView view, @NonNull DoubleSetting setting) {
        view.setListener(setting::setValue)
            .setValue(setting.getMin(), setting.getValue(), setting.getMax())
            .setAvailable(true)
            .setUpdating(setting.isUpdating());
    }

    public static void updateSetting(@NonNull RangedSettingView view, @NonNull IntSetting setting) {
        view.setListener(newValue -> setting.setValue((int) newValue))
            .setValue(setting.getMin(), setting.getValue(), setting.getMax())
            .setAvailable(true)
            .setUpdating(setting.isUpdating());
    }

    public static <E extends Enum<E>> void updateSetting(@NonNull MultiChoiceSettingView<E> view,
                                                         @NonNull EnumSetting<E> setting) {
        view.setListener(setting::setValue)
            .setChoices(setting.getAvailableValues())
            .setSelection(setting.getValue())
            .setUpdating(setting.isUpdating());
    }

    public static void updateSetting(@NonNull TextSettingView view, @NonNull StringSetting setting) {
        view.setListener(setting::setValue)
            .setText(setting.getValue())
            .setAvailable(true)
            .setUpdating(setting.isUpdating());
    }

    private SettingViewAdapters() {
    }
}
