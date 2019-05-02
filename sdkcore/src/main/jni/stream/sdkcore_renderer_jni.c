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

#include "sdkcore_renderer.h"

#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** Static JNI id cache */
static struct
{
	/** SdkCoreRenderer.onFrameReady method. */
	jmethodID jmid_frame_ready;
} s_jni_cache;

/**
 * Calls SdkCoreRenderer.onFrameReady.
 * @param[in] userdata: SdkCoreRenderer jobject
 */
static void on_frame_ready(void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t)  userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_frame_ready);
}

/**
 * Initializes SdkCoreRenderer native backend.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreRenderer jobject
 * @return a new SdkCoreRenderer native backend in case of success, otherwise NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeInit(JNIEnv *env,
		jobject jself)
{
	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct sdkcore_renderer_cbs cbs = {
		.on_frame_ready = on_frame_ready
	};

	struct sdkcore_renderer *self = sdkcore_renderer_create(&cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, jself);

	return 0;
}

/**
 * Configures render zone.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] x: render zone x coordinate
 * @param[in] y: render zone y coordinate
 * @param[in] width: render zone width, must be positive
 * @param[in] height: render zone height, must be positive
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeSetRenderZone(
		JNIEnv *env, jclass clazz, jlong nativePtr, jint x, jint y, jint width,
		jint height)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);
	RETURN_VAL_IF_FAILED(width >= 0 && height >= 0, -EINVAL, JNI_FALSE);

	struct pdraw_rect zone = {
		.x = x,
		.y = y,
		.width = width,
		.height = height
	};

	return (jboolean) sdkcore_renderer_set_render_zone(self, &zone) == 0;
}

/**
 * Configures fill mode.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] mode: fill mode to configure (values map to
 *                  pdraw_video_renderer_fill_mode values)
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeSetFillMode(
		JNIEnv *env, jclass clazz, jlong nativePtr, jint mode)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_set_fill_mode(self,
			(enum pdraw_video_renderer_fill_mode) mode) == 0;
}

/**
 * Configures overexposure zebras rendering.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] enable: JNI_TRUE to enable zebras rendering, JNI_FALSE to disable
 *                    it
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeEnableZebras(
		JNIEnv *env, jclass clazz, jlong nativePtr, jboolean enable)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_enable_zebras(self, (int) enable) == 0;
}

/**
 * Configures overexposure zebras threshold.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] threshold: threshold to configure, in [0, 1] range
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeSetZebraThreshold(
		JNIEnv *env, jclass clazz, jlong nativePtr, jdouble threshold)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_set_zebra_threshold(self,
			(double) threshold) == 0;
}

/**
 * Configures color histogram computation.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] enable: JNI_TRUE to enable color histogram computation, JNI_FALSE
 *                    to disable it
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeEnableHistogram(
		JNIEnv *env, jclass clazz, jlong nativePtr, jboolean enable)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_enable_histogram(self,
			(int) enable) == 0;
}

/**
 * Configures rendering overlayer.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] overlayerNativePtr: SdkCoreOverlayer native backend, 0 to disable
 *                                overlayer
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeSetOverlayer(
		JNIEnv *env, jclass clazz, jlong nativePtr, jlong overlayerNativePtr)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	struct sdkcore_overlayer *overlayer =
			(struct sdkcore_overlayer *) (uintptr_t) overlayerNativePtr;

	return (jboolean) sdkcore_renderer_set_overlayer(self, overlayer) == 0;
}

/**
 * Configures rendering texture loader.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] textureLoaderNativePtr: SdkCoreTextureLoader native backend, 0 to
 *                                    disable texture loader
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeSetTextureLoader(
		JNIEnv *env, jclass clazz, jlong nativePtr,
		jlong textureLoaderNativePtr)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	struct sdkcore_texture_loader *texture_loader =
			(struct sdkcore_texture_loader *) (uintptr_t)
			textureLoaderNativePtr;

	return (jboolean) sdkcore_renderer_set_texture_loader(self,
			texture_loader) == 0;
}

/**
 * Starts rendering.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] nativeStreamPtr: SdkCoreStream native backend
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeStart(JNIEnv *env,
		jclass clazz, jlong nativePtr, jlong streamNativePtr)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	struct sdkcore_stream *stream =
			(struct sdkcore_stream *) (uintptr_t) streamNativePtr;
	RETURN_VAL_IF_FAILED(stream != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_start(self, stream) == 0;
}

/**
 * Renders current frame.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @param[in] content zone: if not NULL, upon success, contains rendering
                            content zone;
 *                          x coordinate at index 0 in array,
 *                          y coordinate at index 1 in array,
 *                          width at index 2 in array,
 *                          height at index 3 in array
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeRenderFrame(
		JNIEnv *env, jclass clazz, jlong nativePtr, jintArray contentZone)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	struct pdraw_rect content_zone = {0};
	RETURN_VAL_IF_ERR(sdkcore_renderer_render_frame(self, &content_zone),
			JNI_FALSE);

	if (!contentZone) {
		return JNI_TRUE;
	}

	jint *contentZonePtr = (*env)->GetPrimitiveArrayCritical(env, contentZone,
			NULL);
	RETURN_VAL_IF_FAILED(contentZonePtr != NULL, -ENOMEM, JNI_TRUE);

	contentZonePtr[0] = (jint) content_zone.x;
	contentZonePtr[1] = (jint) content_zone.y;
	contentZonePtr[2] = (jint) content_zone.width;
	contentZonePtr[3] = (jint) content_zone.height;

	(*env)->ReleasePrimitiveArrayCritical(env, contentZone, contentZonePtr, 0);

	return JNI_TRUE;
}

/**
 * Stops rendering.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeStop(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	return (jboolean) sdkcore_renderer_stop(self) == 0;
}

/**
 * Destroys SdkCoreRenderer native backend.
 * If started, renderer will be stopped beforehand; if this operation fails,
 * then the SdkCoreRenderer native backend is not destroyed.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 * @param[in] nativePtr: SdkCoreRenderer native backend
 * @return JNI_TRUE in case of success, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeDestroy(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct sdkcore_renderer *self =
			(struct sdkcore_renderer *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

    jobject jself = NULL;
	RETURN_VAL_IF_ERR(sdkcore_renderer_destroy(self, (void**) &jself),
			JNI_FALSE);

	(*env)->DeleteGlobalRef(env, jself);

	return JNI_TRUE;
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreRenderer class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreRenderer_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_frame_ready = (*env)->GetMethodID(env, clazz,
			"onFrameReady", "()V");
}
