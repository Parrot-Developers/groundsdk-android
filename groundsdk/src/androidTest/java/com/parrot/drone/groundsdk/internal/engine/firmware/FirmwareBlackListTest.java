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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.FirmwareBlacklistMatcher.isBlacklisted;
import static com.parrot.drone.groundsdk.FirmwareBlacklistMatcher.notBlacklisted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FirmwareBlackListTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private FirmwareBlackListCore mFirmwareBlackList;

    private int onChangeCnt;

    @Before
    public void setup() {
        mContext.deleteSharedPreferences(Persistence.PREF_FILE);

        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());
        mFirmwareBlackList = new FirmwareBlackListCore(new Persistence(mContext));

        mFirmwareBlackList.monitorWith(() -> onChangeCnt++);
    }

    @After
    public void teardown() {
        ApplicationStorageProvider.setInstance(null);
    }

    @Test
    public void testPredefinedBlackList() {
        // blacklisted Anafi 4K version
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "1.2.3"));

        // another blacklisted Anafi 4K version
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "4.5.6"));

        // blacklisted Anafi Thermal version
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "1.2.3"));

        // non-blacklisted versions
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_4K, "1.2.3-rc1"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_4K, "4.5.60"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_4K, "7.8.9"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_4K, "7.8.9-rc10"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_THERMAL, "1.2.3-rc2"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_THERMAL, "4.5.6"));
    }

    @Test
    public void testFetchedBlackList() {
        assertThat(onChangeCnt, is(0));

        // initial state with predefined blacklist
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "1.2.3"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "4.5.6"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_4K, "7.8.9"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "1.2.3"));
        assertThat(mFirmwareBlackList, notBlacklisted(Drone.Model.ANAFI_THERMAL, "4.5.6"));

        Set<FirmwareIdentifier> blacklist = new HashSet<>();
        FirmwareVersion firmwareVersion = FirmwareVersion.parse("4.5.6");
        assert firmwareVersion != null;
        FirmwareIdentifier firmwareIdentifier = new FirmwareIdentifier(Drone.Model.ANAFI_4K, firmwareVersion);
        blacklist.add(firmwareIdentifier);
        firmwareVersion = FirmwareVersion.parse("7.8.9");
        assert firmwareVersion != null;
        firmwareIdentifier = new FirmwareIdentifier(Drone.Model.ANAFI_4K, firmwareVersion);
        blacklist.add(firmwareIdentifier);
        firmwareVersion = FirmwareVersion.parse("4.5.6");
        assert firmwareVersion != null;
        firmwareIdentifier = new FirmwareIdentifier(Drone.Model.ANAFI_THERMAL, firmwareVersion);
        blacklist.add(firmwareIdentifier);

        mFirmwareBlackList.addToBlackList(blacklist);
        assertThat(onChangeCnt, is(1));

        // state after update
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "1.2.3"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "4.5.6"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "7.8.9"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "1.2.3"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "4.5.6"));

        // create new instance to reload blacklist from share preferences
        mFirmwareBlackList = new FirmwareBlackListCore(new Persistence(mContext));
        assertThat(onChangeCnt, is(1));

        // the updated blacklist should be loaded
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "1.2.3"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "4.5.6"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_4K, "7.8.9"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "1.2.3"));
        assertThat(mFirmwareBlackList, isBlacklisted(Drone.Model.ANAFI_THERMAL, "4.5.6"));
    }
}
