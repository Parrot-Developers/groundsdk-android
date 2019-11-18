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

package com.parrot.drone.groundsdkdemo.edit;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class PointOfInterestEditActivity extends GroundSdkActivityBase {

    private static final int DEFAULT_ZOOM = 17;

    @NonNull
    private final LatLng mDefaultLocation = new LatLng(48.853213, 2.349912);

    private MapView mMapView;

    private GoogleMap mMap;

    private Marker mDroneMarker;

    private Marker mCurrentPOIMarker;

    private Marker mSelectionMarker;

    private MarkerOptions mSelectionMarkerOptions;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());

        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_poi_edit);

        mMapView = findViewById(R.id.map);
        EditText altitudeEdit = findViewById(R.id.altitude_edit);
        Spinner modeSpinner = findViewById(R.id.mode_spinner);
        Button startButton = findViewById(R.id.btn_start);
        Button stopButton = findViewById(R.id.btn_stop);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(googleMap -> {
            mMap = googleMap;
            mMap.setMyLocationEnabled(true);
            updateMapWithDeviceLocation();
            mMap.setOnMapClickListener(latLng -> {
                if (mSelectionMarker != null) {
                    mSelectionMarker.remove();
                }
                mSelectionMarkerOptions = new MarkerOptions().position(latLng);
                mSelectionMarker = mMap.addMarker(mSelectionMarkerOptions);
            });
        });

        drone.getPilotingItf(PointOfInterestPilotingItf.class, poiPilotingItf -> {
            if (poiPilotingItf != null) {
                startButton.setEnabled(poiPilotingItf.getState() != Activable.State.UNAVAILABLE);
                stopButton.setEnabled(poiPilotingItf.getCurrentPointOfInterest() != null);
                updateCurrentPOI(poiPilotingItf.getCurrentPointOfInterest());
            }
        });

        drone.getInstrument(Gps.class, gps -> {
            if (gps != null) {
                updateDronePosition(gps.lastKnownLocation());
            }
        });

        PointOfInterestPilotingItf pointOfInterestPilotingItf = drone.getPilotingItf(PointOfInterestPilotingItf.class);
        if (pointOfInterestPilotingItf != null) {
            startButton.setOnClickListener(
                    v -> {
                        if (mSelectionMarkerOptions != null) {
                            LatLng position = mSelectionMarkerOptions.getPosition();
                            PointOfInterestPilotingItf.Mode mode = modeSpinner.getSelectedItemPosition() == 0
                                    ? PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL
                                    : PointOfInterestPilotingItf.Mode.FREE_GIMBAL;
                            pointOfInterestPilotingItf.start(position.latitude, position.longitude,
                                    getDoubleValue(altitudeEdit), mode);
                            if (mSelectionMarker != null) {
                                mSelectionMarker.remove();
                            }
                            mSelectionMarkerOptions = null;
                        }
                    });
            stopButton.setOnClickListener(v -> pointOfInterestPilotingItf.deactivate());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mMapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private static double getDoubleValue(EditText editText) {
        double value = 0.0;
        if (!TextUtils.isEmpty(editText.getText())) {
            try {
                value = Double.parseDouble(editText.getText().toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return value;
    }

    private void updateCurrentPOI(PointOfInterestPilotingItf.PointOfInterest currentPointOfInterest) {
        if (mMap == null) {
            return;
        }
        if (mCurrentPOIMarker != null) {
            mCurrentPOIMarker.remove();
            mCurrentPOIMarker = null;
        }
        if (currentPointOfInterest != null) {
            LatLng position = new LatLng(currentPointOfInterest.getLatitude(), currentPointOfInterest.getLongitude());
            mCurrentPOIMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Current POI")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_poi)));
        }
    }

    private void updateDronePosition(Location location) {
        if (mMap == null) {
            return;
        }
        if (location == null) {
            if (mDroneMarker != null) {
                mDroneMarker.remove();
                mDroneMarker = null;
            }
        } else {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            if (mDroneMarker == null) {
                mDroneMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_copter)));
            } else {
                mDroneMarker.setPosition(position);
            }
        }
    }

    private void updateMapWithDeviceLocation() {
        if (mMap == null) {
            return;
        }
        @SuppressLint("MissingPermission")
        Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(this, task -> {
            Location location = task.isSuccessful() ? task.getResult() : null;
            if (location != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            }
        });
    }
}
