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

#ifndef ULOG_TAG
	#ifdef SDKCORE_LOG_TAG
		#define ULOG_TAG ___CONCAT(sdkcore_,SDKCORE_LOG_TAG)
	#else
		#define ULOG_TAG sdkcore
	#endif
#endif

#include <ulog.h>
#define SDKCORE_LOG_DEFINE_TAG(name) \
	__attribute__((__weak__)) __ULOG_DECL(name)
SDKCORE_LOG_DEFINE_TAG(ULOG_TAG);
#include <errno.h>

/** Log as debug */
#define LOGD(_fmt, ...) ULOGD(_fmt, ##__VA_ARGS__)

/** Log as info */
#define LOGI(_fmt, ...) ULOGI(_fmt, ##__VA_ARGS__)

/** Log as warning */
#define LOGW(_fmt, ...) ULOGW(_fmt, ##__VA_ARGS__)

/** Log as error */
#define LOGE(_fmt, ...) ULOGE(_fmt, ##__VA_ARGS__)

/** Log error */
#define LOG_ERR(_err) \
	do { \
		int err_LOG_ERR_ = (_err); \
		LOGE("%s:%d: err=%d(%s)", __func__, __LINE__, \
				err_LOG_ERR_, strerror(-err_LOG_ERR_)); \
	} while(0)

/** Log error if condition fails */
#define LOG_IF_FAILED(_cond, _err) \
	do { \
		if (!(_cond)) { \
			LOG_ERR(_err); \
		} \
	} while (0)

/** Log if error (err < 0) */
#define LOG_IF_ERR(_err) \
	do { \
		int err_LOG_IF_ERR_ = (_err); \
		LOG_IF_FAILED(err_LOG_IF_ERR_ >= 0, err_LOG_IF_ERR_); \
	} while (0)

/** Log error if condition fails and return */
#define RETURN_IF_FAILED(_cond, _err) \
	do { \
		if (!(_cond)) { \
			LOG_ERR(_err); \
			return; \
		} \
	} while (0)

/** Log and return if error (err < 0) */
#define RETURN_IF_ERR(_err) \
	do { \
		int err_RETURN_IF_ERR_ = (_err); \
		RETURN_IF_FAILED(err_RETURN_IF_ERR_ >= 0, err_RETURN_IF_ERR_); \
	} while (0)

/** Log error if condition fails and goto label */
#define GOTO_IF_FAILED(_cond, _err, _label) \
	do { \
		if (!(_cond)) { \
			LOG_ERR(_err); \
			goto _label; \
		} \
	} while (0)

/** Log and goto label if error (err < 0) */
#define GOTO_IF_ERR(_err, _label) \
	do { \
		int err_GOTO_IF_ERR_ = (_err); \
		GOTO_IF_FAILED(err_GOTO_IF_ERR_ >= 0, err_GOTO_IF_ERR_, _label); \
	} while(0)

/** Log error and return error */
#define RETURN_ERR(_err) \
	do { \
		int err_RETURN_ERR_ = (_err); \
		LOG_ERR(err_RETURN_ERR_); \
		return err_RETURN_ERR_; \
	} while (0)

/** Log error if condition fails and and return error */
#define RETURN_ERRNO_IF_FAILED(_cond, _err) \
	do { \
		int err_RETURN_ERRNO_IF_FAILED_ = (_err); \
		if (!(_cond)) { \
			LOG_ERR(err_RETURN_ERRNO_IF_FAILED_); \
			return err_RETURN_ERRNO_IF_FAILED_; \
		} \
	} while (0)

/** Log and return error if error (err < 0) */
#define RETURN_ERRNO_IF_ERR(_err) \
	do { \
		int err_RETURN_ERRNO_IF_ERR_ = (_err); \
		RETURN_ERRNO_IF_FAILED(err_RETURN_ERRNO_IF_ERR_ >= 0, \
				err_RETURN_ERRNO_IF_ERR_); \
	} while (0)

/** Log error if condition fails and return a value */
#define RETURN_VAL_IF_FAILED(_cond, _err, _val) \
	do { \
		if (!(_cond)) { \
			LOG_ERR(_err); \
			return (_val); \
		} \
	} while (0)

/** Log and return a value if error (err < 0) */
#define RETURN_VAL_IF_ERR(_err, _val) \
	do { \
		int err_RETURN_VAL_IF_ERR_ = (_err); \
		RETURN_VAL_IF_FAILED(err_RETURN_VAL_IF_ERR_ >= 0, \
				err_RETURN_VAL_IF_ERR_, (_val)); \
	} while (0)
