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

package com.parrot.drone.groundsdk.internal.tasks;

import android.util.SparseArray;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Allows grouping multiple tasks for the purpose of cancelling them altogether.
 * <p>
 * Tasks are automatically removed from the group when they complete so that no external bookkeeping is required.
 */
public final class TaskGroup {

    /** Default subset id used for task added without a specific subset id. */
    private static final int DEFAULT_SUBSET_ID = 0;

    /** Tasks in this group, by subset id. */
    @NonNull
    private final SparseArray<Set<Task<?>>> mTasks;

    /**
     * Constructor.
     */
    public TaskGroup() {
        mTasks = new SparseArray<>();
    }

    /**
     * Adds a task to the group.
     *
     * @param task task to add
     *
     * @return this {@code TaskGroup}, to allow call chaining
     */
    @NonNull
    public TaskGroup add(@NonNull Task<?> task) {
        return addTask(task, DEFAULT_SUBSET_ID);
    }

    /**
     * Adds a task to the group, with an associated id.
     * <p>
     * {@code subsetId} may serve as a reference to identify and cancel a specific subset of tasks at once.
     *
     * @param task     task to add
     * @param subsetId subset id to associate this task with
     *
     * @return this {@code TaskGroup}, to allow call chaining
     */
    @NonNull
    public TaskGroup add(@NonNull Task<?> task, @IntRange(from = DEFAULT_SUBSET_ID + 1) int subsetId) {
        return addTask(task, subsetId);
    }

    /**
     * Cancels all non-completed tasks that were added with a specific subset id.
     *
     * @param subsetId id of the subset whose tasks must be canceled
     */
    public void cancel(@IntRange(from = DEFAULT_SUBSET_ID + 1) int subsetId) {
        Collection<Task<?>> subset = mTasks.get(subsetId);
        if (subset != null) {
            // work on a copy of the subset since tasks may be removed by cancel calling back directly
            for (Task<?> task : subset.toArray(new Task<?>[0])) {
                task.cancel();
            }
        }
    }

    /**
     * Cancels all non-completed tasks in the group.
     */
    public void cancelAll() {
        // first take a snapshot of all subsets since subsets may be removed by cancel calling back directly
        int[] subsetIds = new int[mTasks.size()];
        for (int i = 0; i < subsetIds.length; i++) {
            subsetIds[i] = mTasks.keyAt(i);
        }
        // then cancel all collected subsets at once
        for (int subsetId : subsetIds) {
            cancel(subsetId);
        }
    }

    /**
     * Lists non-completed tasks registered with a specific id.
     * <p>
     * The returned set can be freely modified.
     *
     * @param subsetId id of tasks to be included in the returned set
     *
     * @return a set of all non-completed tasks with the given id
     */
    @NonNull
    public Set<Task<?>> list(@IntRange(from = DEFAULT_SUBSET_ID + 1) int subsetId) {
        return new HashSet<>(mTasks.get(subsetId, Collections.emptySet()));
    }

    /**
     * Lists all non-completed tasks in the group
     * <p>
     * The returned set can be freely modified.
     *
     * @return a set of all non-completed tasks
     */
    @NonNull
    public Set<Task<?>> listAll() {
        Set<Task<?>> tasks = new HashSet<>();
        for (int i = 0, N = mTasks.size(); i < N; i++) {
            tasks.addAll(mTasks.valueAt(i));
        }
        return tasks;
    }

    /**
     * Tells whether all tasks added with a specific subset id have completed.
     *
     * @param subsetId id of the subset to check for completion
     *
     * @return {@code true} when all tasks with the given subset id have completed, otherwise {@code false}
     */
    public boolean isComplete(@IntRange(from = DEFAULT_SUBSET_ID + 1) int subsetId) {
        boolean complete = true;
        Collection<Task<?>> subset = mTasks.get(subsetId);
        if (subset != null) {
            for (Iterator<Task<?>> iter = subset.iterator(); complete && iter.hasNext(); ) {
                complete = iter.next().isComplete();
            }
        }
        return complete;
    }

    /**
     * Tells whether all tasks have completed.
     *
     * @return {@code true} when all tasks in the group have completed, otherwise {@code false}
     */
    public boolean allComplete() {
        boolean complete = true;
        for (int i = 0, N = mTasks.size(); complete && i < N; i++) {
            complete = isComplete(mTasks.keyAt(i));
        }
        return complete;
    }

    /**
     * Adds a task to the group.
     *
     * @param task     task to add
     * @param subsetId subset id to associate this task with
     *
     * @return this {@code TaskGroup}, to allow call chaining
     */
    private TaskGroup addTask(@NonNull Task<?> task, @IntRange(from = DEFAULT_SUBSET_ID) int subsetId) {
        Set<Task<?>> subset = addToSubset(task, subsetId);
        task.whenComplete((Task.CompletionListener<Object>) (result, error, canceled) -> {
            subset.remove(task);
            if (subset.isEmpty()) {
                mTasks.remove(subsetId);
            }
        });
        return this;
    }

    /**
     * Adds a task to a subset.
     * <p>
     * A new subset of given id is created if none exists.
     *
     * @param task     task to add
     * @param subsetId id of the subset this task belongs to
     *
     * @return the group where the task was added
     */
    @NonNull
    private Set<Task<?>> addToSubset(@NonNull Task<?> task, @IntRange(from = DEFAULT_SUBSET_ID) int subsetId) {
        Set<Task<?>> subset = mTasks.get(subsetId);
        if (subset == null) {
            subset = new HashSet<>();
            mTasks.put(subsetId, subset);
        }
        subset.add(task);
        return subset;
    }
}
