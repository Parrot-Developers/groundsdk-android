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
#include <sdkcore/sdkcore_log.h>

/** Global Java VM pointer. Initialized in JNI_OnLoad. */
JavaVM * const sdkcore_jvm;

/**
 * Called once by Java VM when sdkcore library is loaded.
 * @param[in] jvm: global Java VM pointer
 * @param[in] reserved: Java implementation reserved
 * @return sdkcore required JNI version
 */
JNIEXPORT jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
	*((JavaVM **) &sdkcore_jvm) = jvm;
	return SDKCORE_JNI_VERSION;
}

/**
 * Called when the class loader containing sdkcore library is GCed.
 * Note: virtually never called
 * @param[in] jvm: global Java VM pointer
 * @param[in] reserved: Java implementation reserved
 */
JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved)
{
	*((JavaVM **) &sdkcore_jvm) = NULL;
}
