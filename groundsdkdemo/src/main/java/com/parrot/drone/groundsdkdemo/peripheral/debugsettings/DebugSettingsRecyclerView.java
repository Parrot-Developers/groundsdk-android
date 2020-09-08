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

package com.parrot.drone.groundsdkdemo.peripheral.debugsettings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdkdemo.R;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("javadoc")
public class DebugSettingsRecyclerView extends RecyclerView {

    @NonNull
    private final ViewAdapter mDebugSettingsAdapter;

    public DebugSettingsRecyclerView(Context context) {
        super(context);
        mDebugSettingsAdapter = new ViewAdapter();
        setAdapter(mDebugSettingsAdapter);
    }

    public DebugSettingsRecyclerView(Context context,
                                     @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDebugSettingsAdapter = new ViewAdapter();
        setAdapter(mDebugSettingsAdapter);
    }

    public DebugSettingsRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDebugSettingsAdapter = new ViewAdapter();
        setAdapter(mDebugSettingsAdapter);
    }

    public void updateDebugSettings(@NonNull List<DevToolbox.DebugSetting> debugSettings) {
        mDebugSettingsAdapter.setDebugSettings(debugSettings);
    }

    private abstract static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }

        abstract void setSetting(@NonNull DevToolbox.DebugSetting debugSetting);
    }

    private static final class BooleanDebugSetting extends ViewHolder {

        @Nullable
        private DevToolbox.BooleanDebugSetting mSetting;

        @NonNull
        private final TextView mTitle;

        @NonNull
        private final TextView mValueText;

        @NonNull
        private final SwitchMaterial mEnableSwitch;

        @NonNull
        private final ProgressBar mUpdatingView;

        private BooleanDebugSetting(@NonNull View rootView) {
            super(rootView);
            mTitle = itemView.findViewById(android.R.id.title);
            mValueText = itemView.findViewById(R.id.value);
            mEnableSwitch = itemView.findViewById(android.R.id.edit);
            mUpdatingView = itemView.findViewById(android.R.id.progress);
        }

        @Override
        void setSetting(@NonNull DevToolbox.DebugSetting debugSetting) {
            mSetting = debugSetting.as(DevToolbox.BooleanDebugSetting.class);
            assert mSetting != null;

            mTitle.setText(debugSetting.getName());

            if (mSetting.isUpdating()) {
                mValueText.setVisibility(GONE);
                mEnableSwitch.setVisibility(GONE);
                mUpdatingView.setVisibility(VISIBLE);
            } else if (mSetting.isReadOnly()) {

                mValueText.setText(Boolean.toString(mSetting.getValue()));

                mValueText.setVisibility(VISIBLE);
                mEnableSwitch.setVisibility(GONE);
                mUpdatingView.setVisibility(GONE);
            } else {
                mEnableSwitch.setOnCheckedChangeListener(null);
                mEnableSwitch.setChecked(mSetting.getValue());
                mEnableSwitch.setOnCheckedChangeListener(mToggleListener);

                mValueText.setVisibility(GONE);
                mEnableSwitch.setVisibility(VISIBLE);
                mUpdatingView.setVisibility(GONE);
            }
        }

        private final CompoundButton.OnCheckedChangeListener mToggleListener =
                new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        assert mSetting != null;
                        mSetting.setValue(isChecked);
                    }
                };
    }

    private static final class NumericDebugSetting extends ViewHolder {

        @Nullable
        private DevToolbox.NumericDebugSetting mSetting;

        @NonNull
        private final TextView mTitle;

        @NonNull
        private final TextView mValueText;

        @NonNull
        private final EditText mEditText;

        @NonNull
        private final TextView mMinText;

        @NonNull
        private final TextView mMaxText;

        @NonNull
        private final SeekBar mValueSeek;

        @NonNull
        private static final NumberFormat DEFAULT_NUMBER_FORMAT = NumberFormat.getInstance(Locale.ROOT);

        double mMin;

        double mMax;

        private NumericDebugSetting(@NonNull View rootView) {
            super(rootView);
            mTitle = itemView.findViewById(android.R.id.title);
            mValueText = itemView.findViewById(android.R.id.summary);
            mMinText = itemView.findViewById(android.R.id.text1);
            mMaxText = itemView.findViewById(android.R.id.text2);
            mEditText = itemView.findViewById(R.id.edit_no_bounds);
            mEditText.setOnEditorActionListener(mEditorActionListener);
            mValueSeek = itemView.findViewById(android.R.id.edit);
            mValueSeek.setOnSeekBarChangeListener(mSeekListener);
        }

        @Override
        void setSetting(@NonNull DevToolbox.DebugSetting debugSetting) {
            mSetting = debugSetting.as(DevToolbox.NumericDebugSetting.class);
            assert mSetting != null;

            String title = mSetting.getName();
            if (mSetting.hasStep()) {
                title += itemView.getContext().getString(R.string.double_step_format, mSetting.getStep());
            }
            mTitle.setText(title);

            if (mSetting.isUpdating()) {
                mValueSeek.setEnabled(false);
                mValueSeek.setIndeterminate(true);

                mValueSeek.setVisibility(VISIBLE);
                mMinText.setVisibility(GONE);
                mMaxText.setVisibility(GONE);
                mEditText.setVisibility(GONE);
                mValueText.setVisibility(GONE);
            } else if (!mSetting.isReadOnly()) {
                double value = mSetting.getValue();
                setValueText(mValueText, value);

                if (mSetting.hasRange() && !mSetting.hasStep()) {
                    mMin = mSetting.getRangeMin();
                    mMax = mSetting.getRangeMax();
                    setValueText(mMinText, mMin);
                    setValueText(mMaxText, mMax);

                    mValueSeek.setEnabled(true);
                    mValueSeek.setIndeterminate(false);
                    mValueSeek.setProgress((int) Math.round((value - mMin) * 100 / (mMax - mMin)));

                    mMinText.setVisibility(VISIBLE);
                    mMaxText.setVisibility(VISIBLE);
                    mValueText.setVisibility(VISIBLE);
                    mValueSeek.setVisibility(VISIBLE);
                    mEditText.setVisibility(GONE);
                } else {
                    mEditText.setText(itemView.getContext().getString(R.string.double_value_format, value));

                    mEditText.setVisibility(VISIBLE);
                    mMinText.setVisibility(GONE);
                    mMaxText.setVisibility(GONE);
                    mValueText.setVisibility(GONE);
                    mValueSeek.setVisibility(GONE);
                }
            } else {
                setValueText(mValueText, mSetting.getValue());
                mValueText.setVisibility(VISIBLE);
                mValueSeek.setVisibility(GONE);
                mMinText.setVisibility(GONE);
                mMaxText.setVisibility(GONE);
                mEditText.setVisibility(GONE);

            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setValueText(mValueText, getValue(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                assert mSetting != null;
                mSetting.setValue(getValue(seekBar.getProgress()));
            }

            private double getValue(int progress) {
                return progress * (mMax - mMin) / 100 + mMin;
            }
        };

        private void setValueText(@NonNull TextView view, double value) {
            view.setText(itemView.getContext().getString(R.string.double_value_format, value));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final TextView.OnEditorActionListener mEditorActionListener = new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    assert mSetting != null;
                    try {
                        mSetting.setValue(DEFAULT_NUMBER_FORMAT.parse(mEditText.getText().toString()).doubleValue());
                    } catch (ParseException e) {
                        mEditText.setText(itemView.getContext().getString(R.string.double_value_format,
                                mSetting.getValue()));
                    }
                }
                return false;
            }
        };
    }

    private static final class TextDebugSetting extends ViewHolder {

        @Nullable
        private DevToolbox.TextDebugSetting mSetting;

        @NonNull
        private final TextView mTitle;

        @NonNull
        private final TextView mValueText;

        @NonNull
        private final EditText mEditText;

        @NonNull
        private final ProgressBar mUpdatingView;

        private TextDebugSetting(@NonNull View rootView) {
            super(rootView);
            mTitle = itemView.findViewById(android.R.id.title);
            mValueText = itemView.findViewById(R.id.value);
            mEditText = itemView.findViewById(android.R.id.edit);
            mEditText.setOnEditorActionListener(mEditorActionListener);
            mUpdatingView = itemView.findViewById(android.R.id.progress);
        }

        @Override
        void setSetting(@NonNull DevToolbox.DebugSetting debugSetting) {
            mSetting = debugSetting.as(DevToolbox.TextDebugSetting.class);
            assert mSetting != null;

            mTitle.setText(debugSetting.getName());

            mUpdatingView.setVisibility(mSetting.isUpdating() ? VISIBLE : GONE);
            if (mSetting.isUpdating()) {
                mValueText.setVisibility(GONE);
                mEditText.setVisibility(GONE);
            } else if (!mSetting.isReadOnly()) {
                mEditText.setText(mSetting.getValue());

                mValueText.setVisibility(GONE);
                mEditText.setVisibility(VISIBLE);
            } else {
                mValueText.setText(mSetting.getValue());

                mEditText.setVisibility(GONE);
                mValueText.setVisibility(VISIBLE);
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final TextView.OnEditorActionListener mEditorActionListener = new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    assert mSetting != null;
                    mSetting.setValue(mEditText.getText().toString());
                }
                return false;
            }
        };
    }

    private static final class ViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        private List<DevToolbox.DebugSetting> mDebugSettings;

        ViewAdapter() {
            mDebugSettings = Collections.emptyList();
        }

        void setDebugSettings(@NonNull List<DevToolbox.DebugSetting> debugSettings) {
            mDebugSettings = debugSettings;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return mDebugSettings.get(position).getType().ordinal();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DevToolbox.DebugSetting.Type type = DevToolbox.DebugSetting.Type.values()[viewType];
            switch (type) {
                case BOOLEAN: {
                    View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.boolean_debug_setting,
                            parent, false);
                    return new BooleanDebugSetting(rootView);
                }
                case NUMERIC: {
                    View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.numeric_debug_setting,
                            parent, false);
                    return new NumericDebugSetting(rootView);
                }
                case TEXT: {
                    View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_debug_setting,
                            parent, false);
                    return new TextDebugSetting(rootView);
                }
            }
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setSetting(mDebugSettings.get(position));
        }

        @Override
        public int getItemCount() {
            return mDebugSettings.size();
        }
    }
}
