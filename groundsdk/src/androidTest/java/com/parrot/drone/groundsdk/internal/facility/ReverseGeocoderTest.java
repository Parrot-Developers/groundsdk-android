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

package com.parrot.drone.groundsdk.internal.facility;

import android.location.Address;

import com.parrot.drone.groundsdk.AddressMatcher;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.ReverseGeocoder;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ReverseGeocoderTest {

    private MockComponentStore<Facility> mStore;

    private ReverseGeocoderCore mReverseGeocoder;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mReverseGeocoder = new ReverseGeocoderCore(mStore);
    }

    @Test
    public void testPublication() {
        mReverseGeocoder.publish();
        assertThat(mReverseGeocoder, is(mStore.get(ReverseGeocoder.class)));
        mReverseGeocoder.unpublish();
        assertThat(mStore.get(ReverseGeocoder.class), nullValue());
    }

    @Test
    public void testGetAddress() {
        mReverseGeocoder.publish();
        ReverseGeocoder reverseGeocoder = mStore.get(ReverseGeocoder.class);
        assert reverseGeocoder != null;
        int[] cnt = new int[1];
        mStore.registerObserver(ReverseGeocoder.class, () -> cnt[0]++);

        // test initial value
        assertThat(cnt[0], is(0));
        assertThat(reverseGeocoder.getAddress(), nullValue());

        // update address
        Address address = new Address(Locale.getDefault());
        address.setAddressLine(0, "174 Quai de Jemmapes");
        address.setPostalCode("75010");
        address.setLocality("Paris");
        address.setCountryCode("FR");
        address.setCountryName("France");
        mReverseGeocoder.updateAddress(address).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(reverseGeocoder.getAddress(), AddressMatcher.addressIs(
                "174 Quai de Jemmapes", "75010", "Paris", "FR", "France"));
    }
}
