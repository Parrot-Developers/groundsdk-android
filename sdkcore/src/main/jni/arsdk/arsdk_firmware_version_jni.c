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

#include "arsdk_log.h"

#include <sdkcore/sdkcore_jni.h>

#include <libpuf.h>

/** Static JNI method id cache */
static struct
{
	/** on ArsdkFirmwareVersion */
	jmethodID jmid_ctor; /** Constructor */
} s_jni_cache;

/**
 * Initializes the static method id cache. Called once from static java block.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_firmware_ArsdkFirmwareVersion_nativeClassInit(
		JNIEnv *env, jclass clazz)
{
	s_jni_cache.jmid_ctor = (*env)->GetMethodID(env, clazz,
			"<init>", "(IIIII)V");
}

/**
 * Instantiates an ArsdkFirmwareVersion from a firmware version string.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] versionStr: firmware version string to parse
 * @return the created ArsdkFirmwareInfo instance if successful, null otherwise
 */
JNIEXPORT jobject JNICALL
Java_com_parrot_drone_sdkcore_arsdk_firmware_ArsdkFirmwareVersion_nativeFromString(
		JNIEnv *env, jclass clazz, jstring versionStr)
{
	const char *version_str = (*env)->GetStringUTFChars(env, versionStr, NULL);
	RETURN_VAL_IF_FAILED(version_str != NULL, -ENOMEM, NULL);

	struct puf_version version;
	int res = puf_version_fromstring(version_str, &version);
	(*env)->ReleaseStringUTFChars(env, versionStr, version_str);

	RETURN_VAL_IF_ERR(res, NULL);

	return (*env)->NewObject(env, clazz, s_jni_cache.jmid_ctor,
			(jint) version.type, (jint) version.major, (jint) version.minor,
			(jint) version.patch, (jint) version.build);
}

/**
 * Initializes an ArsdkFirmwareVersion from a native arsdk firmware version
 * pointer
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk firmware version
 * @return the pointer to the ArsdkFirmwareVersion instance if successful,
 *         null otherwise
 */
JNIEXPORT jobject JNICALL
Java_com_parrot_drone_sdkcore_arsdk_firmware_ArsdkFirmwareVersion_nativeCreate(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct puf_version *version = (struct puf_version *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(version != NULL, -EINVAL, 0);

	return (*env)->NewObject(env, clazz, s_jni_cache.jmid_ctor,
			(jint) version->type, (jint) version->major, (jint) version->minor,
			(jint) version->patch, (jint) version->build);
}

/**
 * Compares two firmware versions.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] lhsType: type of the first firmware version to compare
 * @param[in] lhsMajor: major number of the first firmware version to compare
 * @param[in] lhsMinor: minor number of the first firmware version to compare
 * @param[in] lhsPatch: patch number of the first firmware version to compare
 * @param[in] lhsBuild: build number of the first firmware version to compare
 * @param[in] rhsType: type of the second firmware version to compare
 * @param[in] rhsMajor: major number of the second firmware version to compare
 * @param[in] rhsMinor: minor number of the second firmware version to compare
 * @param[in] rhsPatch: patch number of the second firmware version to compare
 * @param[in] rhsBuild: build number of the second firmware version to compare
 * @return 1 if first version is superior to second version, -1 if second
 *         version is superior to first version, 0 if both are equal
 */
JNIEXPORT jint JNICALL
Java_com_parrot_drone_sdkcore_arsdk_firmware_ArsdkFirmwareVersion_nativeCompare(
		JNIEnv *env, jclass clazz, jint lhsType, jint lhsMajor, jint lhsMinor,
		jint lhsPatch, jint lhsBuild, jint rhsType, jint rhsMajor,
		jint rhsMinor, jint rhsPatch, jint rhsBuild)
{
	struct puf_version lhs = {
		.type = (enum puf_version_type) lhsType,
		.major = (uint32_t) lhsMajor,
		.minor = (uint32_t) lhsMinor,
		.patch = (uint32_t) lhsPatch,
		.build = (uint32_t) lhsBuild
	};

	struct puf_version rhs = {
		.type = (enum puf_version_type) rhsType,
		.major = (uint32_t) rhsMajor,
		.minor = (uint32_t) rhsMinor,
		.patch = (uint32_t) rhsPatch,
		.build = (uint32_t) rhsBuild
	};

	return (jint) puf_compare_version(&lhs, &rhs);
}
