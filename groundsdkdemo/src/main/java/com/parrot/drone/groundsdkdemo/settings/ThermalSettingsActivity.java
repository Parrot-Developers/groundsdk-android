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

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdkdemo.EditColorDialogFragment;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class ThermalSettingsActivity extends GroundSdkActivityBase {

    private MultiChoiceSettingView<ThermalControl.Mode> mModeView;

    private MultiChoiceSettingView<ThermalControl.Sensitivity> mSensitivityView;

    private View mCalibrationGroup;

    private MultiChoiceSettingView<ThermalControl.Calibration.Mode> mCalibrationModeView;

    private Button mCalibrationButton;

    private CardView mEmissivityCardView;

    private RangedSettingView mEmissivityView;

    private CardView mBackgroundTemperatureCardView;

    private RangedSettingView mBackgroundTemperatureView;

    // Palette
    private CardView mPaletteTitleView;

    private CardView mPaletteCardView;

    private RangedSettingView mLowestTemperatureView;

    private RangedSettingView mHighestTemperatureView;

    private MultiChoiceSettingView<ThermalControl.AbsolutePalette.ColorizationMode> mColorizationModeView;

    private ToggleSettingView mLockedView;

    private MultiChoiceSettingView<ThermalControl.SpotPalette.SpotType> mSpotTypeView;

    private RangedSettingView mThresholdView;

    private Button mSendPaletteButton;

    private ColorPaletteAdapter mColorPaletteAdapter;

    private ThermalControl.AbsolutePalette mAbsolutePalette;

    private ThermalControl.RelativePalette mRelativePalette;

    private ThermalControl.SpotPalette mSpotPalette;

    private ThermalControl.Palette mCurrentPalette;

    private List<ThermalControl.Palette.Color> mColors;

    private ThermalControl.AbsolutePalette.ColorizationMode mColorizationMode;

    private boolean mLocked;

    private ThermalControl.SpotPalette.SpotType mSpotType;

    // Rendering
    private CardView mRenderingTitleView;

    private CardView mRenderingCardView;

    @SuppressWarnings("FieldCanBeLocal")
    private MultiChoiceSettingView<ThermalControl.Rendering.Mode> mRenderingModeView;

    private RangedSettingView mBlendingRateView;

    private Button mSendRenderingButton;

    private ThermalControl.Rendering mRendering;

    private ThermalControl.Rendering.Mode mRenderingMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Drone drone = groundSdk().getDrone(deviceUid);
        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_thermal_settings);

        mModeView = findViewById(R.id.mode);
        mSensitivityView = findViewById(R.id.sensitivity);
        mCalibrationGroup = findViewById(R.id.group_calibration);
        mCalibrationModeView = findViewById(R.id.calibration_mode);
        mCalibrationButton = findViewById(R.id.btn_calibrate);

        mEmissivityCardView = findViewById(R.id.card_emissivity);
        mEmissivityView = findViewById(R.id.emissivity);
        mBackgroundTemperatureCardView = findViewById(R.id.card_background_temperature);
        mBackgroundTemperatureView = findViewById(R.id.background_temperature);

        mPaletteTitleView = findViewById(R.id.title_palette);
        mPaletteCardView = findViewById(R.id.card_palette);
        mLowestTemperatureView = findViewById(R.id.lowest_temperature);
        mHighestTemperatureView = findViewById(R.id.highest_temperature);
        mColorizationModeView = findViewById(R.id.colorization_mode);
        mLockedView = findViewById(R.id.locked);
        mSpotTypeView = findViewById(R.id.spot_type);
        mThresholdView = findViewById(R.id.threshold);
        mSendPaletteButton = findViewById(R.id.btn_send_palette);

        mRenderingTitleView = findViewById(R.id.title_rendering);
        mRenderingCardView = findViewById(R.id.card_rendering);
        mRenderingModeView = findViewById(R.id.rendering_mode);
        mBlendingRateView = findViewById(R.id.blending);
        mSendRenderingButton = findViewById(R.id.btn_send_rendering);
        mEmissivityView.setAvailable(true).setValue(0, 0, 1);
        mBackgroundTemperatureView.setAvailable(true).setValue(200, 255, 674);

        Button addColorButton = findViewById(R.id.btn_add_color);
        addColorButton.setOnClickListener(v -> {
            EditColorDialogFragment fragment = new EditColorDialogFragment();
            fragment.setListener(color -> {
                mColors.add(color);
                mColorPaletteAdapter.notifyDataSetChanged();
            });
            fragment.show(getSupportFragmentManager(), null);
        });

        RecyclerView colorsView = findViewById(R.id.colors);
        mColorPaletteAdapter = new ColorPaletteAdapter();
        colorsView.setAdapter(mColorPaletteAdapter);
        colorsView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        RadioGroup paletteTypeRadio = findViewById(R.id.palette_type);
        paletteTypeRadio.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.btn_absolute:
                    mCurrentPalette = mAbsolutePalette;
                    mLowestTemperatureView.setVisibility(View.VISIBLE);
                    mHighestTemperatureView.setVisibility(View.VISIBLE);
                    mColorizationModeView.setVisibility(View.VISIBLE);
                    mLockedView.setVisibility(View.GONE);
                    mSpotTypeView.setVisibility(View.GONE);
                    mThresholdView.setVisibility(View.GONE);
                    break;
                case R.id.btn_relative:
                    mCurrentPalette = mRelativePalette;
                    mLowestTemperatureView.setVisibility(View.VISIBLE);
                    mHighestTemperatureView.setVisibility(View.VISIBLE);
                    mColorizationModeView.setVisibility(View.GONE);
                    mLockedView.setVisibility(View.VISIBLE);
                    mSpotTypeView.setVisibility(View.GONE);
                    mThresholdView.setVisibility(View.GONE);
                    break;
                case R.id.btn_spot:
                    mCurrentPalette = mSpotPalette;
                    mLowestTemperatureView.setVisibility(View.GONE);
                    mHighestTemperatureView.setVisibility(View.GONE);
                    mColorizationModeView.setVisibility(View.GONE);
                    mLockedView.setVisibility(View.GONE);
                    mSpotTypeView.setVisibility(View.VISIBLE);
                    mThresholdView.setVisibility(View.VISIBLE);
                    break;
            }
        });
        RadioButton absoluteButton = findViewById(R.id.btn_absolute);
        absoluteButton.setChecked(true);

        mLowestTemperatureView.setAvailable(true).setValue(0, 0, 1);
        mLowestTemperatureView.setListener(
                newValue -> mHighestTemperatureView.setValue(newValue, mHighestTemperatureView.getValue(), 1));

        mHighestTemperatureView.setAvailable(true).setValue(0, 1, 1);
        mHighestTemperatureView.setListener(
                newValue -> mLowestTemperatureView.setValue(0, mLowestTemperatureView.getValue(), newValue));

        mColorizationMode = ThermalControl.AbsolutePalette.ColorizationMode.EXTENDED;
        mColorizationModeView
                .setChoices(EnumSet.allOf(ThermalControl.AbsolutePalette.ColorizationMode.class))
                .setSelection(mColorizationMode)
                .setListener(chosenItem -> mColorizationMode = chosenItem);

        mLockedView.setAvailable(true).setToggled(mLocked).setListener(() -> mLocked = !mLocked);

        mSpotType = ThermalControl.SpotPalette.SpotType.HOT;
        mSpotTypeView
                .setChoices(EnumSet.allOf(ThermalControl.SpotPalette.SpotType.class))
                .setSelection(mSpotType)
                .setListener(chosenItem -> mSpotType = chosenItem);

        mThresholdView.setAvailable(true).setValue(0, 0.5, 1);

        mColors = new ArrayList<>();
        mAbsolutePalette = new ThermalControl.AbsolutePalette() {

            @NonNull
            @Override
            public Collection<Color> getColors() {
                return mColors;
            }

            @Override
            public double getLowestTemperature() {
                return mLowestTemperatureView.getValue();
            }

            @Override
            public double getHighestTemperature() {
                return mHighestTemperatureView.getValue();
            }

            @NonNull
            @Override
            public ColorizationMode getColorizationMode() {
                return mColorizationMode;
            }
        };

        mRelativePalette = new ThermalControl.RelativePalette() {

            @NonNull
            @Override
            public Collection<Color> getColors() {
                return mColors;
            }

            @Override
            public double getLowestTemperature() {
                return mLowestTemperatureView.getValue();
            }

            @Override
            public double getHighestTemperature() {
                return mHighestTemperatureView.getValue();
            }

            @Override
            public boolean isLocked() {
                return mLocked;
            }
        };

        mSpotPalette = new ThermalControl.SpotPalette() {

            @NonNull
            @Override
            public Collection<Color> getColors() {
                return mColors;
            }

            @NonNull
            @Override
            public SpotType getType() {
                return mSpotType;
            }

            @Override
            public double getThreshold() {
                return mThresholdView.getValue();
            }
        };

        mCurrentPalette = mAbsolutePalette;

        mRenderingMode = ThermalControl.Rendering.Mode.BLENDED;
        mRenderingModeView
                .setChoices(EnumSet.allOf(ThermalControl.Rendering.Mode.class))
                .setSelection(mRenderingMode)
                .setListener(chosenItem -> {
                    mRenderingMode = chosenItem;
                    mBlendingRateView.setVisibility(
                            chosenItem == ThermalControl.Rendering.Mode.BLENDED ? View.VISIBLE : View.GONE);
                });
        mBlendingRateView.setAvailable(true).setValue(0, 0.5, 1);

        mRendering = new ThermalControl.Rendering() {

            @NonNull
            @Override
            public Mode getMode() {
                return mRenderingMode;
            }

            @Override
            public double getBlendingRate() {
                return mBlendingRateView.getValue();
            }
        };

        drone.getState(state -> {
            assert state != null;
            int visibility;
            if (state.getConnectionState() == DeviceState.ConnectionState.CONNECTED) {
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }
            mEmissivityCardView.setVisibility(visibility);
            mBackgroundTemperatureCardView.setVisibility(visibility);
            mPaletteTitleView.setVisibility(visibility);
            mPaletteCardView.setVisibility(visibility);
            mRenderingTitleView.setVisibility(visibility);
            mRenderingCardView.setVisibility(visibility);
        });

        drone.getPeripheral(ThermalControl.class, thermal -> {
            if (thermal != null) {
                updateSetting(mModeView, thermal.mode());
                updateSetting(mSensitivityView, thermal.sensitivity());

                ThermalControl.Calibration calibration = thermal.calibration();
                if (calibration == null) {
                    mCalibrationGroup.setVisibility(View.GONE);
                } else {
                    updateSetting(mCalibrationModeView, calibration.mode());
                    mCalibrationGroup.setVisibility(View.VISIBLE);
                    mCalibrationButton.setOnClickListener(btn -> calibration.calibrate());
                }
                mEmissivityView.setListener(thermal::sendEmissivity);
                mBackgroundTemperatureView.setListener(thermal::sendBackgroundTemperature);
                mSendPaletteButton.setOnClickListener(v -> thermal.sendPalette(mCurrentPalette));
                mSendRenderingButton.setOnClickListener(v -> thermal.sendRendering(mRendering));
            } else {
                finish();
            }
        });
    }

    private final class ColorViewHolder extends RecyclerView.ViewHolder {

        private ThermalControl.Palette.Color mColor;

        private ColorViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.color_preview_item, parent, false));
            itemView.setOnClickListener(v -> {
                EditColorDialogFragment fragment = new EditColorDialogFragment();
                fragment.setColor(mColor);
                fragment.setListener(color -> {
                    if (color == null) {
                        mColors.remove(mColor);
                    }
                    mColorPaletteAdapter.notifyDataSetChanged();
                });
                fragment.show(getSupportFragmentManager(), null);
            });
        }

        void bind(ThermalControl.Palette.Color color) {
            mColor = color;
            itemView.setBackgroundColor(Color.rgb((int) (color.getRed() * 255.0f + 0.5f),
                    (int) (color.getGreen() * 255.0f + 0.5f), (int) (color.getBlue() * 255.0f + 0.5f)));
        }
    }

    private class ColorPaletteAdapter extends RecyclerView.Adapter<ColorViewHolder> {

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ColorViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            holder.bind(mColors.get(position));
        }

        @Override
        public int getItemCount() {
            return mColors.size();
        }
    }
}
