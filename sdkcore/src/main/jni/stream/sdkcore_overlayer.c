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

#include "sdkcore_overlayer.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** SdkCoreOverlayer native backend. */
struct sdkcore_overlayer {
	/** Callbacks. */
	struct sdkcore_overlayer_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
};

/** Documented in public header. */
struct sdkcore_overlayer *sdkcore_overlayer_create(
		const struct sdkcore_overlayer_cbs *cbs,
		void *userdata)
{
	RETURN_VAL_IF_FAILED(cbs != NULL && cbs->on_overlay != NULL, -EINVAL, NULL);

	struct sdkcore_overlayer *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->cbs = *cbs;
	self->userdata = userdata;

	return self;
}

/** Documented in public header. */
int sdkcore_overlayer_overlay(struct sdkcore_overlayer *self,
		const struct pdraw_rect *render_zone,
		const struct pdraw_rect *content_zone,
		const struct pdraw_session_info *session_info,
		const struct vmeta_session *session_meta,
		const struct vmeta_frame *frame_meta,
		const struct pdraw_video_frame_extra *extra)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(render_zone != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(content_zone != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(session_info != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(session_meta != NULL, -EINVAL);

	self->cbs.on_overlay(render_zone, content_zone, session_info, session_meta,
			frame_meta, extra, self->userdata);

	return 0;
}

/** Documented in public header. */
int sdkcore_overlayer_destroy(struct sdkcore_overlayer *self,
		void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
