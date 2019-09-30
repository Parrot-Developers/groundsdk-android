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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdkdemo.R;

public class TextSettingView extends LinearLayout {

    public interface Listener {

        void onTextEntered(@NonNull String text);
    }

    private final EditText mEditText;

    private final ProgressBar mUpdatingView;

    private Listener mListener;

    private boolean mAvailable;

    private boolean mUpdating;

    private String mText;

    public TextSettingView(Context context) {
        this(context, null);
    }

    public TextSettingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextSettingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(true);
        setSaveFromParentEnabled(false);
        // steal the focus from the edit text so that the soft keyboard does not show up at start.
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);

        LayoutInflater.from(context).inflate(R.layout.view_text_setting, this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextSettingView, defStyleAttr, 0);
        try {
            ((TextView) findViewById(android.R.id.title)).setText(a.getString(R.styleable.TextSettingView_title));
        } finally {
            a.recycle();
        }

        mEditText = findViewById(android.R.id.edit);
        mUpdatingView = findViewById(android.R.id.progress);
        mEditText.setOnEditorActionListener(mEditTextListener);
    }


    public TextSettingView setAvailable(boolean available) {
        mAvailable = available;
        updateView();
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TextSettingView setUpdating(boolean updating) {
        mUpdating = updating;
        updateView();
        return this;
    }

    public TextSettingView setText(@Nullable String text) {
        mText = text;
        updateView();
        return this;
    }

    public TextSettingView setListener(@Nullable Listener listener) {
        mListener = listener;
        return this;
    }

    private void updateView() {
        if (!mEditText.isInputMethodTarget()) {
            mEditText.setText(mText);
        }
        mEditText.setEnabled(mAvailable && !mUpdating);
        mEditText.setVisibility(mUpdating ? GONE : VISIBLE);
        mUpdatingView.setVisibility(mUpdating ? VISIBLE : GONE);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final TextView.OnEditorActionListener mEditTextListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mListener != null) {
                    mListener.onTextEntered(textView.getText().toString());
                }
                // since we mess up a bit with the edit text focus, it seems we have to close it by hand now...
                ((InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(textView.getWindowToken(), 0);
                return true;
            }
            return false;
        }
    };
}
