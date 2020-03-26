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

/** SdkCoreTextureLoader native backend. */
struct sdkcore_texture_loader;

/** Texture specification. */
struct sdkcore_texture_loader_texture_spec {
	/** Texture width, in pixels; 0 when unspecified */
	unsigned int width;
	/** Texture aspect ratio. */
	struct {
		/** Aspect ratio width factor; 0 when unspecified. */
		unsigned int width;
		/** Aspect ratio height factor; 0 when unspecified. */
		unsigned int height;
	} aspect_ratio;
};

/** Texture size. */
struct sdkcore_texture_loader_texture_size {
	/** Texture width, in pixels. */
	unsigned int width;
	/** Texture height, in pixels. */
	unsigned int height;
};

/** Frame userdata. */
struct sdkcore_texture_loader_frame_userdata {
	/** Opaque frame userdata. */
	const void *data;
	/** Frame userdata size, in bytes. */
	size_t size;
};

/** SdkCoreTextureLoader native backend callbacks. */
struct sdkcore_texture_loader_cbs {
	/**
	 * Called back when a texture must be loaded.
	 * @param[in] texture_size: texture size information
	 * @param[in] frame: PDRAW video frame
	 * @param[in] frame_userdata: opaque frame user data
	 * @param[in] session_meta: streaming session metadata
	 * @param[in] userdata: opaque pointer from the caller
	 * @return 0 in case of success, a negative errno otherwise
	 */
	int (*on_load_texture) (
			const struct sdkcore_texture_loader_texture_size *texture_size,
			const struct pdraw_video_frame *frame,
			const struct sdkcore_texture_loader_frame_userdata *frame_userdata,
			const struct vmeta_session *session_meta,
			void *userdata);
};

/**
 * Creates a new texture loader instance.
 * @param[in] texture_spec: texture dimension specifications
 * @param[in] cbs: texture loader callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new texture loader instance in case of success, NULL otherwise
 */
struct sdkcore_texture_loader *sdkcore_texture_loader_create(
		const struct sdkcore_texture_loader_texture_spec *texture_spec,
		const struct sdkcore_texture_loader_cbs *cbs,
		void *userdata);

/**
 * Accesses configured texture specifications.
 * @param[in] self: texture loader instance to operate on
 * @return configured texture specification in case of success, NULL otherwise
 */
const struct sdkcore_texture_loader_texture_spec *
sdkcore_texture_loader_texture_spec(
		struct sdkcore_texture_loader *self);

/**
 * Requests texture load.
 * @param[in] self: texture loader instance to operate on
 * @param[in] texture_size: texture size information
 * @param[in] frame: PDRAW video frame
 * @param[in] frame_userdata: opaque frame user data
 * @param[in] session_meta: streaming session metadata
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_texture_loader_load_texture(
		struct sdkcore_texture_loader *self,
		const struct sdkcore_texture_loader_texture_size *texture_size,
		const struct pdraw_video_frame *frame,
		const struct sdkcore_texture_loader_frame_userdata *frame_userdata,
		const struct vmeta_session *session_meta);

/**
 * Destroys texture loader.
 * @param[in] self: texture loader instance to destroy
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_texture_loader_destroy(
		struct sdkcore_texture_loader *self,
		void **userdata);
