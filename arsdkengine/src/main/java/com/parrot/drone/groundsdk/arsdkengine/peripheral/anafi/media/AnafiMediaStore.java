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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaIndexingState;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItem;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaItemCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaRequest;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaStoreCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMediastore;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.List;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_MEDIA;

/** MediaStore peripheral controller for Anafi family drones. */
public final class AnafiMediaStore extends DronePeripheralController {

    /** The MediaStore peripheral for which this object is the backend. */
    @NonNull
    private final MediaStoreCore mMediaStore;

    /** HTTP media client. */
    @Nullable
    private HttpMediaClient mMediaClient;

    /** Caches last media list browse result, when content changes are being watched; {@code null} otherwise. */
    @Nullable
    private List<MediaItemImpl> mCachedMediaList;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiMediaStore(@NonNull DroneController droneController) {
        super(droneController);
        mMediaStore = new MediaStoreCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        // start monitoring changes
        mMediaClient = mDeviceController.getHttpClient(HttpMediaClient.class);
        mMediaStore.publish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureMediastore.UID) {
            ArsdkFeatureMediastore.decode(command, mMediaStoreCallback);
        }
    }

    @Override
    protected void onDisconnecting() {
        mMediaStore.unpublish();
        mCachedMediaList = null;
        if (mMediaClient != null) {
            mMediaClient.dispose();
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureMediastore is decoded. */
    private final ArsdkFeatureMediastore.Callback mMediaStoreCallback =
            new ArsdkFeatureMediastore.Callback() {

                @Override
                public void onState(@Nullable ArsdkFeatureMediastore.State state) {
                    if (state == null) {
                        throw new ArsdkCommand.RejectedEventException("Invalid indexing state");
                    }

                    if (state == ArsdkFeatureMediastore.State.NOT_AVAILABLE) {
                        clearCachedMediaList();
                        mMediaStore.updatePhotoMediaCount(0)
                                   .updateVideoMediaCount(0)
                                   .updatePhotoResourceCount(0)
                                   .updateVideoResourceCount(0);
                    }

                    mMediaStore.updateIndexingState(IndexingStateAdapter.from(state)).notifyUpdated();
                }

                @Override
                public void onCounters(int videoMediaCount, int photoMediaCount, int videoResourceCount,
                                       int photoResourceCount) {
                    if (videoMediaCount < 0 || photoMediaCount < 0 || videoResourceCount < 0
                        || photoResourceCount < 0) {
                        throw new ArsdkCommand.RejectedEventException(
                                "Invalid media counts [videoMedias: " + videoMediaCount + ", photoMedias: "
                                + photoMediaCount + ", videoResources: " + videoResourceCount + ", photoResources: "
                                + photoResourceCount + "]");
                    }

                    mMediaStore.updatePhotoMediaCount(photoMediaCount)
                               .updateVideoMediaCount(videoMediaCount)
                               .updatePhotoResourceCount(photoResourceCount)
                               .updateVideoResourceCount(videoResourceCount)
                               .notifyUpdated();
                }
            };

    /** Listens to HTTP media notifications. */
    private final HttpMediaClient.Listener mListener = new HttpMediaClient.Listener() {

        @Override
        public void onMediaAdded(@NonNull HttpMediaItem media) {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "Media added: " + media.getId());
            }
            clearCachedMediaList();
        }

        @Override
        public void onMediaRemoved(@NonNull String mediaId) {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "Media removed: " + mediaId);
            }
            clearCachedMediaList();
        }

        @Override
        public void onAllMediaRemoved() {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "All media removed");
            }
            clearCachedMediaList();
        }

        @Override
        public void onResourceAdded(@NonNull HttpMediaItem.Resource resource) {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "Resource added: " + resource.getId());
            }
            clearCachedMediaList();
        }

        @Override
        public void onResourceRemoved(@NonNull String resourceId) {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "Resource removed: " + resourceId);
            }
            clearCachedMediaList();
        }

        @Override
        public void onIndexingStateChanged(@NonNull HttpMediaIndexingState state) {
            if (ULog.d(TAG_MEDIA)) {
                ULog.d(TAG_MEDIA, "Indexing state changed: " + state);
            }
            if (state == HttpMediaIndexingState.INDEXED) {
                clearCachedMediaList();
            }
        }
    };

    /**
     * Clears cached media list and notifies store content change.
     */
    private void clearCachedMediaList() {
        mCachedMediaList = null;
        mMediaStore.notifyObservers();
    }

    /** Backend of MediaStoreCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaStoreCore.Backend mBackend = new MediaStoreCore.Backend() {

        /** {@code true} when content changes are being watched; media browse results are cached in this case. */
        private boolean mWatching;

        @Override
        public void startWatchingContentChange() {
            if (mMediaClient != null) {
                mMediaClient.setListener(mListener);
                mWatching = true;
            }
        }

        @Override
        public void stopWatchingContentChange() {
            if (mMediaClient != null) {
                mMediaClient.setListener(null);
                mWatching = false;
                mCachedMediaList = null;
            }
        }

        @Nullable
        @Override
        public MediaRequest browse(@Nullable MediaStore.StorageType storageType,
                                   @NonNull MediaRequest.ResultCallback<List<? extends MediaItemCore>> callback) {
            MediaRequest request = null;
            if (mCachedMediaList != null) {
                callback.onRequestComplete(MediaRequest.Status.SUCCESS, mCachedMediaList);
            } else if (mMediaClient == null) {
                callback.onRequestComplete(MediaRequest.Status.FAILED, null);
            } else {
                HttpRequest.ResultCallback<List<HttpMediaItem>> browseCallback = (status, code, result) -> {
                    switch (status) {
                        case SUCCESS:
                            assert result != null;
                            List<MediaItemImpl> list = MediaItemImpl.from(result);
                            if (mWatching) {
                                mCachedMediaList = list;
                            }
                            callback.onRequestComplete(MediaRequest.Status.SUCCESS, list);
                            break;
                        case FAILED:
                            callback.onRequestComplete(MediaRequest.Status.FAILED, null);
                            break;
                        case CANCELED:
                            callback.onRequestComplete(MediaRequest.Status.CANCELED, null);
                            break;
                    }
                };
                request = mMediaClient.browse(storageType, browseCallback)::cancel;
            }
            return request;
        }

        @Nullable
        @Override
        public MediaRequest download(@NonNull MediaResourceCore resource, @NonNull String destDir,
                                     @NonNull MediaRequest.ProgressResultCallback<File> callback) {
            if (mMediaClient == null) {
                callback.onRequestComplete(MediaRequest.Status.FAILED, null);
                return null;
            }

            File dest = new File(destDir, resource.getUid());
            return mMediaClient.download(MediaResourceImpl.unwrap(resource).getDownloadUrl(), dest,
                    new HttpRequest.ProgressStatusCallback() {

                        @Override
                        public void onRequestProgress(int progress) {
                            callback.onRequestProgress(progress);
                        }

                        @Override
                        public void onRequestComplete(@NonNull HttpRequest.Status status, int code) {
                            switch (status) {
                                case SUCCESS:
                                    callback.onRequestComplete(MediaRequest.Status.SUCCESS, dest);
                                    break;
                                case FAILED:
                                    callback.onRequestComplete(code == HttpRequest.STATUS_CODE_SERVER_ERROR ?
                                            MediaRequest.Status.ABORTED : MediaRequest.Status.FAILED, null);
                                    break;
                                case CANCELED:
                                    callback.onRequestComplete(MediaRequest.Status.CANCELED, null);
                                    break;
                            }
                        }
                    })::cancel;
        }

        @Nullable
        @Override
        public MediaRequest fetchThumbnail(@NonNull MediaItemCore media,
                                           @NonNull MediaRequest.ResultCallback<Bitmap> callback) {
            String url;
            if (mMediaClient == null || (url = MediaItemImpl.unwrap(media).getThumbnailUrl()) == null) {
                callback.onRequestComplete(MediaRequest.Status.SUCCESS, null);
                return null;
            }
            return fetchThumbnail(url, media.getUid(), callback);
        }

        @Nullable
        @Override
        public MediaRequest fetchThumbnail(@NonNull MediaResourceCore resource,
                                           @NonNull MediaRequest.ResultCallback<Bitmap> callback) {
            String url;
            if (mMediaClient == null || (url = MediaResourceImpl.unwrap(resource).getThumbnailUrl()) == null) {
                callback.onRequestComplete(MediaRequest.Status.SUCCESS, null);
                return null;
            }
            return fetchThumbnail(url, resource.getUid(), callback);
        }

        @Nullable
        @Override
        public MediaRequest delete(@NonNull MediaItemCore media, @NonNull MediaRequest.StatusCallback callback) {
            if (mMediaClient == null) {
                callback.onRequestComplete(MediaRequest.Status.FAILED);
                return null;
            }
            return mMediaClient.deleteMedia(media.getUid(), (status, code) -> {
                switch (status) {
                    case SUCCESS:
                        callback.onRequestComplete(MediaRequest.Status.SUCCESS);
                        break;
                    case FAILED:
                        callback.onRequestComplete(code == HttpRequest.STATUS_CODE_SERVER_ERROR ?
                                MediaRequest.Status.ABORTED : MediaRequest.Status.FAILED);
                        break;
                    case CANCELED:
                        callback.onRequestComplete(MediaRequest.Status.CANCELED);
                        break;
                }
            })::cancel;
        }

        @Nullable
        @Override
        public MediaRequest delete(@NonNull MediaResourceCore resource, @NonNull MediaRequest.StatusCallback callback) {
            if (mMediaClient == null) {
                callback.onRequestComplete(MediaRequest.Status.FAILED);
                return null;
            }
            return mMediaClient.deleteResource(resource.getUid(), (status, code) -> {
                switch (status) {
                    case SUCCESS:
                        callback.onRequestComplete(MediaRequest.Status.SUCCESS);
                        break;
                    case FAILED:
                        callback.onRequestComplete(code == HttpRequest.STATUS_CODE_SERVER_ERROR ?
                                MediaRequest.Status.ABORTED : MediaRequest.Status.FAILED);
                        break;
                    case CANCELED:
                        callback.onRequestComplete(MediaRequest.Status.CANCELED);
                        break;
                }
            })::cancel;
        }

        @Nullable
        @Override
        public MediaRequest wipe(@NonNull MediaRequest.StatusCallback callback) {
            if (mMediaClient == null) {
                callback.onRequestComplete(MediaRequest.Status.FAILED);
                return null;
            }
            return mMediaClient.deleteAll((status, code) -> {
                switch (status) {
                    case SUCCESS:
                        callback.onRequestComplete(MediaRequest.Status.SUCCESS);
                        break;
                    case FAILED:
                        callback.onRequestComplete(code == HttpRequest.STATUS_CODE_SERVER_ERROR ?
                                MediaRequest.Status.ABORTED : MediaRequest.Status.FAILED);
                        break;
                    case CANCELED:
                        callback.onRequestComplete(MediaRequest.Status.CANCELED);
                        break;
                }
            })::cancel;
        }

        /**
         * Fetches the thumbnail at the given url.
         * <p>
         * {@code callback} is always called, either after success or failure. <br/>
         * This method returns a {@code MediaRequest} object, which can be used to cancel the request.
         * <p>
         * <strong>Note:</strong> caller should ensure that {@link AnafiMediaStore#mMediaClient} is not null before
         * calling this method.
         *
         * @param url      url of the thumbnail to download
         * @param itemUid  identifier of the thumbnail provider, used for failure logs
         * @param callback callback notified of request result
         *
         * @return a request that can be canceled
         */
        @NonNull
        private MediaRequest fetchThumbnail(@NonNull String url, @NonNull String itemUid,
                                            @NonNull MediaRequest.ResultCallback<Bitmap> callback) {
            assert mMediaClient != null;
            return mMediaClient.fetch(url, (data) -> {
                Bitmap thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (thumbnail == null && ULog.w(TAG_MEDIA)) {
                    ULog.w(TAG_MEDIA, "Failed to decode thumbnail [item:" + itemUid + "]");
                }
                return thumbnail;
            }, (status, code, thumbnail) -> {
                switch (status) {
                    case SUCCESS:
                        callback.onRequestComplete(MediaRequest.Status.SUCCESS, thumbnail);
                        break;
                    case FAILED:
                        callback.onRequestComplete(code < HttpRequest.STATUS_CODE_SERVER_ERROR ?
                                MediaRequest.Status.SUCCESS : MediaRequest.Status.FAILED, null);
                        break;
                    case CANCELED:
                        callback.onRequestComplete(MediaRequest.Status.CANCELED, null);
                        break;
                }
            })::cancel;
        }
    };
}
