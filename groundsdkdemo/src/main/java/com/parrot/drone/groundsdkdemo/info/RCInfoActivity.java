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

package com.parrot.drone.groundsdkdemo.info;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.PickConnectorDialog;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class RCInfoActivity extends GroundSdkActivityBase
        implements PickConnectorDialog.Listener {

    @SuppressWarnings("FieldCanBeLocal")
    private TextView mModelText;

    private TextView mStatusText;

    private Button mForgetButton;

    private Button mConnectButton;

    private RemoteControl mRc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRc = groundSdk().getRemoteControl(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());

        if (mRc == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_rc_info);

        mModelText = findViewById(R.id.model);
        mStatusText = findViewById(R.id.status);
        mForgetButton = findViewById(R.id.btn_forget);
        mConnectButton = findViewById(R.id.btn_connect);

        mModelText.setText(mRc.getModel().toString());

        mRc.getName(this::setTitle);

        mRc.getState(state -> {
            assert state != null;
            DeviceState.ConnectionState connectionState = state.getConnectionState();
            mStatusText.setText(state.toString());
            mConnectButton.setEnabled(state.canBeConnected() || state.canBeDisconnected());
            mConnectButton.setText(connectionState == DeviceState.ConnectionState.DISCONNECTED
                    ? R.string.action_connect : R.string.action_disconnect);
            mForgetButton.setEnabled(state.canBeForgotten());
        });


        mForgetButton.setOnClickListener(v -> mRc.forget());

        mConnectButton.setOnClickListener(v -> {
            DeviceState state = mRc.getState();
            switch (state.getConnectionState()) {
                case DISCONNECTED:
                    if (state.getConnectors().length > 1) {
                        // too many connectors, ask to the user which one to chose
                        PickConnectorDialog connectorChoice = new PickConnectorDialog();
                        connectorChoice.setConnectors(state.getConnectors());
                        connectorChoice.show(getSupportFragmentManager(), null);
                    } else {
                        mRc.connect(state.getConnectors()[0]);
                    }
                    break;
                case CONNECTING:
                case CONNECTED:
                    mRc.disconnect();
                    break;
                case DISCONNECTING:
                    // nothing to do
                    break;
            }
        });

        FirmwareManager firmwareManager = groundSdk().getFacility(FirmwareManager.class);
        assert firmwareManager != null;
        // get fresh update info
        firmwareManager.queryRemoteFirmwares();

        Content.ViewAdapter adapter = new Content.ViewAdapter(
                new HeaderContent(getString(R.string.header_instruments)),
                new BatteryInfoContent(mRc),
                new CompassContent(mRc),
                new HeaderContent(getString(R.string.header_peripherals)),
                new DroneFinderContent(mRc),
                new VirtualGamepadContent(mRc),
                new GamepadContent(mRc),
                new CopilotContent(mRc),
                new SysInfoContent(mRc),
                new CrashReportDownloaderContent(mRc),
                new FlightLogDownloaderContent(mRc),
                new WifiAccessPointContent(mRc),
                new MagnetometerContent(mRc),
                new UpdaterContent(mRc)
        );

        RecyclerView recyclerView = findViewById(R.id.info_content);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        // item change animations are a bit too flashy, disable them
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onConnectorAcquired(@NonNull DeviceConnector connector) {
        mRc.connect(connector);
    }
}
