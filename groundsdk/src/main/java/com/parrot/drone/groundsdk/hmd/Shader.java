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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.parrot.drone.groundsdk.internal.io.Files;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES30.*;

/**
 * Helper functions for loading GL shaders from resource files.
 */
final class Shader {

    /**
     * Supported shader types.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GL_VERTEX_SHADER, GL_FRAGMENT_SHADER})
    @interface Type {}

    /**
     * Describes a shader resource.
     */
    static final class Descriptor {

        /** Shader type. */
        @Type
        private final int mType;

        /** Shader source raw resource id. */
        @RawRes
        private final int mResId;

        /**
         * Constructor.
         *
         * @param type  shader type
         * @param resId shader source raw resource id
         */
        private Descriptor(@Type int type, @RawRes int resId) {
            mType = type;
            mResId = resId;
        }
    }

    /**
     * Creates a shader descriptor.
     *
     * @param shaderType  shader type
     * @param shaderResId shader source raw resource id
     *
     * @return a new {@code Shader.Descriptor} instance
     */
    @NonNull
    static Descriptor descriptor(@Type int shaderType, @RawRes int shaderResId) {
        return new Descriptor(shaderType, shaderResId);
    }

    /**
     * Compiles and links multiple shader sources defined in android raw resources into a single GL program.
     *
     * @param resources         android resources
     * @param shaderDescriptors descriptors specifying each shader type and source code location in raw resources
     *
     * @return a GL handle to the linked program
     *
     * @throws RuntimeException in case shader source could not be loaded from resources
     */
    static int makeProgramFromResources(@NonNull Resources resources, @NonNull Descriptor... shaderDescriptors) {
        int glProgram = glCreateProgram();

        List<Integer> glShaders = new ArrayList<>();

        for (Descriptor descriptor : shaderDescriptors) {
            int glShader = compileFromResource(descriptor.mType, resources, descriptor.mResId);
            glAttachShader(glProgram, glShader);
            glShaders.add(glShader);
        }

        glLinkProgram(glProgram);

        for (int glShader : glShaders) {
            glDeleteShader(glShader);
        }

        return glProgram;
    }

    /**
     * Compiles a shader from a raw resource file containing its source code.
     *
     * @param shaderType  type of shader to compile
     * @param resources   android resources
     * @param shaderResId shader source raw resource id
     *
     * @return a GL handle to the compiled shader
     *
     * @throws RuntimeException in case shader source could not be loaded from resources
     */
    private static int compileFromResource(@Type int shaderType, @NonNull Resources resources,
                                           @RawRes int shaderResId) {
        String shaderSrc;
        try {
            shaderSrc = Files.readRawResourceAsString(resources, shaderResId, StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to load shader [res: "
                                       + resources.getResourceName(shaderResId) + "]", e);
        }

        int glShader = glCreateShader(shaderType);
        glShaderSource(glShader, shaderSrc);
        glCompileShader(glShader);

        return glShader;
    }

    /**
     * Private constructor for static utility class.
     */
    private Shader() {
    }
}
