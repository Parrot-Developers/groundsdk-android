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

#include <pdraw/pdraw.h>

/** Native SdkCore frame. */
struct sdkcore_frame;

/**
 * Obtains PDRAW frame info contained in a sdkcore_frame.
 * @param[in] self: sdkcore_frame instance to operate on
 * @return pointer onto contained PDRAW frame info if successful, otherwise NULL
 */
const struct pdraw_video_frame *sdkcore_frame_get_pdraw_frame(
		const struct sdkcore_frame *self);

/**
 * Obtains contained frame binary data length, in bytes.
 * @param self: sdkcore_frame instance to operate on
 * @return frame data length in case of success, a negative errno otherwise
 */
ssize_t sdkcore_frame_get_data_len(const struct sdkcore_frame *self);

/**
 * Gives access to contained frame binary data.
 * @param self: sdkcore_frame instance to operate on
 * @return pointer onto contained frame data if successful, otherwise NULL
 */
const uint8_t *sdkcore_frame_get_data(const struct sdkcore_frame *self);
