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

#include <sdkcore/internal/sdkcore_pomp.h>

#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG pomp
#include <sdkcore/sdkcore_log.h>

/**
 * Initializes SdkCorePomp native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCorePomp class
 * @param[in] contextFlag: direct ByteBuffer whose first byte is the context
 *                         flag to use; may be NULL
 * @return a new SdkCorePomp native backend in case of success, otherwise NULL
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_pomp_SdkCorePomp_nativeInit(
		JNIEnv *env, jclass clazz, jobject contextFlag)
{
	char *flag = NULL;
	if (contextFlag) {
		flag = (*env)->GetDirectBufferAddress(env, contextFlag);
		LOG_IF_FAILED(flag != NULL, -EINVAL);
	}

	struct sdkcore_pomp *self = sdkcore_pomp_create(flag);
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, 0);

	return (jlong) (uintptr_t) self;
}

/**
 * Destroys SdkCorePomp native backend.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCorePomp class
 * @param[in] nativePtr: SdkCorePomp native backend
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_pomp_SdkCorePomp_nativeDispose(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct sdkcore_pomp *self = (struct sdkcore_pomp *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(sdkcore_pomp_destroy(self));
}
