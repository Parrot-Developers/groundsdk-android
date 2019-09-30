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

package com.parrot.drone.groundsdkdemo.peripheral.gamepad;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class GamepadGrabActivity extends GroundSdkActivityBase {

    @Nullable
    private GamepadFacade mGamepad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RemoteControl rc = groundSdk().getRemoteControl(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (rc == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gamepad_grab);

        ListView listView = findViewById(android.R.id.list);
        Adapter adapter = new Adapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Input item = adapter.getItem(position);
            assert item != null && mGamepad != null;
            item.toggleGrab(mGamepad);
        });

        GamepadFacadeProvider.of(rc).getPeripheral(GamepadFacade.class, gamepad -> {
            if (gamepad == null) {
                finish();
            } else {
                mGamepad = gamepad;
                adapter.notifyDataSetChanged();
            }
        });
    }

    private abstract static class Input {

        private static final class ButtonInput extends Input {

            @NonNull
            private final GamepadFacade.Button mButton;

            ButtonInput(@NonNull GamepadFacade.Button button) {
                mButton = button;
            }

            @Override
            public String toString() {
                return mButton.toString();
            }

            @Override
            void toggleGrab(@NonNull GamepadFacade gamepad) {
                Set<GamepadFacade.Button> buttons = gamepad.getGrabbedButtons();
                if (buttons.contains(mButton)) {
                    buttons.remove(mButton);
                } else {
                    buttons.add(mButton);
                }
                gamepad.grabInputs(buttons, gamepad.getGrabbedAxes());
            }

            @Override
            boolean isGrabbed(@NonNull GamepadFacade gamepad) {
                return gamepad.getGrabbedButtons().contains(mButton);
            }
        }

        private static final class AxisInput extends Input {

            @NonNull
            private final GamepadFacade.Axis mAxis;

            AxisInput(@NonNull GamepadFacade.Axis axis) {
                mAxis = axis;
            }

            @Override
            public String toString() {
                return mAxis.toString();
            }

            @Override
            void toggleGrab(@NonNull GamepadFacade gamepad) {
                Set<GamepadFacade.Axis> axes = gamepad.getGrabbedAxes();
                if (axes.contains(mAxis)) {
                    axes.remove(mAxis);
                } else {
                    axes.add(mAxis);
                }
                gamepad.grabInputs(gamepad.getGrabbedButtons(), axes);
            }

            @Override
            boolean isGrabbed(@NonNull GamepadFacade gamepad) {
                return gamepad.getGrabbedAxes().contains(mAxis);
            }
        }

        abstract void toggleGrab(@NonNull GamepadFacade gamepad);

        abstract boolean isGrabbed(@NonNull GamepadFacade gamepad);


        @NonNull
        static List<Input> from(@NonNull GamepadFacade gamepad) {
            List<Input> inputs = new ArrayList<>();
            for (GamepadFacade.Button button : gamepad.allButtons()) {
                inputs.add(new ButtonInput(button));
            }
            for (GamepadFacade.Axis axis : gamepad.allAxes()) {
                inputs.add(new AxisInput(axis));
            }
            return inputs;
        }
    }

    private class Adapter extends ArrayAdapter<Input> {

        Adapter() {
            super(GamepadGrabActivity.this, android.R.layout.simple_list_item_multiple_choice);
        }

        @Override
        public void notifyDataSetChanged() {
            if (getCount() == 0 && mGamepad != null) {
                addAll(Input.from(mGamepad));
            } else {
                super.notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
            Input item = getItem(position);
            assert item != null && mGamepad != null;
            view.setChecked(item.isGrabbed(mGamepad));
            return view;
        }
    }
}
