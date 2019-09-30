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
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.JsonParser;
import com.parrot.drone.groundsdk.MockSharedPreferences;
import com.parrot.drone.groundsdk.arsdkengine.test.R;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Runs application defaults merging tests.
 * <p>
 * Those are JSON parameterized tests; test cases are defined by all 'app_defaults_*' files in test package raw
 * resources. <br/>
 * Each file may contain <ul>
 * <li>either a single test case, in which regard the file contains a single JSON object root entity,</li>
 * <li>or multiple test cases, in which regard the file contains a JSON array root entity, where each child
 * element is a JSON object defining a single test case.</li>
 * </ul>
 * A JSON test case declaration is a JSON object that <ul>
 * <li>may contain a 'name' entry that defines a custom name for the test case. If not present, then the test
 * will have the same name as the file it is declared in, optionally indexed in case the file defines multiple test
 * cases,</li>
 * <li>must contain an 'app-defaults' entry, which defines the content of the application defaults file to test,</li>
 * <li>may contain an 'initial-store' entry, which allows to setup initial content for arsdkengine persistent store,
 * before the test runs,</li>
 * <li>must contain a 'expected-store' entry, which defines the expected content of arsdkengine persistent store
 * after the test runs.</li>
 * </ul>
 * Any other keys in a JSON test case declaration is ignored; by convention, a 'desc' entry is defined to give a
 * thorough description of the test case.
 */
@RunWith(Parameterized.class)
public class AppDefaultsTests {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> initParameters() {
        Collection<Object[]> testParams = new LinkedList<>();
        for (Field fRawId : R.raw.class.getFields()) {
            String testFileName = fRawId.getName();
            if (testFileName.startsWith("app_defaults_")) {
                try (InputStream resStream = ApplicationProvider.getApplicationContext().getResources().openRawResource(
                        fRawId.getInt(null))) {
                    byte[] buffer = new byte[resStream.available()];
                    //noinspection ResultOfMethodCallIgnored
                    resStream.read(buffer);
                    String data = new String(buffer, StandardCharsets.UTF_8);
                    if (new JSONTokener(data).nextValue() instanceof JSONArray) {
                        JSONArray multiTestJson = new JSONArray(data);
                        for (int i = 0, N = multiTestJson.length(); i < N; i++) {
                            addCase(multiTestJson.getJSONObject(i), testFileName + " " + (i + 1), testParams);
                        }
                    } else {
                        addCase(new JSONObject(data), testFileName, testParams);
                    }
                } catch (IOException | JSONException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        return testParams;
    }

    @NonNull
    private final PersistentStore mStore;

    @NonNull
    private final PersistentStore mExpectedStore;

    @NonNull
    private final AppDefaults mAppDefaults;

    public AppDefaultsTests(@SuppressWarnings("unused") @NonNull String testName, @NonNull JSONObject testJson)
            throws JSONException {
        GroundSdkConfig.loadDefaults();
        Context context = mock(Context.class);
        doAnswer((invocation) -> new MockSharedPreferences())
                .when(context).getSharedPreferences(anyString(), anyInt());
        doReturn(context).when(context).getApplicationContext();

        mStore = new PersistentStore(context);
        mExpectedStore = new PersistentStore(context);

        loadStore(mStore, testJson.optJSONObject("initial-store"));
        loadStore(mExpectedStore, testJson.getJSONObject("expected-store"));
        mAppDefaults = new AppDefaults(mStore, testJson.getJSONObject("app-defaults"));
    }

    @Test
    public void test() throws JSONException {
        mAppDefaults.importAll();

        Map<String, ?> actualStore = mStore.content();
        Map<String, ?> expectedStore = mExpectedStore.content();

        //noinspection ConstantConditions
        assertThat(actualStore.keySet(), containsInAnyOrder(expectedStore.keySet().toArray()));

        for (String key : actualStore.keySet()) {
            //noinspection ConstantConditions
            assertThat(new JsonParser().parse(actualStore.get(key).toString()),
                    equalTo(new JsonParser().parse(expectedStore.get(key).toString())));
        }
    }

    private static void loadStore(@NonNull PersistentStore store, @Nullable JSONObject load) throws JSONException {
        if (load != null) {
            for (Iterator<String> iter = load.keys(); iter.hasNext(); ) {
                String key = iter.next();
                store.storeContent(key, load.getJSONObject(key));
            }
        }
    }

    private static void addCase(@NonNull JSONObject testJson, @NonNull String testBaseName,
                                @NonNull Collection<Object[]> params) {
        params.add(new Object[] {testBaseName + ": " + testJson.optString("name"), testJson});
    }
}
