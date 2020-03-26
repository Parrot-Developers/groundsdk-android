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

#include <sdkcore/sdkcore_jni.h>

#define SDKCORE_LOG_TAG ulog
#include <sdkcore/sdkcore_log.h>

/**
 * Initializes a native ULogTag backend
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] jtag: name of the ULog tag
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULogTag_nativeInit(
		JNIEnv *env, jobject clazz, jstring jname)
{
	RETURN_VAL_IF_FAILED(jname != NULL, -EINVAL, 0);

	struct ulog_cookie *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, 0);

	const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
	self->name = strdup(name);
	(*env)->ReleaseStringUTFChars(env, jname, name);
	GOTO_IF_FAILED(self->name != NULL, -ENOMEM, err);

	self->namesize = strlen(self->name) + 1;
	self->level = -1;

	ulog_init(self);

	return (jlong) (uintptr_t) self;

err:
	free(self);

	return 0;
}

/**
 * Disposes of a native ULogTag backend
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: native ptr to the nativeULogTag backend to dispose
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULogTag_nativeDispose(
		JNIEnv *env, jobject clazz, jlong nativePtr)
{
	struct ulog_cookie *self = (struct ulog_cookie *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	free((char *) self->name);

	free(self);
}

/**
 * Gets a view on the ULogTag level.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: native ptr to the nativeULogTag backend
 * @return a direct byte buffer mapped onto the internal ULog tag level.
 *         Reading the first integer from this buffer gets the up to date
 *         value of the tag level
 */
JNIEXPORT jobject JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULogTag_nativeGetLevel(
		JNIEnv *env, jobject clazz, jlong nativePtr)
{
	struct ulog_cookie *self = (struct ulog_cookie *)(uintptr_t)nativePtr;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return (*env)->NewDirectByteBuffer(env, &self->level, sizeof(self->level));
}

/**
 * Sets the ULogTag level.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: native ptr to the nativeULogTag backend
 * @param[in] level: level to set for this tag
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULogTag_nativeSetLevel(
		JNIEnv *env, jobject clazz, jlong nativePtr, jint level)
{
	struct ulog_cookie *self = (struct ulog_cookie *)(uintptr_t)nativePtr;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	ulog_set_level(self, level);
}
