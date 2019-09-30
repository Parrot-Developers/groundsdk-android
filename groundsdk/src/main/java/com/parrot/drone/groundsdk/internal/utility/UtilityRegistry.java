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

package com.parrot.drone.groundsdk.internal.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores utility instances, indexed by their interface name.
 */
public final class UtilityRegistry {

    /** Utility instance, by interface. */
    private final Map<Class<? extends Utility>, Utility> mUtilities;

    /**
     * Constructor.
     */
    public UtilityRegistry() {
        mUtilities = new HashMap<>();
    }

    /**
     * Registers an utility instance.
     *
     * @param utilityClass    utility interface, used to index the utility instance
     * @param utilityInstance utility instance
     * @param <U>             type of the utility interface
     *
     * @return {@code this}, to allow call chaining
     *
     * @throws AssertionError in case an utility with the same interface is already registered
     */
    @NonNull
    public <U extends Utility> UtilityRegistry registerUtility(@NonNull Class<U> utilityClass, U utilityInstance) {
        if (mUtilities.put(utilityClass, utilityInstance) != null) {
            throw new AssertionError("Utility registered multiple time: " + utilityClass);
        }
        return this;
    }

    /**
     * Retrieves an utility interface.
     * <p>
     * Such an utility may not be available at the time of request.
     *
     * @param utilityClass class of the utility interface to obtain
     * @param <U>          type of the utility interface
     *
     * @return the utility with the requested interface if available, otherwise {@code null}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <U extends Utility> U getUtility(@NonNull Class<U> utilityClass) {
        return (U) mUtilities.get(utilityClass);
    }
}

