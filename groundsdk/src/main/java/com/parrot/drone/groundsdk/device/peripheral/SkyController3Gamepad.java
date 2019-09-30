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
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.RemoteControl.Model;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.AxisMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonEvent;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.ButtonsMappingEntry;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.skycontroller3.MappingEntry;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;

import java.util.Map;
import java.util.Set;

/**
 * Gamepad peripheral interface for {@link Model#SKY_CONTROLLER_3 SkyController3} {@link RemoteControl devices}.
 * <p>
 * This peripheral allows: <ul>
 * <li>To receive events when physical inputs (buttons/axes) on the device are triggered.</li>
 * <li>To configure mappings between combinations of events produced by such physical inputs and predefined
 * actions to execute or events to forward to the application when such combinations are triggered.</li>
 * </ul>
 * To start receiving events, {@link Button buttons} and/or {@link Axis axes} must be grabbed and event listeners
 * must be provided.
 * <br>
 * When a gamepad input is grabbed, the remote control will stop forwarding events associated to this input to the
 * connected drone (if any) and instead forward those events to the application-provided listener.
 * <br>
 * Each input may produce at least one, but possibly multiple specific events, which is documented in {@link Button}
 * for button inputs, and in {@link Axis} for axis inputs.
 * <p>
 * To stop receiving events, the input must be released, and by doing so the remote control will resume forwarding
 * that input's events back to the connected drone instead. <br>
 * Alternatively the application can unregister its event listeners to stop receiving events from all grabbed inputs
 * altogether. Note, however, that doing so does not release any input, so the drone still won't receive the grabbed
 * input events.
 * <p>
 * To receive input events, the application must register some listener to which those events will be forwarded.<br>
 * Event listeners come in two kind, depending on the event to be listened to: <ul>
 * <li>A {@link ButtonEvent.Listener button event listener} that receives events from inputs producing
 * {@link ButtonEvent button events}.<br>
 * This listener also provides the physical state of the associated input, i.e. whether the associated button is
 * {@link ButtonEvent.State#PRESSED pressed} or {@link ButtonEvent.State#RELEASED released}.<br>
 * Note that physical axes produce a button press event every time they reach the start or end of their course,
 * and a button release event every time they quit that position.</li>
 * <li>A {@link AxisEvent.Listener axis event listener} that receives events from inputs producing
 * {@link AxisEvent axis events}.<br>
 * This listener also provides the current value of the associated input, i.e. an int value in range [-100, 100]
 * that represent the current position of the axis, where -100 corresponds to the axis at start of its course
 * (left for horizontal axes, down for vertical axes), and 100 represents the axis at end of its course
 * (right for horizontal axes, up for vertical axes).</li>
 * </ul>
 * <p>
 * A mapping defines a set of actions that may each be triggered by a specific combination of inputs events
 * (buttons, and/or axes) produced by the remote control.
 * <p>
 * SkyController 3 remote control comes with default mappings for
 * {@link #getSupportedDroneModels() supported drone models}. <br>
 * Those mappings can be edited and are persisted on the remote control device: entries can be modified, removed,
 * and new entries can be added as well.
 * <p>
 * An {@link MappingEntry entry} in a mapping defines the association between such an action, the drone model on which
 * it should apply, and the combination of input events that should trigger the action. <br>
 * Two different kind of entries are available: <ul>
 * <li>A {@link ButtonsMappingEntry buttons mapping entry} allows to trigger a {@link ButtonsMappableAction} when
 * the gamepad inputs produce some set of {@link ButtonEvent button events} in the
 * {@link ButtonEvent.State#PRESSED pressed} state.</li>
 * <li>A {@link AxisMappingEntry axis mapping entry} allows to trigger an {@link AxisMappableAction} when the
 * gamepad inputs produce some {@link AxisEvent}, optionally in conjunction with some set of
 * {@link ButtonEvent button events} in the {@link ButtonEvent.State#PRESSED pressed} state.</li>
 * </ul>
 * This peripheral can be obtained from a {@code SkyController3 RemoteControl} using:
 * <pre>{@code remoteControl.getPeripheral(SkyController3.class)}</pre>
 *
 * @see Provider#getPeripheral(Class)
 * @see Provider#getPeripheral(Class, Ref.Observer)
 */
public interface SkyController3Gamepad extends Peripheral {

    /** A physical button input that can be grabbed on SkyController3 gamepad. */
    enum Button {

        /**
         * Top-most button on the front of the controller, immediately above {@link #FRONT_BOTTOM_BUTTON}, featuring
         * a return-home icon print.
         * <p>
         * Produces {@link ButtonEvent#FRONT_TOP_BUTTON} events when grabbed.
         */
        FRONT_TOP_BUTTON,

        /**
         * Bottom-most button on the front of the controller, immediately below {@link #FRONT_TOP_BUTTON}, featuring
         * a takeoff/land icon print.
         * <p>
         * Produces {@link ButtonEvent#FRONT_BOTTOM_BUTTON} events when grabbed.
         */
        FRONT_BOTTOM_BUTTON,

        /**
         * Left-most button on the rear of the controller, immediately above {@link Axis#LEFT_SLIDER}, featuring a
         * centering icon print.
         * <ul>
         * <li>Produces {@link ButtonEvent#REAR_LEFT_BUTTON} events when grabbed.</li>
         * <li>Produces {@link VirtualGamepad.Event#OK} events when {@link VirtualGamepad} is grabbed.</li>
         * </ul>
         */
        REAR_LEFT_BUTTON,

        /**
         * Right-most button on the rear of the controller, immediately above {@link Axis#RIGHT_SLIDER}, featuring a
         * take-photo/record icon print.
         * <ul>
         * <li>Produces {@link ButtonEvent#REAR_RIGHT_BUTTON} events when grabbed.</li>
         * <li>Produces {@link VirtualGamepad.Event#CANCEL} events when {@link VirtualGamepad} is grabbed.</li>
         * </ul>
         */
        REAR_RIGHT_BUTTON,
    }

    /** A physical axis input that can be grabbed on SkyController3 gamepad. */
    enum Axis {

        /**
         * Horizontal (left/right) axis of the left control stick.
         * <ul>
         * <li>Produces {@link ButtonEvent#LEFT_STICK_LEFT}, {@link ButtonEvent#LEFT_STICK_RIGHT} and
         * {@link AxisEvent#LEFT_STICK_HORIZONTAL} events when grabbed.</li>
         * <li>Produces {@link VirtualGamepad.Event#LEFT} and {@link VirtualGamepad.Event#RIGHT} events when
         * {@link VirtualGamepad} is grabbed.</li>
         * </ul>
         */
        LEFT_STICK_HORIZONTAL,

        /**
         * Vertical (down/up) axis of the left control stick.
         * <ul>
         * <li>Produces {@link ButtonEvent#LEFT_STICK_DOWN}, {@link ButtonEvent#LEFT_STICK_UP} and
         * {@link AxisEvent#LEFT_STICK_VERTICAL} events when grabbed.</li>
         * <li>Produces {@link VirtualGamepad.Event#DOWN} and {@link VirtualGamepad.Event#UP} events when
         * {@link VirtualGamepad} is grabbed.</li>
         * </ul>
         */
        LEFT_STICK_VERTICAL,

        /**
         * Horizontal (left/right) axis of the right control stick.
         * <p>
         * Produces {@link ButtonEvent#RIGHT_STICK_LEFT}, {@link ButtonEvent#RIGHT_STICK_RIGHT} and
         * {@link AxisEvent#RIGHT_STICK_HORIZONTAL} events when grabbed.
         */
        RIGHT_STICK_HORIZONTAL,

        /**
         * Vertical (down/up) axis of the right control stick.
         * <p>
         * Produces {@link ButtonEvent#RIGHT_STICK_DOWN}, {@link ButtonEvent#RIGHT_STICK_UP} and
         * {@link AxisEvent#RIGHT_STICK_VERTICAL} events when grabbed.
         */
        RIGHT_STICK_VERTICAL,

        /**
         * Slider on the rear, to the left of the controller, immediately below {@link Button#REAR_LEFT_BUTTON},
         * featuring a gimbal icon print.
         * <p>
         * Produces {@link ButtonEvent#LEFT_SLIDER_UP}, {@link ButtonEvent#LEFT_SLIDER_DOWN} and
         * {@link AxisEvent#LEFT_SLIDER} events when grabbed.
         */
        LEFT_SLIDER,

        /**
         * Slider on the rear, to the right of the controller, immediately below {@link Button#REAR_RIGHT_BUTTON},
         * featuring a zoom icon print.
         * <p>
         * Produces {@link ButtonEvent#RIGHT_SLIDER_UP}, {@link ButtonEvent#RIGHT_SLIDER_DOWN} and
         * {@link AxisEvent#RIGHT_SLIDER} events when grabbed.
         */
        RIGHT_SLIDER
    }

    /**
     * Sets the application listener for button events.
     * <p>
     * Registers the provided listener to be notified of grabbed input button events, or unregisters any currently
     * registered listener. <br>
     * There can be only one registered listener at a time, i.e. registering a different listener will unregister any
     * previous one.
     * <p>
     * Application should unregister any registered listener when done with event handling. <br>
     * Note however that any registered listener will get automatically unregistered as soon as this peripheral
     * disappears.
     *
     * @param listener the listener to register. Use {@code null} to unregister current listener
     *
     * @return this SkyController3Gamepad instance, to allow call chaining
     */
    @NonNull
    SkyController3Gamepad setButtonEventListener(@Nullable ButtonEvent.Listener listener);

    /**
     * Sets the application listener for axis events.
     * <p>
     * Registers the provided listener to be notified of grabbed input axis events, or unregisters any currently
     * registered listener. <br>
     * There can be only one registered listener at a time, i.e. registering a different listener will unregister any
     * previous one.
     * <p>
     * Application should unregister any registered listener when done with event handling. <br>
     * Note however that any registered listener will get automatically unregistered as soon as this peripheral
     * disappears.
     *
     * @param listener the listener to register. Use {@code null} to unregister current listener
     *
     * @return this SkyController3Gamepad instance, to allow call chaining
     */
    @NonNull
    SkyController3Gamepad setAxisEventListener(@Nullable AxisEvent.Listener listener);

    /**
     * Grabs gamepad inputs.
     * <p>
     * Grabs the given sets of button and axes, requiring the skycontroller3 device to send events from those inputs
     * to application listeners instead forwarding them of the drone.
     * <p>
     * The provided sets of inputs completely overrides the current sets of grabbed inputs (if any). So, for instance,
     * to release all inputs, this method should be called with empty buttons and axes sets. <br>
     * To grab or release some specific inputs without altering the rest of the grabbed inputs,
     * {@link #getGrabbedButtons()} and {@link #getGrabbedAxes()} may be used to construct new sets of inputs to
     * provide to this method.
     *
     * @param buttons set of buttons to be grabbed
     * @param axes    set of axes to be grabbed
     *
     * @see #getGrabbedButtons()
     * @see #getGrabbedAxes()
     */
    void grabInputs(@NonNull Set<Button> buttons, @NonNull Set<Axis> axes);

    /**
     * Gets currently grabbed buttons.
     * <p>
     * The application gets its own copy of the grabbed buttons set, which can be freely modified.
     *
     * @return set of currently grabbed buttons
     */
    @NonNull
    Set<Button> getGrabbedButtons();

    /**
     * Gets currently grabbed axes.
     * <p>
     * The application gets its own copy of the grabbed axes set, which can be freely modified.
     *
     * @return set of currently grabbed axes
     */
    @NonNull
    Set<Axis> getGrabbedAxes();

    /**
     * Gets the current state of grabbed button-event-producing inputs.
     * <p>
     * This provides a snapshot of the state of all currently grabbed gamepad inputs that may produce button events.
     * <br>
     * Each button event that may be produced by one of the grabbed inputs is mapped to the current state of the
     * input, i.e. whether the associated button is {@link ButtonEvent.State#PRESSED pressed} or
     * {@link ButtonEvent.State#RELEASED released}.
     * <p>
     * The application gets its own copy of grabbed buttons state map, which can be freely modified.
     *
     * @return a map associating each button event that may be produced by currently grabbed gamepad inputs to the
     *         current state of the input
     */
    @NonNull
    Map<ButtonEvent, ButtonEvent.State> getGrabbedButtonsState();

    /**
     * Gets the drone models supported by the remote control.
     * <p>
     * This defines the set of drone models for which the application can edit mappings.
     * <p>
     * The application gets its own copy of the supported drone models set, which can be freely modified.
     *
     * @return set of supported drone models
     */
    @NonNull
    Set<Drone.Model> getSupportedDroneModels();

    /**
     * Gets the currently active drone model.
     * <p>
     * The active drone model is the model of the drone currently connected through the remote control, or the latest
     * connected drone's model if the remote control is not connected to any drone at the moment.
     *
     * @return currently active drone model
     */
    @Nullable
    Drone.Model getActiveDroneModel();

    /**
     * Gets a drone model mapping.
     * <p>
     * This retrieves the set of all mapping entries currently defined for the provided drone model.
     * <p>
     * The application gets its own copy of the mapping entry set, which can be freely modified.
     *
     * @param droneModel the drone model for which to retrieve the mapping
     *
     * @return set of current mapping entries as configured for the provided drone model (possibly empty if no entry
     *         is defined for that model), otherwise {@code null} in case the drone model is not supported.
     *
     * @see #getSupportedDroneModels()
     */
    @Nullable
    Set<MappingEntry> getMapping(@NonNull Drone.Model droneModel);

    /**
     * Registers a mapping entry.
     * <p>
     * This allows to setup a new mapping entry in a drone model's mapping (in case the entry's action is not
     * registered yet in the drone mapping) or to modify an existing entry (in case the entry's action is already
     * registered in the drone mapping). <br>
     * If the drone model is supported, the entry gets persisted in the corresponding mapping on the remote control.
     * <p>
     * Note however that adding or editing a mapping entry may have impact on other existing entries in the same
     * mapping, since the same combination of input events cannot be used on more than one mapping entry at the same
     * time. <br>
     * As a result, when hitting such a situation, the existing conflicting entry is removed, and the new entry is
     * registered instead.
     *
     * @param mappingEntry mapping entry to register
     *
     * @see #unregisterMappingEntry(MappingEntry)
     */
    void registerMappingEntry(@NonNull MappingEntry mappingEntry);

    /**
     * Unregisters a mapping entry.
     * <p>
     * This allows to remove a mapping entry from a drone model's mapping. <br>
     * If the drone model is supported, the entry gets persistently removed from the corresponding mapping on the
     * remote control.
     *
     * @param mappingEntry mapping entry to unregister
     *
     * @see #registerMappingEntry(MappingEntry)
     */
    void unregisterMappingEntry(@NonNull MappingEntry mappingEntry);

    /**
     * Resets a drone model mapping to its default (built-in) value.
     *
     * @param droneModel the drone model for which to reset the mapping
     */
    void resetDefaultMappings(@NonNull Drone.Model droneModel);

    /**
     * Resets all supported drone models' mappings to their default (built-in) value.
     */
    void resetAllDefaultMappings();

    /**
     * Sets the interpolation formula to be applied on an axis.
     * <p>
     * An axis interpolator affects the values sent to the connected drone when moving the gamepad axis. <br>
     * It maps the physical linear position of the axis to another value by applying a predefined formula.
     * <p>
     * Note that the current interpolator set on an axis also affects the values sent through
     * {@link AxisEvent.Listener axis event listeners} for grabbed inputs.
     *
     * @param droneModel   drone model for which the axis interpolator must be applied
     * @param axis         axis to set the interpolator for
     * @param interpolator interpolator to set
     *
     * @see #getAxisInterpolators
     */
    void setAxisInterpolator(@NonNull Drone.Model droneModel, @NonNull Axis axis,
                             @NonNull AxisInterpolator interpolator);

    /**
     * Gets all axis interpolators currently applied on a given drone model.
     * <p>
     * The application gets its own copy of the axis interpolator map, which can be freely modified.
     *
     * @param droneModel drone model whose axis interpolators must be retrieved
     *
     * @return a map associating each controller axis to the currently applying interpolator, or {@code null} if the
     *         provided drone model is not supported
     *
     * @see #setAxisInterpolator
     */
    @Nullable
    Map<Axis, AxisInterpolator> getAxisInterpolators(@NonNull Drone.Model droneModel);

    /**
     * Reverses a gamepad axis.
     * <p>
     * A reversed axis produces values reversed symmetrically around the axis standstill value (0). <br>
     * For instance, an horizontal axis will produce values from 100 when held at (left) start of its course,
     * to -100, when held at (right) end of its course, while when not reversed, it will produce values from -100 when
     * held at (left) start of its course, to 100 when held at (right) end of its course. <br>
     * Same thing applies to vertical axes, where produced values will range from 100 (bottom start) to -100 (top end)
     * instead of -100 (bottom start) to 100 (top end).
     * <p>
     * Reversing an already {@link #getReversedAxes reversed axis} sets the axis back to normal operation mode.
     * <p>
     * The axis inversion stage occurs <strong>before</strong> any interpolation formula is applied.
     * <p>
     * Note that axis inversion has no effect whatsoever on the values sent through
     * {@link AxisEvent.Listener axis event listeners} for grabbed inputs. In other words, when receiving grabbed axes
     * events, it can be considered that the axis is never reversed.
     *
     * @param droneModel drone model for which the axis must be reversed
     * @param axis       axis to reverse
     *
     * @see #getReversedAxes
     */
    void reverseAxis(@NonNull Drone.Model droneModel, @NonNull Axis axis);

    /**
     * Gets all currently reversed axis for a given drone model.
     * <p>
     * The application gets its own copy of the reversed axes set, which can be freely modified.
     *
     * @param droneModel drone model whose reversed axes must be retrieved
     *
     * @return the set of currently reversed axes, or {@code null} if the provided drone model is not supported
     *
     * @see #reverseAxis
     */
    @Nullable
    Set<Axis> getReversedAxes(@NonNull Drone.Model droneModel);

    /**
     * Gives access to the volatile mapping setting.
     * <p>
     * All mapping entries registered with volatile mapping enabled will be removed when it is disabled or when
     * remote control is disconnected. Disabling volatile mapping also cancels any ongoing action.
     * <p>
     * Volatile mapping may be unsupported depending on the firmware version. Hence, clients of this API should call
     * {@link OptionalBooleanSetting#isAvailable() isAvailable} method on the returned value to check whether it can be
     * considered valid before use.
     * <p>
     * <strong>Note:</strong> this setting is not persistent.
     *
     * @return volatile mapping setting
     */
    @NonNull
    OptionalBooleanSetting volatileMapping();
}
