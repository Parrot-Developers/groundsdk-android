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

import androidx.annotation.NonNull;

/**
 * An GroundSdk session automatically managed according to an android {@link Activity} lifecycle.
 * <p>
 * This class allows the application to retrieve a GroundSdk session whose lifecycle is is automatically managed in
 * correlation with a given {@code Activity}'s own lifecycle.This is the recommended way to use GroundSdk API in an
 * android Activity.
 * <p>
 * The application should obtain such a session in the activity {@code onCreate} method. None of the session's lifecycle
 * management methods, as described in {@link GroundSdk} documentation, need to be called.
 * <ul>
 * <li>The session is automatically retained and restored upon configuration changes.</li>
 * <li>The session is resumed when the activity calls through {@code super.onStart()} method (by default, but the
 * application may chose to have it resumed in when the activity calls through {@code super.onResume()} method
 * instead).</li>
 * <li>The session is suspended when the activity calls through {@code super.onStop()} method (by default, but the
 * application may chose to have it suspended in when the activity calls through {@code super.onPause()} method
 * instead).</li>
 * <li>The session is automatically closed when the activity calls through {@code super.onDestroy()}. The application
 * <strong>MUST NOT</strong> use the session past this point.</li>
 * </ul>
 * <p>
 * For example:
 * <pre>
 *  public class MyManagedGroundSdkActivity extends Activity {
 *      private ManagedGroundSdk mGroundSdk;
 *
 *     {@literal @}Override
 *      public void onCreate(@Nullable Bundle savedInstanceState) {
 *          super.onCreate(savedInstanceState);
 *          mGroundSdk = ManagedGroundSdk.obtainSession(this);
 *      }
 *
 *     {@literal @}Override
 *      public void onDestroy() {
 *          super.onDestroy();
 *          // mGroundSdk MUST NOT be used anymore past this point
 *      }
 *  }
 * </pre>
 */
public final class ManagedGroundSdk extends GroundSdk {

    /**
     * Allows to specify when a managed GroundSdk session will resume/suspend its registered observers.
     */
    public enum ObserverBehavior {

        /**
         * All session's observers are resumed at activity {@code onStart()} time and suspended at activity
         * {@code onStop()} time.
         */
        NOTIFY_ON_START,

        /**
         * All session's observers are resumed at activity {@code onResume()} time and suspended at activity
         * {@code onPause()} time.
         */
        NOTIFY_ON_RESUME
    }

    /**
     * Obtains a GroundSdk session automatically managed according to the provided activity's lifecycle.
     * <p>
     * All observers registered through this session are resumed according to the provided
     * {@link ObserverBehavior observer behaviour}.
     *
     * @param activity         the activity whose lifecycle will control the session's own lifecycle
     * @param observerBehavior observer behaviour this session must comply to
     *
     * @return a GroundSdk session, automatically managed according to the given activity's lifecycle
     */
    @NonNull
    public static ManagedGroundSdk obtainSession(@NonNull Activity activity,
                                                 @NonNull ObserverBehavior observerBehavior) {
        return new ManagedGroundSdk(activity, observerBehavior);
    }

    /**
     * Obtains a GroundSdk session automatically managed according to the provided activity's lifecycle.
     * <p>
     * All observers registered through this session are resumed in activity {@code onStart()} and suspended in activity
     * {@code onStop()}. <br>
     * Use {@link #obtainSession(Activity, ObserverBehavior)} to customize this behavior if unsuitable to the
     * application needs.
     *
     * @param activity the activity whose lifecycle will control the session's own lifecycle
     *
     * @return a GroundSdk session, automatically managed according to the given activity's lifecycle
     *
     * @see #obtainSession(Activity, ObserverBehavior)
     */
    @NonNull
    public static ManagedGroundSdk obtainSession(@NonNull Activity activity) {
        return obtainSession(activity, ObserverBehavior.NOTIFY_ON_START);
    }

    /**
     * Constructor.
     *
     * @param activity         activity whose lifecycle will control the session's lifecycle
     * @param observerBehavior observer behaviour this session must comply to
     */
    private ManagedGroundSdk(@NonNull Activity activity, @NonNull ObserverBehavior observerBehavior) {
        super(activity, sessionManager -> sessionManager.obtainManagedSession(activity, observerBehavior));
    }
}
