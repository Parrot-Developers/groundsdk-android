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

import com.parrot.drone.groundsdk.internal.session.Session;

/**
 * Base implementation for a component that directly implements the API specified in its descriptor and returns
 * self when asked for a proxy.
 * <p>
 * Concrete subclasses <strong>MUST</strong> implement the API interface specified in the descriptor they have
 * provided at construction. <br>
 * Otherwise, instantiating the component will produce a ClassCastException at runtime.
 */
public class SingletonComponentCore extends ComponentCore {

    /**
     * Constructor.
     *
     * @param descriptor     specific descriptor of the provided component
     * @param componentStore store where this component provider belongs
     * @param <TYPE>         type of component
     * @param <API>          type of the API class of the component
     */
    protected <TYPE, API extends TYPE> SingletonComponentCore(
            @NonNull ComponentDescriptor<TYPE, API> descriptor, @NonNull ComponentStore<TYPE> componentStore) {
        super(descriptor, componentStore);
        // fail early if the singleton component does not implement the API in the descriptor
        descriptor.getApiClass().cast(this);
    }

    @Override
    @NonNull
    protected final Object getProxy(@NonNull Session session) {
        return this;
    }
}
