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

package com.parrot.drone.groundsdkdemo.hud;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.hmd.GsdkHmdView;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class HmdActivity extends GroundSdkActivityBase {

    private static final String[] HMD_MODELS = new String [] {
            "cockpitGlasses2",
            "aoguerbe",
            "bNext",
            "googleDayDreamView",
            "hamswanShineconY005",
            "homido",
            "homido2",
            "homidoPrime",
            "mergeVR",
            "samsungGearVR",
            "shinecon6G4E",
            "zeissVROne",
            "skillkorpVR5"
    };

    private static final float MILLIMETERS_PER_INCH = 25.4f;

    private GsdkHmdView mHmdView;

    private View mConfigPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        StreamServer streamServer = drone == null ? null : drone.getPeripheral(StreamServer.class);
        if (streamServer == null) {
            finish();
            return;
        }

        CameraLive live = streamServer.live(it -> {
            if (it == null) {
                finish();
            }
        }).get();


        if (live == null) {
            finish();
            return;
        }

        streamServer.enableStreaming(true);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        double screenHeightMm = metrics.heightPixels * MILLIMETERS_PER_INCH / metrics.ydpi;

        setContentView(R.layout.activity_hmd);

        mHmdView = findViewById(R.id.hmd);

        mHmdView.setStream(live);
        live.play();

        mConfigPanel = findViewById(R.id.config_panel);

        findViewById(R.id.hmd_overlay_progress).setVisibility(View.GONE);

        SeekBar leftOffsetBar = mConfigPanel.findViewById(R.id.left_lens_offset);

        SeekBar rightOffsetBar = mConfigPanel.findViewById(R.id.right_lens_offset);

        SeekBar vertOffsetBar = mConfigPanel.findViewById(R.id.vert_lenses_offset);
        vertOffsetBar.setOnSeekBarChangeListener((SeekBarChangeListener) (seekBar, progress, fromUser) ->
                mHmdView.setLensesVerticalOffset(screenHeightMm / 2 - progress * screenHeightMm / seekBar.getMax()));

        SwitchMaterial seeThroughSwitch = mConfigPanel.findViewById(R.id.see_through);
        seeThroughSwitch.setOnCheckedChangeListener((switchBtn, isChecked) -> mHmdView.enableSeeThrough(isChecked));

        vertOffsetBar.setProgress((int) Math.round((screenHeightMm / 2 - mHmdView.getLensesVerticalOffset())
                                                   * vertOffsetBar.getMax() / screenHeightMm));

        mHmdView.post(mAnimateOverlay);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mConfigPanel.setVisibility(View.VISIBLE);
        mConfigPanel.removeCallbacks(mHideConfigPanel);
        mConfigPanel.postDelayed(mHideConfigPanel, 3000);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mConfigPanel.removeCallbacks(mHideConfigPanel);
        mConfigPanel.setVisibility(View.GONE);
    }

    private final Runnable mHideConfigPanel = new Runnable() {

        @Override
        public void run() {
            mConfigPanel.setVisibility(View.GONE);
        }
    };

    private final Runnable mAnimateOverlay = new Runnable() {

        private int mCnt;

        @Override
        public void run() {
            TextView text = findViewById(R.id.hmd_overlay_text);
            View progress = findViewById(R.id.hmd_overlay_progress);
            LinearLayout.LayoutParams textLayoutParams = (LinearLayout.LayoutParams) text.getLayoutParams();

            if (mCnt % 2 == 0) {
                textLayoutParams.gravity = Gravity.START;
                progress.setVisibility(View.VISIBLE);

                LinearLayout.LayoutParams progressLayoutParams =
                        (LinearLayout.LayoutParams) progress.getLayoutParams();

                if (mCnt % 4 == 0) {
                    progressLayoutParams.gravity = Gravity.START;

                    if (mCnt % 8 == 0) {
                        String model = HMD_MODELS[(mCnt / 8) % HMD_MODELS.length];
                        mHmdView.setHmdModel(R.raw.gsdkdemo_hmd_models, model);
                        text.setText(model);
                    }
                } else {
                    progressLayoutParams.gravity = Gravity.END;
                }

                progress.setLayoutParams(progressLayoutParams);
            } else {
                textLayoutParams.gravity = Gravity.END;
                progress.setVisibility(View.GONE);
            }

            text.setLayoutParams(textLayoutParams);

            mCnt++;

            mHmdView.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        mHmdView.removeCallbacks(mAnimateOverlay);

        super.onDestroy();
    }

    private interface SeekBarChangeListener extends SeekBar.OnSeekBarChangeListener {

        @Override
        default void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        default void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
