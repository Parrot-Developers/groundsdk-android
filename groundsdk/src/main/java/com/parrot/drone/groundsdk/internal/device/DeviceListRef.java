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

package com.parrot.drone.groundsdk.internal.device;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.session.Session;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A reference to a list of devices.
 *
 * @param <ENTRY>  type of entries in the list
 * @param <DEVICE> type of devices the list entries represent
 */
public final class DeviceListRef<ENTRY, DEVICE extends DeviceCore> extends Session.RefBase<List<ENTRY>> {

    /** Device store owning the device list. */
    @NonNull
    private final DeviceStore<DEVICE> mDeviceStore;

    /** Device list filter. */
    @NonNull
    private final Predicate<? super ENTRY> mFilter;

    /** Factory used to build list entries from devices. */
    @NonNull
    private final Function<DEVICE, ENTRY> mEntryFactory;

    /**
     * Constructor.
     *
     * @param session      session that will manage this ref
     * @param observer     observer notified when the list changes
     * @param deviceStore  device store
     * @param entryFactory factory used to build list entry from devices
     * @param filter       filter to apply to include a device into the list
     */
    public DeviceListRef(@NonNull Session session, @NonNull Observer<List<ENTRY>> observer,
                         @NonNull DeviceStore<DEVICE> deviceStore,
                         @NonNull Function<DEVICE, ENTRY> entryFactory,
                         @NonNull Predicate<? super ENTRY> filter) {
        super(session, observer);
        mDeviceStore = deviceStore;
        mEntryFactory = entryFactory;
        mFilter = filter;
        mDeviceStore.monitorWith(mStoreMonitor);
        // build the initial list, filtered from the store
        init(mDeviceStore.all().stream()
                         .map(entryFactory)
                         .filter(filter)
                         .collect(Collectors.collectingAndThen(Collectors.toList(),
                                 Collections::unmodifiableList)));
    }

    @Override
    protected void release() {
        mDeviceStore.disposeMonitor(mStoreMonitor);
        super.release();
    }

    /**
     * Gets a copy of the current entry list. Must only be called after init() when the internal list is
     * guaranteed to be non-null.
     *
     * @return a copy of the current entry list
     */
    @NonNull
    private List<ENTRY> copyList() {
        List<ENTRY> currentList = get();
        assert currentList != null;
        return new ArrayList<>(currentList);
    }

    /** Device store listener. */
    private final DeviceStore.Monitor<DEVICE> mStoreMonitor = new DeviceStore.Monitor<DEVICE>() {

        @Override
        public void onDeviceAdded(@NonNull DEVICE device) {
            // see if the device passes the filter
            ENTRY entry = mEntryFactory.apply(device);
            if (mFilter.test(entry)) {
                List<ENTRY> currentList = copyList();
                // add the entry
                currentList.add(entry);
                // publish the list
                update(Collections.unmodifiableList(currentList));
            }
        }

        @Override
        public void onDeviceChanged(@NonNull DEVICE device) {
            List<ENTRY> currentList = copyList();
            // make an entry for the corresponding device
            ENTRY entry = mEntryFactory.apply(device);
            // see if the filter accept it
            boolean accepted = mFilter.test(entry);
            // try to find a matching entry in the current list
            int index = currentList.indexOf(entry);
            if (accepted && index == -1) {
                // entry not in list and filter-accepted, add entry
                currentList.add(entry);
            } else if (accepted) {
                // entry in list and filter-accepted, update entry
                currentList.set(index, entry);
            } else if (index != -1) {
                // entry in list and filter-refused, remove entry
                currentList.remove(index);
            }
            // if an entry was updated/added or removed, publish list update
            if (accepted || index != -1) {
                update(Collections.unmodifiableList(currentList));
            }
        }

        @Override
        public void onDeviceRemoved(@NonNull DEVICE device) {
            List<ENTRY> currentList = copyList();
            // remove entry from list if present
            if (currentList.remove(mEntryFactory.apply(device))) {
                update(Collections.unmodifiableList(currentList));
            }
        }
    };
}
