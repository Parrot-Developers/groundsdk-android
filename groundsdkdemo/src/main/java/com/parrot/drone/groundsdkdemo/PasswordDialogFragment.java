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
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;

public class PasswordDialogFragment extends DialogFragment {

    public interface PasswordAcquiredListener {

        void onPasswordAcquired(@NonNull String password);
    }

    private PasswordAcquiredListener mListener;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private EditText mPasswordInput;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private TextView mValidityText;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private Button mPositiveButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        @SuppressLint("InflateParams")
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_password, null);
        mPasswordInput = content.findViewById(android.R.id.edit);
        mValidityText = content.findViewById(android.R.id.text1);
        mPasswordInput.addTextChangedListener(mTextWatcher);
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_dialog_password)
                .setView(content)
                .setPositiveButton(R.string.action_done, mClickListener)
                .setNegativeButton(R.string.action_cancel, mClickListener)
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mListener == null) {
            mListener = (PasswordAcquiredListener) context;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mPositiveButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        mPositiveButton.setEnabled(false);
    }

    public void setListener(PasswordAcquiredListener listener) {
        mListener = listener;
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean valid = WifiAccessPoint.SecuritySetting.isPasswordValid(mPasswordInput.getText().toString());
            mValidityText.setText(valid ? R.string.valid : R.string.invalid);
            mValidityText.setTextColor(valid ? Color.GREEN : Color.RED);
            mPositiveButton.setEnabled(valid);
        }
    };

    private final DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mListener != null) {
                    mListener.onPasswordAcquired(mPasswordInput.getText().toString());
                }
                dialog.dismiss();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                dialog.cancel();
            }
        }
    };
}
