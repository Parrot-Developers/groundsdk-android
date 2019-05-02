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

#include <arsdkctrl/arsdkctrl.h>

/**
 * Gets a media resource of a given type.
 * @param[in] media: media to get the resource from
 * @param[in] type: type of the resource to retrieve
 * @return the first found resource of the given type in the given media,
 *         or null if no such resource could be found
 */
const struct arsdk_media_res *arsdkcore_media_get_resource_of_type(
		const struct arsdk_media* media, enum arsdk_media_res_type type);

/**
 * Gets a media resource of a given format.
 * @param[in] media: media to get the resource from
 * @param[in] format: format of the resource to retrieve
 * @return the first found resource of the given format in the given media,
 *         or null if no such resource could be found
 */
const struct arsdk_media_res *arsdkcore_media_get_resource_of_format(
		const struct arsdk_media* media, enum arsdk_media_res_format format);

/**
 * Gets the creation date of a media.
 * @param[in] media: media whose creation date must be retrieved
 * @return the media's creation date, expressed in milliseconds since epoch,
 *         or 0 in case of failure
 */
int64_t arsdkcore_media_get_date(const struct arsdk_media *media);
