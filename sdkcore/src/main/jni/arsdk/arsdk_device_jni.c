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

#include "arsdkcore_device.h"

#define ARSDK_LOG_TAG device
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

#include <arsdkctrl/arsdkctrl.h>
#include <arsdkctrl/internal/arsdkctrl_internal.h>

/** Static JNI method id cache */
static struct
{
	/** on ArsdkDevice */
	jmethodID jmid_device_connecting;          /**< onConnecting */
	jmethodID jmid_device_connected;           /**< onConnected */
	jmethodID jmid_device_disconnected;        /**< onDisconnected */
	jmethodID jmid_device_connection_canceled; /**< onConnectionCanceled */
	jmethodID jmid_device_link_down;           /**< onLinkDown */
	jmethodID jmid_command_received;           /**< onCommandReceived */
	jmethodID jmid_no_ack_cmd_timer_tick;      /**< onNoAckCmdTimerTick */
} s_jni_cache;

static void device_connecting(void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_connecting);
}

static void device_connected(enum arsdk_device_api api, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_connected, (jint)api);
}

static void device_disconnected(int removing, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_disconnected,
			(jboolean) removing != 0);
}

static void device_connection_canceled(int removing,
        const enum arsdk_conn_cancel_reason reason, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_device_connection_canceled, (jint) reason,
			(jboolean) removing != 0);
}

static void device_link_status_changed(enum arsdk_link_status status,
        void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	// TODO: for now just pass link status KO to java.
	// We need to clarify what to do and how reconnection works.
	if (status == ARSDK_LINK_STATUS_KO) {
		(*env)->CallVoidMethod(env, (jobject) userdata,
				s_jni_cache.jmid_device_link_down);
	}
}

static void command_received(const struct arsdk_cmd *cmd, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
			s_jni_cache.jmid_command_received, (jlong) (uintptr_t) cmd);
}

static void no_ack_cmd_timer_tick(void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, (jobject) userdata,
				s_jni_cache.jmid_no_ack_cmd_timer_tick);
}

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_device_connecting = (*env)->GetMethodID(env, clazz,
			"onConnecting", "()V");
	s_jni_cache.jmid_device_connected = (*env)->GetMethodID(env, clazz,
			"onConnected", "(I)V");
	s_jni_cache.jmid_device_disconnected = (*env)->GetMethodID(env, clazz,
			"onDisconnected", "(Z)V");
	s_jni_cache.jmid_device_connection_canceled = (*env)->GetMethodID(env,
			clazz, "onConnectionCanceled", "(IZ)V");
	s_jni_cache.jmid_device_link_down = (*env)->GetMethodID(env, clazz,
			"onLinkDown", "()V");
	s_jni_cache.jmid_command_received = (*env)->GetMethodID(env, clazz,
			"onCommandReceived", "(J)V");
	s_jni_cache.jmid_no_ack_cmd_timer_tick = (*env)->GetMethodID(env, clazz,
			"onNoAckCmdTimerTick", "()V");
}

/**
 * Initializes the ArsdkDevice native backend.
 * @param[in] env: JNI env pointer
 * @param[in] jself: reference on java ArsdkDevice instance
 * @param[in] arsdkNativePtr: pointer to the native arsdkcore instance
 * @param[in] deviceHandle: native handle of the device
 * @return the pointer to the ArsdkDevice native backend instance if successful,
 *         0 otherwise
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeInit(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr, jshort deviceHandle)
{
	struct arsdkcore *arsdk = (struct arsdkcore *)(uintptr_t) arsdkNativePtr;
    RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct arsdkcore_device_cbs cbs = {
		.device_connecting = &device_connecting,
		.device_connected = &device_connected,
		.device_disconnected = &device_disconnected,
		.device_connection_canceled = &device_connection_canceled,
		.device_link_status_changed = &device_link_status_changed,
		.command_received = &command_received,
		.no_ack_cmd_timer_tick = &no_ack_cmd_timer_tick,
		.userdata = jself
	};

	struct arsdkcore_device *device = NULL;
	int res = arsdkcore_device_create(arsdk, (uint16_t) deviceHandle, &cbs,
			&device);

	GOTO_IF_FAILED(device != NULL, res, err);

	return (jlong) (uintptr_t) device;

err:
	(*env)->DeleteGlobalRef(env, jself);
	return 0;
}

/**
 * Connects the device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 * @return true if the connect is successful, otherwise false
 */
JNIEXPORT jboolean JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeConnect(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, JNI_FALSE);

	int res = arsdkcore_device_connect(self);
	LOG_IF_ERR(res);

	return res == 0;
}

/**
 * Sends a command to the device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 * @param[in] cmdNativePtr: pointer to the native arsdk command to send
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeSendCommand(
		JNIEnv *env, jclass clazz, jlong nativePtr, jlong cmdNativePtr)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) cmdNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	arsdkcore_device_send_command(self, command);
}

/**
 * Starts the pcmd timer.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 * @param[in] period: timer period, in milliseconds
 * Note: if successful, this method will register a timer that will call
 * java method onNoAckCmdTimerTick back regularly based on the provided period.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeStartNoAckCmdTimer(
		JNIEnv *env, jclass clazz, jlong nativePtr, jint period)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	arsdkcore_device_start_no_ack_cmd_timer(self, (uint32_t) period);
}

/**
 * Stops the pcmd timer.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 * Note: if successful, this method will stop calling back java method
 * onNoAckCmdTimerTick
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeStopNoAckCmdTimer(
		JNIEnv *env, jclass clazz, jlong nativePtr, jint period)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	arsdkcore_device_stop_no_ack_cmd_timer(self);
}

/**
 * Retrieves the Java connection/discovery backend controller associated with
 * this device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 * Note that not all Java backends associate themselves to the devices they
 * discover and allow to connect to, so this method may return NULL.
 * @return the associated Java connection/discovery backend controller, if any,
 * otherwise NULL
 */
JNIEXPORT jobject JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeGetBackendController(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	struct arsdkctrl_backend *backend = arsdkcore_device_get_backend(self);
	RETURN_VAL_IF_FAILED(backend != NULL, -ENODEV, NULL);

	return (jobject) arsdkctrl_backend_get_osdata(backend);
}

/**
 * Disconnects the device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeDisconnect(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdkcore_device_disconnect(self));
}

/**
 * Disposes of the ArsdkDevice native backend.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkDevice native backend instance
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkDevice_nativeDispose(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdkcore_device *self =
			(struct arsdkcore_device *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	void *jself = NULL;
	RETURN_IF_ERR(arsdkcore_device_destroy(self, &jself));

	(*env)->DeleteGlobalRef(env, (jobject) jself);
}
