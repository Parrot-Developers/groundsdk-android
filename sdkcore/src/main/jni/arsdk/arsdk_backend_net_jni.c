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

/** Static JNI method id cache */
static struct
{
	/* on ArsdkNetBackend */
	jmethodID jmid_backend_socket_created;  /**< onSocketCreated method */
} s_jni_cache;

/** Struct tying java object and native impl together */
struct arsdk_backend_net_jni {
	struct arsdkctrl_backend_net *backend; /**< native backend */
	jobject                       jself;   /**< global ArsdkNetBackend java ref */
};

static void socket_created(struct arsdkctrl_backend_net *self, int fd,
		enum arsdk_socket_kind kind, void *userdata)
{
	jobject jself = (jobject) userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_backend_socket_created,
			(jint) fd);
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_net_ArsdkNetBackend_nativeClassInit(
		JNIEnv *env, jclass type)
{
	s_jni_cache.jmid_backend_socket_created = (*env)->GetMethodID(env, type,
			"onSocketCreated", "(I)V");
}

JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_net_ArsdkNetBackend_nativeInit(
		JNIEnv *env, jobject instance, jlong arsdkNativePtr, jobject controller)
{
	struct arsdkcore *arsdk = (struct arsdkcore *)(uintptr_t)arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	struct arsdkctrl_backend_net_cfg cfg = {
		.stream_supported = 1,
		.proto_v_max = 1,
	};

	struct arsdk_backend_net_jni *self =
			(struct arsdk_backend_net_jni *) calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, 0);

	self->jself = (*env)->NewGlobalRef(env, instance);

	jobject jcontroller = (*env)->NewGlobalRef(env, controller);

	int res = arsdkctrl_backend_net_new(arsdkcore_get_ctrl(arsdk), &cfg,
			&self->backend);
	GOTO_IF_FAILED(self->backend != NULL, res, err);

	arsdkctrl_backend_set_osdata(
			arsdkctrl_backend_net_get_parent(self->backend),
			(void *) jcontroller);

	res = arsdkctrl_backend_net_set_socket_cb(self->backend, &socket_created,
			self->jself);
	GOTO_IF_ERR(res, err);

	return (jlong)(uintptr_t)self;

err:
	(*env)->DeleteGlobalRef(env, self->jself);
	(*env)->DeleteGlobalRef(env, jcontroller);

	if (self->backend) {
		LOG_IF_ERR(arsdkctrl_backend_net_destroy(self->backend));
	}

	free(self);

	return 0;
}

JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_net_ArsdkNetBackend_nativeRelease(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_backend_net_jni *self =
			(struct arsdk_backend_net_jni *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	(*env)->DeleteGlobalRef(env, self->jself);

	struct arsdkctrl_backend *backend =
			arsdkctrl_backend_net_get_parent(self->backend);

	(*env)->DeleteGlobalRef(env,
			(jobject) arsdkctrl_backend_get_osdata(backend));

	arsdkctrl_backend_set_osdata(backend, (void *) NULL);

	LOG_IF_ERR(arsdkctrl_backend_net_destroy(self->backend));

	free(self);
}

JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_net_ArsdkNetBackend_nativeGetParent(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_backend_net_jni *self =
			(struct arsdk_backend_net_jni *)(uintptr_t)nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, 0);

	return (jlong)(uintptr_t) arsdkctrl_backend_net_get_parent(self->backend);
}
