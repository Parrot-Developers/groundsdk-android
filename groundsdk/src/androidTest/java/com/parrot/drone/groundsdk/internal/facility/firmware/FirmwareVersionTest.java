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

package com.parrot.drone.groundsdk.internal.facility.firmware;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.hasMajor;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.hasMinor;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.hasPatch;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.isAlpha;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.isBeta;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.isDev;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.isRC;
import static com.parrot.drone.groundsdk.FirmwareVersionMatcher.isRelease;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

public class FirmwareVersionTest {

    @SafeVarargs
    private static void testVersion(@NonNull String version, Matcher<? super FirmwareVersion>... matchers) {
        assertThat(FirmwareVersion.parse(version), Matchers.allOf(matchers));
    }

    @Test
    public void testVersions() {
        testVersion("1.2.3-alpha1", hasMajor(1), hasMinor(2), hasPatch(3), isAlpha(1));
        testVersion("3.1.2-beta4", hasMajor(3), hasMinor(1), hasPatch(2), isBeta(4));
        testVersion("0.0.1-rc2", hasMajor(0), hasMinor(0), hasPatch(1), isRC(2));
        testVersion("1.2.0", hasMajor(1), hasMinor(2), hasPatch(0), isRelease());
        testVersion("0.0.0", hasMajor(0), hasMinor(0), hasPatch(0), isDev());
    }

    @Test
    public void testComparison() {
        FirmwareVersion a = FirmwareVersion.parse("1.2.3-beta2");
        FirmwareVersion b = FirmwareVersion.parse("1.2.3-beta3");
        FirmwareVersion c = FirmwareVersion.parse("1.2.3-rc3");
        FirmwareVersion d = FirmwareVersion.parse("1.2.4-rc2");
        FirmwareVersion e = FirmwareVersion.parse("1.3.0");
        FirmwareVersion f = FirmwareVersion.parse("2.0.0-alpha2");
        FirmwareVersion fBis = FirmwareVersion.parse("2.0.0-alpha2");

        assertThat(a, lessThan(b));
        assertThat(b, lessThan(c));
        assertThat(c, lessThan(d));
        assertThat(d, lessThan(e));
        assertThat(e, lessThan(f));

        assertThat(f, equalTo(fBis));

        assertThat(f, greaterThan(e));
        assertThat(e, greaterThan(d));
        assertThat(d, greaterThan(c));
        assertThat(c, greaterThan(b));
        assertThat(b, greaterThan(a));

        FirmwareVersion dev = FirmwareVersion.parse("0.0.0");
        assertThat(dev, greaterThan(a));
        assertThat(dev, greaterThan(b));
        assertThat(dev, greaterThan(c));
        assertThat(dev, greaterThan(d));
        assertThat(dev, greaterThan(e));

        assertThat(a, lessThan(dev));
        assertThat(b, lessThan(dev));
        assertThat(c, lessThan(dev));
        assertThat(d, lessThan(dev));
        assertThat(e, lessThan(dev));
    }
}
