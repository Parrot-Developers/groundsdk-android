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

import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;

/**
 * Helper utility to process list flags in a functional way.
 */
public final class ArsdkListFlags {

    /** A runnable that does nothing. */
    private static final Runnable NOP = () -> {};

    /**
     * Processes a bitfield of {@link ArsdkFeatureGeneric.ListFlags}.
     * <p>
     * This method ignores processing of elements to be removed.
     *
     * @param listFlagsBitField list flags bitfield to process
     * @param clear             called when the related data set must be cleared
     * @param add               called when an element must be added to the related data set
     * @param complete          called when modification on the related data set are finished
     */
    public static void process(int listFlagsBitField, @NonNull Runnable clear, @NonNull Runnable add,
                               @NonNull Runnable complete) {
        process(listFlagsBitField, clear, add, NOP, complete);
    }

    /**
     * Processes a bitfield of {@link ArsdkFeatureGeneric.ListFlags}.
     *
     * @param listFlagsBitField list flags bitfield to process
     * @param clear             called when the related data set must be cleared
     * @param add               called when an element must be added to the related data set
     * @param remove            called when an element must be removed from the related data set
     * @param complete          called when modification on the related data set are finished
     */
    public static void process(int listFlagsBitField, @NonNull Runnable clear, @NonNull Runnable add,
                               @NonNull Runnable remove, @NonNull Runnable complete) {
        if (ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField)) {
            clear.run();
            complete.run();
        } else {
            if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                clear.run();
            }
            if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                remove.run();
            } else {
                add.run();
            }
            if (ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                complete.run();
            }
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private ArsdkListFlags() {

    }
}
