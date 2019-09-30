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
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadEditMappingActivity.EXTRA_ENTRY_ACTION;
import static com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadEditMappingActivity.EXTRA_ENTRY_MODEL;
import static com.parrot.drone.groundsdkdemo.peripheral.gamepad.GamepadEditMappingActivity.EXTRA_ENTRY_TYPE;

public class GamepadMappingFragment extends Fragment {

    private static final String ARG_RC_UID = "rc_uid";

    private static final String ARG_DRONE_MODEL = "drone_model";

    static GamepadMappingFragment newInstance(@NonNull String rcUid, @NonNull Drone.Model droneModel) {
        Bundle args = new Bundle();
        args.putString(ARG_RC_UID, rcUid);
        args.putInt(ARG_DRONE_MODEL, droneModel.ordinal());
        GamepadMappingFragment fragment = new GamepadMappingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String mRcUid;

    private Drone.Model mDroneModel;

    private Adapter mAdapter;

    private final ItemTouchHelper mSwipeHandler = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    mAdapter.handleSwipe(viewHolder.getAdapterPosition());
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        mRcUid = args.getString(ARG_RC_UID);
        mDroneModel = Drone.Model.values()[args.getInt(ARG_DRONE_MODEL, -1)];

        mAdapter = new Adapter();
        assert getActivity() != null;
        RemoteControl rc = ManagedGroundSdk.obtainSession(getActivity()).getRemoteControl(mRcUid);
        if (rc != null) {
            GamepadFacadeProvider.of(rc).getPeripheral(GamepadFacade.class, gamepad -> mAdapter.setGamepad(gamepad));
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
        mSwipeHandler.attachToRecyclerView(recyclerView);
        return recyclerView;
    }

    private class Adapter extends RecyclerView.Adapter<EntryHolder<?>> {

        Adapter() {
            mMappingEntries = new GamepadFacade.Mapping.Entry[0];
        }

        private GamepadFacade mGamepad;

        private GamepadFacade.Mapping.Entry[] mMappingEntries;

        void setGamepad(GamepadFacade gamepad) {
            mGamepad = gamepad;
            if (mGamepad == null) {
                mMappingEntries = null;
            } else {
                Set<GamepadFacade.Mapping.Entry> entrySet = mGamepad.getMapping(mDroneModel);
                assert entrySet != null;
                mMappingEntries = entrySet.toArray(new GamepadFacade.Mapping.Entry[0]);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EntryHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (GamepadFacade.Mapping.Entry.Type.values()[viewType]) {
                case BUTTONS_MAPPING:
                    return new ButtonsEntryHolder(parent);
                case AXIS_MAPPING:
                    return new AxisEntryHolder(parent);
                default:
                    throw new AssertionError();
            }
        }

        void handleSwipe(int position) {
            mGamepad.unregisterMappingEntry(mMappingEntries[position]);
        }

        @Override
        public void onBindViewHolder(@NonNull EntryHolder<?> holder, int position) {
            holder.bindEntry(mMappingEntries[position]);
        }

        @Override
        public int getItemCount() {
            return mMappingEntries == null ? 0 : mMappingEntries.length;
        }

        @Override
        public int getItemViewType(int position) {
            return mMappingEntries[position].getType().ordinal();
        }
    }

    private abstract class EntryHolder<E extends GamepadFacade.Mapping.Entry> extends RecyclerView.ViewHolder {

        @NonNull
        private final Class<E> mEntryClass;

        @NonNull
        final TextView mActionText;

        @NonNull
        final TextView mButtonsText;

        @NonNull
        final TextView mAxisText;

        @NonNull
        final View mAxisRow;

        @NonNull
        final View mButtonsRow;

        Intent mEditEntryIntent;

        EntryHolder(ViewGroup parent, @NonNull Class<E> entryClass) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.mapping_entry, parent, false));
            mEntryClass = entryClass;
            mActionText = itemView.findViewById(R.id.action);
            mButtonsText = itemView.findViewById(R.id.buttons);
            mAxisText = itemView.findViewById(R.id.axis);
            mAxisRow = itemView.findViewById(R.id.axis_row);
            mButtonsRow = itemView.findViewById(R.id.buttons_row);
            mEditEntryIntent = new Intent(parent.getContext(), GamepadEditMappingActivity.class);
            itemView.findViewById(R.id.btn_edit).setOnClickListener(
                    v -> v.getContext().startActivity(mEditEntryIntent));
        }

        final void bindEntry(@NonNull GamepadFacade.Mapping.Entry entry) {
            mAxisRow.setVisibility(View.GONE);
            mButtonsRow.setVisibility(View.VISIBLE);
            bind(entry.as(mEntryClass));
        }

        @CallSuper
        void bind(@NonNull E entry) {
            mEditEntryIntent = mEditEntryIntent
                    .cloneFilter()
                    .putExtra(EXTRA_DEVICE_UID, mRcUid)
                    .putExtra(EXTRA_ENTRY_MODEL, entry.getDroneModel().ordinal())
                    .putExtra(EXTRA_ENTRY_TYPE, entry.getType().ordinal());
        }
    }

    private final class ButtonsEntryHolder extends EntryHolder<GamepadFacade.Button.MappingEntry> {

        ButtonsEntryHolder(ViewGroup parent) {
            super(parent, GamepadFacade.Button.MappingEntry.class);
        }

        @Override
        void bind(@NonNull GamepadFacade.Button.MappingEntry entry) {
            super.bind(entry);
            mEditEntryIntent.putExtra(EXTRA_ENTRY_ACTION, entry.getAction().ordinal());
            mActionText.setText(entry.getAction().toString());
            mButtonsText.setText(TextUtils.join(" ", entry.getButtonEvents()));
        }
    }

    private final class AxisEntryHolder extends EntryHolder<GamepadFacade.Axis.MappingEntry> {

        AxisEntryHolder(ViewGroup parent) {
            super(parent, GamepadFacade.Axis.MappingEntry.class);
        }

        @Override
        void bind(@NonNull GamepadFacade.Axis.MappingEntry entry) {
            super.bind(entry);
            mEditEntryIntent.putExtra(EXTRA_ENTRY_ACTION, entry.getAction().ordinal());
            mActionText.setText(entry.getAction().toString());
            Set<GamepadFacade.Button.Event> buttons = entry.getButtonEvents();
            if (buttons.isEmpty()) {
                mButtonsRow.setVisibility(View.GONE);
            } else {
                mButtonsText.setText(TextUtils.join(" ", entry.getButtonEvents()));
            }
            mAxisText.setText(entry.getAxisEvent().toString());
            mAxisRow.setVisibility(View.VISIBLE);
        }
    }
}
