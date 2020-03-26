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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.utility.Utility;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Set;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_ENGINE;

/**
 * Base class for an engine.
 * <p>
 * External engines must be subclasses of this class, and define a specific metadata entry under the application tag
 * in the {@code AndroidManifest.xml}. <br>
 * The name of the metadata entry must start with {@link #META_KEY}; the metadata value must be the fully qualified
 * name of the engine implementation class. For example:
 * <pre>{@code
 * <application ... >
 *     <meta-data
 *         android:name="com.parrot.drone.groundsdk.engine.myexternalengine"
 *         android:value="my.domain.package.MyExternalEngine"/>
 *     ...
 * </application>
 * }</pre>
 */
public class EngineBase {

    /**
     * External engine meta-data prefix in the manifest.
     * <p>
     * Each external engine must define a meta-data entry. The entry name must start with this key, followed by the
     * engine name and its value must be the fully qualified engine class.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String META_KEY = "com.parrot.drone.groundsdk.engine";

    /**
     * Interface allowing to be notified when the underlying engine implementation acknowledges a stop request.
     */
    interface OnStopRequestAcknowledgedListener {

        /**
         * Called back when an engine acknowledges a formerly issued stop request.
         *
         * @param engine engine that did acknowledge the stop request
         */
        void onStopRequestAcknowledged(@NonNull EngineBase engine);
    }

    /** Engine state. */
    private enum State {

        /**
         * Engine is stopped. From there on, it may only be {@link #STARTED started}. This transition will be signaled
         * by a call to {@link #onStart()}.
         */
        STOPPED,

        /**
         * Engine is started. From there on, it may only be {@link #STOP_REQUESTED requested to stop}. This transition
         * will be signaled by a call to {@link #onStopRequested()}.
         */
        STARTED,

        /**
         * Engine has been requested to stop. From there on: <ul>
         * <li>Either the stop request may be {@link #STOP_ACKNOWLEDGED acknowledged}. This happens when the
         * implementation subclass calls {@link #acknowledgeStopRequest()}; </li>
         * <li>or the stop request may be canceled, and the engine becomes {@link #STARTED started} again. This
         * transition will be signaled by a call to {@link #onStopRequestCanceled()}.</li>
         * </ul>
         */
        STOP_REQUESTED,

        /**
         * Engine has acknowledged a pending stop request. From there on, the engine controller may: <ul>
         * <li>either {@link #STOPPED stop} the engine. This transition will be signaled by a call to
         * {@link #onStop()};</li>
         * <li>or {@link #STARTED start} the engine again. This transition will be signaled by a call to
         * {@link #onStart()}.</li>
         * </ul>
         */
        STOP_ACKNOWLEDGED
    }

    /** Current engine state. */
    @NonNull
    private State mState;

    /** Listens to stop request acknowledgement. Set by {@link #requestStop}. */
    @Nullable
    private OnStopRequestAcknowledgedListener mStopAckListener;

    /**
     * Provides an interface to the engines controller.
     * <p>
     * This class is opaque to subclasses, which <strong>MUST</strong> forward the {@code Controller} instance
     * through {@link EngineBase#EngineBase(Controller) super} in their own constructor.
     */
    protected static final class Controller {

        /** Android application context. */
        @NonNull
        final Context mContext;

        /** GroundSdk utility registry. */
        @NonNull
        final UtilityRegistry mUtilityRegistry;

        /** Facility store. */
        @NonNull
        final ComponentStore<Facility> mFacilityStore;

        /**
         * Constructor.
         *
         * @param context         android application context
         * @param utilityRegistry groundsdk utility registry
         * @param facilityStore   store used to publish facilities
         */
        Controller(@NonNull Context context, @NonNull UtilityRegistry utilityRegistry,
                   @NonNull ComponentStore<Facility> facilityStore) {
            mContext = context;
            mUtilityRegistry = utilityRegistry;
            mFacilityStore = facilityStore;
        }
    }

    /** Engines controller interface. */
    @NonNull
    private final Controller mController;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    protected EngineBase(@NonNull Controller controller) {
        mController = controller;
        mState = State.STOPPED;
    }

    /**
     * Retrieves the android application context.
     *
     * @return android application context
     */
    @NonNull
    public final Context getContext() {
        return mController.mContext;
    }

    /**
     * Retrieves an utility interface.
     * <p>
     * <strong>IMPORTANT:</strong> this method should be called only once the engine is started. This restriction
     * allows to ensure that all engines have published their appropriate utilities at that point. <br>
     * Failure to observe this restriction will produce an {@link IllegalStateException}.
     *
     * @param utilityType class of the utility interface to obtain
     * @param <U>         type of the utility interface
     *
     * @return the utility with the requested interface if available, otherwise {@code null}
     *
     * @throws IllegalStateException if the engine is not started
     */
    @Nullable
    public final <U extends Utility> U getUtility(@NonNull Class<U> utilityType) {
        return internalGetUtility(utilityType);
    }

    /**
     * Retrieves an utility interface.
     * <p>
     * This method should only be used to retrieve utilities for which it has been clearly specified (in their javadoc)
     * that they are always available.
     * <p>
     * <strong>IMPORTANT:</strong> this method should be called only once the engine is started. This restriction
     * allows to ensure that all engines have published their appropriate utilities at that point. <br>
     * Failure to observe this restriction will produce an {@link IllegalStateException}.
     *
     * @param utilityType class of the utility interface to obtain
     * @param <U>         type of the utility interface
     *
     * @return the utility with the requested interface if available, otherwise {@code null}
     *
     * @throws IllegalStateException if the engine is not started
     * @throws AssertionError        if the engine is started but the requested utility is not registered
     */
    @NonNull
    public final <U extends Utility> U getUtilityOrThrow(@NonNull Class<U> utilityType) {
        U utility = getUtility(utilityType);
        if (utility == null) {
            throw new AssertionError("Utility " + utilityType.getSimpleName() + " not found");
        }
        return utility;
    }

    /**
     * Publishes an utility interface to groundsdk.
     * <p>
     * All engines may publish utilities and should do so immediately in their constructor, so that
     * all appropriate utilities are registered by the time any engine is started.
     *
     * @param <U>             type of the utility interface
     * @param utilityType     utility interface, used to index the utility instance
     * @param utilityInstance utility instance
     */
    protected final <U extends Utility> void publishUtility(@NonNull Class<U> utilityType, @NonNull U utilityInstance) {
        mController.mUtilityRegistry.registerUtility(utilityType, utilityInstance);
    }

    /**
     * Retrieves the facility publisher.
     *
     * @return facility publisher
     */
    @NonNull
    protected final ComponentStore<Facility> getFacilityPublisher() {
        return mController.mFacilityStore;
    }

    /**
     * Requests the engine to move to the {@link State#STARTED started state}.
     */
    final void requestStart() {
        State formerState = mState;
        mState = State.STARTED;
        switch (formerState) {
            case STARTED:
                break; // already started
            case STOP_REQUESTED:
                if (ULog.i(TAG_ENGINE)) {
                    ULog.i(TAG_ENGINE, "Canceling engine stop request: " + this);
                }
                mStopAckListener = null;
                onStopRequestCanceled();
                break;
            case STOP_ACKNOWLEDGED:
            case STOPPED:
                if (ULog.i(TAG_ENGINE)) {
                    ULog.i(TAG_ENGINE, "Starting engine: " + this);
                }
                onStart();
                break;
        }
    }

    /**
     * Public alias to {@link #requestStart()}. Used only by tests.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public final void start() {
        requestStart();
    }

    /**
     * Requests the engine to stop.
     *
     * @param listener listener to be notified when the underlying engine implementation acknowledges the stop request
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public final void requestStop(@Nullable OnStopRequestAcknowledgedListener listener) {
        switch (mState) {
            case STARTED:
                mStopAckListener = listener;
                mState = State.STOP_REQUESTED;
                if (ULog.i(TAG_ENGINE)) {
                    ULog.i(TAG_ENGINE, "Requesting engine stop: " + this);
                }
                onStopRequested();
                break;
            case STOPPED:
            case STOP_REQUESTED:
            case STOP_ACKNOWLEDGED:
                break; // already requested or stopped
        }
    }

    /**
     * Stops the engine.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public final void stop() {
        if (mState != State.STOP_ACKNOWLEDGED) {
            throw new IllegalStateException("Tried to stop " + this + " without prior stop request acknowledge");
        }
        if (ULog.i(TAG_ENGINE)) {
            ULog.i(TAG_ENGINE, "Stopping engine: " + this);
        }
        onStop();
        mState = State.STOPPED;
    }

    /**
     * Internal base implementation for {@code getUtility} methods.
     * <p>
     * Only exposed for easily injecting utilities when mocking engines.
     * Overriding mocks can also bypass the {@link State#STOPPED} state check.
     *
     * @param utilityType class of the utility interface to obtain
     * @param <U>         type of the utility interface
     *
     * @return the utility with the requested interface if available, otherwise {@code null}
     *
     * @throws IllegalStateException if the engine is not started
     */
    @VisibleForTesting
    @Nullable
    public <U extends Utility> U internalGetUtility(@NonNull Class<U> utilityType) {
        if (mState == State.STOPPED) {
            throw new IllegalStateException("Utilities are only available when the engine is started");
        }
        return mController.mUtilityRegistry.getUtility(utilityType);
    }

    /**
     * Called when the engine is started.
     * <p>
     * This method is called when the engine starts after either having been stopped or having acknowledged a prior
     * stop request.
     * <p>
     * Subclass may override this method to implement their specific behavior. Default implementation does nothing.
     */
    protected void onStart() {
    }

    /**
     * Called after all engines' {@link #onStart()} method have been called.
     * <p>
     * Subclass may override this method to implement their specific behavior. Default implementation does nothing.
     */
    protected void onAllEnginesStarted() {
    }

    /**
     * Called when the engine is requested to stop.
     * <p>
     * This method acts as a signal from GroundSdk that the engine is not needed anymore, at least externally. <br>
     * The underlying engine implementation may decide not to stop yet, for internal reasons, but should eventually
     * call {@link #acknowledgeStopRequest()} when it reaches an internal state where it is acceptable for itself to
     * be stopped.
     * <p>
     * Subclass may override this method to implement their specific behavior. Default implementation directly
     * acknowledges the stop request.
     */
    protected void onStopRequested() {
        acknowledgeStopRequest();
    }

    /**
     * Called when a non-acknowledged stop request becomes obsolete.
     * <p>
     * This method is called when the engine is started again after having been requested to stop,
     * but before the underlying implementation has acknowledged the stop request.
     * <p>
     * Subclass may override this method to implement their specific behavior. Default implementation does nothing.
     */
    protected void onStopRequestCanceled() {
    }

    /**
     * Called when the engine stops.
     * <p>
     * Subclass may override this method to implement their specific behavior. Default implementation does nothing.
     */
    protected void onStop() {
    }

    /**
     * Tells whether the engine has been requested to stop.
     *
     * @return {@code true} if there is a non-acknowledged stop request for this engine, otherwise {@code false}
     */
    protected final boolean isRequestedToStop() {
        return mState == State.STOP_REQUESTED;
    }

    /**
     * Tells whether the engine is stopped or has acknowledged a prior stop request.
     *
     * @return {@code true} if the engine is stopped or has acknowledged to get stopped, otherwise {@code false}
     */
    protected final boolean isStoppedOrAcknowledged() {
        return mState == State.STOP_ACKNOWLEDGED || mState == State.STOPPED;
    }

    /**
     * Acknowledges a pending stop request.
     * <p>
     * This informs GroundSdk that the engine is now prepared to be forcefully stopped at any time. <br>
     * After having called this method, the underlying implementation must be prepared for either the {@link #onStop()}
     * (if GroundSdk decides to stop the engine) or the {@link #onStart()} (if GroundSdk decides to restart the engine)
     * callback to be called at any time.
     * <p>
     * This method may be called <strong>only once</strong> after the engine has been requested to stop (the
     * {@link #onStopRequested()} callback has been called), and <strong>only if</strong> that request has not been
     * canceled yet (the {@link #onStopRequestCanceled()} callback has not been called).
     */
    protected final void acknowledgeStopRequest() {
        if (mState != State.STOP_REQUESTED) {
            throw new IllegalStateException(this + "tried to acknowledge unrequested stop");
        }
        mState = State.STOP_ACKNOWLEDGED;
        if (mStopAckListener != null) {
            mStopAckListener.onStopRequestAcknowledged(this);
            mStopAckListener = null;
        }
        if (ULog.i(TAG_ENGINE)) {
            ULog.i(TAG_ENGINE, "Engine ready to stop: " + this);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Engine-specific debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
    }

    /**
     * Engine state debug dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    void dumpState(@NonNull PrintWriter writer, @NonNull String prefix) {
        writer.write(prefix + this + ": " + mState + "\n");
    }
}
