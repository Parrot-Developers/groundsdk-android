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

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaResourceList;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class MediaResourceListTest {

    private MediaItem.Resource mResource1, mResource2, mResource5;

    private MediaResourceList mList;

    @Before
    public void setUp() {
        MockMediaItem media1 = MockMediaItem.create("media1", "resource1", "resource2", "resource3");
        MockMediaItem media2 = MockMediaItem.create("media2", "resource4", "resource5");

        mResource1 = media1.getResources().get(0);
        mResource2 = media1.getResources().get(1);
        mResource5 = media2.getResources().get(1);

        mList = new MediaResourceList();
    }

    @Test
    public void testGet() {
        mList.add(mResource1);

        assertThat(mList.get(0), is(mResource1));
    }

    @Test
    public void testAdd() {
        mList.add(mResource1);

        assertThat(mList, contains(mResource1));
    }

    @Test
    public void testSet() {
        mList.add(mResource1);
        mList.set(0, mResource2);

        assertThat(mList, contains(mResource2));
    }

    @Test
    public void testSize() {
        assertThat(mList.size(), is(0));

        mList.add(mResource1);

        assertThat(mList.size(), is(1));
    }

    @Test
    public void testParcel() {
        mList.addAll(Arrays.asList(mResource1, mResource2, mResource5));

        Parcel parcel = Parcel.obtain();

        parcel.writeParcelable(mList, 0);

        parcel.setDataPosition(0);

        assertThat(parcel.readParcelable(mList.getClass().getClassLoader()), is(mList));
    }

    private abstract static class MockMediaItem implements MediaItemCore {

        @NonNull
        static MockMediaItem create(@NonNull String uid, @NonNull String... resourceUids) {
            return mock(MockMediaItem.class, withSettings()
                    .useConstructor(uid, resourceUids)
                    .defaultAnswer(invocation -> {
                        throw new UnsupportedOperationException();
                    }));
        }

        @NonNull
        private final String mUid;

        @NonNull
        private final List<Resource> mResources;

        MockMediaItem(@NonNull String uid, @NonNull String... resourceUids) {
            mUid = uid;
            mResources = Stream.of(resourceUids).map(it -> Resource.create(it, this)).collect(Collectors.toList());
        }

        @NonNull
        @Override
        public final String getUid() {
            return mUid;
        }

        @NonNull
        @Override
        public final List<? extends MediaResourceCore> getResources() {
            return mResources;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockMediaItem that = (MockMediaItem) o;

            return mUid.equals(that.mUid);
        }

        @Override
        public final int hashCode() {
            return mUid.hashCode();
        }

        @Override
        public final int describeContents() {
            return 0;
        }

        @Override
        public final void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mUid);
            dest.writeStringArray(mResources.stream().map(Resource::getUid).toArray(String[]::new));
        }

        public static final Parcelable.Creator<MockMediaItem> CREATOR = new Parcelable.Creator<MockMediaItem>() {

            @Override
            public MockMediaItem createFromParcel(@NonNull Parcel src) {
                //noinspection ConstantConditions
                return create(src.readString(), src.createStringArray());
            }

            @Override
            public MockMediaItem[] newArray(int size) {
                return new MockMediaItem[size];
            }
        };

        private abstract static class Resource implements MediaResourceCore {

            @NonNull
            static Resource create(@NonNull String uid, @NonNull MediaItemCore parent) {
                return mock(Resource.class, withSettings()
                        .useConstructor(uid, parent)
                        .defaultAnswer(invocation -> {
                            throw new UnsupportedOperationException();
                        }));
            }

            @NonNull
            private final String mUid;

            @NonNull
            private final MediaItemCore mParent;

            Resource(@NonNull String uid, @NonNull MediaItemCore parent) {
                mUid = uid;
                mParent = parent;
            }

            @NonNull
            @Override
            public final String getUid() {
                return mUid;
            }

            @NonNull
            @Override
            public final MediaItemCore getMedia() {
                return mParent;
            }

            @Override
            public final boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Resource that = (Resource) o;

                return mUid.equals(that.mUid);
            }

            @Override
            public final int hashCode() {
                return mUid.hashCode();
            }
        }
    }
}
