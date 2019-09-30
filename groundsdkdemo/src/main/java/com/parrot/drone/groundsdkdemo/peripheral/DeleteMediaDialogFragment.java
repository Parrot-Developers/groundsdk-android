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

package com.parrot.drone.groundsdkdemo.peripheral;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdkdemo.R;

public class DeleteMediaDialogFragment extends DialogFragment {


    interface Listener {

        void onTaskCanceled();

        void onDialogDismissed();
    }

    private Listener mListener;

    private MediaDeleter mDeleter;

    private TextView mMediaCountText;

    private ProgressBar mMediaProgressBar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        @SuppressLint("InflateParams")
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_delete_media, null);
        mMediaCountText = content.findViewById(R.id.media_count);
        mMediaProgressBar = content.findViewById(R.id.media_progress);
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_dialog_delete_media)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, mClickListener)
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            mListener = (Listener) targetFragment;
        } else {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDeleter != null) {
            update(mDeleter);
        }
    }

    final void update(@NonNull MediaDeleter deleter) {
        mDeleter = deleter;
        if (isResumed()) {
            int deleted = mDeleter.getCurrentMediaIndex();
            int total = mDeleter.getTotalMediaCount();
            mMediaCountText.setText(getString(R.string.media_counter_format, deleted, total));
            mMediaProgressBar.setProgress(deleted * 100 / total);
        }
    }

    private final DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mListener.onTaskCanceled();
            }
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDeleter = null;
        mListener.onDialogDismissed();
        super.onDismiss(dialog);
    }
}
