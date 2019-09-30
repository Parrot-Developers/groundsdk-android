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

package com.parrot.drone.sdkcore.arsdk;

import android.os.Looper;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.arsdk.device.MockArsdkDevice;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * A mock Arsdk core.
 */
public final class MockArsdkCore extends ArsdkCore {

    private final SparseArray<MockArsdkDevice> mDevices;

    private final LinkedList<Expectation> mExpectQueue;

    public static MockArsdkCore create(@NonNull Listener listener) {
        // Prepare a looper in the current thread if required. This allows arsdk to create its main handler.
        // MockArsdkCore is not using this handler.
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        return new MockArsdkCore(listener);
    }

    /**
     * Constructor.
     *
     * @param listener listener notifying device added/removed
     */
    private MockArsdkCore(@NonNull Listener listener) {
        super(new ArsdkBackendController[] {}, listener, "test", "mockarsdkcore", true);
        mExpectQueue = new LinkedList<>();
        mDevices = new SparseArray<>();
    }

    @Override
    public void start() {
        // cleanup to start new test
        mExpectQueue.clear();
        mDevices.clear();
    }

    @Override
    public void stop() {
        assertNoExpectation();
    }

    public MockArsdkCore expect(Expectation expectation) {
        mExpectQueue.add(expectation);
        return this;
    }

    public <E extends Expectation> E assertExpectation(@NonNull Class<E> expectationType,
                                                       @Nullable Matcher<? super E> expectationMatcher) {
        assertThat(mExpectQueue.peek(), instanceOf(expectationType));
        // asserted above
        @SuppressWarnings("unchecked")
        E expectation = (E) mExpectQueue.peek();
        assert expectation != null;
        if (expectationMatcher != null) {
            if (!expectationMatcher.matches(expectation)) {
                Description description = new StringDescription();
                expectationMatcher.describeMismatch(expectation,
                        description.appendText("\nExpected: ")
                                   .appendDescriptionOf(expectation)
                                   .appendText("\n     but: "));
                throw new AssertionError(description.toString());
            }
        }
        if (expectation.isCompleted()) {
            mExpectQueue.remove();
        }
        return expectation;
    }

    public void assertNoExpectation() {
        if (!mExpectQueue.isEmpty()) {
            throw new AssertionError(new StringDescription()
                    .appendText("Unattended expectations:")
                    .appendList("\n", "\n", "", mExpectQueue)
                    .toString());
        }
    }

    public MockArsdkDevice addDevice(@NonNull String uid, @ArsdkDevice.Type int type, @NonNull String name,
                                     int handle, @Backend.Type int backendType) {
        MockArsdkDevice device = new MockArsdkDevice(this, (short) handle, uid, type, name, backendType);
        mDevices.put(handle, device);
        mListener.onDeviceAdded(device);
        return device;
    }

    public MockArsdkCore commandReceived(int handle, @NonNull ArsdkCommand... commands) {
        for (ArsdkCommand command : commands) {
            mDevices.get(handle).commandReceived(command);
        }
        return this;
    }

    public void pollNoAckCommands(int handle, @NonNull Class<? extends ArsdkNoAckCmdEncoder> encoderType) {
        mDevices.get(handle).pollNoAckCommands(encoderType);
    }

    public void removeDevice(int handle) {
        MockArsdkDevice device = mDevices.get(handle);
        mListener.onDeviceRemoved(device);
    }

    public void addDevice(@NonNull MockArsdkDevice device) {
        mDevices.put(device.getHandle(), device);
    }

    public void deviceConnecting(int handle) {
        mDevices.get(handle).deviceConnecting();
    }

    public void deviceConnected(int handle) {
        mDevices.get(handle).deviceConnected();
    }

    public void deviceDisconnected(int handle, boolean removing) {
        mDevices.get(handle).deviceDisconnected(removing);
    }

    public void deviceConnectionCanceled(int handle, @ArsdkDevice.ConnectionCancelReason int reason, boolean removing) {
        mDevices.get(handle).deviceConnectionCanceled(reason, removing);
    }
}
