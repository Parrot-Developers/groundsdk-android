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

#include "arsdkcore_tcp_proxy.h"

#define ARSDK_LOG_TAG device
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

/** Static JNI id cache */
static struct
{
	/* on ArsdkTcpProxy */
	jmethodID jmid_on_open; /**< onOpen */
} s_jni_cache;

/**
 * Calls ArsdkTcpProxy.onOpen.
 * @param[in] error: 0 in case of open success, a negative errno otherwise
 * @param[in] address: proxy local address in case of open success, NULL
 *                     otherwise
 * @param[in] port: proxy local port in case of open success, undefined
 *                  otherwise
 * @param[in] userdata: ArsdkTcpProxy jobject
 */
static void on_open(int error, const char *address, uint16_t port,
		void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	jobject jself = (jobject) (uintptr_t) userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_on_open,
			error == 0 ? (*env)->NewStringUTF(env, address) : NULL,
			(jint) port);
}

/**
 * Initializes ArsdkTcpProxy native backend and open proxy.
 * @param[in] env: JNI env
 * @param[in] jself: ArsdkTcpProxy jobject
 * @param[in] arsdkNativePtr: ArsdkCore native backend.
 * @param[in] deviceHandle: handle of the device that will handle the proxy
 * @param[in] deviceType: type of device to be accessed through the proxy
 * @param[in] port: remote port to proxy
 * @return a new ArsdkTcpProxy native backend in case of success, otherwise NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeOpen(
		JNIEnv *env, jobject jself, jlong arsdkNativePtr, jshort deviceHandle,
		jint deviceType, jint port)
{
	struct arsdkcore *arsdk = (struct arsdkcore *) (uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	RETURN_VAL_IF_FAILED(port >= 0 && port <= UINT16_MAX, -EINVAL, 0);

	jself = (*env)->NewGlobalRef(env, jself);
	RETURN_VAL_IF_FAILED(jself != NULL, -ENOMEM, 0);

	struct arsdkcore_tcp_proxy_cbs cbs = {
		.on_open = on_open,
	};

	struct arsdkcore_tcp_proxy *self = arsdkcore_tcp_proxy_create(arsdk,
			(uint16_t) deviceHandle, (enum arsdk_device_type) deviceType,
			(uint16_t) port, &cbs, jself);
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err);

	return (jlong) (uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, jself);

	return 0;
}

/**
 * Closes proxy and destroys ArsdkTcpProxy native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: ArsdkTcpProxy class
 * @param[in] nativePtr: ArsdkMuxBackend native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeClose(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdkcore_tcp_proxy *self =
			(struct arsdkcore_tcp_proxy *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	jobject jself = NULL;
	RETURN_IF_ERR(arsdkcore_tcp_proxy_destroy(self, (void**) &jself));

	(*env)->DeleteGlobalRef(env, jself);
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: ArsdkMuxBackend class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_device_ArsdkTcpProxy_nativeClassInit(JNIEnv *env,
		jclass clazz)
{
	s_jni_cache.jmid_on_open = (*env)->GetMethodID(env, clazz,
			"onOpen", "(Ljava/lang/String;I)V");
}
