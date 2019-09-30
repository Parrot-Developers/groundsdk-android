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

package com.parrot.drone.sdkcore.arsdk.command;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.sdkcore.PooledObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Wrapper on top of arsdk_cmd structure.
 * <p>
 * This class hold a native copy of struct arsdk_cmd. Instances are allocated from a pool to avoid continuous java
 * allocations and reduce gc pressure.
 */
public final class ArsdkCommand extends PooledObject {

    /** Command logging level. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOG_LEVEL_NONE, LOG_LEVEL_ACKNOWLEDGED_WITHOUT_FREQUENT, LOG_LEVEL_ACKNOWLEDGED, LOG_LEVEL_ALL})
    public @interface LogLevel {}

    /* Numerical values MUST be kept in sync with C enum arsdkcore_command_log_level */

    /** Don't log any commands. */
    public static final int LOG_LEVEL_NONE = 0;

    /** Only log non-frequent acknowledged commands (unlike mass storage info or number of satellites). */
    public static final int LOG_LEVEL_ACKNOWLEDGED_WITHOUT_FREQUENT = 1;

    /** Log acknowledged commands only. */
    public static final int LOG_LEVEL_ACKNOWLEDGED = 2;

    /** Log all commands. */
    public static final int LOG_LEVEL_ALL = 3;

    /**
     * Thrown to indicate that some ArsdkCommand event is rejected.
     * <p>
     * This is a convenience facility that may be used by ArsdkCommand decode callback implementations, that may throw
     * this exception when receiving invalid parameters.
     * <p>
     * This exception will then be logged at error level and the callback will be aborted.
     */
    public static final class RejectedEventException extends IllegalArgumentException {

        /**
         * Constructor.
         *
         * @param message message describing the reason for this exception to be thrown
         */
        public RejectedEventException(String message) {
            super(message);
        }
    }

    /**
     * Command pool.
     */
    public static final class Pool extends PooledObject.Pool<ArsdkCommand> {

        /** A shared default pool. */
        @NonNull
        public static final Pool DEFAULT = new Pool("cmd", 2, DEFAULT_POOL_MAX_SIZE);

        /**
         * Constructor.
         *
         * @param name        pool name, only used for debug
         * @param initialSize pool initial size
         * @param maxSize     pool maximum size. When the pool has its maximum size, released objects are not returned
         */
        Pool(@NonNull String name, int initialSize, int maxSize) {
            super(name, initialSize, maxSize);
        }

        /**
         * Gets a pre-allocated commands from the pool or allocate a new command if the pool is empty.
         * <p>
         * Returned command must be relapsed by calling {@link ArsdkCommand#release()}.
         *
         * @return a empty command.
         */
        @NonNull
        public ArsdkCommand obtain() {
            return obtain(0);
        }

        /**
         * Gets a pre-allocated commands from the pool or allocate a new command if the pool is empty.
         * <p>
         * Returned command must be relapsed by calling {@link ArsdkCommand#release()}.
         *
         * @param otherCmdPtr native pointer of a command to link to this command, or 0 for a new native command
         *
         * @return a command.
         */
        @NonNull
        public synchronized ArsdkCommand obtain(long otherCmdPtr) {
            ArsdkCommand command = obtainEntry();
            command.mNativePtr = nativeInit(otherCmdPtr);
            if (command.mNativePtr == 0) {
                throw new AssertionError("Failed to create ArsdkCommand native backend");
            }
            return command;
        }

        @NonNull
        @Override
        protected ArsdkCommand createEntry() {
            return new ArsdkCommand(this);
        }
    }

    /**
     * Gets the name of command name identified by id (for logging).
     *
     * @param featureId feature id
     * @param commandId command id
     *
     * @return command name
     */
    @NonNull
    public static String getName(short featureId, short commandId) {
        return nativeGetCmdName(featureId, commandId);
    }

    /** Native command pointer, 0 if empty. */
    private long mNativePtr;

    /** Command feature id, lazy initialized. */
    private int mFeatureId;

    /** Command Cmd/Evt id, lazy initialized. */
    private int mCommandId;

    /**
     * Constructor.
     *
     * @param pool pool owning this command
     */
    private ArsdkCommand(@NonNull Pool pool) {
        super(pool);
        mFeatureId = -1;
        mCommandId = -1;
    }

    /**
     * Copy the command to an existing native command.
     *
     * @param destCmdPtr pointer to the native command to copy to
     */
    public void copyTo(long destCmdPtr) {
        nativeCopy(mNativePtr, destCmdPtr);
    }

    /**
     * Gets the native command pointer.
     *
     * @return native command pointer, 0 if not set
     */
    public long getNativePtr() {
        return mNativePtr;
    }

    /**
     * Gets the command feature id.
     *
     * @return feature id, or -1 if not available
     */
    public int getFeatureId() {
        if (mFeatureId == -1 && mNativePtr != 0) {
            mFeatureId = nativeGetFeatureId(mNativePtr);
        }
        return mFeatureId;
    }

    /**
     * Gets the command or event id.
     *
     * @return command/event id, or -1 if not available
     */
    public int getCommandId() {
        if (mCommandId == -1 && mNativePtr != 0) {
            mCommandId = nativeGetCommandId(mNativePtr);
        }
        return mCommandId;
    }

    /**
     * Gets the command name (for logging).
     *
     * @return command name
     */
    @NonNull
    public String getName() {
        return nativeGetName(mNativePtr);
    }

    /**
     * Release the command and put it back into the pool.
     */
    @Override
    protected void doRelease() {
        if (mNativePtr != 0) {
            nativeRelease(mNativePtr);
            mNativePtr = 0;
            mFeatureId = -1;
            mCommandId = -1;
        }
    }

    /* JNI declarations and setup */
    private static native long nativeInit(long otherCmdPtr);

    private static native void nativeCopy(long srcCmdPtr, long destCmdPtr);

    private static native int nativeGetFeatureId(long nativePtr);

    private static native int nativeGetCommandId(long nativePtr);

    private static native String nativeGetName(long nativePtr);

    private static native void nativeRelease(long nativePtr);

    private static native String nativeGetCmdName(short featureId, short commandId);

    private static native ByteBuffer nativeGetData(long nativePtr);

    private static native void nativeSetData(long nativePtr, ByteBuffer buffer);

    /**
     * Gets native command data as ByteBuffer.
     * <p>
     * Used only for testing.
     *
     * @return command data
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @NonNull
    public ByteBuffer getData() {
        return nativeGetData(mNativePtr);
    }

    /**
     * Set native command data from ByteBuffer.
     * <p>
     * Used only for testing.
     *
     * @param buffer command data
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setData(@NonNull ByteBuffer buffer) {
        nativeSetData(mNativePtr, buffer);
    }
}
