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

package com.parrot.drone.groundsdkdemo.settings;

import android.os.Bundle;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class ReturnHomeSettingsActivity extends GroundSdkActivityBase {

    private ToggleSettingView mAutoTrigger;

    private MultiChoiceSettingView<ReturnHomePilotingItf.Target> mPreferredTargetView;

    private MultiChoiceSettingView<ReturnHomePilotingItf.EndingBehavior> mEndingBehavior;

    private RangedSettingView mAutostartOnDisconnectDelayView;

    private RangedSettingView mMinAltitudeView;

    private RangedSettingView mHoveringAltitudeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());

        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_return_home_settings);

        mAutoTrigger = findViewById(R.id.autoTriggerSwitch);
        mPreferredTargetView = findViewById(R.id.preferredTarget);
        mEndingBehavior = findViewById(R.id.endingBehavior);
        mAutostartOnDisconnectDelayView = findViewById(R.id.delay);
        mMinAltitudeView = findViewById(R.id.min_altitude);
        mHoveringAltitudeView = findViewById(R.id.hovering_altitude);

        drone.getPilotingItf(ReturnHomePilotingItf.class, pilotingItf -> {
            assert pilotingItf != null;
            updateSetting(mAutoTrigger, pilotingItf.autoTrigger());
            updateSetting(mPreferredTargetView, pilotingItf.getPreferredTarget());
            updateSetting(mEndingBehavior, pilotingItf.getEndingBehavior());
            updateSetting(mAutostartOnDisconnectDelayView, pilotingItf.getAutoStartOnDisconnectDelay());
            updateSetting(mMinAltitudeView, pilotingItf.getMinAltitude());
            updateSetting(mHoveringAltitudeView, pilotingItf.getEndingHoveringAltitude());
        });
    }
}
