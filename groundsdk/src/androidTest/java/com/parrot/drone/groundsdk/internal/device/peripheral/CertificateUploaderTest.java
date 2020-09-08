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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.CertificateUploader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class CertificateUploaderTest {

    private MockComponentStore<Peripheral> mStore;

    private CertificateUploaderCore mCertificateUploaderImpl;

    private CertificateUploader mCertificateUploader;

    private Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mMockBackend = new Backend();
        mCertificateUploaderImpl = new CertificateUploaderCore(mStore, mMockBackend);
        mCertificateUploader = mStore.get(CertificateUploader.class);
        mStore.registerObserver(CertificateUploader.class, () -> {
            mComponentChangeCnt++;
            mCertificateUploader = mStore.get(CertificateUploader.class);
        });
        mComponentChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mCertificateUploader, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mCertificateUploaderImpl.publish();

        assertThat(mCertificateUploader, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mCertificateUploaderImpl.unpublish();

        assertThat(mCertificateUploader, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testIsUploading() {
        mCertificateUploaderImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCertificateUploader.isUploading(), is(false));

        // mock update uploading flag from backend
        mCertificateUploaderImpl.updateUploadingFlag(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCertificateUploader.isUploading(), is(true));

        // mock update uploading flag from backend
        mCertificateUploaderImpl.updateUploadingFlag(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCertificateUploader.isUploading(), is(false));
    }

    @Test
    public void testCompletionStatus() {
        mCertificateUploaderImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.NONE));

        // mock update completion status from backend
        mCertificateUploaderImpl.updateCompletionStatus(CertificateUploader.CompletionStatus.SUCCESS).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.SUCCESS));

        // mock update completion status from backend
        mCertificateUploaderImpl.updateCompletionStatus(CertificateUploader.CompletionStatus.FAILED)
                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.FAILED));

        // mock update completion status from backend
        mCertificateUploaderImpl.updateCompletionStatus(CertificateUploader.CompletionStatus.NONE)
                .notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCertificateUploader.getCompletionStatus(), is(CertificateUploader.CompletionStatus.NONE));
    }

    @Test
    public void testUpload() {
        File mockFile = mock(File.class);
        mCertificateUploaderImpl.publish();

        assertThat(mMockBackend.uploadFile, nullValue());

        // uploading mock file
        mCertificateUploader.upload(mockFile);

        // mock file sent to backend
        assertThat(mMockBackend.uploadFile, is(mockFile));
    }

    @Test
    public void testCancel() {
        mCertificateUploaderImpl.publish();

        assertThat(mMockBackend.canceled, is(false));

        // canceling upload
        mCertificateUploader.cancel();

        // cancel sent to backend
        assertThat(mMockBackend.canceled, is(true));
    }

    private static final class Backend implements CertificateUploaderCore.Backend {

        private File uploadFile;

        private boolean canceled;

        @Override
        public void upload(@NonNull File certificate) {
            uploadFile = certificate;
        }

        @Override
        public void cancel() {
            canceled = true;
        }
    }
}
