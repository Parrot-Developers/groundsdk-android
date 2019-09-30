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

package com.parrot.drone.groundsdk.arsdkengine.persistence;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.MockSharedPreferences;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PersistentStoreTests {

    private MockSharedPreferences mPref;

    private Context mContext;

    @Before
    public void setUp() {
        mPref = new MockSharedPreferences();
        mContext = mock(Context.class);
        doReturn(mPref).when(mContext).getSharedPreferences(any(), anyInt());
    }

    @Test
    public void testEmpty() {
        PersistentStore store = new PersistentStore(mContext);
        Set<String> uids = store.getDevicesUid();
        assertThat(uids, empty());
    }

    @Test
    public void testGetDeviceNotFound() {
        PersistentStore store = new PersistentStore(mContext);
        PersistentStore.Dictionary dictionary = store.getDevice("123");
        assertThat(dictionary.isNew(), is(true));
        assertThat(mPref.getChangeCnt(), is(0));
    }

    @Test
    public void testCreate() {
        PersistentStore store = new PersistentStore(mContext);
        PersistentStore.Dictionary dictionary = createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name");
        dictionary.commit();
        assertThat(dictionary, is(notNullValue()));
        assertThat(dictionary.getInt(PersistentStore.KEY_DEVICE_MODEL), is(Drone.Model.ANAFI_4K.id()));
        assertThat(dictionary.getString(PersistentStore.KEY_DEVICE_NAME), is("name"));
        assertThat(mPref.getChangeCnt(), is(1));

        Set<String> uids = store.getDevicesUid();
        assertThat(uids, contains("123"));
    }

    @Test
    public void testAdd() {
        PersistentStore store = new PersistentStore(mContext);
        PersistentStore.Dictionary dictionary = createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name");
        dictionary.put("STR", "string").put("INT", 1).put("DBL", 4.5).put("BOOL", true).commit();

        dictionary = store.getDevice("123");
        assertThat(dictionary.getString("STR"), is("string"));
        assertThat(dictionary.getInt("INT"), is(1));
        assertThat(dictionary.getDouble("DBL"), is(4.5));
        assertThat(dictionary.getBoolean("BOOL"), is(true));
        assertThat(dictionary.getString("X"), is(nullValue()));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testAddChild() {
        PersistentStore store = new PersistentStore(mContext);
        PersistentStore.Dictionary subDictionary = createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name")
                .getDictionary("SUB");
        assertThat(subDictionary.isNew(), is(true));
        subDictionary.put("A", "A").put("B", "B").commit();

        subDictionary = store.getDevice("123").getDictionary("SUB");
        assertThat(subDictionary.isNew(), is(false));
        assertThat(subDictionary.getString("A"), is("A"));
        assertThat(subDictionary.getString("B"), is("B"));
        assertThat(subDictionary.getString("X"), is(nullValue()));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testUpdate() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name").put("STR", "string").commit();
        mPref.clearChangeCnt();

        store.getDevice("123").put("STR", "string2").commit();

        assertThat(store.getDevice("123").getString("STR"), is("string2"));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testUpdateChild() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name")
                .getDictionary("SUB").put("A", "A").commit();
        mPref.clearChangeCnt();

        store.getDevice("123").getDictionary("SUB").put("A", "a").commit();

        assertThat(store.getDevice("123").getDictionary("SUB").getString("A"), is("a"));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testClear() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name").put("STR", "string").commit();
        mPref.clearChangeCnt();
        assertThat(store.getDevice("123"), is(notNullValue()));

        store.getDevice("123").clear().commit();

        assertThat(store.getDevice("123").isNew(), is(true));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testClearChild() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name")
                .getDictionary("SUB").put("A", "A").commit();
        assertThat(store.getDevice("123").getDictionary("SUB").isNew(), is(false));
        mPref.clearChangeCnt();

        store.getDevice("123").getDictionary("SUB").clear().commit();

        assertThat(store.getDevice("123").getDictionary("SUB").isNew(), is(true));
        assertThat(mPref.getChangeCnt(), is(1));
    }

    @Test
    public void testCommitWithoutChanges() {
        PersistentStore store = new PersistentStore(mContext);
        PersistentStore.Dictionary deviceDict = createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "name");
        PersistentStore.Dictionary subDict = deviceDict.getDictionary("SUB");
        subDict.put("A", "a").put("A", "a");
        deviceDict.commit();
        assertThat(mPref.getChangeCnt(), is(1));
        mPref.clearChangeCnt();
        subDict.commit();
        deviceDict.commit();
        assertThat(mPref.getChangeCnt(), is(0));
    }

    @NonNull
    private static PersistentStore.Dictionary createDeviceDict(@NonNull PersistentStore store,
                                                               @NonNull String uid,
                                                               @NonNull DeviceModel model,
                                                               @NonNull String name) {
        PersistentStore.Dictionary dict = store.getDevice(uid);
        dict.put(PersistentStore.KEY_DEVICE_MODEL, model.id()).put(PersistentStore.KEY_DEVICE_NAME, name);
        return dict;
    }
}