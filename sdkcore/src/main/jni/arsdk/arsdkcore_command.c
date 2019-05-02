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

#include "arsdkcore_command.h"

#define ARSDK_LOG_TAG command
#include "arsdk_log.h"

/** Global command log level. */
static enum arsdkcore_command_log_level
		g_command_log_level = ARSDKCORE_COMMAND_LOG_LEVEL_NONE;

/** Documented in public header. */
void arsdkcore_command_set_log_level(enum arsdkcore_command_log_level level)
{
	g_command_log_level = level;
}

/** Documented in public header. */
void arsdkcore_command_log(const struct arsdk_cmd *cmd, enum arsdk_cmd_dir dir)
{
	switch (g_command_log_level) {
		case ARSDKCORE_COMMAND_LOG_LEVEL_NONE:
			return;
		case ARSDKCORE_COMMAND_LOG_LEVEL_ACK_NO_FREQUENT:
			switch (cmd->id) {
			case ARSDK_ID_ARDRONE3_GPSSTATE_NUMBEROFSATELLITECHANGED:
			case ARSDK_ID_COMMON_COMMONSTATE_MASSSTORAGESTATELISTCHANGED:
			case ARSDK_ID_COMMON_COMMONSTATE_MASSSTORAGEINFOSTATELISTCHANGED:
			case ARSDK_ID_COMMON_COMMONSTATE_DEPRECATEDMASSSTORAGECONTENTCHANGED:
			case ARSDK_ID_COMMON_COMMONSTATE_MASSSTORAGECONTENT:
				return;
			}
			// fallthrough
		case ARSDKCORE_COMMAND_LOG_LEVEL_ACK:
			if (cmd->buffer_type == ARSDK_CMD_BUFFER_TYPE_NON_ACK)
				return;
			if (cmd->buffer_type == ARSDK_CMD_BUFFER_TYPE_INVALID) {
				const struct arsdk_cmd_desc* cmd_desc =
						arsdk_cmd_find_desc(cmd);
				if (cmd_desc && cmd_desc->buffer_type
						== ARSDK_CMD_BUFFER_TYPE_NON_ACK)
					return;
			}
			// fallthrough
		case ARSDKCORE_COMMAND_LOG_LEVEL_ALL: {
			// Command to string
			char cmd_str[512];
			if (arsdk_cmd_fmt(cmd, cmd_str, sizeof(cmd_str)) == 0) {
				LOGD("%s %s", dir == ARSDK_CMD_DIR_TX ? ">>" : "<<", cmd_str);
			}
			break;
		}
	}
}
