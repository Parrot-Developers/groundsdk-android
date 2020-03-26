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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.BooleanSetting;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.DoubleSetting;

import java.util.EnumSet;
import java.util.Set;

/**
 * Gimbal peripheral interface.
 * <p>
 * The gimbal is the peripheral "holding" and orientating the camera. It can be a real mechanical gimbal, or a software
 * one.
 * <p>
 * The gimbal can act on one or multiple axes. It can stabilize a given axis, meaning that the movement on this axis
 * will be following the horizon (for {@link Axis#PITCH} and {@link Axis#ROLL}) or the North (for the {@link Axis#YAW}).
 * <p>
 * Two frames of reference are used to {@link #control(ControlMode, Double, Double, Double) control} the gimbal with the
 * {@link ControlMode#POSITION POSITION} mode, and to retrieve the gimbal
 * {@link #getAttitude(Axis, FrameOfReference) attitude}.
 * <p>
 * {@link FrameOfReference#ABSOLUTE ABSOLUTE}:
 * <ul>
 * <li>yaw: given angle is relative to the magnetic North (clockwise).</li>
 * <li>pitch: given angle is relative to the horizon.
 * Positive pitch values means an orientation towards sky.</li>
 * <li>roll: given angle is relative to the horizon line.
 * Positive roll values means an orientation to the right when seeing the gimbal from behind.</li>
 * </ul><p>
 * {@link FrameOfReference#RELATIVE RELATIVE}:
 * <ul>
 * <li>yaw: given angle is relative to the heading of the drone.
 * Positive yaw values means a right orientation when seeing the gimbal from above.</li>
 * <li>pitch: given angle is relative to the body of the drone.
 * Positive pitch values means an orientation of the gimbal towards the top of the drone.</li>
 * <li>roll: given angle is relative to the body of the drone.
 * Positive roll values means an clockwise rotation of the gimbal.</li>
 * </ul>
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(Gimbal.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface Gimbal extends Peripheral {

    /** Gimbal axis. */
    enum Axis {

        /** Yaw axis of the gimbal. */
        YAW,

        /** Pitch axis of the gimbal. */
        PITCH,

        /** Roll axis of the gimbal. */
        ROLL
    }

    /** Frame of reference. */
    enum FrameOfReference {

        /** Absolute frame of reference. */
        ABSOLUTE,

        /** Frame of reference relative to the drone. */
        RELATIVE
    }

    /** Way of controlling the gimbal. */
    enum ControlMode {

        /** Mode that allows controlling the gimbal giving position target. */
        POSITION,

        /** Mode that allows controlling the gimbal giving velocity target. */
        VELOCITY
    }

    /** Gimbal calibration process state. */
    enum CalibrationProcessState {

        /** No ongoing calibration process. */
        NONE,

        /** Calibration process in progress. */
        CALIBRATING,

        /**
         * Calibration was successful.
         * <p>
         * This result is transient; {@link #getCalibrationProcessState()} will change back to
         * {@link CalibrationProcessState#NONE} immediately after success is notified.
         */
        SUCCESS,

        /**
         * Calibration failed.
         * <p>
         * This result is transient; {@link #getCalibrationProcessState()} will change back to
         * {@link CalibrationProcessState#NONE} immediately after failure is notified.
         */
        FAILURE,

        /**
         * Calibration was canceled.
         * <p>
         * This result is transient; {@link #getCalibrationProcessState()} will change back to
         * {@link CalibrationProcessState#NONE} immediately after canceled is notified.
         */
        CANCELED
    }

    /** Gimbal offsets manual correction process. */
    interface OffsetCorrectionProcess {

        /**
         * Gets axes that can be manually corrected.
         * <p>
         * When the drone is disconnected, axes can't be manually correctable, so the returned {@code Set} is empty.
         * <p>
         * Use {@link #getOffset} to correct axes.
         *
         * @return a set of calibratable axes
         */
        @NonNull
        Set<Axis> getCorrectableAxes();

        /**
         * Retrieves the offset setting applied to an axis, in degrees.
         * <p>
         * The given {@code axis} <strong>MUST</strong> be a {@link #getCorrectableAxes() correctable axis}.
         *
         * @param axis the axis
         *
         * @return offset setting for the specified axis
         *
         * @throws IllegalArgumentException in case the specified axis is not a correctable axis
         */
        @NonNull
        DoubleSetting getOffset(@NonNull Axis axis);
    }

    /**
     * Gets all supported axes, i.e. axes that can be controlled.
     *
     * @return a set of supported axes
     */
    @NonNull
    Set<Axis> getSupportedAxes();

    /**
     * Retrieves attitude bounds of an axis in the current frame of reference, in degrees.
     * <p>
     * The given {@code axis} <strong>MUST</strong> be a {@link #getSupportedAxes() supported axis}.
     *
     * @param axis the axis
     *
     * @return attitude bounds for the specified axis
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    @NonNull
    DoubleRange getAttitudeBounds(@NonNull Axis axis);

    /**
     * Retrieves the maximum speed setting of an axis, in degrees per second.
     * <p>
     * The given {@code axis} <strong>MUST</strong> be a {@link #getSupportedAxes() supported axis}.
     *
     * @param axis the axis
     *
     * @return maximum speed setting for the specified axis
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    @NonNull
    DoubleSetting getMaxSpeed(@NonNull Axis axis);

    /**
     * Gets currently locked axes.
     * <p>
     * While an axis is locked, you can not set a speed or a position.
     * <p>
     * Note: the result only contains supported axes.
     *
     * @return a set of currently locked axes
     */
    @NonNull
    Set<Axis> getLockedAxes();

    /**
     * Retrieves the stabilization setting of an axis.
     * <p>
     * The given {@code axis} <strong>MUST</strong> be a {@link #getSupportedAxes() supported axis}.
     *
     * @param axis the axis
     *
     * @return stabilization setting for the specified axis
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    @NonNull
    BooleanSetting getStabilization(@NonNull Axis axis);

    /**
     * Retrieves current attitude of an axis in the current frame of reference, in degrees.
     * <p>
     * The current frame of reference is {@link FrameOfReference#ABSOLUTE ABSOLUTE} if the axis is stabilized, and is
     * {@link FrameOfReference#RELATIVE RELATIVE} if it is not.
     * <p>
     * The given {@code axis} <strong>MUST</strong> be a {@link #getSupportedAxes() supported axis}.
     *
     * @param axis the axis
     *
     * @return the attitude for the specified axis
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    double getAttitude(@NonNull Axis axis);

    /**
     * Retrieves current attitude of an axis in a given frame of reference, in degrees.
     * <p>
     * The given {@code axis} <strong>MUST</strong> be a {@link #getSupportedAxes() supported axis}.
     *
     * @param axis  the axis
     * @param frame the frame of reference
     *
     * @return the attitude for the specified axis in the specified frame of reference
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    double getAttitude(@NonNull Axis axis, @NonNull FrameOfReference frame);

    /**
     * Controls the gimbal.
     * <p>
     * Unit of the {@code yaw}, {@code pitch}, {@code roll} values depends on the value of the {@code mode} parameter:
     * <ul>
     * <li>{@link ControlMode#POSITION POSITION}: axis value is in degrees and represents the desired position of
     * the gimbal on the given axis.</li>
     * <li>{@link ControlMode#VELOCITY VELOCITY}: axis value is in max velocity
     * ({@link #getMaxSpeed(Axis)}.getValue()) ratio (from -1 to 1).</li>
     * </ul><p>
     * If mode is {@link ControlMode#POSITION POSITION}, frame of reference of a given axis depends on the value of the
     * stabilization on this axis. If this axis is stabilized (i.e. {@link #getStabilization(Axis)}.isEnabled()), the
     * {@link FrameOfReference#ABSOLUTE ABSOLUTE} frame of reference is used. Otherwise, the
     * {@link FrameOfReference#RELATIVE RELATIVE} frame of reference is used.
     *
     * @param mode  the mode that should be used to move the gimbal. This parameter will change the unit of the
     *              following parameters
     * @param yaw   target on the yaw axis, or {@code null} if you want to keep the current value
     * @param pitch target on the pitch axis, or {@code null} if you want to keep the current value
     * @param roll  target on the roll axis, or {@code null} if you want to keep the current value
     */
    void control(@NonNull ControlMode mode, @Nullable Double yaw, @Nullable Double pitch, @Nullable Double roll);

    /**
     * Gets offsets correction process.
     *
     * @return the offsets correction process if the process is started, otherwise {@code null}
     *
     * @see #startOffsetsCorrectionProcess()
     */
    @Nullable
    OffsetCorrectionProcess getOffsetCorrectionProcess();

    /**
     * Starts the offsets correction process.
     * <p>
     * When offsets correction is started, {@link #getOffsetCorrectionProcess()} returns a non-{@code null} value
     * and correctable offsets can be corrected.
     * <p>
     * Note: no change if the process is already started.
     */
    void startOffsetsCorrectionProcess();

    /**
     * Stops the offsets correction process.
     * <p>
     * When offsets correction is stopped, {@link #getOffsetCorrectionProcess()} returns a {@code null} value.
     */
    void stopOffsetsCorrectionProcess();

    /**
     * Tells whether the gimbal is calibrated.
     *
     * @return {@code true} if the gimbal is calibrated, otherwise {@code false}
     */
    boolean isCalibrated();

    /**
     * Gets calibration process state.
     *
     * @return calibration state
     */
    @NonNull
    CalibrationProcessState getCalibrationProcessState();

    /**
     * Starts calibration process.
     * <p>
     * Does nothing when {@link #getCalibrationProcessState() calibration state} is
     * {@link CalibrationProcessState#CALIBRATING}.
     */
    void startCalibration();

    /**
     * Cancels the current calibration process.
     * <p>
     * Does nothing when {@link #getCalibrationProcessState() calibration state} is not
     * {@link CalibrationProcessState#CALIBRATING}.
     */
    void cancelCalibration();

    /** Represents an error that the gimbal may undergo. */
    enum Error {

        /**
         * Calibration error.
         * <p>
         * May happen during manual or automatic calibration.
         * <p>
         * Application should inform the user that the gimbal is currently inoperable and suggest to verify that
         * nothing currently hinders proper gimbal movement.
         * <p>
         * The device will retry calibration regularly; after several failed attempts, it will escalate the current
         * error to {@link #CRITICAL critical} level, at which point the gimbal becomes inoperable until both the issue
         * is fixed and the device is restarted.
         */
        CALIBRATION,

        /**
         * Overload error.
         * <p>
         * May happen during normal operation of the gimbal.
         * <p>
         * Application should inform the user that the gimbal is currently inoperable and suggest to verify that
         * nothing currently hinders proper gimbal movement.
         * <p>
         * The device will retry stabilization regularly; after several failed attempts, it will escalate the current
         * error to {@link #CRITICAL critical} level, at which point the gimbal becomes inoperable until both the issue
         * is fixed and the device is restarted.
         */
        OVERLOAD,

        /**
         * Communication error.
         * <p>
         * Communication with the gimbal is broken due to some unknown software and/or hardware
         * issue.
         * <p>
         * Application should inform the user that the gimbal is currently inoperable. <br>
         * However, there is nothing the application should recommend the user to do at that point: either the issue
         * will hopefully resolve itself (most likely a software issue), or it will escalate to critical level
         * (probably hardware issue) and the application should recommend the user to send back the device for repair.
         * <p>
         * The device will retry stabilization regularly; after several failed attempts, it will escalate the current
         * error to {@link #CRITICAL critical} level, at which point the gimbal becomes inoperable until both the issue
         * is fixed and the device is restarted.
         */
        COMMUNICATION,

        /**
         * Critical error.
         * <p>
         * May occur at any time; in particular, occurs when any of the other errors persists after
         * multiple retries from the device.
         * <p>
         * Application should inform the user that the gimbal has become completely inoperable until both the issue is
         * fixed and the device is restarted, as well as suggest to verify that nothing currently hinders proper gimbal
         * movement and that the gimbal is not damaged in any way.
         */
        CRITICAL
    }

    /**
     * Reports any error that the gimbal is currently undergoing.
     * <p>
     * When empty, the gimbal can be operated normally, otherwise, it is currently inoperable. <br>
     * In case the returned set contains the {@link Gimbal.Error#CRITICAL} error, then gimbal has become completely
     * inoperable until both all other reported errors are fixed and the device is restarted.
     *
     * @return current gimbal errors
     */
    @NonNull
    EnumSet<Error> currentErrors();
}
