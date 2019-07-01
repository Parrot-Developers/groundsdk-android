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

#include "sdkcore_stream.h"

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

#define MILLI_IN_MICROS 1000
#define DEFAULT_TRACK_ID 0

/** Debug logs current stream & playback state; self must be valid. */
#define LOG_WITH_STATE(_fmt, ...) \
	LOGD("Stream %p [state: %s, pdraw:%p, position: %"PRIi64", speed:%.2f, " \
			"duration: %"PRIi64", track:%s]\n\t"_fmt, \
			self, state_to_str(self->state), self->pdraw, \
			self->playback.position, self->playback.speed, \
			self->playback.duration, self->track, ##__VA_ARGS__);

/** SdkCoreStream native backend */
struct sdkcore_stream {
	/** Callbacks. */
	struct sdkcore_stream_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
	/** Stream source. */
	struct sdkcore_source *source;
	/** Stream track, NULL for default track, if any. */
	char *track;
	/** Stream internal state. */
	enum state {
		/** Initial state, stream is closed. */
		STATE_CLOSED = 0,
		/** Stream is opening, PDRAW open_* has been called. */
		STATE_OPENING,
		/** Stream is opened, PDRAW open_resp success + ready_to_play == 1. */
		STATE_OPEN,
		/** Stream is closing, PDRAW close has been called. */
		STATE_CLOSING,
	} state;

	/** Playback state. */
	struct sdkcore_stream_playback_state playback;

	/** PDRAW instance, NULL before successful open and after close. */
	struct pdraw *pdraw;
};

/**
 * Gets string representation of internal stream state.
 * @param[in]: stream internal state
 * @return corresponding state string representation if exists, otherwise NULL
 */
static const char *state_to_str(enum state state)
{
	switch (state) {
		case STATE_CLOSED:  return "CLOSED";
		case STATE_OPENING: return "OPENING";
		case STATE_OPEN:    return "OPEN";
		case STATE_CLOSING: return "CLOSING";
		default:            return NULL;
	}
}

/**
 * Fixes an uint64_t microsecond time value to int64_t milliseconds.
 * UINT64_MAX input is treated specially and 0 is returned; otherwise, only
 * 63 least significant bits of input are kept, resulting in an always positive
 * int64_t return value.
 * @param value time value to fix, in microseconds
 * @return fixed time value, in milliseconds
 */
static inline int64_t fix_time(uint64_t value)
{
	return value == UINT64_MAX ? 0 :
			(value / MILLI_IN_MICROS) & UINT64_C(0x7FFFFFFFFFFFFFFF);
}

/**
 * Destroys stream instance, releasing PDRAW instance and closing source.
 * @param[in] self: stream instance to operate on; MUST be non-NULL
 */
static void destroy_stream(struct sdkcore_stream *self) {
	if (self->pdraw) {
		pdraw_destroy(self->pdraw);
		self->pdraw = NULL;
	}

	if (self->source) {
		self->source->release(self->source);
		self->source = NULL;
	}

	free(self->track);
	self->track = NULL;

	free(self);
}

/**
 * Stream open request callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] status: 0 in case of success, a negative errno otherwise
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_open_resp(struct pdraw *pdraw, int status, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- OPEN response [status: %d]", status);

	if (status == 0) {
		LOG_IF_FAILED(self->state == STATE_OPENING, -EPROTO);
		return;
	}

	LOG_ERR(status);
	RETURN_IF_FAILED(   self->state == STATE_OPENING
	// TODO : as we don't know exact PDRAW behavior when calling close before
	// TODO   open_resp has been received, we handle here a scenario where
	// TODO   open_resp would be called with failure status in such a case.
	                 || self->state == STATE_CLOSING, -EPROTO);

	if (self->state == STATE_OPENING) {
		self->state = STATE_CLOSING;
		self->cbs.on_stream.closing(self->userdata);
	}

	if (self->state == STATE_CLOSING) {
		self->state = STATE_CLOSED;
		self->cbs.on_stream.closed(self->userdata);
		destroy_stream(self);
	}
}

/**
 * Stream close request callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] status: 0 in case of success, a negative errno otherwise
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_close_resp(struct pdraw *pdraw, int status, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- CLOSE response [status: %d]", status);

	RETURN_IF_FAILED(self->state == STATE_CLOSING, -EPROTO);
	RETURN_IF_ERR(status);

	self->state = STATE_CLOSED;
	self->cbs.on_stream.closed(self->userdata);
	destroy_stream(self);
}

/**
 * Stream error callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_error(struct pdraw *pdraw, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- UNRECOVERABLE ERROR");

	RETURN_IF_FAILED(   self->state == STATE_OPENING
	                 || self->state == STATE_OPEN, -EPROTO);

	LOG_IF_ERR(sdkcore_stream_close(self));
}

/**
 * Stream media/track selection callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] medias: available media array
 * @param[in] count: available media count
 * @param[in] userdata: sdkcore_stream instance
 */
static int pdraw_media_select(struct pdraw *pdraw,
		const struct pdraw_demuxer_media *medias, size_t count, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	int track_id = -ENOMEDIUM; // by default, assume track not found
	if (!self->track) {
		track_id = DEFAULT_TRACK_ID; // let PDRAW use default track if feasible
	} else for (unsigned int i = 0; i < count && track_id < 0; i++) {
		// search for a matching track
		if (strcmp(self->track, medias[i].name) == 0) {
			track_id = medias[i].media_id;
		}
	}

	LOG_WITH_STATE("<- SELECT [track: %d]", track_id);
	for (unsigned int i = 0; i < count; i++) {
		LOGD("\t\t %s%d: %s", medias[i].is_default ? "0, " : "",
				medias[i].media_id, medias[i].name);
	}

	return track_id;
}

/**
 * Stream playback availability callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] ready: 1 when playback is available, 0 otherwise
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_ready_to_play(struct pdraw *pdraw, int ready, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- READY event [ready: %d]", ready);

	if (ready) {
		RETURN_IF_FAILED(self->state == STATE_OPENING, -EPROTO);

		self->state = STATE_OPEN;

		self->playback.duration = fix_time(pdraw_get_duration(pdraw));
		self->playback.position = 0;
		self->playback.speed = 0;

		self->cbs.on_playback_state(&self->playback, self->userdata);
	} else if (self->state == STATE_OPEN) {
		LOG_IF_ERR(sdkcore_stream_close(self));
	}
}

/**
 * Playback end of range callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] timestamp: playback position timestamp, in microseconds
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_end_of_range(struct pdraw *pdraw, uint64_t timestamp,
		void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- END OF RANGE event [timestamp:%"PRIu64"]", timestamp);

	self->playback.position = self->playback.duration;
	self->playback.speed = 0;

	self->cbs.on_playback_state(&self->playback, self->userdata);
}

/**
 * Playback play request callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] status: 0 in case of success, a negative errno otherwise
 * @param[in] timestamp: playback position timestamp, in microseconds
 * @param[in] speed: playback speed multiplier
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_play_resp(struct pdraw *pdraw, int status, uint64_t timestamp,
		float speed, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- PLAY response [status: %d, timestamp:%"PRIu64
			", speed: %f]", status, timestamp, speed);

	RETURN_IF_FAILED(self->state == STATE_OPEN, -EPROTO);
	LOG_IF_ERR(status);

	self->playback.position = fix_time(timestamp);
	self->playback.speed = speed;

	self->cbs.on_playback_state(&self->playback, self->userdata);
}

/**
 * Playback pause request callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] status: 0 in case of success, a negative errno otherwise
 * @param[in] timestamp: playback position timestamp, in microseconds
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_pause_resp(struct pdraw *pdraw, int status,
		uint64_t timestamp, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- PAUSE response [status: %d, timestamp:%"PRIu64"]",
			status, timestamp);

	RETURN_IF_FAILED(self->state == STATE_OPEN, -EPROTO);
	LOG_IF_ERR(status);

	self->playback.position = fix_time(timestamp);
	self->playback.speed = 0;

	self->cbs.on_playback_state(&self->playback, self->userdata);
}

/**
 * Playback seek request callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] status: 0 in case of success, a negative errno otherwise
 * @param[in] timestamp: playback position timestamp, in microseconds
 * @param[in] speed: playback speed multiplier
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_seek_resp(struct pdraw *pdraw, int status, uint64_t timestamp,
		float speed, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	LOG_WITH_STATE("<- SEEK response [status: %d, timestamp:%"PRIu64
			", speed: %f]", status, timestamp, speed);

	RETURN_IF_FAILED(self->state == STATE_OPEN, -EINVAL);
	LOG_IF_ERR(status);

	self->playback.position = fix_time(timestamp);
	// in case speed was 0, we were paused, so we don't update speed since
	// pdraw may send a positive value here, even in pause
	self->playback.speed = self->playback.speed == 0 ? 0 : speed;

	self->cbs.on_playback_state(&self->playback, self->userdata);
}

/**
 * Socket created callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] fd: socket descriptor
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_socket_created(struct pdraw *pdraw, int fd, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (self->source && self->source->on_socket_created) {
		self->source->on_socket_created(self->source, fd);
	}
}

/**
 * Media added callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] info: added media info
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_media_added(struct pdraw *pdraw,
		const struct pdraw_media_info *info, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	RETURN_IF_FAILED(info != NULL, -EINVAL);

	self->cbs.on_media.added(info, self->userdata);
}

/**
 * Media removed callback.
 * @param[in] pdraw: PDRAW instance
 * @param[in] info: removed media info
 * @param[in] userdata: sdkcore_stream instance
 */
static void pdraw_media_removed(struct pdraw *pdraw,
		const struct pdraw_media_info *info, void *userdata)
{
	struct sdkcore_stream *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	RETURN_IF_FAILED(info != NULL, -EINVAL);

	self->cbs.on_media.removed(info, self->userdata);
}

/** Documented in public header. */
struct sdkcore_stream *sdkcore_stream_open(struct pomp_loop *loop,
		struct sdkcore_source *source, const char *track,
		const struct sdkcore_stream_cbs *cbs, void *userdata)
{
	RETURN_VAL_IF_FAILED(loop != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(   source != NULL
	                     && source->open != NULL
	                     && source->release, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(   cbs != NULL
	                     && cbs->on_stream.closing != NULL
	                     && cbs->on_stream.closed != NULL
	                     && cbs->on_playback_state != NULL
	                     && cbs->on_media.added != NULL
	                     && cbs->on_media.removed != NULL, -EINVAL, NULL);

	struct sdkcore_stream *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->cbs = *cbs;
	self->userdata = userdata;
	self->state = STATE_CLOSED;

	struct pdraw_cbs pdraw_cbs = {
		.open_resp = pdraw_open_resp,
		.close_resp = pdraw_close_resp,
		.unrecoverable_error = pdraw_error,
		.select_demuxer_media = pdraw_media_select,
		.ready_to_play = pdraw_ready_to_play,
		.end_of_range = pdraw_end_of_range,
		.play_resp = pdraw_play_resp,
		.pause_resp = pdraw_pause_resp,
		.seek_resp = pdraw_seek_resp,
		.socket_created = pdraw_socket_created,
		.media_added = pdraw_media_added,
		.media_removed = pdraw_media_removed,
	};

	int res = pdraw_new(loop, &pdraw_cbs, self, &self->pdraw);
	GOTO_IF_FAILED(self->pdraw != NULL, res, err);

	if (track) {
		self->track = strdup(track);
		GOTO_IF_FAILED(self->track != NULL, -ENOMEM, err);
	}

	LOG_WITH_STATE("-> OPEN request");

	self->source = source;
	self->state = STATE_OPENING;
	GOTO_IF_ERR(source->open(source, self->pdraw), err);

	return self;

err:
	self->source = NULL; // user must release the source itself in case of error
	destroy_stream(self);

	return NULL;
}

/** Documented in public header. */
int sdkcore_stream_play(struct sdkcore_stream *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw != NULL, -EPROTO);
	RETURN_ERRNO_IF_FAILED(self->state == STATE_OPEN, -EPROTO);

	LOG_WITH_STATE("-> PLAY request");

	RETURN_ERRNO_IF_ERR(pdraw_play(self->pdraw));

	return 0;
}

/** Documented in public header. */
int sdkcore_stream_pause(struct sdkcore_stream *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw != NULL, -EPROTO);
	RETURN_ERRNO_IF_FAILED(self->state == STATE_OPEN, -EPROTO);

	LOG_WITH_STATE("-> PAUSE request");

	RETURN_ERRNO_IF_ERR(pdraw_pause(self->pdraw));

	return 0;
}

/** Documented in public header. */
int sdkcore_stream_seek(struct sdkcore_stream *self, int64_t position)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw != NULL, -EPROTO);
	RETURN_ERRNO_IF_FAILED(self->state == STATE_OPEN, -EPROTO);
	RETURN_ERRNO_IF_FAILED(position >= 0 && position <= self->playback.duration,
			-ERANGE);

	LOG_WITH_STATE("-> SEEK request [position:%"PRIi64"]", position);

	RETURN_ERRNO_IF_ERR(pdraw_seek_to(self->pdraw,
			position * MILLI_IN_MICROS, 0));

	return 0;
}

/** Documented in public header. */
struct pdraw *sdkcore_stream_get_pdraw(struct sdkcore_stream *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(self->pdraw != NULL, -EPROTO, NULL);

	return self->pdraw;
}

/** Documented in public header. */
int sdkcore_stream_close(struct sdkcore_stream *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->pdraw != NULL, -EPROTO);
	RETURN_ERRNO_IF_FAILED(   self->state == STATE_OPENING
	                       || self->state == STATE_OPEN, -EPROTO);

	LOG_WITH_STATE("-> CLOSE request");

	RETURN_ERRNO_IF_ERR(pdraw_close(self->pdraw));

	self->state = STATE_CLOSING;
	self->cbs.on_stream.closing(self->userdata);

	return 0;
}
