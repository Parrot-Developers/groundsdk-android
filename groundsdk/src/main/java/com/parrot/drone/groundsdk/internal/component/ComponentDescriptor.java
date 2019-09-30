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

package com.parrot.drone.groundsdk.internal.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A component descriptor.
 *
 * @param <TYPE> type of component (Instrument, PilotingItf or Peripheral)
 * @param <API>  type of the API class of the component
 */
public final class ComponentDescriptor<TYPE, API extends TYPE> {

    /**
     * Makes a new component descriptor with a parent.
     *
     * @param apiClass class defining the component API
     * @param parent   parent descriptor of the component
     * @param <TYPE>   type of component
     * @param <API>    type of the API class of the component
     *
     * @return a new component descriptor
     */
    public static <TYPE, API extends TYPE> ComponentDescriptor<TYPE, API> of(
            @NonNull Class<API> apiClass, @NonNull ComponentDescriptor<TYPE, ? super API> parent) {
        return new ComponentDescriptor<>(apiClass, parent);
    }

    /**
     * Makes a new component descriptor with no parent.
     *
     * @param apiClass class defining the component API
     * @param <TYPE>   type of component
     * @param <API>    type of the API class of the component
     *
     * @return a new component descriptor
     */
    public static <TYPE, API extends TYPE> ComponentDescriptor<TYPE, API> of(
            @NonNull Class<API> apiClass) {
        return new ComponentDescriptor<>(apiClass, null);
    }

    /** Class defining the component API. */
    @NonNull
    private final Class<API> mApiClass;

    /** Parent component descriptor, {@code null} if no parent. */
    @Nullable
    private final ComponentDescriptor<TYPE, ? super API> mParentDescriptor;

    /**
     * Constructor.
     *
     * @param apiClass         class defining the component API
     * @param parentDescriptor parent descriptor of the component
     */
    private ComponentDescriptor(@NonNull Class<API> apiClass,
                                @Nullable ComponentDescriptor<TYPE, ? super API> parentDescriptor) {
        mApiClass = apiClass;
        mParentDescriptor = parentDescriptor;
    }

    /**
     * Gets the class defining the component API.
     *
     * @return component API class
     */
    @NonNull
    Class<API> getApiClass() {
        return mApiClass;
    }

    /**
     * Gets the descriptor of the parent component.
     *
     * @return parent component descriptor
     */
    @Nullable
    ComponentDescriptor<TYPE, ? super API> getParentDescriptor() {
        return mParentDescriptor;
    }
}
