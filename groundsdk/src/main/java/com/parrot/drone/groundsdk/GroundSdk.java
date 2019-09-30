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

package com.parrot.drone.groundsdk;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.DroneListEntry;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.RemoteControlListEntry;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.GroundSdkCore;
import com.parrot.drone.groundsdk.internal.session.Session;
import com.parrot.drone.groundsdk.internal.session.SessionManager;
import com.parrot.drone.groundsdk.stream.FileReplay;

import java.util.List;
import java.util.function.Predicate;

/**
 * Core ground SDK access class.
 * <p>
 * This class provides the main entry-point API of GroundSdk allowing to retrieve and manage devices
 * ({@link Drone drones} and/or {@link RemoteControl remote controls})
 * <p>
 * A GroundSdk instance also represents a session through which GroundSdk API can be used. Such a session has a
 * lifecycle that <strong>MUST</strong> be properly managed by the application. <br>
 * A session has mainly two purposes:<ul>
 * <li>It allows GroundSdk to start its underlying engines as soon as the first session is requested by the
 * application and to close them when the last session is closed by the application.</li>
 * <li>It keeps track of all the {@link Ref.Observer observers} that the application may register using GroundSdk
 * APIs in order to unregister them automatically when the session closes. It also provides the application a way
 * to suspend or resume all those registered observers so that it can receive change notifications only when
 * appropriate depending on its own lifecycle. </li>
 * </ul>
 * <p>
 * The following methods form, together, the session management API: {@code newSession}, {@code resume},
 * {@code suspend}, {@code retain} and finally {@code close}.
 * <p>
 * {@link #newSession newSession} method allows the application to create a new session. Every call to this method
 * leads to the creation of a <strong>new</strong> session that <strong>MUST</strong> be closed by the application at
 * some point. <br>
 * A new session is always implicitly linked to an android {@link Context} (an {@link Activity},
 * a {@link android.app.Service Service} or an {@link android.app.Application Application}), that must be provided when
 * the session is requested. <br>
 * This context is tied to the session lifecycle; as a consequence, a session <strong>MUST NEVER</strong> outlive its
 * associated context. In other words, method {@link #close} must be called before the associated context disappears.
 * <br>
 * Failure to do so will result in a session leak, and GroundSdk won't be able to stop its underlying engines when
 * necessary, which in turn will keep the application process running, draining battery and possibly using network.
 * <br>
 * Optionally, the application may provide an android Bundle containing retained session information in order to
 * 'restore' a retained session. See below.
 * <p>
 * A new GroundSdk session is always created in a so-called 'suspended' state: any observer that the application may
 * register with this session through any GroundSdk API will not forward any change notification to the application
 * while the session remains in this state. <br>
 * The application must call the {@link #resume} method to allow observers to forward change notifications, and may
 * call {@link #suspend} method in order to get back to the suspended state, to stop receiving change notifications.
 * <p>
 * {@link #retain retain} method allows the application to temporarily persist a session before it is closed in order
 * to restore it at a later time: when one or more sessions are retained, GroundSdk won't stop its underlying engines,
 * even if all sessions (including the retained ones) are closed.
 * <br>
 * To retain a session, the application must provide an Android {@link Bundle} where GroundSdk can record internal
 * session info. The application can then later restore the session using {@link #newSession(Context, Bundle)}
 * method by providing this bundle.
 * <br>
 * The application needs to be careful that a retained session <strong>MUST</strong> always be restored at some point.
 * Failure to do so will result in a session leak, and GroundSdk won't be able to stop its underlying engines when
 * necessary, which in turn will keep the application process running, draining battery and possibly using network.
 * <br>
 * Note that the session will be restored in a 'suspended' state, as if it was a new session, and all lifecycle
 * recommendations presented above apply.
 * <p>
 * Below are given several recommendations to use a GroundSdk session tied to an android {@code Activity}:
 * <ul>
 * <li>Create the session in activity {@code onCreate()} method, passing the android-provided
 * {@code savedInstanceState Bundle}, and to close it in activity {@code onDestroy()} method.</li>
 * <li>Resume the session in activity {@code onStart()} and suspend it in activity {@code onStop()}. </li>
 * <li>Alternatively, resume the session in activity {@code onResume()} and suspend it in activity
 * {@code onPause()}.</li>
 * <li>Retain the session in activity {@code onSaveInstanceState()} method, if (and only if) there is a pending
 * {@link Activity#isChangingConfigurations configuration change}.
 * </ul>
 * <p>
 * For example:
 * <pre>
 *  public class MyGroundSdkActivity extends Activity {
 *      private GroundSdk mGroundSdk;
 *
 *     {@literal @}Override
 *      public void onCreate(@Nullable Bundle savedInstanceState) {
 *          super.onCreate(savedInstanceState);
 *          mGroundSdk = GroundSdk.newSession(this, savedInstanceState);
 *      }
 *
 *     {@literal @}Override
 *      public void onStart() {
 *          super.onStart();
 *          mGroundSdk.resume();
 *      }
 *
 *     {@literal @}Override
 *      public void onStop() {
 *          mGroundSdk.suspend();
 *          super.onStop();
 *      }
 *
 *     {@literal @}Override
 *      public void onSaveInstanceState(@NonNull Bundle outState) {
 *          super.onSaveInstanceState(outState);
 *          if (isChangingConfigurations()) {
 *              mGroundSdk.retain(outState);
 *          }
 *      }
 *
 *     {@literal @}Override
 *      public void onDestroy() {
 *          mGroundSdk.close();
 *          super.onDestroy();
 *      }
 *  }
 * </pre>
 * GroundSdk also provides automatic session lifecycle management in correlation with an {@code Activity} lifecycle,
 * please refer to {@link ManagedGroundSdk} documentation for further information.
 */
public class GroundSdk {

    /**
     * Obtains a new GroundSdk session instance.
     *
     * @param context            the android context to tie with the session
     * @param savedInstanceState a bundle containing a retained session info to be restored, otherwise {@code null}
     *
     * @return a new GroundsSdk session
     */
    @NonNull
    public static GroundSdk newSession(@NonNull Context context, @Nullable Bundle savedInstanceState) {
        return new GroundSdk(context, savedInstanceState);
    }

    /**
     * Resumes this session.
     * <p>
     * All registered observers may now notify changes to the application, and may do so immediately in case any
     * observed component did change while the session was suspended.
     *
     * @see #suspend()
     */
    public void resume() {
        mSessionManager.resumeSession(mSession);
    }

    /**
     * Suspends this session.
     * <p>
     * All registered observers are not allowed to notify changes to the application until {@link #resume()} is called.
     *
     * @see #resume()
     */
    public void suspend() {
        mSessionManager.suspendSession(mSession);
    }

    /**
     * Retains this session.
     *
     * @param outState android-provided bundle where session information will be stored temporarily
     */
    public void retain(@NonNull Bundle outState) {
        mSessionManager.retainSession(mSession, outState);
    }

    /**
     * Closes this session.
     * <p>
     * All registered observers are disposed. Session must not be used anymore after this method is called.
     */
    public void close() {
        mSessionManager.closeSession(mSession);
    }

    /**
     * Gets a list of known drones and registers an observer notified each time this list changes.
     *
     * @param filter   filter to select drones to include into the returned list. The filter criteria must not
     *                 change during the list reference lifecycle.
     * @param observer observer notified each time this list changes
     *
     * @return a reference to the requested list
     */
    @NonNull
    public final Ref<List<DroneListEntry>> getDroneList(@NonNull Predicate<DroneListEntry> filter,
                                                        @NonNull Ref.Observer<List<DroneListEntry>> observer) {
        return mCore.getDroneList(mSession, filter, observer);
    }

    /**
     * Gets a list of known remote controls and registers an observer notified each time this list changes.
     *
     * @param filter   filter to select remote controls to include into the returned list. The filter criteria must not
     *                 change during the list reference lifecycle.
     * @param observer observer notified each time this list changes
     *
     * @return a reference to the requested list
     */
    @NonNull
    public final Ref<List<RemoteControlListEntry>> getRemoteControlList(
            @NonNull Predicate<RemoteControlListEntry> filter,
            @NonNull Ref.Observer<List<RemoteControlListEntry>> observer) {
        return mCore.getRemoteControlList(mSession, filter, observer);
    }

    /**
     * Gets a drone by uid.
     *
     * @param uid uid of the requested drone
     *
     * @return the drone with the requested uid, or {@code null} if there is no drone with such uid
     */
    @Nullable
    public final Drone getDrone(@NonNull String uid) {
        return mCore.getDrone(mSession, uid, null);
    }

    /**
     * Gets a remote control by uid.
     *
     * @param uid uid of the requested remote control
     *
     * @return the remote control with the requested uid, or {@code null} if there is no remote control with such uid
     */
    @Nullable
    public final RemoteControl getRemoteControl(@NonNull String uid) {
        return mCore.getRemoteControl(mSession, uid, null);
    }

    /** Listener notified when a device (drone, remote control) disappears. */
    public interface OnDeviceRemovedListener {

        /**
         * Called back when the device with given uid disappears.
         *
         * @param uid uid of the device which has disappeared
         */
        void onDeviceRemoved(@NonNull String uid);
    }

    /**
     * Gets a drone by uid.
     *
     * @param uid      uid of the requested drone
     * @param listener called back when the requested drone disappears. Never called if the requested drone does not
     *                 exist (i.e. when this method returns {@code null}).
     *
     * @return the drone with the requested uid, or {@code null} if there is no drone with such uid
     */
    @Nullable
    public final Drone getDrone(@NonNull String uid, @NonNull OnDeviceRemovedListener listener) {
        return mCore.getDrone(mSession, uid, listener);
    }

    /**
     * Gets a remote control by uid.
     *
     * @param uid      uid of the requested remote control
     * @param listener called back when the requested remote control disappears. Never called if the requested
     *                 remote control does not exist (i.e. when this method returns {@code null}).
     *
     * @return the remote control with the requested uid, or {@code null} if there is no remote control with such uid
     */
    @Nullable
    public final RemoteControl getRemoteControl(@NonNull String uid, @NonNull OnDeviceRemovedListener listener) {
        return mCore.getRemoteControl(mSession, uid, listener);
    }

    /**
     * Connects a drone identified by its uid.
     * <p>
     * Connects the drone with the best available connector, chosen as follows: <ul>
     * <li>If there is only one available connector, then it is used to connect the drone. Otherwise,</li>
     * <li>if there is only one available {@link DeviceConnector.Type#REMOTE_CONTROL remote control connector},
     * then it is used to connect the drone.
     * </ul>
     * If none of the aforementioned condition holds, then the connection fails and {@code false} is returned. <br>
     * {@link #connectDrone(String, DeviceConnector)} method shall be used to select an appropriate connector instead.
     *
     * @param uid uid of the drone to connect
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone is
     *         not visible anymore
     */
    public final boolean connectDrone(@NonNull String uid) {
        return mCore.connectDrone(uid, null, null);
    }

    /**
     * Connects a drone identified by its uid using a specific connector.
     *
     * @param uid       uid of the drone to connect
     * @param connector the connector through which to establish the connection
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone is
     *         not visible anymore
     */
    public final boolean connectDrone(@NonNull String uid, @NonNull DeviceConnector connector) {
        return mCore.connectDrone(uid, connector, null);
    }

    /**
     * Connects a secured drone identified by its uid.
     *
     * @param uid       uid of the drone to connect
     * @param connector the connector through which to establish the connection
     * @param password  password to use for authentication
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone is
     *         not visible anymore
     */
    public final boolean connectDrone(@NonNull String uid, @NonNull DeviceConnector connector,
                                      @NonNull String password) {
        return mCore.connectDrone(uid, connector, password);
    }

    /**
     * Connects a remote control identified by its uid.
     * <p>
     * Connects the remote control with the best available connector, chosen as follows: <ul>
     * <li>If there is only one available connector, then it is used to connect the remote control. Otherwise,</li>
     * <li>if there is only one available {@link DeviceConnector.Technology#USB USB} connector (note that there can
     * be two of those in case both the device USB and the USB Debug Bridge are used), then it is used to connect
     * the remote control.</li>
     * </ul>
     * If none of the aforementioned condition holds, then the connection fails and {@code false} is returned. <br>
     * {@link #connectRemoteControl(String, DeviceConnector)} method shall be used to select an appropriate connector
     * instead.
     *
     * @param uid uid of the remote control to connect
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the remote
     *         control is not visible anymore
     */
    public final boolean connectRemoteControl(@NonNull String uid) {
        return mCore.connectRemoteControl(uid, null, null);
    }

    /**
     * Connects a remote control identified by its uid using a specific connector.
     *
     * @param uid       uid of the remote control to connect
     * @param connector the connector through which to establish the connection
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the
     *         remote control is not visible anymore
     */
    public final boolean connectRemoteControl(@NonNull String uid, @NonNull DeviceConnector connector) {
        return mCore.connectRemoteControl(uid, connector, null);
    }

    /**
     * Connects a remote control identified by its uid.
     *
     * @param uid       uid of the remote control to connect
     * @param connector the connector through which to establish the connection
     * @param password  password to use for authentication
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the
     *         remote control is not visible anymore
     */
    public final boolean connectRemoteControl(@NonNull String uid, @NonNull DeviceConnector connector,
                                              @NonNull String password) {
        return mCore.connectRemoteControl(uid, connector, password);
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
        return mCore.disconnectDrone(uid);
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
        return mCore.disconnectRemoteControl(uid);
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
        return mCore.forgetDrone(uid);
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
        return mCore.forgetRemoteControl(uid);
    }


    /**
     * Retrieves a facility.
     *
     * @param facilityClass class of the facility
     * @param <F>           type of the facility class
     *
     * @return requested facility, or {@code null} if it's not present
     */
    @Nullable
    public final <F extends Facility> F getFacility(@NonNull Class<F> facilityClass) {
        return mCore.getFacility(mSession, facilityClass);
    }

    /**
     * Retrieves a facility and registers an observer notified each time it changes.
     *
     * @param facilityClass class of the facility
     * @param observer      observer to notify when the facility changes
     * @param <F>           type of the facility class
     *
     * @return reference to the requested facility
     */
    @NonNull
    public final <F extends Facility> Ref<F> getFacility(@NonNull Class<F> facilityClass,
                                                         @NonNull Ref.Observer<F> observer) {
        return mCore.getFacility(mSession, facilityClass, observer);
    }

    /**
     * Creates a new replay stream for some local media file.
     * <p>
     * Every call to this method creates a new replay stream instance for the given local file, that must be disposed
     * by closing the returned reference once that stream is not needed.
     * <p>
     * Closing the returned reference automatically stops the referenced replay stream.
     *
     * @param source   identifies local source stream
     * @param observer observer notified when the stream state changes
     *
     * @return a reference onto a new replay stream for the given file
     */
    @NonNull
    public final Ref<FileReplay> replay(@NonNull FileReplay.Source source, @NonNull Ref.Observer<FileReplay> observer) {
        return GroundSdkCore.newFileReplay(mSession, source, observer);
    }

    /**
     * Commands GroundSDK to manage the given remote controller USB accessory.
     * <p>
     * Client application is responsible to obtain the USB accessory in question using the appropriate Android API
     * and also to request proper permission to use the latter from Android SDK. <br>
     * This is usually done using an activity registered with the
     * {@link android.hardware.usb.UsbManager#ACTION_USB_ACCESSORY_ATTACHED} intent action filter. <br>
     * Please refer to <a href="https://developer.android.com/guide/topics/connectivity/usb/accessory.html">Android USB
     * accessory guide</a> for further documentation.
     *
     * @param context     an android context
     * @param rcAccessory android USB RC accessory to manage
     */
    public static void manageRcAccessory(@NonNull Context context, @NonNull UsbAccessory rcAccessory) {
        GroundSdkCore.get(context).manageRcAccessory(rcAccessory);
    }

    /** Singleton GroundSdkCore instance. */
    @NonNull
    private final GroundSdkCore mCore;

    /** GroundSdkCore's session manager. */
    @NonNull
    private final SessionManager mSessionManager;

    /** This GroundSdk session. */
    @NonNull
    private final Session mSession;

    /**
     * Constructor for a new unmanaged GroundSdk session.
     *
     * @param context            android client context
     * @param savedInstanceState android saved instance state bundle, may be {@code null}
     */
    private GroundSdk(@NonNull Context context, @Nullable Bundle savedInstanceState) {
        mCore = GroundSdkCore.get(context);
        mSessionManager = mCore.getSessionManager();
        mSession = mSessionManager.obtainUnmanagedSession(context, savedInstanceState);
    }

    /**
     * Interface for providing a session.
     * <p>
     * Provided through subclass constructors, allowing them to provide their own instance of a session.
     */
    interface SessionProvider {

        /**
         * Retrieves the session that the GroundSdk instance represents.
         *
         * @param sessionManager GroundSdk session manager
         *
         * @return the session to use
         */
        @NonNull
        Session obtainSession(@NonNull SessionManager sessionManager);
    }

    /**
     * Constructor for GroundSdk subclasses.
     *
     * @param context         android client context
     * @param sessionProvider provides the session to use for this GroundSdk instance
     */
    GroundSdk(@NonNull Context context, @NonNull SessionProvider sessionProvider) {
        mCore = GroundSdkCore.get(context);
        mSessionManager = mCore.getSessionManager();
        mSession = sessionProvider.obtainSession(mSessionManager);
    }
}
