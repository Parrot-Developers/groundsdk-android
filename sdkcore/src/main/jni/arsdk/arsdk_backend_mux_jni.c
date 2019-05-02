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

#include <libmux.h>

/** Struct tying java object and native impl together */
struct arsdk_backend_mux_jni {
	struct arsdkctrl_backend_mux   *backend;   /**< native backend */
	struct arsdk_discovery_mux     *discovery; /**< native discovery */
	jobject                         jself;     /**< ArsdkMuxBackend global ref */
};

/** Static JNI method id cache */
static struct
{
	/* on ArsdkMuxBackend */
	jmethodID jmid_mux_eof;  /**< onEof method */
} s_jni_cache;

/**
 * Notifies the java layer that an EOF or error condition occurred on the MUX
 * transport fd. Called back from mux pomp loop.
 * @param[in] mux_ctx: mux context where the condition occurred
 * @param[in] userdata: user data passed at mux creation, which is
 * struct arsdk_backend_mux_jni.
  */
static void mux_eof_cb(struct mux_ctx *mux_ctx, void *userdata)
{
	jobject jself = (jobject) userdata;
	RETURN_IF_FAILED(jself != NULL, -EINVAL);

	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->CallVoidMethod(env, jself, s_jni_cache.jmid_mux_eof);
}

/**
 * Notifies the java layer that the mux is closing and that userdata can be
 * cleaned.
 * @param[in] mux_ctx: mux context where the condition occurred
 * @param[in] userdata: user data passed at mux creation, which is
 * struct arsdk_backend_mux_jni.
  */
static void mux_release_cb(struct mux_ctx *mux_ctx, void *userdata)
{
	JNIEnv *env = NULL;
	int res = (*sdkcore_jvm)->GetEnv(sdkcore_jvm, (void **) &env,
			SDKCORE_JNI_VERSION);
	RETURN_IF_FAILED(env != NULL, res);

	(*env)->DeleteGlobalRef(env, userdata);
}

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer.
 * @param[in] type: class where this static java method exist.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_mux_ArsdkMuxBackend_nativeClassInit(
		JNIEnv *env, jclass type)
{
	s_jni_cache.jmid_mux_eof = (*env)->GetMethodID(env, type, "onEof", "()V");
}

/**
 * Initializes the java backend handler. Called from ArsdkMuxBackend
 * constructor.
 * @param[in] env: JNI env pointer.
 * @param[in] instance: java backend handler ref.
 * @param[in] arsdkNativePtr: pointer to the native arsdk manager instance.
 * @param[in] discoveryTypes: device types to enable in discovery
 * @param[in] fd: mux transport fd
 * @return the pointer to the native backend instance if the java backend was
 *         properly initialized, 0 otherwise.
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_mux_ArsdkMuxBackend_nativeInit(
		JNIEnv *env, jobject instance, jlong arsdkNativePtr,
		jintArray discoveryTypes, jint fd)
{
	struct arsdkcore *arsdk = (struct arsdkcore *)(uintptr_t) arsdkNativePtr;
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, 0);

	struct arsdk_backend_mux_jni *self =
			(struct arsdk_backend_mux_jni *) calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, 0);

	self->jself = (*env)->NewGlobalRef(env, instance);

	struct mux_ops ops = {
			.fdeof = &mux_eof_cb,
			.release = &mux_release_cb,
			.userdata = self->jself
	};

	struct arsdkctrl_backend_mux_cfg cfg = {
			.stream_supported = 1,
			.mux = mux_new(fd, arsdkcore_get_loop(arsdk), &ops,
					MUX_FLAG_FD_NOT_POLLABLE)
	};

	GOTO_IF_FAILED(cfg.mux != NULL, -ENOMEM, err);

	int res = arsdkctrl_backend_mux_new(arsdkcore_get_ctrl(arsdk), &cfg,
						&self->backend);

	GOTO_IF_FAILED(self->backend != NULL, res, err);

	struct arsdk_discovery_cfg discovery_cfg = {
			.types = (enum arsdk_device_type *)
					(*env)->GetIntArrayElements(env, discoveryTypes, NULL),
			.count = (uint8_t) (*env)->GetArrayLength(env, discoveryTypes)
	};

	res = arsdk_discovery_mux_new(arsdkcore_get_ctrl(arsdk), self->backend,
			&discovery_cfg, cfg.mux, &self->discovery);

	(*env)->ReleaseIntArrayElements(env, discoveryTypes,
			(jint *) discovery_cfg.types, JNI_ABORT);

	GOTO_IF_FAILED(self->discovery != NULL, res, err);

	return (jlong)(uintptr_t) self;

err:
	(*env)->DeleteGlobalRef(env, self->jself);

	if (cfg.mux) {
		mux_unref(cfg.mux);
	}

	if (self->backend) {
		LOG_IF_ERR(arsdkctrl_backend_mux_destroy(self->backend));
	}

	if (self->discovery) {
		LOG_IF_ERR(arsdk_discovery_mux_destroy(self->discovery));
	}

	free(self);

	return 0;
}

/**
 * Releases the java backend handler. Called from ArsdkMuxBackend.destroy().
 * @param[in] env: JNI env pointer.
 * @param[in] instance: java backend handler ref.
 * @param[in] nativePtr: pointer to the native backend instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_mux_ArsdkMuxBackend_nativeRelease(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_backend_mux_jni *self =
			(struct arsdk_backend_mux_jni *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct mux_ctx *mux = arsdkctrl_backend_mux_get_mux_ctx(self->backend);

	LOG_IF_ERR(mux_stop(mux));

	LOG_IF_ERR(arsdkctrl_backend_mux_destroy(self->backend));

	LOG_IF_ERR(arsdk_discovery_mux_destroy(self->discovery));

	mux_unref(mux);

	free(self);
}

/**
 * Starts mux discovery. Called from ArsdkMuxBackend.startDiscovery().
 * @param[in] env: JNI env pointer.
 * @param[in] instance: java backend handler ref.
 * @param[in] nativePtr: pointer to the native backend instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_mux_ArsdkMuxBackend_nativeStartDiscovery(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_backend_mux_jni *self =
			(struct arsdk_backend_mux_jni *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_discovery_mux_start(self->discovery));
}

/**
 * Stops mux discovery. Called from ArsdkMuxBackend.stopDiscovery().
 * @param[in] env: JNI env pointer.
 * @param[in] instance: java backend handler ref.
 * @param[in] nativePtr: pointer to the native backend instance.
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_backend_mux_ArsdkMuxBackend_nativeStopDiscovery(
		JNIEnv *env, jobject instance, jlong nativePtr)
{
	struct arsdk_backend_mux_jni *self =
			(struct arsdk_backend_mux_jni *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(arsdk_discovery_mux_stop(self->discovery));
}
