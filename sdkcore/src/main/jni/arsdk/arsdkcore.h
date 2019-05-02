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

#include <sdkcore/internal/sdkcore_pomp.h>

#include <arsdkctrl/arsdkctrl.h>

/** ArsdkCore native backend */
struct arsdkcore;

/** ArsdkCore native backend callbacks */
struct arsdkcore_cbs {

	/**
	 * Called back when a device is added.
	 * @param[in] handle: arsdk device native handle
	 * @param[in] info: arsdk device info
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_added) (uint16_t handle,
			 const struct arsdk_device_info *info, void *userdata);

	/**
	 * Called back when a device is removed.
	 * @param[in] device_handle: native handle of the device
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*device_removed) (uint16_t device_handle, void *userdata);
};

/**
 * Creates a new arsdkcore instance.
 * @param[in] pomp: SdkCorePomp native backend, providing pomp loop to run arsdk
 *                  manager into
 * @param[in] cbs: callbacks to send arsdk manager events to
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new arsdkcore instance in case of success, NULL otherwise
 */
struct arsdkcore *arsdkcore_create(
		struct sdkcore_pomp *pomp,
		const struct arsdkcore_cbs *cbs,
		void *userdata);

/**
 * Sets user agent info.
 * Note: such info is forwarded in the json when connecting to a device.
 * @param[in] self: arsdkcore instance to operate on
 * @param[in] type: controller type
 * @param[in] name: controller name
 */
void arsdkcore_set_user_agent(
		struct arsdkcore *self,
		const char *type,
		const char *name);

/**
 * Configures video decoding.
 * @param[in] self: arsdkcore instance to operate on
 * @param[in] enable: 1 to enable video decoding, 0 to disable it
 */
void arsdkcore_enable_video_decoding(struct arsdkcore *self, int enable);

/**
 * Gets controller name.
 * @param[in] self: arsdkcore instance to operate on
 * @return controller name in case of success, NULL otherwise
 */
const char *arsdkcore_get_name(const struct arsdkcore *self);

/**
 * Gets controller type.
 * @param[in] self: arsdkcore instance to operate on
 * @return controller type in case of success, NULL otherwise
 */
const char *arsdkcore_get_type(const struct arsdkcore *self);

/**
 * Gets the arsdk manager instance.
 * @param[in] self: arsdkcore instance to operate on
 * @return arsdk manager instance in case of success, NULL otherwise
 */
struct arsdk_ctrl *arsdkcore_get_ctrl(const struct arsdkcore *self);

/**
 * Gets the pomp loop instance.
 * @param[in] self: arsdkcore instance to operate on
 * @return pomp loop instance in case of success, NULL otherwise
 */
struct pomp_loop *arsdkcore_get_loop(const struct arsdkcore *self);

/**
 * Gets an arsdk device instance from its native handle.
 * @param[in] self: arsdkcore instance to operate on
 * @param[in] handle: native handle of the device to obtain
 * @return the corresponding arsdk device, or NULL if none found with
 *         such a handle
 */
struct arsdk_device *arsdkcore_get_device(const struct arsdkcore *self,
		uint16_t handle);

/**
 * Retrieves enabling of the video decoding.
 * @param[in] self: ArsdkCore native backend instance
 * @return 0 in case video decoding is disabled, 1 in case it is enabled
 */
int arsdkcore_is_video_decoding_enabled(const struct arsdkcore *self);

/**
 * Destroys the ArsdkCore native backend.
 * @param[in] self: arsdkcore instance to destroy
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise
 */
int arsdkcore_destroy(struct arsdkcore *self, void **userdata);
