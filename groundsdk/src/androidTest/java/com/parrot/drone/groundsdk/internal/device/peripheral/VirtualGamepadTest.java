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

import android.content.Intent;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.internal.ApplicationNotifier;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.gamepad.VirtualGamepadCore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class VirtualGamepadTest {

    private MockComponentStore<Peripheral> mStore;

    private VirtualGamepadCore mVirtualGamepadImpl;

    private VirtualGamepad mVirtualGamepad;

    private Backend mBackend;

    private NavigationListener mNavigationListener;

    private AppEventReceiver mAppEventReceiver;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mNavigationListener = new NavigationListener();
        mAppEventReceiver = new AppEventReceiver();
        mVirtualGamepadImpl = new VirtualGamepadCore(mStore, mBackend);
        mVirtualGamepad = mStore.get(VirtualGamepad.class);
        mStore.registerObserver(VirtualGamepad.class, () -> {
            mChangeCnt++;
            mVirtualGamepad = mStore.get(VirtualGamepad.class);
        });
        mChangeCnt = 0;
        mBackend.reset();
        mNavigationListener.reset();
        mAppEventReceiver.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mVirtualGamepad, nullValue());
        assertThat(mChangeCnt, is(0));

        mVirtualGamepadImpl.publish();
        assertThat(mVirtualGamepad, is(mVirtualGamepadImpl));
        assertThat(mChangeCnt, is(1));

        mVirtualGamepadImpl.unpublish();
        assertThat(mVirtualGamepad, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testGrabRelease() {
        mVirtualGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // at start, should be released and can be grabbed
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));
        assertThat(mVirtualGamepad.canGrab(), is(true));

        // release should do nothing
        mVirtualGamepad.release();
        assertThat(mBackend.mReleaseCnt, is(0));

        // grab
        mVirtualGamepad.grab(mNavigationListener);
        assertThat(mBackend.mGrabCnt, is(1));

        // mock grabbed from low-level
        mVirtualGamepadImpl.updateGrabbed(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.GRABBED));

        // grab should do nothing
        assertThat(mVirtualGamepad.canGrab(), is(false));
        mVirtualGamepad.grab(mNavigationListener);
        assertThat(mBackend.mGrabCnt, is(1));

        // set preempted
        mVirtualGamepadImpl.updatePreempted(true).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.PREEMPTED));

        // grab should do nothing
        assertThat(mVirtualGamepad.canGrab(), is(false));
        mVirtualGamepad.grab(mNavigationListener);
        assertThat(mBackend.mGrabCnt, is(1));

        // release
        mVirtualGamepad.release();
        assertThat(mBackend.mReleaseCnt, is(1));
        // mock released from low-level
        mVirtualGamepadImpl.updateGrabbed(false).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // we are still preempted, so grab should be unavailable
        assertThat(mVirtualGamepad.canGrab(), is(false));
        mVirtualGamepad.grab(mNavigationListener);
        assertThat(mBackend.mGrabCnt, is(1));

        // release should do nothing either
        mVirtualGamepad.release();
        assertThat(mBackend.mReleaseCnt, is(1));

        // mock end of preemption from low level
        mVirtualGamepadImpl.updatePreempted(false).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mVirtualGamepad.getState(), is(VirtualGamepad.State.RELEASED));

        // ensure can be grabbed
        assertThat(mVirtualGamepad.canGrab(), is(true));
    }

    @Test
    public void testNavigationEvents() {
        mVirtualGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        // grab navigation to receive events
        mVirtualGamepad.grab(mNavigationListener);
        // mock grab acknowledge from low level
        mVirtualGamepadImpl.updateGrabbed(true);

        // ensure we receive events
        mVirtualGamepadImpl.notifyNavigationEvent(VirtualGamepad.Event.CANCEL, VirtualGamepad.Event.State.PRESSED);
        assertThat(mNavigationListener.mEvent, is(VirtualGamepad.Event.CANCEL));
        assertThat(mNavigationListener.mState, is(VirtualGamepad.Event.State.PRESSED));

        // release
        mVirtualGamepad.release();
        // mock release acknowledge from low level
        mVirtualGamepadImpl.updateGrabbed(false);

        // ensure we don't receive events anymore
        mNavigationListener.reset();
        mVirtualGamepadImpl.notifyNavigationEvent(VirtualGamepad.Event.CANCEL, VirtualGamepad.Event.State.RELEASED);
        assertThat(mNavigationListener.mEvent, nullValue());
        assertThat(mNavigationListener.mState, nullValue());

        // grab again
        mVirtualGamepad.grab(mNavigationListener);

        // ensure we receive events again
        mVirtualGamepadImpl.notifyNavigationEvent(VirtualGamepad.Event.OK, VirtualGamepad.Event.State.RELEASED);
        assertThat(mNavigationListener.mEvent, is(VirtualGamepad.Event.OK));
        assertThat(mNavigationListener.mState, is(VirtualGamepad.Event.State.RELEASED));

        // unpublish
        mVirtualGamepadImpl.unpublish();
        // ensure we don't receive events either
        mNavigationListener.reset();
        mVirtualGamepadImpl.notifyNavigationEvent(VirtualGamepad.Event.OK, VirtualGamepad.Event.State.PRESSED);
        assertThat(mNavigationListener.mEvent, nullValue());
        assertThat(mNavigationListener.mState, nullValue());
    }

    @Test
    public void testAppEvents() {
        mVirtualGamepadImpl.publish();
        assertThat(mChangeCnt, is(1));

        ApplicationNotifier.setInstance(mAppEventReceiver);

        VirtualGamepadCore.notifyAppEvent(ButtonsMappableAction.APP_ACTION_SETTINGS);
        assertThat(mAppEventReceiver.mAction, is(ButtonsMappableAction.APP_ACTION_SETTINGS));

        AppEventReceiver.setInstance(null);
    }

    private static final class Backend implements VirtualGamepadCore.Backend {

        int mGrabCnt;

        int mReleaseCnt;

        void reset() {
            mGrabCnt = mReleaseCnt = 0;
        }

        @Override
        public boolean grabNavigation() {
            mGrabCnt++;
            return true;
        }

        @Override
        public void releaseNavigation() {
            mReleaseCnt++;
        }
    }

    private static final class NavigationListener implements VirtualGamepad.Event.Listener {

        VirtualGamepad.Event mEvent;

        VirtualGamepad.Event.State mState;

        void reset() {
            mEvent = null;
            mState = null;
        }

        @Override
        public void onEvent(@NonNull VirtualGamepad.Event event, @NonNull VirtualGamepad.Event.State state) {
            mEvent = event;
            mState = state;
        }
    }

    private static final class AppEventReceiver extends ApplicationNotifier {

        ButtonsMappableAction mAction;

        void reset() {
            mAction = null;
        }

        @Override
        public void broadcastIntent(@NonNull Intent intent) {
            assertThat(intent.getAction(), is(VirtualGamepad.ACTION_GAMEPAD_APP_EVENT));
            assertThat(intent.hasExtra(VirtualGamepad.EXTRA_GAMEPAD_APP_EVENT_ACTION), is(true));
            mAction = ButtonsMappableAction.values()[intent.getIntExtra(
                    VirtualGamepad.EXTRA_GAMEPAD_APP_EVENT_ACTION, -1)];
        }
    }
}
