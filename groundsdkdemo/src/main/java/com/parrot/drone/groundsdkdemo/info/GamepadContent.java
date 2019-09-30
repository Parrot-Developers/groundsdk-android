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

package com.parrot.drone.groundsdkdemo.info;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadAxesSetupActivity;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadGrabActivity;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadMappingsActivity;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadSettingsActivity;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class GamepadContent extends PeripheralContent<GamepadFacadeProvider, GamepadFacade> {

    @NonNull
    private final GrabEventListener mGrabEventListener;

    @Nullable
    private GamepadFacade.Button.Event mLastButton;

    @Nullable
    private GamepadFacade.Button.State mLastButtonState;

    @Nullable
    private GamepadFacade.Axis.Event mLastAxis;

    private int mLastAxisValue;

    GamepadContent(@NonNull RemoteControl rc) {
        super(R.layout.gamepad_info, GamepadFacadeProvider.of(rc), GamepadFacade.class);
        mGrabEventListener = new GrabEventListener();
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    @Override
    void onContentChanged(@NonNull GamepadFacade gamepad, boolean becameAvailable) {
        if (becameAvailable) {
            gamepad.setButtonEventListener(mGrabEventListener).setAxisEventListener(mGrabEventListener);
        }
    }

    @Override
    void onContentUnavailable() {
        mLastButton = null;
        mLastButtonState = null;
        mLastAxis = null;
        mLastAxisValue = -1;
    }

    private final class GrabEventListener implements GamepadFacade.Button.Event.Listener,
                                                     GamepadFacade.Axis.Event.Listener {

        @Override
        public void onAxisEvent(@NonNull GamepadFacade.Axis.Event event, @IntRange(from = -100, to = 100) int value) {
            mLastButton = null;
            mLastButtonState = null;
            mLastAxis = event;
            mLastAxisValue = value;
            contentChanged();
        }

        @Override
        public void onButtonEvent(@NonNull GamepadFacade.Button.Event event,
                                  @NonNull GamepadFacade.Button.State state) {
            mLastButton = event;
            mLastButtonState = state;
            mLastAxis = null;
            mLastAxisValue = -1;
            contentChanged();
        }
    }

    private static final class ViewHolder
            extends PeripheralContent.ViewHolder<GamepadContent, GamepadFacade> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditButton;

        @NonNull
        private final TextView mActiveModelText;

        @NonNull
        private final TextView mVolatileMappingText;

        @NonNull
        private final TextView mInputsText;

        @NonNull
        private final TextView mEventsText;

        @NonNull
        private final TextView mLastEventText;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mGrabButton;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mMappingButton;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mAxesSetupButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditButton = findViewById(R.id.btn_edit);
            mEditButton.setOnClickListener(mClickListener);
            mActiveModelText = findViewById(R.id.active_model);
            mVolatileMappingText = findViewById(R.id.volatile_mapping);
            mInputsText = findViewById(R.id.inputs);
            mEventsText = findViewById(R.id.events);
            mLastEventText = findViewById(R.id.last_event);
            mGrabButton = findViewById(R.id.btn_grab);
            mGrabButton.setOnClickListener(mClickListener);
            mMappingButton = findViewById(R.id.btn_mapping);
            mMappingButton.setOnClickListener(mClickListener);
            mAxesSetupButton = findViewById(R.id.btn_axes_setup);
            mAxesSetupButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull GamepadContent content, @NonNull GamepadFacade gamepad) {
            Drone.Model activeModel = gamepad.getActiveDroneModel();
            mActiveModelText.setText(activeModel == null ? null : activeModel.toString());
            mVolatileMappingText.setText(gamepad.getVolatileMapping().isEnabled() ?
                    R.string.boolean_setting_enabled : R.string.boolean_setting_disabled);
            List<Object> inputs = new ArrayList<>();
            inputs.addAll(gamepad.getGrabbedButtons());
            inputs.addAll(gamepad.getGrabbedAxes());
            mInputsText.setText(TextUtils.join(" ", inputs));

            Map<GamepadFacade.Button.Event, GamepadFacade.Button.State> grabState = gamepad.getGrabbedButtonsState();
            Set<String> events = new LinkedHashSet<>(); // keep insertion order
            for (GamepadFacade.Button.Event event : grabState.keySet()) {
                events.add(mContext.getString(grabState.get(event) == GamepadFacade.Button.State.PRESSED
                        ? R.string.pressed_button_format : R.string.released_button_format, event));
            }
            mEventsText.setText(Html.fromHtml(TextUtils.join(" ", events)));
            if (content.mLastButton != null) {
                mLastEventText.setText(
                        mContext.getString(R.string.button_event_format, content.mLastButton,
                                content.mLastButtonState));
            } else if (content.mLastAxis != null) {
                mLastEventText.setText(
                        mContext.getString(R.string.axis_event_format, content.mLastAxis, content.mLastAxisValue));
            } else {
                mLastEventText.setText(null);
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            public void onClick(View v, @NonNull GamepadContent content, @NonNull GamepadFacade gamepad) {
                Intent intent = null;
                if (v.getId() == R.id.btn_edit) {
                    intent = new Intent(mContext, GamepadSettingsActivity.class);
                } else if (v.getId() == R.id.btn_grab) {
                    intent = new Intent(mContext, GamepadGrabActivity.class);
                } else if (v.getId() == R.id.btn_mapping) {
                    intent = new Intent(mContext, GamepadMappingsActivity.class);
                } else if (v.getId() == R.id.btn_axes_setup) {
                    intent = new Intent(mContext, GamepadAxesSetupActivity.class);
                }
                if (intent != null) {
                    mContext.startActivity(intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                }
            }
        };
    }
}
