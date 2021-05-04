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

#pragma once

#include <pdraw/pdraw_defs.h>

/** SdkCoreOverlayer native backend. */
struct sdkcore_overlayer;

/** SdkCoreOverlayer native backend callbacks. */
struct sdkcore_overlayer_cbs {

	/**
	 * Called back when overlay may be applied.
	 * @param[in] render_zone: render zone
	 * @param[in] content_zone: content zone
	 * @param[in] session_info: PDRAW session info
	 * @param[in] session_meta: session metadata
	 * @param[in] frame_meta: frame metadata
	 * @param[in] extra: PDRAW frame extraneous data
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*on_overlay) (
			const struct pdraw_rect *render_zone,
			const struct pdraw_rect *content_zone,
			const struct pdraw_session_info *session_info,
			const struct vmeta_session *session_meta,
			const struct vmeta_frame *frame_meta,
			const struct pdraw_video_frame_extra *extra,
			void *userdata);
};

/**
 * Creates a new overlayer instance.
 * @param[in] cbs: overlayer callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new overlayer instance in case of success, NULL otherwise
 */
struct sdkcore_overlayer *sdkcore_overlayer_create(
		const struct sdkcore_overlayer_cbs *cbs,
		void *userdata);

/**
 * Requests overlay.
 * @param[in] self: overlayer instance to operate on
 * @param[in] render_zone: render zone
 * @param[in] content_zone: content zone
 * @param[in] session_info: PDRAW session info
 * @param[in] session_meta: session metadata
 * @param[in] frame_meta: frame metadata
 * @param[in] extra: PDRAW frame extraneous data
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_overlayer_overlay(
		struct sdkcore_overlayer *self,
		const struct pdraw_rect *render_zone,
		const struct pdraw_rect *content_zone,
		const struct pdraw_session_info *session_info,
		const struct vmeta_session *session_meta,
		const struct vmeta_frame *frame_meta,
		const struct pdraw_video_frame_extra *extra);

/**
 * Destroys overlayer.
 * @param[in] self: overlayer instance to destroy
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_overlayer_destroy(
		struct sdkcore_overlayer *self,
		void **userdata);
