/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.guided;

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;

import androidx.annotation.NonNull;

/** Core class for FinishedFlightInfo. */
public class FinishedFlightInfoCore implements GuidedPilotingItf.FinishedFlightInfo {

    /** Flight type. */
    private final GuidedPilotingItf.Type mType;

    /** {@code true} if the guided flight succeeded. */
    private final boolean mSuccess;

    private FinishedFlightInfoCore(@NonNull GuidedPilotingItf.Type type, boolean success) {
        mType = type;
        mSuccess = success;
    }

    @NonNull
    @Override
    public final GuidedPilotingItf.Type getType() {
        return mType;
    }

    @Override
    public final boolean wasSuccessful() {
        return mSuccess;
    }

    /** Core class for FinishedLocationFlightInfo. */
    public static class Location extends FinishedFlightInfoCore
            implements GuidedPilotingItf.FinishedLocationFlightInfo {

        /** Initial location directive. */
        @NonNull
        private final GuidedPilotingItf.LocationDirective mDirective;

        /**
         * Constructor.
         *
         * @param directive the initial directive
         * @param success   {@code true} if the guided flight succeeded
         */
        public Location(@NonNull GuidedPilotingItf.LocationDirective directive, boolean success) {
            super(GuidedPilotingItf.Type.ABSOLUTE_LOCATION, success);
            mDirective = directive;
        }


        @NonNull
        @Override
        public GuidedPilotingItf.LocationDirective getDirective() {
            return mDirective;
        }
    }

    /** Core class for FinishedRelativeMoveFlightInfo. */
    public static class Relative extends FinishedFlightInfoCore
            implements GuidedPilotingItf.FinishedRelativeMoveFlightInfo {

        /** Initial location directive. */
        @NonNull
        private final GuidedPilotingItf.RelativeMoveDirective mDirective;

        /** Forward component of the actual move. */
        private final double mActualForwardComponent;

        /** Right component of the actual move. */
        private final double mActualRightComponent;

        /** Downward component of the actual move. */
        private final double mActualDownwardComponent;

        /** Heading rotation of the actual move. */
        private final double mActualHeadingRotation;

        /**
         * Constructor.
         *
         * @param directive               the initial directive
         * @param success                 {@code true} if the guided flight succeeded
         * @param actualForwardComponent  forward component of the actual move
         * @param actualRightComponent    right component of the actual move
         * @param actualDownwardComponent downward component of the actual move
         * @param actualHeadingRotation   heading rotation component of the actual move
         */
        public Relative(@NonNull GuidedPilotingItf.RelativeMoveDirective directive, boolean success,
                        double actualForwardComponent, double actualRightComponent,
                        double actualDownwardComponent, double actualHeadingRotation) {
            super(GuidedPilotingItf.Type.RELATIVE_MOVE, success);
            mDirective = directive;
            mActualForwardComponent = actualForwardComponent;
            mActualRightComponent = actualRightComponent;
            mActualDownwardComponent = actualDownwardComponent;
            mActualHeadingRotation = actualHeadingRotation;
        }

        @NonNull
        @Override
        public GuidedPilotingItf.RelativeMoveDirective getDirective() {
            return mDirective;
        }

        @Override
        public double getActualForwardComponent() {
            return mActualForwardComponent;
        }

        @Override
        public double getActualRightComponent() {
            return mActualRightComponent;
        }

        @Override
        public double getActualDownwardComponent() {
            return mActualDownwardComponent;
        }

        @Override
        public double getActualHeadingRotation() {
            return mActualHeadingRotation;
        }
    }
}
