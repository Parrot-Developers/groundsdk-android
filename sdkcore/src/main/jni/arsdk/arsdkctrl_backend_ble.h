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

/** BLE backend */
struct arsdkctrl_backend_ble;

/**
 * Creates a new BLE backend.
 * @param[in] ctrl : arsdk controller owning this backend.
 * @param[in] jself : global ref to the java backend instance.
 * @param[out] ret_backend: new BLE backend instance.
 * @return 0 if the backend could be created, a negative error code otherwise.
 */
int arsdkctrl_backend_ble_new(struct arsdk_ctrl *ctrl,
		void *jself, struct arsdkctrl_backend_ble **ret_backend);

/**
 * Detaches the java handler and destroys the given BLE backend.
 * @param[in] self: BLE backend instance to destroy.
 * @return the detached java handler global reference.
 */
void *arsdkctrl_backend_ble_destroy(struct arsdkctrl_backend_ble *self);

/**
 * Gets the parent backend of the given BLE backend.
 * @param[in] self: BLE backend instance.
 * @return the BLE backend instance's parent, or NULL if none
 */
struct arsdkctrl_backend *arsdkctrl_backend_ble_get_parent(
		struct arsdkctrl_backend_ble *self);
