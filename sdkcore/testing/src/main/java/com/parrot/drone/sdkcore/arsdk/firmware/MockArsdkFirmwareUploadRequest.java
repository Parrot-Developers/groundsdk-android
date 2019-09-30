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

package com.parrot.drone.sdkcore.arsdk.firmware;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@SuppressWarnings("JavaDoc")
public class MockArsdkFirmwareUploadRequest extends ArsdkRequest {

    @Nullable
    private ArsdkFirmwareUploadRequest.Listener mListener;

    @Nullable
    private String mDestPath;

    public MockArsdkFirmwareUploadRequest setListener(@NonNull ArsdkFirmwareUploadRequest.Listener listener) {
        mListener = listener;
        return this;
    }

    public MockArsdkFirmwareUploadRequest setDestPath(@Nullable String destPath) {
        mDestPath = destPath;
        return this;
    }

    @Nullable
    public String getDestPath() {
        return mDestPath;
    }

    @Override
    public void cancel() {
        if (mListener != null) {
            mListener.onRequestComplete(ArsdkFirmwareUploadRequest.STATUS_CANCELED);
            mListener = null;
        }
    }

    public void mockCompletion(@ArsdkFirmwareUploadRequest.Status int status) {
        consumeListener().onRequestComplete(status);
    }

    public void mockProgress(float progress) {
        useListener().onRequestProgress(progress);
    }

    @NonNull
    private ArsdkFirmwareUploadRequest.Listener useListener() {
        assertThat(mListener, notNullValue());
        assert mListener != null;
        return mListener;
    }

    @NonNull
    private ArsdkFirmwareUploadRequest.Listener consumeListener() {
        ArsdkFirmwareUploadRequest.Listener listener = useListener();
        mListener = null;
        return listener;
    }
}
