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

#define ARSDK_LOG_TAG backend
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

#include <arsdkctrl/internal/arsdkctrl_internal.h>

JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeNew(
		JNIEnv *env, jobject instance, jlong arsdkctl_native,
		jstring jname, jlong backend_native)
{
	struct arsdkcore *arsdkctl =
		(struct arsdkcore *)(uintptr_t)arsdkctl_native;
	RETURN_VAL_IF_FAILED(arsdkctl != NULL, -EINVAL, 0);

	struct arsdkctrl_backend *backend =
			(struct arsdkctrl_backend *)(uintptr_t)backend_native;
	RETURN_VAL_IF_FAILED(backend != NULL, -EINVAL, 0);

	const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
	RETURN_VAL_IF_FAILED(name != NULL, -EINVAL, 0);

	/* create discovery */
	struct arsdk_discovery *discovery = NULL;
	int res = arsdk_discovery_new(name, backend, arsdkcore_get_ctrl(arsdkctl),
			&discovery);
	LOG_IF_FAILED(discovery != NULL, res);

	(*env)->ReleaseStringUTFChars(env, jname, name);

	return (jlong)(uintptr_t)discovery;
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeRelease(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_discovery *self =
			(struct arsdk_discovery *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_discovery_destroy(self));
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeStart(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_discovery *self =
		(struct arsdk_discovery *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_discovery_start(self));
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeStop(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_discovery *self =
			(struct arsdk_discovery *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_discovery_stop(self));
}


JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeAddDevice(
		JNIEnv *env, jobject instance, jlong nativePtr, jstring jname,
		jint type, jstring jaddr, jint port, jstring jid)
{
	struct arsdk_discovery *self =
			(struct arsdk_discovery *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct arsdk_discovery_device_info info = {
			.name = (*env)->GetStringUTFChars(env, jname, NULL),
			.addr = (*env)->GetStringUTFChars(env, jaddr, NULL),
			.id = (*env)->GetStringUTFChars(env, jid, NULL),
			.type = (enum arsdk_device_type) type,
			.port = (uint16_t) port
	};

	GOTO_IF_FAILED(info.name != NULL, -ENOMEM, out);
	GOTO_IF_FAILED(info.addr != NULL, -ENOMEM, out);
	GOTO_IF_FAILED(info.id != NULL, -ENOMEM, out);

	LOG_IF_ERR(arsdk_discovery_add_device(self, &info));

out:
	if (info.name)
		(*env)->ReleaseStringUTFChars(env, jname, info.name);
	if (info.addr)
		(*env)->ReleaseStringUTFChars(env, jaddr, info.addr);
	if (info.id)
		(*env)->ReleaseStringUTFChars(env, jid, info.id);
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_ArsdkDiscovery_nativeRemoveDevice(
		JNIEnv *env, jobject instance, jlong nativePtr, jstring jname,
		jint type)
{
	struct arsdk_discovery *self =
			(struct arsdk_discovery *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct arsdk_discovery_device_info info = {
			.name = (*env)->GetStringUTFChars(env, jname, NULL),
			.type = (enum arsdk_device_type) type
	};

	RETURN_IF_FAILED(info.name != NULL, -ENOMEM);

	LOG_IF_ERR(arsdk_discovery_remove_device(self, &info));

	(*env)->ReleaseStringUTFChars(env, jname, info.name);
}
