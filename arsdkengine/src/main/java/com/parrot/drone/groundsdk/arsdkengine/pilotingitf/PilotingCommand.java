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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;

/**
 * A generic piloting command that may be encoded into various specific piloting ArsdkCommands.
 * <p>
 * Note that the piloting command value fields are written to from the main thread while they are read from the pcmd
 * loop that runs in the pomp loop thread.
 */
public final class PilotingCommand {

    /** Piloting command common roll value. */
    private volatile int mRoll;

    /** Piloting command common pitch value. */
    private volatile int mPitch;

    /** Piloting command common yaw value. */
    private volatile int mYaw;

    /** Piloting command common gaz value. */
    private volatile int mGaz;

    /**
     * Retrieves the current flag value.
     *
     * @return flag value
     */
    public int getFlag() {
        return mRoll == 0 && mPitch == 0 ? 0 : 1;
    }

    /**
     * Retrieves the current roll value.
     *
     * @return roll value
     */
    public int getRoll() {
        return mRoll;
    }

    /**
     * Retrieves the current pitch value.
     *
     * @return roll value
     */
    public int getPitch() {
        return mPitch;
    }

    /**
     * Retrieves the current yaw value.
     *
     * @return roll value
     */
    public int getYaw() {
        return mYaw;
    }

    /**
     * Retrieves the current gaz value.
     *
     * @return gaz value
     */
    public int getGaz() {
        return mGaz;
    }

    /**
     * Abstract base for a PilotingCommand encoder.
     */
    public abstract static class Encoder implements ArsdkNoAckCmdEncoder {

        /** Current piloting command that is read and encoded by the pcmd loop. */
        @NonNull
        final PilotingCommand mPCmd;

        /**
         * Constructor.
         */
        private Encoder() {
            mPCmd = new PilotingCommand();
        }

        /**
         * Retrieves current piloting command.
         *
         * @return current piloting command
         */
        @NonNull
        public final PilotingCommand getPilotingCommand() {
            return mPCmd;
        }

        /**
         * Updates the current piloting command roll value.
         *
         * @param roll roll value to set
         *
         * @return {@code true} if setting this value changed the piloting command, otherwise {@code false}
         */
        public final boolean setRoll(int roll) {
            if (mPCmd.mRoll == roll) {
                return false;
            }
            mPCmd.mRoll = roll;
            return true;
        }

        /**
         * Updates the current piloting command pitch value.
         *
         * @param pitch pitch value to set
         *
         * @return {@code true} if setting this value changed the piloting command, otherwise {@code false}
         */
        public final boolean setPitch(int pitch) {
            if (mPCmd.mPitch == pitch) {
                return false;
            }
            mPCmd.mPitch = pitch;
            return true;
        }

        /**
         * Updates the current piloting command yaw value.
         *
         * @param yaw yaw value to set
         *
         * @return {@code true} if setting this value changed the piloting command, otherwise {@code false}
         */
        public final boolean setYaw(int yaw) {
            if (mPCmd.mYaw == yaw) {
                return false;
            }
            mPCmd.mYaw = yaw;
            return true;
        }

        /**
         * Updates the current piloting command gaz value.
         *
         * @param gaz gaz value to set
         *
         * @return {@code true} if setting this value changed the piloting command, otherwise {@code false}
         */
        public final boolean setGaz(int gaz) {
            if (mPCmd.mGaz == gaz) {
                return false;
            }
            mPCmd.mGaz = gaz;
            return true;
        }

        /**
         * Retrieves the period at which piloting command should be encoded.
         *
         * @return the piloting command encoding period, in milliseconds
         */
        public abstract int getPilotingCommandLoopPeriod();

        /**
         * Resets the encoder, setting all piloting command values to their defaults (0).
         */
        @CallSuper
        public void reset() {
            mPCmd.mRoll = mPCmd.mPitch = mPCmd.mGaz = mPCmd.mYaw = 0;
        }

        /**
         * Implementation of a PilotingCommand encoder for all Anafi family drones.
         * <p>
         * Anafi family piloting command encoders use a loop period of 50 milliseconds.
         */
        public static final class Anafi extends Encoder {

            /** Sequence number seed. */
            private int mSeqNr;

            @Override
            public int getPilotingCommandLoopPeriod() {
                return 50;
            }

            @Override
            public void reset() {
                super.reset();
                mSeqNr = 0;
            }

            @NonNull
            @Override
            public ArsdkCommand encodeNoAckCmd() {
                // negate pitch: positive pitch from the drone POV means tilted towards ground (i.e. forward move),
                // negative pitch means tilted towards sky (i.e. backward move)
                return ArsdkFeatureArdrone3.Piloting.encodePCMD(mPCmd.getFlag(), mPCmd.mRoll, -mPCmd.mPitch, mPCmd.mYaw,
                        mPCmd.mGaz, nextSequenceNumber());
            }

            /**
             * Generates subsequent piloting command sequence number and timestamp.
             *
             * @return sequence number to use to encode next command
             */
            private int nextSequenceNumber() {
                mSeqNr = (++mSeqNr) & 0xFF;
                return (mSeqNr << 24) + (int) TimeProvider.elapsedRealtime();
            }
        }
    }

    /**
     * Private constructor.
     */
    private PilotingCommand() {
    }
}
