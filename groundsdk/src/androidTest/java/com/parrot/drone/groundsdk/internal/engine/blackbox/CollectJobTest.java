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

package com.parrot.drone.groundsdk.internal.engine.blackbox;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.AssetInstaller;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CollectJobTest {

    private static final String MOCK_BLACKBOX_ASSET_DIR = "mock_blackbox_files";

    private CollectJob mJob;

    @Mock
    private BlackBoxEngine mMockEngine;

    @Captor
    private ArgumentCaptor<Collection<File>> mBlackboxCaptor;

    @Before
    public void setup() {
        TestExecutor.setup();

        when(mMockEngine.getWorkDirectory()).thenAnswer(
                invocationOnMock -> new File(mMockEngine.getEngineDirectory(), "current_workdir"));

        mJob = new CollectJob(mMockEngine, 1 /* blackbox_a size */ + 1 /* blackbox_b size */,
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testJobCancel() {
        mJob.onComplete(null, null, true);
        verify(mMockEngine, never()).queueForUpload(any());
    }

    @Test
    public void testNonCreatableEngineDir() {
        File nonCreatableDir = new File("/non_creatable_dir");

        when(mMockEngine.getEngineDirectory()).thenReturn(nonCreatableDir);

        mJob.launch();

        assertThat(nonCreatableDir.exists(), is(false));
        assertThat(nonCreatableDir.isDirectory(), is(false));

        verify(mMockEngine, never()).queueForUpload(any());
    }

    @Test
    public void testNonExistentEngineDir() {
        File nonExistentDir = new File(ApplicationProvider.getApplicationContext().getFilesDir(),
                "non_existent_dir");
        Files.deleteDirectoryTree(nonExistentDir);

        when(mMockEngine.getEngineDirectory()).thenReturn(nonExistentDir);

        mJob.launch();

        assertThat(nonExistentDir.exists(), is(true));
        assertThat(nonExistentDir.isDirectory(), is(true));

        verify(mMockEngine, never()).queueForUpload(any());
    }

    @Test
    public void testCollect() {
        File mockEngineDir = AssetInstaller.installAsset(MOCK_BLACKBOX_ASSET_DIR);
        try {
            when(mMockEngine.getEngineDirectory()).thenReturn(mockEngineDir);

            File quotaFile = new File(mockEngineDir, "previous_workdir/blackbox_quota");
            if (!quotaFile.setLastModified(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3))) {
                throw new AssertionError();
            }

            File dateFile = new File(mockEngineDir, "previous_workdir/blackbox_date");
            if (!dateFile.setLastModified(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2))) {
                throw new AssertionError();
            }

            // ensure all files that should be pruned are present at first
            assertThat(new File(mockEngineDir, "current_workdir/incomplete_blackbox.tmp").exists(), is(true));
            assertThat(new File(mockEngineDir, "previous_workdir/incomplete_blackbox.tmp").exists(), is(true));
            assertThat(new File(mockEngineDir, "random_file").exists(), is(true));
            assertThat(new File(mockEngineDir, "empty_dir").exists(), is(true));
            assertThat(quotaFile.exists(), is(true));
            assertThat(dateFile.exists(), is(true));

            mJob.launch();

            assertThat(mockEngineDir.exists(), is(true));
            assertThat(mockEngineDir.isDirectory(), is(true));

            // ensure random files at engine dir root have been pruned
            assertThat(new File(mockEngineDir, "random_file").exists(), is(false));
            // ensure random dirs at engine dir root have been pruned
            assertThat(new File(mockEngineDir, "empty_dir").exists(), is(false));
            // ensure random dirs in previous work dirs' root have been pruned
            assertThat(new File(mockEngineDir, "previous_workdir/random_dir").exists(), is(false));
            // ensure tmp files from previous work dirs have been pruned
            assertThat(new File(mockEngineDir, "previous_workdir/incomplete_blackbox.tmp").exists(), is(false));
            // ensure valid files from previous work dirs have been left untouched
            assertThat(new File(mockEngineDir, "previous_workdir/blackbox_a").exists(), is(true));
            assertThat(new File(mockEngineDir, "previous_workdir/blackbox_b").exists(), is(true));
            // ensure eldest files have been removed to respect quota
            assertThat(quotaFile.exists(), is(false));
            // ensure files beyond date validity have been removed
            assertThat(dateFile.exists(), is(false));
            // ensure tmp files from current work dirs have been left untouched
            assertThat(new File(mockEngineDir, "current_workdir/incomplete_blackbox.tmp").exists(), is(true));
            // ensure valid files from current work dir have been left untouched
            assertThat(new File(mockEngineDir, "current_workdir/blackbox_c").exists(), is(true));

            verify(mMockEngine).queueForUpload(mBlackboxCaptor.capture());
            assertThat(mBlackboxCaptor.getValue(), containsInAnyOrder(
                    new File(mockEngineDir, "previous_workdir/blackbox_a"),
                    new File(mockEngineDir, "previous_workdir/blackbox_b")
            ));
        } finally {
            AssetInstaller.uninstallAsset(MOCK_BLACKBOX_ASSET_DIR);
        }
    }
}
