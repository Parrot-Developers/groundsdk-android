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

#include "sdkcore_overlayer.h"

#include <sdkcore/internal/sdkcore_rect_jni.h>
#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** Static JNI id cache */
static struct
{
	/** SdkCoreOverlayer.onOverlay method. */
	jmethodID jmid_on_overlay;
	/** SdkCoreOverlayer.mRenderZone Rect field. */
	jfieldID jfid_render_zone;
	/** SdkCoreOverlayer.mContentZone Rect field. */
	jfieldID jfid_content_zone;
	/** SdkCoreOverlayer.mHistogram* float[] fields. */
	jfieldID jfid_histogram[PDRAW_HISTOGRAM_CHANNEL_MAX];
} s_jni_cache;

/** Histogram instance cache. */
struct jhistogram {
	/** Histogram java array. */
	jfloatArray self;
	/** Histogram size. */
	size_t size;
};

/** SdkCoreOverlayer instance cache. */
struct joverlayer {
	/** SdkCoreOverlayer jobject. */
	jobject self;
	/** SdkCoreOverlayer.mRenderZone Rect jobject. */
	jobject render_zone;
	/** SdkCoreOverlayer.mContentZone Rect jobject. */
	jobject content_zone;
	/** SdkCoreOverlayer.mHistogram* instances cache. */
	struct jhistogram histogram[PDRAW_HISTOGRAM_CHANNEL_MAX];
};

/**
 * Updates an SdkCoreOverlayer Rect field.
 * @param[in] env: JNI env
 * @param[in] jzone: Rect jobject to update
 * @param[in] zone: zone to update rect with
 */
static void update_zone(JNIEnv *env, jobject jzone,
		const struct pdraw_rect *zone)
{
	LOG_IF_ERR(sdkcore_rect_set(env, jzone, zone->x, zone->y, zone->width,
			zone->height));
}

/**
 * Updates an SdkCoreOverlayer histogram field.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreOverlayer instance cache
 * @param[in] extra: PDRAW frame extraneous data, containing histogram data
 * @param[in] channel: histogram channel to update
 */
static void update_histogram(JNIEnv *env, struct joverlayer *jself,
		const struct pdraw_video_frame_extra *extra,
		enum pdraw_histogram_channel channel)
{
	if (!s_jni_cache.jfid_histogram[channel]) {
		return;
	}

	struct jhistogram *histogram = &jself->histogram[channel];
	size_t size = extra->histogram_len[channel];

	if (histogram->size != size) {
		(*env)->DeleteGlobalRef(env, histogram->self);
		histogram->self = NULL;
		histogram->size = 0;

		jfloatArray data = (*env)->NewFloatArray(env, size);
		RETURN_IF_FAILED(data != NULL, -ENOMEM);

		histogram->self = (*env)->NewGlobalRef(env, data);
		(*env)->DeleteLocalRef(env, data);
		RETURN_IF_FAILED(histogram->self != NULL, -ENOMEM);

		histogram->size = size;
		(*env)->SetObjectField(env, jself->self,
				s_jni_cache.jfid_histogram[channel], histogram->self);
	}

	(*env)->SetFloatArrayRegion(env, histogram->self, 0, size,
			extra->histogram[channel]);
}

/**
 * Updates SdkCoreOverlayer fields and calls onOverlay.
 * @param[in] render_zone: render zone update to apply
 * @param[in] content_zone: content zone update to apply
 * @param[in] session_info: PDRAW session info
 * @param[in] session_meta: session metadata
 * @param[in] frame_meta: frame metadata
 * @param[in] extra: PDRAW frame extraneous data, containing histogram data to
 *                   apply; NULL in case of redrawing old frame to keep
 *                   the frame rate.
 * @param[in] userdata: SdkCoreOverlayer instance cache
 */
static void on_overlay(const struct pdraw_rect *render_zone,
		const struct pdraw_rect *content_zone,
		const struct pdraw_session_info *session_info,
		const struct vmeta_session *session_meta,
		const struct vmeta_frame *frame_meta,
		const struct pdraw_video_frame_extra *extra, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	struct joverlayer *jself =  (jobject) (uintptr_t) userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	update_zone(env, jself->render_zone, render_zone);
	update_zone(env, jself->content_zone, content_zone);

	if (extra != NULL) {
		for (enum pdraw_histogram_channel channel = 0;
		     channel < PDRAW_HISTOGRAM_CHANNEL_MAX; ++channel) {
			update_histogram(env, jself, extra, channel);
		}
	}

	(*env)->CallVoidMethod(env, jself->self, s_jni_cache.jmid_on_overlay,
			session_info, session_meta, frame_meta);
}

/**
 * Destroys SdkCoreOverlayer instance cache.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreOverlayer instance cache to destroy
 */
static void destroy(JNIEnv *env, struct joverlayer *jself) {

	(*env)->DeleteGlobalRef(env, jself->self);
	(*env)->DeleteGlobalRef(env, jself->render_zone);
	(*env)->DeleteGlobalRef(env, jself->content_zone);

	for (enum pdraw_histogram_channel channel = 0;
			channel < PDRAW_HISTOGRAM_CHANNEL_MAX; ++channel) {
		(*env)->DeleteGlobalRef(env, jself->histogram[channel].self);
	}

	free(jself);
}

/**
 * Initializes SdkCoreOverlayer native backend.
 * @param[in] env: JNI env
 * @param[in] jself: SdkCoreOverlayer jobject
 * @return a new SdkCoreOverlayer native backend in case of success, otherwise
 *         NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreOverlayer_nativeInit(JNIEnv *env,
		jobject jOverlayer)
{
	struct joverlayer *jself = calloc(1, sizeof(*jself));
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	jself->self = (*env)->NewGlobalRef(env, jOverlayer);
	GOTO_IF_FAILED(jself->self != NULL, -ENOMEM, err);

	jself->render_zone = (*env)->GetObjectField(env, jself->self,
			s_jni_cache.jfid_render_zone);
	GOTO_IF_FAILED(jself->render_zone != NULL, -EINVAL, err);

	jself->render_zone = (*env)->NewGlobalRef(env, jself->render_zone);
	GOTO_IF_FAILED(jself->render_zone != NULL, -ENOMEM, err);

	jself->content_zone = (*env)->GetObjectField(env, jself->self,
			s_jni_cache.jfid_content_zone);
	GOTO_IF_FAILED(jself->content_zone != NULL, -EINVAL, err);

	jself->content_zone = (*env)->NewGlobalRef(env, jself->content_zone);
	GOTO_IF_FAILED(jself->content_zone != NULL, -ENOMEM, err);

	for (enum pdraw_histogram_channel channel = 0;
			channel < PDRAW_HISTOGRAM_CHANNEL_MAX; ++channel) {
		if (!s_jni_cache.jfid_histogram[channel]) {
			LOGW("Histogram channel %d support not implemented", channel);
			continue;
		}

		struct jhistogram *histogram = &jself->histogram[channel];

		histogram->self = (*env)->GetObjectField(env, jself->self,
				s_jni_cache.jfid_histogram[channel]);
		GOTO_IF_FAILED(histogram->self != NULL, -EINVAL, err);

		histogram->self = (*env)->NewGlobalRef(env, histogram->self);
		GOTO_IF_FAILED(histogram->self != NULL, -ENOMEM, err);

		histogram->size = (*env)->GetArrayLength(env, histogram->self);
	}

	struct sdkcore_overlayer_cbs cbs = {
		.on_overlay = on_overlay
	};

	struct sdkcore_overlayer *self = sdkcore_overlayer_create(&cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	destroy(env, jself);

	return 0;
}

/**
 * Destroys SdkCoreOverlayer native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreOverlayer class
 * @param[in] nativePtr: SdkCoreOverlayer native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreOverlayer_nativeDestroy(JNIEnv *env,
		jclass clazz, jobject nativePtr)
{
	struct sdkcore_overlayer *self =
			(struct sdkcore_overlayer *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct joverlayer *jself = NULL;
	RETURN_IF_ERR(sdkcore_overlayer_destroy(self, (void**) &jself));

	destroy(env, jself);
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreOverlayer class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreOverlayer_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_on_overlay = (*env)->GetMethodID(env, clazz, "onOverlay",
			"(JJJ)V");
	s_jni_cache.jfid_render_zone = (*env)->GetFieldID(env, clazz, "mRenderZone",
			"Landroid/graphics/Rect;");
	s_jni_cache.jfid_content_zone = (*env)->GetFieldID(env, clazz,
			"mContentZone", "Landroid/graphics/Rect;");
	s_jni_cache.jfid_histogram[PDRAW_HISTOGRAM_CHANNEL_RED] =
			(*env)->GetFieldID(env, clazz, "mHistogramRed", "[F");
	s_jni_cache.jfid_histogram[PDRAW_HISTOGRAM_CHANNEL_GREEN] =
			(*env)->GetFieldID(env, clazz, "mHistogramGreen", "[F");
	s_jni_cache.jfid_histogram[PDRAW_HISTOGRAM_CHANNEL_BLUE] =
			(*env)->GetFieldID(env, clazz, "mHistogramBlue", "[F");
	s_jni_cache.jfid_histogram[PDRAW_HISTOGRAM_CHANNEL_LUMA] =
			(*env)->GetFieldID(env, clazz, "mHistogramLuma", "[F");
}
