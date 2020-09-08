/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

public class PickFileDialog extends DialogFragment {

    private static final String EXTRA_TITLE = "TITLE";

    private static final String EXTRA_EXTENSION_FILTER = "EXTENSION_FILTER";

    private File mExternalFilesDir;

    @NonNull
    static PickFileDialog newInstance(@StringRes int title, @Nullable String extensionFilter) {
        PickFileDialog fragment = new PickFileDialog();
        Bundle args = new Bundle();
        args.putInt(EXTRA_TITLE, title);
        args.putString(EXTRA_EXTENSION_FILTER, extensionFilter);
        fragment.setArguments(args);
        return fragment;
    }

    public interface Listener {

        void onFileSelected(@NonNull File file);
    }

    private PickFileDialog.Listener mListener;

    private ArrayAdapter<String> mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getArguments() != null;
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getInt(EXTRA_TITLE))
                .setAdapter(mAdapter, mClickListener)
                .create();
    }

    private final DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            String filename = mAdapter.getItem(which);
            if (filename != null) {
                mListener.onFileSelected(new File(mExternalFilesDir, filename));
            }
            dismiss();
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (PickFileDialog.Listener) context;

        mExternalFilesDir = context.getExternalFilesDir(null);
        if (mExternalFilesDir == null) {
            dismiss();
        } else {
            assert getArguments() != null;
            String filter = getArguments().getString(EXTRA_EXTENSION_FILTER, "");
            mAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1,
                    mExternalFilesDir.list((dir, name) -> name.endsWith(filter)));
        }
    }
}
