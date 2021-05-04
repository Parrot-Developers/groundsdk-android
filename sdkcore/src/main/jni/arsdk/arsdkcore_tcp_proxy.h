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

/** ArsdkTcpProxy native backend. */
struct arsdkcore_tcp_proxy;

/** ArsdkTcpProxy native backend callbacks */
struct arsdkcore_tcp_proxy_cbs {
	/**
	 * Called back upon proxy open success or failure.
	 * @param[in] error: 0 in case of open success, a negative errno otherwise
	 * @param[in] address: proxy local address in case of open success, NULL
	 *                     otherwise
	 * @param[in] port: proxy local port in case of open success, undefined
	 *                  otherwise
	 * @param[in] userdata: opaque pointer from the caller
	 */
	void (*on_open) (int error, const char *address, uint16_t port,
			void *userdata);
};

/**
 * Creates a new TCP proxy backend instance.
 * @param[in] arsdk: ArsdkCore native backend
 * @param[in] device_handle: handle of the device that will handle the proxy
 * @param[in] device_type: type of device to be accessed through the proxy
 * @param[in] port: remote port to proxy
 * @param[in] cbs: backend callbacks
 * @param[in] userdata: opaque pointer from caller, forwarded in callbacks
 * @return a new TCP proxy backend instance in case of success, NULL otherwise
 */
struct arsdkcore_tcp_proxy *arsdkcore_tcp_proxy_create(
		const struct arsdkcore *arsdk,
		uint16_t device_handle,
		enum arsdk_device_type device_type,
		uint16_t port,
		const struct arsdkcore_tcp_proxy_cbs *cbs,
		void *userdata);

/**
 * Destroys TCP proxy backend.
 * @param[in] self: TCP proxy backend instance to operate on
 * @param[out] userdata: upon success, contains userdata provided at creation;
 *                       otherwise unchanged; may be NULL
 * @return 0 in case of success, a negative errno otherwise. In particular:
 *         -EPROTO in case the backend has been destroyed
 */
int arsdkcore_tcp_proxy_destroy(
		struct arsdkcore_tcp_proxy *self,
		void **userdata);
