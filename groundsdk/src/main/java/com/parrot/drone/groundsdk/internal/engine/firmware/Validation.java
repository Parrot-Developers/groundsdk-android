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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonParseException;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FIRMWARE;

/**
 * Utility class for validating parsed JSON firmware data.
 */
final class Validation {

    /**
     * Requires a field to be non-{@code null}.
     *
     * @param field name of the checked field, used to build meaningful error message the case being
     * @param value actual field value (to be checked not being {@code null})
     *
     * @throws JsonParseException in case {@code value} is {@code null}
     */
    static void require(@NonNull String field, @Nullable Object value) {
        if (value == null) {
            throw new JsonParseException("Missing required '" + field + "' field");
        }
    }

    /**
     * Validates a product identifier string and returns a corresponding {@code DeviceModel}.
     *
     * @param field     name of the field to be validated, used to build meaningful error message the case being
     * @param productId product identifier to validate
     *
     * @return the corresponding {@code DeviceModel}
     *
     * @throws JsonParseException in case the product identifier failed to be parsed as a {@code DeviceModel}
     */
    @NonNull
    static DeviceModel validateModel(@NonNull String field, @NonNull String productId) {
        try {
            return DeviceModels.modelOrThrow(Integer.parseInt(productId, 16));
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid value for '" + field + "' field: " + productId);
        }
    }

    /**
     * Validates a version string and returns a corresponding {@code FirmwareVersion}.
     *
     * @param field      name of the field to be validated, used to build meaningful error message the case being
     * @param versionStr version string to validate
     *
     * @return the corresponding {@code FirmwareVersion}
     *
     * @throws JsonParseException in case the version string failed to be parsed as a {@code FirmwareVersion}
     */
    @NonNull
    static FirmwareVersion validateVersion(@NonNull String field, @NonNull String versionStr) {
        FirmwareVersion version = FirmwareVersion.parse(versionStr);
        if (version == null) {
            throw new JsonParseException("Invalid value for '" + field + "' field: " + versionStr);
        }
        return version;
    }

    /**
     * Validates an URI string and returns a corresponding {@code URI}.
     *
     * @param field          name of the field to be validated, used to build meaningful error message the case being
     * @param allowedSchemes allowed URI schemes
     * @param uriStr         URI string to validate
     *
     * @return the corresponding {@code URI}
     *
     * @throws JsonParseException in case the URI string failed to be parsed as a {@code URI}
     */
    @NonNull
    static URI validateUri(@NonNull String field, @NonNull Set<String> allowedSchemes, @NonNull String uriStr) {
        URI uri;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new JsonParseException("Invalid value for '" + field + "' field: " + uriStr);
        }
        if (!allowedSchemes.contains(uri.getScheme())) {
            throw new JsonParseException("Invalid scheme for '" + field + "' field: " + uriStr);
        }
        if (TextUtils.isEmpty(uri.getPath())) {
            throw new JsonParseException("Invalid path for '" + field + "' field: " + uriStr);
        }
        return uri;
    }

    /**
     * Validates a firmware file size not being negative.
     *
     * @param field name of the field to be validated, used to build meaningful error message the case being
     * @param size  size value to validate
     *
     * @return the size, always positive or zero
     *
     * @throws JsonParseException in case the size is negative
     */
    @IntRange(from = 0)
    static long validateSize(@NonNull String field, long size) {
        if (size < 0) {
            throw new JsonParseException("Invalid value for '" + field + "' field: " + size);
        }
        return size;
    }

    /**
     * Validates a set of attribute string and returns a corresponding set of {@code FirmwareInfo.Attribute>}.
     *
     * @param field         name of the field to be validated, used to build meaningful error message the case being
     * @param flags         set of attribute strings to validate
     * @param failIfUnknown if {@code true} then unknown flag strings will fail validation, otherwise they are simply
     *                      dropped
     *
     * @return the corresponding set of {@code FirmwareInfo.Attribute>}
     *
     * @throws JsonParseException in case the set of attribute strings failed to be parsed as a set of
     *                            {@code FirmwareInfo.Attribute>}
     */
    @NonNull
    static EnumSet<FirmwareInfo.Attribute> validateAttributes(@NonNull String field, @NonNull Set<String> flags,
                                                              boolean failIfUnknown) {
        EnumSet<FirmwareInfo.Attribute> attributes = EnumSet.noneOf(FirmwareInfo.Attribute.class);
        for (String flag : flags) {
            if (flag == null) {
                throw new JsonParseException("Null value in '" + field + "' field: " + flags);
            }
            FirmwareInfo.Attribute attribute = null;
            try {
                attribute = FirmwareInfo.Attribute.valueOf(flag);
            } catch (IllegalArgumentException e) {
                switch (flag) {
                    case "delete_user_data": // http server value
                        attribute = FirmwareInfo.Attribute.DELETES_USER_DATA;
                }
            }
            if (attribute != null) {
                attributes.add(attribute);
            } else if (failIfUnknown) {
                throw new JsonParseException("Invalid value in '" + field + "' field: " + flag);
            } else if (ULog.w(TAG_FIRMWARE)) {
                ULog.w(TAG_FIRMWARE, "Invalid value in '" + field + "' field: " + flag);
            }
        }
        return attributes;
    }

    /**
     * Private constructor for static utility class.
     */
    private Validation() {
    }
}
