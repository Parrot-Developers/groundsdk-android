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

#include <sdkcore/internal/sdkcore_frame.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

#include <video-buffers/vbuf_generic.h>

/** Generic malloc/free vbuf_buffer callbacks. Initialized once upon load. */
static struct vbuf_cbs s_vbuf_generic_cbs;

/**
 * Initializes s_vbuf_generic_cbs.
 * Called once at library load time.
 */
__attribute__((constructor))
static void init_vbuf_generic_cbs()
{
	RETURN_IF_FAILED(s_vbuf_generic_cbs.alloc == NULL, -EINVAL);

	int res = vbuf_generic_get_cbs(&s_vbuf_generic_cbs);
	RETURN_IF_FAILED(s_vbuf_generic_cbs.alloc != NULL, res);
}

/** Native SdkCore frame. */
struct sdkcore_frame {
	/** Video buffer containing actual frame data. Unreferenced upon destroy. */
	struct vbuf_buffer *vbuf;
	/** Key to PDRAW metadata in vbuf. */
	void *pdraw_frame_meta_key;
};

/** Documented in public header. */
struct sdkcore_frame *sdkcore_frame_create_from_buffer_copy(
		struct vbuf_buffer *src,
		void *pdraw_frame_meta_key)
{
	RETURN_VAL_IF_FAILED(src != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(pdraw_frame_meta_key != NULL, -EINVAL, NULL);

	/* obtain original data pointer */
	const uint8_t *src_data = vbuf_get_cdata(src);
	RETURN_VAL_IF_FAILED(src_data != NULL, -EINVAL, NULL);

	/* obtain original metadata */
	struct pdraw_video_frame *src_frame = NULL;
	int res = vbuf_metadata_get(src, pdraw_frame_meta_key, NULL, NULL,
			(uint8_t **) &src_frame);
	RETURN_VAL_IF_FAILED(src_frame != NULL, res, NULL);

	struct sdkcore_frame *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	/* create copy buffer */
	res = vbuf_new(0, 0, &s_vbuf_generic_cbs, NULL, &self->vbuf);
	GOTO_IF_FAILED(self->vbuf != NULL, res, err_free_self);

	/* copy buffer */
	GOTO_IF_ERR(vbuf_copy(src, self->vbuf), err_unref_copy);

	/* fix metadata plane pointers for YUV frames */
	if (src_frame->format == PDRAW_VIDEO_MEDIA_FORMAT_YUV) {
		/* obtain copy data pointer */
		const uint8_t *copy_data = vbuf_get_cdata(self->vbuf);

		/* obtain copy metadata */
		struct pdraw_video_frame *copy_frame = NULL;
		vbuf_metadata_get(self->vbuf, pdraw_frame_meta_key, NULL, NULL,
				(uint8_t **) &copy_frame);

		/* fix 3 planes offsets */
		for (int i = 0; i < 3; i++) {
			copy_frame->yuv.plane[i] =
					copy_data + (src_frame->yuv.plane[i] - src_data);
		}
	}

	self->pdraw_frame_meta_key = pdraw_frame_meta_key;

	return self;

err_unref_copy:

	vbuf_unref(self->vbuf);

err_free_self:

	free(self);

	return NULL;
}

/** Documented in public header. */
const struct pdraw_video_frame *sdkcore_frame_get_pdraw_frame(
		const struct sdkcore_frame *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(self->vbuf != NULL, -EPROTO, NULL);

	struct pdraw_video_frame *frame = NULL;

	int res = vbuf_metadata_get(self->vbuf, self->pdraw_frame_meta_key, NULL,
			NULL, (uint8_t **) &frame);
	RETURN_VAL_IF_FAILED(frame != NULL, res, NULL);

	return frame;
}

/** Documented in public header. */
ssize_t sdkcore_frame_get_data_len(const struct sdkcore_frame *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->vbuf != NULL, -EPROTO);

	ssize_t size = vbuf_get_size(self->vbuf);
	LOG_IF_ERR((int) size);

	return size;
}

/** Documented in public header. */
const uint8_t *sdkcore_frame_get_data(const struct sdkcore_frame *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(self->vbuf != NULL, -EPROTO, NULL);

	return vbuf_get_cdata(self->vbuf);
}

/** Documented in public header. */
int sdkcore_frame_destroy(struct sdkcore_frame *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->vbuf != NULL, -EPROTO);

	RETURN_ERRNO_IF_ERR(vbuf_unref(self->vbuf));

	self->vbuf = NULL;

	free(self);

	return 0;
}
