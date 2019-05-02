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

#include "arsdk_ble_connection.h"

#include "arsdk_ble_connection_jni.h"
#include "arsdkctrl_backend_ble.h"

#define ARSDK_LOG_TAG backend
#include "arsdk_log.h"

/** BLE connection internal data */
struct arsdk_device_conn {
	struct arsdk_device                   *device;    /**< managed device */
	struct arsdk_transport                *transport; /**< conn. transport */
	struct pomp_loop                      *loop;      /**< main pomp loop */
	char                                  *addr;      /**< device address */
	void                                  *jself;     /**< java handler ref */
	struct arsdk_device_conn_internal_cbs  cbs;       /**< conn. callbacks */
};

/** Documented in public header. */
int arsdk_ble_connection_new(struct arsdk_device *device, const char *addr,
		const struct arsdk_device_conn_internal_cbs *cbs,
		struct pomp_loop *loop, struct arsdk_device_conn **ret_conn)
{
	RETURN_ERRNO_IF_FAILED(ret_conn != NULL, -EINVAL);
	*ret_conn = NULL;

	RETURN_ERRNO_IF_FAILED(device != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(addr != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs->connecting != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs->connected != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs->disconnected != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(cbs->canceled != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(loop != NULL, -EINVAL);

	char *addr_copy = strdup(addr);
	RETURN_ERRNO_IF_FAILED(addr_copy != NULL, -ENOMEM);

	/* Allocate structure */
	*ret_conn = calloc(1, sizeof(**ret_conn));
	RETURN_ERRNO_IF_FAILED(*ret_conn != NULL, -ENOMEM);

	/* Setup */
	(*ret_conn)->device = device;
	(*ret_conn)->loop = loop;
	(*ret_conn)->addr = addr_copy;
	(*ret_conn)->cbs = *cbs;

	/* Success */
	return 0;
}

/** Documented in public header. */
void arsdk_ble_connection_destroy(struct arsdk_device_conn *self)
{
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	/* ensures the java handler is detached */
	LOG_IF_FAILED(self->jself == NULL, -EINVAL);

	free(self->addr);
	free(self);
}

/** Documented in public header. */
int arsdk_ble_connection_is_attached(struct arsdk_device_conn *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	return self->jself == NULL ? -1 : 0;
}

/** Documented in public header. */
void arsdk_ble_connection_attach_jself(struct arsdk_device_conn *self,
		void *jself)
{
	RETURN_IF_FAILED(self != NULL, -EINVAL);

	self->jself = jself;
}

/** Documented in public header. */
void *arsdk_ble_connection_detach_jself(struct arsdk_device_conn *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);
	RETURN_VAL_IF_FAILED(self->jself != NULL, -EINVAL, NULL);

	void *ret = self->jself;
	self->jself = NULL;

	return ret;
}

/** Documented in public header. */
const char *arsdk_ble_connection_get_address(struct arsdk_device_conn *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->addr;
}

/**
 * Disposes of the given connection transport instance.
 * @param base: transport instance base.
 * @return 0, for success, as the connection does not really manages a
 *         transport.
 */
static int arsdk_ble_transport_dispose(struct arsdk_transport *base) {
	return 0;
}

/**
 * Starts the given connection transport.
 * @return 0, for success, as the connection does not really manages a
 *         transport.
 */
static int arsdk_ble_transport_start(struct arsdk_transport *base) {
	return 0;
}

/**
 * Stops the given connection transport.
 * @return 0, for success, as the connection does not really manages a
 *         transport.
 */
static int arsdk_ble_transport_stop(struct arsdk_transport *base) {
	return 0;
}

/**
 * Forwards data to be send to the remote BLE device managed by the connection.
 * @param base: transport instance base.
 * @param header: data header.
 * @param payload: data payload.
 * @param extra_hdr: extra data header.
 * @param extra_hdrlen: extra data header len.
 * @return 0 if the data could be forwarded, a negative error code otherwise.
 */
static int arsdk_ble_transport_send_data(struct arsdk_transport *base,
		const struct arsdk_transport_header *header,
		const struct arsdk_transport_payload *payload,
		const void *extra_hdr, size_t extra_hdrlen)
{
	struct arsdk_device_conn *conn = arsdk_transport_get_child(base);
	RETURN_ERRNO_IF_FAILED(conn != NULL, -EINVAL);

	RETURN_ERRNO_IF_FAILED(header != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(payload != NULL, -EINVAL);
	RETURN_ERRNO_IF_FAILED(extra_hdrlen == 0 || extra_hdr != NULL, -EINVAL);

	arsdk_ble_connection_send_data_jni(conn->jself, header, payload, extra_hdr,
			extra_hdrlen);

	return 0;
}

/** Connection transport callbacks */
static const struct arsdk_transport_ops s_arsdk_ble_transport_ops = {
    .dispose = &arsdk_ble_transport_dispose,
    .start = &arsdk_ble_transport_start,
    .stop = &arsdk_ble_transport_stop,
	.send_data = &arsdk_ble_transport_send_data
};

/** Documented in public header. */
int arsdk_ble_connection_receive_data(struct arsdk_device_conn *self,
		struct arsdk_transport_header* header, void *data, size_t len)
{
	struct pomp_buffer *buffer = pomp_buffer_new_with_data(data, len);
	RETURN_ERRNO_IF_FAILED(buffer != NULL, -EINVAL);

	struct arsdk_transport_payload payload;
	arsdk_transport_payload_init_with_buf(&payload, buffer);

	int res = arsdk_transport_recv_data(self->transport, header, &payload);
	LOG_IF_ERR(res);

	pomp_buffer_unref(buffer);

	return res;
}

/** Documented in public header. */
int arsdk_ble_connection_disconnected(struct arsdk_device_conn *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	(*self->cbs.disconnected)(self->device, self, self->cbs.userdata);

	int res = 0;
	if (self->transport) {
		arsdk_transport_stop(self->transport);

		res = arsdk_transport_destroy(self->transport);
		LOG_IF_ERR(res);

		self->transport = NULL;
	}

	return res;
}

/** Documented in public header. */
int arsdk_ble_connection_connecting(struct arsdk_device_conn *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	(*self->cbs.connecting)(self->device, self, self->cbs.userdata);

	return 0;
}

/** Documented in public header. */
int arsdk_ble_connection_connected(struct arsdk_device_conn *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	/* Use 'child' pointer in transport to hold the connection */
	struct arsdk_transport *transport = NULL;
	int res = arsdk_transport_new(self, &s_arsdk_ble_transport_ops, self->loop,
			0, "ble", &transport);
	RETURN_ERRNO_IF_FAILED(transport != NULL, res);

	self->transport = transport;

	(*self->cbs.connected)(self->device, NULL, self, transport,
			self->cbs.userdata);

	return 0;
}

/** Documented in public header. */
int arsdk_ble_connection_failed(struct arsdk_device_conn *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	(*self->cbs.canceled)(self->device, self, ARSDK_CONN_CANCEL_REASON_LOCAL,
			self->cbs.userdata);

	return 0;
}
