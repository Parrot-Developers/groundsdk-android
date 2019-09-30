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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdkdemo.R;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MediaResourcesDialogFragment extends DialogFragment
        implements DownloadMediaDialogFragment.Listener, DeleteMediaDialogFragment.Listener {

    interface Listener {

        void onResourcesDialogDismissed();
    }

    private Listener mListener;

    private Button mDeleteButton;

    private Button mDownloadButton;

    private ProgressBar mTaskRunningIndicator;

    private DialogFragment mCurrentTaskDialog;

    private Adapter mAdapter;

    private final Handler mHandler;

    @Nullable
    private MediaStore mMediaStore;

    private MediaItem.Type mType;

    private Drone mDrone;

    @NonNull
    private List<? extends MediaItem.Resource> mResources;

    private final Set<MediaItem.Resource> mSelectedResources;

    private Ref<?> mCurrentTaskRef;

    public MediaResourcesDialogFragment() {
        mHandler = new Handler();
        mResources = Collections.emptyList();
        mSelectedResources = new LinkedHashSet<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

        @SuppressLint("InflateParams")
        View content = activity.getLayoutInflater().inflate(R.layout.activity_media_resources, null);

        mTaskRunningIndicator = content.findViewById(R.id.task_running);

        mDeleteButton = content.findViewById(R.id.btn_delete);
        mDeleteButton.setOnClickListener(mButtonClickListener);

        mDownloadButton = content.findViewById(R.id.btn_download);
        mDownloadButton.setOnClickListener(mButtonClickListener);

        updateButtons();

        mAdapter = new Adapter();
        RecyclerView recyclerView = content.findViewById(android.R.id.list);
        recyclerView.setAdapter(mAdapter);

        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_dialog_media_resources)
                .setView(content)
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (Listener) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaStore != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mListener.onResourcesDialogDismissed();
        super.onDismiss(dialog);
    }

    final void update(@NonNull Drone drone, @NonNull MediaStore mediaStore, @NonNull MediaItem item) {
        mDrone = drone;
        mMediaStore = mediaStore;
        mType = item.getType();
        mResources = item.getResources();
        mSelectedResources.retainAll(mResources);
        if (isResumed()) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void toggleItemSelection(int itemAdapterPosition) {
        MediaItem.Resource resource = mResources.get(itemAdapterPosition);
        boolean selected = mSelectedResources.contains(resource);
        if (selected) {
            mSelectedResources.remove(resource);
        } else {
            mSelectedResources.add(resource);
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
                taskRef = mMediaStore.delete(mSelectedResources, deleter -> {
                    if (deleter != null) {
                        deleteDialog.update(deleter);
                        MediaTaskStatus status = deleter.getStatus();
                        if (status != MediaTaskStatus.RUNNING) {
                            if (status == MediaTaskStatus.ERROR && isAdded()) {
                                Toast.makeText(getActivity(), R.string.media_delete_failed, Toast.LENGTH_SHORT).show();
                            }
                            mHandler.postDelayed(mDismissDialogRunnable, 1000);
                            updateCurrentTaskRef(null);
                        }
                    }
                });
            } else if (button == mDownloadButton) {
                DownloadMediaDialogFragment downloadDialog = new DownloadMediaDialogFragment();
                mCurrentTaskDialog = downloadDialog;
                taskRef = mMediaStore.download(mSelectedResources,
                        MediaDestination.platformMediaStore("groundSdkDemo"), downloader -> {
                            if (downloader != null) {
                                downloadDialog.update(downloader);
                                MediaTaskStatus status = downloader.getStatus();
                                if (status != MediaTaskStatus.RUNNING) {
                                    if (status == MediaTaskStatus.ERROR && isAdded()) {
                                        Toast.makeText(getActivity(), R.string.media_download_failed,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    mHandler.postDelayed(mDismissDialogRunnable, 500);
                                    updateCurrentTaskRef(null);
                                }
                            }
                        });
            }
            if (mCurrentTaskDialog != null) {
                mHandler.removeCallbacks(mDismissDialogRunnable);
                mCurrentTaskDialog.setTargetFragment(MediaResourcesDialogFragment.this, 0);
                assert getFragmentManager() != null;
                mCurrentTaskDialog.show(getFragmentManager(), null);
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
        mDeleteButton.setEnabled(mCurrentTaskRef == null && !mSelectedResources.isEmpty());
        mDownloadButton.setEnabled(mCurrentTaskRef == null && !mSelectedResources.isEmpty());
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

    private final class MediaItemHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final ImageView mThumbnail;

        @NonNull
        private final ProgressBar mLoadingIndicator;

        @NonNull
        private final TextView mFormat;

        @NonNull
        private final TextView mSize;

        @NonNull
        private final TextView mDuration;

        @NonNull
        private final ImageView mCheckMark;

        @NonNull
        private final ImageButton mReplayBtn;

        @Nullable
        private Ref<Bitmap> mThumbnailRequest;

        MediaItemHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_resource, parent, false));
            mThumbnail = itemView.findViewById(R.id.thumbnail);
            mLoadingIndicator = itemView.findViewById(R.id.loading);
            mFormat = itemView.findViewById(R.id.format);
            mSize = itemView.findViewById(R.id.size);
            mDuration = itemView.findViewById(R.id.duration);
            mCheckMark = itemView.findViewById(R.id.checkmark);
            mReplayBtn = itemView.findViewById(R.id.replay);
            itemView.setOnClickListener(v -> toggleItemSelection(getAdapterPosition()));
        }

        void bind(@NonNull MediaItem.Resource resource) {
            Context context = itemView.getContext();
            mFormat.setText(getString(R.string.media_resources_format, resource.getFormat()));
            mSize.setText(getString(R.string.media_resources_size,
                    Formatter.formatShortFileSize(context, resource.getSize())));
            if (mType == MediaItem.Type.PHOTO) {
                mDuration.setVisibility(View.GONE);
            } else {
                mDuration.setVisibility(View.VISIBLE);
                mDuration.setText(getString(R.string.media_resources_duration,
                        DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(resource.getDuration()))));
            }

            mLoadingIndicator.setVisibility(View.VISIBLE);
            mThumbnail.setVisibility(View.INVISIBLE);
            if (mThumbnailRequest != null) {
                mThumbnailRequest.close();
            }
            if (mMediaStore != null) {
                mThumbnailRequest = mMediaStore.fetchThumbnailOf(resource, thumbnail -> {
                    if (thumbnail != null) {
                        mThumbnail.setImageBitmap(thumbnail);
                    } else {
                        switch (resource.getFormat()) {
                            case JPG:
                            case DNG:
                                mThumbnail.setImageResource(R.drawable.ic_photo);
                                break;
                            case MP4:
                                mThumbnail.setImageResource(R.drawable.ic_video);
                        }
                    }
                    mLoadingIndicator.setVisibility(View.INVISIBLE);
                    mThumbnail.setVisibility(View.VISIBLE);
                });
            }

            mCheckMark.setVisibility(mSelectedResources.contains(resource) ? View.VISIBLE : View.GONE);

            if (resource.getAvailableTracks().isEmpty()) {
                mReplayBtn.setVisibility(View.GONE);
            } else {
                mReplayBtn.setVisibility(View.VISIBLE);
                mReplayBtn.setOnClickListener(btn -> MediaReplayActivity.launch(context, mDrone,
                        MediaReplay.videoTrackOf(resource, MediaItem.Track.DEFAULT_VIDEO)));
            }
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
            holder.bind(mResources.get(position));
        }

        @Override
        public int getItemCount() {
            return mResources.size();
        }
    }
}
