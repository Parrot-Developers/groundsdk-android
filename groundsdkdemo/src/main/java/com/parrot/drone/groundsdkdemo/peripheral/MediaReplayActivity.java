/*
 * Copyright (C) 2019 Parrot Drones SAS
 */

package com.parrot.drone.groundsdkdemo.peripheral;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.stream.GsdkStreamView;
import com.parrot.drone.groundsdk.stream.Replay;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.groundsdkdemo.Extras;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class MediaReplayActivity extends GroundSdkActivityBase {

    private static final String EXTRA_STREAM_SOURCE = Extras.withKey("STREAM_SRC");

    private static final int REFRESH_INTERVAL = 100; // milliseconds

    public static void launch(@NonNull Context context, @NonNull Drone drone,
                              @NonNull MediaReplay.Source source) {
        context.startActivity(new Intent(context, MediaReplayActivity.class)
                .putExtra(EXTRA_DEVICE_UID, drone.getUid())
                .putExtra(EXTRA_STREAM_SOURCE, source));
    }

    private GsdkStreamView mStreamView;

    private ImageButton mPlayPauseBtn;

    private SeekBar mSeekBar;

    private TextView mPositionText;

    private TextView mDurationText;

    private Handler mProgressHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaReplay.Source source = getIntent().getParcelableExtra(EXTRA_STREAM_SOURCE);
        if (source == null) {
            finish();
            return;
        }

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (drone == null) {
            finish();
            return;
        }

        StreamServer streamServer = drone.getPeripheral(StreamServer.class);
        if (streamServer == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_media_replay);

        mStreamView = findViewById(R.id.stream_view);
        mPlayPauseBtn = findViewById(R.id.play_pause_btn);
        mSeekBar = findViewById(R.id.seek_bar);
        mPositionText = findViewById(R.id.position_text);
        mDurationText = findViewById(R.id.duration_text);

        mProgressHandler = new Handler();

        streamServer.replay(source, stream -> {
            if (stream != null) {
                updateStream(stream);
            } else {
                finish();
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

    private void updateStream(@NonNull Replay stream) {
        mStreamView.setStream(stream);

        Stream.State state = stream.state();
        Replay.PlayState playState = stream.playState();
        long duration = stream.duration();

        if (state == Stream.State.STOPPED) {
            stream.pause();
        } else if (state == Stream.State.STARTED && stream.position() >= duration) {
            stream.stop();
        }

        mPlayPauseBtn.setImageResource(playState == Replay.PlayState.PLAYING ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        mPlayPauseBtn.setOnClickListener(btn -> {
            if (playState == Replay.PlayState.PLAYING) {
                stream.pause();
            } else {
                stream.play();
            }
        });

        setTime(mDurationText, duration);

        Runnable progressUpdate = new Runnable() {

            @Override
            public void run() {
                int position = (int) stream.position();
                mSeekBar.setProgress(position);
                setTime(mPositionText, position);

                if (playState == Replay.PlayState.PLAYING) {
                    mProgressHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };

        mSeekBar.setEnabled(duration > 0);
        mSeekBar.setMax((int) duration);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setTime(mPositionText, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mProgressHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                stream.seekTo(seekBar.getProgress());
            }
        });

        mProgressHandler.removeCallbacksAndMessages(null);
        progressUpdate.run();
    }

    private static void setTime(@NonNull TextView view, long timeMillis) {
        view.setText(DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(timeMillis)));
    }
}
