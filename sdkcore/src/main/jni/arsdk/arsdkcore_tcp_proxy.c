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

#include "arsdkcore_tcp_proxy.h"

#define ARSDK_LOG_TAG device
#include "arsdk_log.h"

/** ArsdkTcpProxy native backend. */
struct arsdkcore_tcp_proxy {
	/** Callbacks. */
	struct arsdkcore_tcp_proxy_cbs cbs;
	/** Opaque pointer from caller, forwarded in callbacks. */
	void *userdata;
	/** Arsdk-ng TCP proxy. */
	struct arsdk_device_tcp_proxy *proxy;
};

/**
 * Proxy open success callback.
 * @param[in] proxy: arsdk proxy instance
 * @param[in] userdata: arsdkcore_tcp_proxy instance
 */
static void proxy_open(struct arsdk_device_tcp_proxy *proxy,
		uint16_t port, void *userdata)
{
	struct arsdkcore_tcp_proxy *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	RETURN_IF_FAILED(self->cbs.on_open != NULL, -EPROTO);

	const char *address = arsdk_device_tcp_proxy_get_addr(self->proxy);

	self->cbs.on_open(address ? 0 : -EPIPE, address, port, self->userdata);
	// Don't report close events after open: unsupported by upper layers
	self->cbs.on_open = NULL;
}

/**
 * Proxy open failure/unexpected close callback.
 * @param[in] proxy: arsdk proxy instance
 * @param[in] userdata: arsdkcore_tcp_proxy instance
 */
static void proxy_close(struct arsdk_device_tcp_proxy *proxy, void *userdata)
{
	struct arsdkcore_tcp_proxy *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (self->cbs.on_open) {
		self->cbs.on_open(-EPIPE, NULL, 0, self->userdata);
	}
}

/** Documented in public header. */
struct arsdkcore_tcp_proxy *arsdkcore_tcp_proxy_create(
		const struct arsdkcore *arsdk, uint16_t device_handle,
		enum arsdk_device_type device_type, uint16_t port,
		const struct arsdkcore_tcp_proxy_cbs *cbs, void *userdata)
{
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(device_handle != ARSDK_DEVICE_INVALID_HANDLE, -EINVAL,
			NULL);
	RETURN_VAL_IF_FAILED(   cbs != NULL
	                     && cbs->on_open != NULL, -EINVAL, NULL);

	struct arsdk_device *device = arsdkcore_get_device(arsdk, device_handle);
	RETURN_VAL_IF_FAILED(device != NULL, -ENODEV, NULL);

	struct arsdkcore_tcp_proxy *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->cbs = *cbs;
	self->userdata = userdata;

	struct arsdk_device_tcp_proxy_cbs proxy_cbs = {
		.open = proxy_open,
		.close = proxy_close,
		.userdata = self
	};

	int res = arsdk_device_create_tcp_proxy(device, device_type, port,
			&proxy_cbs, &self->proxy);
	GOTO_IF_FAILED(self->proxy != NULL, res, err);

	return self;

err:
	free(self);

	return NULL;
}

/** Documented in public header. */
int arsdkcore_tcp_proxy_destroy(struct arsdkcore_tcp_proxy *self,
		void **userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->proxy != NULL, -EPROTO);

	LOG_IF_ERR(arsdk_device_destroy_tcp_proxy(self->proxy));
	self->proxy = NULL;

	if (userdata) {
		*userdata = self->userdata;
	}

	free(self);

	return 0;
}
