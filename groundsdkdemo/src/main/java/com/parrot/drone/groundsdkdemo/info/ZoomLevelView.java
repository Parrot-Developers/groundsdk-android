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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.parrot.drone.groundsdkdemo.R;

public class ZoomLevelView extends RelativeLayout {

    public interface Listener {

        void onValueChanged(double newValue);
    }

    private final TextView mValueText;

    private final SeekBar mValueSeek;

    @ColorInt
    private final int mLossLessColor;

    @ColorInt
    private final int mLossyColor;

    private Listener mListener;

    private double mMaxLossyZoomLevel;

    private boolean mInitialized;

    public ZoomLevelView(Context context) {
        this(context, null);
    }

    public ZoomLevelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomLevelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.view_zoom_level, this);

        mValueText = findViewById(android.R.id.summary);
        mValueSeek = findViewById(android.R.id.progress);
        mValueSeek.setOnSeekBarChangeListener(mLevelSeekListener);

        mLossLessColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
        mLossyColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
    }

    public ZoomLevelView setZoomLevel(double current, double maxLossLess, double maxLossy) {
        mMaxLossyZoomLevel = maxLossy;
        mValueText.setText(getContext().getString(R.string.zoom_level_format, current));
        mValueText.setTextColor(current <= maxLossLess ? mLossLessColor : mLossyColor);
        if (!mInitialized) {
            mValueSeek.setProgress((int) Math.round((current - 1) * 100 / (mMaxLossyZoomLevel - 1)));
            mInitialized = true;
        }
        return this;
    }

    public ZoomLevelView setListener(Listener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mValueSeek.setEnabled(enabled);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final SeekBar.OnSeekBarChangeListener mLevelSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mListener != null) {
                double value = seekBar.getProgress() * (mMaxLossyZoomLevel - 1) / 100 + 1;
                mListener.onValueChanged(value);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
}
