/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpService;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpServicesClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpCertificateClient;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.CertificateUploader;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AnafiCertificateUploaderTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private CertificateUploader mCertificateUploader;

    private int mChangeCnt;

    @Mock
    private HttpCertificateClient mMockCertificateClient;

    @Mock
    private HttpServicesClient mMockActiveModuleClient;

    @Captor
    private ArgumentCaptor<HttpRequest.StatusCallback> mStatusCallbackCaptor;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<List<HttpService>>> mModuleListCallbackCaptor;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupDrone();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupDrone();
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        super.teardown();
    }

    private void setupDrone() {
        doReturn((HttpRequest) () -> {}).when(mMockCertificateClient).uploadCertificate(any(), any());
        doReturn((HttpRequest) () -> {}).when(mMockActiveModuleClient).listModules(any());

        MockHttpSession.registerOnly(mMockCertificateClient, mMockActiveModuleClient);

        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mCertificateUploader = mDrone.getPeripheralStore().get(mMockSession, CertificateUploader.class);
        mDrone.getPeripheralStore().registerObserver(CertificateUploader.class, () -> {
            mCertificateUploader = mDrone.getPeripheralStore().get(mMockSession, CertificateUploader.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mCertificateUploader, nullValue());

        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient).listModules(mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200,
                Collections.singletonList(HttpService.CREDENTIAL));

        assertThat(mChangeCnt, is(1));
        assertThat(mCertificateUploader, notNullValue());

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mCertificateUploader, nullValue());

        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient, times(2)).listModules(
                mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200,
                Collections.emptyList());

        assertThat(mChangeCnt, is(2));
        assertThat(mCertificateUploader, nullValue());

        disconnectDrone(mDrone, 1);

        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient, times(3)).listModules(
                mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400, null);

        assertThat(mChangeCnt, is(2));
        assertThat(mCertificateUploader, nullValue());

        disconnectDrone(mDrone, 1);
    }

    @Test
    public void testUploadSuccess() {
        File mockFile = mock(File.class);
        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient).listModules(mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200,
                Collections.singletonList(HttpService.CREDENTIAL));

        assertThat(mChangeCnt, is(1));

        assertThat(mCertificateUploader.isUploading(), is(false));

        mCertificateUploader.upload(mockFile);
        verify(mMockCertificateClient).uploadCertificate(eq(mockFile), mStatusCallbackCaptor.capture());

        assertThat(mStatusCallbackCaptor.getValue(), notNullValue());

        assertThat(mCertificateUploader.isUploading(), is(true));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.NONE));

        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.SUCCESS));
        assertThat(mCertificateUploader.isUploading(), is(false));
    }

    @Test
    public void testUploadError() {
        File mockFile = mock(File.class);
        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient).listModules(mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200,
                Collections.singletonList(HttpService.CREDENTIAL));

        assertThat(mChangeCnt, is(1));

        assertThat(mCertificateUploader.isUploading(), is(false));

        mCertificateUploader.upload(mockFile);
        verify(mMockCertificateClient).uploadCertificate(eq(mockFile), mStatusCallbackCaptor.capture());

        assertThat(mStatusCallbackCaptor.getValue(), notNullValue());

        assertThat(mCertificateUploader.isUploading(), is(true));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.NONE));

        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400);
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.FAILED));
        assertThat(mCertificateUploader.isUploading(), is(false));
    }

    @Test
    public void testCancelUpload() {
        Cancelable request = mock(HttpRequest.class);
        doReturn(request).when(mMockCertificateClient).uploadCertificate(any(), any());

        File mockFile = mock(File.class);
        connectDrone(mDrone, 1);

        verify(mMockActiveModuleClient).listModules(mModuleListCallbackCaptor.capture());

        mModuleListCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200,
                Collections.singletonList(HttpService.CREDENTIAL));

        assertThat(mChangeCnt, is(1));
        assertThat(mCertificateUploader.isUploading(), is(false));

        mCertificateUploader.upload(mockFile);

        verify(mMockCertificateClient).uploadCertificate(eq(mockFile), mStatusCallbackCaptor.capture());
        assertThat(mChangeCnt, is(2));
        assertThat(mStatusCallbackCaptor.getValue(), notNullValue());
        assertThat(mCertificateUploader.isUploading(), is(true));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.NONE));

        mCertificateUploader.cancel();

        verify(request).cancel();

        // mock canceled
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(mChangeCnt, is(3));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.FAILED));
        assertThat(mCertificateUploader.isUploading(), is(false));
    }
}
