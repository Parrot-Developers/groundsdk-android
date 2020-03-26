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
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.engine.activation.ActivationEngine;
import com.parrot.drone.groundsdk.internal.engine.blackbox.BlackBoxEngine;
import com.parrot.drone.groundsdk.internal.engine.crashreport.CrashReportEngine;
import com.parrot.drone.groundsdk.internal.engine.firmware.FirmwareEngine;
import com.parrot.drone.groundsdk.internal.engine.flightdata.FlightDataEngine;
import com.parrot.drone.groundsdk.internal.engine.flightlog.FlightLogEngine;
import com.parrot.drone.groundsdk.internal.engine.gutmalog.GutmaLogEngine;
import com.parrot.drone.groundsdk.internal.engine.reversegeocoder.ReverseGeocoderEngine;
import com.parrot.drone.groundsdk.internal.engine.system.SystemEngine;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central control of all engines' lifecycle.
 * <p>
 * The {@code EnginesController} is started once the first {@code GroundSdk} session is opened, and stopped when the
 * last session is closed.
 * <p>
 * Upon construction, this controller will instantiate all internal engines and also load external engines that are
 * declared in the application manifest. <br>
 * All those engines are started when this controller starts. <br>
 * When the controller stops, it requests all engines to stop. Each engine may decide not to stop immediately for
 * various reasons. When an engine is finally ready to stop, it notifies this controller. Then, when all engines have
 * declared being ready to stop, the controller stops them all.
 */
public class EnginesController {

    /** Called back when all managed engines have finally come to a stop. */
    public interface OnStopListener {

        /**
         * Notifies that all managed engines are finally stopped.
         */
        void onStop();
    }

    /** All engines, either internal or external. */
    private final Set<EngineBase> mEngines;

    /**
     * Constructor.
     *
     * @param context         android application context
     * @param utilityRegistry groundsdk utility registry
     * @param facilityStore   groundsdk facility store
     */
    public EnginesController(@NonNull Context context, @NonNull UtilityRegistry utilityRegistry,
                             @NonNull ComponentStore<Facility> facilityStore) {
        mEngines = createEngines(new EngineBase.Controller(context, utilityRegistry, facilityStore));
    }

    /**
     * Creates all internal and load all external engines.
     *
     * @param controller interface used to forward the utility registry and the android context to each engine
     *
     * @return a set of all created engines
     */
    private static Set<EngineBase> createEngines(@NonNull EngineBase.Controller controller) {
        HashSet<EngineBase> engines = new LinkedHashSet<>();
        // create internal engines
        engines.add(new SystemEngine(controller));
        engines.add(new UserLocationEngine(controller));
        engines.add(new AutoConnectionEngine(controller));
        engines.add(new ActivationEngine(controller));
        engines.add(new UserAccountEngine(controller));

        GroundSdkConfig config = GroundSdkConfig.get(controller.mContext);

        if (Geocoder.isPresent()) {
            engines.add(new ReverseGeocoderEngine(controller));
        }

        if (config.isFirmwareEnabled()) {
            engines.add(new FirmwareEngine(controller));
        }

        if (config.isFlightDataEnabled()) {
            engines.add(new FlightDataEngine(controller));
        }

        if (config.isGutmaLogEnabled()) {
            engines.add(new GutmaLogEngine(controller));
        }

        if (config.hasApplicationKey()) {
            if (config.isBlackBoxEnabled()) {
                engines.add(new BlackBoxEngine(controller));
            }
            if (config.isCrashReportEnabled()) {
                engines.add(new CrashReportEngine(controller));
            }
            if (config.isFlightLogEnabled()) {
                engines.add(new FlightLogEngine(controller));
            }
        }

        // load external engines
        engines.addAll(ExternalEngines.load(controller));
        return engines;
    }

    /**
     * Starts the controller.
     */
    public void start() {
        for (EngineBase engine : mEngines) {
            engine.requestStart();
        }
        // notify that all engines are now started
        for (EngineBase engine : mEngines) {
            engine.onAllEnginesStarted();
        }
    }

    /**
     * Stops the controller.
     *
     * @param stopListener listener notified when the controller finally stops
     */
    public void stop(@NonNull OnStopListener stopListener) {
        Set<EngineBase> unacknowledgedEngines = new HashSet<>(mEngines);
        for (EngineBase engine : mEngines) {
            engine.requestStop(acknowledgedEngine -> {
                unacknowledgedEngines.remove(acknowledgedEngine);
                if (unacknowledgedEngines.isEmpty()) {
                    for (EngineBase stoppableEngine : mEngines) {
                        stoppableEngine.stop();
                    }
                    stopListener.onStop();
                }
            });
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--engines: dump engines state\n");
        } else if (args.contains("--engines") || args.contains("--all")) {
            writer.write("Engines: " + mEngines.size() + "\n");
            for (EngineBase engine : mEngines) {
                engine.dumpState(writer, "\t");
            }
        }
        for (EngineBase engine : mEngines) {
            engine.dump(writer, args);
        }
    }

    /**
     * Constructor for test mocks.
     *
     * @param engines set of engines controlled by this controller
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    EnginesController(@NonNull Set<? extends EngineBase> engines) {
        mEngines = Collections.unmodifiableSet(engines);
    }
}
