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

#define ARSDK_LOG_TAG command
#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

#include <arsdkctrl/arsdkctrl.h>
#include <arsdkcore_multiset.h>

/**
 * Initializes an ArsdkMultiset.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] otherMultisetPtr: pointer to a native arsdk command to copy, NULL
 *            for an empty multiset
 * @return the pointer to the ArsdkMultiset native backend instance if
 *         successful, otherwise 0
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkMultiset_nativeInit(
		JNIEnv *env, jclass clazz, jlong otherMultisetPtr)
{
	union arsdkcore_multiset *multiset = calloc(1, sizeof(*multiset));
	RETURN_VAL_IF_FAILED(multiset != NULL, -ENOMEM, 0);

	if (otherMultisetPtr != 0) {
		memcpy(multiset,
				(union arsdkcore_multiset *) (uintptr_t) otherMultisetPtr,
				sizeof(*multiset));
	}

	return (jlong) (uintptr_t) multiset;
}

/**
 * Copies an ArsdkMultiset to another.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] srcMultisetPtr: pointer to the native multiset to copy from
 * @param[in] destMultisetPtr: pointer to the native multiset to copy to
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkMultiset_nativeCopy(JNIEnv *env,
		jclass clazz, jlong srcMultisetPtr, jlong destMultisetPtr)
{
	union arsdkcore_multiset *src =
			(union arsdkcore_multiset *) (uintptr_t) srcMultisetPtr;
	RETURN_IF_FAILED(src != NULL, -EINVAL);

	union arsdkcore_multiset *dst =
			(union arsdkcore_multiset *) (uintptr_t) destMultisetPtr;
	RETURN_IF_FAILED(dst != NULL, -EINVAL);

	memcpy(dst, src, sizeof(*src));
}

/**
 * Compares two ArsdkMultisets.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] lhsMultisetPtr: pointer to the first native multiset to compare
 * @param[in] destMultisetPtr: pointer to the second native multiset to compare
 * @return 0 if both ArsdkMultisets are the same, a strictly negative or
 *           positive value otherwise
 */
JNIEXPORT jint JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkMultiset_nativeCmp(JNIEnv *env,
		jclass clazz, jlong lhsMultisetPtr, jlong rhsMultisetPtr)
{
	union arsdkcore_multiset *lhs =
			(union arsdkcore_multiset *) (uintptr_t) lhsMultisetPtr;
	RETURN_ERRNO_IF_FAILED(lhs != NULL, -EINVAL);
	union arsdkcore_multiset *rhs =
			(union arsdkcore_multiset *) (uintptr_t) rhsMultisetPtr;
	RETURN_ERRNO_IF_FAILED(rhs != NULL, -EINVAL);

	return memcmp(lhs, rhs, sizeof(*lhs));
}

/**
 * Releases an ArsdkMultiset.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk multiset
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_ArsdkMultiset_nativeRelease(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	union arsdkcore_multiset *multiset =
			(union arsdkcore_multiset *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(multiset != NULL, -EINVAL);

	free(multiset);
}
