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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class GuidedEditActivity extends GroundSdkActivityBase {

    private static final int DEFAULT_ZOOM = 17;

    @NonNull
    private final LatLng mDefaultLocation = new LatLng(48.853213, 2.349912);

    private MapView mMapView;

    private GoogleMap mMap;

    private Marker mMarker;

    private MarkerOptions mMarkerOptions;

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

        setContentView(R.layout.activity_guided_edit);

        mMapView = findViewById(R.id.map);
        EditText altitudeEdit = findViewById(R.id.altitude_edit);
        Spinner orientationSpinner = findViewById(R.id.orientation_spinner);
        EditText headingEdit = findViewById(R.id.heading_edit);
        Button moveToButton = findViewById(R.id.btn_move_to);
        EditText forwardEdit = findViewById(R.id.forward_edit);
        EditText rightEdit = findViewById(R.id.right_edit);
        EditText downwardEdit = findViewById(R.id.downward_edit);
        EditText rotationEdit = findViewById(R.id.rotation_edit);
        Button moveByButton = findViewById(R.id.btn_move_by);
        Button stopButton = findViewById(R.id.btn_stop);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(googleMap -> {
            mMap = googleMap;
            mMap.setMyLocationEnabled(true);
            updateMapWithDeviceLocation();
            mMap.setOnMapClickListener(latLng -> {
                if (mMarker != null) {
                    mMarker.remove();
                }
                mMarkerOptions = new MarkerOptions().position(latLng);
                mMarker = mMap.addMarker(mMarkerOptions);
            });
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.orientation_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orientationSpinner.setAdapter(adapter);
        orientationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                headingEdit.setEnabled(i >= 2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        GuidedPilotingItf guidedPilotingItf = drone.getPilotingItf(GuidedPilotingItf.class);
        if (guidedPilotingItf != null) {
            moveToButton.setOnClickListener(
                    v -> {
                        if (mMarkerOptions != null) {
                            LatLng position = mMarkerOptions.getPosition();
                            Orientation orientation = Orientation.NONE;
                            switch (orientationSpinner.getSelectedItemPosition()) {
                                case 0:
                                    orientation = Orientation.NONE;
                                    break;
                                case 1:
                                    orientation = Orientation.TO_TARGET;
                                    break;
                                case 2:
                                    orientation = Orientation.headingStart(getDoubleValue(headingEdit));
                                    break;
                                case 3:
                                    orientation = Orientation.headingDuring(getDoubleValue(headingEdit));
                                    break;
                            }
                            guidedPilotingItf.moveToLocation(position.latitude, position.longitude,
                                    getDoubleValue(altitudeEdit), orientation);
                        }
                    });
            moveByButton.setOnClickListener(
                    v -> guidedPilotingItf.moveToRelativePosition(getDoubleValue(forwardEdit),
                            getDoubleValue(rightEdit),
                            getDoubleValue(downwardEdit), getDoubleValue(rotationEdit)));
            stopButton.setOnClickListener(v -> guidedPilotingItf.deactivate());
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

    private void updateMapWithDeviceLocation() {
        if (mMap == null) {
            return;
        }
        @SuppressLint("MissingPermission")
        Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(this, task -> {
            boolean locationHasBeenSet = false;
            if (task.isSuccessful()) {
                Location location = task.getResult();
                if (location != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                    locationHasBeenSet = true;
                }
            }
            if (!locationHasBeenSet) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            }
        });
    }
}
