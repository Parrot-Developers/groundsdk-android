/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.drone.sdkcore.arsdk.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.PooledObject;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/**
 * A pool of command runnables used to dispatch command processing to appropriate threads without having to allocate
 * a runnable for each received command or each command to send.
 */
abstract class CommandRunnablePool extends PooledObject.Pool<CommandRunnablePool.Entry> {

    /**
     * A pool entry, which is a runnable with an associated ArsdkCommand.
     */
    final class Entry extends PooledObject implements Runnable {

        /** ArsdkCommand passed to the runnable run method. Must be set before run() is called. */
        @Nullable
        private ArsdkCommand mCommand;

        /**
         * Initializes the pool entry by setting its associated command.
         *
         * @param command arsdk command to associate with the entry
         *
         * @return this, to allow call chaining
         */
        Entry init(@NonNull ArsdkCommand command) {
            mCommand = command;
            return this;
        }

        /**
         * Constructor.
         */
        Entry() {
            super(CommandRunnablePool.this);
        }

        @Override
        protected void doRelease() {
            assert mCommand != null;
            mCommand.release();
            mCommand = null;
        }

        @Override
        public void run() {
            if (mCommand == null) {
                throw new IllegalStateException("Command not set");
            }
            doWithCommand(mCommand);
            release();
        }
    }

    /**
     * Constructor.
     *
     * @param name pool name, only used for debug
     */
    CommandRunnablePool(@NonNull String name) {
        super(name, 1, DEFAULT_POOL_MAX_SIZE);
    }

    @NonNull
    @Override
    protected final Entry createEntry() {
        return new Entry();
    }

    /**
     * Processes the given command.
     * <p>
     * This method is called from a pool's entry run() method. The provided command is the command that is associated
     * with the processed pool entry.
     *
     * @param command arsdk command to process
     */
    abstract void doWithCommand(@NonNull ArsdkCommand command);
}
