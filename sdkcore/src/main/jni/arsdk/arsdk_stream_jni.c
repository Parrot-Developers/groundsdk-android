/*
 *	 Copyright (C) 2019 Parrot Drones SAS
 *
 *	 Redistribution and use in source and binary forms, with or without
 *	 modification, are permitted provided that the following conditions
 *	 are met:
 *	 * Redistributions of source code must retain the above copyright
 *	   notice, this list of conditions and the following disclaimer.
 *	 * Redistributions in binary form must reproduce the above copyright
 *	   notice, this list of conditions and the following disclaimer in
 *	   the documentation and/or other materials provided with the
 *	   distribution.
 *	 * Neither the name of the Parrot Company nor the names
 *	   of its contributors may be used to endorse or promote products
 *	   derived from this software without specific prior written
 *	   permission.
 *
 *	 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *	 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *	 FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *	 PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *	 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *	 BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *	 OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	 AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *	 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *	 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *	 SUCH DAMAGE.
 *
 */

#include "arsdkcore.h"
#include "arsdkcore_source.h"

#define ARSDK_LOG_TAG stream
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>
#include <sdkcore/internal/sdkcore_stream_jni.h>

/**
 * Initializes ArsdkStream native backend and opens an RTSP stream from a remote
 * device.
 * @param[in] env: JNI env
 * @param[in] jself: ArsdkStream jobject
 * @param[in] arsdkNativePtr: ArsdkCore native backend
 * @param[in] deviceHandle: handle of the device providing the stream
 * @param[in] jurl: URL of stream to open
 * @param[in] jtrack: track to select, NULL to select default track, if any
 * @return a new ArsdkStream native backend in case of success, otherwise NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_stream_ArsdkStream_nativeOpen(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr, jshort deviceHandle,
		jstring jurl, jstring jtrack)
{
	struct arsdkcore *arsdk = (struct arsdkcore *) (uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	struct pomp_loop *loop = arsdkcore_get_loop(arsdk);
	RETURN_VAL_IF_FAILED(loop != NULL, -EINVAL, 0);

	const char *url = (*env)->GetStringUTFChars(env, jurl, NULL);
	RETURN_VAL_IF_FAILED(url != NULL, -ENOMEM, 0);

	struct arsdkcore_source *source = arsdkcore_source_create(arsdk,
			(uint16_t) deviceHandle, url);

	(*env)->ReleaseStringUTFChars(env, jurl, url);

	RETURN_VAL_IF_FAILED(source != NULL, -ENOMEM, 0);

	const char *track = NULL;
	if (jtrack) {
		track = (*env)->GetStringUTFChars(env, jtrack, NULL);
		GOTO_IF_FAILED(track != NULL, -ENOMEM, err);
	}

	struct sdkcore_stream *self = sdkcore_stream_jni_open(env, jself, loop,
			(struct sdkcore_source *) source, track);

	if (jtrack) {
		(*env)->ReleaseStringUTFChars(env, jtrack, track);
	}

	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	LOG_IF_ERR(arsdkcore_source_destroy(source));

	return 0;
}
