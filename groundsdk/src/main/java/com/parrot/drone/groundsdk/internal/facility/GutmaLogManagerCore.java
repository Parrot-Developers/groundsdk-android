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

package com.parrot.drone.groundsdk.internal.facility;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.GutmaLogManager;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

/**
 * Core class for the {@code GutmaLogManager} facility.
 */
public final class GutmaLogManagerCore extends SingletonComponentCore implements GutmaLogManager {

    /** Description of GutmaLogManager. */
    private static final ComponentDescriptor<Facility, GutmaLogManager> DESC =
            ComponentDescriptor.of(GutmaLogManager.class);

    /** Engine-specific backend for the GutmaLogManager. */
    public interface Backend {

        /**
         * Requests deletion of a GUTMA log file.
         *
         * @param gutmaLogFile GUTMA log file to delete
         *
         * @return {@code true} if the file did exist and was deleted, otherwise {@code false}
         */
        boolean delete(@NonNull File gutmaLogFile);
    }


    /** Engine facility backend. */
    @NonNull
    private final Backend mBackend;

    /** List of GUTMA log files. */
    @NonNull
    private final Set<File> mFiles;

    /**
     * Constructor.
     *
     * @param facilityStore store where this facility belongs
     * @param backend       backend used to forward actions to the engine
     */
    public GutmaLogManagerCore(@NonNull ComponentStore<Facility> facilityStore, @NonNull Backend backend) {
        super(DESC, facilityStore);
        mBackend = backend;
        mFiles = new HashSet<>();
    }

    @NonNull
    @Override
    public Set<File> files() {
        return Collections.unmodifiableSet(mFiles);
    }

    @Override
    public boolean delete(@NonNull File file) {
        return mFiles.contains(file) && mBackend.delete(file);
    }

    /**
     * Updates the list of available GUTMA log files.
     *
     * @param files available GUTMA log files list
     *
     * @return {@code this}, to allow call chaining
     */
    public GutmaLogManagerCore updateFiles(@NonNull Collection<File> files) {
        mChanged |= mFiles.retainAll(files) | mFiles.addAll(files);
        return this;
    }
}
