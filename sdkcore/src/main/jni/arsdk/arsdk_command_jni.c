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
#include <stdlib.h>

/**
 * Initializes an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] otherCmdPtr: pointer to a native arsdk command to copy, NULL
 *            for an empty command
 * @return the pointer to the ArsdkCommand native backend instance if
 *         successful, otherwise 0
 */
JNIEXPORT jlong JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeInit(
		JNIEnv *env, jclass clazz, jlong otherCmdPtr)
{
	struct arsdk_cmd *command = calloc(1, sizeof(*command));
	RETURN_VAL_IF_FAILED(command != NULL, -ENOMEM, 0);

	if (otherCmdPtr == 0) {
		arsdk_cmd_init(command);
	} else {
		arsdk_cmd_copy(command, (struct arsdk_cmd *) (uintptr_t) otherCmdPtr);
	}

	return (jlong) (uintptr_t) command;
}

/**
 * Copies an ArsdkCommand to another.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] srcCmdPtr: pointer to the native arsdk command to copy from
 * @param[in] destCmdPtr: pointer to the native arsdk command to copy to
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeCopy(
		JNIEnv *env, jclass clazz, jlong srcCmdPtr, jlong destCmdPtr)
{
	struct arsdk_cmd *src = (struct arsdk_cmd *) (uintptr_t) srcCmdPtr;
	RETURN_IF_FAILED(src != NULL, -EINVAL);

	struct arsdk_cmd *dst = (struct arsdk_cmd *) (uintptr_t) destCmdPtr;
	RETURN_IF_FAILED(dst != NULL, -EINVAL);

	arsdk_cmd_copy(dst, src);
}

/**
 * Gets the feature identifier of an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 * @return the feature identifier if successful, a negative error code
 *         otherwise
 */
JNIEXPORT jint JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeGetFeatureId(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_ERRNO_IF_FAILED(command != NULL, -EINVAL);

	return ((jint) command->prj_id << 8) + command->cls_id;
}

/**
 * Gets the command identifier of an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 * @return the command identifier if successful, a negative error code
 *         otherwise
 */
JNIEXPORT jint JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeGetCommandId(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_ERRNO_IF_FAILED(command != NULL, -EINVAL);

	return command->cmd_id;
}

/**
 * Gets the name of an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 * @return the command name if successful, otherwise null
 */
JNIEXPORT jstring JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeGetName(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(command != NULL, -EINVAL, 0);

	const char *name = arsdk_cmd_get_name(command);
	RETURN_VAL_IF_FAILED(name != NULL, -ENODEV, 0);

	return (*env)->NewStringUTF(env, name);
}

/**
 * Gets the internal data of an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 * @return a DirectByteBuffer onto the command internal data if successful,
 *         otherwise null
 */
JNIEXPORT jobject JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeGetData(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_VAL_IF_FAILED(command != NULL, -EINVAL, 0);
	RETURN_VAL_IF_FAILED(command->buf != NULL, -EINVAL, 0);

	const void *cdata = NULL;
	size_t len = 0, capacity = 0;
	pomp_buffer_get_cdata(command->buf, &cdata, &len, &capacity);

	RETURN_VAL_IF_FAILED(cdata != NULL, -ENODEV, 0);

	return (*env)->NewDirectByteBuffer(env, (void *) cdata, (jlong) len);
}

/**
 * Sets the internal data of an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 * @param[in] buffer: a DirectByteBuffer onto the data to set
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeSetData(
		JNIEnv *env, jclass clazz, jlong nativePtr, jobject jbuffer)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(command != NULL, -EINVAL);

	void *buffer = (*env)->GetDirectBufferAddress(env, jbuffer);
	RETURN_IF_FAILED(buffer != NULL, -EINVAL);

	size_t size = (size_t) (*env)->GetDirectBufferCapacity(env, jbuffer);

	command->buf = pomp_buffer_new_with_data(buffer, size);
	arsdk_cmd_dec_header(command);
}

/**
 * Releases an ArsdkCommand.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativePtr: pointer to the native arsdk command
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeRelease(
		JNIEnv *env, jclass clazz, jlong nativePtr)
{
	struct arsdk_cmd *command = (struct arsdk_cmd *) (uintptr_t) nativePtr;
	RETURN_IF_FAILED(command != NULL, -EINVAL);

	arsdk_cmd_clear(command);
	free(command);
}

/**
 * Gets an ArsdkCommand name given its feature and command identifiers.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] featureId: feature identifier of the command
 * @param[in] commandId: command identifier of the command
 * @return the command name if successful, otherwise null
 */
JNIEXPORT jstring JNICALL
Java_com_parrot_drone_sdkcore_arsdk_command_ArsdkCommand_nativeGetCmdName(
		JNIEnv *env, jclass clazz, jshort featureId, jshort commandId)
{
	struct arsdk_cmd command;

	arsdk_cmd_init(&command);

	command.prj_id = (uint8_t) (featureId >> 8);
	command.cls_id = (uint8_t) (featureId & 0x00FF);
	command.cmd_id = (uint8_t) commandId;

	const char *name = arsdk_cmd_get_name(&command);
	RETURN_VAL_IF_FAILED(name != NULL, -ENODEV, 0);

	return (*env)->NewStringUTF(env, name);
}
