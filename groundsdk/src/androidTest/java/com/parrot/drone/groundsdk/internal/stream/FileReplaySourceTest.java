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

package com.parrot.drone.groundsdk.internal.stream;

import android.os.Parcel;

import com.parrot.drone.groundsdk.stream.FileReplay;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class FileReplaySourceTest {

    private File mFile;

    private FileSourceCore mSource;

    @Before
    public void setUp() {
        mFile = new File("/replay");
        mSource = new FileSourceCore(mFile, "track");
    }

    @Test
    public void testSource() {
        assertThat(mSource.file(), is(mFile));
        assertThat(mSource.trackName(), is("track"));
    }

    @Test
    public void testPublicConstructors() {
        FileReplay.Source source = FileReplay.videoTrackOf(mFile, "track");
        assertThat(source.file(), is(mFile));
        assertThat(source.trackName(), is("track"));

        source = FileReplay.defaultVideoTrackOf(mFile);
        assertThat(source.file(), is(mFile));
        assertThat(source.trackName(), nullValue());
    }

    @Test
    public void testSourceParcel() {
        Parcel p = Parcel.obtain();

        p.writeParcelable(mSource, 0);

        p.setDataPosition(0);

        FileSourceCore fromParcel = p.readParcelable(FileReplay.Source.class.getClassLoader());

        assertThat(fromParcel, notNullValue());
        assertThat(fromParcel.file(), is(mSource.file()));
        assertThat(fromParcel.trackName(), is(mSource.trackName()));
    }
}
