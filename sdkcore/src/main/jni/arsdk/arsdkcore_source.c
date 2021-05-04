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

#include "arsdkcore_source.h"

#define ARSDK_LOG_TAG stream
#include "arsdk_log.h"

#include <sdkcore/internal/sdkcore_source.h>

#include <arsdkctrl/internal/arsdkctrl_internal.h>

/** ArsdkStream source. */
struct arsdkcore_source {
	/** SdkCoreStream source parent. */
	struct sdkcore_source parent;
	/** ArsdkCore native backend. */
	const struct arsdkcore *arsdk;
	/** Device RTSP proxy; used for MUX-proxied devices, NULL otherwise. */
	struct arsdk_device_tcp_proxy *rtsp_proxy;
	/** Handle of the device that provides the stream. */
	uint16_t device_handle;
	/** Stream URL. */
	char *url;

	/** Context data only for MUX-proxied devices, undefined otherwise */
	struct {
		/** Device RTSP proxy. */
		struct arsdk_device_tcp_proxy *rtsp_proxy;
		/** Pdraw intsance to open this source for once the proxy opens. */
		struct pdraw *pdraw;
	} proxy_ctx;
};

/**
 * Function called at the tcp proxy opening.
 *
 * @param self : Proxy object.
 * @param localport : Proxy local socket port.
 * @param userdata : User data.
 */
static void proxy_open(struct arsdk_device_tcp_proxy *proxy, uint16_t localport,
		void *userdata)
{
	struct arsdkcore_source *self = userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct arsdk_device *device = arsdkcore_get_device(self->arsdk,
			self->device_handle);
	RETURN_IF_FAILED(device != NULL, -ENODEV);
	RETURN_IF_FAILED(self->proxy_ctx.rtsp_proxy != NULL, -EPROTO);
	RETURN_IF_FAILED(self->proxy_ctx.pdraw != NULL, -EPROTO);

	int port = arsdk_device_tcp_proxy_get_port(self->proxy_ctx.rtsp_proxy);
	RETURN_IF_ERR(port);

	struct arsdkctrl_backend *backend = arsdk_device_get_backend(device);
	RETURN_IF_FAILED(backend != NULL, -ENODEV);

	struct arsdkctrl_backend_mux *backend_mux =
			arsdkctrl_backend_get_child(backend);
	RETURN_IF_FAILED(backend_mux != NULL, -ENODEV);

	struct mux_ctx *mux = arsdkctrl_backend_mux_get_mux_ctx(backend_mux);
	RETURN_IF_FAILED(mux != NULL, -ENODEV);

	char *url = NULL;
	int res = asprintf(&url, "rtsp://127.0.0.1:%d/%s", port, self->url) == -1 ?
			-ENOMEM : 0;
	GOTO_IF_ERR(res, out);

	res = pdraw_open_url_mux(self->proxy_ctx.pdraw, url, mux);
	GOTO_IF_ERR(res, out);

out:
	free(url);
}

/**
 * Function called at the tcp proxy closing.
 *
 * @param self : Proxy object.
 * @param userdata : User data.
 */
static void proxy_close(struct arsdk_device_tcp_proxy *proxy, void *userdata)
{
	// TODO: for the time being, our design does not know how to handle neither
	//       proxy open failure nor proxy unexpected close.
	LOG_ERR(-ENOSYS);
}

/**
 * Opens the source.
 * @param[in] source: arsdkcore_source instance
 * @param[in] pdraw: pdraw instance to open the source with
 * @return 0 in case of success, a negative errno otherwise
 */
static int source_open(const struct sdkcore_source *source, struct pdraw *pdraw)
{
	struct arsdkcore_source *self = (struct arsdkcore_source *) source;
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	RETURN_ERRNO_IF_FAILED(self->arsdk != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->device_handle != 0, -EINVAL);

	struct arsdk_device *device = arsdkcore_get_device(self->arsdk,
			self->device_handle);
	RETURN_ERRNO_IF_FAILED(device != NULL, -ENODEV);

	const struct arsdk_device_info *info = NULL;
	int res = arsdk_device_get_info(device, &info);
	RETURN_ERRNO_IF_FAILED(info != NULL, res);

	char *url = NULL;
	switch (info->backend_type) {
	case ARSDK_BACKEND_TYPE_NET:
		switch (info->type) {
		case ARSDK_DEVICE_TYPE_ANAFI4K:
		case ARSDK_DEVICE_TYPE_ANAFI_THERMAL:
		case ARSDK_DEVICE_TYPE_ANAFI_UA:
		case ARSDK_DEVICE_TYPE_ANAFI_USA:
			res = asprintf(&url, "rtsp://%s/%s", info->addr, self->url) == -1 ?
				-ENOMEM : 0;
			GOTO_IF_ERR(res, out);

			res = pdraw_open_url(pdraw, url);
			GOTO_IF_ERR(res, out);
			break;
		default:
			res = -ENOSYS;
			LOG_ERR(res);
			goto out;
			break;
		}
		break;
	case ARSDK_BACKEND_TYPE_MUX:
		switch (info->type) {
		case ARSDK_DEVICE_TYPE_SKYCTRL_3:
		case ARSDK_DEVICE_TYPE_SKYCTRL_UA:

			self->proxy_ctx.pdraw = pdraw;

			struct arsdk_device_tcp_proxy_cbs cbs = {
				.open = proxy_open,
				.close = proxy_close,
				.userdata = self
			};

			res = arsdk_device_create_tcp_proxy(device, info->type, 554,
					&cbs, &self->proxy_ctx.rtsp_proxy);
			GOTO_IF_FAILED(self->proxy_ctx.rtsp_proxy != NULL, res, out);
			/** wait proxy opening */
			break;
		default:
			res = -ENOSYS;
			LOG_ERR(res);
			goto out;
			break;

		}
		break;
	default:
		res = -ENOSYS;
		LOG_ERR(res);
		goto out;
	}

	res = 0;

out:
	free(url);

	return res;
}

/**
 * Source socket creation callback.
 * @param[in] source: arsdkcore_source instance
 * @param[in] fd: created socket descriptor
 */
static void source_socket_created(const struct sdkcore_source *source, int fd)
{
	struct arsdkcore_source *self = (struct arsdkcore_source *) source;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	struct arsdk_device *device = arsdkcore_get_device(self->arsdk,
			self->device_handle);
	RETURN_IF_FAILED(device != NULL, -ENODEV);

	struct arsdkctrl_backend *backend = arsdk_device_get_backend(device);
	RETURN_IF_FAILED(backend != NULL, -ENODEV);

	LOG_IF_ERR(arsdkctrl_backend_socket_cb(backend, fd,
			ARSDK_SOCKET_KIND_VIDEO));
}

/**
 * Releases the source.
 * @param[in] source: arsdkcore_source instance to destroy
 */
static void source_release(struct sdkcore_source *source)
{
	struct arsdkcore_source *self = (struct arsdkcore_source *) source;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	arsdkcore_source_destroy(self);
}

/** Documented in public header. */
struct arsdkcore_source *arsdkcore_source_create (
		const struct arsdkcore *arsdk, uint16_t device_handle, const char *url)
{
	RETURN_VAL_IF_FAILED(arsdk != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(device_handle != 0, -EINVAL, NULL);

	struct arsdkcore_source *self = calloc(1, sizeof(*self));
	RETURN_VAL_IF_FAILED(self != NULL, -ENOMEM, NULL);

	self->arsdk = arsdk;
	self->device_handle = device_handle;
	self->url = strdup(url);
	GOTO_IF_FAILED(self->url != NULL, -ENOMEM, err);

	self->parent.open = source_open;
	self->parent.on_socket_created = source_socket_created;
	self->parent.release = source_release;

	return self;

err:
	free(self);

	return NULL;
}

/** Documented in public header. */
int arsdkcore_source_destroy(struct arsdkcore_source *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	self->arsdk = NULL;
	self->device_handle = 0;

	free(self->url);
	self->url = NULL;

	if (self->rtsp_proxy) {
		LOG_IF_ERR(arsdk_device_destroy_tcp_proxy(self->rtsp_proxy));
		self->rtsp_proxy = NULL;
	}

	free(self);

	return 0;
}
