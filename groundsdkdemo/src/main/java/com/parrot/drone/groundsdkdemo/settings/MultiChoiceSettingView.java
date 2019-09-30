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

package com.parrot.drone.groundsdkdemo.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MultiChoiceSettingView<T> extends LinearLayout {

    @NonNull
    private final List<T> mChoices;

    @Nullable
    private T mSelection;

    private boolean mUpdating;

    public interface OnItemChosenListener<T> {

        void onItemChosen(@NonNull T chosenItem);
    }

    private OnItemChosenListener<T> mListener;

    private final Spinner mChoiceView;

    private final ProgressBar mUpdatingView;

    public MultiChoiceSettingView(Context context) {
        this(context, null);
    }

    public MultiChoiceSettingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiChoiceSettingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(true);
        setSaveFromParentEnabled(false);

        LayoutInflater.from(context).inflate(R.layout.view_multichoice_setting, this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiChoiceSettingView, defStyleAttr, 0);
        try {
            ((TextView) findViewById(android.R.id.title)).setText(
                    a.getString(R.styleable.MultiChoiceSettingView_title));
        } finally {
            a.recycle();
        }

        mChoices = new ArrayList<>();

        mChoiceView = findViewById(android.R.id.edit);
        mUpdatingView = findViewById(android.R.id.progress);

        mChoiceView.setAdapter(mAdapter);
        mChoiceView.setEmptyView(findViewById(android.R.id.empty));
        mChoiceView.setOnItemSelectedListener(mItemSelectedListener);
    }

    @NonNull
    public MultiChoiceSettingView<T> setUpdating(boolean updating) {
        if (mUpdating != updating) {
            mUpdating = updating;
            mUpdatingView.setVisibility(mUpdating ? VISIBLE : GONE);
            updateEnabledState();
        }
        return this;
    }

    @NonNull
    public MultiChoiceSettingView<T> setChoices(@NonNull Set<T> choices) {
        mChoices.clear();
        mChoices.addAll(choices);
        updateSelection();
        updateEnabledState();
        mAdapter.notifyDataSetChanged();
        return this;
    }

    @NonNull
    public MultiChoiceSettingView<T> setSelection(@Nullable T selection) {
        if (mSelection != selection) {
            mSelection = selection;
            updateSelection();
        }
        return this;
    }

    @NonNull
    public MultiChoiceSettingView<T> setListener(@Nullable OnItemChosenListener<T> listener) {
        mListener = listener;
        return this;
    }

    private void updateSelection() {
        mChoiceView.setSelection(mSelection == null ? AdapterView.INVALID_POSITION : mChoices.indexOf(mSelection));
    }

    private void updateEnabledState() {
        mChoiceView.setEnabled(!mUpdating && mChoices.size() > 1);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            T userSelection = mChoices.get(position);
            if (mListener != null && userSelection != mSelection) {
                mSelection = userSelection;
                mListener.onItemChosen(userSelection);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private final BaseAdapter mAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return mChoices.size();
        }

        @Override
        public T getItem(int position) {
            return mChoices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line,
                        parent, false);
            }
            ((TextView) convertView).setText(getItem(position).toString());
            return convertView;
        }
    };

}
