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

package com.parrot.drone.groundsdk.internal;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbAccessory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.BuildConfig;
import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.DroneListEntry;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.RemoteControlListEntry;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.component.ComponentRef;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.device.DeviceListRef;
import com.parrot.drone.groundsdk.internal.device.DeviceRemovedListenerRef;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.DroneListEntryCore;
import com.parrot.drone.groundsdk.internal.device.DroneProxy;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlListEntryCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlProxy;
import com.parrot.drone.groundsdk.internal.engine.EnginesController;
import com.parrot.drone.groundsdk.internal.session.Session;
import com.parrot.drone.groundsdk.internal.session.SessionManager;
import com.parrot.drone.groundsdk.internal.stream.FileReplayRef;
import com.parrot.drone.groundsdk.internal.stream.FileSourceCore;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RcUsbAccessoryManager;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;
import com.parrot.drone.groundsdk.stream.FileReplay;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_API;

/**
 * SDK entry point. This class is a singleton.
 */
public class GroundSdkCore {

    /** Singleton instance. */
    @SuppressLint("StaticFieldLeak")
    @Nullable
    private static GroundSdkCore sInstance;

    /**
     * Singleton instance factory.
     *
     * @param context android context
     *
     * @return GroundSdkCore singleton instance
     */
    public static GroundSdkCore get(@NonNull Context context) {
        synchronized (GroundSdkCore.class) {
            if (sInstance == null) {
                Application application = (Application) context.getApplicationContext();
                ApplicationNotifier.setDefault(application);
                ApplicationStorageProvider.setDefault(application);
                sInstance = new GroundSdkCore(application);
            }
            return sInstance;
        }
    }

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** Session manager. */
    @NonNull
    private final SessionManager mSessionManager;

    /** Drone store. */
    @NonNull
    private final DeviceStoreCore.Drone mDroneStore;

    /** Remote Control store. */
    @NonNull
    private final DeviceStoreCore.RemoteControl mRemoteControlStore;

    /** Facility store. */
    @NonNull
    private final ComponentStore<Facility> mFacilityStore;

    /** Utility registry. */
    @NonNull
    private final UtilityRegistry mUtilities;

    /** Engine controller managing all implementation engines. */
    @NonNull
    private final EnginesController mEnginesController;

    /**
     * Constructor.
     *
     * @param application android application singleton
     */
    @VisibleForTesting
    protected GroundSdkCore(@NonNull Application application) {
        mContext = application;
        GroundSdkConfig.lock(mContext);
        mSessionManager = new SessionManager(application, mSessionManagerListener);
        mDroneStore = new DeviceStoreCore.Drone();
        mRemoteControlStore = new DeviceStoreCore.RemoteControl();
        mFacilityStore = new ComponentStore<>();

        // register utilities
        mUtilities = new UtilityRegistry();
        mUtilities.registerUtility(DroneStore.class, mDroneStore);
        mUtilities.registerUtility(RemoteControlStore.class, mRemoteControlStore);

        mEnginesController = createEnginesController(mContext, mUtilities, mFacilityStore);
    }

    /**
     * Retrieves the session manager.
     *
     * @return the session manager
     */
    @NonNull
    public final SessionManager getSessionManager() {
        return mSessionManager;
    }

    /**
     * Gets a list of known drones and registers an observer notified each time this list changes.
     *
     * @param session  session that will manage the returned ref
     * @param filter   filter to select drones to include into the returned list. The filter criteria must not
     *                 change during the list reference lifecycle.
     * @param observer observer notified each time this list changes
     *
     * @return a reference to the requested list
     */
    @NonNull
    public final Ref<List<DroneListEntry>> getDroneList(@NonNull Session session,
                                                        @NonNull Predicate<DroneListEntry> filter,
                                                        @NonNull Ref.Observer<List<DroneListEntry>> observer) {
        return new DeviceListRef<>(session, observer, mDroneStore, DroneListEntryCore::new, filter);
    }

    /**
     * Gets a list of known remote controls and registers an observer notified each time this list changes.
     *
     * @param session  session that will manage the returned ref
     * @param filter   filter to select remote controls to include into the returned list. The filter criteria must not
     *                 change during the list reference lifecycle.
     * @param observer observer notified each time this list changes
     *
     * @return a reference to the requested list
     */
    @NonNull
    public final Ref<List<RemoteControlListEntry>> getRemoteControlList(
            @NonNull Session session, @NonNull Predicate<RemoteControlListEntry> filter,
            @NonNull Ref.Observer<List<RemoteControlListEntry>> observer) {
        return new DeviceListRef<>(session, observer, mRemoteControlStore, RemoteControlListEntryCore::new, filter);
    }

    /**
     * Gets a drone by uid.
     *
     * @param session  session that will manage refs issued by the device proxy
     * @param uid      uid of the requested drone
     * @param listener listener to be notified when the requested device disappears
     *
     * @return the drone with the requested uid, or {@code null} if there is no drone with such uid
     */
    @Nullable
    public final Drone getDrone(@NonNull Session session, @NonNull String uid,
                                @Nullable GroundSdk.OnDeviceRemovedListener listener) {
        DroneCore drone = getDevice(session, mDroneStore, uid, listener);
        return drone == null ? null : new DroneProxy(session, drone);
    }

    /**
     * Gets a remote control by uid.
     *
     * @param session  session that will manage refs issued by the device proxy
     * @param uid      uid of the requested remote control
     * @param listener listener to be notified when the requested device disappears
     *
     * @return the remote control with the requested uid, or {@code null} if there is no remote control with such uid
     */
    @Nullable
    public final RemoteControl getRemoteControl(@NonNull Session session, @NonNull String uid,
                                                @Nullable GroundSdk.OnDeviceRemovedListener listener) {
        RemoteControlCore remoteControl = getDevice(session, mRemoteControlStore, uid, listener);
        return remoteControl == null ? null : new RemoteControlProxy(session, remoteControl);
    }

    /**
     * Connects a drone identified by its uid.
     *
     * @param uid       uid of the drone to connect
     * @param connector the connector through which to establish the connection. Use {@code null} to use the best
     *                  available connector
     * @param password  password to use for authentication. Use {@code null} if the drone connection is not
     *                  secured, or to use the provider's saved password, if any (for RC providers)
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone is
     *         not visible anymore
     */
    public final boolean connectDrone(@NonNull String uid, @Nullable DeviceConnector connector,
                                      @Nullable String password) {
        return connectDevice(mDroneStore, uid, connector, password);
    }

    /**
     * Connects a remote control identified by its uid.
     *
     * @param uid       uid of the remote control to connect
     * @param connector the connector through which to establish the connection. Use {@code null} to use the best
     *                  available connector
     * @param password  password to use for authentication. Use {@code null} if the remote control connection is not
     *                  secured
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the
     *         remote control is not visible anymore
     */
    public final boolean connectRemoteControl(@NonNull String uid, @Nullable DeviceConnector connector,
                                              @Nullable String password) {
        return connectDevice(mRemoteControlStore, uid, connector, password);
    }

    /**
     * Disconnects a drone identified by its uid.
     * <p>
     * This method can be used to disconnect the drone when connected or to cancel the connection process if the drone
     * is currently connecting.
     *
     * @param uid uid of the drone to disconnect
     *
     * @return {@code true} if the disconnection process has started, otherwise {@code false}
     */
    public final boolean disconnectDrone(@NonNull String uid) {
        return disconnectDevice(mDroneStore, uid);
    }

    /**
     * Disconnects a remote control identified by its uid.
     * <p>
     * This method can be used to disconnect the remote control when connected or to cancel the connection process if
     * the remote control is currently connecting.
     *
     * @param uid uid of the remote control to disconnect
     *
     * @return {@code true} if the disconnection process has started, otherwise {@code false}
     */
    public final boolean disconnectRemoteControl(@NonNull String uid) {
        return disconnectDevice(mRemoteControlStore, uid);
    }

    /**
     * Forgets a drone identified by its uid.
     * <p>
     * Persisted drone data are deleted and the drone is removed if it is not currently visible.
     *
     * @param uid uid of the drone to forget
     *
     * @return {@code true} if the drone has been forgotten, otherwise {@code false}
     */
    public final boolean forgetDrone(@NonNull String uid) {
        return forgetDevice(mDroneStore, uid);
    }

    /**
     * Forgets a remote control identified by its uid.
     * <p>
     * Persisted remote control data are deleted and the remote control is removed if it is not currently visible.
     *
     * @param uid uid of the remote control to forget
     *
     * @return {@code true} if the remote control has been forgotten, otherwise {@code false}
     */
    public final boolean forgetRemoteControl(@NonNull String uid) {
        return forgetDevice(mRemoteControlStore, uid);
    }

    /**
     * Gets a facility.
     *
     * @param session       session that will manage the returned ref
     * @param facilityClass class of the facility
     * @param <F>           type of the facility class
     *
     * @return requested facility, or {@code null} if it's not present
     */
    @Nullable
    public final <F extends Facility> F getFacility(@NonNull Session session, @NonNull Class<F> facilityClass) {
        return mFacilityStore.get(session, facilityClass);
    }

    /**
     * Gets a facility and registers an observer notified each time it changes.
     *
     * @param session       session that will manage the returned ref
     * @param facilityClass class of the facility
     * @param observer      observer to notify when the facility changes
     * @param <F>           type of the facility class
     *
     * @return reference to the requested facility
     */
    @NonNull
    public final <F extends Facility> Ref<F> getFacility(@NonNull Session session, @NonNull Class<F> facilityClass,
                                                         @NonNull Ref.Observer<F> observer) {
        return new ComponentRef<>(session, observer, mFacilityStore, facilityClass);
    }


    /**
     * Creates a new replay stream for local media file.
     * <p>
     * Every call to this method creates a new replay stream instance for the given local file, that must be disposed
     * by closing the returned reference once that stream is not needed.
     * <p>
     * Closing the returned reference automatically stops the referenced replay stream.
     *
     * @param session  session that will manage the returned ref
     * @param source   identifies local source stream
     * @param observer observer notified when the stream state changes
     *
     * @return a reference onto a new replay stream for the given file
     */
    @NonNull
    public static Ref<FileReplay> newFileReplay(@NonNull Session session, @NonNull FileReplay.Source source,
                                                @NonNull Ref.Observer<FileReplay> observer) {
        try {
            return new FileReplayRef(session, observer, (FileSourceCore) source);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid source: " + source, e);
        }
    }

    /**
     * Starts managing the given USB RC accessory.
     *
     * @param rcAccessory USB RC accessory to manage
     */
    public final void manageRcAccessory(@NonNull UsbAccessory rcAccessory) {
        RcUsbAccessoryManager manager = mUtilities.getUtility(RcUsbAccessoryManager.class);
        if (manager != null) {
            manager.manageRcAccessory(rcAccessory);
        } else if (ULog.w(TAG_API)) {
            ULog.w(TAG_API, "No registered manager for RC accessory : " + rcAccessory);
        }
    }

    /**
     * Gets the device with the given uid in the given store.
     *
     * @param session  session where the listener will be registered
     * @param store    device store where the device can be found
     * @param uid      uid of the requested device in the store
     * @param listener listener to call back when the requested device disappears. Never called if the requested device
     *                 does not exist (i.e. when this method returns {@code null}. May be {@code null}
     *
     * @return the device with the requested uid, or {@code null} if there is no device with such uid in the given
     *         store
     */
    @Nullable
    private static <D extends DeviceCore> D getDevice(@NonNull Session session, @NonNull DeviceStore<D> store,
                                                      @NonNull String uid,
                                                      @Nullable GroundSdk.OnDeviceRemovedListener listener) {
        D device = store.get(uid);
        if (device != null && listener != null) {
            DeviceRemovedListenerRef.register(session, store, uid, listener);
        }
        return device;
    }

    /**
     * Connects the device with the given uid in the given store.
     *
     * @param store     device store where the device can be found
     * @param uid       uid of the device in the store
     * @param connector the connector through which to establish the connection. Use {@code null} to use the best
     *                  available connector
     * @param password  password to use for authentication. Use {@code null} if the drone connection is not
     *                  secured, or to use the provider's saved password, if any (for RC providers)
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the device
     *         is not visible anymore
     */
    private static boolean connectDevice(@NonNull DeviceStore<?> store, @NonNull String uid,
                                         @Nullable DeviceConnector connector, @Nullable String password) {
        DeviceCore device = store.get(uid);
        return device != null && device.connect(connector, password);
    }

    /**
     * Disconnects the device with the given uid in the given store.
     *
     * @param store device store where the device can be found
     * @param uid   uid of the device in the store
     *
     * @return {@code true} if the disconnection process has started, otherwise {@code false}
     */
    private static boolean disconnectDevice(@NonNull DeviceStore<?> store, @NonNull String uid) {
        DeviceCore device = store.get(uid);
        return device != null && device.disconnect();
    }

    /**
     * Forgets the device with the given uid in the given store.
     *
     * @param store device store where the device can be found
     * @param uid   uid of the device in the store
     *
     * @return {@code true} if the device has been forgotten, otherwise {@code false}
     */
    private static boolean forgetDevice(@NonNull DeviceStore<?> store, @NonNull String uid) {
        DeviceCore device = store.get(uid);
        return device != null && device.forget();
    }

    /** Listens to session manager events and starts/stops engines controller as appropriate. */
    @SuppressWarnings("FieldCanBeLocal")
    private final SessionManager.Listener mSessionManagerListener = new SessionManager.Listener() {

        @Override
        public void onFirstSessionOpened() {
            DumpSys.startService(mContext, GroundSdkCore::dump);
            mEnginesController.start();
        }

        @Override
        public void onLastSessionClosed() {
            mEnginesController.stop(mControllerStopListener);
        }
    };

    /** Notified when all engines managed by the controller are stopped. */
    private final EnginesController.OnStopListener mControllerStopListener = new EnginesController.OnStopListener() {

        @Override
        public void onStop() {
            DumpSys.stopService(mContext);
            if (BuildConfig.DEBUG) {
                if (!mDroneStore.all().isEmpty()) {
                    throw new IllegalStateException("Drone store not empty upon engine stop");
                }
                if (!mRemoteControlStore.all().isEmpty()) {
                    throw new IllegalStateException("RC store not empty upon engine stop");
                }
            }
        }
    };

    /**
     * Factory method to create the engine controller.
     * <p>
     * This is done using a factory function to allow unit test to create a mock engine controller
     * <p>
     * TODO get rid of this factory function, mocking now needs to happen when utilities are registered
     *
     * @param context       application context
     * @param utilities     provides various utility APIs to the controlled engine(s)
     * @param facilityStore allows to publish public facility APIs
     *
     * @return EnginesController instance
     */
    @VisibleForTesting
    @NonNull
    protected EnginesController createEnginesController(@NonNull Context context, @NonNull UtilityRegistry utilities,
                                                        @NonNull ComponentStore<Facility> facilityStore) {
        return new EnginesController(context, utilities, facilityStore);
    }

    /**
     * Bypasses singleton creation and set the current instance as the current singleton. Used by unit test to create
     * instance with a mock engine controller.
     */
    @VisibleForTesting
    public void setAsDefault() {
        synchronized (GroundSdkCore.class) {
            sInstance = this;
        }
    }

    /**
     * Clears current singleton. This bypass singleton pattern for unit tests that used {@link #setAsDefault()}.
     */
    @VisibleForTesting
    public static void close() {
        synchronized (GroundSdkCore.class) {
            sInstance = null;
            GroundSdkConfig.close();
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    private static void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--drones: dumps drone store\n");
            writer.write("\t--rcs: dumps remote control store\n");
        }

        GroundSdkCore self = sInstance;
        assert self != null; // if null, NPE will be logged to dumpsys

        if (args.contains("--drones") || args.contains("--all")) {
            self.mDroneStore.dump(writer, "Drones");
        }

        if (args.contains("--rcs") || args.contains("--all")) {
            self.mRemoteControlStore.dump(writer, "Remote Controls");
        }

        self.mSessionManager.dump(writer, args);
        self.mEnginesController.dump(writer, args);
        Executor.dump(writer, args);
    }
}
