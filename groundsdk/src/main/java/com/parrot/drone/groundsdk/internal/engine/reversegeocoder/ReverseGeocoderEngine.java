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

package com.parrot.drone.groundsdk.internal.engine.reversegeocoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.facility.ReverseGeocoderCore;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.utility.ReverseGeocoderUtility;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_ENGINE;

/** Engine that provides reverse geocoding service. */
public class ReverseGeocoderEngine extends EngineBase {

    /** Reverse geocoder store shared preferences file name. */
    private static final String SHARED_PREF_NAME = "reverse_geocoder";

    /** Storage key for the latitude. */
    private static final String LATITUDE_KEY = "latitude";

    /** Storage key for the longitude. */
    private static final String LONGITUDE_KEY = "longitude";

    /** Storage key for the address. */
    private static final String ADDRESS_KEY = "address";

    /** Storage key for the latest address update time. */
    private static final String UPDATE_TIME_KEY = "update_time";

    /** Storage key for the dirty flag. */
    private static final String DIRTY_KEY = "dirty";

    /** Dummy latitude and longitude value. */
    private static final int INVALID_LOCATION = 500;

    /** Minimal distance between location retrieved that triggers reverse geocoding, in meters. */
    private static final int MIN_DISTANCE_TO_REVERSE_GEOCODE = 3000; // 3 km

    /** Period between location requests, in milliseconds. */
    private static final long LOCATION_REQUEST_PERIOD = TimeUnit.MINUTES.toMillis(10);

    /** Delay before executing reverse geocoding request after internet becomes available, in milliseconds. */
    private static final long REVERSE_GEOCODING_REQUEST_DELAY = TimeUnit.MINUTES.toMillis(1);


    /** ReverseGeocoder facility for which this object is the backend. */
    @NonNull
    private final ReverseGeocoderCore mReverseGeocoder;

    /** ReverseGeocoder utility for which this object is the backend. */
    @NonNull
    private final ReverseGeocoderUtilityCore mReverseGeocoderUtility;

    /** Shared preferences where reverse geocoder data are stored. */
    @NonNull
    private final SharedPreferences mSharedPreferences;

    /** JSon serializer. */
    @NonNull
    private final Gson mGson;

    /** Latest reverse geocoding task. */
    @Nullable
    private Task<Address> mReverseGeocodingTask;

    /** System location utility. */
    @Nullable
    private SystemLocation mSystemLocation;

    /** Latest latitude returned by system location. */
    private double mLatitude;

    /** Latest longitude returned by system location. */
    private double mLongitude;

    /** Latest address obtained from reverse geocoding system location. */
    private Address mAddress;

    /** Dirty flag indicating that a new location has been retrieved and reverse geocoding should be performed. */
    private boolean mDirty;

    /** {@code true} when internet connectivity is known to be available, otherwise {@code false}. */
    private boolean mInternetAvailable;

    /** {@code true} when internet has been available at least once, otherwise {@code false}. */
    private boolean mHasInternetAlreadyBeenAvailable;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public ReverseGeocoderEngine(@NonNull Controller controller) {
        super(controller);
        mReverseGeocoder = new ReverseGeocoderCore(getFacilityPublisher());
        mReverseGeocoderUtility = new ReverseGeocoderUtilityCore();
        mSharedPreferences = getContext().getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mGson = new Gson();
        String defaultCountryCode = GroundSdkConfig.get().getReverseGeocoderDefaultCountryCode();
        if (defaultCountryCode != null) {
            mAddress = new Address(Locale.getDefault());
            mAddress.setCountryCode(defaultCountryCode);
        } else {
            loadPersistedData();
        }
        publishUtility(ReverseGeocoderUtility.class, mReverseGeocoderUtility);
    }

    /**
     * Loads reverse geocoder data, from persistent storage.
     */
    private void loadPersistedData() {
        mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong(LATITUDE_KEY, INVALID_LOCATION));
        mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong(LONGITUDE_KEY, INVALID_LOCATION));
        mDirty = mSharedPreferences.getBoolean(DIRTY_KEY, false);
        String addressJson = mSharedPreferences.getString(ADDRESS_KEY, null);
        try {
            mAddress = mGson.fromJson(addressJson, Address.class);
        } catch (JsonSyntaxException e) {
            ULog.w(TAG_ENGINE, "Could not parse address", e);
        }
    }

    @Override
    protected void onStart() {
        mSystemLocation = getUtility(SystemLocation.class);
        if (mSystemLocation != null) {
            mSystemLocation.monitorWith(mLocationMonitor);
            mLocationRequestRunnable.run();
            getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);
            mReverseGeocoder.publish();
            updateFacilityAndUtility();
        }
    }

    @Override
    protected void onStopRequested() {
        if (mSystemLocation != null) {
            mSystemLocation.disposeMonitor(mLocationMonitor);
            Executor.unschedule(mLocationRequestRunnable);
            Executor.unschedule(mReverseGeocodingSchedulingRunnable);
            if (mReverseGeocodingTask != null) {
                mReverseGeocodingTask.cancel();
            }
            getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);
            mReverseGeocoder.unpublish();
        }
        acknowledgeStopRequest();
    }

    /** Schedules location request for execution. */
    private void scheduleLocationRequest() {
        Executor.schedule(mLocationRequestRunnable, LOCATION_REQUEST_PERIOD);
    }

    /** Runnable responsible for requesting location, scheduled periodically. */
    @NonNull
    private final Runnable mLocationRequestRunnable = () -> {
        if (mSystemLocation != null) {
            mSystemLocation.requestOneLocation();
            scheduleLocationRequest();
        }
    };

    /**
     * Schedules any pending reverse geocoding request for execution after the given delay.
     *
     * @param delayMillis delay, in milliseconds, before the request is executed
     */
    private void scheduleReverseGeocoding(long delayMillis) {
        if (mDirty && mInternetAvailable) {
            Executor.unschedule(mReverseGeocodingSchedulingRunnable);
            Executor.schedule(mReverseGeocodingSchedulingRunnable, delayMillis);
        }
    }

    /**
     * Called when a new address has been reverse-geocoded.
     *
     * @param address new address obtained from reverse geocoding
     */
    private void onAddressReady(@NonNull Address address) {
        mAddress = address;
        mDirty = false;
        mSharedPreferences.edit()
                          .putString(ADDRESS_KEY, mGson.toJson(address))
                          .putLong(UPDATE_TIME_KEY, System.currentTimeMillis())
                          .putBoolean(DIRTY_KEY, false)
                          .apply();
        updateFacilityAndUtility();
    }

    /** Updates reverse geocoder facility and utility. */
    private void updateFacilityAndUtility() {
        mReverseGeocoder.updateAddress(mAddress).notifyUpdated();
        mReverseGeocoderUtility.updateAddress(mAddress);
    }

    /** Geocoder handling reverse geocoding. */
    @NonNull
    private final Geocoder mGeocoder = new Geocoder(getContext(), Locale.US);

    /** Runnable responsible for scheduling reverse geocoding. */
    @NonNull
    private final Runnable mReverseGeocodingSchedulingRunnable = () -> {
        if (mReverseGeocodingTask != null) {
            mReverseGeocodingTask.cancel();
        }
        mReverseGeocodingTask = Executor.runInBackground(() -> {
            Address address = null;
            if (mInternetAvailable) {
                List<Address> addresses = mGeocoder.getFromLocation(mLatitude, mLongitude, 1);
                address = addresses == null || addresses.isEmpty() ? null : addresses.get(0);
            }
            return address;
        }).whenComplete((address, error, canceled) -> {
            if (error != null) {
                ULog.w(TAG_ENGINE, "Reverse geocoding failed", error);
            } else if (address != null) {
                onAddressReady(address);
            }
        });
    };

    /** Listens to system location changes. */
    @NonNull
    private final SystemLocation.Monitor mLocationMonitor = new SystemLocation.Monitor.Passive() {

        /** Stores result of distance computation. */
        @NonNull
        private final float[] mDistanceResult = new float[1];

        @Override
        public void onLocationChanged(@NonNull Location location) {
            Location.distanceBetween(mLatitude, mLongitude, location.getLatitude(), location.getLongitude(),
                    mDistanceResult);
            if (mDistanceResult[0] > MIN_DISTANCE_TO_REVERSE_GEOCODE) {
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
                mDirty = true;
                mSharedPreferences.edit()
                                  .putLong(LATITUDE_KEY, Double.doubleToLongBits(mLatitude))
                                  .putLong(LONGITUDE_KEY, Double.doubleToLongBits(mLongitude))
                                  .putBoolean(DIRTY_KEY, true)
                                  .apply();
                scheduleReverseGeocoding(0);
            }
        }
    };

    /** Listens to internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = availableNow -> {
        mInternetAvailable = availableNow;
        if (availableNow) {
            // Geocoding doesn't work as soon as internet is available, we have to wait 1 min
            scheduleReverseGeocoding(mHasInternetAlreadyBeenAvailable ? REVERSE_GEOCODING_REQUEST_DELAY : 0);
            mHasInternetAlreadyBeenAvailable = true;
        } else {
            Executor.unschedule(mReverseGeocodingSchedulingRunnable);
        }
    };
}
