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

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdkdemo.DebugTagDialogFragment;
import com.parrot.drone.groundsdkdemo.FormatDialogFragment;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.PasswordDialogFragment;
import com.parrot.drone.groundsdkdemo.PickConnectorDialog;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.StoragePasswordFragment;
import com.parrot.drone.groundsdkdemo.animation.Animations;
import com.parrot.drone.groundsdkdemo.animation.PickAnimationDialog;
import com.parrot.drone.groundsdkdemo.animation.PickFlipDirectionDialog;
import com.parrot.drone.groundsdkdemo.hud.CopterHudActivity;
import com.parrot.drone.groundsdkdemo.hud.HmdActivity;

import java.io.File;
import java.util.EnumSet;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class DroneInfoActivity extends GroundSdkActivityBase
        implements PasswordDialogFragment.PasswordAcquiredListener, PickConnectorDialog.Listener,
                   AnimationContent.OnAnimationConfigRequestListener, PickAnimationDialog.Listener,
                   PickFlipDirectionDialog.Listener, RemovableUserStorageContent.OnUserStorageFormatRequestListener,
                   RemovableUserStorageContent.OnUserStorageEnterPasswordListener,
                   OnPickFileRequestListener, PickFileDialog.Listener, DevToolboxContent.OnDebugTagRequestListener {

    private Button mHudButton;

    private Button mFpvButton;

    @SuppressWarnings("FieldCanBeLocal")
    private TextView mModelText;

    private TextView mStatusText;

    private Button mForgetButton;

    private Button mConnectButton;

    private Intent mHudIntent;

    private Drone mDrone;

    @Nullable
    private DeviceConnector mChosenConnector;

    private AnimationContent.OnAnimationConfigRequestListener.Response mAnimResponse;

    private OnPickFileRequestListener.Response mFileResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), it -> finish());

        if (mDrone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_drone_info);

        Content flyingIndicatorsContent = null;

        switch (mDrone.getModel()) {
            case ANAFI_4K:
            case ANAFI_THERMAL:
            case ANAFI_UA:
            case ANAFI_USA:
                mHudIntent = new Intent(this, CopterHudActivity.class);
                flyingIndicatorsContent = new FlyingIndicatorsContent(mDrone);
                break;
        }
        mHudIntent.putExtra(EXTRA_DEVICE_UID, mDrone.getUid());

        mHudButton = findViewById(R.id.btn_hud);
        mFpvButton = findViewById(R.id.btn_fpv);

        mModelText = findViewById(R.id.model);
        mStatusText = findViewById(R.id.status);
        mForgetButton = findViewById(R.id.btn_forget);
        mConnectButton = findViewById(R.id.btn_connect);

        mModelText.setText(mDrone.getModel().toString());

        mDrone.getName(this::setTitle);

        mDrone.getState(state -> {
            assert state != null;
            mStatusText.setText(state.toString());
            DeviceState.ConnectionState connectionState = state.getConnectionState();
            mConnectButton.setEnabled(state.canBeConnected() || state.canBeDisconnected());
            mConnectButton.setText(connectionState == DeviceState.ConnectionState.DISCONNECTED
                    ? R.string.action_connect : R.string.action_disconnect);
            mHudButton.setEnabled(connectionState == DeviceState.ConnectionState.CONNECTED);
            mFpvButton.setEnabled(connectionState == DeviceState.ConnectionState.CONNECTED);
            mForgetButton.setEnabled(state.canBeForgotten());
        });

        mHudButton.setOnClickListener(v -> startActivity(mHudIntent));
        mFpvButton.setOnClickListener(v -> startActivity(
                new Intent(this, HmdActivity.class).putExtra(EXTRA_DEVICE_UID, mDrone.getUid())));

        mForgetButton.setOnClickListener(v -> mDrone.forget());

        mConnectButton.setOnClickListener(v -> {
            DeviceState state = mDrone.getState();
            switch (state.getConnectionState()) {
                case DISCONNECTED:
                    if (state.getConnectionStateCause() == DeviceState.ConnectionStateCause.BAD_PASSWORD) {
                        DialogFragment passwordRequest = new PasswordDialogFragment();
                        passwordRequest.show(getSupportFragmentManager(), null);
                    } else {
                        if (state.getConnectors().length > 1) {
                            // too many connectors, ask to the user which one to chose
                            PickConnectorDialog connectorChoice = new PickConnectorDialog();
                            connectorChoice.setConnectors(state.getConnectors());
                            connectorChoice.show(getSupportFragmentManager(), null);
                        } else {
                            mDrone.connect(state.getConnectors()[0]);
                        }
                    }
                    break;
                case CONNECTING:
                case CONNECTED:
                    mDrone.disconnect();
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
                new AlarmsContent(mDrone),
                new AltimeterContent(mDrone),
                new AttitudeContent(mDrone),
                new CompassContent(mDrone),
                flyingIndicatorsContent,
                new GpsContent(mDrone),
                new SpeedometerContent(mDrone),
                new RadioContent(mDrone),
                new BatteryInfoContent(mDrone),
                new FlightMeterContent(mDrone),
                new CameraExposureContent(mDrone),
                new PhotoProgressContent(mDrone),
                new HeaderContent(getString(R.string.header_piloting_itf)),
                new ManualCopterContent(mDrone),
                new ReturnHomeContent(mDrone),
                new FlightPlanContent(mDrone),
                new GuidedContent(mDrone),
                new LookAtContent(mDrone),
                new FollowMeContent(mDrone),
                new PointOfInterestContent(mDrone),
                new AnimationContent(mDrone),
                new HeaderContent(getString(R.string.header_peripherals)),
                new PilotingControlContent(mDrone),
                new PreciseHomeContent(mDrone),
                new ThermalContent(mDrone),
                new TargetTrackerContent(mDrone),
                new MagnetometerContent(mDrone),
                new LiveStreamContent(mDrone),
                CameraContent.main(mDrone),
                CameraContent.thermal(mDrone),
                CameraContent.blendedThermal(mDrone),
                new AntiFlickerContent(mDrone),
                new GimbalContent(mDrone),
                new GeofenceContent(mDrone),
                new SysInfoContent(mDrone),
                new BeeperContent(mDrone),
                new LedsContent(mDrone),
                new CopterMotorsContent(mDrone),
                new CrashReportDownloaderContent(mDrone),
                new FlightDataDownloaderContent(mDrone),
                new FlightLogDownloaderContent(mDrone),
                new MediaStoreContent(mDrone),
                new RemovableUserStorageContent(mDrone),
                new WifiAccessPointContent(mDrone),
                new WifiScannerContent(mDrone),
                new UpdaterContent(mDrone),
                new DevToolboxContent(mDrone),
                new BatteryGaugeUpdaterContent(mDrone),
                new DriContent(mDrone),
                new CertificateUploaderContent(mDrone)
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
    public void onPasswordAcquired(@NonNull String password) {
        DeviceConnector connector = mChosenConnector != null ? mChosenConnector : mDrone.getState().getConnectors()[0];
        mDrone.connect(connector, password);
        mChosenConnector = null;
    }

    @Override
    public void onConnectorAcquired(@NonNull DeviceConnector connector) {
        mChosenConnector = connector;
        mDrone.connect(connector);
    }

    @Override
    public void onAnimationConfigRequest(@NonNull AnimationContent.OnAnimationConfigRequestListener.Response response) {
        mAnimResponse = response;
        PickAnimationDialog.newInstance(mDrone.getUid()).show(getSupportFragmentManager(), null);
    }

    @Override
    public void onAnimationTypeAcquired(@NonNull Animation.Type animationType) {
        Animation.Config config = Animations.defaultConfigFor(animationType);
        if (config != null) {
            mAnimResponse.setAnimationConfig(config);
        } else if (animationType == Animation.Type.FLIP) {
            new PickFlipDirectionDialog().show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onFlipDirectionAcquired(@NonNull Flip.Direction direction) {
        mAnimResponse.setAnimationConfig(new Flip.Config(direction));
    }

    @Override
    public void onUserStorageFormatRequest(@Nullable FormatDialogFragment.Listener listener,
                                           @NonNull EnumSet<RemovableUserStorage.FormattingType> supportedTypes,
                                           boolean isEncryptionSupported) {
        FormatDialogFragment formatDialog = new FormatDialogFragment();
        formatDialog.setListener(listener);
        formatDialog.setSupportedFormattedTypes(supportedTypes);
        formatDialog.setEncryptionSupported(isEncryptionSupported);
        formatDialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onUserStorageEnterPassword(@Nullable StoragePasswordFragment.Listener listener) {
        StoragePasswordFragment passwordDialog = new StoragePasswordFragment();
        passwordDialog.setListener(listener);
        passwordDialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onPickFileRequest(@StringRes int title, @Nullable String extensionFilter,
                                  @NonNull OnPickFileRequestListener.Response response) {
        mFileResponse = response;
        PickFileDialog.newInstance(title, extensionFilter).show(getSupportFragmentManager(), null);
    }

    @Override
    public void onFileSelected(@NonNull File file) {
        mFileResponse.setFile(file);
    }

    @Override
    public void onDebugTagRequest(@NonNull DebugTagDialogFragment.Listener listener) {
        DebugTagDialogFragment debugTagDialog = new DebugTagDialogFragment();
        debugTagDialog.setListener(listener);
        debugTagDialog.show(getSupportFragmentManager(), null);
    }
}
