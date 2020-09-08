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

package com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontrollerua;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import androidx.annotation.NonNull;

/**
 * A mapping entry that defines an {@link AxisMappableAction} to be triggered when the gamepad inputs produce
 * an {@link AxisEvent}, optionally in conjunction with a specific set of {@link ButtonEvent button events}
 * in the {@link ButtonEvent.State#PRESSED pressed} state.
 */
public final class AxisMappingEntry extends MappingEntry {

    /** Action to be triggered. */
    @NonNull
    private final AxisMappableAction mAction;

    /** Axis event that triggers the action. */
    @NonNull
    private final AxisEvent mAxisEvent;

    /** Set of button events that triggers the action when in the {@link ButtonEvent.State#PRESSED pressed} state. */
    @NonNull
    private final Set<ButtonEvent> mButtonEvents;

    /**
     * Constructor.
     *
     * @param droneModel   drone model onto which the entry should apply
     * @param action       action to be triggered
     * @param axisEvent    axis event that triggers the action
     * @param buttonEvents button event set that triggers the action
     */
    public AxisMappingEntry(@NonNull Drone.Model droneModel, @NonNull AxisMappableAction action,
                            @NonNull AxisEvent axisEvent,
                            @NonNull EnumSet<ButtonEvent> buttonEvents) {
        super(Type.AXIS_MAPPING, droneModel);
        mAction = action;
        mAxisEvent = axisEvent;
        mButtonEvents = buttonEvents.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(buttonEvents));
    }

    /**
     * Gets the action to be triggered.
     *
     * @return action to be triggered
     */
    @NonNull
    public AxisMappableAction getAction() {
        return mAction;
    }

    /**
     * Gets the axis event that triggers the action.
     *
     * @return axis event that triggers the action
     */
    @NonNull
    public AxisEvent getAxisEvent() {
        return mAxisEvent;
    }

    /**
     * Gets the set of button events that triggers the action when pressed.
     * <p>
     * The application gets a read only view of those events, which cannot be modified.
     *
     * @return set of button event that triggers the action
     */
    @NonNull
    public Set<ButtonEvent> getButtonEvents() {
        return mButtonEvents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        AxisMappingEntry that = (AxisMappingEntry) o;

        return mAction == that.mAction;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mAction.hashCode();
        return result;
    }
}
