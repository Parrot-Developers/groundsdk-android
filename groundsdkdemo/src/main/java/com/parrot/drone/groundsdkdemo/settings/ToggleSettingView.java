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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parrot.drone.groundsdkdemo.R;

public class ToggleSettingView extends LinearLayout {

    public interface Listener {

        void onToggled();
    }

    private final TextView mTitleText;

    private final SwitchMaterial mEnableSwitch;

    private final ProgressBar mUpdatingView;

    private Listener mListener;

    private boolean mAvailable;

    private boolean mToggled;

    private boolean mUpdating;

    public ToggleSettingView(Context context) {
        this(context, null);
    }

    public ToggleSettingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSettingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(true);
        setSaveFromParentEnabled(false);

        LayoutInflater.from(context).inflate(R.layout.view_toggle_setting, this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToggleSettingView, defStyleAttr, 0);
        try {
            mTitleText = findViewById(android.R.id.title);
            mTitleText.setText(a.getString(R.styleable.ToggleSettingView_title));
        } finally {
            a.recycle();
        }

        mEnableSwitch = findViewById(android.R.id.edit);
        mUpdatingView = findViewById(android.R.id.progress);
    }

    public ToggleSettingView setTitle(@Nullable String title) {
        mTitleText.setText(title);
        return this;
    }

    public ToggleSettingView setAvailable(boolean available) {
        mAvailable = available;
        updateView();
        return this;
    }

    public ToggleSettingView setToggled(boolean toggled) {
        mToggled = toggled;
        updateView();
        return this;
    }

    public ToggleSettingView setUpdating(boolean updating) {
        mUpdating = updating;
        updateView();
        return this;
    }

    public ToggleSettingView setListener(@Nullable Listener listener) {
        mListener = listener;
        return this;
    }

    private void updateView() {
        mEnableSwitch.setOnCheckedChangeListener(null);
        mEnableSwitch.setEnabled(mAvailable);
        mEnableSwitch.setChecked(mToggled);
        mEnableSwitch.setVisibility(mUpdating ? GONE : VISIBLE);
        mUpdatingView.setVisibility(mUpdating ? VISIBLE : GONE);
        mEnableSwitch.setOnCheckedChangeListener(mToggleListener);
    }

    private final CompoundButton.OnCheckedChangeListener mToggleListener =
            new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mListener != null) {
                        mListener.onToggled();
                    }
                }
            };
}
