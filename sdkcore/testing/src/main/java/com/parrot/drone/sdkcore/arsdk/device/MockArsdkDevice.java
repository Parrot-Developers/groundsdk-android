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

import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.MockArsdkCore;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;
import com.parrot.drone.sdkcore.arsdk.crashml.ArsdkCrashmlDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareUploadRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.ArsdkFlightLogDownloadRequest;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.sdkcore.arsdk.Expectation.hasCrashmlsPath;
import static com.parrot.drone.sdkcore.arsdk.Expectation.hasFirmwarePath;
import static com.parrot.drone.sdkcore.arsdk.Expectation.hasFlightLogsPath;
import static com.parrot.drone.sdkcore.arsdk.Expectation.hasHandle;
import static com.parrot.drone.sdkcore.arsdk.Expectation.isCommand;
import static org.hamcrest.Matchers.allOf;

public class MockArsdkDevice extends ArsdkDevice {

    private final MockArsdkCore mMockArsdkCore;

    private final Set<ArsdkNoAckCmdEncoder> mNoAckEncoders;

    public MockArsdkDevice(@NonNull MockArsdkCore arsdkCore, short nativeHandle, @NonNull String uid,
                           @ArsdkDevice.Type int type, @NonNull String name, @Backend.Type int backendType) {
        super(nativeHandle, uid, type, name, backendType, ArsdkDevice.API_FULL);
        mMockArsdkCore = arsdkCore;
        mNoAckEncoders = new HashSet<>();
    }

    @Override
    public short getHandle() {
        return super.getHandle();
    }

    @Override
    public void connect(@NonNull Listener listener) {
        mMockArsdkCore.assertExpectation(Expectation.Connect.class, hasHandle(getHandle()));
        mListener = listener;
        // add the device just in case it was not there
        mMockArsdkCore.addDevice(this);
    }

    @Override
    public void disconnect() {
        mMockArsdkCore.assertExpectation(Expectation.Disconnect.class, hasHandle(getHandle()));
    }

    @Override
    public void sendCommand(@NonNull ArsdkCommand command) {
        mMockArsdkCore.assertExpectation(Expectation.Command.class, allOf(hasHandle(getHandle()), isCommand(command)));
    }

    @Override
    public void setNoAckCommandLoopPeriod(int period) {
    }

    @Override
    public void registerNoAckCommandEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
        mNoAckEncoders.add(encoder);
    }

    @Override
    public void unregisterNoAckCommandEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
        mNoAckEncoders.remove(encoder);
    }

    public void pollNoAckCommands(@NonNull Class<? extends ArsdkNoAckCmdEncoder> encoderType) {
        for (ArsdkNoAckCmdEncoder encoder : mNoAckEncoders) {
            if (encoderType.isInstance(encoder)) {
                ArsdkCommand command = encoder.encodeNoAckCmd();
                if (command != null) {
                    sendCommand(command);
                }
            }
        }
    }

    @Override
    public ArsdkTcpProxy createTcpProxy(@Type int deviceType, int port,
                                        @NonNull ArsdkTcpProxy.Listener listener) {
        listener.onComplete("test", 80, null);
        return null;
    }

    @NonNull
    @Override
    public SdkCoreStream openVideoStream(@NonNull String url, @Nullable String track,
                                         @NonNull SdkCoreStream.Client client) {
        throw new UnsupportedOperationException("TODO");
    }

    @NonNull
    @Override
    public ArsdkRequest downloadCrashml(@Type int deviceType, @NonNull String path,
                                        @NonNull ArsdkCrashmlDownloadRequest.Listener listener) {
        Expectation.CrashmlReport expectation = mMockArsdkCore.assertExpectation(Expectation.CrashmlReport.class,
                hasCrashmlsPath(path));
        return expectation.getRequest().setListener(listener);
    }

    @NonNull
    @Override
    public ArsdkRequest downloadFlightLog(@Type int deviceType, @NonNull String path,
                                          @NonNull ArsdkFlightLogDownloadRequest.Listener listener) {
        Expectation.FlightLog expectation = mMockArsdkCore.assertExpectation(Expectation.FlightLog.class,
                hasFlightLogsPath(path));
        return expectation.getRequest().setListener(listener);
    }


    @NonNull
    @Override
    public ArsdkRequest uploadFirmware(@Type int deviceType, @NonNull String srcPath,
                                       @NonNull ArsdkFirmwareUploadRequest.Listener listener) {
        Expectation.FirmwareUpload expectation = mMockArsdkCore.assertExpectation(Expectation.FirmwareUpload.class,
                hasFirmwarePath(srcPath));
        return expectation.getRequest().setListener(listener);
    }


    public void deviceConnecting() {
        assert mListener != null;
        mListener.onConnecting();
    }

    public void deviceConnected() {
        assert mListener != null;
        mListener.onConnected();
    }

    public void deviceDisconnected(boolean removing) {
        assert mListener != null;
        mListener.onDisconnected(removing);
        mListener = null;
    }

    public void deviceConnectionCanceled(@ArsdkDevice.ConnectionCancelReason int reason, boolean removing) {
        assert mListener != null;
        mListener.onConnectionCanceled(reason, removing);
        mListener = null;
    }

    public void commandReceived(@NonNull ArsdkCommand command) {
        assert mListener != null;
        mListener.onCommandReceived(command);
    }
}
