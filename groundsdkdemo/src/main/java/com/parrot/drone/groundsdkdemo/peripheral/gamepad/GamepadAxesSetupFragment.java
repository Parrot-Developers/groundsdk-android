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

package com.parrot.drone.groundsdkdemo.peripheral.gamepad;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisInterpolator;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

public class GamepadAxesSetupFragment extends Fragment {

    private static final String ARG_RC_UID = "rc_uid";

    private static final String ARG_DRONE_MODEL = "drone_model";

    static GamepadAxesSetupFragment newInstance(@NonNull String rcUid, @NonNull Drone.Model droneModel) {
        Bundle args = new Bundle();
        args.putString(ARG_RC_UID, rcUid);
        args.putInt(ARG_DRONE_MODEL, droneModel.ordinal());
        GamepadAxesSetupFragment fragment = new GamepadAxesSetupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Drone.Model mDroneModel;

    private Adapter mAdapter;

    private GamepadFacade mGamepad;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        mDroneModel = Drone.Model.values()[args.getInt(ARG_DRONE_MODEL, -1)];

        mAdapter = new Adapter();

        //noinspection ConstantConditions
        RemoteControl rc = ManagedGroundSdk.obtainSession(getActivity()).getRemoteControl(args.getString(ARG_RC_UID));
        if (rc != null) {
            GamepadFacadeProvider.of(rc).getPeripheral(GamepadFacade.class, gamepad -> {
                mGamepad = gamepad;
                mAdapter.notifyDataSetChanged();
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        assert context != null;
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        return recyclerView;
    }

    private class Adapter extends RecyclerView.Adapter<AxisSetupEntryHolder> {

        @NonNull
        @Override
        public AxisSetupEntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new AxisSetupEntryHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull AxisSetupEntryHolder holder, int position) {
            holder.bindEntry(getItem(position));
        }

        GamepadFacade.Axis getItem(int position) {
            return mGamepad.allAxes().get(position);
        }

        @Override
        public int getItemCount() {
            return mGamepad.allAxes().size();
        }
    }

    private class AxisSetupEntryHolder extends RecyclerView.ViewHolder {

        @NonNull
        final TextView mAxisText;

        @NonNull
        final Spinner mInterpolatorSpinner;

        @NonNull
        final CheckBox mReversedCheckbox;

        AxisSetupEntryHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.axis_setup_entry, parent, false));
            mAxisText = itemView.findViewById(R.id.axis);
            mInterpolatorSpinner = itemView.findViewById(R.id.interpolator);
            mInterpolatorSpinner.setAdapter(new ArrayAdapter<>(parent.getContext(),
                    R.layout.support_simple_spinner_dropdown_item, AxisInterpolator.values()));
            mInterpolatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mGamepad.setAxisInterpolator(mDroneModel, mAdapter.getItem(getAdapterPosition()),
                            AxisInterpolator.values()[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    throw new IllegalStateException();
                }
            });
            mReversedCheckbox = itemView.findViewById(R.id.reversed);
            mReversedCheckbox.setOnClickListener(v -> mGamepad.reverseAxis(mDroneModel,
                    mAdapter.getItem(getAdapterPosition())));
        }

        final void bindEntry(@NonNull GamepadFacade.Axis axis) {
            mAxisText.setText(axis.toString());
            //noinspection ConstantConditions
            mInterpolatorSpinner.setSelection(mGamepad.getAxisInterpolators(mDroneModel).get(axis).ordinal());
            //noinspection ConstantConditions
            mReversedCheckbox.setChecked(mGamepad.getReversedAxes(mDroneModel).contains(axis));
        }
    }
}
