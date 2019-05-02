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

#include "sdkcore_sink.h"

#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

#include <sdkcore/internal/sdkcore_pomp.h>

/** Static JNI id cache */
static struct
{
	/** SdkCoreSink.onFrame method. */
	jmethodID jmid_on_frame;
} s_jni_cache;

/**
 * Calls SdkCoreSink.onFrame.
 * @param[in] frame: received frame
 * @param[in] userdata: SdkCoreSink jobject
 */
static void on_frame(struct sdkcore_frame *frame, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_frame,
			(jlong) (uintptr_t) frame);
}

/**
 * Initializes SdkCoreSink native backend and starts sink.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreSink jobject
 * @param[in] nativeStreamPtr: ArsdkStream native backend
 * @param[in] nativePompPtr: SdkCorePomp native backend
 * @param[in] mediaId: identifies the media to start a sink for
 * @param[in] queueSize: sink frame queue size, must be strictly positive
 * @param[in] queueFullPolicy: policy to apply to incoming frames when the queue
 *                             is full
 * @param[in] frameFormat: media-specific format for incoming frames (only for
 *                         H.264 encoded frame sinks)
 * @return a new SdkCoreSink native backend in case of success, otherwise NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreSink_nativeStart(JNIEnv *env,
		jobject jself, jlong streamNativePtr, jlong pompNativePtr,
		jlong mediaId, jint queueSize, jint queueFullPolicy, jint frameFormat)
{
	struct sdkcore_stream *stream =
			(struct sdkcore_stream *) (uintptr_t) streamNativePtr;
	RETURN_VAL_IF_FAILED(stream != NULL, -EINVAL, 0);

	struct sdkcore_pomp *pomp =
			(struct sdkcore_pomp *) (uintptr_t) pompNativePtr;
	RETURN_VAL_IF_FAILED(pomp != NULL, -EINVAL, 0);

	struct pomp_loop *loop = sdkcore_pomp_get_loop(pomp);
	RETURN_VAL_IF_FAILED(loop != NULL, -EPROTO, 0);

	RETURN_VAL_IF_FAILED(queueSize > 0, -EINVAL, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct sdkcore_sink_cbs cbs = {
		.on_frame = on_frame
	};

	struct sdkcore_sink *self = sdkcore_sink_create(&cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err_delete_ref);

	GOTO_IF_ERR(sdkcore_sink_set_queue_size(self, (unsigned int) queueSize),
			err_destroy);

	GOTO_IF_ERR(sdkcore_sink_set_queue_full_policy(self,
			(enum sdkcore_sink_queue_full_policy) queueFullPolicy),
			err_destroy);

	GOTO_IF_ERR(sdkcore_sink_set_frame_format(self, (unsigned int) frameFormat),
			err_destroy);

	GOTO_IF_ERR(sdkcore_sink_start(self, stream, loop, (unsigned int) mediaId),
			err_destroy);

	return (jlong) (uintptr_t) self;

err_destroy:
	LOG_IF_ERR(sdkcore_sink_destroy(self, NULL));

err_delete_ref:
	(*env)->DeleteGlobalRef(env, jself);

	return 0;
}

/**
 * Stops sink and destroy SdkCoreSink native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreSink class
 * @param[in] nativePtr: SdkCoreSink native backend
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreSink_nativeStop(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_sink *self = (struct sdkcore_sink *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	jobject jself = NULL;
	RETURN_VAL_IF_ERR(sdkcore_sink_destroy(self, (void**) &jself), JNI_FALSE);

	(*env)->DeleteGlobalRef(env, jself);

	return JNI_TRUE;
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreSink class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreSink_nativeClassInit(JNIEnv *env,
		jclass clazz)
{
	s_jni_cache.jmid_on_frame = (*env)->GetMethodID(env, clazz, "onFrame",
			"(J)V");
}
