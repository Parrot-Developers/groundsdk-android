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

package com.parrot.drone.groundsdk.internal.session;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_SESSION;

/**
 * GroundSdk Session manager.
 * <p>
 * Manages the creation and monitors the lifecycle of all sessions requested by the application.
 */
public final class SessionManager {

    /**
     * Notifies main session manager events.
     */
    public interface Listener {

        /**
         * Called back when the first session gets registered.
         */
        void onFirstSessionOpened();

        /**
         * Called back when the last registered session gets closed.
         */
        void onLastSessionClosed();
    }

    /** Listener notified of session manager events. */
    @NonNull
    private final Listener mListener;

    /**
     * Weak reference on a session's associated context, plus a strong reference on the session, used to detect
     * and prune leaked sessions.
     */
    private static final class LeakChecker extends WeakReference<Context> {

        /** The session to detect leaks of. */
        @NonNull
        final Session mSession;

        /**
         * Constructor.
         *
         * @param context the associated session context, which will be weakly referenced
         * @param session the session to detect leaks of
         */
        LeakChecker(@NonNull Context context, @NonNull Session session) {
            super(context);
            mSession = session;
        }
    }

    /**
     * All registered sessions.
     * <p>
     * Contains sessions from the time they are created or resurrected from the
     * {@link #mRetainedSessions retained session set} to the point where they are closed.
     * <p>
     * Each session is associated to a leak checker that allows to know if a session is still open while the
     * associated context has been is gc-ed, which is considered a leak.
     */
    @NonNull
    private final Map<Session, LeakChecker> mSessions;

    /**
     * Managed sessions, indexed weakly by associated context.
     * <p>
     * Keys are weak references in order to prevent leaking android Context(s).
     */
    @NonNull
    private final WeakHashMap<Activity, ManagedSession> mManagedSessions;

    /**
     * Retained sessions set.
     * <p>
     * Contains sessions from the time they are retained (and their identifier persisted into an application-provided
     * instance state Bundle) to the point where they are resurrected.
     */
    @NonNull
    private final SparseArray<Session> mRetainedSessions;

    /** Key for the session identifier stored in the activity instance state. */
    private static final String BUNDLE_KEY_SESSION_ID = "com.parrot.drone.groundsdk.internal.session.SESSION_ID";

    /**
     * Constructor.
     *
     * @param application android application singleton
     * @param listener    listener notified of session manager events
     */
    public SessionManager(@NonNull Application application, @NonNull Listener listener) {
        mListener = listener;
        mSessions = new HashMap<>();
        mManagedSessions = new WeakHashMap<>();
        mRetainedSessions = new SparseArray<>();
        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    /**
     * Obtains a session managed automatically according to the provided activity lifecycle.
     *
     * @param activity         activity whose lifecycle will serve to manage the session
     * @param observerBehavior desired observer behavior for the session
     *
     * @return an existing session in case such a session is currently registered for the provided activity, otherwise
     *         a new session for that activity.
     */
    @NonNull
    public Session obtainManagedSession(@NonNull Activity activity,
                                        @NonNull ManagedGroundSdk.ObserverBehavior observerBehavior) {
        if (activity.isDestroyed()) {
            throw new IllegalStateException("Can't manage session on destroyed activity: " + activity);
        }
        // try to fetch a live managed session from the given context
        ManagedSession session = mManagedSessions.get(activity);
        if (session == null) {
            session = new ManagedSession(observerBehavior);
            registerManagedSession(activity, session);
        }
        return session;
    }

    /**
     * Obtains a session whose lifecycle is to be managed manually by the application.
     *
     * @param context            context to be associated with the session, mainly used for leak checking
     * @param savedInstanceState application-provided instance state bundle, used to resurrect a retained session
     *                           if appropriate
     *
     * @return an existing retained session in case the provided bundle contains the identifier of such a session,
     *         otherwise a new session
     */
    @NonNull
    public Session obtainUnmanagedSession(@NonNull Context context, @Nullable Bundle savedInstanceState) {
        Session session = getRetainedSession(savedInstanceState);
        if (session == null) {
            session = new Session();
        }
        registerSession(context, session);
        return session;
    }

    /**
     * Resumes a session.
     *
     * @param session session to be resumed
     */
    public void resumeSession(@NonNull Session session) {
        if (!mSessions.containsKey(session)) {
            throw new IllegalArgumentException("Cannot resume unregistered session: " + session);
        }
        session.resumeObservers();
    }

    /**
     * Suspends a session.
     *
     * @param session session to be suspended
     */
    public void suspendSession(@NonNull Session session) {
        if (!mSessions.containsKey(session)) {
            throw new IllegalArgumentException("Cannot suspend unregistered session: " + session);
        }
        session.suspendObservers();
    }

    /**
     * Retains a session.
     *
     * @param session  session to be retained
     * @param outState application-provided bundle where to persist the retained session id
     */
    public void retainSession(@NonNull Session session, @NonNull Bundle outState) {
        if (!mSessions.containsKey(session)) {
            throw new IllegalArgumentException("Cannot retain unregistered session: " + session);
        }
        int id = session.getId();
        if (mRetainedSessions.get(id) != null) {
            throw new IllegalStateException("Session already retained: " + session);
        }
        outState.putInt(BUNDLE_KEY_SESSION_ID, id);
        mRetainedSessions.put(id, session);
    }

    /**
     * Closes a session.
     *
     * @param session session to close
     */
    public void closeSession(@NonNull Session session) {
        if (!mSessions.containsKey(session)) {
            throw new IllegalArgumentException("Cannot close unregistered session: " + session);
        }
        session.close();
        mSessions.remove(session);
        pruneLeakedSessions();
        if (mSessions.isEmpty() && mRetainedSessions.size() == 0) {
            mListener.onLastSessionClosed();
        }
    }

    /**
     * Registers a session.
     * <p>
     * Registers the given session with the provided context and removes it from the retain sessions set
     * if appropriate.
     * <p>
     * Notifies the listener in case this is the first, non-previously retained session to be registered.
     *
     * @param context context to attach to the session
     * @param session session to be registered
     */
    private void registerSession(@NonNull Context context, @NonNull Session session) {
        pruneLeakedSessions();
        if (mSessions.containsKey(session)) {
            throw new IllegalStateException("Session already registered: " + session);
        }
        boolean firstSession = mSessions.isEmpty() && mRetainedSessions.size() == 0;
        mSessions.put(session, new LeakChecker(context, session));
        mRetainedSessions.remove(session.getId());
        session.open();
        if (firstSession) {
            mListener.onFirstSessionOpened();
        }
    }

    /**
     * Registers a managed session.
     * <p>
     * The given session is registered as any other session would, then it is also registered in the managed sessions
     * set.
     *
     * @param activity activity context to attach to the session
     * @param session  managed session to be registered
     *
     * @see #registerSession
     */
    private void registerManagedSession(@NonNull Activity activity, @NonNull ManagedSession session) {
        registerSession(activity, session);
        mManagedSessions.put(activity, session);
    }

    /**
     * Retrieves a retained session.
     * <p>
     * Tries to obtain a retained session based on the session id, if present, in the provided saved instance state
     * bundle.
     * <p>
     * Note that even in case of success, the retained session remains in the retained set for the moment. The
     * {@link #registerSession} method will take care of removing it if appropriate since it needs this piece of
     * information to check whether to notify the listener of the first non-retained session creation.
     *
     * @param savedInstanceState saved instance state bundle, as provided by the application (so may be {@code null})
     *
     * @return the retained session with an uid such as the one in the provided bundle, or {@code null} if the provided
     *         bundle is {@code null} or if does not contain a session id key, or if the session id found in the
     *         bundle does not match any retained session id (which is logged as error since abnormal).
     */
    @Nullable
    private Session getRetainedSession(@Nullable Bundle savedInstanceState) {
        Session session = null;
        if (savedInstanceState != null) {
            int id = savedInstanceState.getInt(BUNDLE_KEY_SESSION_ID, Session.INVALID_ID);
            if (id != Session.INVALID_ID) {
                session = mRetainedSessions.get(id);
                if (session == null) {
                    ULog.e(TAG_SESSION, "Cannot restore retained session, invalid id in bundle: " + id);
                }
            }
        }
        return session;
    }

    /**
     * Closes and unregisters all leaked registered sessions and warns the case being.
     */
    private void pruneLeakedSessions() {
        for (Iterator<Map.Entry<Session, LeakChecker>> iter = mSessions.entrySet().iterator(); iter.hasNext(); ) {
            LeakChecker leakChecker = iter.next().getValue();
            if (leakChecker.get() == null) {
                ULog.w(TAG_SESSION, "Pruning leaked session: " + leakChecker.mSession);
                leakChecker.mSession.close();
                iter.remove();
            }
        }
    }

    /** Listens to all application's activities lifecycle. */
    @SuppressWarnings("FieldCanBeLocal")
    private final Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks =
            new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                    Session session = getRetainedSession(savedInstanceState);
                    if (session instanceof ManagedSession) {
                        // register the session as if the application asked for an unmanaged session.
                        // When the application will then invoke obtainManagedSession, this session will be
                        // fetched directly from the managed session set.
                        registerManagedSession(activity, (ManagedSession) session);
                    }
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    ManagedSession session = mManagedSessions.get(activity);
                    if (session != null) {
                        session.onActivityStarted();
                    }
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    ManagedSession session = mManagedSessions.get(activity);
                    if (session != null) {
                        session.onActivityResumed();
                    }
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    ManagedSession session = mManagedSessions.get(activity);
                    if (session != null) {
                        session.onActivityPaused();
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    ManagedSession session = mManagedSessions.get(activity);
                    if (session != null) {
                        session.onActivityStopped();
                    }
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                    // only if the activity is changing configurations, so we can be confident that
                    // onCreate(savedInstanceState) will be called almost immediately after.
                    if (activity.isChangingConfigurations()) {
                        ManagedSession session = mManagedSessions.get(activity);
                        if (session != null) {
                            retainSession(session, outState);
                        }
                    }
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    ManagedSession session = mManagedSessions.remove(activity);
                    if (session != null) {
                        closeSession(session);
                    }
                }
            };

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--sessions: dumps sessions info\n");
            writer.write("\t\t--refs: dumps refs info\n");
        } else if (args.contains("--sessions") || args.contains("--all")) {
            for (Session session : mSessions.keySet()) {
                writer.write("\t" + session + " -> ");
                if (session instanceof ManagedSession) {
                    writer.write("managed, ");
                }
                if (mRetainedSessions.get(session.getId()) != null) {
                    writer.write("retained, ");
                }
                //noinspection ConstantConditions
                Context context = mSessions.get(session).get();
                writer.write((context == null ? "leaked" : context.toString()) + "\n");
                if (args.contains("--refs") || args.contains("--all")) {
                    session.dumpRefs(writer, "\t  ");
                }
            }
        }
    }
}