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

#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/** Static JNI method id cache */
static struct
{
	/** on ArsdkBlackBoxRequest */
	jmethodID jmid_rc_button_action; /** onRcButtonAction */
	jmethodID jmid_rc_piloting_info; /** onRcPilotingInfo */
	jmethodID jmid_unregistered;     /** onUnregistered */
} s_jni_cache;

static void rc_button_action(struct arsdk_blackbox_itf *itf,
		struct arsdk_blackbox_listener *listener, int action, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_rc_button_action, (jint) action);
}

static void rc_piloting_info(struct arsdk_blackbox_itf *itf,
		struct arsdk_blackbox_listener *listener,
		struct arsdk_blackbox_rc_piloting_info *info, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_rc_piloting_info, (jint) info->roll,
			(jint) info->pitch, (jint) info->yaw, (jint) info->gaz,
			(jint) info->source);
}

static void unregistered(struct arsdk_blackbox_itf *itf,
		struct arsdk_blackbox_listener *listener, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_unregistered);

	(*env)->DeleteGlobalRef(env, (jobject) userdata);
}

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_blackbox_ArsdkBlackBoxRequest_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_rc_button_action = (*env)->GetMethodID(env, clazz,
			"onRcButtonAction", "(I)V");
	s_jni_cache.jmid_rc_piloting_info = (*env)->GetMethodID(env, clazz,
			"onRcPilotingInfo", "(IIIII)V");
	s_jni_cache.jmid_unregistered = (*env)->GetMethodID(env, clazz,
			"onUnregistered", "()V");
}

/**
 * Creates and runs an ArsdkBlackBoxRequest
 * @param[in] env: JNI env pointer
 * @param[in] jself: reference on java ArsdkBlackBoxRequest instance
 * @param[in] arsdkNativePtr: pointer to the native arsdkcore instance
 * @param[in] deviceHandle: native handle of the device for which to collect
 *            black box info
 * @return the pointer to the ArsdkBlackBoxRequest native backend instance
 *         if successful, 0 otherwise
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_blackbox_ArsdkBlackBoxRequest_nativeCreate(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr, jshort deviceHandle)
{
	struct arsdkcore *arsdk = (struct arsdkcore *)(uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	struct arsdk_device *device = arsdkcore_get_device(arsdk,
			(uint16_t) deviceHandle);
	RETURN_VAL_IF_FAILED(device != NULL, -ENODEV, 0);

	struct arsdk_blackbox_itf *blackbox_itf = NULL;
	int res = arsdk_device_get_blackbox_itf(device, &blackbox_itf);
	RETURN_VAL_IF_FAILED(blackbox_itf != NULL, res, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct arsdk_blackbox_listener_cbs cbs = {
		.rc_button_action = &rc_button_action,
		.rc_piloting_info = &rc_piloting_info,
		.unregister = &unregistered,
		.userdata = jself,
	};

	struct arsdk_blackbox_listener *listener = NULL;
	res = arsdk_blackbox_itf_create_listener(blackbox_itf, &cbs,
			&listener);

	GOTO_IF_FAILED(listener != NULL, res, err);

	return (jlong) (uintptr_t) listener;

err:
	(*env)->DeleteGlobalRef(env, jself);
	return 0;
}

/**
 * Cancels a running ArsdkBlackBoxRequest
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkBlackBoxRequest native
 *            backend instance
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_blackbox_ArsdkBlackBoxRequest_nativeCancel(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_blackbox_listener *listener =
			(struct arsdk_blackbox_listener *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(listener != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_blackbox_listener_unregister(listener));
}
