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

import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.io.IoStreams;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveJobTest {

    private ArchiveJob mJob;

    @Mock
    private BlackBoxEngine mMockEngine;

    @Captor
    private ArgumentCaptor<Collection<File>> mBlackboxCaptor;

    private boolean mFailWrite;

    @Before
    public void setup() {
        TestExecutor.setup();
        mFailWrite = false;

        when(mMockEngine.getWorkDirectory()).thenAnswer(
                invocationOnMock -> new File(mMockEngine.getEngineDirectory(), "current_workdir"));

        mJob = new ArchiveJob(mMockEngine, output -> {
            try {
                if (mFailWrite) {
                    throw new IOException("Test case: blackbox write failure");
                }
                IoStreams.transfer(ApplicationProvider.getApplicationContext().getResources().openRawResource(
                        R.raw.mock_blackbox), output);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        });
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
        File nonCreatableReportsDir = new File("/non_creatable_dir");

        when(mMockEngine.getEngineDirectory()).thenReturn(nonCreatableReportsDir);

        mJob.launch();

        assertThat(nonCreatableReportsDir.exists(), is(false));
        assertThat(nonCreatableReportsDir.isDirectory(), is(false));

        verify(mMockEngine, never()).queueForUpload(any());
    }

    @Test
    public void testArchive() {
        // non existent engine dir should be created
        File nonExistentReportsDir = new File(ApplicationProvider.getApplicationContext().getFilesDir(),
                "non_existent_dir");
        Files.deleteDirectoryTree(nonExistentReportsDir);

        when(mMockEngine.getEngineDirectory()).thenReturn(nonExistentReportsDir);

        mJob.launch();

        assertThat(nonExistentReportsDir.exists(), is(true));
        assertThat(nonExistentReportsDir.isDirectory(), is(true));

        verify(mMockEngine).queueForUpload(mBlackboxCaptor.capture());

        File blackbox = mBlackboxCaptor.getValue().iterator().next();
        assertThat(mMockEngine.getWorkDirectory().listFiles(), arrayContaining(blackbox));

        ByteArrayOutputStream blackboxContent = new ByteArrayOutputStream();
        ByteArrayOutputStream expectedContent = new ByteArrayOutputStream();
        try {
            IoStreams.transfer(new FileInputStream(blackbox), blackboxContent);
            GZIPOutputStream zipStream = new GZIPOutputStream(expectedContent);
            IoStreams.transfer(ApplicationProvider.getApplicationContext().getResources().openRawResource(
                    R.raw.mock_blackbox), zipStream);
            zipStream.close();
        } catch (IOException | InterruptedException e) {
            throw new AssertionError(e);
        }
        assertThat(blackboxContent.toByteArray(), is(expectedContent.toByteArray()));
    }

    @Test
    public void testFailureDuringArchive() {
        // non existent engine dir should be created
        File nonExistentReportsDir = new File(ApplicationProvider.getApplicationContext().getFilesDir(),
                "non_existent_dir");
        Files.deleteDirectoryTree(nonExistentReportsDir);

        when(mMockEngine.getEngineDirectory()).thenReturn(nonExistentReportsDir);

        mFailWrite = true;
        mJob.launch();

        assertThat(nonExistentReportsDir.exists(), is(true));
        assertThat(nonExistentReportsDir.isDirectory(), is(true));
        assertThat(mMockEngine.getWorkDirectory().listFiles(), emptyArray());
        verify(mMockEngine, never()).queueForUpload(any());
    }
}
