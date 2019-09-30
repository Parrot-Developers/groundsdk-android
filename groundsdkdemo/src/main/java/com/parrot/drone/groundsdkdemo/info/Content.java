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

package com.parrot.drone.groundsdkdemo.info;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

abstract class Content {

    @LayoutRes
    private final int mLayoutId;

    @Nullable
    private ViewAdapter mAdapter;

    Content(@LayoutRes int layoutId) {
        mLayoutId = layoutId;
    }

    final void showContent() {
        assert mAdapter != null;
        mAdapter.onContentAdded(this);
    }

    final void hideContent() {
        assert mAdapter != null;
        mAdapter.onContentRemoved(this);
    }

    final void contentChanged() {
        assert mAdapter != null;
        mAdapter.onContentChanged(this);
    }

    @IntDef({SHOW_CONTENT, HIDE_CONTENT})
    @interface ContentOp {}

    static final int SHOW_CONTENT = 0;

    static final int HIDE_CONTENT = 1;

    @ContentOp
    int onContentRegistered() {
        return SHOW_CONTENT;
    }

    abstract ViewHolder<?> onCreateViewHolder(@NonNull View rootView);

    private boolean registerWith(@NonNull ViewAdapter adapter) {
        mAdapter = adapter;
        return onContentRegistered() == SHOW_CONTENT;
    }

    abstract static class ViewHolder<C extends Content> extends RecyclerView.ViewHolder {

        @NonNull
        final Context mContext;

        @Nullable
        private C mContent;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mContext = itemView.getContext();
        }

        @NonNull
        final <T extends View> T findViewById(@IdRes int id) {
            T view = itemView.findViewById(id);
            if (view == null) {
                throw new IllegalArgumentException("No such view id in layout: " + id);
            }
            return view;
        }

        final void bind(@NonNull C content) {
            mContent = content;
            onBind(mContent);
        }

        abstract void onBind(@NonNull C content);

        abstract class OnClickListener implements View.OnClickListener {

            @Override
            public final void onClick(View v) {
                if (mContent != null) {
                    onClick(v, mContent);
                }
            }

            public abstract void onClick(View v, @NonNull C content);
        }
    }

    static final class ViewAdapter extends RecyclerView.Adapter<Content.ViewHolder> {

        @NonNull
        private final SparseArray<Content> mContentViewFactories;

        @NonNull
        private final List<Content> mContentList;

        @NonNull
        private final SparseArray<Content> mVisibleContent;

        ViewAdapter(@NonNull Content... contents) {
            mContentList = Arrays.asList(contents);
            mVisibleContent = new SparseArray<>(contents.length);
            mContentViewFactories = new SparseArray<>(contents.length);
            for (int i = 0; i < contents.length; i++) {
                Content content = contents[i];
                mContentViewFactories.put(content.mLayoutId, content);
                if (content.registerWith(this)) {
                    mVisibleContent.put(i, content);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return mVisibleContent.valueAt(position).mLayoutId;
        }

        @NonNull
        @Override
        public Content.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return mContentViewFactories.get(viewType).onCreateViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onBindViewHolder(Content.ViewHolder holder, int position) {
            holder.bind(mVisibleContent.valueAt(position));
        }

        @Override
        public int getItemCount() {
            return mVisibleContent.size();
        }

        private void onContentAdded(@NonNull Content content) {
            int key = mContentList.indexOf(content);
            mVisibleContent.put(key, content);
            notifyItemInserted(mVisibleContent.indexOfKey(key));
        }

        private void onContentRemoved(@NonNull Content content) {
            int key = mContentList.indexOf(content);
            int index = mVisibleContent.indexOfKey(key);
            mVisibleContent.remove(key);
            notifyItemRemoved(index);
        }

        private void onContentChanged(@NonNull Content content) {
            notifyItemChanged(mVisibleContent.indexOfKey(mContentList.indexOf(content)));
        }
    }
}
