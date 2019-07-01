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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.FormattingStateMatcher;
import com.parrot.drone.groundsdk.MediaInfoMatcher;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureUserStorage;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiRemovableUserStorageTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private RemovableUserStorage mRemovableUserStorage;

    private int mChangeCnt;

    private RemovableUserStorage.State mExpectedState;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mRemovableUserStorage = mDrone.getPeripheralStore().get(mMockSession, RemovableUserStorage.class);
        mDrone.getPeripheralStore().registerObserver(RemovableUserStorage.class, () -> {
            mRemovableUserStorage = mDrone.getPeripheralStore().get(mMockSession, RemovableUserStorage.class);
            mChangeCnt++;

            if (mExpectedState != null) {
                // check if current state matches the expected state
                assertThat(mRemovableUserStorage, notNullValue());
                assertThat(mRemovableUserStorage.getState(), is(mExpectedState));
                mExpectedState = null;
            }
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mRemovableUserStorage, nullValue());
        assertThat(mChangeCnt, is(0));

        connectDrone(mDrone, 1);

        assertThat(mRemovableUserStorage, notNullValue());
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mRemovableUserStorage, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testState() {
        connectDrone(mDrone, 1,
                () -> mMockArsdkCore.commandReceived(1,
                        ArsdkEncoder.encodeUserStorageCapabilities(ArsdkFeatureUserStorage.Feature.toBitField(
                                ArsdkFeatureUserStorage.Feature.FORMAT_RESULT_EVT_SUPPORTED))));

        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(1));

        // Format denied
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.DENIED));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING_DENIED));
        assertThat(mChangeCnt, is(2));

        // Format success
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.SUCCESS));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING_SUCCEEDED));
        assertThat(mChangeCnt, is(3));

        // Format error
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.ERROR));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING_FAILED));
        assertThat(mChangeCnt, is(4));

        // No media detected
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.UNDETECTED,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(5));

        // Media rejected since it's to small
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.TOO_SMALL,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MEDIA_TOO_SMALL));
        assertThat(mChangeCnt, is(6));

        // Media rejected since it's too slow
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.TOO_SLOW,
                ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MEDIA_TOO_SLOW));
        assertThat(mChangeCnt, is(7));

        // Drone acts as a USB mass-storage device
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.USB_MASS_STORAGE,
                ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.USB_MASS_STORAGE));
        assertThat(mChangeCnt, is(8));

        // Mounting
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MOUNTING));
        assertThat(mChangeCnt, is(9));

        // Need format
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));
        assertThat(mChangeCnt, is(10));

        // Formatting
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMATTING, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));
        assertThat(mChangeCnt, is(11));

        // Ready
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));
        assertThat(mChangeCnt, is(12));

        // Media error
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStopMonitoring()));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.ERROR, 0, 1, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.ERROR));
        assertThat(mChangeCnt, is(13));

        // No media detected
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.UNDETECTED,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(14));
    }

    @Test
    public void testMediaInfo() {
        connectDrone(mDrone, 1);
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(1));

        // Not media info received
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mChangeCnt, is(2));

        // Receive media info
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageInfo("MediaName", 0));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("MediaName", 0));
        assertThat(mChangeCnt, is(3));

        // Capacity change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageInfo("MediaName", 100000));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("MediaName", 100000));
        assertThat(mChangeCnt, is(4));

        // Name change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageInfo("NewMediaName", 100000));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("NewMediaName", 100000));
        assertThat(mChangeCnt, is(5));

        // Same media info, no change should be notified
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageInfo("NewMediaName", 100000));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("NewMediaName", 100000));
        assertThat(mChangeCnt, is(5));

        // No media detected, media info should be reset to null
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.UNDETECTED,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 0, 0));
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mChangeCnt, is(6));
    }

    @Test
    public void testAvailableSpace() {
        connectDrone(mDrone, 1);
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(1));

        // Media ready
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mChangeCnt, is(2));

        // Receive free space available notification
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageMonitor(100L));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(100L));
        assertThat(mChangeCnt, is(3));

        // Receive same value, no change should be notified
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageMonitor(100L));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(100L));
        assertThat(mChangeCnt, is(3));

        // Receive free space available notification
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeUserStorageMonitor(0L));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(0L));
        assertThat(mChangeCnt, is(4));

        // No media detected, free space available should be reset to -1
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStopMonitoring()));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.UNDETECTED,
                        ArsdkFeatureUserStorage.FsState.UNKNOWN, 0, 1, 0));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mChangeCnt, is(5));
    }

    @Test
    public void testFormatWithFormatResultEvent() {
        connectDrone(mDrone, 1,
                () -> mMockArsdkCore.commandReceived(1,
                        ArsdkEncoder.encodeUserStorageCapabilities(ArsdkFeatureUserStorage.Feature.toBitField(
                                ArsdkFeatureUserStorage.Feature.FORMAT_WHEN_READY_ALLOWED,
                                ArsdkFeatureUserStorage.Feature.FORMAT_RESULT_EVT_SUPPORTED))));

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));

        // format needed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMATTING, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting result, success
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.SUCCESS));
        assertThat(mChangeCnt, is(4));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING_SUCCEEDED));

        // user storage ready
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mChangeCnt, is(5));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("NewName")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL, "NewName"), is(true));
        assertThat(mChangeCnt, is(6));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting and ready states before formatting result, nothing should change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMATTING, 0, 0, 0));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mChangeCnt, is(6));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting result, success
        mExpectedState = RemovableUserStorage.State.FORMATTING_SUCCEEDED;
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.SUCCESS));
        assertThat(mChangeCnt, is(8)); // 1 change for State.FORMATTING_SUCCEEDED and 1 change for State.READY
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));
        assertThat(mExpectedState, nullValue());

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(9));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting result, error
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.ERROR));
        assertThat(mChangeCnt, is(10));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING_FAILED));

        // receive format needed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(11));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(12));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // receive formatting result, denied, previous state should be restored
        mExpectedState = RemovableUserStorage.State.FORMATTING_DENIED;
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatResult(ArsdkFeatureUserStorage.FormattingResult.DENIED));
        assertThat(mChangeCnt, is(14)); // 1 change for State.FORMATTING_DENIED and 1 change for State.NEED_FORMAT
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));
        assertThat(mExpectedState, nullValue());
    }

    @Test
    public void testFormatWithoutFormatResultEvent() {
        connectDrone(mDrone, 1);

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));

        // format needed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // receive formatting state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMATTING, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // user storage ready, meaning formatting succeed
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mExpectedState = RemovableUserStorage.State.FORMATTING_SUCCEEDED; // expected transient state for format result
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mChangeCnt, is(5));
        assertThat(mExpectedState, nullValue());
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));

        // format not allowed in READY state
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL, "NewName"), is(false));
        assertThat(mChangeCnt, is(5));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));

        // format needed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(6));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(6));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // receive formatting state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMATTING, 0, 0, 0));
        assertThat(mChangeCnt, is(7));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.FORMATTING));

        // user storage need format, meaning formatting failed
        mExpectedState = RemovableUserStorage.State.FORMATTING_FAILED; // expected transient state for format result
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(9));
        assertThat(mExpectedState, nullValue());
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));
    }

    @Test
    public void testFormattingTypes() {
        connectDrone(mDrone, 1);

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));

        // format needed
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));

        // SupportedFormattingTypes not received, the simple format command should be sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));

        // SupportedFormattingTypes received, the formatWithType command should be sent
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageSupportedFormattingTypes(
                        ArsdkFeatureUserStorage.FormattingType.toBitField(
                                ArsdkFeatureUserStorage.FormattingType.QUICK)));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.userStorageFormatWithType("", ArsdkFeatureUserStorage.FormattingType.QUICK)));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.QUICK), is(true));

        // no command should be sent for unsupported formatting type
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(false));
    }

    @Test
    public void testFormattingState() {
        connectDrone(mDrone, 1);

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        // prepare to format
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.FORMAT_NEEDED, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        // request format, formatting state should be null as it's not supported
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        // updating formatting state from backend should not trigger any change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatProgress(ArsdkFeatureUserStorage.FormattingStep.PARTITIONING, 25));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        // add progress event capability
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageCapabilities(ArsdkFeatureUserStorage.Feature.toBitField(
                        ArsdkFeatureUserStorage.Feature.FORMAT_PROGRESS_EVT_SUPPORTED)));
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        // request format, formatting state should be initialized
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageFormat("")));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.formattingState(),
                FormattingStateMatcher.is(RemovableUserStorage.FormattingState.Step.PARTITIONING, 0));

        // update formatting state from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatProgress(ArsdkFeatureUserStorage.FormattingStep.PARTITIONING, 33));
        assertThat(mChangeCnt, is(4));
        assertThat(mRemovableUserStorage.formattingState(),
                FormattingStateMatcher.is(RemovableUserStorage.FormattingState.Step.PARTITIONING, 33));

        // update again formatting state from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageFormatProgress(ArsdkFeatureUserStorage.FormattingStep.CREATING_FS, 85));
        assertThat(mChangeCnt, is(5));
        assertThat(mRemovableUserStorage.formattingState(),
                FormattingStateMatcher.is(RemovableUserStorage.FormattingState.Step.CREATING_FILE_SYSTEM, 85));

        // formatting state should be reset on process completion
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.userStorageStartMonitoring(0)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeUserStorageState(ArsdkFeatureUserStorage.PhyState.AVAILABLE,
                        ArsdkFeatureUserStorage.FsState.READY, 0, 0, 0));
        assertThat(mChangeCnt, is(6));
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());
    }
}
