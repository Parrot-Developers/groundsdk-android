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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import android.support.annotation.NonNull;

import com.parrot.drone.groundsdk.FormattingStateMatcher;
import com.parrot.drone.groundsdk.MediaInfoMatcher;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class RemovableUserStorageTest {

    private MockComponentStore<Peripheral> mStore;

    private RemovableUserStorageCore mRemovableUserStorageImpl;

    private RemovableUserStorage mRemovableUserStorage;

    private Backend mBackend;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mRemovableUserStorageImpl = new RemovableUserStorageCore(mStore, mBackend);
        mRemovableUserStorage = mStore.get(RemovableUserStorage.class);
        mStore.registerObserver(RemovableUserStorage.class, () -> {
            mChangeCnt++;
            mRemovableUserStorage = mStore.get(RemovableUserStorage.class);
        });
        mChangeCnt = 0;
        mBackend.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mRemovableUserStorage, nullValue());
        assertThat(mChangeCnt, is(0));

        mRemovableUserStorageImpl.publish();
        assertThat(mRemovableUserStorage, notNullValue());
        assertThat(mChangeCnt, is(1));

        mRemovableUserStorageImpl.unpublish();
        assertThat(mRemovableUserStorage, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testState() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.NO_MEDIA).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NO_MEDIA));
        assertThat(mChangeCnt, is(1));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.MEDIA_TOO_SMALL).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MEDIA_TOO_SMALL));
        assertThat(mChangeCnt, is(2));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.USB_MASS_STORAGE).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.USB_MASS_STORAGE));
        assertThat(mChangeCnt, is(3));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.MEDIA_TOO_SLOW).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MEDIA_TOO_SLOW));
        assertThat(mChangeCnt, is(4));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.MOUNTING).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.MOUNTING));
        assertThat(mChangeCnt, is(5));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.NEED_FORMAT).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.NEED_FORMAT));
        assertThat(mChangeCnt, is(6));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.READY).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.READY));
        assertThat(mChangeCnt, is(7));

        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.ERROR).notifyUpdated();
        assertThat(mRemovableUserStorage.getState(), is(RemovableUserStorage.State.ERROR));
        assertThat(mChangeCnt, is(8));

        // Check same state does not trigger a change
        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.ERROR).notifyUpdated();
        assertThat(mChangeCnt, is(8));
    }

    @Test
    public void testAvailableSpace() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        assertThat(mRemovableUserStorage.getAvailableSpace(), is(-1L));

        mRemovableUserStorageImpl.updateAvailableSpace(1000L).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getAvailableSpace(), is(1000L));

        // Check same value does not trigger a change
        mRemovableUserStorageImpl.updateAvailableSpace(1000L).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testMediaInfo() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        assertThat(mRemovableUserStorage.getMediaInfo(), nullValue());

        mRemovableUserStorageImpl.updateMediaInfo("MediaName", 1000L).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("MediaName", 1000L));

        mRemovableUserStorageImpl.updateMediaInfo("NewMediaName", 1000L).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("NewMediaName", 1000L));

        mRemovableUserStorageImpl.updateMediaInfo("NewMediaName", 2000L).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mRemovableUserStorage.getMediaInfo(), MediaInfoMatcher.is("NewMediaName", 2000L));

        // Check same value does not trigger a change
        mRemovableUserStorageImpl.updateMediaInfo("NewMediaName", 2000L).notifyUpdated();
        assertThat(mChangeCnt, is(4));
    }

    @Test
    public void testCanFormat() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        assertThat(mRemovableUserStorage.canFormat(), is(false));

        mRemovableUserStorageImpl.updateCanFormat(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.canFormat(), is(true));

        // Check same value does not trigger a change
        mRemovableUserStorageImpl.updateCanFormat(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testSupportedFormattingTypes() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        // test initial value
        assertThat(mRemovableUserStorage.supportedFormattingTypes(), containsInAnyOrder(
                RemovableUserStorage.FormattingType.FULL));

        // test update from backend
        mRemovableUserStorageImpl.updateSupportedFormattingTypes(EnumSet.of(
                RemovableUserStorage.FormattingType.FULL,
                RemovableUserStorage.FormattingType.QUICK))
                                 .notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.supportedFormattingTypes(), containsInAnyOrder(
                RemovableUserStorage.FormattingType.FULL,
                RemovableUserStorage.FormattingType.QUICK));

        // test new update (remove type) from backend
        mRemovableUserStorageImpl.updateSupportedFormattingTypes(EnumSet.of(
                RemovableUserStorage.FormattingType.FULL))
                                 .notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.supportedFormattingTypes(), containsInAnyOrder(
                RemovableUserStorage.FormattingType.FULL));

        // check that updating with same value does not trigger a notification
        mRemovableUserStorageImpl.updateSupportedFormattingTypes(EnumSet.of(
                RemovableUserStorage.FormattingType.FULL))
                                 .notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.supportedFormattingTypes(), containsInAnyOrder(
                RemovableUserStorage.FormattingType.FULL));
    }

    @Test
    public void testFormat() {
        mRemovableUserStorageImpl.publish();
        assertThat(mChangeCnt, is(1));

        // formatting should not start as canFormat is not set
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(false));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL, "MediaName"), is(false));
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mFormatCnt, is(0));

        // formatting should start with the supported type
        mRemovableUserStorageImpl.updateCanFormat(true).notifyUpdated();
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(true));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL, "MediaName"), is(true));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.QUICK), is(false));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.QUICK, "MediaName"), is(false));
        assertThat(mChangeCnt, is(2));
        assertThat(mBackend.mFormatCnt, is(2));

        // formatting should not start as canFormat is unset
        mRemovableUserStorageImpl.updateCanFormat(false).notifyUpdated();
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL), is(false));
        assertThat(mRemovableUserStorage.format(RemovableUserStorage.FormattingType.FULL, "MediaName"), is(false));
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mFormatCnt, is(2));
    }

    @Test
    public void testFormattingState() {
        mRemovableUserStorageImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());

        mRemovableUserStorageImpl.initFormattingState().notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mRemovableUserStorage.formattingState(), FormattingStateMatcher.is(
                RemovableUserStorage.FormattingState.Step.PARTITIONING, 0));

        mRemovableUserStorageImpl.updateFormattingStep(RemovableUserStorage.FormattingState.Step.CLEARING_DATA)
                                 .notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mRemovableUserStorage.formattingState(), FormattingStateMatcher.is(
                RemovableUserStorage.FormattingState.Step.CLEARING_DATA, 0));

        mRemovableUserStorageImpl.updateFormattingProgress(10).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mRemovableUserStorage.formattingState(), FormattingStateMatcher.is(
                RemovableUserStorage.FormattingState.Step.CLEARING_DATA, 10));

        // Check same value does not trigger a change
        mRemovableUserStorageImpl.updateFormattingStep(RemovableUserStorage.FormattingState.Step.CLEARING_DATA)
                                 .updateFormattingProgress(10)
                                 .notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mRemovableUserStorage.formattingState(), FormattingStateMatcher.is(
                RemovableUserStorage.FormattingState.Step.CLEARING_DATA, 10));

        // Updating main state to a non formatting state should reset formatting state
        mRemovableUserStorageImpl.updateState(RemovableUserStorage.State.FORMATTING_SUCCEEDED).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mRemovableUserStorage.formattingState(), nullValue());
    }

    private static final class Backend implements RemovableUserStorageCore.Backend {

        int mFormatCnt;

        void reset() {
            mFormatCnt = 0;
        }

        @Override
        public boolean format(@NonNull RemovableUserStorage.FormattingType type, @NonNull String name) {
            mFormatCnt++;
            return true;
        }
    }
}
