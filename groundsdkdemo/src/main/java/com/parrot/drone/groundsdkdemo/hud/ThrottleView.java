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

package com.parrot.drone.groundsdkdemo.hud;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.parrot.drone.groundsdkdemo.R;

public class ThrottleView extends View {

    interface OnThrottleChangeListener {

        void onThrottleChange(ThrottleView view, int value, boolean fromUser);
    }

    private OnThrottleChangeListener mListener;

    private float mLineHalfThickness;

    private Paint mStrokePaint;

    private Paint mFillPaint;

    private RectF mRect;

    private PointF mThrottleCenter;

    private float mCornerRadius;

    private float mThrottleRadius;

    private float mTopThrottleY;

    private float mThrottleHeight;

    private int mThrottle;

    public ThrottleView(Context context) {
        super(context);
        init();
    }

    public ThrottleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThrottleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setOnThrottleChangeListener(OnThrottleChangeListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // compute view rectangle, accounting for user padding
        mRect.top = getPaddingTop();
        mRect.bottom = getMeasuredHeight() - mRect.top - getPaddingBottom();
        mRect.left = getPaddingStart();
        mRect.right = getMeasuredWidth() - mRect.left - getPaddingEnd();
        mRect.inset(mLineHalfThickness, mLineHalfThickness);

        mCornerRadius = mRect.width() * .5f;
        mThrottleCenter.x = mRect.centerX();
        mTopThrottleY = mRect.top + mCornerRadius;
        mThrottleHeight = mRect.height() - mCornerRadius * 2;
        mThrottleRadius = (mRect.width() - mLineHalfThickness * 6) * .5f;
        computeThrottlePosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mStrokePaint);
        canvas.drawCircle(mThrottleCenter.x, mThrottleCenter.y, mThrottleRadius, mFillPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // convert to throttle range
                updateThrottle((int) ((mTopThrottleY - event.getY()) * 200 / (mThrottleHeight) + 100));
                return true;
            default:
                return false;
        }
    }

    private void init() {
        Context context = getContext();

        mLineHalfThickness = getResources().getDimensionPixelSize(R.dimen.throttle_line_half_thickness);

        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(ContextCompat.getColor(context, R.color.color_primary_dark));
        mStrokePaint.setStrokeWidth(mLineHalfThickness * 2);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(ContextCompat.getColor(context, R.color.color_primary));
        mFillPaint.setAntiAlias(true);

        mRect = new RectF();

        mThrottleCenter = new PointF();
    }

    private void updateThrottle(int throttle) {
        throttle = Math.max(Math.min(throttle, 100), -100);
        if (mThrottle != throttle) {
            mThrottle = throttle;
            mListener.onThrottleChange(this, mThrottle, true);
            computeThrottlePosition();
            invalidate();
        }
    }

    private void computeThrottlePosition() {
        mThrottleCenter.y = mTopThrottleY - (mThrottle - 100) * mThrottleHeight / 200f;
    }
}