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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_SESSION;

/**
 * A GroundSdk Session.
 * <p>
 * A session manages the lifecycle of refs issued by various GroundSdk components. The sole way to create a ref
 * to be further handed to the application is to request it from a session.
 */
public class Session {

    /**
     * Base implementation for a Ref.
     *
     * @param <T> Type of the object the ref provides access to
     */
    public abstract static class RefBase<T> extends Ref<T> {

        /** Session managing this ref. */
        protected final Session mSession;

        /** Registered ref observer, notified of referenced object updates. */
        private final Ref.Observer<? super T> mObserver;

        /** Referenced object. */
        @Nullable
        private T mObject;

        /** {@code true} when the referenced object has been updated while the session was suspended. */
        private boolean mPendingNotify;

        /** {@code true} when the reference is closed. A closed reference does not notify any update. */
        private boolean mClosed;

        /**
         * Constructor.
         *
         * @param session  session that will manage this ref
         * @param observer observer that will be notified when the referenced object is updated
         */
        protected RefBase(@NonNull Session session, @NonNull Ref.Observer<? super T> observer) {
            if (session.mState == State.CLOSED) {
                throw new IllegalStateException("Cannot create ref, session is closed: " + session);
            }
            mSession = session;
            mObserver = observer;
            mSession.mRefs.add(this);
        }

        @Nullable
        @Override
        public T get() {
            return mObject;
        }

        @Override
        public void close() {
            if (!mClosed) {
                mClosed = true;
                mSession.mRefs.remove(this);
                release();
            }
        }

        /**
         * Releases the reference.
         * <p>
         * Base implementation nullifies the referenced object. Subclasses <strong>MUST</strong> unregister any
         * internal listener they had registered.
         * <p>
         * Must be called at the end of overridden methods.
         */
        @CallSuper
        protected void release() {
            mObject = null;
        }

        /**
         * Initialises the reference.
         * <p>
         * Subclasses may register internal listeners to watch referenced object changes as appropriate.
         *
         * @param object referenced object instance
         */
        protected final void init(@Nullable T object) {
            if (object != null) {
                update(object);
            }
        }

        /**
         * Updates referenced object and notify observer if appropriate.
         * <p>
         * In case the session managing this ref is suspended, the observer is not notified; instead, the object
         * update is cached until the session is resumed.
         *
         * @param object new referenced object instance
         */
        protected final void update(@Nullable T object) {
            if (!mClosed) {
                mObject = object;
                if (mSession.mState == State.RESUMED) {
                    mObserver.onChanged(mObject);
                } else {
                    mPendingNotify = true;
                }
            }
        }

        /**
         * Forwards any cached object update to the observer.
         * <p>
         * Called by the managing session when it gets resumed.
         */
        final void resume() {
            if (!mClosed && mPendingNotify) {
                mPendingNotify = false;
                mObserver.onChanged(mObject);
            }
        }

        /**
         * Retrieves a string representation of the kind of object managed by this ref.
         * <p>
         * Only for debug dump.
         *
         * @return content description
         */
        @NonNull
        protected String describeContent() {
            // as most ref implementation classes follow the '<Content>Ref' pattern, return the '<Content>' part from
            // the class name if possible. Refs for which this default is not ideal should override this method.
            String desc = getClass().getSimpleName();
            return desc.endsWith("Ref") ? desc.substring(0, desc.length() - 3) : desc;
        }
    }

    /** Specific identifier for an invalid session. */
    static final int INVALID_ID = -1;

    /** Session unique identifier. */
    private final int mSessionId;

    /** References managed by this session. */
    private final Set<RefBase<?>> mRefs;

    /** Represents a session's state. */
    private enum State {

        /** Session is suspended, refs cannot notify observers but may cache referenced object updates. */
        SUSPENDED,

        /** Session is resumed, refs may notify observers. */
        RESUMED,

        /** Session is closed, refs cannot be created anymore for this session. */
        CLOSED
    }

    /** Current session state. */
    @NonNull
    private State mState;

    /**
     * Constructor.
     */
    Session() {
        mSessionId = nextSessionId();
        mRefs = new CopyOnWriteArraySet<>();
        mState = State.SUSPENDED;
    }

    /**
     * Retrieves the session identifier.
     *
     * @return this session's id.
     */
    public final int getId() {
        return mSessionId;
    }

    /**
     * Resumes the session.
     * <p>
     * Asks all managed refs to forward any pending notification to their observer.
     */
    final void resumeObservers() {
        if (mState == State.SUSPENDED) {
            mState = State.RESUMED;
            for (RefBase<?> ref : mRefs) {
                ref.resume();
            }
        }
    }

    /**
     * Suspends the session.
     * <p>
     * Managed refs are not allowed to forward notification to their observers anymore, until the session is
     * {@link #resumeObservers() resumed}. <br>
     * Refs must instead cache the latest update request they receive (if any), and forward it to their observer
     * once the session is resumed.
     */
    final void suspendObservers() {
        if (mState == State.RESUMED) {
            mState = State.SUSPENDED;
        }
    }

    /**
     * Opens the session.
     * <p>
     * Put the session back to the initial suspended state.
     */
    final void open() {
        if (mState == State.CLOSED) {
            mState = State.SUSPENDED;
        }
    }

    /**
     * Closes the session.
     * <p>
     * All managed refs are released and removed. This session cannot be used anymore and must be disposed.
     */
    final void close() {
        if (mState != State.CLOSED) {
            mState = State.CLOSED;
            for (RefBase<?> ref : mRefs) {
                ref.release();
            }
            mRefs.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Session session = (Session) o;

        return mSessionId == session.mSessionId;
    }

    @Override
    public int hashCode() {
        return mSessionId;
    }

    @NonNull
    @Override
    public String toString() {
        return "Session [id: " + mSessionId + ", refs: " + mRefs.size() + ", state: " + mState + "]";
    }

    /**
     * Debug refs dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    void dumpRefs(@NonNull PrintWriter writer, @NonNull String prefix) {
        for (RefBase<?> ref : mRefs) {
            writer.print(prefix + "- " + ref.describeContent() + " [" + ref.mObject + "] <- " + ref.mObserver + "\n");
        }
    }

    /** Last issued session id. INVALID_ID until the first session is created. */
    private static int sSessionIdBase = INVALID_ID;

    /**
     * Gets the next available session identifier.
     *
     * @return next available session id
     */
    private static int nextSessionId() {
        int nextId = ++sSessionIdBase;
        if (nextId == INVALID_ID) {
            if (ULog.i(TAG_SESSION)) {
                ULog.i(TAG_SESSION, "Session ids wrapped");
            }
            nextId = ++sSessionIdBase;
        }
        return nextId;
    }
}
