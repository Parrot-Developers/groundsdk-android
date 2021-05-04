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

#include "sdkcore_renderer.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** Static transition flags setup. */
#define TRANSITION_FLAGS \
	  PDRAW_VIDEO_RENDERER_TRANSITION_FLAG_RECONFIGURE \
	| PDRAW_VIDEO_RENDERER_TRANSITION_FLAG_TIMEOUT \
	| PDRAW_VIDEO_RENDERER_TRANSITION_FLAG_PHOTO_TRIGGER

/** SdkCoreRenderer native backend. */
struct sdkcore_renderer {
	/** Callbacks. */
	struct sdkcore_renderer_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
	/** Configured render zone. Invalid [0, 0] at creation. */
	struct pdraw_rect render_zone;
	/** Renderer parameters. */
	struct pdraw_video_renderer_params params;
	/** Configured overlayer; NULL if none. */
	struct sdkcore_overlayer *overlayer;
	/** Configured texture loader; NULL if none. */
	struct sdkcore_texture_loader *texture_loader;
	/** PDRAW & renderer, held together as they share lifecycle. */
	struct {
		/** PDRAW instance; non NULL if pdraw.renderer is non NULL. */
		struct pdraw *self;
		/** PDRAW renderer instance; non NULL iff rendering is started. */
		struct pdraw_video_renderer *renderer;
	} pdraw;
};

/**
 * Render ready callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] renderer: PDRAW renderer
 * @param[in] userdata: renderer instance
 */
static void render_ready(struct pdraw *pdraw,
		struct pdraw_video_renderer *renderer, void *userdata)
{
	struct sdkcore_renderer *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->cbs.on_frame_ready(self->userdata);
}

/**
 * Render overlay callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] renderer: PDRAW renderer
 * @param[in] render_pos: render zone
 * @param[in] content_pos: content zone
 * @param[in] view_matrix: view 4x4 matrix, 16 elements float array
 * @param[in] projection_matrix: projection 4x4 matrix, 16 element float array
 * @param[in] session_info: PDRAW session info
 * @param[in] session_meta: session metadata
 * @param[in] frame_meta: frame metadata;
 *            NULL in case of redrawing old frame to keep the frame rate.
 * @param[in] frame_extra: PDRAW frame extraneous data ;
 *            NULL in case of redrawing old frame to keep the frame rate.
 * @param[in] userdata: renderer instance
 */
static void render_overlay(struct pdraw *pdraw,
		struct pdraw_video_renderer *renderer,
		const struct pdraw_rect *render_pos,
		const struct pdraw_rect *content_pos,
		const float *view_matrix,
		const float *projection_matrix,
		const struct pdraw_session_info *session_info,
		const struct vmeta_session *session_meta,
		const struct vmeta_frame *frame_meta,
		const struct pdraw_video_frame_extra *frame_extra, void *userdata)
{
	struct sdkcore_renderer *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (self->overlayer) {
		sdkcore_overlayer_overlay(self->overlayer, render_pos, content_pos,
				session_info, session_meta, frame_meta, frame_extra);
	}
}

/**
 * Texture load callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] renderer: PDRAW renderer
 * @param[in] texture_width: render texture width, in pixels
 * @param[in] texture_height: render texture height, in pixels
 * @param[in] session_info: PDRAW session info
 * @param[in] session_meta: session metadata
 * @param[in] frame: PDRAW video frame
 * @param[in] frame_userdata_buf: opaque frame userdata
 * @param[in] frame_userdata_len; frame userdata size
 * @param[in] userdata: renderer instance
 * @return 0 in case of success, a negative errno otherwise
 */
static int load_texture(struct pdraw *pdraw,
		struct pdraw_video_renderer *renderer,
		unsigned int texture_width,
		unsigned int texture_height,
		const struct pdraw_session_info *session_info,
		const struct vmeta_session *session_meta,
		const struct pdraw_video_frame *frame, const void *frame_userdata_buf,
		size_t frame_userdata_len, void *userdata)
{
	struct sdkcore_renderer *self = userdata;
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->texture_loader != NULL, -EINVAL);

	struct sdkcore_texture_loader_frame_userdata frame_userdata = {
		.data = frame_userdata_buf,
		.size = frame_userdata_len
	};

	struct sdkcore_texture_loader_texture_size texture_size = {
		.width = texture_width,
		.height = texture_height
	};

	RETURN_ERRNO_IF_ERR(sdkcore_texture_loader_load_texture(
			self->texture_loader, &texture_size, frame, &frame_userdata,
			session_meta));

	return 0;
}

/**
 * Forwards current renderer params to pdraw renderer if started.
 * @param[in] self: renderer instance to operate on
 * @return 0 in case of success, or if rendering is not started, a negative
 *         errno otherwise.
 */
static int apply_params(struct sdkcore_renderer *self)
{
	if (self->pdraw.renderer) {
		RETURN_ERRNO_IF_ERR(pdraw_set_video_renderer_params(
				self->pdraw.self, self->pdraw.renderer, &self->params));
	}

	return 0;
}

/** Documented in public header. */
struct sdkcore_renderer *sdkcore_renderer_create(
		const struct sdkcore_renderer_cbs *cbs, void *userdata)
{
	RETURN_VAL_IF_FAILED(cbs != NULL && cbs->on_frame_ready != NULL, -EINVAL,
			NULL);

	struct sdkcore_renderer *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

    self->params.enable_transition_flags = TRANSITION_FLAGS;
	self->cbs = *cbs;
	self->userdata = userdata;

	return self;
}

/** Documented in public header. */
int sdkcore_renderer_set_render_zone(struct sdkcore_renderer *self,
		const struct pdraw_rect *zone)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->render_zone = *zone;

	if (self->pdraw.renderer) {
		RETURN_ERRNO_IF_ERR(pdraw_resize_video_renderer(
				self->pdraw.self, self->pdraw.renderer, &self->render_zone));
	}

	return 0;
}

/** Documented in public header. */
int sdkcore_renderer_set_fill_mode(struct sdkcore_renderer *self,
		enum pdraw_video_renderer_fill_mode mode)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->params.fill_mode = mode;

	return apply_params(self);
}

/** Documented in public header. */
int sdkcore_renderer_enable_zebras(struct sdkcore_renderer *self,
		int enable)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->params.enable_overexposure_zebras = enable;

	return apply_params(self);
}

/** Documented in public header. */
int sdkcore_renderer_set_zebra_threshold(struct sdkcore_renderer *self,
		double threshold)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->params.overexposure_zebras_threshold = (float) threshold;

	return apply_params(self);
}

/** Documented in public header. */
int sdkcore_renderer_enable_histogram(struct sdkcore_renderer *self,
		int enable)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->params.enable_histograms = enable;

	return apply_params(self);
}

/** Documented in public header. */
int sdkcore_renderer_set_overlayer(struct sdkcore_renderer *self,
		struct sdkcore_overlayer *overlayer)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->overlayer = overlayer;

	return 0;
}

/** Documented in public header. */
int sdkcore_renderer_set_texture_loader(struct sdkcore_renderer *self,
		struct sdkcore_texture_loader *texture_loader)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw.renderer == NULL, -EPROTO);

	self->texture_loader = texture_loader;

	return 0;
}

/** Documented in public header. */
int sdkcore_renderer_start(struct sdkcore_renderer *self,
		struct sdkcore_stream *stream)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(stream != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw.renderer == NULL, -EPROTO);

	self->pdraw.self = sdkcore_stream_get_pdraw(stream);
	RETURN_ERRNO_IF_FAILED(self->pdraw.self != NULL, -EPROTO);

	struct pdraw_video_renderer_cbs cbs = {
		.render_ready = render_ready,
		.render_overlay = render_overlay,
	};

	if (self->texture_loader) {
		const struct sdkcore_texture_loader_texture_spec *texture_spec =
				sdkcore_texture_loader_texture_spec(self->texture_loader);
		cbs.load_texture = load_texture;
		self->params.video_texture_width = texture_spec->width;
		self->params.video_texture_dar_width = texture_spec->aspect_ratio.width;
		self->params.video_texture_dar_height = texture_spec->aspect_ratio.height;
	}

	int res = pdraw_start_video_renderer(self->pdraw.self, &self->render_zone,
			&self->params, &cbs, self, &self->pdraw.renderer);

	GOTO_IF_FAILED(self->pdraw.renderer != NULL, res, err);

	LOGD("Renderer %p START [stream: %p, pdraw: %p]", self, stream,
			self->pdraw.self);

	return 0;

err:
	self->pdraw.self = NULL;

	return res;
}

/** Documented in public header. */
int sdkcore_renderer_render_frame(struct sdkcore_renderer *self,
		struct pdraw_rect *content_zone)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw.renderer != NULL, -EPROTO);

	RETURN_ERRNO_IF_ERR(pdraw_render_video(self->pdraw.self,
			self->pdraw.renderer, content_zone));

	return 0;
}

/** Documented in public header. */
int sdkcore_renderer_stop(struct sdkcore_renderer *self) {
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw.renderer != NULL, -EPROTO);

	RETURN_ERRNO_IF_ERR(pdraw_stop_video_renderer(self->pdraw.self,
			self->pdraw.renderer));

	LOGD("Renderer %p STOP [pdraw: %p]", self, self->pdraw.self);

	self->pdraw.self = NULL;
	self->pdraw.renderer = NULL;

	return 0;
}

/** Documented in public header. */
int sdkcore_renderer_destroy(struct sdkcore_renderer *self, void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (self->pdraw.renderer) {
		RETURN_ERRNO_IF_ERR(sdkcore_renderer_stop(self));
	}

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
