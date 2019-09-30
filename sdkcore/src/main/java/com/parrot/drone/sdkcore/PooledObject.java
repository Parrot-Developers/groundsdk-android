/*
 * Copyright (C) 2019 Parrot Drones SAS
 */

package com.parrot.drone.sdkcore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.sdkcore.BuildConfig;
import com.parrot.drone.sdkcore.ulog.ULog;
import com.parrot.drone.sdkcore.ulog.ULogTag;

/**
 * Base class for objects that are allocated from a pool.
 */
public abstract class PooledObject {

    /** Log tag. */
    private static final ULogTag TAG = new ULogTag("sdkcore.pool");

    /**
     * Base class of the pool.
     *
     * @param <T> pooled objects type
     */
    public abstract static class Pool<T extends PooledObject> {

        /** Default pool max size. */
        public static final int DEFAULT_POOL_MAX_SIZE = 50;

        /** Pool name, used only for debug logs. */
        private final String mName;

        /** Poll maximum size. */
        private final int mMaxSize;

        /** Poll current size. */
        private int mSize;

        /** Number of allocated objects. */
        private int mAllocatedCnt;

        /** Pool head. */
        private PooledObject mHead;

        /**
         * Constructor.
         *
         * @param name        pool name, only used for debug
         * @param initialSize pool initial size
         * @param maxSize     pool maximum size. When the pool has its maximum size, released objects are not returned
         *                    to the pool
         */
        protected Pool(@NonNull String name, int initialSize, int maxSize) {
            mName = name;
            mMaxSize = maxSize;
            for (int i = 0; i < initialSize; i++) {
                returnNewEntry();
                mAllocatedCnt++;
            }
        }

        /**
         * Obtains an entry from the pool or allocates a new one.
         *
         * @return entry
         */
        @NonNull
        public final synchronized T obtainEntry() {
            PooledObject entry;
            if (mHead != null) {
                entry = mHead;
                mHead = entry.mNext;
                entry.mNext = null;
                mSize--;
            } else {
                entry = createEntry();
                mAllocatedCnt++;
                if (mAllocatedCnt > mMaxSize) {
                    ULog.w(TAG, "Pool '" + mName + "' allocating more than maximum (" + mMaxSize + ") items");
                }
            }
            entry.mPool = this;
            @SuppressWarnings("unchecked")
            T result = (T) entry;
            return result;
        }

        /**
         * Returns an entry to the pool.
         *
         * @param entry entry to return
         */
        private synchronized void returnEntry(@NonNull PooledObject entry) {
            if (mSize < mMaxSize) {
                entry.mNext = mHead;
                mHead = entry;
                mSize++;
            }
            entry.mPool = null;
        }

        /**
         * Creates an entry and directly return it to the pool if space is available.
         */
        private synchronized void returnNewEntry() {
            if (mSize < mMaxSize) {
                T entry = createEntry();
                entry.mNext = mHead;
                mHead = entry;
                mSize++;
            }
        }

        /**
         * Creates an new entry.
         *
         * @return new entry.
         */
        @NonNull
        protected abstract T createEntry();
    }

    /** Pool owning this entry. Null if entry is in the pool */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    Pool<?> mPool;

    /** Next entry when the entry is in the pool. */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    PooledObject mNext;

    /**
     * Constructor.
     *
     * @param pool pool owning the entry
     */
    protected PooledObject(@NonNull Pool pool) {
        mPool = pool;
    }

    /**
     * Releases the entry.
     */
    public final void release() {
        if (mPool != null) {
            doRelease();
            mPool.returnEntry(this);
        } else if (BuildConfig.DEBUG) {
            throw new IllegalStateException("Pooled object already released: " + this);
        } else {
            ULog.e(TAG, "Pooled object already released: " + this);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (mPool != null) {
            if (ULog.w(TAG)) {
                ULog.w(TAG, "Pooled object never returned: " + this);
            }
            // Object was never returned to the pool. This may happen for pooled runnables that are out of the pool
            // when the handler thread that should process them exits before they get a chance to run.
            // Release it anyway.
            doRelease();
            // Then mock as if it returned to the pool gracefully.
            mPool.returnNewEntry();
            mPool = null;
        }
        super.finalize();
    }

    /**
     * Called when the entry is released.
     * <p>
     * Implementation may cleanup the item here before returning it to the pool. <br/>
     * Default implementation does nothing.
     */
    protected void doRelease() {

    }
}
