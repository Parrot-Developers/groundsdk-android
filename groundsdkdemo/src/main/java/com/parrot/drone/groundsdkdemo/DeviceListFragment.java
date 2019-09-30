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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.Ref;

import java.util.List;

abstract class DeviceListFragment<ENTRY> extends ListFragment {

    interface OnDeviceSelectedListener {

        void onDeviceSelected(@NonNull DeviceListFragment<?> fragment, @NonNull String deviceUid);
    }

    @SuppressWarnings("NullableProblems")
    @NonNull
    private GroundSdk mSdk;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private Adapter mListAdapter;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private OnDeviceSelectedListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getActivity() != null;
        mSdk = ManagedGroundSdk.obtainSession(getActivity());
        mListAdapter = new Adapter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (OnDeviceSelectedListener) context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mListAdapter);

        watchList(list -> mListAdapter.setDataList(list));
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        mListener.onDeviceSelected(this, getUid(mListAdapter.getItem(position)));
    }

    @NonNull
    final GroundSdk groundSdk() {
        return mSdk;
    }

    protected abstract void watchList(@NonNull Ref.Observer<List<ENTRY>> observer);

    @NonNull
    protected abstract String getUid(@NonNull ENTRY entry);

    @NonNull
    protected abstract String getModel(@NonNull ENTRY entry);

    @NonNull
    protected abstract String getName(@NonNull ENTRY entry);

    @NonNull
    protected abstract String getState(@NonNull ENTRY entry);

    private class Adapter extends BaseAdapter {

        private List<ENTRY> mData;

        void setDataList(List<ENTRY> dataList) {
            mData = dataList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mData != null) {
                return mData.size();
            }
            return 0;
        }

        @Override
        public ENTRY getItem(int position) {
            if (mData != null) {
                return mData.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return getUid(getItem(position)).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            View view = (convertView != null)
                    ? convertView : LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);

            ENTRY entry = getItem(position);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);

            text1.setText(context.getString(R.string.device_id_format, getName(entry), getUid(entry)));
            text2.setText(context.getString(R.string.device_state_format, getModel(entry), getState(entry)));
            return view;
        }
    }
}
