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

#include <sdkcore/internal/sdkcore_internal_api.h>

#include <pdraw/pdraw.h>

/**
 * An abstract stream source.
 * Allows to implement custom behaviour as to how a stream opens a source.
 */
struct sdkcore_source {

	/**
	 * Source open method.
	 * This method is called when a stream is requested to open this source.
	 * Implementation shall there open the source using the provided stream
	 * internal pdraw instance.
	 * @param[in] self: source instance
	 * @param[in] pdraw: stream internal pdraw instance, to be used to open
	 *                   the source as appropriate
	 * @return 0 in case of success, a negative errno otherwise
	 */
	int (*open) (
			const struct sdkcore_source *self,
			struct pdraw *pdraw);

	/**
	 * Socket creation callback.
	 * This method is called whenever the stream creates a socket when so
	 * required to stream this source. May be NULL.
	 * @param[in] self: source instance
	 * @param[in] fd: created socket descriptor
	 */
	void (*on_socket_created) (
			const struct sdkcore_source *self,
			int fd);

	/**
	 * Source release method.
	 * This method is called when a stream opened for this source closes.
	 * Implementation shall there release any related resources (including this
	 * source instance) that are no longer needed as the source itself is no
	 * longer needed.
	 * @param[in] self: source instance
	 */
	void (*release) (
			struct sdkcore_source *self);
};
