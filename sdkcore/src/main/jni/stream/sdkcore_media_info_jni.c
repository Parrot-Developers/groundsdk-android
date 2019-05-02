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

#include "sdkcore_media_info_jni.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** Static JNI id cache */
static struct {
	/** SdkCoreMediaInfo.Video.H264 cache. */
	struct {
		/** SdkCoreMediaInfo.Video.H264 class. */
		jclass clazz;
		/** SdkCoreMediaInfo.Video.H264 constructor. */
		jmethodID ctor;
	} media_info_video_h264;

	/** SdkCoreMediaInfo.Video.Yuv cache. */
	struct {
			/** SdkCoreMediaInfo.Video.Yuv class. */
		jclass clazz;
		/** SdkCoreMediaInfo.Video.Yuv constructor. */
		jmethodID ctor;
	} media_info_video_yuv;
} s_jni_cache;

/** Function template for creating SdkCoreMediaInfo instances. */
typedef jobject (media_info_builder) (JNIEnv *env,
		const struct pdraw_media_info *info);

/**
 * Creates a new SdkCoreMediaInfo.Video.H264 object.
 * @param[in] env: JNI env
 * @param[in] info: PDRAW media info to build upon, assuming h264 info.
 * @return a new SdkCoreMediaInfo.Video.H264 object in case of success,
 *         otherwise NULL
 */
static jobject new_h264_video_info(JNIEnv *env,
		const struct pdraw_media_info *info)
{
	jobject jinfo = NULL;

	jbyteArray sps = (*env)->NewByteArray(env, info->video.h264.spslen);
	RETURN_VAL_IF_FAILED(sps != NULL, -ENOMEM, NULL);

	(*env)->SetByteArrayRegion(env, sps, 0, info->video.h264.spslen,
			(jbyte *) info->video.h264.sps);

	jbyteArray pps = (*env)->NewByteArray(env, info->video.h264.ppslen);
	GOTO_IF_FAILED(pps != NULL, -ENOMEM, out);

	(*env)->SetByteArrayRegion(env, pps, 0, info->video.h264.ppslen,
			(jbyte *) info->video.h264.pps);

	jinfo = (*env)->NewObject(env,
			s_jni_cache.media_info_video_h264.clazz,
			s_jni_cache.media_info_video_h264.ctor,
			(jlong) info->id,
			(jint) info->video.type,
			(jint) info->video.h264.width,
			(jint) info->video.h264.height,
			sps, pps);

	LOG_IF_FAILED(jinfo != NULL, -ENOMEM);

out:
	(*env)->DeleteLocalRef(env, sps);
	(*env)->DeleteLocalRef(env, pps);

	return jinfo;
}

/**
 * Creates a new SdkCoreMediaInfo.Video.Yuv object.
 * @param[in] env: JNI env
 * @param[in] info: PDRAW media info to build upon, assuming YUV info.
 * @return a new SdkCoreMediaInfo.Video.Yuv object in case of success,
 *         otherwise NULL
 */
static jobject new_yuv_video_info(JNIEnv *env,
		const struct pdraw_media_info *info)
{
	jobject jinfo = NULL;

	jinfo = (*env)->NewObject(env,
			s_jni_cache.media_info_video_yuv.clazz,
			s_jni_cache.media_info_video_yuv.ctor,
			(jlong) info->id,
			(jint) info->video.type,
			(jint) info->video.yuv.width,
			(jint) info->video.yuv.height);

	LOG_IF_FAILED(jinfo != NULL, -ENOMEM);

	return jinfo;
}

/**
 * Obtains function to build appropriate SdkCoreMediaInfo based upon given PDRAW
 * media info.
 * @param[in] info: PDRAW media info to build upon
 * @return pointer to function to call to build appropriate SdkCoreMediaInfo,
 *         if supported, otherwise NULL
 */
static media_info_builder *get_builder(const struct pdraw_media_info *info)
{
	if (info->type != PDRAW_MEDIA_TYPE_VIDEO) {
		LOGI("Unsupported media type: %d", info->type);
		return NULL;
	}

	if (info->video.format == PDRAW_VIDEO_MEDIA_FORMAT_H264) {
		return new_h264_video_info;
	} else if (info->video.format == PDRAW_VIDEO_MEDIA_FORMAT_YUV) {
		return new_yuv_video_info;
	}

	LOGI("Unsupported video format: %d", info->video.format);
	return NULL;
}

/** Documented in public header. */
jobject sdkcore_media_info_new(JNIEnv *env, const struct pdraw_media_info *info)
{
	RETURN_VAL_IF_FAILED(env != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(info != NULL, -EINVAL, NULL);

	media_info_builder *builder = get_builder(info);

	return builder ? builder(env, info) : NULL;
}

/** Documented in public header. */
int sdkcore_media_info_is_supported(const struct pdraw_media_info *info)
{
	RETURN_ERRNO_IF_FAILED(info != NULL, -EINVAL);

	return get_builder(info) ? 0 : -ENOSYS;
}

/**
 * Initializes SdkCoreMediaInfo.Video.H264 static JNI id cache. Called once from
 * static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreMediaInfo.Video.H264 class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreMediaInfo_00024Video_00024H264_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.media_info_video_h264.clazz = (*env)->NewGlobalRef(env, clazz);
	s_jni_cache.media_info_video_h264.ctor = (*env)->GetMethodID(env, clazz,
			"<init>", "(JIII[B[B)V");
}

/**
 * Initializes SdkCoreMediaInfo.Video.Yuv static JNI id cache. Called once from
 * static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCoreMediaInfo.Video.Yuv class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_stream_SdkCoreMediaInfo_00024Video_00024Yuv_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.media_info_video_yuv.clazz = (*env)->NewGlobalRef(env, clazz);
	s_jni_cache.media_info_video_yuv.ctor = (*env)->GetMethodID(env, clazz,
			"<init>", "(JIII)V");
}
