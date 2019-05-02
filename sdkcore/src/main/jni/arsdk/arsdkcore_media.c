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

#include "arsdkcore_media.h"

#define ARSDK_LOG_TAG media
#include "arsdk_log.h"

#include <time.h>

/** Documented in public header. */
const struct arsdk_media_res *arsdkcore_media_get_resource_of_type(
		const struct arsdk_media* media, enum arsdk_media_res_type type)
{
	RETURN_VAL_IF_FAILED(media != NULL, -EINVAL, NULL);
	struct arsdk_media_res *resource = NULL;

	do {
		resource = arsdk_media_next_res((struct arsdk_media*) media, resource);
	} while (resource != NULL && arsdk_media_res_get_type(resource) != type);

	return resource;
}

/** Documented in public header. */
const struct arsdk_media_res *arsdkcore_media_get_resource_of_format(
		const struct arsdk_media* media, enum arsdk_media_res_format format)
{
	RETURN_VAL_IF_FAILED(media != NULL, -EINVAL, NULL);
	struct arsdk_media_res *resource = NULL;

	do {
		resource = arsdk_media_next_res((struct arsdk_media*) media, resource);
	} while (resource != NULL && arsdk_media_res_get_fmt(resource) != format);

	return resource;
}

/** Documented in public header. */
int64_t arsdkcore_media_get_date(const struct arsdk_media *media)
{
	RETURN_VAL_IF_FAILED(media != NULL, -EINVAL, 0);

	const struct tm *date = arsdk_media_get_date(media);
	RETURN_VAL_IF_FAILED(date != NULL, -ENODEV, 0);

	time_t res = mktime((struct tm *)date);
	RETURN_VAL_IF_FAILED(res != -1, -ENODEV, 0);

	return ((int64_t) res) * 1000;
}
