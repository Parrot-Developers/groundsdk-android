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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.persistence.Converter;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraPhotoSettingCore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A specific {@code StorageEntry} implementation for collections of {@code CameraPhotoSettingCore.Capability}.
 */
final class PhotoCapabilitiesStorageEntry extends StorageEntry<Collection<CameraPhotoSettingCore.Capability>> {

    /** Capability modes key. */
    private static final String KEY_MODES = "modes";

    /** Capability formats key. */
    private static final String KEY_FORMATS = "formats";

    /** Capability file formats key. */
    private static final String KEY_FILE_FORMATS = "fileFormats";

    /** Capability HDR key. */
    private static final String KEY_HDR = "hdr";

    /**
     * Constructor.
     *
     * @param key storage key
     */
    PhotoCapabilitiesStorageEntry(@NonNull String key) {
        super(key);
    }

    @NonNull
    @Override
    protected Collection<CameraPhotoSettingCore.Capability> parse(@NonNull Object serializedObject) {
        return Converter.parseCollection((JSONArray) serializedObject, ArrayList::new,
                PhotoCapabilitiesStorageEntry::parseCapability);
    }

    @NonNull
    @Override
    protected JSONArray serialize(@NonNull Collection<CameraPhotoSettingCore.Capability> capabilities) {
        return Converter.serializeCollection(capabilities, PhotoCapabilitiesStorageEntry::serializeCapability);
    }

    /**
     * Parses a JSONObject representation of a {@code CameraPhotoSettingCore.Capability}.
     *
     * @param serializedCapability JSONObject representation of the capability to parse
     *
     * @return the corresponding capability
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    private static CameraPhotoSettingCore.Capability parseCapability(@NonNull JSONObject serializedCapability) {
        try {
            return CameraPhotoSettingCore.Capability.of(
                    Converter.parseEnumSet(serializedCapability.getJSONArray(KEY_MODES), CameraPhoto.Mode.class),
                    Converter.parseEnumSet(serializedCapability.getJSONArray(KEY_FORMATS), CameraPhoto.Format.class),
                    Converter.parseEnumSet(serializedCapability.getJSONArray(KEY_FILE_FORMATS),
                            CameraPhoto.FileFormat.class),
                    serializedCapability.getBoolean(KEY_HDR));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serializes a {@code CameraPhotoSettingCore.Capability}.
     *
     * @param capability capability to serialize
     *
     * @return a JSONObject representation of the given capability
     */
    @NonNull
    private static JSONObject serializeCapability(@NonNull CameraPhotoSettingCore.Capability capability) {
        try {
            return new JSONObject()
                    .put(KEY_MODES, Converter.serializeEnumSet(capability.mModes))
                    .put(KEY_FORMATS, Converter.serializeEnumSet(capability.mFormats))
                    .put(KEY_FILE_FORMATS, Converter.serializeEnumSet(capability.mFileFormats))
                    .put(KEY_HDR, capability.mHdrAvailable);
        } catch (JSONException e) {
            throw new AssertionError(e); // never happens
        }
    }
}
