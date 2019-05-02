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

#include <sdkcore/internal/sdkcore_frame.h>

/** SdkCoreSink native backend. */
struct sdkcore_sink;

/** Policy to apply with regard to new frames when the queue is full. */
enum sdkcore_sink_queue_full_policy {
	/** Drops eldest frame in queue to make room for new frame. */
	SDKCORE_SINK_DROP_ELDEST = 0,

	/** Drops new frame. */
	SDKCORE_SINK_DROP_NEW
};

/** SdkCoreSink native backend callbacks */
struct sdkcore_sink_cbs {

	/**
	 * Called back when an new frame as been received.
	 * @param[in] frame: received frame
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*on_frame) (struct sdkcore_frame *frame, void *userdata);
};

/**
 * Creates a new sink instance.
 * @param[in] cbs: sink callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new sink instance in case of success, NULL otherwise
 */
struct sdkcore_sink *sdkcore_sink_create(
		const struct sdkcore_sink_cbs *cbs,
		void *userdata);

/**
 * Configures sink queue size.
 * @param[in] self: sink instance to operate on
 * @param size: sink queue size to configure
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the sink is started
 */
int sdkcore_sink_set_queue_size(
		struct sdkcore_sink *self,
		unsigned int size);

/**
 * Configures sink queue full policy.
 * @param[in] self: sink instance to operate on
 * @param policy: sink queue full policy to configure
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the sink is started
 */
int sdkcore_sink_set_queue_full_policy(
		struct sdkcore_sink *self,
		enum sdkcore_sink_queue_full_policy policy);

/**
 * Configures sink output frame format.
 * Note: only for H.264 encoded frames sinks.
 * @param[in] self: sink instance to operate on
 * @param format: h264 frame format to configure
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the sink is started
 */
int sdkcore_sink_set_frame_format(
		struct sdkcore_sink *self,
		enum pdraw_h264_format format);

/**
 * Starts sink.
 * @param[in] self: sink instance to operate on
 * @param[in] stream: stream source for this sink
 * @param[in] loop:  pomp loop where the sink will run.
 * @param[in] media_id: identifier of the media source for this sink
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the sink is already started, or if the stream is not
 *                 in a state allowing starting a sink
 */
int sdkcore_sink_start(
		struct sdkcore_sink *self,
		struct sdkcore_stream *stream,
		struct pomp_loop *loop,
		unsigned int media_id);

/**
 * Resynchronizes sink.
 * TODO: document the effect
 * @param[in] self: sink instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the sink is not started
 */
int sdkcore_sink_resynchronize(struct sdkcore_sink *self);

/**
 * Stops sink.
 * @param[in] self: sink instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO if the sink is not started
 */
int sdkcore_sink_stop(struct sdkcore_sink *self);

/**
 * Destroys sink.
 * If started, sink will be stopped beforehand; if this operation fails,
 * then the sink is not destroyed.
 * @param[in] self: sink instance to destroy
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_sink_destroy(
		struct sdkcore_sink *self,
		void **userdata);
