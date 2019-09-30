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
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.session.Session;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Store of components.
 *
 * @param <TYPE> component type (Instrument, PilotingItf or Peripheral)
 */
public class ComponentStore<TYPE> {

    /**
     * Store observer.
     */
    public interface Observer {

        /**
         * Called when the observed component changes.
         */
        void onChange();
    }

    /** Map of components by api class. */
    @NonNull
    private final HashMap<Class<? extends TYPE>, ComponentCore> mComponents;

    /** Map of component listeners, by api class. */
    @NonNull
    private final HashMap<Class<? extends TYPE>, List<Observer>> mComponentObservers;

    /**
     * Constructor.
     */
    public ComponentStore() {
        mComponents = new HashMap<>();
        mComponentObservers = new HashMap<>();
    }

    /**
     * Register a component observer.
     *
     * @param klass    api class of the component to observe
     * @param observer observer to register
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void registerObserver(@NonNull Class<? extends TYPE> klass, @NonNull Observer observer) {
        boolean isFirstObserver = !hasObserver(klass);

        List<Observer> observers = mComponentObservers.get(klass);
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
            mComponentObservers.put(klass, observers);
        }
        observers.add(observer);

        if (isFirstObserver) {
            ComponentCore component = mComponents.get(klass);
            if (component != null) {
                component.onObserved();
            }
        }
    }

    /**
     * Unregister a component observer.
     *
     * @param klass    api class of the component to observe
     * @param observer observer to register
     */
    void unregisterObserver(@NonNull Class<? extends TYPE> klass, @NonNull Observer observer) {
        List<Observer> observers = mComponentObservers.get(klass);
        if (observers != null) {
            observers.remove(observer);
        }

        if (!hasObserver(klass)) {
            ComponentCore component = mComponents.get(klass);
            if (component != null) {
                component.onNoMoreObserved();
            }
        }
    }

    /**
     * Notify all observers that an existing component has been updated.
     * <p>
     * This method notify all direct observers, and all observers of the parents components.
     *
     * @param descriptor descriptor or the component to notify changes
     */
    void notifyUpdated(@NonNull ComponentDescriptor<TYPE, ?> descriptor) {
        if (mComponents.containsKey(descriptor.getApiClass())) {
            notifyChanged(descriptor);
        }
    }

    /**
     * Gets a component.
     *
     * @param session session that will manage refs issued by the component
     * @param klass   api class of the requested component
     * @param <API>   component api class
     *
     * @return the requested component, or null if the component is not in the store
     */
    @Nullable
    public <API extends TYPE> API get(@NonNull Session session, @NonNull Class<API> klass) {
        ComponentCore component = mComponents.get(klass);
        return component == null ? null : klass.cast(component.getProxy(session));
    }

    /**
     * Add a new component to the store.
     *
     * @param component  component to add
     * @param descriptor descriptor of the component to add
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void add(@NonNull ComponentCore component, @NonNull ComponentDescriptor<TYPE, ?> descriptor) {
        ComponentDescriptor<TYPE, ?> desc = descriptor;
        do {
            mComponents.put(desc.getApiClass(), component);
            desc = desc.getParentDescriptor();
        } while (desc != null);

        if (hasObserver(descriptor.getApiClass())) {
            component.onObserved();
        }

        notifyChanged(descriptor);
    }

    /**
     * Remove a component from the store.
     *
     * @param descriptor descriptor of the component to add
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void remove(@NonNull ComponentDescriptor<TYPE, ?> descriptor) {
        ComponentDescriptor<TYPE, ?> desc = descriptor;
        do {
            mComponents.remove(desc.getApiClass());
            desc = desc.getParentDescriptor();
        } while (desc != null);
        notifyChanged(descriptor);
    }

    /**
     * Destroy the store.
     */
    public void destroy() {
        mComponents.clear();
        for (List<Observer> observers : mComponentObservers.values()) {
            for (Observer observer : observers) {
                observer.onChange();
            }
        }
        mComponentObservers.clear();
    }

    /**
     * Checks if at least one observer is registered for a component and its parents.
     *
     * @param klass api class of the component to check
     *
     * @return true if at least one observer has been found
     */
    private boolean hasObserver(@NonNull Class<? extends TYPE> klass) {
        ComponentCore component = mComponents.get(klass);
        if (component != null) {
            // component comes from the store so its descriptor has the correct type
            @SuppressWarnings("unchecked")
            ComponentDescriptor<TYPE, ?> desc = (ComponentDescriptor) component.mDesc;
            do {
                List<Observer> observers = mComponentObservers.get(desc.getApiClass());
                if (observers != null && !observers.isEmpty()) {
                    return true;
                }
                desc = desc.getParentDescriptor();
            } while (desc != null);
        }
        return false;
    }

    /**
     * Notify all observers that a component has been updated.
     * <p>
     * This method notify all direct observers, and all observers of the parents components.
     *
     * @param descriptor descriptor or the component to notify changes
     */
    private void notifyChanged(@NonNull ComponentDescriptor<TYPE, ?> descriptor) {
        ComponentDescriptor<TYPE, ?> desc = descriptor;
        do {
            Class<? extends TYPE> apiClass = desc.getApiClass();
            List<Observer> observers = mComponentObservers.get(apiClass);
            if (observers != null) {
                for (Observer observer : observers) {
                    observer.onChange();
                }
            }
            desc = desc.getParentDescriptor();
        } while (desc != null);
    }
}
