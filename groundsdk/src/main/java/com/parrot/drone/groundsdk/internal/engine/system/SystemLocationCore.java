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

package com.parrot.drone.groundsdk.internal.engine.system;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.parrot.drone.groundsdk.internal.Logging;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.android.gms.common.ConnectionResult.SUCCESS;

/**
 * Implementation class for {@code SystemLocation} monitoring utility.
 * <p>
 * It uses fused location provider from Google Play Services as default provider. It combines NETWORK
 * and GPS providers for better results. If Play Services are not available, we use {@link LocationManager} with
 * GPS provider.<br>
 * However, it is possible to disable the NETWORK provider when necessary. In this case, {@link LocationManager} is used
 * with only the GPS provider.
 * <p>
 * Use case (when the drone is flying):
 * a. Default usage: disable NETWORK provider, because it uses WIFI which creates perturbations on the drone video
 * stream.
 * b. Follow me: enable NETWORK provider to increase accuracy.
 */
public final class SystemLocationCore implements SystemLocation {

    /** Location type, indicating which location API is used. */
    private enum LocationType {
        /** No location API in use. */
        STOPPED,

        /** Android location API, using only GPS provider. */
        GPS_ONLY,

        /** Google Play Services location API, using both GPS and NETWORK provider. */
        FUSED,
    }

    /**
     * Default preferred time interval between updates, in milliseconds.
     * GPS refresh rate seems to be blocked at 1 Hz minimum on most devices.
     * We use 500 ms because we want to avoid "dropping" a location.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_PREFERRED_TIME_INTERVAL = 500;

    /** Default fastest time interval between updates, in milliseconds. */
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_FASTEST_TIME_INTERVAL = 100;

    /** Default minimal distance the location should change between updates, in meters. */
    @SuppressWarnings("WeakerAccess")
    public static final double DEFAULT_MIN_SPACE_INTERVAL = 0;

    /**
     * Creates a {@code SystemLocationCore}.
     *
     * @param context               android application context
     * @param preferredTimeInterval preferred time interval between two location change notifications, in milliseconds
     * @param fastestTimeInterval   fastest time interval between two location change notifications, in milliseconds
     * @param minSpaceInterval      minimal distance the location should change between two notifications, in meters
     *
     * @return a new {@code SystemLocationCore} instance if the device supports one or more methods of reporting current
     *         location, otherwise {@code null}
     */
    @Nullable
    public static SystemLocationCore create(@NonNull Context context, int preferredTimeInterval,
                                            int fastestTimeInterval, double minSpaceInterval) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
            FusedLocationProviderClient fusedLocationClient = null;
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == SUCCESS) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            }
            return new SystemLocationCore(context, (LocationManager) context.getSystemService(
                    Context.LOCATION_SERVICE), fusedLocationClient, preferredTimeInterval, fastestTimeInterval,
                    minSpaceInterval);
        }
        return null;
    }

    /**
     * Creates a {@code SystemLocationCore} with default {@link #DEFAULT_PREFERRED_TIME_INTERVAL} and
     * {@link #DEFAULT_MIN_SPACE_INTERVAL} parameters.
     *
     * @param context android application context
     *
     * @return a new {@code SystemLocationCore} instance if the application has been granted the appropriate
     *         {@link Manifest.permission#ACCESS_FINE_LOCATION} permission, otherwise {@code null}
     */
    @Nullable
    public static SystemLocationCore create(@NonNull Context context) {
        return create(context, DEFAULT_PREFERRED_TIME_INTERVAL, DEFAULT_FASTEST_TIME_INTERVAL,
                DEFAULT_MIN_SPACE_INTERVAL);
    }

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** All registered monitors of this utility. */
    @NonNull
    private final Set<SystemLocation.Monitor> mMonitors;

    /** Android location manager. */
    @NonNull
    private final LocationManager mLocationManager;

    /** Fused location provider client using Google Play Services location API. */
    @Nullable
    private final FusedLocationProviderClient mFusedLocationClient;

    /** Set of tokens disallowing wifi usage. */
    private final Set<Object> mWifiDenialTokens;

    /** Set of tokens forcing wifi usage. */
    private final Set<Object> mWifiEnforcementTokens;

    /** Preferred time interval between two location updates, in milliseconds. */
    private final int mPreferredTimeInterval;

    /** Fastest time interval between two location change notifications, in milliseconds. */
    private final int mFastestTimeInterval;

    /** Minimum distance the location should change to be considered an update, in meters. */
    private final double mMinSpaceInterval;

    /** The current location type. */
    @NonNull
    private LocationType mLocationType;

    /** Latest known geographical location. */
    @Nullable
    private Location mLocation;

    /** Latest known altitude above mean sea level. */
    private double mLatestAltitude;

    /** Difference between the WGS-84 earth ellipsoid and mean sea level (geoid). */
    private double mGeoidalSeparation;

    /** {@code true} when system location is enabled and permission is granted. */
    private boolean mAuthorized;

    /** Keeps track of active monitors. */
    private int mActiveMonitors;

    /**
     * Constructor.
     *
     * @param context               android application context
     * @param locationManager       android location manager
     * @param fusedLocationClient   fused location provider client from Google Play Services
     * @param preferredTimeInterval preferred time interval between two location updates, in milliseconds
     * @param fastestTimeInterval   fastest time interval between two location change notifications, in milliseconds
     * @param minSpaceInterval      minimal distance the location should change between two updates, in meters
     */
    @VisibleForTesting
    SystemLocationCore(@NonNull Context context, @NonNull LocationManager locationManager,
                       @Nullable FusedLocationProviderClient fusedLocationClient, int preferredTimeInterval,
                       int fastestTimeInterval, double minSpaceInterval) {
        mContext = context;
        mLocationManager = locationManager;
        mFusedLocationClient = fusedLocationClient;
        mPreferredTimeInterval = preferredTimeInterval;
        mFastestTimeInterval = fastestTimeInterval;
        mMinSpaceInterval = minSpaceInterval;
        mLocationType = LocationType.STOPPED;
        mMonitors = new HashSet<>();
        mWifiDenialTokens = new HashSet<>();
        mWifiEnforcementTokens = new HashSet<>();
    }

    @Override
    public void monitorWith(@NonNull Monitor monitor) {
        if (mMonitors.add(monitor) && !(monitor instanceof Monitor.Passive)) {
            mActiveMonitors++;
            onMonitoringConditionsChanged();
        }
    }

    @Override
    public void disposeMonitor(@NonNull Monitor monitor) {
        if (mMonitors.remove(monitor) && !(monitor instanceof Monitor.Passive)) {
            mActiveMonitors--;
            onMonitoringConditionsChanged();
        }
    }

    @Nullable
    @Override
    public Location lastKnownLocation() {
        return mLocation;
    }

    @Override
    public boolean isAuthorized() {
        return mAuthorized;
    }

    @Override
    public void restartLocationUpdates() {
        if (mActiveMonitors > 0 && mLocationType == LocationType.STOPPED) {
            startMonitoring();
        }
    }

    @Override
    public void enforceWifiUsage(@NonNull Object token) {
        if (mWifiEnforcementTokens.add(token)) {
            onMonitoringConditionsChanged();
        }
    }

    @Override
    public void revokeWifiUsageEnforcement(@NonNull Object token) {
        if (mWifiEnforcementTokens.remove(token)) {
            onMonitoringConditionsChanged();
        }
    }

    @Override
    public void denyWifiUsage(@NonNull Object token) {
        if (mWifiDenialTokens.add(token)) {
            onMonitoringConditionsChanged();
        }
    }

    @Override
    public void revokeWifiUsageDenial(@NonNull Object token) {
        if (mWifiDenialTokens.remove(token)) {
            onMonitoringConditionsChanged();
        }
    }

    @Override
    public void requestOneLocation() {
        // this bypasses the active monitor check, monitoring will be started without any active monitor.
        // Once a location is obtained, monitoring will stop automatically if there are still no active monitors.
        startMonitoring();
    }

    /**
     * Called when any condition, such as the number of {@link #mActiveMonitors active monitors}, or current WIFI
     * usage restrictions ({@link #mWifiDenialTokens denials} or {@link #mWifiEnforcementTokens enforcements}, that
     * have direct impact on how monitoring should be performed, changes.
     * <p>
     * This method start or stops monitoring as appropriate depending on such conditions.
     */
    private void onMonitoringConditionsChanged() {
        if (mActiveMonitors > 0) {
            startMonitoring();
        } else {
            stopMonitoring();
        }
    }

    /**
     * Checks if {@link Manifest.permission#ACCESS_FINE_LOCATION} permission is granted and system location is enabled
     * (either GPS or NETWORK).<br>
     * This will notify any monitor in case of change.
     *
     * @return {@code true} if fine location permission is granted and system location is enabled
     */
    private boolean checkAuthorization() {
        boolean authorized = mContext.checkCallingOrSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                             (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                              mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (mAuthorized != authorized) {
            mAuthorized = authorized;
            dispatchNotification();
        }
        return authorized;
    }


    /**
     * Starts monitoring location.
     * <p>
     * If all authorizations are granted, this method ensures monitoring is started using the appropriate provider(s)
     * depending on restrictions currently imposed by {@link #mWifiEnforcementTokens} and {@link #mWifiDenialTokens}.
     * <p>
     * This method may stop and restart monitoring as appropriate to enforce such current restrictions. It does nothing
     * in case monitoring is already starting and fulfills those restrictions.
     */
    @SuppressLint("MissingPermission")
    private void startMonitoring() {
        if (!checkAuthorization()) {
            return;
        }
        if ((mWifiEnforcementTokens.isEmpty() && !mWifiDenialTokens.isEmpty()) || mFusedLocationClient == null) {
            if (mLocationType != LocationType.GPS_ONLY) {
                stopMonitoring();
                startGpsMonitoring();
            }
        } else if (mLocationType == LocationType.GPS_ONLY || mLocationType == LocationType.STOPPED) {
            stopMonitoring();
            startFusedMonitoring();
        }
    }

    /**
     * Starts GPS request using the system location API, if GPS location is enabled.
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    @SuppressLint("MissingPermission")
    private void startGpsMonitoring() {
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ULog.i(Logging.TAG_MONITOR, "Start monitoring device location [GPS only]");
            mLocationType = LocationType.GPS_ONLY;
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mPreferredTimeInterval,
                    (float) mMinSpaceInterval, mLocationListener);
            mLocationManager.addNmeaListener(mNmeaListener);
        } else {
            ULog.w(Logging.TAG_MONITOR, "Could not start location monitoring: no GPS provider");
        }
    }

    /**
     * Starts location request using any provider. It will use fused location from Google Play Services if available,
     * otherwise it does nothing.
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    @SuppressLint("MissingPermission")
    private void startFusedMonitoring() {
        if (mFusedLocationClient != null) {
            ULog.i(Logging.TAG_MONITOR, "Start monitoring device location [FUSED]");
            mLocationType = LocationType.FUSED;
            mFusedLocationClient.requestLocationUpdates(
                    new LocationRequest().setInterval(mPreferredTimeInterval).setFastestInterval(mFastestTimeInterval)
                                         .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                    mLocationCallback, null);
            mLocationManager.addNmeaListener(mNmeaListener);
        }
    }

    /**
     * Stops monitoring location if started.
     */
    private void stopMonitoring() {
        switch (mLocationType) {
            case STOPPED:
                break;
            case GPS_ONLY:
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.removeNmeaListener(mNmeaListener);
                ULog.i(Logging.TAG_MONITOR, "Stop monitoring device location [GPS]");
                break;
            case FUSED:
                if (mFusedLocationClient != null) {
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    mLocationManager.removeNmeaListener(mNmeaListener);
                    ULog.i(Logging.TAG_MONITOR, "Stop monitoring device location [FUSED]");
                }
                break;
        }
        mLocationType = LocationType.STOPPED;
    }

    /**
     * Dispatches a change notification to all registered monitors.
     */
    private void dispatchNotification() {
        for (Monitor monitor : mMonitors) {
            if (mLocation != null) {
                monitor.onLocationChanged(mLocation);
            }
            monitor.onAuthorizationChanged(mAuthorized);
        }
    }

    /**
     * Called back by location service when a new location is available.
     *
     * @param location the new location
     */
    private void onNewLocation(@Nullable Location location) {
        if (ULog.d(Logging.TAG_MONITOR)) {
            ULog.d(Logging.TAG_MONITOR, "New location detected: " + location);
        }
        mLocation = location;
        if (mLocation != null) {
            // Sometimes location retrieved has no altitude, in this case we return latest altitude retrieved, adjusted
            // with geoidal separation in order to get altitude above mean sea level
            if (mLocation.hasAltitude()) {
                mLatestAltitude = mLocation.getAltitude() - mGeoidalSeparation;
            }
            mLocation.setAltitude(mLatestAltitude);
        }
        dispatchNotification();
        if (mActiveMonitors == 0) {
            stopMonitoring();
        }
    }

    /** Listens to location change notifications from system location API. */
    private final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            onNewLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /** Listens to location change notifications from Google Play Services location API. */
    private final LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            onNewLocation(locationResult.getLastLocation());
        }
    };

    /** Listens to NMEA messages. */
    private final OnNmeaMessageListener mNmeaListener = new OnNmeaMessageListener() {

        /** Pattern used to retrieve geoidal separation from NMEA messages. */
        private final Pattern mPattern = Pattern.compile("^\\$..(GGA|GNS),([^,]*,){10}([^,]+)");

        @Override
        public void onNmeaMessage(String message, long timestamp) {
            if (mLocation != null) {
                Matcher matcher = mPattern.matcher(message);
                if (matcher.find()) {
                    String separationField = matcher.group(3);
                    if (separationField != null) {
                        try {
                            // Geoidal separation is very stable and allows to calculate altitude above mean sea level
                            mGeoidalSeparation = Double.parseDouble(separationField);
                        } catch (NumberFormatException e) {
                            ULog.w(Logging.TAG_MONITOR, "Invalid geoidal separation format: " + separationField);
                        }
                    }
                }
            }
        }
    };
}
