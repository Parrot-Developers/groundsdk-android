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

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.crashml.MockArsdkCrashmlDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.firmware.MockArsdkFirmwareUploadRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.MockArsdkFlightLogDownloadRequest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;

/**
 * Mock arsdk control expectations. Used to test expected call to ArsdkCore
 */
public class Expectation implements SelfDescribing {

    private final int mHandle;

    Expectation(int deviceHandle) {
        mHandle = deviceHandle;
    }

    @Override
    public void describeTo(@NonNull Description description) {
        description.appendText("Handle ").appendValue(mHandle)
                   .appendText(" and ").appendText(getClass().getSimpleName());
    }

    public boolean isCompleted() {
        return true;
    }

    public static class Connect extends Expectation {

        public Connect(int handle) {
            super(handle);
        }
    }

    public static class Disconnect extends Expectation {

        public Disconnect(int handle) {
            super(handle);
        }
    }

    public static class Command extends Expectation {

        private final Collection<ExpectedCmd> mCommands;

        final boolean mCheckParams;

        public Command(int handle, ExpectedCmd cmd, boolean checkParams) {
            this(handle, new ExpectedCmd[] {cmd}, checkParams);
        }

        public Command(int handle, ExpectedCmd... commands) {
            this(handle, commands, true);
        }

        public Command(int handle, ExpectedCmd commands[], boolean checkParams) {
            super(handle);
            mCommands = new ArrayList<>(Arrays.asList(commands));
            mCheckParams = checkParams;
        }

        @Override
        public void describeTo(@NonNull Description description) {
            super.describeTo(description);
            for (ExpectedCmd cmd : mCommands) {
                cmd.describeExpected(description.appendText(" {"));
                description.appendText("}");
            }
        }

        @Override
        public boolean isCompleted() {
            return mCommands.isEmpty();
        }

    }

    public static final class CrashmlReport extends Expectation {

        @NonNull
        private final MockArsdkCrashmlDownloadRequest mRequest;

        @NonNull
        private final String mCrashmlsPath;

        public static CrashmlReport of(int handle, @NonNull MockArsdkCrashmlDownloadRequest request,
                                       @NonNull String crashmlsPath) {
            return new CrashmlReport(handle, request, crashmlsPath);
        }

        private CrashmlReport(int handle, @NonNull MockArsdkCrashmlDownloadRequest request,
                              @NonNull String crashmlsPath) {
            super(handle);
            mRequest = request;
            mCrashmlsPath = crashmlsPath;
        }

        @NonNull
        public MockArsdkCrashmlDownloadRequest getRequest() {
            return mRequest;
        }
    }

    public static final class FlightLog extends Expectation {

        @NonNull
        private final MockArsdkFlightLogDownloadRequest mRequest;

        @NonNull
        private final String mFlightLogsPath;

        public static FlightLog of(int handle, @NonNull MockArsdkFlightLogDownloadRequest request,
                                   @NonNull String flightLogsPath) {
            return new FlightLog(handle, request, flightLogsPath);
        }

        private FlightLog(int handle, @NonNull MockArsdkFlightLogDownloadRequest request,
                          @NonNull String flightLogsPath) {
            super(handle);
            mRequest = request;
            mFlightLogsPath = flightLogsPath;
        }

        @NonNull
        public MockArsdkFlightLogDownloadRequest getRequest() {
            return mRequest;
        }
    }

    public static final class FirmwareUpload extends Expectation {

        @NonNull
        private final MockArsdkFirmwareUploadRequest mRequest;

        @NonNull
        private final String mFirmwarePath;

        public static FirmwareUpload of(int handle, @NonNull MockArsdkFirmwareUploadRequest request,
                                        @NonNull String srcPath) {
            return new FirmwareUpload(handle, request, srcPath);
        }

        private FirmwareUpload(int handle, @NonNull MockArsdkFirmwareUploadRequest request, @NonNull String srcPath) {
            super(handle);
            mRequest = request;
            mFirmwarePath = srcPath;
        }

        @NonNull
        public MockArsdkFirmwareUploadRequest getRequest() {
            return mRequest;
        }
    }

    private abstract static class ExpectationFeatureMatcher<E extends Expectation, T>
            extends TypeSafeDiagnosingMatcher<E> {

        @NonNull
        private final String mName;

        @NonNull
        private final Matcher<? super T> mSubMatcher;

        ExpectationFeatureMatcher(@NonNull Matcher<? super T> subMatcher, @NonNull String name) {
            mSubMatcher = subMatcher;
            mName = name;
        }

        @Override
        protected final boolean matchesSafely(E actual, Description mismatchDescription) {
            T featureValue = featureValueOf(actual);
            if (!mSubMatcher.matches(featureValue)) {
                mSubMatcher.describeTo(mismatchDescription.appendText("was "));
                return false;
            }
            return true;
        }

        abstract T featureValueOf(E actual);

        @Override
        public final void describeTo(Description description) {
            description.appendText(mName);
        }
    }

    public static Matcher<Expectation> hasHandle(int handle) {
        return new ExpectationFeatureMatcher<Expectation, Integer>(equalTo(handle), "Handle") {

            @Override
            protected Integer featureValueOf(Expectation actual) {
                return actual.mHandle;
            }
        };
    }

    public static Matcher<CrashmlReport> hasCrashmlsPath(@NonNull String crashmlsPath) {
        return new ExpectationFeatureMatcher<CrashmlReport, String>(equalTo(crashmlsPath), "CrashmlsPath") {

            @Override
            protected String featureValueOf(CrashmlReport actual) {
                return actual.mCrashmlsPath;
            }
        };
    }

    public static Matcher<FlightLog> hasFlightLogsPath(@NonNull String flightLogsPath) {
        return new ExpectationFeatureMatcher<FlightLog, String>(equalTo(flightLogsPath), "FlightLogsPath") {

            @Override
            protected String featureValueOf(FlightLog actual) {
                return actual.mFlightLogsPath;
            }
        };
    }

    public static Matcher<FirmwareUpload> hasFirmwarePath(@NonNull String srcPath) {
        return new ExpectationFeatureMatcher<FirmwareUpload, String>(equalTo(srcPath), "FirmwarePath") {

            @Override
            protected String featureValueOf(FirmwareUpload actual) {
                return actual.mFirmwarePath;
            }
        };
    }

    public static Matcher<Command> isCommand(ArsdkCommand cmd) {

        return new TypeSafeDiagnosingMatcher<Command>(Command.class) {

            @Override
            protected boolean matchesSafely(Command item, Description mismatchDescription) {
                ExpectedCmd matchingExpectedCmd = null;
                for (ExpectedCmd expectedCmd : item.mCommands) {
                    if (expectedCmd.match(cmd, item.mCheckParams)) {
                        matchingExpectedCmd = expectedCmd;
                        break;
                    }
                }

                if (matchingExpectedCmd == null) {
                    for (ExpectedCmd expectedCmd : item.mCommands) {
                        expectedCmd.describeMismatch(cmd, mismatchDescription);
                    }
                    return false;
                }
                item.mCommands.remove(matchingExpectedCmd);
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Command");
            }
        };
    }

}
