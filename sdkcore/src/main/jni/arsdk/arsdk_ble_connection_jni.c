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

#include "arsdk_ble_connection_jni.h"

#include "arsdk_ble_connection.h"

#define ARSDK_LOG_TAG backend
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/** Static JNI method id cache */
static struct
{
	/* on ArsdkBleConnection */
	jmethodID jmid_connection_send_data; /**< sendData method */
} s_jni_cache;

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer.
 * @param[in] type: class where this static java method exist.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeClassInit(
		JNIEnv *env, jclass type)
{
	s_jni_cache.jmid_connection_send_data = (*env)->GetMethodID(env, type,
			"sendData", "(BBBLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V");
}

/**
 * Initializes the java connection handler. Called from ArsdkBleConnection
 * constructor.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection handler ref.
 * @param[in] connNativePtr: pointer to the native connection instance.
 * @return the pointer to the native connection instance if the java connection
 *         was properly initialized, 0 otherwise.
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeInit(
	JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, 0);

    jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	arsdk_ble_connection_attach_jself(self, jself);

	return connNativePtr;
}

/**
 * Releases the java connection handler. Called from
 * ArsdkBleConnection.close().
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection handler ref.
 * @param[in] connNativePtr: pointer to the native connection instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeRelease(
		JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	(*env)->DeleteGlobalRef(env, arsdk_ble_connection_detach_jself(self));

	/* dispose of native connection proxy */
	arsdk_ble_connection_destroy(self);
}

/**
 * Notifies that the device managed by the connection is disconnected.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection ref
 * @param[in] connNativePtr: pointer to the native connection instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeDisconnected(
		JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_ble_connection_disconnected(self));
}

/**
 * Notifies that the device managed by the connection is connecting.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection ref
 * @param[in] connNativePtr: pointer to the native connection instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeConnecting(
		JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_ble_connection_connecting(self));
}

/**
 * Notifies that the device managed by the connection is connected.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection ref
 * @param[in] connNativePtr: pointer to the native connection instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeConnected(
		JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_ble_connection_connected(self));
}

/**
 * Notifies that the device managed by the connection failed to connect.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection ref
 * @param[in] connNativePtr: pointer to the native connection instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeConnectionFailed(
		JNIEnv *env, jobject jself, jlong connNativePtr)
{
	struct arsdk_device_conn *self =
			(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_ble_connection_failed(self));
}

/**
 * Transmits received data from the BLE device managed by the connection.
 * @param[in] env: JNI env pointer.
 * @param[in] jself: java connection ref
 * @param[in] connNativePtr: pointer to the native connection instance.
 * @param[in] id: data header id.
 * @param[in] buffer: data (header + payload). DirectByteBuffer object.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ble_ArsdkBleConnection_nativeReceiveData(
		JNIEnv *env, jobject jself, jlong connNativePtr, jbyte id,
		jobject jbuffer)
{
	struct arsdk_device_conn *self =
		(struct arsdk_device_conn *)(uintptr_t) connNativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	uint8_t *buffer = (uint8_t *)(*env)->GetDirectBufferAddress(env, jbuffer);
	RETURN_IF_FAILED(buffer != NULL, -EINVAL);

	size_t len = (size_t) (*env)->GetDirectBufferCapacity(env, jbuffer) - 2;
	RETURN_IF_FAILED(len > 0, -EINVAL);

	struct arsdk_transport_header header = {
		.type = (enum arsdk_transport_data_type) buffer[0],
		.id = (uint8_t) id,
		.seq = buffer[1],
	};

	LOG_IF_ERR(arsdk_ble_connection_receive_data(self, &header, &buffer[2],
			len));
}

/** Documented in public header. */
void arsdk_ble_connection_send_data_jni(void *jself,
		const struct arsdk_transport_header *header,
		const struct arsdk_transport_payload *payload, const void *extra_hdr,
		size_t extra_hdrlen)
{
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jpayload = (*env)->NewDirectByteBuffer(env,
			(void *) payload->cdata, (jlong) payload->len);
	RETURN_IF_FAILED(jpayload != NULL, -ENOMEM);

	jobject jextra_hdr = (*env)->NewDirectByteBuffer(env, (void *) extra_hdr,
			(jlong) extra_hdrlen);
	GOTO_IF_FAILED(jextra_hdr != NULL, -ENOMEM, out);

	(*env)->CallVoidMethod(env, (jobject) jself,
			s_jni_cache.jmid_connection_send_data, (jbyte) header->id,
			(jbyte) header->type, (jbyte) header->seq, jpayload, jextra_hdr);

out:
	(*env)->DeleteLocalRef(env, jpayload);
	(*env)->DeleteLocalRef(env, jextra_hdr);
}
