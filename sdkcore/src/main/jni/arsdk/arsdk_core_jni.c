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

#include "arsdkcore_command.h"

#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/** Static JNI method id cache */
static struct
{
	/* on ArsdkCore */
	jmethodID jmid_device_added;   /**< onDeviceAdded */
	jmethodID jmid_device_removed; /**< onDeviceRemoved */
} s_jni_cache;

static void device_added(uint16_t handle, const struct arsdk_device_info *info,
		void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jstring juid = (*env)->NewStringUTF(env, info->id);
	RETURN_IF_FAILED(juid != NULL, -ENOMEM);

	jstring jname = (*env)->NewStringUTF(env, info->name);
	GOTO_IF_FAILED(jname != NULL, -ENOMEM, out);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_added, (jshort) handle, juid,
			(jint) info->type, jname, (jint) info->backend_type,
			(jint) info->api);

out:
	(*env)->DeleteLocalRef(env, juid);
	(*env)->DeleteLocalRef(env, jname);
}

static void device_removed(uint16_t handle, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_removed, (jshort) handle);
}

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] rectClazz: android.graphics.Rect class, used to initialize jni IDs cache for this class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeClassInit(JNIEnv *env,
		jclass clazz, jclass rectClazz)
{
	s_jni_cache.jmid_device_added = (*env)->GetMethodID(env, clazz,
			"onDeviceAdded","(SLjava/lang/String;ILjava/lang/String;II)V");
	s_jni_cache.jmid_device_removed = (*env)->GetMethodID(env, clazz,
			"onDeviceRemoved", "(S)V");
}

/**
 * Initializes the ArsdkCore native backend.
 * @param[in] env: JNI env pointer
 * @param[in] jself: reference on java ArsdkCore instance
 * @param[in] pompNativePtr: SdkCorePomp native backend
 * @return the pointer to the ArsdkCore native backend instance if successful,
 *         0 otherwise
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeInit(JNIEnv *env,
		jobject jself, jlong pompNativePtr)
{

	struct sdkcore_pomp *pomp = (struct sdkcore_pomp *) pompNativePtr;
	RETURN_VAL_IF_FAILED(pomp != NULL, -EINVAL, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct arsdkcore_cbs cbs = {
		.device_added = &device_added,
		.device_removed = &device_removed,
	};

	struct arsdkcore *self = arsdkcore_create(pomp, &cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, jself);
	return 0;
}

/**
 * Sets global command log level.
 * @param[in] env: JNI env
 * @param[in] clazz: ArsdkCore class
 * @param[in] level: log level
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeSetCommandLogLevel(
		JNIEnv *env, jclass clazz, jint level) {
    arsdkcore_command_set_log_level((enum arsdkcore_command_log_level) level);
}

/**
 * Sets user agent info.
 * Note: such info is sent in the json during connection with a remote device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkCore native backend instance
 * @param[in] jtype: string representing the controller type
 * @param[in] jname: string representing the controller name
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeSetUserAgent(JNIEnv *env,
		jclass clazz, jlong nativePtr, jstring jtype, jstring jname)
{
	struct arsdkcore *self = (struct arsdkcore *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	const char *type = (*env)->GetStringUTFChars(env, jtype, NULL);
	RETURN_IF_FAILED(type != NULL, -ENOMEM);

	const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
	GOTO_IF_FAILED(name != NULL, -ENOMEM, out);

	arsdkcore_set_user_agent(self, type, name);

out:
	(*env)->ReleaseStringUTFChars(env, jtype, type);
	(*env)->ReleaseStringUTFChars(env, jname, name);
}

/**
 * Configures video decoding.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkCore native backend instance
 * @param[in] enable: 1 to enable video decoding, 0 to disable it
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeEnableVideoDecoding(
		JNIEnv *env, jclass clazz, jlong nativePtr, jboolean enable)
{
	struct arsdkcore *self = (struct arsdkcore *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	arsdkcore_enable_video_decoding(self, (int) enable);
}
/**
 * Disposes of the ArsdkCore native backend.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkCore native backend instance
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkCore_nativeDispose(JNIEnv *env,
		jclass clazz, jlong nativePtr)
{
	struct arsdkcore *self = (struct arsdkcore *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	jobject jself = NULL;
	RETURN_IF_ERR(arsdkcore_destroy(self, (void **) jself));

	(*env)->DeleteGlobalRef(env, jself);
}
