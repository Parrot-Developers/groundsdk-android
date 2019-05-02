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

#include "arsdkctrl_backend_ble.h"
#include "arsdk_backend_ble_jni.h"
#include "arsdk_ble_connection.h"

#define ARSDK_LOG_TAG backend
#include "arsdk_log.h"

#include <arsdkctrl/internal/arsdkctrl_internal.h>

/** BLE backend internal data */
struct arsdkctrl_backend_ble {
	struct arsdkctrl_backend *parent; /**< backend base */
	void                     *jself;  /**< java backend handler global ref */
	struct pomp_loop         *loop;   /**< main pomp loop */
};

/**
 * Called back when the given connection needs to be stopped.
 * @param[in] base: backend instance base.
 * @param[in] device: device to disconnect.
 * @param[in] conn: connection to stop.
 * @return 0 if the connection could be stopped, a negative error code
 *         otherwise.
 */
static int arsdkctrl_backend_ble_stop_device_conn(
		struct arsdkctrl_backend *base, struct arsdk_device *device,
		struct arsdk_device_conn *conn)
{
	struct arsdkctrl_backend_ble *self = arsdkctrl_backend_get_child(base);
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	RETURN_ERRNO_IF_FAILED(device != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(conn != NULL, -EINVAL);

	/* close java connection handler */
	arsdk_backend_ble_connection_close_jni(self->jself,
			arsdk_ble_connection_get_address(conn));

	/* native connection proxy is owned and closed by java handler */
	return 0;
}

/**
 * Called back when a new connection with the given device needs to be started.
 * @param[in] base: backend instance base.
 * @param[in] device: device to connect.
 * @param[in] info: device info.
 * @param[in] cfg: connection config.
 * @param[in] cbs: connection callbacks.
 * @param[in] loop: main pomp loop.
 * @param[out] ret_conn: new connection instance.
 * @return 0 if the connection could be created and started, a negative error
 *         code otherwise.
 */
static int arsdkctrl_backend_ble_start_device_conn(struct arsdkctrl_backend *base,
		struct arsdk_device *device, struct arsdk_device_info *info,
		const struct arsdk_device_conn_cfg *cfg,
		const struct arsdk_device_conn_internal_cbs *cbs,
		struct pomp_loop *loop,	struct arsdk_device_conn **ret_conn)
{
	struct arsdkctrl_backend_ble *self = arsdkctrl_backend_get_child(base);

	RETURN_ERRNO_IF_FAILED(ret_conn != NULL, -EINVAL);
	*ret_conn = NULL;

	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(info != NULL, -EINVAL);

	/* create native connection proxy */
	int res = arsdk_ble_connection_new(device, info->addr, cbs, loop, ret_conn);
	RETURN_ERRNO_IF_FAILED(*ret_conn != NULL, res);

	/* create java connection handler */
	res = arsdk_backend_ble_connection_open_jni(self->jself, info->addr,
			*ret_conn);
	GOTO_IF_ERR(res, err);

	/* check native connection is owned by java handler */
	res = arsdk_ble_connection_is_attached(*ret_conn);
	GOTO_IF_ERR(res, err);

	return 0;

err:;
	arsdk_ble_connection_destroy(*ret_conn);
	return res;
}

/** Backend start/stop callbacks */
static const struct arsdkctrl_backend_ops s_arsdkctrl_backend_ble_ops = {
	.stop_device_conn = &arsdkctrl_backend_ble_stop_device_conn,
	.start_device_conn = &arsdkctrl_backend_ble_start_device_conn,
};

/** Documented in public header. */
int arsdkctrl_backend_ble_new(struct arsdk_ctrl *ctrl, void *jself,
		struct arsdkctrl_backend_ble **ret_backend)
{
	/* Allocate structure */
	*ret_backend  = calloc(1, sizeof(**ret_backend));
	RETURN_ERRNO_IF_FAILED(*ret_backend != NULL, -ENOMEM);

	/* Setup base structure */
	int res = arsdkctrl_backend_new(*ret_backend , ctrl, "ble",
			ARSDK_BACKEND_TYPE_BLE, &s_arsdkctrl_backend_ble_ops,
			&(*ret_backend)->parent);
	GOTO_IF_ERR(res, err);

	/* Initialize structure */
	(*ret_backend)->loop = arsdk_ctrl_get_loop(ctrl);
	(*ret_backend)->jself = jself;

	/* Success */
	return 0;

err:
	free(*ret_backend);
	*ret_backend = NULL;

	return res;
}

/** Documented in public header. */
void *arsdkctrl_backend_ble_destroy(struct arsdkctrl_backend_ble *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	/* destroy backend */
	LOG_IF_ERR(arsdkctrl_backend_destroy(self->parent));

	/* Free resources */
	void *jbackend = self->jself;
	free(self);

	return jbackend;
}

/** Documented in public header. */
struct arsdkctrl_backend *arsdkctrl_backend_ble_get_parent(
		struct arsdkctrl_backend_ble *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->parent;
}
