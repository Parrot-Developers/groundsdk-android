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

#include <sdkcore/internal/sdkcore_rect_jni.h>

#include <sdkcore/sdkcore_log.h>

/** Static JNI id cache */
static struct
{
	/** android.graphics.Rect.left int field. */
	jfieldID jfid_left;
	/** android.graphics.Rect.top int field. */
	jfieldID jfid_top;
	/** android.graphics.Rect.right int field. */
	jfieldID jfid_right;
	/** android.graphics.Rect.bottom int field. */
	jfieldID jfid_bottom;
} s_jni_cache;

/**
 * Updates an android.graphics.Rect.left member.
 * @param[in] env: JNI env
 * @param[in] rect: Rect jobject to update
 * @param[in] left: left value to set
 */
static inline void sdkcore_rect_set_left(JNIEnv *env, jobject rect, int left)
{
	(*env)->SetIntField(env, rect, s_jni_cache.jfid_left, (jint) left);
}

/**
 * Updates an android.graphics.Rect.top member.
 * @param[in] env: JNI env
 * @param[in] rect: Rect jobject to update
 * @param[in] top: top value to set
 */
static inline void sdkcore_rect_set_top(JNIEnv *env, jobject rect, int top)
{
	(*env)->SetIntField(env, rect, s_jni_cache.jfid_top, (jint) top);
}

/**
 * Updates an android.graphics.Rect.right member.
 * @param[in] env: JNI env
 * @param[in] rect: Rect jobject to update
 * @param[in] right: right value to set
 */
static inline void sdkcore_rect_set_right(JNIEnv *env, jobject rect, int right)
{
	(*env)->SetIntField(env, rect, s_jni_cache.jfid_right, (jint) right);
}

/**
 * Updates an android.graphics.Rect.bottom member.
 * @param[in] env: JNI env
 * @param[in] rect: Rect jobject to update
 * @param[in] bottom: bottom value to set
 */
static inline void sdkcore_rect_set_bottom(JNIEnv *env, jobject rect,
		int bottom)
{
	(*env)->SetIntField(env, rect, s_jni_cache.jfid_bottom, (jint) bottom);
}

/** Documented in public header. */
int sdkcore_rect_set(JNIEnv *env, jobject rect, int x, int y, int width,
		int height)
{
	RETURN_ERRNO_IF_FAILED(rect != NULL, -EINVAL);

	sdkcore_rect_set_left(env, rect, x);
	sdkcore_rect_set_top(env, rect, y);
	sdkcore_rect_set_right(env, rect, x + width);
	sdkcore_rect_set_bottom(env, rect, y + height);

	return 0;
}

/**
 * Initializes the static JNI id cache. Called once from static java block.
 * @param[in] env: JNI env
 * @param[in] clazz: SdkCore class
 * @param[in] rectClazz: android.graphics.Rect class
 */
JNIEXPORT void JNICALL
Java_com_parrot_drone_sdkcore_SdkCore_nativeRectClassInit(JNIEnv *env, jclass clazz,
		jclass rectClazz)
{
	s_jni_cache.jfid_left = (*env)->GetFieldID(env, rectClazz, "left", "I");
	s_jni_cache.jfid_top = (*env)->GetFieldID(env, rectClazz, "top", "I");
	s_jni_cache.jfid_right = (*env)->GetFieldID(env, rectClazz, "right", "I");
	s_jni_cache.jfid_bottom = (*env)->GetFieldID(env, rectClazz, "bottom", "I");
}
