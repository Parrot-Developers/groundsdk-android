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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.gimbal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal.FrameOfReference;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;

import java.util.EnumMap;
import java.util.EnumSet;

/** Gimbal control command encoder. */
class GimbalControlCommandEncoder implements ArsdkNoAckCmdEncoder {

    /** Maximum number of time the command should be sent with the same value. */
    private static final int GIMBAL_COMMANDS_REPETITIONS = 10;

    // region synchronized vars

    /** Desired control mode. */
    @NonNull
    private ArsdkFeatureGimbal.ControlMode mControlMode;

    /** Set of desired stabilized axes. */
    @NonNull
    private final EnumSet<Gimbal.Axis> mStabilizedAxes;

    /** Desired targets. Value for an axis is null if this axis should not be controlled. */
    @NonNull
    private final EnumMap<Gimbal.Axis, Double> mTargets;
    // endregion synchronized vars

    // region pomp loop access only vars

    /** Latest control mode. */
    @NonNull
    private ArsdkFeatureGimbal.ControlMode mLatestControlMode;

    /** Latest stabilized axes. */
    @NonNull
    private EnumSet<Gimbal.Axis> mLatestStabilizedAxes;

    /** Latest target. */
    @NonNull
    private EnumMap<Gimbal.Axis, Double> mLatestTargets;

    /** Frames of reference. */
    @NonNull
    private final EnumMap<Gimbal.Axis, FrameOfReference> mFramesOfReference;

    /** Remaining command repetition count. */
    private int mRepetitions;
    // endregion pomp loop access only vars

    /**
     * Constructor.
     */
    GimbalControlCommandEncoder() {
        mControlMode = mLatestControlMode = ArsdkFeatureGimbal.ControlMode.POSITION;
        mStabilizedAxes = EnumSet.noneOf(Gimbal.Axis.class);
        mTargets = new EnumMap<>(Gimbal.Axis.class);
        mLatestTargets = new EnumMap<>(Gimbal.Axis.class);
        mLatestStabilizedAxes = EnumSet.noneOf(Gimbal.Axis.class);
        mFramesOfReference = new EnumMap<>(Gimbal.Axis.class);
    }

    @Nullable
    @Override
    public ArsdkCommand encodeNoAckCmd() {
        ArsdkFeatureGimbal.ControlMode controlMode;
        EnumSet<Gimbal.Axis> stabilizedAxes;
        EnumMap<Gimbal.Axis, Double> targets;
        synchronized (this) {
            controlMode = mControlMode;
            stabilizedAxes = mStabilizedAxes;
            targets = mTargets;
        }

        // if control, target or stabilization has changed
        if (mLatestControlMode != controlMode || !mLatestStabilizedAxes.equals(stabilizedAxes)
            || !mLatestTargets.equals(targets)) {
            mLatestControlMode = controlMode;
            mLatestStabilizedAxes = stabilizedAxes.clone();
            mLatestTargets = targets.clone();
            mRepetitions = GIMBAL_COMMANDS_REPETITIONS;
        }

        // only decrement the counter if the control is in position,
        // or if the control is in velocity and all velocity targets are null or zero
        if (mRepetitions >= 0 &&
            (controlMode == ArsdkFeatureGimbal.ControlMode.POSITION || containsOnlyZeros(targets))) {
            mRepetitions--;
        }

        if (mRepetitions >= 0) {
            for (Gimbal.Axis axis : EnumSet.allOf(Gimbal.Axis.class)) {
                if (targets.get(axis) != null) {
                    mFramesOfReference.put(axis, stabilizedAxes.contains(axis) ? FrameOfReference.ABSOLUTE :
                            FrameOfReference.RELATIVE);
                } else {
                    mFramesOfReference.put(axis, FrameOfReference.NONE);
                }
            }

            Double yaw = targets.get(Gimbal.Axis.YAW);
            Double pitch = targets.get(Gimbal.Axis.PITCH);
            Double roll = targets.get(Gimbal.Axis.ROLL);
            //noinspection ConstantConditions: mFramesOfReference always as a value for all axes
            return ArsdkFeatureGimbal.encodeSetTarget(0,
                    controlMode,
                    mFramesOfReference.get(Gimbal.Axis.YAW),
                    yaw != null ? (float) yaw.doubleValue() : 0f,
                    mFramesOfReference.get(Gimbal.Axis.PITCH),
                    pitch != null ? (float) pitch.doubleValue() : 0f,
                    mFramesOfReference.get(Gimbal.Axis.ROLL),
                    roll != null ? (float) roll.doubleValue() : 0f);
        }
        return null;
    }

    /**
     * Tells if the given map has only null or zero values.
     *
     * @param map the map to check
     *
     * @return {@code true} if the map contains only null or zeros, otherwise {@code false}
     */
    private static boolean containsOnlyZeros(@NonNull EnumMap<Gimbal.Axis, Double> map) {
        boolean onlyZeros = true;
        for (Double target : map.values()) {
            if (target != null && target != 0.0) {
                onlyZeros = false;
                break;
            }
        }
        return onlyZeros;
    }

    /**
     * Controls the gimbal.
     *
     * @param mode  the control mode
     * @param yaw   the yaw target, or {@code null} if yaw should not change
     * @param pitch the pitch target, or {@code null} if pitch should not change
     * @param roll  the roll target, or {@code null} if roll should not change
     */
    synchronized void control(@NonNull Gimbal.ControlMode mode, @Nullable Double yaw, @Nullable Double pitch,
                              @Nullable Double roll) {
        switch (mode) {
            case POSITION:
                mControlMode = ArsdkFeatureGimbal.ControlMode.POSITION;
                break;
            case VELOCITY:
                mControlMode = ArsdkFeatureGimbal.ControlMode.VELOCITY;
                break;
        }
        mTargets.put(Gimbal.Axis.YAW, yaw);
        mTargets.put(Gimbal.Axis.PITCH, pitch);
        mTargets.put(Gimbal.Axis.ROLL, roll);
    }

    /**
     * Sets the stabilization on the given axis.
     *
     * @param axis           the axis
     * @param stabilized     the stabilization state
     * @param targetAttitude the target to set in the new frame of reference; if {@code null}, new stabilization will
     *                       only be sent when a target for this axis is set
     */
    synchronized void setStabilization(@NonNull Gimbal.Axis axis, boolean stabilized, @Nullable Double targetAttitude) {
        if (stabilized) {
            mStabilizedAxes.add(axis);
        } else {
            mStabilizedAxes.remove(axis);
        }

        if (mControlMode == ArsdkFeatureGimbal.ControlMode.POSITION) {
            mTargets.put(axis, targetAttitude);
        } else if (mTargets.get(axis) == null) {
            // In velocity mode, target should not be null to change stabilization
            mTargets.put(axis, 0.0);
        }
    }

    /**
     * Resets the encoder.
     * <p>
     * The encoder should not be registered to be executed in the pomp loop when this method is called.
     */
    void reset() {
        // we allow to modify these vars outside of the pomp loop without synchronizing them because the encoder
        // is not registered to be executed in the pomp loop.
        mControlMode = mLatestControlMode = ArsdkFeatureGimbal.ControlMode.POSITION;
        mTargets.clear();
        mLatestTargets.clear();
        mStabilizedAxes.clear();
        mLatestStabilizedAxes.clear();
        mRepetitions = 0;
    }
}
