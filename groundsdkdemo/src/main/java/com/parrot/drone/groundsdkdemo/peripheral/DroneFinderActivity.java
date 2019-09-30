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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.PasswordDialogFragment;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.info.DroneInfoActivity;

import java.util.Collections;
import java.util.List;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class DroneFinderActivity extends GroundSdkActivityBase
        implements PasswordDialogFragment.PasswordAcquiredListener {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private DroneListAdapter mDroneListAdapter;

    @Nullable
    private DroneFinder mFinder;

    private DroneFinder.DiscoveredDrone mDroneToConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_drone_finder);

        mDroneListAdapter = new DroneListAdapter(getLayoutInflater());
        ListView listView = findViewById(android.R.id.list);
        listView.setAdapter(mDroneListAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            DroneFinder.DiscoveredDrone drone = mDroneListAdapter.getItem(position);
            if (drone.getConnectionSecurity() == DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD) {
                mDroneToConnect = drone;
                DialogFragment fragment = new PasswordDialogFragment();
                fragment.show(getSupportFragmentManager(), null);
            } else {
                if (mFinder != null && mFinder.connect(drone)) {
                    switchToDroneInfoActivity(drone);
                }
            }
        });

        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh);
        refreshLayout.setColorSchemeResources(R.color.color_accent);
        refreshLayout.setOnRefreshListener(() -> {
            if (mFinder != null) {
                mFinder.refresh();
            }
        });

        RemoteControl rc = groundSdk().getRemoteControl(getIntent().getStringExtra(EXTRA_DEVICE_UID),
                uid -> finish());

        if (rc == null) {
            finish();
            return;
        }

        rc.getPeripheral(DroneFinder.class, droneFinder -> {
            if (mFinder == null && droneFinder != null) {
                droneFinder.refresh();
            }
            mFinder = droneFinder;
            if (mFinder != null) {
                boolean refreshing = mFinder.getState() == DroneFinder.State.SCANNING;
                refreshLayout.post(() -> refreshLayout.setRefreshing(refreshing));
                mDroneListAdapter.setDroneList(mFinder.getDiscoveredDrones());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFinder != null) {
            mFinder.clear();
        }
    }

    @Override
    public void onPasswordAcquired(@NonNull String password) {
        if (mDroneToConnect != null) {
            if (mFinder != null && mFinder.connect(mDroneToConnect, password)) {
                switchToDroneInfoActivity(mDroneToConnect);
            }
            mDroneToConnect = null;
        }
    }

    private void switchToDroneInfoActivity(@NonNull DroneFinder.DiscoveredDrone droneToConnect) {
        Intent intent = new Intent(this, DroneInfoActivity.class);
        intent.putExtra(EXTRA_DEVICE_UID, droneToConnect.getUid());
        startActivity(intent);
        finish();
    }

    private static final class DroneListAdapter extends BaseAdapter {

        @NonNull
        private List<DroneFinder.DiscoveredDrone> mDrones;

        @NonNull
        private final LayoutInflater mLayoutInflater;

        DroneListAdapter(@NonNull LayoutInflater layoutInflater) {
            mLayoutInflater = layoutInflater;
            mDrones = Collections.emptyList();
        }

        void setDroneList(@NonNull List<DroneFinder.DiscoveredDrone> drones) {
            mDrones = drones;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDrones.size();
        }

        @Override
        public DroneFinder.DiscoveredDrone getItem(int position) {
            return mDrones.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getUid().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = (convertView != null) ? convertView :
                    mLayoutInflater.inflate(android.R.layout.simple_list_item_2, null);
            DroneFinder.DiscoveredDrone drone = getItem(position);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);
            Context context = view.getContext();
            text1.setText(context.getString(R.string.drone_finder_drone_id_format, drone.getName(), drone.getModel()));
            text2.setText(context.getString(R.string.drone_finder_drone_info_format, drone.getUid(),
                    context.getString(drone.isKnown() ? R.string.known : R.string.unknown), drone.getRssi(),
                    drone.getConnectionSecurity().toString()));
            return view;
        }
    }
}
