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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class GimbalSettingsActivity extends GroundSdkActivityBase {

    private RecyclerView mPositionTargets;

    private RecyclerView mVelocityTargets;

    private StabilizationAdapter mStabilizationAdapter;

    private MaxSpeedsAdapter mMaxSpeedsAdapter;

    private PositionTargetsAdapter mPositionTargetsAdapter;

    private VelocityTargetsAdapter mVelocityTargetsAdapter;

    private Gimbal mGimbal;

    private List<Gimbal.Axis> mSupportedAxes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Drone drone = groundSdk().getDrone(deviceUid);
        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gimbal_settings);

        mSupportedAxes = Collections.emptyList();

        mStabilizationAdapter = new StabilizationAdapter();
        RecyclerView recyclerView = findViewById(R.id.stabilized_axes);
        recyclerView.setAdapter(mStabilizationAdapter);

        mMaxSpeedsAdapter = new MaxSpeedsAdapter();
        recyclerView = findViewById(R.id.max_speeds);
        recyclerView.setAdapter(mMaxSpeedsAdapter);

        mPositionTargetsAdapter = new PositionTargetsAdapter();
        mPositionTargets = findViewById(R.id.position_targets);
        mPositionTargets.setAdapter(mPositionTargetsAdapter);

        mVelocityTargetsAdapter = new VelocityTargetsAdapter();
        mVelocityTargets = findViewById(R.id.velocity_targets);
        mVelocityTargets.setAdapter(mVelocityTargetsAdapter);

        RadioGroup controlModeRadio = findViewById(R.id.control_mode);
        controlModeRadio.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.btn_position:
                    mPositionTargets.setVisibility(View.VISIBLE);
                    mVelocityTargets.setVisibility(View.GONE);
                    break;
                case R.id.btn_velocity:
                    mPositionTargets.setVisibility(View.GONE);
                    mVelocityTargets.setVisibility(View.VISIBLE);
                    break;
            }
        });
        RadioButton positionButton = findViewById(R.id.btn_position);
        positionButton.setChecked(true);

        drone.getPeripheral(Gimbal.class, this::updateGimbal);
    }

    private void updateGimbal(@Nullable Gimbal gimbal) {
        if (gimbal == null) {
            finish();
            return;
        }

        mGimbal = gimbal;

        if (mSupportedAxes.isEmpty()) {
            mSupportedAxes = new ArrayList<>(gimbal.getSupportedAxes());
        }

        mStabilizationAdapter.notifyDataSetChanged();
        mMaxSpeedsAdapter.notifyDataSetChanged();
        mPositionTargetsAdapter.notifyDataSetChanged();
        mVelocityTargetsAdapter.notifyDataSetChanged();
    }

    private final class StabilizationItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final ToggleSettingView mToggleSettingView;

        StabilizationItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.toggle_setting_item, parent, false));
            mToggleSettingView = (ToggleSettingView) itemView;
        }

        void bind(@NonNull Gimbal.Axis axis) {
            mToggleSettingView.setTitle(axis.toString());
            updateSetting(mToggleSettingView, mGimbal.getStabilization(axis));
        }
    }

    private class StabilizationAdapter extends RecyclerView.Adapter<StabilizationItemHolder> {

        @NonNull
        @Override
        public StabilizationItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new StabilizationItemHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull StabilizationItemHolder holder, int position) {
            holder.bind(mSupportedAxes.get(position));
        }

        @Override
        public int getItemCount() {
            return mSupportedAxes.size();
        }
    }

    private final class MaxSpeedsItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final RangedSettingView mRangedSettingView;

        MaxSpeedsItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.ranged_setting_item, parent, false));
            mRangedSettingView = (RangedSettingView) itemView;
        }

        void bind(@NonNull Gimbal.Axis axis) {
            mRangedSettingView.setTitle(axis.toString());
            updateSetting(mRangedSettingView, mGimbal.getMaxSpeed(axis));
        }
    }

    private class MaxSpeedsAdapter extends RecyclerView.Adapter<MaxSpeedsItemHolder> {

        @NonNull
        @Override
        public MaxSpeedsItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MaxSpeedsItemHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull MaxSpeedsItemHolder holder, int position) {
            holder.bind(mSupportedAxes.get(position));
        }

        @Override
        public int getItemCount() {
            return mSupportedAxes.size();
        }
    }

    private final class PositionTargetsItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final RangedSettingView mRangedSettingView;

        private Gimbal.Axis mAxis;

        PositionTargetsItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.ranged_setting_item, parent, false));
            mRangedSettingView = (RangedSettingView) itemView;
            mRangedSettingView.setAvailable(true)
                              .setListener(newValue -> {
                                  Double yaw = mAxis == Gimbal.Axis.YAW ? newValue : null;
                                  Double pitch = mAxis == Gimbal.Axis.PITCH ? newValue : null;
                                  Double roll = mAxis == Gimbal.Axis.ROLL ? newValue : null;
                                  mGimbal.control(Gimbal.ControlMode.POSITION, yaw, pitch, roll);
                              });
        }

        void bind(@NonNull Gimbal.Axis axis) {
            mAxis = axis;
            mRangedSettingView.setTitle(axis.toString());
            DoubleRange bounds = mGimbal.getAttitudeBounds(axis);
            mRangedSettingView.setValue(bounds.getLower(), mGimbal.getAttitude(axis), bounds.getUpper());
        }
    }

    private class PositionTargetsAdapter extends RecyclerView.Adapter<PositionTargetsItemHolder> {

        @NonNull
        @Override
        public PositionTargetsItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PositionTargetsItemHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PositionTargetsItemHolder holder, int position) {
            holder.bind(mSupportedAxes.get(position));
        }

        @Override
        public int getItemCount() {
            return mSupportedAxes.size();
        }
    }

    private final class VelocityTargetsItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final VelocityControlView mVelocityControlView;

        private Gimbal.Axis mAxis;

        VelocityTargetsItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.velocity_control_item, parent, false));
            mVelocityControlView = (VelocityControlView) itemView;
            mVelocityControlView.setListener(newValue -> {
                Double yaw = mAxis == Gimbal.Axis.YAW ? newValue : null;
                Double pitch = mAxis == Gimbal.Axis.PITCH ? newValue : null;
                Double roll = mAxis == Gimbal.Axis.ROLL ? newValue : null;
                mGimbal.control(Gimbal.ControlMode.VELOCITY, yaw, pitch, roll);
            });
        }

        void bind(@NonNull Gimbal.Axis axis) {
            mAxis = axis;
            mVelocityControlView.setTitle(axis.toString()).setMaxSpeed(mGimbal.getMaxSpeed(axis).getValue());
        }
    }

    private class VelocityTargetsAdapter extends RecyclerView.Adapter<VelocityTargetsItemHolder> {

        @NonNull
        @Override
        public VelocityTargetsItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VelocityTargetsItemHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull VelocityTargetsItemHolder holder, int position) {
            holder.bind(mSupportedAxes.get(position));
        }

        @Override
        public int getItemCount() {
            return mSupportedAxes.size();
        }
    }
}
