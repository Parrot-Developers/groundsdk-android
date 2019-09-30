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

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.PooledObject;

/**
 * Wrapper on top of Multiset structure.
 * <p>
 * This class hold a native copy of Multiset struct. Instances are allocated from a pool to avoid continuous java
 * allocations and reduce gc pressure.
 */
public final class ArsdkMultiset extends PooledObject {

    /**
     * Multiset pool.
     */
    public static final class Pool extends PooledObject.Pool<ArsdkMultiset> {

        /** A shared default pool. */
        @NonNull
        public static final Pool DEFAULT = new Pool("multiset", 2, DEFAULT_POOL_MAX_SIZE);

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
         * Gets a pre-allocated Multiset from the pool or allocate a new Multiset if the pool is empty.
         * <p>
         * Returned Multiset must be relapsed by calling {@link ArsdkMultiset#release()}.
         *
         * @return a empty Multiset.
         */
        @NonNull
        public ArsdkMultiset obtain() {
            return obtain(0);
        }

        /**
         * Gets a pre-allocated Multiset from the pool or allocate a new Multiset if the pool is empty.
         * <p>
         * Returned Multiset must be relapsed by calling {@link ArsdkMultiset#release()}.
         *
         * @param otherMultisetPtr existing native Multiset to link to this multiset, or 0 for a new native Multiset
         *
         * @return a Multiset.
         */
        @NonNull
        public synchronized ArsdkMultiset obtain(long otherMultisetPtr) {
            ArsdkMultiset multiset = obtainEntry();
            multiset.mNativePtr = nativeInit(otherMultisetPtr);
            if (multiset.mNativePtr == 0) {
                throw new RuntimeException("native create fail");
            }
            return multiset;
        }

        @NonNull
        @Override
        protected ArsdkMultiset createEntry() {
            return new ArsdkMultiset(this);
        }
    }

    /** Native Multiset pointer, 0 if empty. */
    private long mNativePtr;

    /**
     * Constructor.
     *
     * @param pool pool owning this Multiset
     */
    private ArsdkMultiset(@NonNull Pool pool) {
        super(pool);
    }

    /**
     * Copy the Multiset to an existing native Multiset.
     *
     * @param destMultisetPtr pointer to the native Multiset to copy to
     */
    public void copyTo(long destMultisetPtr) {
        nativeCopy(mNativePtr, destMultisetPtr);
    }

    /**
     * Tells whether this Multiset contains the same data as another Multiset.
     *
     * @param other the Multiset compare this instance with.
     *
     * @return {@code true} if both Multisets are equal, otherwise {@code false}
     */
    public boolean contentEquals(@NonNull ArsdkMultiset other) {
        return nativeCmp(mNativePtr, other.mNativePtr) == 0;
    }

    /**
     * Gets the native Multiset pointer.
     *
     * @return native Multiset pointer, 0 if not set
     */
    public long getNativePtr() {
        return mNativePtr;
    }

    /**
     * Release the Multiset and put it back into the pool.
     */
    @Override
    protected void doRelease() {
        if (mNativePtr != 0) {
            nativeRelease(mNativePtr);
            mNativePtr = 0;
        }
    }

    /* JNI declarations and setup */
    private static native long nativeInit(long otherMultisetPtr);

    private static native void nativeCopy(long srcMultisetPtr, long destMultisetPtr);

    private static native int nativeCmp(long lhsMultisetPtr, long rhsMultisetPtr);

    private static native void nativeRelease(long nativePtr);
}
