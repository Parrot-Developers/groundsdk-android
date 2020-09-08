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

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides all known {@link DeviceModel device models} and mappings from model names to the corresponding model.
 */
public final class DeviceModels {

    /** All known device models. */
    @NonNull
    public static final Set<DeviceModel> ALL;

    /**
     * Retrieves the device model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the device model with such an identifier if it exists, otherwise {@code null}
     */
    @Nullable
    public static DeviceModel model(@DeviceModel.Id int id) {
        DeviceModel model = DRONE_MODELS_BY_ID.get(id);
        return model == null ? RC_MODELS_BY_ID.get(id) : model;
    }

    /**
     * Retrieves the device model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the device model with such an identifier
     *
     * @throws IllegalArgumentException if no such device model exists
     */
    @NonNull
    public static DeviceModel modelOrThrow(@DeviceModel.Id int id) {
        DeviceModel model = DRONE_MODELS_BY_ID.get(id);
        if (model == null) {
            model = RC_MODELS_BY_ID.get(id);
        }
        if (model == null) {
            throw new IllegalArgumentException("Unsupported device model id: " + id);
        }
        return model;
    }

    /**
     * Retrieves the drone model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the drone model with such an identifier if it exists, otherwise {@code null}
     */
    @Nullable
    public static Drone.Model droneModel(@DeviceModel.Id int id) {
        return DRONE_MODELS_BY_ID.get(id);
    }

    /**
     * Retrieves the drone model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the drone model with such an identifier
     *
     * @throws IllegalArgumentException if no such drone model exists
     */
    @NonNull
    public static Drone.Model droneModelOrThrow(@DeviceModel.Id int id) {
        Drone.Model model = DRONE_MODELS_BY_ID.get(id);
        if (model == null) {
            throw new IllegalArgumentException("Unsupported drone model id: " + id);
        }
        return model;
    }

    /**
     * Retrieves the remote control model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the remote control model with such an identifier if it exists, otherwise {@code null}
     */
    @Nullable
    public static RemoteControl.Model rcModel(@DeviceModel.Id int id) {
        return RC_MODELS_BY_ID.get(id);
    }

    /**
     * Retrieves the remote control model with the given identifier.
     *
     * @param id model identifier
     *
     * @return the remote control model with such an identifier
     *
     * @throws IllegalArgumentException if no such remote control model exists
     */
    @NonNull
    public static RemoteControl.Model rcModelOrThrow(@DeviceModel.Id int id) {
        RemoteControl.Model model = RC_MODELS_BY_ID.get(id);
        if (model == null) {
            throw new IllegalArgumentException("Unsupported remote control model id: " + id);
        }
        return model;
    }

    /**
     * Collects identifiers from the given device models.
     *
     * @param models device models to collect identifiers of
     *
     * @return an array of device identifiers
     */
    @DeviceModel.Id
    public static int[] identifiers(@NonNull Collection<DeviceModel> models) {
        @DeviceModel.Id int[] ids = new int[models.size()];
        int i = 0;
        for (DeviceModel model : models) {
            ids[i++] = model.id();
        }
        return ids;
    }

    /**
     * Filters device models that support a given technology.
     *
     * @param models     set of device models to filter
     * @param technology technology that must be supported
     *
     * @return a subset of the given device models that support this technology
     */
    @NonNull
    public static Set<DeviceModel> supportingTechnology(@NonNull Set<DeviceModel> models,
                                                        @NonNull DeviceConnector.Technology technology) {
        Set<DeviceModel> filteredModels = new HashSet<>();
        Predicate<DeviceModel> filter = TECHNOLOGY_FILTERS.get(technology);
        if (filter != null) {
            for (DeviceModel model : models) {
                if (filter.test(model)) {
                    filteredModels.add(model);
                }
            }
        }
        return filteredModels;
    }

    /**
     * Retrieves a device model, given its name.
     *
     * @param name name of the device model to retrieve
     *
     * @return the corresponding {@code DeviceModel}, or {@code null} if no model with such a name exists
     */
    @Nullable
    public static DeviceModel fromName(@NonNull String name) {
        return MODELS_BY_NAME.get(name);
    }

    /** Map of device models, by their associated name. */
    private static final Map<String, DeviceModel> MODELS_BY_NAME;

    /** Map of drone models models, by their model identifier. */
    private static final SparseArray<Drone.Model> DRONE_MODELS_BY_ID;

    /** Map of remote control models models, by their model identifier. */
    private static final SparseArray<RemoteControl.Model> RC_MODELS_BY_ID;

    /** Map of device models filters, by supported technology. */
    private static final Map<DeviceConnector.Technology, Predicate<DeviceModel>> TECHNOLOGY_FILTERS;

    static {
        int droneCount = Drone.Model.values().length, rcCount = RemoteControl.Model.values().length,
                modelCount = droneCount + rcCount;

        HashSet<DeviceModel> allModels = new HashSet<>(modelCount);

        MODELS_BY_NAME = new HashMap<>(modelCount);

        DRONE_MODELS_BY_ID = new SparseArray<>(droneCount);
        for (Drone.Model model : Drone.Model.values()) {
            allModels.add(model);
            MODELS_BY_NAME.put(model.name(), model);
            DRONE_MODELS_BY_ID.put(model.id(), model);
        }

        RC_MODELS_BY_ID = new SparseArray<>(rcCount);
        for (RemoteControl.Model model : RemoteControl.Model.values()) {
            allModels.add(model);
            MODELS_BY_NAME.put(model.name(), model);
            RC_MODELS_BY_ID.put(model.id(), model);
        }

        ALL = Collections.unmodifiableSet(allModels);

        TECHNOLOGY_FILTERS = new EnumMap<>(DeviceConnector.Technology.class);

        TECHNOLOGY_FILTERS.put(DeviceConnector.Technology.USB,
                model -> model == RemoteControl.Model.SKY_CONTROLLER_3
                         || model == RemoteControl.Model.SKY_CONTROLLER_UA);

        TECHNOLOGY_FILTERS.put(DeviceConnector.Technology.WIFI,
                model -> model == Drone.Model.ANAFI_4K
                        || model == Drone.Model.ANAFI_THERMAL
                        || model == Drone.Model.ANAFI_UA
                        || model == Drone.Model.ANAFI_USA);
    }

    /**
     * Private constructor for static utility class.
     */
    private DeviceModels() {
    }
}
