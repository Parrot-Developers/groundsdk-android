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

package com.parrot.drone.groundsdk.internal.device;

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;

import org.junit.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class DeviceModelsTest {

    @Test
    public void testMappings() {
        for (Drone.Model model : Drone.Model.values()) {
            assertThat(DeviceModels.droneModelOrThrow(model.id()), is(model));
            assertThat(DeviceModels.modelOrThrow(model.id()), is(model));
            assertThat(DeviceModels.fromName(model.name()), is(model));
        }

        for (RemoteControl.Model model : RemoteControl.Model.values()) {
            assertThat(DeviceModels.rcModelOrThrow(model.id()), is(model));
            assertThat(DeviceModels.modelOrThrow(model.id()), is(model));
            assertThat(DeviceModels.fromName(model.name()), is(model));
        }
    }

    @Test
    public void testAllModels() {
        Set<DeviceModel> models = new HashSet<>();
        models.addAll(EnumSet.allOf(Drone.Model.class));
        models.addAll(EnumSet.allOf(RemoteControl.Model.class));
        assertThat(DeviceModels.ALL, equalTo(models));
    }

    @Test
    public void testNoNameDuplicate() {
        // this test ensures the uniqueness of enum names across both Drone.Model and RemoteControl.Model,
        // which unfortunately cannot be ensured statically at compile-time
        Set<String> names = new HashSet<>();
        for (DeviceModel model : DeviceModels.ALL) {
            String name = model.name();
            assertThat(names, not(contains(name)));
            names.add(name);
        }
    }

    @Test
    public void testNoIdentifierDuplicate() {
        // this test ensures the uniqueness of identifiers across both Drone.Model and RemoteControl.Model,
        // which unfortunately cannot be ensured statically at compile-time
        Set<Integer> ids = new HashSet<>();
        for (DeviceModel model : DeviceModels.ALL) {
            @DeviceModel.Id int id = model.id();
            assertThat(ids, not(contains(id)));
            ids.add(id);
        }
    }
}
