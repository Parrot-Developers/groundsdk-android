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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.session.Session;

/**
 * Base for a component implementation class.
 * <p>
 * Concrete subclass <strong>MUST</strong> implement getProxy method to provide an implementation of the API interface
 * that is specified in the descriptor they provide. <br>
 * Otherwise, getting the component will produce a ClassCastException at runtime.
 */
public abstract class ComponentCore {

    /** An interface for receiving change notifications. */
    public interface ChangeListener {

        /** Called back when some change occurred. */
        void onChange();
    }

    /** Component descriptor for the interface. Provided by concrete subclasses. */
    @NonNull
    final ComponentDescriptor<?, ?> mDesc;

    /** Component store where this component belongs. */
    @NonNull
    private final ComponentStore<?> mComponentStore;

    /** Has pending changes waiting for {@link #notifyUpdated()} call. */
    protected boolean mChanged;

    /** {@code true} when the component is currently published. */
    private boolean mPublished;

    /**
     * Constructor.
     *
     * @param descriptor     specific descriptor of the provided component
     * @param componentStore store where this component provider belongs
     * @param <TYPE>         type of component
     */
    protected <TYPE> ComponentCore(@NonNull ComponentDescriptor<TYPE, ?> descriptor,
                                   @NonNull ComponentStore<TYPE> componentStore) {

        mDesc = descriptor;
        mComponentStore = componentStore;
    }

    /**
     * Publishes the component in the store.
     */
    @SuppressWarnings("unchecked") // constructor ensures that descriptor and store have same TYPE
    @CallSuper
    public void publish() {
        if (mPublished) {
            notifyUpdated();
        } else {
            mComponentStore.add(this, (ComponentDescriptor) mDesc);
            mPublished = true;
            mChanged = false;
        }
    }

    /**
     * Unpublishes the component from the store.
     */
    @SuppressWarnings("unchecked") // constructor ensures that descriptor and store have same TYPE
    @CallSuper
    public void unpublish() {
        if (mPublished) {
            mComponentStore.remove((ComponentDescriptor) mDesc);
            mPublished = mChanged = false;
        }
    }

    /**
     * Notifies changes made by previously called setters.
     */
    @SuppressWarnings("unchecked") // constructor ensures that descriptor and store have same TYPE
    public final void notifyUpdated() {
        if (mChanged) {
            mChanged = false;
            onUpdate();
            mComponentStore.notifyUpdated((ComponentDescriptor) mDesc);
        }
    }

    /**
     * Tells whether the component is currently published.
     *
     * @return {@code true} if the component is published, otherwise {@code false}
     */
    public final boolean isPublished() {
        return mPublished;
    }

    /**
     * Marks that a change occurred on the component.
     */
    protected final void onChange() {
        mChanged = true;
    }

    /**
     * Called before a component change gets notified to the application.
     * <p>
     * Subclasses may override this method to notify their own internal listeners. <br>
     * Default implementation does nothing.
     */
    @SuppressWarnings("WeakerAccess")
    protected void onUpdate() {
    }

    /**
     * Called when the component becomes observed, i.e. when a first observer is registered for the component.
     * <p>
     * Subclasses may override this method to start internal tasks that are consuming resources. <br>
     * Default implementation does nothing.
     */
    protected void onObserved() {

    }

    /**
     * Called when the component is no more observed, i.e. when the last observer is unregistered for the component.
     * <p>
     * Subclasses may override this method to stop internal tasks that are consuming resources. <br>
     * Default implementation does nothing.
     */
    protected void onNoMoreObserved() {

    }

    /**
     * Get a proxy of the component for the provided session.
     * <p>
     * The returned Object <strong>MUST</strong> implement the API interface specified in the descriptor the
     * implementing subclass has provided at construction.
     *
     * @param session the session that will manage refs issued by the proxy
     *
     * @return a proxy for the component, that implements the descriptor's API
     */
    @NonNull
    protected abstract Object getProxy(@NonNull Session session);
}
