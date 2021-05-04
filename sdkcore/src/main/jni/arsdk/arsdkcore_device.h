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

#include "arsdkcore.h"

/** ArsdkDevice native backend */
struct arsdkcore_device;

/** ArsdkDevice native backend callbacks */
struct arsdkcore_device_cbs {

	/** Opaque pointer from the caller, given in callbacks */
	void *userdata;

	/**
	 * Called back when a device begins to connect.
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_connecting) (void *userdata);

	/**
	 * Called back when a device is connected.
	 * @param[in] api: api capabilites
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_connected) (enum arsdk_device_api api, void *userdata);

	/**
	 * Called back when a device is disconnected.
	 * @param[in] removing: 1 if the device is disconnected because it is
	 *            also being removed from arsdk, 0 otherwise
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_disconnected) (int removing, void *userdata);

	/**
	 * Called back when connection to the device has been canceled.
	 * @param[in] removing: 1 if the connection is canceled because it is
	 *            also being removed from arsdk, 0 otherwise
	 * @param[in] reason: reason why the connection is canceled
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_connection_canceled) (int removing,
			enum arsdk_conn_cancel_reason reason, void *userdata);

	/**
	 * Called back when device link status changes.
	 * @param[in] status: new link status
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_link_status_changed) (enum arsdk_link_status status,
			void *userdata);

	/**
	 * Called back when a command is received from the device.
	 * @param[in] cmd: received command
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*command_received) (const struct arsdk_cmd *cmd, void *userdata);

	/**
	 * Called back when it is time to send non-acknowledged commands.
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*no_ack_cmd_timer_tick) (void *userdata);
};

/**
 * Creates a new ArsdkDevice native backend.
 * @param[in] device_handle: native handle of the arsdk device
 * @param[in] cbs: callbacks to send arsdk device events to
 * @param[out] ret_arsdk: a new ArsdkDevice native backend instance if
 *             successful (return value is 0)
 * @return 0 if the ArsdkDevice native backend could be created,
 *         a negative error code otherwise.
 */
int arsdkcore_device_create(const struct arsdkcore *arsdk,
		const uint16_t device_handle, const struct arsdkcore_device_cbs *cbs,
		struct arsdkcore_device **ret_device);

/**
 * Connects the ArsdkDevice.
 * @param[in] self: ArsdkDevice native backend instance
 * @return 0 if the connection could be initiated, a negative error code
 *         otherwise
 */
int arsdkcore_device_connect(struct arsdkcore_device *self);

/**
 * Sends a command to the ArsdkDevice.
 * @param[in] self: ArsdkDevice native backend instance
 * @param[in] command: command to send
 * @return 0 if the command could be sent, a negative error code otherwise
 */
int arsdkcore_device_send_command(struct arsdkcore_device *self,
		struct arsdk_cmd *command);

/**
 * Starts the non-acknowledged command loop timer.
 * Once successfully started, a timer will call back no_ack_cmd_timer_tick
 * regularly based on the provided period.
 * @param[in] self: ArsdkDevice native backend instance
 * @param[in] period: timer period, in milliseconds
 * @return 0 if the timer could be started, a negative error code otherwise
 */
int arsdkcore_device_start_no_ack_cmd_timer(struct arsdkcore_device *self,
		uint32_t period);

/**
 * Stops the non-acknowledged command loop timer.
 * @param[in] self: ArsdkDevice native backend instance
 * @return 0 if the timer could be stopped, a negative error code otherwise
 */
int arsdkcore_device_stop_no_ack_cmd_timer(struct arsdkcore_device *self);

/**
 * Retrieves the connection/discovery backend that manages this device.
 * @param[in] self: ArsdkDevice native backend instance
 * @return the associated connection/discovery backend, or NULL if none
 */
struct arsdkctrl_backend *arsdkcore_device_get_backend(
		struct arsdkcore_device *self);

/**
 * Disconnects the ArsdkDevice.
 * @param[in] self: ArsdkDevice native backend instance
 * @return 0 if the disconnection could be initiated, a negative error code
 *         otherwise
 */
int arsdkcore_device_disconnect(struct arsdkcore_device *self);

/**
 * Destroys the ArsdkDevice native backend.
 * @param[in] self: ArsdkDevice native backend instance
 * @param[out] userdata: the opaque pointer as given by caller at creation
 *             time if successful, otherwise unchanged.
 * @return 0 if the ArsdkDevice backend could be destroyed, a negative error
 *         code otherwise
 */
int arsdkcore_device_destroy(struct arsdkcore_device *self, void** userdata);
