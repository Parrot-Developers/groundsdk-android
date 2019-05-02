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

#define ARSDK_LOG_TAG device
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/**
 * Creates tcp proxy to the device.
 * @param[in] env: JNI env pointer
 * @param[in] jself: reference on java ArsdkTcpProxy instance
 * @param[in] arsdkNativePtr: pointer to the native arsdkcore instance
 * @param[in] deviceHandle: native handle of the device that will handle the proxy
 * @param[in] deviceType: type of the device to access by the proxy
 * @param[in] port: port to access
 * @return the pointer to the ArsdkTcpProxy native backend instance if
 *         successful, 0 otherwise
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeCreate(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr, jshort deviceHandle,
		jint deviceType, jint port)
{
	struct arsdkcore *arsdk = (struct arsdkcore *)(uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	struct arsdk_device *device = arsdkcore_get_device(arsdk,
	(uint16_t) deviceHandle);
	RETURN_VAL_IF_FAILED(device != NULL, -ENODEV, 0);

	struct arsdk_device_tcp_proxy *proxy = NULL;
	int res = arsdk_device_create_tcp_proxy(device,
			(enum arsdk_device_type) deviceType, port, &proxy);

	GOTO_IF_FAILED(proxy != NULL, res, err);

	return (jlong) (uintptr_t) proxy;

err:
	return 0;
}

/**
 * Gets proxy port.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkTcpProxy native
 *            backend instance
 * @return port of the proxy in case of success, negative errno value in case of error.
 */
JNIEXPORT jint JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeGetPort(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_device_tcp_proxy*proxy =
			(struct arsdk_device_tcp_proxy *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(proxy != NULL, -EINVAL, 0);

	return arsdk_device_tcp_proxy_get_port(proxy);
}

/**
 * Gets proxy address.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkTcpProxy native
 *            backend instance
 * @return address of the proxy in case of success, negative errno value in case of error.
 */
JNIEXPORT jstring JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeGetAddr(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_device_tcp_proxy *proxy =
			(struct arsdk_device_tcp_proxy *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(proxy != NULL, -EINVAL, 0);

	return (*env)->NewStringUTF(env, arsdk_device_tcp_proxy_get_addr(proxy));
}

/**
 * Close a tcp proxy to the device.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the ArsdkTcpProxy native
 *            backend instance
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeClose(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_device_tcp_proxy *proxy =
			(struct arsdk_device_tcp_proxy *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(proxy != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_device_destroy_tcp_proxy(proxy));
}
