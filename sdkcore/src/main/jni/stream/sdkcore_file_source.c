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

#include "sdkcore_file_source.h"

#include <sdkcore/internal/sdkcore_source.h>

#define SDKCORE_LOG_TAG stream
#include <sdkcore/sdkcore_log.h>

/** SdkCoreStream local file source. */
struct sdkcore_file_source {
	/** SdkCoreStream source parent. */
	struct sdkcore_source parent;
	/** Stream local file absolute path. */
	char *path;
};

/**
 * Opens the source.
 * @param[in] source: sdkcore_file_source instance
 * @param[in] pdraw: pdraw instance to open the source with
 * @return 0 in case of success, a negative errno otherwise
 */
static int source_open(const struct sdkcore_source *source, struct pdraw *pdraw)
{
	struct sdkcore_file_source *self = (struct sdkcore_file_source *) source;
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	RETURN_ERRNO_IF_ERR(pdraw_open_url(pdraw, self->path));

	return 0;
}

/**
 * Releases the source.
 * @param[in] source: sdkcore_file_source instance to destroy
 */
static void source_release(struct sdkcore_source *source)
{
	struct sdkcore_file_source *self = (struct sdkcore_file_source *) source;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	sdkcore_file_source_destroy(self);
}

/** Documented in public header. */
struct sdkcore_file_source *sdkcore_file_source_create(const char *path)
{
	struct sdkcore_file_source *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->path = strdup(path);
	GOTO_IF_FAILED(self->path != NULL, -ENOMEM, err);

	self->parent.open = source_open;
	self->parent.release = source_release;

	return self;

err:
	free(self);

	return NULL;
}

/** Documented in public header. */
int sdkcore_file_source_destroy(struct sdkcore_file_source *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	free(self->path);
	self->path = NULL;

	free(self);

	return 0;
}
