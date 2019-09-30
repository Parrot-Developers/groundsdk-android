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

package com.parrot.drone.groundsdk;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public final class AssetInstaller {

    /** Suffix for files that the installer should not install. */
    private static final String IGNORE_FILE_SUFFIX = ".asset_installer";

    /**
     * Suffix for files that have to be renamed.
     * <p>
     * To prevent assets files with ".gz" extension to be renamed at application build, they are named with a special
     * extension. They will be renamed at runtime when they will be installed by {@link #installAsset}.
     */
    private static final String RENAME_FILE_SUFFIX = ".suffixtoremove";

    /** Root of installed assets in test packages's local storage on the device. */
    private static final String LOCAL_ASSET_DIR = "assets";

    /**
     * Copies an asset from the test package's assets into test package's local storage on the device
     * <p>
     * The source asset may be a directory.
     *
     * @param sourceAssetPath path of the asset to install, relative to the assets root
     *
     * @return a file pointing to the copied asset on the local file system
     */
    @NonNull
    public static File installAsset(@NonNull String sourceAssetPath) {
        Context context = ApplicationProvider.getApplicationContext();
        AssetManager assetManager = context.getAssets();
        Queue<String> copyQueue = new LinkedList<>(Collections.singletonList(sourceAssetPath));
        File destRoot = new File(context.getFilesDir(), LOCAL_ASSET_DIR);

        while (!copyQueue.isEmpty()) {
            String toCopy = copyQueue.poll();
            // ignore asset_installer files
            if (toCopy.endsWith(IGNORE_FILE_SUFFIX)) {
                continue;
            }

            // files needing a rename
            String destRelativePath;
            if (toCopy.endsWith(RENAME_FILE_SUFFIX)) {
                destRelativePath = toCopy.substring(0, toCopy.length() - RENAME_FILE_SUFFIX.length());
            } else {
                destRelativePath = toCopy;
            }

            File dest = new File(destRoot, destRelativePath);
            // ensure destination parent directory exists
            if (!ensurePathFor(dest)) {
                throw new AssertionError("Could not create path to: " + dest);
            }
            InputStream srcStream = null;
            try {
                // tell if file or directory ...
                srcStream = assetManager.open(toCopy);
                // ... just a file, copy it
                try {
                    copyToFile(srcStream, dest);
                } catch (IOException e) {
                    throw new AssertionError("Could not copy file [src: " + toCopy + ", dst: " + dest + "]");
                }
            } catch (IOException e) {
                // ... a directory
                try {
                    // create empty destination directory
                    if (!dest.mkdir() && !deleteDirectoryContent(dest)) {
                        throw new AssertionError("Could not empty directory: " + dest);
                    }
                    // add its content to be copied on next passes
                    //noinspection ConstantConditions
                    for (String dirFile : assetManager.list(toCopy)) {
                        copyQueue.add(toCopy + File.separator + dirFile);
                    }
                } catch (IOException ignored) {
                    throw new AssertionError("No such asset file or directory: " + toCopy);
                }
            } finally {
                closeUnchecked(srcStream);
            }
        }

        return localAsset(sourceAssetPath);
    }

    /**
     * Deletes an installed asset from the device's local asset storage.
     *
     * @param sourceAssetPath path of the source asset to uninstall
     */
    public static void uninstallAsset(@NonNull String sourceAssetPath) {
        File localAsset = localAsset(sourceAssetPath);
        if (!deleteDirectoryContent(localAsset) || !localAsset.delete()) {
            throw new AssertionError("Could not uninstall local asset: " + localAsset);
        }
    }

    /**
     * Obtains the path of an asset on the device's local asset storage.
     *
     * @param sourceAssetPath path of the asset to obtain a local path from
     *
     * @return the corresponding local path for the given asset
     */
    @NonNull
    private static File localAsset(@NonNull String sourceAssetPath) {
        return new File(ApplicationProvider.getApplicationContext().getFilesDir(),
                LOCAL_ASSET_DIR + File.separator + sourceAssetPath);
    }

    /**
     * Dumps an input stream to a file.
     * <p>
     * Destination file is created if it does not exist; otherwise it is overwritten.
     *
     * @param srcStream src stream to dump
     * @param dstFile   destination file to dump to
     *
     * @throws IOException in case of any failure
     */
    private static void copyToFile(@NonNull InputStream srcStream, @NonNull File dstFile) throws IOException {
        OutputStream dstStream = null;
        try {
            dstStream = new FileOutputStream(dstFile);
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = srcStream.read(buffer)) > 0) {
                dstStream.write(buffer, 0, readBytes);
            }
        } finally {
            closeUnchecked(dstStream);
        }
    }

    /**
     * Ensures that all parent directories of a given file exists.
     * <p>
     * Directories are created in case the do not exist.
     *
     * @param file file to make sure a path exists for
     *
     * @return {@code true} if a path exists for the file at the end of the process, otherwise {@code false}
     */
    private static boolean ensurePathFor(@NonNull File file) {
        File parent = file.getParentFile();
        return parent != null && (parent.exists() || parent.mkdirs());
    }

    /**
     * Deletes a directory's content.
     * <p>
     * The directory itself is not deleted.
     *
     * @param dir path to the directory to delete
     *
     * @return {@code true} if the operation was successful, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean deleteDirectoryContent(@NonNull File dir) {
        Deque<File> deletionDeque = new LinkedList<>(Collections.singletonList(dir));
        boolean success = true;
        while (!deletionDeque.isEmpty()) {
            File toDelete = deletionDeque.peek();
            File[] children = toDelete.listFiles();
            if (children == null || children.length == 0) {
                if (toDelete != dir) {
                    success &= toDelete.delete();
                } else if (children == null) {
                    // root was not a directory
                    success = false;
                }
                deletionDeque.pop();
            } else {
                // Add the children before the folder because they have to be deleted first
                for (File child : children) {
                    deletionDeque.addFirst(child);
                }
            }
        }
        return success;
    }

    /**
     * Closes the given {@code Closeable}, ignoring any thrown {@link IOException}.
     *
     * @param closeable closable to close, may be {@code null}, in which case this method does nothing
     */
    private static void closeUnchecked(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private AssetInstaller() {
    }
}
