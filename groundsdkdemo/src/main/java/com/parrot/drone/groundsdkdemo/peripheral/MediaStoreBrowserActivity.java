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

package com.parrot.drone.groundsdkdemo.peripheral;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.MultiChoiceSettingView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class MediaStoreBrowserActivity extends GroundSdkActivityBase
        implements DownloadMediaDialogFragment.Listener, DeleteMediaDialogFragment.Listener,
        DeleteAllMediaDialogFragment.Listener, MediaResourcesDialogFragment.Listener {

    enum StorageType {
        REMOVABLE,
        INTERNAL,
        ALL
    }

    private Button mDeleteButton;

    private Button mDeleteAllButton;

    private Button mDownloadButton;

    private ProgressBar mTaskRunningIndicator;

    private DialogFragment mCurrentTaskDialog;

    private MediaResourcesDialogFragment mResourcesDialogFragment;

    private MediaItem mObservedMediaItem;

    private Adapter mAdapter;

    private Handler mHandler;

    private Drone mDrone;

    private MediaStore mMediaStore;

    private List<MediaItem> mMedias;

    private Set<MediaItem> mSelectedMedias;

    private Ref<?> mCurrentTaskRef;

    private Ref<List<MediaItem>> mCurrentBrowseTaskRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_media_browser);
        mHandler = new Handler();

        mMedias = Collections.emptyList();
        mSelectedMedias = new LinkedHashSet<>();

        mTaskRunningIndicator = findViewById(R.id.task_running);

        MultiChoiceSettingView<StorageType> mTypeView = findViewById(R.id.storage_type_filter);
        mTypeView.setChoices(EnumSet.allOf(StorageType.class));
        mTypeView.setSelection(StorageType.ALL);
        mTypeView.setListener(this::browseOn);

        mDeleteButton = findViewById(R.id.btn_delete);
        mDeleteButton.setOnClickListener(mButtonClickListener);

        mDeleteAllButton = findViewById(R.id.btn_delete_all);
        mDeleteAllButton.setOnClickListener(mButtonClickListener);

        mDownloadButton = findViewById(R.id.btn_download);
        mDownloadButton.setOnClickListener(mButtonClickListener);

        mAdapter = new Adapter();
        RecyclerView recyclerView = findViewById(android.R.id.list);
        recyclerView.setAdapter(mAdapter);

        mDrone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (mDrone == null) {
            finish();
            return;
        }

        mMediaStore = mDrone.getPeripheral(MediaStore.class, mediaStore -> {
            if (mediaStore == null) {
                finish();
            }
        }).get();

        if (mMediaStore == null) {
            finish();
            return;
        }

        browseOn(StorageType.ALL);
    }

    private void browseOn(StorageType storageType) {
        if (mMediaStore == null) {
            finish();
            return;
        }
        if (mCurrentBrowseTaskRef != null) {
            mCurrentBrowseTaskRef.close();
        }
        Ref.Observer<List<MediaItem>> callback = mediaList -> {
            mMedias = mediaList;
            computeSelectedMedias();
            mAdapter.notifyDataSetChanged();

            if (mResourcesDialogFragment != null) {
                int index = mMedias.indexOf(mObservedMediaItem);
                if (index == -1) {
                    mResourcesDialogFragment.dismiss();
                } else {
                    mObservedMediaItem = mMedias.get(index);
                    mResourcesDialogFragment.update(mDrone, mMediaStore, mObservedMediaItem);
                }
            }
        };

        switch (storageType) {
            case REMOVABLE:
                mCurrentBrowseTaskRef = mMediaStore.browse(MediaStore.StorageType.REMOVABLE, callback);
                break;
            case INTERNAL:
                mCurrentBrowseTaskRef = mMediaStore.browse(MediaStore.StorageType.INTERNAL, callback);
                break;
            case ALL:
                mCurrentBrowseTaskRef = mMediaStore.browse(callback);
                break;
        }
    }

    private void computeSelectedMedias() {
        mSelectedMedias.retainAll(mMedias);
        updateButtons();
    }

    private void toggleItemSelection(int itemAdapterPosition) {
        MediaItem item = mAdapter.getItem(itemAdapterPosition);
        if (mSelectedMedias.contains(item)) {
            mSelectedMedias.remove(item);
        } else {
            mSelectedMedias.add(item);
        }
        mAdapter.notifyItemChanged(itemAdapterPosition);
        updateButtons();
    }

    private final View.OnClickListener mButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View button) {
            Ref<?> taskRef = null;
            if (button == mDeleteButton) {
                DeleteMediaDialogFragment deleteDialog = new DeleteMediaDialogFragment();
                mCurrentTaskDialog = deleteDialog;
                taskRef = mMediaStore.delete(
                        mSelectedMedias.stream()
                                .flatMap(it -> it.getResources().stream())
                                .collect(Collectors.toList()),
                        deleter -> {
                            if (deleter != null) {
                                deleteDialog.update(deleter);
                                MediaTaskStatus status = deleter.getStatus();
                                if (status != MediaTaskStatus.RUNNING) {
                                    if (status == MediaTaskStatus.ERROR) {
                                        Toast.makeText(MediaStoreBrowserActivity.this,
                                                R.string.media_delete_failed, Toast.LENGTH_SHORT).show();
                                    }
                                    mHandler.postDelayed(mDismissDialogRunnable, 1000);
                                    updateCurrentTaskRef(null);
                                }
                            }
                        });
            } else if (button == mDeleteAllButton) {
                mCurrentTaskDialog = new DeleteAllMediaDialogFragment();
                taskRef = mMediaStore.wipe(deleter -> {
                    if (deleter != null) {
                        MediaTaskStatus status = deleter.getStatus();
                        if (status != MediaTaskStatus.RUNNING) {
                            if (status == MediaTaskStatus.ERROR) {
                                Toast.makeText(MediaStoreBrowserActivity.this,
                                        R.string.media_delete_all_failed, Toast.LENGTH_SHORT).show();
                            }
                            mHandler.postDelayed(mDismissDialogRunnable, 1000);
                            updateCurrentTaskRef(null);
                        }
                    }
                });
            } else if (button == mDownloadButton) {
                DownloadMediaDialogFragment downloadDialog = new DownloadMediaDialogFragment();
                mCurrentTaskDialog = downloadDialog;
                taskRef = mMediaStore.download(
                        mSelectedMedias.stream()
                                .flatMap(it -> it.getResources().stream())
                                .filter(it -> it.getFormat() != MediaItem.Resource.Format.DNG)
                                .collect(Collectors.toList()),
                        MediaDestination.platformMediaStore("groundSdkDemo"),
                        downloader -> {
                            if (downloader != null) {
                                downloadDialog.update(downloader);
                                MediaTaskStatus status = downloader.getStatus();
                                switch (status) {
                                    case ERROR:
                                        Toast.makeText(MediaStoreBrowserActivity.this,
                                                R.string.media_download_failed, Toast.LENGTH_SHORT).show();
                                        // fall-through
                                    case COMPLETE:
                                        mHandler.postDelayed(mDismissDialogRunnable, 500);
                                        updateCurrentTaskRef(null);
                                        break;
                                    case RUNNING:
                                    case FILE_PROCESSED:
                                        break;
                                }
                            }
                        });
            }
            if (mCurrentTaskDialog != null) {
                mHandler.removeCallbacks(mDismissDialogRunnable);
                mCurrentTaskDialog.show(getSupportFragmentManager(), null);
            }
            updateCurrentTaskRef(taskRef);
        }
    };

    private final Runnable mDismissDialogRunnable = new Runnable() {

        @Override
        public void run() {
            if (mCurrentTaskDialog != null) {
                mCurrentTaskDialog.dismiss();
            }
        }
    };

    private void updateCurrentTaskRef(@Nullable Ref<?> taskRef) {
        mCurrentTaskRef = taskRef;
        updateButtons();
    }

    private void updateButtons() {
        mDeleteButton.setEnabled(mCurrentTaskRef == null && !mSelectedMedias.isEmpty());
        mDeleteAllButton.setEnabled(!mMedias.isEmpty());
        mDownloadButton.setEnabled(mCurrentTaskRef == null && !mSelectedMedias.isEmpty());
        mTaskRunningIndicator.setVisibility(
                mCurrentTaskDialog == null && mCurrentTaskRef != null ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTaskCanceled() {
        if (mCurrentTaskRef != null) {
            mCurrentTaskRef.close();
            updateCurrentTaskRef(null);
            if (mCurrentTaskDialog != null) {
                mCurrentTaskDialog.dismiss();
            }
        }
    }

    @Override
    public void onDialogDismissed() {
        mHandler.removeCallbacks(mDismissDialogRunnable);
        mCurrentTaskDialog = null;
        updateButtons();
    }

    @Override
    public void onResourcesDialogDismissed() {
        mResourcesDialogFragment = null;
        mObservedMediaItem = null;
    }

    private final class MediaItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final ImageView mThumbnail;

        @NonNull
        private final ProgressBar mLoadingIndicator;

        @NonNull
        private final TextView mCreationDate;

        @NonNull
        private final TextView mRunId;

        @NonNull
        private final TextView mTypeAndFormats;

        @NonNull
        private final ImageView mCheckMark;

        @Nullable
        private Ref<Bitmap> mThumbnailRequest;

        @Nullable
        private MediaItem mMediaItem;

        MediaItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item, parent, false));
            mThumbnail = itemView.findViewById(R.id.thumbnail);
            mLoadingIndicator = itemView.findViewById(R.id.loading);
            mCreationDate = itemView.findViewById(R.id.creation_date);
            mRunId = itemView.findViewById(R.id.runid);
            mTypeAndFormats = itemView.findViewById(R.id.type_and_formats);
            mCheckMark = itemView.findViewById(R.id.checkmark);
            itemView.setOnClickListener(v -> toggleItemSelection(getAdapterPosition()));
            itemView.setOnLongClickListener(v -> {
                if (mMediaItem != null) {
                    mObservedMediaItem = mMediaItem;
                    mResourcesDialogFragment = new MediaResourcesDialogFragment();
                    mResourcesDialogFragment.update(mDrone, mMediaStore, mMediaItem);
                    mResourcesDialogFragment.show(getSupportFragmentManager(), null);
                    return true;
                }
                return false;
            });
        }

        void bind(@NonNull MediaItem item) {
            mMediaItem = item;
            Context context = itemView.getContext();
            mCreationDate.setText(item.getCreationDate().toString());
            mRunId.setText(getString(R.string.media_runid_format, item.getRunUid()));

            String typeAndMode;
            if (item.getPhotoMode() == null) {
                typeAndMode = item.getType().toString();
            } else {
                String photoMode;
                if (item.getPanoramaType() == null) {
                    photoMode = item.getPhotoMode().toString();
                } else {
                    photoMode = getString(R.string.media_photo_panorama_type_format, item.getPhotoMode(),
                            item.getPanoramaType());
                }
                typeAndMode = getString(R.string.media_photo_mode_format, item.getType(), photoMode);
            }
            List<String> resources = new ArrayList<>();
            for (MediaItem.Resource resource : item.getResources()) {
                resources.add(getString(R.string.media_resource_format, resource.getFormat(),
                        Formatter.formatShortFileSize(context, resource.getSize())));
            }
            mTypeAndFormats.setText(
                    getString(R.string.media_type_format, typeAndMode, TextUtils.join(", ", resources)));


            mLoadingIndicator.setVisibility(View.VISIBLE);
            mThumbnail.setVisibility(View.INVISIBLE);
            if (mThumbnailRequest != null) {
                mThumbnailRequest.close();
            }
            mThumbnailRequest = mMediaStore.fetchThumbnailOf(item, thumbnail -> {
                if (thumbnail != null) {
                    mThumbnail.setImageBitmap(thumbnail);
                } else {
                    switch (item.getType()) {
                        case PHOTO:
                            mThumbnail.setImageResource(R.drawable.ic_photo);
                            break;
                        case VIDEO:
                            mThumbnail.setImageResource(R.drawable.ic_video);
                    }
                }
                mLoadingIndicator.setVisibility(View.INVISIBLE);
                mThumbnail.setVisibility(View.VISIBLE);
            });

            mCheckMark.setVisibility(mSelectedMedias.contains(item) ? View.VISIBLE : View.GONE);
        }
    }

    private class Adapter extends RecyclerView.Adapter<MediaItemHolder> {

        @NonNull
        @Override
        public MediaItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MediaItemHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull MediaItemHolder holder, int position) {
            holder.bind(mMedias.get(position));
        }

        @NonNull
        MediaItem getItem(int position) {
            return mMedias.get(position);
        }

        @Override
        public int getItemCount() {
            return mMedias.size();
        }
    }
}
