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

package com.parrot.drone.groundsdkdemo.info;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;

abstract class RefContent<RT> extends Content {

    @Nullable
    private RT mReferencedObject;

    RefContent(int type) {
        super(type);
    }

    @Override
    @ContentOp
    final int onContentRegistered() {
        watchRef(referencedObject -> {
            boolean existed = mReferencedObject != null;
            mReferencedObject = referencedObject;
            if (referencedObject != null) {
                if (existed) {
                    onContentChanged(mReferencedObject, false);
                    contentChanged();
                } else {
                    onContentChanged(mReferencedObject, true);
                    showContent();
                }
            } else if (existed) {
                hideContent();
                onContentUnavailable();
            }
        });
        return HIDE_CONTENT;
    }

    void onContentChanged(@NonNull RT referencedObject, boolean becameAvailable) {

    }

    void onContentUnavailable() {

    }

    abstract void watchRef(@NonNull Ref.Observer<RT> observer);

    abstract static class ViewHolder<C extends RefContent<RT>, RT> extends Content.ViewHolder<C> {

        ViewHolder(@NonNull View rootView) {
            super(rootView);
        }

        @Override
        final void onBind(@NonNull C content) {
            RefContent<RT> deviceRefContent = content;
            if (deviceRefContent.mReferencedObject != null) {
                onBind(content, deviceRefContent.mReferencedObject);
            }
        }

        abstract void onBind(@NonNull C content, @NonNull RT object);

        abstract class OnClickListener extends Content.ViewHolder<C>.OnClickListener {

            @Override
            public final void onClick(View v, @NonNull C content) {
                RefContent<RT> deviceRefContent = content;
                if (deviceRefContent.mReferencedObject != null) {
                    onClick(v, content, deviceRefContent.mReferencedObject);
                }
            }

            abstract void onClick(View v, @NonNull C content, @NonNull RT object);
        }
    }
}
