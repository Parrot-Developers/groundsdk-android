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

#include "sdkcore_stream.h"
#include "sdkcore_media_info_jni.h"
#include "sdkcore_file_source.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

#include <sdkcore/internal/sdkcore_pomp.h>

/** Static JNI id cache */
static struct
{
	/** SdkCoreStream.onClosing method. */
	jmethodID jmid_on_closing;
	/** SdkCoreStream.onClosed method. */
	jmethodID jmid_on_closed;
	/** SdkCoreStream.onPlaybackState method. */
	jmethodID jmid_on_playback_state;
	/** SdkCoreStream.onMediaAdded method. */
	jmethodID jmid_on_media_added;
	/** SdkCoreStream.onMediaRemoved method. */
	jmethodID jmid_on_media_removed;
} s_jni_cache;

/**
 * Calls SdkCoreStream.onClosing.
 * @param[in] userdata: SdkCoreStream jobject
 */
static void on_closing(void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_closing);
}

/**
 * Calls SdkCoreStream.onClosed.
 * @param[in] userdata: SdkCoreStream jobject
 */
static void on_closed(void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_closed);
	(*env)->DeleteGlobalRef(env, jself);
}

/**
 * Calls SdkCoreStream.onPlaybackStateChanged.
 * @param[in] playback_state: current playback state
 * @param[in] userdata: SdkCoreStream jobject
 */
static void on_playback_state(
		const struct sdkcore_stream_playback_state *playback, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t) userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_playback_state,
			(jlong) playback->duration, (jlong) playback->position,
			(jdouble) playback->speed);
}

/**
 * Calls SdkCoreStream.onMediaAdded.
 * @param[in] info: added PDRAW media info
 * @param[in] userdata: SdkCoreStream jobject
 */
static void on_media_added(const struct pdraw_media_info *info, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	RETURN_IF_FAILED(info != NULL, -EINVAL);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	jobject jinfo = sdkcore_media_info_new(env, info);
	if (jinfo != NULL) {
		(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_media_added,
				jinfo);
		(*env)->DeleteLocalRef(env, jinfo);
	}
}

/**
 * Calls SdkCoreStream.onMediaRemoved.
 * @param[in] info: removed PDRAW media info
 * @param[in] userdata: SdkCoreStream jobject
 */
static void on_media_removed(const struct pdraw_media_info *info,
		void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	RETURN_IF_FAILED(info != NULL, -EINVAL);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	if (sdkcore_media_info_is_supported(info) == 0) {
		(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_media_removed,
				(jlong) info->id);
	}
}

/** Documented in public header. */
struct sdkcore_stream * sdkcore_stream_jni_open(JNIEnv *env, jobject jself,
		struct pomp_loop *loop, struct sdkcore_source *source,
		const char *track)
{
	RETURN_VAL_IF_FAILED(env != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(jself != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(loop != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(source != NULL, -EINVAL, NULL);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, NULL);

	struct sdkcore_stream_cbs cbs = {
		.on_stream = {
			.closing = on_closing,
			.closed = on_closed
		},
		.on_playback_state = on_playback_state,
		.on_media = {
			.added = on_media_added,
			.removed = on_media_removed
		}
	};

	struct sdkcore_stream *self = sdkcore_stream_open(loop, source, track, &cbs,
			jself);

	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return self;

err:
	(*env)->DeleteGlobalRef(env, jself);

	return NULL;
}

/**
 * Initializes SdkCoreStream native backend and opens a stream from local file.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreStream jobject
 * @param[in] pompNativePtr: SdkCorePomp native backend
 * @param[in] jpath: absolute path of the file to open
 * @param[in] jtrack: track to select, NULL to select default track, if any
 * @return a new SdkCoreStream native backend in case of success, otherwise NULL
 */
JNIEXPORT long JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativeOpenFile(JNIEnv *env,
		jobject jself, jlong pompNativePtr, jstring jpath, jstring jtrack)
{
	struct sdkcore_pomp *pomp =
			(struct sdkcore_pomp *) (uintptr_t) pompNativePtr;
	RETURN_VAL_IF_FAILED(pomp != NULL, -EINVAL, 0);

	struct pomp_loop *loop = sdkcore_pomp_get_loop(pomp);
	RETURN_VAL_IF_FAILED(loop != NULL, -EINVAL, 0);

	const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
	RETURN_VAL_IF_FAILED(path != NULL, -ENOMEM, 0);

	struct sdkcore_file_source *source = sdkcore_file_source_create(path);

	(*env)->ReleaseStringUTFChars(env, jpath, path);

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
	LOG_IF_ERR(sdkcore_file_source_destroy(source));

	return 0;
}

/**
 * Resumes playback.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreStream class
 * @param[in] nativePtr: SdkCoreStream native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativePlay(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_stream *self =
			(struct sdkcore_stream *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(sdkcore_stream_play(self));
}

/**
 * Pauses playback.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreStream class
 * @param[in] nativePtr: SdkCoreStream native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativePause(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_stream *self =
			(struct sdkcore_stream *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(sdkcore_stream_pause(self));
}

/**
 * Seeks to position.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreStream class
 * @param[in] nativePtr: SdkCoreStream native backend
 * @param[in] position: position to seek to, in milliseconds
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativeSeek(JNIEnv *env,
		jclass clazz, jlong nativePtr, jlong position)
{
	struct sdkcore_stream *self =
			(struct sdkcore_stream *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(sdkcore_stream_seek(self, (int64_t) position));
}

/**
 * Closes stream.
 * Stream closes asynchronously; once it happens, onClose method is called, then
 * SdkCoreStream native backend will be destroyed upon return.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreStream class
 * @param[in] nativePtr: SdkCoreStream native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativeClose(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_stream *self =
			(struct sdkcore_stream *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(sdkcore_stream_close(self));
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreStream class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreStream_nativeClassInit(JNIEnv *env,
		jclass clazz)
{
	s_jni_cache.jmid_on_closing = (*env)->GetMethodID(env, clazz, "onClosing",
			"()V");
	s_jni_cache.jmid_on_closed = (*env)->GetMethodID(env, clazz, "onClosed",
			"()V");
	s_jni_cache.jmid_on_playback_state = (*env)->GetMethodID(env, clazz,
			"onPlaybackState", "(JJD)V");
	s_jni_cache.jmid_on_media_added = (*env)->GetMethodID(env, clazz,
			"onMediaAdded",
			"(Lcom/parrot/drone/sdkcore/stream/SdkCoreMediaInfo;)V");
	s_jni_cache.jmid_on_media_removed = (*env)->GetMethodID(env, clazz,
			"onMediaRemoved", "(J)V");
}
