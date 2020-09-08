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

package com.parrot.drone.groundsdkdemo.peripheral;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.MainCamera;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.ThermalCamera;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.stream.GsdkStreamView;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.MultiChoiceSettingView;
import com.parrot.drone.groundsdkdemo.settings.ToggleSettingView;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class ThermalStreamActivity extends GroundSdkActivityBase {

    private StreamServer mStreamServer;

    private CameraLive mCameraLive;

    private ThermalControl mThermalControl;

    private GsdkStreamView mStreamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_thermal_stream);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());
        if (drone == null) {
            finish();
            return;
        }

        ToggleSettingView enableSwitch = findViewById(R.id.enable_stream);
        enableSwitch.setAvailable(true);

        mStreamServer = drone.getPeripheral(StreamServer.class, streamServer -> {
            if (streamServer == null) {
                finish();
            } else {
                boolean enabled = streamServer.streamingEnabled();
                enableSwitch
                        .setToggled(enabled)
                        .setListener(() -> streamServer.enableStreaming(!enabled));
            }
        }).get();

        if (mStreamServer == null) {
            finish();
            return;
        }


        mStreamView = findViewById(R.id.stream_view);

        mCameraLive = mStreamServer.live(cameraLive -> {
            mStreamView.setStream(cameraLive);
            Stream.State state = cameraLive == null ? null : cameraLive.state();
            CameraLive.PlayState playState = cameraLive == null ? null : cameraLive.playState();

            TextView stateText = findViewById(R.id.stream_state);
            stateText.setText(getString(R.string.live_stream_state_format, state, playState));

            Button playPauseButton = findViewById(R.id.stream_play_pause);
            playPauseButton.setEnabled(cameraLive != null);
            playPauseButton.setText(getString(playState == CameraLive.PlayState.PLAYING ?
                    R.string.action_pause : R.string.action_play));
            playPauseButton.setOnClickListener(v -> {
                if (cameraLive != null) {
                    if (cameraLive.playState() == CameraLive.PlayState.PLAYING) {
                        cameraLive.pause();
                    } else {
                        cameraLive.play();
                    }
                }
            });

            Button stopButton = findViewById(R.id.stream_stop);
            stopButton.setEnabled(state != Stream.State.STOPPED);
            stopButton.setOnClickListener(v -> {
                if (cameraLive != null && cameraLive.state() != Stream.State.STOPPED) {
                    cameraLive.stop();
                }
            });
        }).get();

        if (mCameraLive == null) {
            finish();
            return;
        }

        MultiChoiceSettingView<ThermalControl.Mode>  thermalModeView = findViewById(R.id.thermal_mode);

        mThermalControl = drone.getPeripheral(ThermalControl.class, thermalControl -> {
            if (thermalControl == null) {
                finish();
            } else {

                thermalModeView.setListener(chosenItem -> {
                    mCameraLive.stop();
                    thermalControl.mode().setValue(chosenItem);
                }).setChoices(thermalControl.mode().getAvailableValues())
                        .setSelection(thermalControl.mode().getValue())
                        .setUpdating(thermalControl.mode().isUpdating());
            }
        }).get();

        if (mThermalControl == null) {
            finish();
            return;
        }


        TextView mainCameraActiveView = findViewById(R.id.main_camera_active);
        drone.getPeripheral(MainCamera.class, mainCamera -> {
            if (mainCamera == null) {
                finish();
            } else {
                mainCameraActiveView.setText(Boolean.toString(mainCamera.isActive()));
                playIfActive(mainCamera);
            }
        });

        TextView thermalCameraActiveView = findViewById(R.id.thermal_camera_active);
        drone.getPeripheral(ThermalCamera.class, thermalCamera -> {
            if (thermalCamera == null) {
                finish();
            } else {
                thermalCameraActiveView.setText(Boolean.toString(thermalCamera.isActive()));
                playIfActive(thermalCamera);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mStreamView != null) {
            mStreamView.setStream(null);
        }
        super.onDestroy();
    }

    private void playIfActive(@NonNull Camera camera) {
        ThermalControl.Mode mode = camera instanceof MainCamera ?
                ThermalControl.Mode.DISABLED : ThermalControl.Mode.STANDARD;
        if (camera.isActive()
            && mThermalControl.mode().getValue() == mode
            && mStreamServer.streamingEnabled()) {
            mCameraLive.play();
        }
    }
}
