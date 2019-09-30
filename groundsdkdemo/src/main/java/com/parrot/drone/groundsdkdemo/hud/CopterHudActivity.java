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

package com.parrot.drone.groundsdkdemo.hud;

import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdk.device.instrument.Altimeter;
import com.parrot.drone.groundsdk.device.instrument.AttitudeIndicator;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.device.instrument.Compass;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators;
import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdk.device.peripheral.MainCamera;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.AnimationItf;
import com.parrot.drone.groundsdk.device.pilotingitf.LookAtPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf.SmartTakeOffLandAction;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.facility.UserLocation;
import com.parrot.drone.groundsdk.stream.GsdkStreamView;
import com.parrot.drone.groundsdk.value.OptionalDouble;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.animation.Animations;
import com.parrot.drone.groundsdkdemo.animation.PickAnimationDialog;
import com.parrot.drone.groundsdkdemo.animation.PickFlipDirectionDialog;
import com.parrot.drone.groundsdkdemo.format.LocationFormatter;
import com.parrot.drone.groundsdkdemo.info.ZoomLevelView;
import com.parrot.drone.groundsdkdemo.info.ZoomVelocityView;
import com.parrot.drone.groundsdkdemo.peripheral.debugsettings.DebugSettingsRecyclerView;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

/** Activity to pilot a copter. */
public class CopterHudActivity extends GroundSdkActivityBase
        implements PickAnimationDialog.Listener, PickFlipDirectionDialog.Listener {

    private View mDrawer;

    private View mContent;

    private View mResizableContent;

    private View mDrawerGrip;

    private DrawerLayout mDrawerLayout;

    private ImageView mFlyingIndicators;

    private ImageView mPowerAlarm;

    private ImageView mMotorCutoutAlarm;

    private ImageView mMotorErrorAlarm;

    private ImageView mUserEmergencyAlarm;

    private AttitudeView mAttitudeView;

    private ImageView mBatteryLevelIcon;

    private TextView mBatteryLevelText;

    private TextView mDistanceText;

    private TextView mLocationText;

    private ImageView mGpsIcon;

    private HeadingView mHeadingView;

    private AltimeterView mAltimeterView;

    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mUserEmergencyBtn;

    private ImageButton mTakeOffLandBtn;

    private ImageButton mReturnHomeBtn;

    private ImageButton mAnimationBtn;

    private ImageButton mLookAtBtn;

    private ZoomLevelView mZoomLevelView;

    private ZoomVelocityView mZoomVelocityView;

    @SuppressWarnings("FieldCanBeLocal")
    private ToggleButton mOverlayVisibilityBtn;

    private JoystickView mRollPitchJoystick;

    private JoystickView mYawGazJoystick;

    private Drone mDrone;

    private ManualCopterPilotingItf mPilotingItf;

    private ReturnHomePilotingItf mReturnHomeItf;

    private LookAtPilotingItf mLookAtItf;

    private PointOfInterestPilotingItf mPointOfInterestPilotingItf;

    private AnimationItf mAnimationItf;

    private GsdkStreamView mStreamView;

    private DebugSettingsRecyclerView mDebugSettingsList;

    private boolean mIsTablet;

    private Animation.Config mAnimationConfig;

    private Location mDroneLocation;

    private Location mUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (mDrone == null) {
            finish();
            return;
        }

        mIsTablet = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                    Configuration.SCREENLAYOUT_SIZE_LARGE;

        setContentView(R.layout.activity_copter_hud);

        mContent = findViewById(R.id.content);
        mDrawer = findViewById(R.id.drawer);
        mResizableContent = findViewById(R.id.resizable_content);
        mDrawerGrip = findViewById(R.id.drawer_grip);

        mStreamView = findViewById(R.id.video_view);
        mFlyingIndicators = findViewById(R.id.flying_indicator);
        mPowerAlarm = findViewById(R.id.power_alarm);
        mMotorCutoutAlarm = findViewById(R.id.motor_cut_out_alarm);
        mMotorErrorAlarm = findViewById(R.id.motor_error_alarm);
        mUserEmergencyAlarm = findViewById(R.id.user_emergency_alarm);
        mAttitudeView = findViewById(R.id.attitude);
        mBatteryLevelIcon = findViewById(R.id.battery_level_icon);
        mBatteryLevelText = findViewById(R.id.battery_level);
        mDistanceText = findViewById(R.id.distance);
        mLocationText = findViewById(R.id.location_text);
        mGpsIcon = findViewById(R.id.gps);
        mAltimeterView = findViewById(R.id.altimeter);
        mHeadingView = findViewById(R.id.heading);
        mUserEmergencyBtn = findViewById(R.id.emergency_btn);
        mTakeOffLandBtn = findViewById(R.id.take_off_land_btn);
        mReturnHomeBtn = findViewById(R.id.return_home_btn);
        mAnimationBtn = findViewById(R.id.animation_btn);
        mLookAtBtn = findViewById(R.id.look_at_btn);
        mZoomLevelView = findViewById(R.id.zoom_level);
        mZoomVelocityView = findViewById(R.id.zoom_velocity);
        mOverlayVisibilityBtn = findViewById(R.id.overlay_visibility_btn);
        mRollPitchJoystick = findViewById(R.id.roll_pitch_joystick);
        mYawGazJoystick = findViewById(R.id.yaw_gaz_joystick);
        mDebugSettingsList = findViewById(android.R.id.list);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        // item change animations are a bit too flashy, disable them
        animator.setSupportsChangeAnimations(false);
        mDebugSettingsList.setItemAnimator(animator);
        mDebugSettingsList.setLayoutManager(new LinearLayoutManager(this));

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerLayout.addDrawerListener(mDrawerListener);

        mDrone.getPilotingItf(ManualCopterPilotingItf.class, pilotingItf -> {
            if (pilotingItf == null) {
                finish();
                return;
            }

            mPilotingItf = pilotingItf;
            SmartTakeOffLandAction btnAction = mPilotingItf.getSmartTakeOffLandAction();
            int resId;
            switch (btnAction) {
                case TAKE_OFF:
                    resId = R.drawable.ic_flight_takeoff;
                    break;
                case THROWN_TAKE_OFF:
                    resId = R.drawable.ic_flight_thrown_takeoff;
                    break;
                case LAND:
                case NONE:
                default:
                    resId = R.drawable.ic_flight_land;
                    break;
            }
            mTakeOffLandBtn.setImageResource(resId);
            mTakeOffLandBtn.setEnabled(btnAction != SmartTakeOffLandAction.NONE);
        });

        mDrone.getPilotingItf(ReturnHomePilotingItf.class, pilotingItf -> {
            mReturnHomeItf = pilotingItf;
            Activable.State state =
                    mReturnHomeItf == null ? Activable.State.UNAVAILABLE : mReturnHomeItf.getState();
            mReturnHomeBtn.setEnabled(state != Activable.State.UNAVAILABLE);
            mReturnHomeBtn.setActivated(state == Activable.State.ACTIVE);
        });

        mDrone.getPilotingItf(LookAtPilotingItf.class, pilotingItf -> {
            mLookAtItf = pilotingItf;
            Activable.State state =
                    mLookAtItf == null ? Activable.State.UNAVAILABLE : mLookAtItf.getState();
            mLookAtBtn.setEnabled(state != Activable.State.UNAVAILABLE);
            mLookAtBtn.setActivated(state == Activable.State.ACTIVE);
        });

        mDrone.getPilotingItf(AnimationItf.class, animationItf -> {
            mAnimationItf = animationItf;
            mAnimationBtn.setEnabled(mAnimationItf != null && !mAnimationItf.getAvailableAnimations().isEmpty());
        });

        mDrone.getPilotingItf(PointOfInterestPilotingItf.class,
                pointOfInterestPilotingItf -> mPointOfInterestPilotingItf = pointOfInterestPilotingItf);

        mDrone.getInstrument(FlyingIndicators.class, flyingIndicators -> {
            int resource = 0;
            if (flyingIndicators != null) {
                switch (flyingIndicators.getState()) {
                    case EMERGENCY:
                    case EMERGENCY_LANDING:
                        resource = R.drawable.ic_indicator_emergency;
                        break;
                    case LANDED:
                        switch (flyingIndicators.getLandedState()) {
                            case INITIALIZING:
                            case MOTOR_RAMPING:
                                resource = R.drawable.ic_indicator_standby;
                                break;
                            case IDLE:
                                resource = R.drawable.ic_indicator_ready;
                                break;
                            case WAITING_USER_ACTION:
                                resource = R.drawable.ic_flight_thrown_takeoff;
                                break;
                            case NONE:
                            default:
                                resource = 0;
                                break;
                        }
                        break;
                    case FLYING:
                        switch (flyingIndicators.getFlyingState()) {
                            case TAKING_OFF:
                                resource = R.drawable.ic_indicator_takeoff;
                                break;
                            case LANDING:
                                resource = R.drawable.ic_indicator_landing;
                                break;
                            case WAITING:
                                resource = R.drawable.ic_indicator_waiting;
                                break;
                            case FLYING:
                                resource = R.drawable.ic_indicator_flying;
                                break;
                            case NONE:
                            default:
                                resource = 0;
                                break;
                        }
                        break;
                    default:
                        resource = 0;
                        break;
                }
            }
            mFlyingIndicators.setImageResource(resource);
        });

        mDrone.getInstrument(Alarms.class, alarms -> {
            if (alarms != null) {
                switch (alarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel()) {
                    case CRITICAL:
                        mPowerAlarm.setImageResource(R.drawable.ic_alarm_critical_power);
                        break;
                    case WARNING:
                        mPowerAlarm.setImageResource(R.drawable.ic_alarm_low_power);
                        break;
                    case OFF:
                    case NOT_SUPPORTED:
                        mPowerAlarm.setImageResource(0);
                        break;
                }

                mMotorCutoutAlarm.setVisibility(
                        alarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel() == Alarms.Alarm.Level.CRITICAL
                                ? View.VISIBLE : View.GONE);
                mMotorErrorAlarm.setVisibility(
                        alarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel() == Alarms.Alarm.Level.CRITICAL
                                ? View.VISIBLE : View.GONE);
                mUserEmergencyAlarm.setVisibility(
                        alarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel() == Alarms.Alarm.Level.CRITICAL
                                ? View.VISIBLE : View.GONE);
            }
        });

        mDrone.getInstrument(AttitudeIndicator.class, attitudeIndicator -> {
            float pitch = 0.0f, roll = 0.0f;
            if (attitudeIndicator != null) {
                pitch = (float) attitudeIndicator.getPitch();
                roll = (float) attitudeIndicator.getRoll();
            }
            mAttitudeView.setPitch(pitch);
            mAttitudeView.setRoll(roll);
        });

        mDrone.getInstrument(Gps.class, gps -> {
            mGpsIcon.setAlpha(gps != null && gps.isFixed() ? 1.0f : 0.1f);
            mDroneLocation = gps == null ? null : gps.lastKnownLocation();
            mLocationText.setText(
                    mDroneLocation == null ? getString(R.string.no_value) : LocationFormatter.format(mDroneLocation));
            updateDistance();
        });

        groundSdk().getFacility(UserLocation.class, userLocation -> {
            mUserLocation = userLocation == null ? null : userLocation.lastKnownLocation();
            updateDistance();
        });

        mDrone.getInstrument(Altimeter.class, altimeter -> {
            if (altimeter != null) {
                float takeOffAltitude = (float) altimeter.getTakeOffRelativeAltitude();
                mAltimeterView.setTakeOffAltitude(takeOffAltitude);
                OptionalDouble groundAltitude = altimeter.getGroundRelativeAltitude();
                if (groundAltitude.isAvailable()) {
                    mAltimeterView.setGroundAltitude((float) groundAltitude.getValue());
                } else {
                    mAltimeterView.setGroundAltitude(takeOffAltitude);
                }
            } else {
                mAltimeterView.setTakeOffAltitude(0);
                mAltimeterView.setGroundAltitude(0);
            }
        });

        mDrone.getInstrument(Compass.class, compass -> {
            if (compass != null) {
                mHeadingView.setHeading((float) compass.getHeading());
            }
        });

        mDrone.getInstrument(BatteryInfo.class, batteryInfo -> {
            if (batteryInfo != null) {
                mBatteryLevelText.setText(getString(R.string.battery_level_format, batteryInfo.getBatteryLevel()));
                mBatteryLevelText.setVisibility(View.VISIBLE);
                mBatteryLevelIcon.setVisibility(View.VISIBLE);
            } else {
                mBatteryLevelText.setVisibility(View.GONE);
                mBatteryLevelIcon.setVisibility(View.GONE);
            }
        });

        mDrone.getPeripheral(DevToolbox.class, devToolbox -> {
            if (devToolbox != null) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                mDebugSettingsList.updateDebugSettings(devToolbox.getDebugSettings());
                mDrawerGrip.setVisibility(View.VISIBLE);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mDrawerGrip.setVisibility(View.GONE);
            }
        });

        mDrone.getPeripheral(MainCamera.class, camera -> {
            if (camera != null) {
                CameraZoom zoom = camera.zoom();
                if (zoom != null) {
                    mZoomLevelView.setVisibility(View.VISIBLE);
                    mZoomLevelView.setZoomLevel(zoom.getCurrentLevel(), zoom.getMaxLossLessLevel(),
                            zoom.getMaxLossyLevel())
                                  .setListener(level -> zoom.control(CameraZoom.ControlMode.LEVEL, level))
                                  .setEnabled(zoom.isAvailable());
                    mZoomVelocityView.setVisibility(View.VISIBLE);
                    mZoomVelocityView.setMaxZoomSpeed(zoom.maxSpeed().getValue())
                                     .setListener(velocity -> zoom.control(CameraZoom.ControlMode.VELOCITY, velocity))
                                     .setEnabled(zoom.isAvailable());
                }
            }
        });

        if (mIsTablet && mDrone.getPeripheral(DevToolbox.class) != null) {
            mDrawerLayout.openDrawer(GravityCompat.END);
            mDrawerLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {

                        @Override
                        public void onGlobalLayout() {
                            mDrawerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            mResizableContent.setPivotX(0);
                            mResizableContent.getLayoutParams().width = Math.round(
                                    mContent.getWidth() - mDrawer.getWidth());
                            mResizableContent.requestLayout();

                        }
                    });
        }

        mDrone.getPeripheral(StreamServer.class, streamServer -> {
            if (streamServer != null) {
                streamServer.enableStreaming(true);
                streamServer.live(cameraLive -> {
                            if (cameraLive != null) {
                                mStreamView.setStream(cameraLive);
                                cameraLive.play();
                            }
                        }
                );
            }
        });

        mUserEmergencyBtn.setOnClickListener(v -> mPilotingItf.emergencyCutOut());

        mTakeOffLandBtn.setOnClickListener(v -> mPilotingItf.smartTakeOffLand());

        mReturnHomeBtn.setOnClickListener(v -> {
            if (mReturnHomeItf != null) {
                Activable.State state = mReturnHomeItf.getState();
                if (state == Activable.State.ACTIVE) {
                    mReturnHomeItf.deactivate();
                } else if (state == Activable.State.IDLE) {
                    mReturnHomeItf.activate();
                }
            }
        });

        mLookAtBtn.setOnClickListener(v -> {
            if (mLookAtItf != null) {
                Activable.State state = mLookAtItf.getState();
                if (state == Activable.State.ACTIVE) {
                    mLookAtItf.deactivate();
                } else if (state == Activable.State.IDLE) {
                    mLookAtItf.activate();
                }
            }
        });

        mOverlayVisibilityBtn.setOnCheckedChangeListener((buttonView, isChecked) -> setOverlayVisibility(isChecked));

        mAnimationBtn.setOnClickListener(v -> {
            if (mAnimationConfig != null) {
                mAnimationItf.startAnimation(mAnimationConfig);
            } else {
                showAnimationChoiceDialog();
            }
        });

        mAnimationBtn.setOnLongClickListener(v -> {
            showAnimationChoiceDialog();
            return true;
        });

        mRollPitchJoystick.setListener((joystick, percentX, percentY) -> {
            int roll = (int) (percentX * 100);
            int pitch = (int) (-percentY * 100);
            if (mLookAtItf != null && mLookAtItf.getState() == Activable.State.ACTIVE) {
                mLookAtItf.setRoll(roll);
                mLookAtItf.setPitch(pitch);
            } else if (mPointOfInterestPilotingItf != null && mPointOfInterestPilotingItf.getState() == Activable.State.ACTIVE) {
                mPointOfInterestPilotingItf.setRoll(roll);
                mPointOfInterestPilotingItf.setPitch(pitch);
            } else {
                mPilotingItf.setRoll(roll);
                mPilotingItf.setPitch(pitch);
            }
        });

        mYawGazJoystick.setListener((joystick, percentX, percentY) -> {
            int yaw = (int) (percentX * 100);
            int speed = (int) (percentY * 100);
            if (mLookAtItf != null && mLookAtItf.getState() == Activable.State.ACTIVE) {
                mLookAtItf.setVerticalSpeed(speed);
            } else if (mPointOfInterestPilotingItf != null && mPointOfInterestPilotingItf.getState() == Activable.State.ACTIVE) {
                mPointOfInterestPilotingItf.setVerticalSpeed(speed);
            } else {
                mPilotingItf.setYawRotationSpeed(yaw);
                mPilotingItf.setVerticalSpeed(speed);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mStreamView != null) {
            mStreamView.setStream(null);
        }
        super.onDestroy();
    }

    private void updateDistance() {
        boolean distanceAvailable = mDroneLocation != null && mUserLocation != null;
        mDistanceText.setText(
                distanceAvailable ? getString(R.string.distance_format, mDroneLocation.distanceTo(mUserLocation)) :
                        getString(R.string.no_value));
    }

    private void setOverlayVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mRollPitchJoystick.setVisibility(visibility);
        mYawGazJoystick.setVisibility(visibility);
        mAltimeterView.setVisibility(visibility);
        mAttitudeView.setVisibility(visibility);
    }

    private void showAnimationChoiceDialog() {
        PickAnimationDialog.newInstance(mDrone.getUid()).show(getSupportFragmentManager(), null);
    }

    @Override
    public void onAnimationTypeAcquired(@NonNull Animation.Type animationType) {
        Animation.Config config = Animations.defaultConfigFor(animationType);
        if (config != null) {
            mAnimationConfig = config;
        } else if (animationType == Animation.Type.FLIP) {
            new PickFlipDirectionDialog().show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onFlipDirectionAcquired(@NonNull Flip.Direction direction) {
        mAnimationConfig = new Flip.Config(direction);
    }

    private final DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            if (mIsTablet) {
                // on tablets, change the content size
                mResizableContent.setPivotX(0);
                mResizableContent.getLayoutParams().width = Math.round(
                        mContent.getWidth() - (mDrawer.getWidth() * slideOffset));
                mResizableContent.requestLayout();
            } else {
                // on phones, the drawer opens over the content, only change the drawer grip horizontal position
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(mDrawerGrip,
                        "x", mContent.getWidth() - (mDrawer.getWidth() * slideOffset) - mDrawerGrip.getWidth());
                animation2.setDuration(0);
                animation2.start();
            }
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    };
}
