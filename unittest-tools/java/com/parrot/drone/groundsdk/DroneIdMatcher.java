/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.device.peripheral.Dri;

import org.hamcrest.Matcher;

import androidx.annotation.NonNull;

import static com.parrot.drone.groundsdk.MatcherBuilders.featureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.nullValue;

/**
 * DRI IdInfo matcher
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public final class DroneIdMatcher {
    public static Matcher<Dri.DroneId> is(Dri.IdType idType, String name) {
        return allOf(
                valueMatcher(name, "ID", Dri.DroneId::getId),
                valueMatcher(idType, "ID type", Dri.DroneId::getType)
        );
    }

    public static Matcher<Dri.TypeConfig> is(@NonNull Dri.TypeConfig config) {
        return allOf(
                valueMatcher(config.getType(), "type", Dri.TypeConfig::getType),
                valueMatcher(config.getOperatorId(), "operator", Dri.TypeConfig::getOperatorId)
        );
    }

    public static Matcher<Dri.TypeConfigState> is(@NonNull Dri.TypeConfigState.State state,
                                                  @NonNull Dri.TypeConfig config) {
        return allOf(
                valueMatcher(state, "state", Dri.TypeConfigState::getState),
                featureMatcher(is(config), "config", Dri.TypeConfigState::getConfig)
        );
    }

    public static Matcher<Dri.TypeConfigState> is(@NonNull Dri.TypeConfigState.State state) {
        return allOf(
                valueMatcher(state, "state", Dri.TypeConfigState::getState),
                featureMatcher(nullValue(), "config", Dri.TypeConfigState::getConfig)
        );
    }
}
