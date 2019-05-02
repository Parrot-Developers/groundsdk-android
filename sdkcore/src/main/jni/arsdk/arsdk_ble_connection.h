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
#include <arsdkctrl/internal/arsdkctrl_internal.h>

/**
 * Creates a new BLE connection.
 * @param[in] device : BLE device to connect to.
 * @param[in] addr: BLE device address.
 * @param[in] cbs: connection callbacks.
 * @param[in] loop: main pomp loop.
 * @param[out] ret_conn: new BLE connection instance.
 * @return 0 if the connection could be created, a negative error code
 *         otherwise.
 */
int arsdk_ble_connection_new(struct arsdk_device *device, const char *addr,
		const struct arsdk_device_conn_internal_cbs *cbs,
		struct pomp_loop *loop, struct arsdk_device_conn **ret_conn);

/**
 * Destroys the given BLE connection.
 * @param[in] self: BLE connection instance to destroy.
 */
void arsdk_ble_connection_destroy(struct arsdk_device_conn *self);

/**
 * Tells whether the given BLE native connection is attached to a java handler.
 * @param[in] self: BLE native connection instance.
 * @return 0 if the connection is attached, a negative error code otherwise.
 */
int arsdk_ble_connection_is_attached(struct arsdk_device_conn *self);

/**
 * Attaches the given java connection handler to the native BLE connection.
 * @param[in] self: BLE native connection instance.
 * @param[in] jself: java connection handler global ref.
 */
void arsdk_ble_connection_attach_jself(struct arsdk_device_conn *self,
		void *jself);

/**
 * Detaches the given java connection handler from the native BLE connection.
 * @param[in] self: BLE native connection instance.
 * @return the detached java connection handler global ref, or NULL if none.
 */
void *arsdk_ble_connection_detach_jself(struct arsdk_device_conn *self);

/**
 * Gets the address of the BLE device managed by the given connection.
 * @param[in] self: BLE connection instance.
 * @return the managed BLE device address, or NULL if none.
 */
const char *arsdk_ble_connection_get_address(struct arsdk_device_conn *self);

/**
 * Transmits received data from the BLE device managed by the connection.
 * @param[in] self: BLE connection instance.
 * @param[in] header: data header.
 * @param[in] payload: data payload.
 * @param[in] len: data payload len.
 * @return 0 if the data could be forwarded, a negative error code otherwise.
 */
int arsdk_ble_connection_receive_data(struct arsdk_device_conn *self,
		struct arsdk_transport_header* header, void *payload, size_t len);

/**
 * Notifies that the device managed by the connection is disconnected.
 * @param[in] self: BLE connection instance.
 * @return 0 if the notification could be sent, a negative error code
 *         otherwise.
 */
int arsdk_ble_connection_disconnected(struct arsdk_device_conn *self);

/**
 * Notifies that the device managed by the connection is connecting.
 * @param[in] self: BLE connection instance.
 * @return 0 if the notification could be sent, a negative error code
 *         otherwise.
 */
int arsdk_ble_connection_connecting(struct arsdk_device_conn *self);

/**
 * Notifies that the device managed by the connection is connected.
 * @param[in] self: BLE connection instance.
 * @return 0 if the notification could be sent, a negative error code
 *         otherwise.
 */
int arsdk_ble_connection_connected(struct arsdk_device_conn *self);

/**
 * Notifies that the device managed by the connection failed to connect.
 * @param[in] self: BLE connection instance.
 * @return 0 if the notification could be sent, a negative error code
 *         otherwise.
 */
int arsdk_ble_connection_failed(struct arsdk_device_conn *self);
