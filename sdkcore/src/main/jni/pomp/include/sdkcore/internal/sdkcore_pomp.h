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

#include <sdkcore/internal/sdkcore_internal_api.h>

#include <libpomp.h>

/** SdkCorePomp native backend. */
struct sdkcore_pomp;

/**
 * Creates a new sdkcore pomp instance.
 * @param[in] context_flag: points to context flag byte; may be NULL
 * @return a new sdkcore pomp instance in case of success, NULL otherwise
 */
struct sdkcore_pomp *sdkcore_pomp_create(char *context_flag);

/**
 * Accesses internal pomp loop.
 * @param[in] self: sdkcore pomp instance to operate on
 * @return internal pomp loop in case of success, NULL otherwise
 */
struct pomp_loop *sdkcore_pomp_get_loop(struct sdkcore_pomp *self);

/**
 * Destroys sdkcore pomp.
 * @param[in] self: sdkcore pomp instance to destroy
 * @return 0 in case of success, a negative errno otherwise
 */
int sdkcore_pomp_destroy(struct sdkcore_pomp *self);
