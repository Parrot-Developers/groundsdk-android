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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.stream.GsdkStreamView;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.sdkcore.ulog.ULog;
import com.parrot.drone.sdkcore.ulog.ULogTag;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

@SuppressLint("SetTextI18n")
public class VideoStreamActivity extends GroundSdkActivityBase {

    private GsdkStreamView mStreamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());
        if (drone == null) {
            finish();
            return;
        }

        ToggleButton enableSwitch = findViewById(R.id.enable_stream);

        StreamServer streamServer = drone.getPeripheral(StreamServer.class, it -> {
            if (it == null) {
                finish();
            } else {
                enableSwitch.setChecked(it.streamingEnabled());
            }
        }).get();

        if (streamServer == null) {
            finish();
            return;
        }

        enableSwitch.setOnClickListener(v -> streamServer.enableStreaming(enableSwitch.isChecked()));

        mStreamView = findViewById(R.id.stream_view);

        streamServer.live(stream -> {
            if (stream != null) {
                ULog.e(new ULogTag("arsdkcore"), "stream [state:" + stream.state()
                                                 + ", playState: " + stream.playState() + "]");
            }
            mStreamView.setStream(stream);
            Stream.State state = stream == null ? null : stream.state();
            CameraLive.PlayState playState = stream == null ? null : stream.playState();

            TextView stateText = findViewById(R.id.stream1_state);
            stateText.setText("live: " + state + " " + playState);

            Button playPauseBtn = findViewById(R.id.stream1_play_pause);
            playPauseBtn.setEnabled(stream != null);
            playPauseBtn.setText(playState == CameraLive.PlayState.PLAYING ? "pause" : "play");
            playPauseBtn.setOnClickListener(v -> {
                if (stream != null) {
                    if (stream.playState() == CameraLive.PlayState.PLAYING) {
                        stream.pause();
                    } else {
                        stream.play();
                    }
                }
            });

            Button stopBtn = findViewById(R.id.stream1_stop);
            stopBtn.setEnabled(state != Stream.State.STOPPED);
            stopBtn.setOnClickListener(v -> {
                if (stream != null && stream.state() != Stream.State.STOPPED) {
                    stream.stop();
                }
            });
        });

        streamServer.live(stream -> {
            Stream.State state = stream == null ? null : stream.state();
            CameraLive.PlayState playState = stream == null ? null : stream.playState();

            TextView stateText = findViewById(R.id.stream2_state);
            stateText.setText("live: " + state + " " + playState);

            Button playPauseBtn = findViewById(R.id.stream2_play_pause);
            playPauseBtn.setEnabled(stream != null);
            playPauseBtn.setText(playState == CameraLive.PlayState.PLAYING ? "pause" : "play");
            playPauseBtn.setOnClickListener(v -> {
                if (stream != null) {
                    if (stream.playState() == CameraLive.PlayState.PLAYING) {
                        stream.pause();
                    } else {
                        stream.play();
                    }
                }
            });

            Button stopBtn = findViewById(R.id.stream2_stop);
            stopBtn.setEnabled(state != Stream.State.STOPPED);
            stopBtn.setOnClickListener(v -> {
                if (stream != null && stream.state() != Stream.State.STOPPED) {
                    stream.stop();
                }
            });
        });

//        streamServer.replay(MOCK_RES, stream -> {
//            Stream.State state = stream == null ? null : stream.state();
//            MediaReplay.PlayState playState = stream == null ? null : stream.playState();
//
//            TextView stateText = findViewById(R.id.stream3_state);
//            stateText.setText("replay A: " + state + " " + playState);
//
//            Button playPauseBtn = findViewById(R.id.stream3_play_pause);
//            playPauseBtn.setEnabled(stream != null);
//            playPauseBtn.setText(playState == MediaReplay.PlayState.PLAYING ? "pause" : "play");
//            playPauseBtn.setOnClickListener(v -> {
//                if (stream != null) {
//                    if (stream.playState() == MediaReplay.PlayState.PLAYING ) {
//                        stream.pause();
//                    } else {
//                        stream.play();
//                    }
//                }
//            });
//
//            Button stopBtn = findViewById(R.id.stream3_stop);
//            stopBtn.setEnabled(state != Stream.State.STOPPED);
//            stopBtn.setOnClickListener(v -> {
//                if (stream != null && stream.state() != Stream.State.STOPPED) {
//                    stream.stop();
//                }
//            });
//        });
//
//        streamServer.replay(MOCK_RES, stream -> {
//            Stream.State state = stream == null ? null : stream.state();
//            MediaReplay.PlayState playState = stream == null ? null : stream.playState();
//
//            TextView stateText = findViewById(R.id.stream4_state);
//            stateText.setText("replay B: " + state + " " + playState);
//
//            Button playPauseBtn = findViewById(R.id.stream4_play_pause);
//            playPauseBtn.setEnabled(stream != null);
//            playPauseBtn.setText(playState == MediaReplay.PlayState.PLAYING ? "pause" : "play");
//            playPauseBtn.setOnClickListener(v -> {
//                if (stream != null) {
//                    if (stream.playState() == MediaReplay.PlayState.PLAYING ) {
//                        stream.pause();
//                    } else {
//                        stream.play();
//                    }
//                }
//            });
//
//            Button stopBtn = findViewById(R.id.stream4_stop);
//            stopBtn.setEnabled(state != Stream.State.STOPPED);
//            stopBtn.setOnClickListener(v -> {
//                if (stream != null && stream.state() != Stream.State.STOPPED) {
//                    stream.stop();
//                }
//            });
//        });
    }

    @Override
    protected void onDestroy() {
        if (mStreamView != null) {
            mStreamView.setStream(null);
        }
        super.onDestroy();
    }
}
