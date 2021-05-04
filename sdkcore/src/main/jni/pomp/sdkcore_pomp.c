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

#include <sdkcore/internal/sdkcore_pomp.h>

#define SDKCORE_LOG_TAG pomp
#include <sdkcore/sdkcore_log.h>

#include <android/looper.h>

/** SdkCorePomp native backend. */
struct sdkcore_pomp {
	/** Android looper hosting the loop. */
	ALooper *looper;
	/** Internal pomp loop. */
	struct pomp_loop *loop;
	/** Context flag; may be NULL. */
	char *context_flag;
};

/** Context flag, indicates whether call runs in pomp or main loop. */
enum context_flag {
	/** Currently running in main loop. */
	CONTEXT_FLAG_IN_MAIN = 0,
	/** Currently running in pomp loop. */
	CONTEXT_FLAG_IN_POMP = 1
};

/**
 * Called back when pomp loop events must be processed.
 * @param[in] fd: pomp loop internal fd
 * @param[in] events: triggered events bitmask
 * @param[in] userdata: sdkcore pomp instance
 * @return 1 in case of success, 0 otherwise
 */
static int on_pomp_event(int fd, int events, void *userdata)
{
	struct sdkcore_pomp *self = userdata;
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, 0);

	char flag = 0;

	if (self->context_flag) {
		flag = *self->context_flag;
		*self->context_flag = CONTEXT_FLAG_IN_POMP;
	}

	int res = pomp_loop_process_fd(self->loop);

	if (self->context_flag) {
		*self->context_flag = flag;
	}

	LOG_IF_ERR(res);

	return 1;
}

/** Documented in public header. */
struct sdkcore_pomp *sdkcore_pomp_create(char *context_flag)
{
	ALooper *looper = ALooper_forThread();
	RETURN_VAL_IF_FAILED(looper != NULL, -EPROTO, NULL);

	struct pomp_loop *loop = pomp_loop_new();
	RETURN_VAL_IF_FAILED(loop != NULL, -ENOMEM, NULL);

	intptr_t fd = pomp_loop_get_fd(loop);
	GOTO_IF_ERR((int) fd, err_destroy_loop);

	struct sdkcore_pomp *self = calloc(1, sizeof(*self));
	GOTO_IF_FAILED(self != NULL, -ENOMEM, err_destroy);

	self->context_flag = context_flag;
	self->looper = looper;
	self->loop = loop;

	GOTO_IF_FAILED(ALooper_addFd(self->looper, fd, ALOOPER_POLL_CALLBACK,
			ALOOPER_EVENT_INPUT | ALOOPER_EVENT_OUTPUT, on_pomp_event,
			self) == 1, -ENOTSUP, err_destroy);

	return self;

err_destroy:
	free(self);

err_destroy_loop:
	LOG_IF_ERR(pomp_loop_destroy(loop));

	return NULL;
}

/** Documented in public header. */
struct pomp_loop *sdkcore_pomp_get_loop(struct sdkcore_pomp *self)
{
	RETURN_VAL_IF_FAILED(self != NULL, -EINVAL, NULL);

	return self->loop;
}

/** Documented in public header. */
int sdkcore_pomp_destroy(struct sdkcore_pomp *self)
{
	RETURN_ERRNO_IF_FAILED(self != NULL, -EINVAL);

	ALooper *looper = ALooper_forThread();
	RETURN_ERRNO_IF_FAILED(looper == self->looper, -EPROTO);

	RETURN_ERRNO_IF_ERR(pomp_loop_idle_flush(self->loop));

	intptr_t fd = pomp_loop_get_fd(self->loop);
	RETURN_ERRNO_IF_ERR((int) fd);

	RETURN_ERRNO_IF_FAILED(ALooper_removeFd(self->looper, fd) == 1, -EPROTO);

	RETURN_ERRNO_IF_ERR(pomp_loop_destroy(self->loop));

	self->loop = NULL;
	self->looper = NULL;
	self->context_flag = NULL;

	free(self);

	return 0;
}
