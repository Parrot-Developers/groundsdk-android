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

#include "sdkcore_texture_loader.h"

#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** Static JNI id cache */
static struct
{
	/** SdkCoreTextureLoader.onLoadTexture method. */
	jmethodID jmid_on_load_texture;
} s_jni_cache;

/**
 * Calls SdkCoreTextureLoader.onLoadTexture.
 * @param[in] texture_size: render texture size information
 * @param[in] frame: PDRAW video frame
 * @param[in] frame_userdata: opaque frame user data
 * @param[in] session_meta: streaming session metadata
 * @param[in] userdata: SdkCoreTextureLoader jobject
 */
static int on_load_texture(
		const struct sdkcore_texture_loader_texture_size *texture_size,
		const struct pdraw_video_frame *frame,
		const struct sdkcore_texture_loader_frame_userdata *frame_userdata,
		const struct vmeta_session *session_meta, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_ERRNO_IF_FAILED(env != NULL, res);

	struct jobject *jself = (jobject) (uintptr_t) userdata;
	RETURN_ERRNO_IF_FAILED(jself != NULL, -EINVAL);

	return (*env)->CallBooleanMethod(env, jself,
			s_jni_cache.jmid_on_load_texture, (jint) texture_size->width,
			(jint) texture_size->height, (jlong) (uintptr_t) frame,
			(jlong) (uintptr_t) frame_userdata->data,
			(jlong) frame_userdata->size, (jlong) (uintptr_t) session_meta)
			== JNI_TRUE ? 0 : -ECANCELED;
}

/**
 * Initializes SdkCoreTextureLoader native backend.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreTextureLoader jobject
 * @param[in] width: specified texture width; positive; 0 if unspecified
 * @param[in] ratioWidth: specified texture aspect ratio width factor; positive;
 *                        0 if unspecified
 * @param[in] ratioHeight: specified texture aspect ratio height factor;
 *                         positive; 0 if unspecified
 * @return a new SdkCoreTextureLoader native backend in case of success, otherwise
 *         NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreTextureLoader_nativeInit(
		JNIEnv *env, jobject jself, jint width, jint ratioWidth,
		jint ratioHeight)
{
	RETURN_VAL_IF_FAILED(width >= 0, -EINVAL, 0);
	RETURN_VAL_IF_FAILED(ratioWidth >= 0, -EINVAL, 0);
	RETURN_VAL_IF_FAILED(ratioHeight >= 0, -EINVAL, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct sdkcore_texture_loader_cbs cbs = {
		.on_load_texture = on_load_texture
	};

	struct sdkcore_texture_loader_texture_spec texture_spec = {
		.width = (unsigned int) width,
		.aspect_ratio = {
			.width = (unsigned int) ratioWidth,
			.height = (unsigned int) ratioHeight,
		}
	};

	struct sdkcore_texture_loader *self = sdkcore_texture_loader_create(
			&texture_spec, &cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, jself);

	return 0;
}

/**
 * Destroys SdkCoreTextureLoader native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreTextureLoader class
 * @param[in] nativePtr: SdkCoreTextureLoader native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreTextureLoader_nativeDestroy(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct sdkcore_texture_loader *self =
			(struct sdkcore_texture_loader *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	jobject jself = NULL;
	RETURN_IF_ERR(sdkcore_texture_loader_destroy(self, (void**) &jself));

	(*env)->DeleteGlobalRef(env, jself);
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreTextureLoader class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreTextureLoader_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_on_load_texture = (*env)->GetMethodID(env, clazz,
			"onLoadTexture", "(IIJJJJ)Z");
}
