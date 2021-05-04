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

#include "arsdkcore_device.h"
#include "arsdkcore_command.h"

#define ARSDK_LOG_TAG device
#include "arsdk_log.h"

#include <arsdkctrl/internal/arsdkctrl_internal.h>

/** ArsdkDevice native backend */
struct arsdkcore_device {
	const struct arsdkcore *arsdk;   /**< ArsdkCore native backend instance */
	struct arsdk_device *device;     /**< arsdk device delegate */
	struct pomp_timer *timer;        /**< non-ack command loop timer */
	struct arsdkcore_device_cbs cbs; /**< arsdkcore_device callbacks */
};

static void command_received(struct arsdk_cmd_itf *itf,
		const struct arsdk_cmd *cmd, void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->cbs.command_received(cmd, self->cbs.userdata);
}

static void log_command(struct arsdk_cmd_itf *itf, enum arsdk_cmd_dir dir,
		const struct arsdk_cmd *cmd, void *userdata)
{
	arsdkcore_command_log(cmd, dir);
}

static void log_link_quality(struct arsdk_cmd_itf *itf, int32_t tx_quality,
		int32_t rx_quality, int32_t rx_useful, void *userdata)
{
	LOGD("link quality [tx:%d rx:%d rx_useful:%d]", tx_quality, rx_quality,
			rx_useful);
}

static void device_connecting(struct arsdk_device *device,
		const struct arsdk_device_info *info, void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->cbs.device_connecting(self->cbs.userdata);
}

static void device_connected(struct arsdk_device *device,
		const struct arsdk_device_info *info,
		void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	/* create command interface */
	struct arsdk_cmd_itf_cbs cmd_cbs = {
		.recv_cmd = &command_received,
		.cmd_log = &log_command,
		.link_quality = &log_link_quality,
		.userdata = self
	};

	struct arsdk_cmd_itf *cmd_itf = NULL;
	int res = arsdk_device_create_cmd_itf(self->device, &cmd_cbs, &cmd_itf);
	RETURN_IF_FAILED(cmd_itf != NULL, res);

	self->cbs.device_connected(info->api, self->cbs.userdata);
}

static void device_disconnected(struct arsdk_device *device,
		const struct arsdk_device_info *info,
		void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (self->timer != NULL) {
		LOG_IF_ERR(arsdkcore_device_stop_no_ack_cmd_timer(self));
	}

	self->cbs.device_disconnected(info->state == ARSDK_DEVICE_STATE_REMOVING,
			self->cbs.userdata);
}

static void device_connection_canceled(struct arsdk_device *device,
		const struct arsdk_device_info *info,
		enum arsdk_conn_cancel_reason reason,
		void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (self->timer != NULL) {
		LOG_IF_ERR(arsdkcore_device_stop_no_ack_cmd_timer(self));
	}

	self->cbs.device_connection_canceled(
			info->state == ARSDK_DEVICE_STATE_REMOVING, reason,
			self->cbs.userdata);
}

static void device_link_status_changed(struct arsdk_device *device,
		const struct arsdk_device_info *info,
		enum arsdk_link_status status,
		void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	if (status == ARSDK_LINK_STATUS_KO && self->timer != NULL) {
		LOG_IF_ERR(arsdkcore_device_stop_no_ack_cmd_timer(self));
	}

	self->cbs.device_link_status_changed(status, self->cbs.userdata);
}

static void timer_cb(struct pomp_timer *timer, void *userdata)
{
	struct arsdkcore_device *self = (struct arsdkcore_device *) userdata;
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->cbs.no_ack_cmd_timer_tick(self->cbs.userdata);
}

/** Documented in public header. */
int arsdkcore_device_create(const struct arsdkcore *arsdk,
		const uint16_t device_handle,
		const struct arsdkcore_device_cbs *cbs,
		struct arsdkcore_device **ret_device)
{
	RETURN_ERRNO_IF_FAILED(ret_device != NULL, -EINVAL);
	*ret_device = NULL;

	RETURN_ERRNO_IF_FAILED(arsdk != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(device_handle != ARSDK_DEVICE_INVALID_HANDLE,
			-EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs != NULL, -EINVAL);

	struct arsdk_device *device = arsdkcore_get_device(arsdk, device_handle);
	RETURN_ERRNO_IF_FAILED(device != NULL, -ENODEV);

	*ret_device = calloc(1, sizeof(**ret_device));
	RETURN_ERRNO_IF_FAILED(*ret_device != NULL, -ENOMEM);

	arsdk_device_set_osdata(device, *ret_device);
	(*ret_device)->arsdk = arsdk;
	(*ret_device)->device = device;
	(*ret_device)->cbs = *cbs;

	return 0;
}

/** Documented in public header. */
int arsdkcore_device_connect(struct arsdkcore_device *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	struct pomp_loop *loop = arsdkcore_get_loop(self->arsdk);
	RETURN_ERRNO_IF_FAILED(loop != NULL, -ENODEV);

	const struct arsdk_device_info *info = NULL;
	int res = arsdk_device_get_info(self->device, &info);
	RETURN_ERRNO_IF_ERR(res);

	struct arsdk_device_conn_cfg cfg = {
		.ctrl_name = arsdkcore_get_name(self->arsdk),
		.ctrl_type = arsdkcore_get_type(self->arsdk),
	};

	struct arsdk_device_conn_cbs cbs = {
		.connecting = &device_connecting,
		.connected = &device_connected,
		.disconnected = &device_disconnected,
		.canceled = &device_connection_canceled,
		.link_status = &device_link_status_changed,
		.userdata = self
	};

	return arsdk_device_connect(self->device, &cfg, &cbs, loop);
}

/** Documented in public header. */
int arsdkcore_device_send_command(struct arsdkcore_device *self,
		struct arsdk_cmd *command)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(command != NULL, -EINVAL);

	struct arsdk_cmd_itf *cmd_itf = arsdk_device_get_cmd_itf(self->device);
	RETURN_ERRNO_IF_FAILED(cmd_itf != NULL, -ENODEV);

	return arsdk_cmd_itf_send(cmd_itf, command, NULL, NULL);
}

/** Documented in public header. */
int arsdkcore_device_start_no_ack_cmd_timer(struct arsdkcore_device *self,
		uint32_t period)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->timer == NULL, -EBUSY);

	struct pomp_loop *loop = arsdkcore_get_loop(self->arsdk);
	RETURN_ERRNO_IF_FAILED(loop != NULL, -ENODEV);

	self->timer = pomp_timer_new(loop, &timer_cb, self);
	RETURN_ERRNO_IF_FAILED(self->timer != NULL, -ENODEV);

	int res = pomp_timer_set_periodic(self->timer, period, period);
	GOTO_IF_ERR(res, err);

	return 0;

err:
	pomp_timer_destroy(self->timer);
	self->timer = NULL;

	return res;
}

/** Documented in public header. */
int arsdkcore_device_stop_no_ack_cmd_timer(struct arsdkcore_device *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(self->timer != NULL, -ENODEV);

	int res = pomp_timer_clear(self->timer);
	LOG_IF_ERR(res);

	res = pomp_timer_destroy(self->timer);
	LOG_IF_ERR(res);

	self->timer = NULL;

	return res;
}

/** Documented in public header. */
struct arsdkctrl_backend *arsdkcore_device_get_backend(struct arsdkcore_device *self) {
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return arsdk_device_get_backend(self->device);
}

/** Documented in public header. */
int arsdkcore_device_disconnect(struct arsdkcore_device *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (self->timer != NULL) {
		LOG_IF_ERR(arsdkcore_device_stop_no_ack_cmd_timer(self));
	}

	return arsdk_device_disconnect(self->device);
}

/** Documented in public header. */
int arsdkcore_device_destroy(struct arsdkcore_device *self, void** userdata)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	if (self->timer != NULL) {
		LOG_IF_ERR(arsdkcore_device_stop_no_ack_cmd_timer(self));
	}

	if (userdata != NULL) {
		*userdata = self->cbs.userdata;
	}

	free(self);

	return 0;
}
