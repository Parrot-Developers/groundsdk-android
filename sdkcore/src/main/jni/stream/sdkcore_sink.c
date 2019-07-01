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

#include "sdkcore_sink.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** SdkCoreSink native backend. */
struct sdkcore_sink {
	/** Callbacks. */
	struct sdkcore_sink_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
	/** Sink parameters. */
	struct pdraw_video_sink_params params;
	/** Sink management, all components share same lifecycle as sink.self. */
	struct {
		/** PDRAW sink; non NULL iff sink is started. */
		struct pdraw_video_sink *self;
		/** PDRAW instance that controls sink. */
		struct pdraw *pdraw;
		/** Frame queue. */
		struct vbuf_queue *queue;
		/** Pomp event notified when frames are pushed into queue. */
		struct pomp_evt *event;
		/** Pomp loop where event is registered. */
		struct pomp_loop *loop;
	} sink;
};

/**
 * Called back when a new frame has been pushed in the sink's queue.
 * @param[in] evt: pomp_evt that triggered this callback
 * @param[in] userdata: sdkcore_sink instance
 */
static void pdraw_queue_push(struct pomp_evt *evt, void *userdata)
{
	struct sdkcore_sink *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);
	RETURN_IF_FAILED(self->sink.self != NULL, -EPROTO);

	struct vbuf_buffer *buffer = NULL;
	int res = vbuf_queue_pop(self->sink.queue, 0, &buffer);
	RETURN_IF_FAILED(buffer != NULL, res);

	struct sdkcore_frame *frame = sdkcore_frame_create_from_buffer_copy(buffer,
			self->sink.self);

	LOG_IF_ERR(vbuf_unref(buffer));

	RETURN_IF_FAILED(frame != NULL, -ENOMEM);

	self->cbs.on_frame(frame, self->userdata);
}

/**
 * Called back when PDRAW requests that all outstanding buffers be unreferenced
 * and the sink's queue be flushed.
 * @param[in] pdraw: PDRAW instance
 * @param[in] sink: PDRAW sink instance
 * @param[in] userdata: sdkcore_sink instance
 */
static void pdraw_flush(struct pdraw *pdraw, struct pdraw_video_sink *sink,
		void *userdata)
{
	struct sdkcore_sink *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_IF_ERR(vbuf_queue_flush(self->sink.queue));

	LOG_IF_ERR(pdraw_video_sink_queue_flushed(pdraw, sink));
}

/** Documented in public header. */
struct sdkcore_sink *sdkcore_sink_create(const struct sdkcore_sink_cbs *cbs,
		void *userdata)
{
	RETURN_VAL_IF_FAILED(   cbs != NULL
	                     && cbs->on_frame != NULL, -EINVAL, NULL);

	struct sdkcore_sink *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->params.queue_max_count = 1;
	self->cbs = *cbs;
	self->userdata = userdata;

	return self;
}

/** Documented in public header. */
int sdkcore_sink_set_queue_size(struct sdkcore_sink *self, unsigned int size)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self == NULL, -EPROTO);
	RETURN_ERRNO_IF_FAILED(size > 0, -ERANGE);

	self->params.queue_max_count = size;

	return 0;
}

/** Documented in public header. */
int sdkcore_sink_set_queue_full_policy(struct sdkcore_sink *self,
	enum sdkcore_sink_queue_full_policy policy)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self == NULL, -EPROTO);

	switch (policy) {
		case SDKCORE_SINK_DROP_ELDEST:
			self->params.queue_drop_when_full = 1;
			break;
		case SDKCORE_SINK_DROP_NEW:
			self->params.queue_drop_when_full = 0;
			break;
		default:
			RETURN_ERR(-EINVAL);
	}

	return 0;
}

/** Documented in public header. */
int sdkcore_sink_set_frame_format(struct sdkcore_sink *self,
	enum pdraw_h264_format format)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self == NULL, -EPROTO);

	self->params.required_format = format;

	return 0;
}

/** Documented in public header. */
int sdkcore_sink_start(struct sdkcore_sink *self, struct sdkcore_stream *stream,
		struct pomp_loop *loop, unsigned int media_id)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(stream != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(loop != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self == NULL, -EPROTO);

	self->sink.pdraw = sdkcore_stream_get_pdraw(stream);
	RETURN_ERRNO_IF_FAILED(self->sink.pdraw != NULL, -EPROTO);

	struct pdraw_video_sink_cbs cbs = {
		.flush = pdraw_flush
	};

	int res = pdraw_start_video_sink(self->sink.pdraw, media_id, &self->params,
			&cbs, self, &self->sink.self);

	GOTO_IF_FAILED(self->sink.self != NULL, res, err_cleanup);

	self->sink.queue = pdraw_get_video_sink_queue(self->sink.pdraw,
			self->sink.self);
	res = self->sink.queue ? 0 : -ENOTSUP;
	GOTO_IF_ERR(res, err_stop_sink);

	self->sink.loop = loop;
	self->sink.event = vbuf_queue_get_evt(self->sink.queue);
	res = self->sink.event ? 0 : -ENOTSUP;
	GOTO_IF_ERR(res, err_stop_sink);

	res = pomp_evt_attach_to_loop(self->sink.event, loop, pdraw_queue_push, self);
	GOTO_IF_ERR(res, err_stop_sink);

	LOGD("Sink %p START [stream: %p, pdraw: %p]", self, stream,
			self->sink.pdraw);

	return 0;

err_stop_sink:
	res = pdraw_stop_video_sink(self->sink.pdraw, self->sink.self);
	LOG_IF_ERR(res);

err_cleanup:
	self->sink.event = NULL;
	self->sink.loop = NULL;
	self->sink.queue = NULL;
	self->sink.pdraw = NULL;
	self->sink.self = NULL;

	return res;
}

/** Documented in public header. */
int sdkcore_sink_resynchronize(struct sdkcore_sink *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self != NULL, -EPROTO);

	RETURN_ERRNO_IF_ERR(pdraw_resync_video_sink(self->sink.pdraw,
			self->sink.self));

	return 0;
}

/** Documented in public header. */
int sdkcore_sink_stop(struct sdkcore_sink *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->sink.self != NULL, -EPROTO);

	RETURN_ERRNO_IF_ERR(pdraw_stop_video_sink(self->sink.pdraw,
			self->sink.self));

	LOGD("Sink %p STOP [pdraw: %p]", self, self->sink.pdraw);

	self->sink.pdraw = NULL;
	self->sink.self = NULL;

	LOG_IF_ERR(pomp_evt_detach_from_loop(self->sink.event, self->sink.loop));

	self->sink.event = NULL;
	self->sink.loop = NULL;
	self->sink.queue = NULL;
	return 0;
}

/** Documented in public header. */
int sdkcore_sink_destroy(struct sdkcore_sink *self, void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (self->sink.self) {
		RETURN_ERRNO_IF_ERR(sdkcore_sink_stop(self));
	}

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
