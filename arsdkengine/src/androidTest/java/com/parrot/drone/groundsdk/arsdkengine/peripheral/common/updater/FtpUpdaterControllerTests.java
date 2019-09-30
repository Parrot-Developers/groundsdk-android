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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareUploadRequest;
import com.parrot.drone.sdkcore.arsdk.firmware.MockArsdkFirmwareUploadRequest;

import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateCurrentFirmwareIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateCurrentProgressIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateIndexIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateStateIs;
import static com.parrot.drone.groundsdk.UpdateMatcher.firmwareUpdateTotalCountIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FtpUpdaterControllerTests extends ArsdkEngineTestBase {

    private static final DeviceModel MODEL = RemoteControl.Model.SKY_CONTROLLER_3;

    @SuppressWarnings("ConstantConditions")
    private static final FirmwareInfo FIRMWARE = when(mock(FirmwareInfo.class).getFirmware()).thenReturn(
            new FirmwareIdentifier(MODEL, FirmwareVersion.parse("1.0.0"))).getMock();

    private static final File FIRMWARE_FILE = new File("/tmp/firmware");

    private RemoteControlCore mRc;

    private Updater mUpdater;

    private int mChangeCnt;

    private MockArsdkFirmwareUploadRequest mRequest;

    private Queue<Runnable> mOnChangeRunnables;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Override
    public void setUp() {
        super.setUp();

        FirmwareStore mockFirmwareStore = mock(FirmwareStore.class);
        // mock applicable firmware in store
        doReturn(Collections.singletonList(FIRMWARE))
                .when(mockFirmwareStore)
                .applicableUpdatesFor(any());
        // mock firmware data
        doReturn(Task.success(FIRMWARE_FILE)).when(mockFirmwareStore).getFirmwareFile(any());

        mUtilities.registerUtility(FirmwareStore.class, mockFirmwareStore)
                  .registerUtility(FirmwareDownloader.class, mock(FirmwareDownloader.class));

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", MODEL.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRc = mRCStore.get("123");
        assert mRc != null;

        mOnChangeRunnables = new LinkedList<>();
        mUpdater = mRc.getPeripheralStore().get(mMockSession, Updater.class);
        mRc.getPeripheralStore().registerObserver(Updater.class, () -> {
            mUpdater = mRc.getPeripheralStore().get(mMockSession, Updater.class);
            mChangeCnt++;
            Runnable r = mOnChangeRunnables.poll();
            if (r != null) {
                r.run();
            }
        });

        // connect the drone
        connectRemoteControl(mRc, 1);

        assertThat(mChangeCnt, is(1));

        mRequest = spy(new MockArsdkFirmwareUploadRequest());
        mMockArsdkCore.expect(Expectation.FirmwareUpload.of(1, mRequest, FIRMWARE_FILE.getAbsolutePath()));

        // request an update
        mUpdater.updateToLatestFirmware();

        // should be uploading
        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(0)));

        mChangeCnt = 0;
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        super.teardown();
    }

    @Test
    public void testUpdateProgress() {
        // mock progress
        mRequest.mockProgress(20);

        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.UPLOADING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(20)));
    }

    @Test
    public void testUpdateSuccess() {
        // mock processing
        mRequest.mockProgress(100);

        // should be processing
        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.PROCESSING),
                firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(100)));

        // mock success
        mRequest.mockCompletion(ArsdkFirmwareUploadRequest.STATUS_OK);

        // nothing changes until disconnect
        assertThat(mChangeCnt, is(1));

        disconnectRemoteControl(mRc, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), allOf(
                firmwareUpdateStateIs(Updater.Update.State.WAITING_FOR_REBOOT),
                firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                firmwareUpdateIndexIs(1),
                firmwareUpdateTotalCountIs(1),
                firmwareUpdateCurrentProgressIs(100)));
    }

    @Test
    public void testUpdateFailure() {
        // mock failure
        mRequest.mockCompletion(ArsdkFirmwareUploadRequest.STATUS_FAILED);

        onNextChange(() -> {
            // whole update task should be canceled
            assertThat(mChangeCnt, is(1));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.FAILED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(1)));
        });

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }

    @Test
    public void testUpdateCancel() {
        onNextChange(() -> {
            // whole update task should be canceled
            assertThat(mChangeCnt, is(1));
            assertThat(mUpdater.currentUpdate(), allOf(
                    firmwareUpdateStateIs(Updater.Update.State.CANCELED),
                    firmwareUpdateCurrentFirmwareIs(FIRMWARE),
                    firmwareUpdateIndexIs(1),
                    firmwareUpdateTotalCountIs(1)));
        });

        mUpdater.cancelUpdate();

        verify(mRequest).cancel();

        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.currentUpdate(), nullValue());
    }
}
