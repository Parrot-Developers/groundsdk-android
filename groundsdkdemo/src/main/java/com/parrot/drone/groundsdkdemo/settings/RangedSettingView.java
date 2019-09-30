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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdkdemo.R;

public class RangedSettingView extends RelativeLayout {

    public interface Listener {

        void onValueChanged(double newValue);
    }

    private final TextView mTitleText;

    private final TextView mValueText;

    private final TextView mMinText;

    private final TextView mMaxText;

    private final SeekBar mValueSeek;

    private final Format mFormat;

    private Listener mListener;

    private double mMin;

    private double mMax;

    private double mValue;

    private boolean mAvailable;

    private boolean mUpdating;

    /** keep in sync with attrs.xml/RangedSettingView/format enum */
    private enum Format {
        INTEGER {
            @Override
            String format(@NonNull Context context, double value) {
                return context.getString(R.string.int_value_format, (int) value);
            }
        },
        DOUBLE {
            @Override
            String format(@NonNull Context context, double value) {
                return context.getString(R.string.double_value_format, value);
            }
        };

        abstract String format(@NonNull Context context, double value);
    }

    public RangedSettingView(Context context) {
        this(context, null);
    }

    public RangedSettingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangedSettingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(true);
        setSaveFromParentEnabled(false);

        LayoutInflater.from(context).inflate(R.layout.view_ranged_setting, this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangedSettingView, defStyleAttr, 0);
        try {
            mTitleText = findViewById(android.R.id.title);
            mTitleText.setText(a.getString(R.styleable.RangedSettingView_title));
            mFormat = Format.values()[a.getInt(R.styleable.RangedSettingView_format, 0)];
        } finally {
            a.recycle();
        }

        mValueText = findViewById(android.R.id.summary);
        mMinText = findViewById(android.R.id.text1);
        mMaxText = findViewById(android.R.id.text2);
        mValueSeek = findViewById(android.R.id.edit);
        mValueSeek.setOnSeekBarChangeListener(mSeekListener);
    }

    public RangedSettingView setTitle(@Nullable String title) {
        mTitleText.setText(title);
        return this;
    }

    public RangedSettingView setUpdating(boolean updating) {
        mUpdating = updating;
        updateView();
        return this;
    }

    public RangedSettingView setAvailable(boolean available) {
        mAvailable = available;
        updateView();
        return this;
    }

    public RangedSettingView setValue(double min, double value, double max) {
        mValue = value;
        mMin = Math.min(min, value);
        mMax = Math.max(max, value);
        updateView();
        return this;
    }

    public double getValue() {
        return mValue;
    }

    public RangedSettingView setListener(@Nullable Listener listener) {
        mListener = listener;
        return this;
    }

    private void updateView() {
        Context context = getContext();

        mValueText.setText(mAvailable ? mFormat.format(context, mValue) : context.getString(R.string.unsupported));
        mMinText.setText(mAvailable ? mFormat.format(context, mMin) : context.getString(R.string.no_value));
        mMaxText.setText(mAvailable ? mFormat.format(context, mMax) : context.getString(R.string.no_value));
        mValueSeek.setIndeterminate(mUpdating);
        mValueSeek.setEnabled(mAvailable && !mUpdating);
        // setProgress should always be applied after setIndeterminate since it is ignored if indeterminate == false
        mValueSeek.setProgress(mAvailable ? (int) Math.round((mValue - mMin) * 100 / (mMax - mMin)) : 0);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mValueText.setText(mFormat.format(getContext(), getValue(progress)));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mValue = getValue(seekBar.getProgress());
            if (mListener != null) {
                mListener.onValueChanged(mValue);
            }
        }

        private double getValue(int progress) {
            return progress * (mMax - mMin) / 100 + mMin;
        }
    };
}
