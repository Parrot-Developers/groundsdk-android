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

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdkdemo.R;

class VirtualGamepadContent extends PeripheralContent<RemoteControl, VirtualGamepad> {

    @NonNull
    private final GrabEventListener mGrabEventListener;

    @Nullable
    private VirtualGamepad.Event mLastEvent;

    @Nullable
    private VirtualGamepad.Event.State mLastEventState;

    VirtualGamepadContent(@NonNull RemoteControl rc) {
        super(R.layout.virtual_gamepad_info, rc, VirtualGamepad.class);
        mGrabEventListener = new GrabEventListener();
    }

    @Override
    void onContentUnavailable() {
        mLastEvent = null;
        mLastEventState = null;
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private final class GrabEventListener implements VirtualGamepad.Event.Listener {

        @Override
        public void onEvent(@NonNull VirtualGamepad.Event event, @NonNull VirtualGamepad.Event.State state) {
            mLastEvent = event;
            mLastEventState = state;
            contentChanged();
        }
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<VirtualGamepadContent, VirtualGamepad> {

        @NonNull
        private final Button mGrabButton;

        @NonNull
        private final TextView mGrabState;

        @NonNull
        private final TextView mLastEventText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mGrabButton = findViewById(R.id.btn_grab);
            mGrabButton.setOnClickListener(mGrabClickListener);
            mGrabState = findViewById(R.id.state);
            mLastEventText = findViewById(R.id.last_event);
        }

        @Override
        void onBind(@NonNull VirtualGamepadContent content, @NonNull VirtualGamepad gamepad) {
            VirtualGamepad.State state = gamepad.getState();
            mGrabButton.setText(state == VirtualGamepad.State.RELEASED ? R.string.action_grab
                    : R.string.action_release);
            mGrabButton.setEnabled(state != VirtualGamepad.State.RELEASED || gamepad.canGrab());
            mGrabState.setText(state.toString());
            mLastEventText.setText(content.mLastEvent == null ? null : mContext.getString(R.string.button_event_format,
                    content.mLastEvent, content.mLastEventState));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mGrabClickListener = new OnClickListener() {

            @Override
            public void onClick(View v, @NonNull VirtualGamepadContent content, @NonNull VirtualGamepad gamepad) {
                if (gamepad.canGrab()) {
                    gamepad.grab(content.mGrabEventListener);
                } else {
                    gamepad.release();
                }
            }
        };
    }
}
