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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class GamepadAxesSetupActivity extends GroundSdkActivityBase {

    private String mRcUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RemoteControl rc = groundSdk().getRemoteControl(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (rc == null) {
            finish();
            return;
        }
        mRcUid = rc.getUid();

        setContentView(R.layout.activity_gamepad_axes_setup);

        setSupportActionBar(findViewById(R.id.toolbar));

        ViewPager pager = findViewById(R.id.pager);
        TabLayout tabs = findViewById(R.id.tabs);

        PageAdapter adapter = new PageAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);

        GamepadFacadeProvider.of(rc).getPeripheral(GamepadFacade.class, gamepad -> {
            if (gamepad == null) {
                finish();
            } else {
                adapter.setModels(gamepad.getSupportedDroneModels());
            }
        });
    }

    private class PageAdapter extends FragmentStatePagerAdapter {

        private Drone.Model[] mModels;

        PageAdapter(FragmentManager fm) {
            super(fm);
        }

        void setModels(@NonNull Set<Drone.Model> models) {
            mModels = models.toArray(new Drone.Model[0]);
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            return GamepadAxesSetupFragment.newInstance(mRcUid, mModels[position]);
        }

        @Override
        public int getCount() {
            return mModels == null ? 0 : mModels.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mModels[position].toString();
        }
    }
}
