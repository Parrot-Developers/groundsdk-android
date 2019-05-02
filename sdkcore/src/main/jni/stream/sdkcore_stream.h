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

#include <sdkcore/internal/sdkcore_source.h>

#include <libpomp.h>
#include <pdraw/pdraw.h>

/** SdkCoreStream native backend */
struct sdkcore_stream;

/** Playback state. */
struct sdkcore_stream_playback_state {
	/** Stream duration, in milliseconds; 0 when irrelevant; always positive. */
	int64_t duration;
	/** Playback position, in milliseconds; always positive. */
	int64_t position;
	/** Playback speed (multiplier); 0 when paused. */
	double speed;
};

/** SdkCoreStream native backend callbacks */
struct sdkcore_stream_cbs {

	/** Lifecycle callbacks. */
	struct {
		/**
		 * Called back as soon as the stream as been requested to close.
		 * All started renderers are cooperatively required to stop as soon as
		 * possible so that the stream can be closed.
		 * @param[in] userdata: opaque pointer from the caller
		 */
		void (*closing) (void *userdata);
		/**
		 * Called back when the stream is finally closed.
		 * SdkCoreStream native backend is destroyed right after this function returns.
		 * @param[in] userdata: opaque pointer from the caller
		 */
		void (*closed) (void *userdata);
	} on_stream;

	/**
	 * Called once when the stream is ready, then when playback state changes
	 * @param[in] playback: playback state
	 * @param[in] userdata: opaque pointer from caller
	 */
	void (*on_playback_state) (
			const struct sdkcore_stream_playback_state *playback,
			void *userdata);

	/** Media management callbacks. */
	struct {
		/**
		 * Called back when a media has been added.
		 * @param[in] info : added media info
		 * @param[in] userdata: opaque pointer from the caller
		 */
		void (*added) (const struct pdraw_media_info *info, void *userdata);
		/**
		 * Called back when a media has been removed.
		 * @param[in] info : removed media info
		 * @param[in] userdata: opaque pointer from the caller
		 */
		void (*removed) (const struct pdraw_media_info *info, void *userdata);
	} on_media;
};

/**
 * Creates and opens a new stream instance.
 * Note: in case this method returns any error, provided source is NOT released;
 *       it is the caller's responsibility to release it properly. Otherwise,
 *       the source release method will be called when the stream closes.
 * @param[in] loop: pomp loop where the stream will run.
 * @param[in] source: abstract stream source
 * @param[in] track: track to select, NULL to select default track, if any
 * @param[in] cbs: stream callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new stream instance in case of success, NULL otherwise
 */
struct sdkcore_stream *sdkcore_stream_open(
		struct pomp_loop *loop,
		struct sdkcore_source *source,
		const char *track,
		const struct sdkcore_stream_cbs *cbs,
		void *userdata);

/**
 * Resumes playback.
 * Playback control is only available when the stream is open, i.e. only after
 * on_stream.opened and before on_stream.closing are called.
 * @param[in] stream: stream instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the stream is not ready for playback.
 */
int sdkcore_stream_play(
		struct sdkcore_stream *self);

/**
 * Pauses playback.
 * Playback control is only available when the stream is open, i.e. only after
 * on_stream.opened and before on_stream.closing are called.
 * @param[in] stream: stream instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the stream is closed.
 */
int sdkcore_stream_pause(
		struct sdkcore_stream *self);

/**
 * Seeks to position in stream.
 * Playback control is only available when the stream is open, i.e. only after
 * on_stream.opened and before on_stream.closing are called.
 * @param[in] self: stream instance to operate on
 * @param{in] position: position to seek to, in milliseconds; must be positive
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the stream is closed
 *         -ERANGE in case seek position is negative or exceeds stream duration
 */
int sdkcore_stream_seek(
		struct sdkcore_stream *self,
		int64_t position);

/**
 * Provides access to internal PDRAW instance.
 * Only used to start renderers. Stream must not be closed.
 * @param[in] self: stream instance to operate on
 * @return internal PDRAW instance in case of success, NULL otherwise.
 */
struct pdraw *sdkcore_stream_get_pdraw(
		struct sdkcore_stream *self);

/**
 * Closes stream.
 * Closing is asynchronous; after this function returns successfully,
 * on_stream.closing is called to inform that the close operation begins. At
 * this point, all started renderers must be stopped. Once all renderers are
 * stopped, closing will proceed and eventually on_stream.closed is called to
 * inform that the close operation ends. After this callback returns, the
 * native SdkCoreStream instance is destroyed.
 * @param[in] self: stream instance to operate on
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the stream is already closed.
 */
int sdkcore_stream_close(
		struct sdkcore_stream *self);
