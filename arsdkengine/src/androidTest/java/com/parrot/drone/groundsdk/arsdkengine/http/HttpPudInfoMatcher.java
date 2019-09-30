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

package com.parrot.drone.groundsdk.arsdkengine.http;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collection;

import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;

final class HttpPudInfoMatcher {

    private static Matcher<HttpPudInfo> pudNameIs(@Nullable String name) {
        return valueMatcher(name, "name", HttpPudInfo::getName);
    }

    private static Matcher<HttpPudInfo> pudDateIs(@Nullable String date) {
        return valueMatcher(date, "date", HttpPudInfo::getDate);
    }

    private static Matcher<HttpPudInfo> pudSizeIs(@IntRange(from = 0) long size) {
        return valueMatcher(size, "size", HttpPudInfo::getSize);
    }

    private static Matcher<HttpPudInfo> pudUrlIs(@Nullable String url) {
        return valueMatcher(url, "url", HttpPudInfo::getUrl);
    }

    private static Matcher<HttpPudInfo> pudEquals(@NonNull HttpPudInfo pud) {
        return allOf(
                pudNameIs(pud.getName()),
                pudDateIs(pud.getDate()),
                pudSizeIs(pud.getSize()),
                pudUrlIs(pud.getUrl()));
    }

    static Matcher<Iterable<? extends HttpPudInfo>> pudListEquals(
            @NonNull Collection<HttpPudInfo> list) {
        Collection<Matcher<? super HttpPudInfo>> matchers = new ArrayList<>();
        for (HttpPudInfo pud : list) {
            matchers.add(pudEquals(pud));
        }
        return containsInAnyOrder(matchers);
    }

    private HttpPudInfoMatcher() {
    }
}
