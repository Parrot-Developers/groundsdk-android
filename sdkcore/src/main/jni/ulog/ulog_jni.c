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
 * Sends a log message to ULog.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] nativeCookie: pointer to the native ulog cookie (tag)
 * @param[in] jmsg: message string to log
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULog_nativeLog(
		JNIEnv *env, jclass clazz, jint prio, jlong nativeCookie, jstring jmsg)
{
	RETURN_IF_FAILED(jmsg != NULL, -EINVAL);

	struct ulog_cookie *cookie =
			(struct ulog_cookie *) (uintptr_t) nativeCookie;
	RETURN_IF_FAILED(cookie != NULL, -EINVAL);

	const char *msg = (*env)->GetStringUTFChars(env, jmsg, NULL);
	RETURN_IF_FAILED(msg != NULL, -ENOMEM);

	ulog_log_str((uint32_t) prio, cookie, msg);

	(*env)->ReleaseStringUTFChars(env, jmsg, msg);
}

/**
 * Sets the log level for the provided tag.
 * @param[in] env: JNI env pointer
 * @param[in] clazz: class where this static java method is defined
 * @param[in] jtag: name of the tag whose level must be set
 * @param[in] jmsg: level to set for this tag
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_ulog_ULog_nativeSetTagLevel(
		JNIEnv *env, jclass clazz, jstring jtag, jint level)
{
	RETURN_IF_FAILED(jtag != NULL, -EINVAL);

	const char *tag = (*env)->GetStringUTFChars(env, jtag, NULL);
	RETURN_IF_FAILED(tag != NULL, -ENOMEM);

	LOG_IF_ERR(ulog_set_tag_level(tag, level));

	(*env)->ReleaseStringUTFChars(env, jtag, tag);
}
