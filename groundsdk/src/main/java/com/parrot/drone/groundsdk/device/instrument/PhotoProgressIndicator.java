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

package com.parrot.drone.groundsdk.device.instrument;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.value.OptionalDouble;

/**
 * Instrument that informs about photo capture progress.
 * <p>
 * This instrument can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getInstrument(PhotoProgressIndicator.class)}</pre>
 *
 * @see Drone#getInstrument(Class)
 * @see Drone#getInstrument(Class, Ref.Observer)
 */
public interface PhotoProgressIndicator extends Instrument {

    /**
     * Retrieves the remaining time before the next photo is taken, in seconds.
     * <p>
     * This value is only available when the current {@link CameraPhoto.Mode photo mode} is
     * {@link CameraPhoto.Mode#TIME_LAPSE time-lapse} and {@link CameraPhoto.State#get() photo capture} is
     * {@link CameraPhoto.State.FunctionState#STARTED started}. <br>
     * It may also be unsupported depending on the drone model and/or firmware version. <br>
     * Hence, clients of this API should call {@link OptionalDouble#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return remaining time before next photo
     */
    @NonNull
    OptionalDouble getRemainingTime();

    /**
     * Retrieves the remaining distance before the next photo is taken, in meters.
     * <p>
     * This value is only available when the current {@link CameraPhoto.Mode photo mode} is
     * {@link CameraPhoto.Mode#GPS_LAPSE GPS-lapse} and {@link CameraPhoto.State#get() photo capture} is
     * {@link CameraPhoto.State.FunctionState#STARTED started}. <br>
     * It may also be unsupported depending on the drone model and/or firmware version. <br>
     * Hence, clients of this API should call {@link OptionalDouble#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return remaining distance before next photo
     */
    @NonNull
    OptionalDouble getRemainingDistance();
}
