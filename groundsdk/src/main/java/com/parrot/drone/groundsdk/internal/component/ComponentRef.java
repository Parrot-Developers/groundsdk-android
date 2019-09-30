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

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.internal.session.Session;

/**
 * A Reference on a component.
 *
 * @param <TYPE> component type (Instrument, PilotingItf or Peripheral)
 * @param <API>  component api class
 */
public final class ComponentRef<TYPE, API extends TYPE> extends Session.RefBase<API> {

    /** Reference to the component store. */
    private final ComponentStore<TYPE> mComponentStore;

    /** Api class of the component. */
    private final Class<API> mComponentClass;

    /**
     * Constructor.
     *
     * @param session        session that will manage this ref
     * @param observer       observer notified when the component changes
     * @param componentStore component store
     * @param klass          component api class
     */
    public ComponentRef(@NonNull Session session, @NonNull Ref.Observer<API> observer,
                        @NonNull ComponentStore<TYPE> componentStore, @NonNull Class<API> klass) {
        super(session, observer);
        mComponentStore = componentStore;
        mComponentClass = klass;
        mComponentStore.registerObserver(mComponentClass, mObserver);
        init(mComponentStore.get(mSession, klass));
    }

    /**
     * Release the component ref.
     */
    @Override
    protected void release() {
        mComponentStore.unregisterObserver(mComponentClass, mObserver);
        super.release();
    }

    /** Component store observer. */
    @NonNull
    private final ComponentStore.Observer mObserver = new ComponentStore.Observer() {

        @Override
        public void onChange() {
            update(mComponentStore.get(mSession, mComponentClass));
        }
    };

    @NonNull
    @Override
    protected String describeContent() {
        return mComponentClass.getSimpleName();
    }
}
