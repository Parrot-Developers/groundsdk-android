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

#include "arsdk_backend_ble_jni.h"

#include "arsdkctrl_backend_ble.h"
#include "arsdkcore.h"

#define ARSDK_LOG_TAG backend
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/** Static JNI method id cache */
static struct
{
	/* on ArsdkBleBackend */
	jmethodID jmid_backend_open_connection;  /**< openConnection method */
	jmethodID jmid_backend_close_connection; /**< closeConnection method */
} s_jni_cache;

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer.
 * @param[in] type: class where this static java method exist.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleBackend_nativeClassInit(
		JNIEnv *env, jclass type)
{
	s_jni_cache.jmid_backend_open_connection = (*env)->GetMethodID(env, type,
			"openConnection", "(Ljava/lang/String;J)Z");
	s_jni_cache.jmid_backend_close_connection = (*env)->GetMethodID(env, type,
			"closeConnection", "(Ljava/lang/String;)V");
}

/**
 * Initializes the java backend handler. Called from ArsdkBleBackend
 * constructor.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java backend handler ref.
 * @param[in] arsdkNativePtr: pointer to the native arsdk manager instance.
 * @return the pointer to the native backend instance if the java backend was
 *         properly initialized, 0 otherwise.
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleBackend_nativeInit(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr)
{
	struct arsdkcore *arsdk = (struct arsdkcore *) (uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

    jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct arsdkctrl_backend_ble *self = NULL;
	int res = arsdkctrl_backend_ble_new(arsdkcore_get_ctrl(arsdk), (void *)jself,
			&self);

	GOTO_IF_FAILED(self != NULL, res, err);

	return (jlong) (uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, jself);
	return 0;
}

/**
 * Releases the java backend handler. Called from ArsdkBleBackend.destroy().
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java backend handler ref.
 * @param[in] nativePtr: pointer to the native backend instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleBackend_nativeRelease(
		JNIEnv *env, jobject jself, jlong nativePtr)
{
	struct arsdkctrl_backend_ble *self =
			(struct arsdkctrl_backend_ble *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	jself = arsdkctrl_backend_ble_destroy(self);
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->DeleteGlobalRef(env, (jobject) jself);
}

/**
 * Gets the parent backend native pointer for this java backend handler.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java backend handler ref.
 * @param[in] nativePtr: pointer to the native backend instance.
 * @return a pointer to the native parent backend, or NULL if none.
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleBackend_nativeGetParent(
		JNIEnv *env, jobject jself, jlong nativePtr)
{
	struct arsdkctrl_backend_ble *self =
			(struct arsdkctrl_backend_ble *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, 0);

	return (jlong) (uintptr_t) arsdkctrl_backend_ble_get_parent(self);
}

/** Documented in public header. */
int arsdk_backend_ble_connection_open_jni(void *jself, const char *address,
		struct arsdk_device_conn *conn)
{
	RETURN_ERRNO_IF_FAILED(jself != NULL, -EINVAL);

	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_ERRNO_IF_FAILED(env != NULL, res);

	jstring jaddress = (*env)->NewStringUTF(env, address);
	RETURN_ERRNO_IF_FAILED(jaddress != NULL, -ENOMEM);

	jboolean ret = (*env)->CallBooleanMethod(env, (jobject)(uintptr_t)jself,
			s_jni_cache.jmid_backend_open_connection, jaddress,
			(jlong)(uintptr_t) conn);

	(*env)->DeleteLocalRef(env, jaddress);

	return ret == JNI_TRUE ? 0 : -EINVAL;
}

/** Documented in public header. */
void arsdk_backend_ble_connection_close_jni(void *jself, const char *address)
{
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jstring jaddress = (*env)->NewStringUTF(env, address);
	RETURN_IF_FAILED(jaddress != NULL, -ENOMEM);

	(*env)->CallVoidMethod(env, (jobject)(uintptr_t)jself,
			s_jni_cache.jmid_backend_close_connection, jaddress);

	(*env)->DeleteLocalRef(env, jaddress);
}
