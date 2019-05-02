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

#include "arsdkcore.h"
#include "arsdk_log.h"

/** Internal structure of ArsdkCore native backend */
struct arsdkcore {
	/** Callbacks */
	struct arsdkcore_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
	/** Arsdk manager delegate. */
	struct arsdk_ctrl *ctrl;
	/** Controller name. */
	char *name;
	/** Controller type. */
	char *type;
	/** Whether video decoding is enabled (1) or disabled (0). */
	int video_decoding_enabled;
};

static void device_added(struct arsdk_device *device, void *userdata)
{
	struct arsdkcore *self = (struct arsdkcore *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	uint16_t handle = arsdk_device_get_handle(device);
	RETURN_IF_FAILED(handle != ARSDK_DEVICE_INVALID_HANDLE, -ENODEV);

	const struct arsdk_device_info *info = NULL;
	int res = arsdk_device_get_info(device, &info);
	RETURN_IF_FAILED(info != NULL, res);

	self->cbs.device_added(handle, info, self->userdata);
}

static void device_removed(struct arsdk_device *device, void *userdata)
{
	struct arsdkcore *self = (struct arsdkcore *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	uint16_t handle = arsdk_device_get_handle(device);

	self->cbs.device_removed(handle, self->userdata);
}

/** Documented in public header. */
struct arsdkcore *arsdkcore_create(struct sdkcore_pomp *pomp,
		const struct arsdkcore_cbs *cbs, void *userdata)
{
	RETURN_VAL_IF_FAILED(pomp != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(   cbs != NULL
	                     && cbs->device_added != NULL
	                     && cbs->device_removed != NULL, -EINVAL, NULL);

	struct pomp_loop *loop = sdkcore_pomp_get_loop(pomp);
	RETURN_VAL_IF_FAILED(loop != NULL, -EINVAL, NULL);

	struct arsdkcore *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->cbs = *cbs;
	self->userdata = userdata;

	/* create manager */
	int res = arsdk_ctrl_new(loop, &self->ctrl);
	GOTO_IF_FAILED(self->ctrl != NULL, res, err);

	/* set device callback */
	struct arsdk_ctrl_device_cbs ctrl_cbs = {
		.added = &device_added,
		.removed = &device_removed,
		.userdata = self
	};

	GOTO_IF_ERR(arsdk_ctrl_set_device_cbs(self->ctrl, &ctrl_cbs), err);

	return self;

err:
	if (self->ctrl) {
		LOG_IF_ERR(arsdk_ctrl_destroy(self->ctrl));
	}

	free(self);

	return NULL;
}

/** Documented in public header. */
void arsdkcore_set_user_agent(struct arsdkcore *self, const char *type,
		const char *name) {
	RETURN_IF_FAILED(self != NULL, -EINVAL);
	RETURN_IF_FAILED(type != NULL, -EINVAL);
	RETURN_IF_FAILED(name != NULL, -EINVAL);

	free(self->type);
	self->type = strdup(type);

	free(self->name);
	self->name = strdup(name);
}

/** Documented in public header. */
void arsdkcore_enable_video_decoding(struct arsdkcore *self, int enable)
{
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->video_decoding_enabled = enable;
}

/** Documented in public header. */
const char *arsdkcore_get_name(const struct arsdkcore *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->name == NULL ? "groundsdk" : self->name;
}

/** Documented in public header. */
const char *arsdkcore_get_type(const struct arsdkcore *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->type == NULL ? "android" : self->type;
}

/** Documented in public header. */
struct arsdk_ctrl *arsdkcore_get_ctrl(const struct arsdkcore *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->ctrl;
}

/** Documented in public header. */
struct pomp_loop *arsdkcore_get_loop(const struct arsdkcore *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return arsdk_ctrl_get_loop(self->ctrl);
}

/** Documented in public header. */
struct arsdk_device *arsdkcore_get_device(const struct arsdkcore *self,
		uint16_t handle)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return arsdk_ctrl_get_device(self->ctrl, handle);
}

/** Documented in public header. */
int arsdkcore_is_video_decoding_enabled(const struct arsdkcore *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, 0);

	return self->video_decoding_enabled;
}

/** Documented in public header. */
int arsdkcore_destroy(struct arsdkcore *self, void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	RETURN_ERRNO_IF_ERR(arsdk_ctrl_destroy(self->ctrl));

	free(self->type);
	self->type = NULL;

	free(self->name);
	self->name = NULL;

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
