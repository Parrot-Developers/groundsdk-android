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

package com.parrot.drone.groundsdk.hmd;

import android.content.res.Resources;
import android.util.SizeF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.parrot.drone.groundsdk.hmd.HmdModel.DataPack;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HMD;

/**
 * An HMD data pack loaded from an android raw resource.
 */
final class ResourceHmdModelPack implements DataPack {

    /** File magic. */
    private static final byte[] MAGIC = "Parrot HMD Data Pack".getBytes(StandardCharsets.UTF_8);

    /** HMD data pack supported version. */
    private static final byte VERSION = 1;

    /** Gson generic type to deserialize model record map. */
    private static final Type MODELS_MAP_TYPE = new TypeToken<Map<String, ModelRecord>>() {}.getType();

    /** Android resources. */
    @NonNull
    private final Resources mResources;

    /** Pack resource identifier. */
    @RawRes
    private final int mPackId;

    /** Loaded models, by identifier. */
    @NonNull
    private final Map<String, Model> mModels;

    /** Offset where binary data (GL buffers) starts in the data pack file. */
    private final long mBinaryDataOffset;

    /**
     * Constructor.
     *
     * @param resources android resources
     * @param resId     raw resource to load the pack from
     */
    ResourceHmdModelPack(@NonNull Resources resources, @RawRes int resId) {
        mResources = resources;
        mPackId = resId;

        int jsonSize = 0;
        Map<String, Model> models = Collections.emptyMap();
        try (DataInputStream src = new DataInputStream(resources.openRawResource(resId))) {
            byte[] magicBuffer = new byte[MAGIC.length];
            src.readFully(magicBuffer);
            if (!Arrays.equals(magicBuffer, MAGIC)) {
                throw new IOException("Bad magic: " + new String(magicBuffer, StandardCharsets.UTF_8));
            }

            byte version = src.readByte();
            if (version != VERSION) {
                throw new IOException("Unsupported version: " + version);
            }

            byte[] jsonBuffer = new byte[src.readInt()];
            src.readFully(jsonBuffer);
            jsonSize = jsonBuffer.length;

            models = new Gson()
                    .<Map<String, ModelRecord>>fromJson(new String(jsonBuffer, StandardCharsets.UTF_8), MODELS_MAP_TYPE)
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Model model = null;
                        try {
                            ModelRecord record = entry.getValue();
                            if (record == null) {
                                throw new JsonParseException("Empty record");
                            }
                            model = new Model(entry.getKey(), record);
                        } catch (JsonParseException e) {
                            ULog.e(TAG_HMD, "Invalid model definition [id: " + entry.getKey()
                                            + ", resource:" + resources.getResourceName(resId) + "]", e);
                        }

                        return model;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Model::id, Function.identity()));
        } catch (IOException | JsonSyntaxException e) {
            ULog.e(TAG_HMD, "Invalid HMD data pack file [resource: " + resources.getResourceName(resId) + "]", e);
        }

        mBinaryDataOffset = MAGIC.length + Byte.BYTES + Integer.BYTES + jsonSize;
        mModels = models;
    }

    @NonNull
    @Override
    public HmdModel loadModel(@NonNull String id) {
        HmdModel model = mModels.get(id);
        if (model == null) {
            throw new IllegalArgumentException("Model not found [model: " + id
                                               + ", resource: " + mResources.getResourceName(mPackId) + "]");
        }
        return model;
    }

    @NonNull
    @Override
    public Set<String> listModels() {
        return Collections.unmodifiableSet(mModels.keySet());
    }

    /**
     * Implementation for HMD Model coming from resources HMD data packs.
     */
    private class Model implements HmdModel {

        /** Model identifier. */
        @NonNull
        private final String mId;

        /** Model display name. */
        @NonNull
        private final String mName;

        /** Projection mesh size, in millimeters. */
        private final float mMeshSize;

        /** Maximum allowed lens render size. Width and height in millimeters. */
        @NonNull
        private final SizeF mMaxRenderSize;

        /** Default interpupillary distance, in millimeters. */
        private final float mDefaultIpd;

        /** Offset for mesh positions GL buffer in resource file. */
        private final long mMeshPositionsOffset;

        /** Size of mesh positions GL buffer, in bytes. */
        private final int mMeshPositionsSize;

        /** Offset for texture coordinates GL buffer in resource file. */
        private final long mTexCoordsOffset;

        /** Size of texture coordinates GL buffer, in bytes. */
        private final int mTexCoordsSize;

        /** Offset for drawing indices GL buffer in resource file. */
        private final long mIndicesOffset;

        /** Size of drawing indices GL buffer, in bytes. */
        private final int mIndicesSize;

        /** Offset for color filter GL buffer in resource file. */
        private final long mColorFilterOffset;

        /** Size of color filter GL buffer, in bytes. */
        private final int mColorFilterSize;

        /** Chromatic aberration correction factors. */
        @NonNull
        private final ChromaCorrection mChromaCorrection;

        /**
         * Constructor.
         *
         * @param modelId model identifier
         * @param record  model JSON record
         *
         * @throws JsonParseException when record validation fails
         */
        Model(@NonNull String modelId, @NonNull ModelRecord record) throws JsonParseException {
            mId = modelId;

            mName = record.name == null ? mId : record.name;

            mMeshSize = require("meshSize", record.meshSize);

            float maxRenderWidth = Float.MAX_VALUE;
            float maxRenderHeight = Float.MAX_VALUE;
            if (record.maxRenderSize != null) {
                if (record.maxRenderSize.width != null) {
                    maxRenderWidth = record.maxRenderSize.width;
                }
                if (record.maxRenderSize.height != null) {
                    maxRenderHeight = record.maxRenderSize.height;
                }
            }

            mMaxRenderSize = new SizeF(maxRenderWidth, maxRenderHeight);

            require("ipd", record.ipd);
            mDefaultIpd = require("ipd.def", record.ipd.def);

            require("meshPositions", record.meshPositions);
            mMeshPositionsOffset = require("meshPositions.offset", record.meshPositions.offset);
            mMeshPositionsSize = require("meshPositions.size", record.meshPositions.size);

            require("texCoords", record.texCoords);
            mTexCoordsOffset = require("texCoords.offset", record.texCoords.offset);
            mTexCoordsSize = require("texCoords.size", record.texCoords.size);

            require("indices", record.indices);
            mIndicesOffset = require("indices.offset", record.indices.offset);
            mIndicesSize = require("indices.size", record.indices.size);

            if (record.colorFilter == null) {
                mColorFilterOffset = mColorFilterSize = -1;
            } else {
                mColorFilterOffset = require("colorFilter.offset", record.colorFilter.offset);
                mColorFilterSize = require("colorFilter.size", record.colorFilter.size);
            }

            mChromaCorrection = new ChromaCorrection() {

                /** Red channel correction. */
                final float mRed;

                /** Green channel correction. */
                final float mGreen;

                /** Blue channel correction. */
                final float mBlue;

                {
                    if (record.chromaCorrection == null) {
                        mRed = mGreen = mBlue = 1;
                    } else {
                        mRed = record.chromaCorrection.r == null ? 1 : record.chromaCorrection.r;
                        mGreen = record.chromaCorrection.g == null ? 1 : record.chromaCorrection.g;
                        mBlue = record.chromaCorrection.b == null ? 1 : record.chromaCorrection.b;
                    }
                }

                @Override
                public float red() {
                    return mRed;
                }

                @Override
                public float green() {
                    return mGreen;
                }

                @Override
                public float blue() {
                    return mBlue;
                }
            };
        }

        /**
         * Gives this model unique identifier.
         *
         * @return model identifier
         */
        @NonNull
        public String id() {
            return mId;
        }

        @NonNull
        @Override
        public String name() {
            return mName;
        }

        @Override
        public float meshSize() {
            return mMeshSize;
        }

        @NonNull
        @Override
        public SizeF maxRenderSize() {
            return mMaxRenderSize;
        }

        @Override
        public float defaultIpd() {
            return mDefaultIpd;
        }

        @NonNull
        @Override
        public ChromaCorrection chromaCorrection() {
            return mChromaCorrection;
        }

        @Nullable
        @Override
        public ByteBuffer loadMeshPositions() {
            return loadFloatBuffer(mMeshPositionsOffset, mMeshPositionsSize);
        }

        @Nullable
        @Override
        public ByteBuffer loadTexCoords() {
            return loadFloatBuffer(mTexCoordsOffset, mTexCoordsSize);
        }

        @Nullable
        @Override
        public ByteBuffer loadIndices() {
            return loadIntBuffer(mIndicesOffset, mIndicesSize);
        }

        @Nullable
        @Override
        public ByteBuffer loadColorFilter() {
            return mColorFilterOffset == -1 || mColorFilterSize == -1 ? null : loadFloatBuffer(mColorFilterOffset,
                    mColorFilterSize);
        }

        /**
         * Loads an integer buffer from the pack.
         * <p>
         * Stored data is expected to have little endian ordering.
         *
         * @param offset offset in resource file where the buffer starts
         * @param length buffer length, in bytes
         *
         * @return a byte buffer containing loaded data, with native endianness, or null in case loading failed
         */
        @Nullable
        private ByteBuffer loadIntBuffer(long offset, int length) {
            ByteBuffer buffer = loadRawBuffer(offset, length);
            if (buffer != null && ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                buffer.order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(buffer.duplicate().asIntBuffer());
            }
            return buffer;
        }

        /**
         * Loads a float buffer from the pack.
         * <p>
         * Stored data is expected to have little endian ordering.
         *
         * @param offset offset in resource file where the buffer starts
         * @param length buffer length, in bytes
         *
         * @return a byte buffer containing loaded data, with native endianness, or null in case loading failed
         */
        @Nullable
        private ByteBuffer loadFloatBuffer(long offset, int length) {
            ByteBuffer buffer = loadRawBuffer(offset, length);
            if (buffer != null && ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                buffer.order(ByteOrder.BIG_ENDIAN).asFloatBuffer().put(buffer.duplicate().asFloatBuffer());
            }
            return buffer;
        }

        /**
         * Loads a buffer from the pack.
         * <p>
         * Stored data is expected to have little endian ordering.
         *
         * @param offset offset in resource file where the buffer starts
         * @param length buffer length, in bytes
         *
         * @return a byte buffer containing loaded data, or null in case loading failed
         */
        @Nullable
        private ByteBuffer loadRawBuffer(long offset, int length) {
            ByteBuffer buffer = null;
            try (InputStream src = mResources.openRawResource(mPackId)) {
                long toSkip = mBinaryDataOffset + offset;
                if (src.skip(toSkip) != toSkip) {
                    throw new EOFException();
                }
                byte[] data = new byte[length];
                int stored = 0;
                while (stored < length) {
                    int read = src.read(data, stored, length - stored);
                    if (read < 0) {
                        throw new EOFException();
                    }
                    stored += read;
                }
                buffer = ByteBuffer.wrap(data);
            } catch (IOException e) {
                ULog.e(TAG_HMD, "Failed to load HMD data buffer  [resource: "
                                + mResources.getResourceName(mPackId) + ", offset: " + offset
                                + "length: " + length + "]", e);
            }
            return buffer;
        }
    }

    /**
     * Requires a field to be non-{@code null}.
     *
     * @param field name of the checked field, used to build meaningful error message the case being
     * @param value actual field value (to be checked not being {@code null})
     *
     * @throws JsonParseException in case {@code value} is {@code null}
     */
    @NonNull
    private static <T> T require(@NonNull String field, @Nullable T value) {
        if (value == null) {
            throw new JsonParseException("Missing required '" + field + "' field");
        }
        return value;
    }

    /**
     * HMD model JSON record, as expected when parsing the pack resource.
     * <p>
     * All fields may be {@code null}; mandatory fields need to be validated before further use.
     */
    private static final class ModelRecord {

        /** Model display name. Optional field */
        @Nullable
        String name;

        /** Projection mesh size, in millimeters. Required field. */
        @Nullable
        Float meshSize;

        /** A size, with a width and a height. */
        static final class Size {

            /** Width. */
            @Nullable
            Float width;

            /** Height. */
            @Nullable
            Float height;
        }

        /** Lens maximum render size. Values in millimeters. Optional field. Width and height are each also optional. */
        @Nullable
        Size maxRenderSize;

        /** Interpupillary distance default value and range. */
        static final class Ipd {

            /** Default interpupillary distance, in millimeters. Required field. */
            @SerializedName("default")
            Float def;

            /** Minimal interpupillary distance, in millimeters. Optional field. */
            @Nullable
            Float min;

            /** Maximal interpupillary distance, in millimeters. Optional field. */
            @Nullable
            Float max;
        }

        /** Interpupillary distance config. Required field. */
        @Nullable
        Ipd ipd;

        /** Offset and size information on a GL buffer. */
        static final class Buffer {

            /** Buffer offset in pack file. */
            @Nullable
            Long offset;

            /** Buffer size in bytes. */
            @Nullable
            Integer size;
        }

        /** Mesh position GL buffer index. Required field. */
        @Nullable
        Buffer meshPositions;

        /** Texture coordinates GL buffer index. Required field. */
        @Nullable
        Buffer texCoords;

        /** Drawing indices GL buffer index. Required field. */
        @Nullable
        Buffer indices;

        /** Color filter GL buffer index. Optional field. */
        @Nullable
        Buffer colorFilter;

        /** Chromatic aberration correction factors. */
        static final class ChromaCorrection {

            /** Red color channel correction factor. Optional field. */
            @Nullable
            Float r;

            /** Green color channel correction factor. Optional field. */
            @Nullable
            Float g;

            /** Blue color channel correction factor. Optional field. */
            @Nullable
            Float b;
        }

        /** Chromatic aberration correction factors. Optional field. */
        @Nullable
        ChromaCorrection chromaCorrection;
    }
}
