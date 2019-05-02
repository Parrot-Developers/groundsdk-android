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

#include "sdkcore_texture_loader.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** SdkCoreTextureLoader native backend. */
struct sdkcore_texture_loader {
	/** Texture specifications. */
	struct sdkcore_texture_loader_texture_spec texture_spec;
	/** Callbacks. */
	struct sdkcore_texture_loader_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
};

/** Documented in public header. */
struct sdkcore_texture_loader *sdkcore_texture_loader_create(
		const struct sdkcore_texture_loader_texture_spec *texture_spec,
		const struct sdkcore_texture_loader_cbs *cbs,
		void *userdata)
{
	RETURN_VAL_IF_FAILED(texture_spec != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(   cbs != NULL
	                     && cbs->on_load_texture != NULL, -EINVAL, NULL);

	struct sdkcore_texture_loader *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->texture_spec = *texture_spec;
	self->cbs = *cbs;
	self->userdata = userdata;

	return self;
}

/** Documented in public header. */
const struct sdkcore_texture_loader_texture_spec *
sdkcore_texture_loader_texture_spec(struct sdkcore_texture_loader *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return &self->texture_spec;
}

/** Documented in public header. */
int sdkcore_texture_loader_load_texture(struct sdkcore_texture_loader *self,
	const struct sdkcore_texture_loader_texture_size *texture_size,
	const struct pdraw_video_frame *frame,
	const struct sdkcore_texture_loader_frame_userdata *frame_userdata,
	const struct vmeta_session *session_meta)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(frame != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(frame_userdata != NULL, -EINVAL);

	self->cbs.on_load_texture(texture_size, frame, frame_userdata, session_meta,
			self->userdata);

	return 0;
}

/** Documented in public header. */
int sdkcore_texture_loader_destroy(struct sdkcore_texture_loader *self,
		void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
