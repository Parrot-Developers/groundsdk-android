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

#include "sdkcore_stream.h"
#include "sdkcore_overlayer.h"
#include "sdkcore_texture_loader.h"

#include <pdraw/pdraw_defs.h>

/** SdkCoreRenderer native backend. */
struct sdkcore_renderer;

/** SdkCoreRenderer native backend callbacks. */
struct sdkcore_renderer_cbs {
	/**
	 * Called back when a frame is ready to be rendered.
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*on_frame_ready) (void *userdata);
};

/**
 * Creates a new renderer instance.
 * @param[in] cbs: renderer callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new renderer instance in case of success, NULL otherwise
 */
struct sdkcore_renderer *sdkcore_renderer_create(
		const struct sdkcore_renderer_cbs *cbs,
		void *userdata);

/**
 * Configures render zone.
 * @param[in] self: renderer instance to operate on
 * @param[in] zone: render zone to configure
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_set_render_zone(
		struct sdkcore_renderer *self,
		const struct pdraw_rect *zone);

/**
 * Configures fill mode.
 * @param[in] self: renderer instance to operate on
 * @param[in] mode: fill mode to configure
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_set_fill_mode(
		struct sdkcore_renderer *self,
		enum pdraw_video_renderer_fill_mode mode);

/**
 * Configures overexposure zebras rendering.
 * @param[in] self: renderer instance to operate on
 * @param[in] enable: 0 to disable zebras rendering, any other value to enable
 *                    it
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_enable_zebras(
		struct sdkcore_renderer *self,
		int enable);

/**
 * Configures overexposure zebras threshold.
 * @param[in] self: renderer instance to operate on
 * @param[in] threshold: threshold to configure, in [0, 1] range
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_set_zebra_threshold(
		struct sdkcore_renderer *self,
		double threshold);

/**
 * Configures color histogram computation.
 * @param[in] self: renderer instance to operate on
 * @param[in] enable: 0 to disable histogram computation, any other value to
 *                    enable it
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_enable_histogram(
		struct sdkcore_renderer *self,
		int enable);

/**
 * Configures rendering overlayer.
 * @param[in] self: renderer instance to operate on
 * @param[in] overlayer: overlayer to configure; NULL to disable overlayer
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_set_overlayer(
		struct sdkcore_renderer *self,
		struct sdkcore_overlayer *overlayer);

/**
 * Configures rendering texture loader.
 * @param[in] self: renderer instance to operate on
 * @param[in] overlayer: texture loader to configure; NULL to disable texture
 *                       loader
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the renderer is started
 */
int sdkcore_renderer_set_texture_loader(
		struct sdkcore_renderer *self,
		struct sdkcore_texture_loader *texture_loader);

/**
 * Starts rendering.
 * @param[in] self: renderer instance to operate on
 * @param[in] stream: stream to render
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the renderer is already started, or if the stream is not
 *                 in a state allowing rendering
 */
int sdkcore_renderer_start(
		struct sdkcore_renderer *self,
		struct sdkcore_stream *stream);

/**
 * Renders current frame.
 * @param[in] self: renderer instance to operate on
 * @param[out] content_zone: upon success, contains rendering content zone; may
 *                           be NULL
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the renderer is not started
 */
int sdkcore_renderer_render_frame(
		struct sdkcore_renderer *self,
		struct pdraw_rect *content_zone);

/**
 * Stops rendering.
 * @param[in] self: renderer instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the renderer is not started
 */
int sdkcore_renderer_stop(
		struct sdkcore_renderer *self);

/**
 * Destroys renderer.
 * If started, renderer will be stopped beforehand; if this operation fails,
 * then the renderer is not destroyed.
 * @param[in] self: renderer instance to destroy
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_renderer_destroy(
		struct sdkcore_renderer *self,
		void **userdata);
