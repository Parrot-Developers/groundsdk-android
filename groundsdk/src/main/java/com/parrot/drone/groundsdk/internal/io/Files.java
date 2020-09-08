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

package com.parrot.drone.groundsdk.internal.io;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Provides file manipulation utility methods.
 */
public final class Files {

    /** Sorts Files by modification date. Most recent files last. */
    public static final Comparator<File> DESCENDING_DATE = (lhs, rhs) -> Long.compare(
            lhs.lastModified(), rhs.lastModified());

    /**
     * Creates the given directory and all required parent directories if needed.
     * <p>
     * Does nothing if the given directory already exists.
     *
     * @param dir directory to create
     *
     * @return {@code true} if some directory was effectively created, {@code false} if the given directory already
     *         exists
     *
     * @throws IOException if the given directory could not be created
     */
    public static boolean makeDirectories(@NonNull File dir) throws IOException {
        if (dir.mkdirs()) {
            return true;
        } else if (dir.exists()) {
            return false;
        } else {
            throw new IOException("Could not create directory: " + dir);
        }
    }

    /**
     * Writes to a file.
     * <p>
     * This method overwrites any existing file.
     * <p>
     * This method reads chunks of {@value IoStreams#DEFAULT_TRANSFER_CHUNK_SIZE} bytes from {@code srcStream} and
     * writes them to {@code dstFile}.
     *
     * @param srcStream input stream to read data from.
     * @param dstFile   file to write to
     *
     * @throws IOException          in case write failed
     * @throws InterruptedException if the current thread is interrupted while this method executes. Interruption
     *                              status is checked in between each chunk read and write.
     */
    public static void writeFile(@NonNull InputStream srcStream, @NonNull File dstFile)
            throws IOException, InterruptedException {
        writeFile(srcStream, dstFile, IoStreams.DEFAULT_TRANSFER_CHUNK_SIZE);
    }

    /**
     * Writes to a file.
     * <p>
     * This method overwrites any existing file.
     * <p>
     * This method reads chunks of {@code chunkSize} bytes from {@code srcStream} and writes them to {@code dstFile}.
     *
     * @param srcStream input stream to read data from.
     * @param dstFile   file to write to
     * @param chunkSize size of chunks read from {@code srcStream}, in bytes
     *
     * @throws IOException          in case write failed
     * @throws InterruptedException if the current thread is interrupted while this method executes. Interruption
     *                              status is checked in between each chunk read and write.
     */
    public static void writeFile(@NonNull InputStream srcStream, @NonNull File dstFile, int chunkSize)
            throws IOException, InterruptedException {
        makeDirectories(dstFile.getParentFile());

        try (OutputStream dstStream = new FileOutputStream(dstFile)) {
            IoStreams.transfer(srcStream, dstStream, chunkSize);
        }
    }

    /**
     * Copies the source file to the destination file.
     *
     * @param srcFile file to copy
     * @param dstFile destination of the copy
     *
     * @throws IOException          in case copy failed
     * @throws InterruptedException if the current thread is interrupted while this method executes. Interruption
     *                              status is checked in between each chunk read and write.
     */
    public static void copyFile(@NonNull File srcFile, @NonNull File dstFile) throws IOException, InterruptedException {
        makeDirectories(dstFile.getParentFile());
        File tmp = File.createTempFile(".copy", ".tmp", dstFile.getParentFile());

        try (FileInputStream src = new FileInputStream(srcFile)) {
            writeFile(src, tmp);
            if (!tmp.renameTo(dstFile)) {
                throw new IOException("Could not rename temporary file " + tmp + " to " + dstFile);
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    /**
     * Deletes the given directory and all of its children.
     * <p>
     * Tries its best to delete all files in the directory. In case some file cannot be
     * deleted, it is skipped, and deletion proceeds.
     *
     * @param rootDir directory to delete
     *
     * @return {@code true} if the directory tree was successfully and completely deleted, otherwise {@code false}
     */
    public static boolean deleteDirectoryTree(@NonNull File rootDir) {
        Deque<File> deletionQueue = new LinkedList<>(Collections.singletonList(rootDir));
        while (!deletionQueue.isEmpty()) {
            File toDelete = deletionQueue.peek();
            assert toDelete != null;
            File[] children = toDelete.listFiles();
            if (children == null || children.length == 0) { // either a file or an empty directory, delete
                //noinspection ResultOfMethodCallIgnored
                toDelete.delete();
                deletionQueue.pop();
            } else { // queue children before folder to delete them first
                for (File child : children) {
                    deletionQueue.addFirst(child);
                }
            }
        }
        return !rootDir.exists();
    }

    /**
     * Reads a raw android resource as a string.
     *
     * @param resources android resources
     * @param resId     identifier of the raw resource to read
     * @param charset   character set to use for decoding raw binary data to a string
     *
     * @return a string decoded from loaded resource data
     *
     * @throws IOException          in case reading failed
     * @throws InterruptedException if the current thread is interrupted while this method executes.
     */
    @NonNull
    public static String readRawResourceAsString(@NonNull Resources resources, @RawRes int resId,
                                                 @NonNull Charset charset)
            throws IOException, InterruptedException {
        try (InputStream src = resources.openRawResource(resId)) {
            ByteArrayOutputStream dst = new ByteArrayOutputStream(src.available());
            IoStreams.transfer(src, dst);
            return dst.toString(charset.name());
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private Files() {
    }
}
