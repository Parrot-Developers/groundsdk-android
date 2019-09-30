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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdkdemo.R;

import java.util.Arrays;

public class WifiAutoChannelModeChoiceDialogFragment extends DialogFragment {

    interface ModeSelectionListener {

        void onModeSelected(@NonNull WifiAccessPoint.ChannelSetting.SelectionMode mode);
    }

    @SuppressWarnings("NullableProblems")
    @NonNull
    private ModeSelectionListener mListener;

    private ArrayAdapter<WifiAccessPoint.ChannelSetting.SelectionMode> mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        mAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, Arrays.asList(
                WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND,
                WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND,
                WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND
        ));

        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_dialog_wifi_auto_mode_choice)
                .setSingleChoiceItems(mAdapter, 0, mClickListener)
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (ModeSelectionListener) context;
    }

    private final DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            WifiAccessPoint.ChannelSetting.SelectionMode mode = mAdapter.getItem(which);
            assert mode != null;
            mListener.onModeSelected(mode);
            dialog.dismiss();
        }
    };
}
