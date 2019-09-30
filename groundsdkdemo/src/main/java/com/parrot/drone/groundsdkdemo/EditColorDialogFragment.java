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

package com.parrot.drone.groundsdkdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdkdemo.settings.RangedSettingView;

public class EditColorDialogFragment extends DialogFragment {

    public interface Listener {

        void onColorChanged(@Nullable ThermalControl.Palette.Color color);
    }

    @Nullable
    private Listener mListener;

    private RangedSettingView mRedView;

    private RangedSettingView mGreenView;

    private RangedSettingView mBlueView;

    private RangedSettingView mPositionView;

    private View mPreview;

    private EditColor mColor;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        @SuppressLint("InflateParams")
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_edit_color, null);

        mRedView = content.findViewById(R.id.red);
        mGreenView = content.findViewById(R.id.green);
        mBlueView = content.findViewById(R.id.blue);
        mPositionView = content.findViewById(R.id.position);
        mPreview = content.findViewById(R.id.preview);

        boolean addDeleteButton = mColor != null;
        if (mColor == null) {
            mColor = new EditColor();
        }

        mRedView.setAvailable(true).setValue(0, mColor.getRed(), 1);
        mGreenView.setAvailable(true).setValue(0, mColor.getGreen(), 1);
        mBlueView.setAvailable(true).setValue(0, mColor.getBlue(), 1);
        mPositionView.setAvailable(true).setValue(0, mColor.getPosition(), 1);

        mRedView.setListener(newValue -> updatePreview());
        mGreenView.setListener(newValue -> updatePreview());
        mBlueView.setListener(newValue -> updatePreview());

        updatePreview();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.title_dialog_edit_color)
               .setView(content)
               .setPositiveButton(R.string.action_ok, mClickListener)
               .setNegativeButton(R.string.action_cancel, mClickListener);
        if (addDeleteButton) {
            builder.setNeutralButton(R.string.action_delete, mClickListener);
        }
        return builder.create();
    }

    public void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    public void setColor(@NonNull ThermalControl.Palette.Color color) {
        mColor = (EditColor) color;
    }

    private void updatePreview() {
        mPreview.setBackgroundColor(Color.rgb((int) (mRedView.getValue() * 255.0f + 0.5f),
                (int) (mGreenView.getValue() * 255.0f + 0.5f), (int) (mBlueView.getValue() * 255.0f + 0.5f)));
    }

    private final DialogInterface.OnClickListener mClickListener = (dialog, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mListener != null) {
                mColor.mRed = mRedView.getValue();
                mColor.mGreen = mGreenView.getValue();
                mColor.mBlue = mBlueView.getValue();
                mColor.mPosition = mPositionView.getValue();
                mListener.onColorChanged(mColor);
            }
            dialog.dismiss();
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            if (mListener != null) {
                mListener.onColorChanged(null);
            }
            dialog.dismiss();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            dialog.cancel();
        }
    };

    private static class EditColor implements ThermalControl.Palette.Color {

        private double mRed;

        private double mGreen;

        private double mBlue;

        private double mPosition;

        @Override
        public double getRed() {
            return mRed;
        }

        @Override
        public double getGreen() {
            return mGreen;
        }

        @Override
        public double getBlue() {
            return mBlue;
        }

        @Override
        public double getPosition() {
            return mPosition;
        }
    }
}
