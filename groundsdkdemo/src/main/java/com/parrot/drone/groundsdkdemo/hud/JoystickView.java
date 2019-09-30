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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.parrot.drone.groundsdkdemo.R;

public class JoystickView extends LinearLayout {

    public interface JoystickListener {

        void onValuesUpdated(JoystickView joystick, float percentX, float percentY);
    }

    private JoystickListener mListener;

    private Paint mStrokePaint;

    private Paint mFillPaint;

    private PointF mCenter;

    private PointF mJoystick;

    private float mRadius;

    private float mJoystickRadius;

    private float mJoystickCenterRadius;    // mRadius - mJoystickRadius. only here to avoid calculation

    public JoystickView(Context context) {
        super(context);
        customInit();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        customInit();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customInit();
    }

    private void customInit() {
        Context context = getContext();

        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(ContextCompat.getColor(context, R.color.color_primary_dark));
        mStrokePaint.setStrokeWidth(10);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(ContextCompat.getColor(context, R.color.color_primary));
        mFillPaint.setAntiAlias(true);

        mCenter = new PointF(0, 0);
        mJoystick = new PointF(0, 0);
        mRadius = 0;

        setWillNotDraw(false);
    }

    private void setJoystickPosition(float x, float y) {
        mJoystick.x = x;
        mJoystick.y = y;
        invalidate();
        notifyValuesUpdated();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mCenter.x = getMeasuredWidth() / 2.f;
        mCenter.y = getMeasuredHeight() / 2.f;
        mJoystick.x = mCenter.x;
        mJoystick.y = mCenter.y;
        mRadius = Math.min(getMeasuredWidth() - getPaddingStart() - getPaddingEnd(),
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom()) / 2.0f;
        mJoystickRadius = 0.2f * mRadius;
        mJoystickCenterRadius = mRadius - mJoystickRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mJoystick.x, mJoystick.y, mJoystickRadius, mFillPaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mStrokePaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isPointInsideCircle(event.getX(), event.getY())) {
                    setJoystickPosition(event.getX(), event.getY());
                } else {
                    // if pointed down outside the circle, don't do anything
                    result = false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (isPointInsideCircle(event.getX(), event.getY())) {
                    setJoystickPosition(event.getX(), event.getY());
                } else {
                    // if pointed down outside the circle, put the point on the edge of the circle
                    float vX = event.getX() - mCenter.x;
                    float vY = event.getY() - mCenter.y;
                    float magV = (float) Math.sqrt(vX * vX + vY * vY);
                    setJoystickPosition(
                            mCenter.x + vX / magV * mJoystickCenterRadius,
                            mCenter.y + vY / magV * mJoystickCenterRadius);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // put the joystick back on the center
                setJoystickPosition(mCenter.x, mCenter.y);
                break;
            default:
                result = false;
        }

        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean shouldDisableParentScroll = isPointInsideCircle(event.getX(), event.getY());

        getParent().requestDisallowInterceptTouchEvent(shouldDisableParentScroll);
        return super.onInterceptTouchEvent(event);
    }

    private boolean isPointInsideCircle(float x, float y) {
        return (Math.pow(x - mCenter.x, 2) + Math.pow(y - mCenter.y, 2) < Math.pow(mJoystickCenterRadius, 2));
    }

    public void setListener(JoystickListener listener) {
        mListener = listener;
    }

    private void notifyValuesUpdated() {
        if (mListener == null) {
            return;
        }
        mListener.onValuesUpdated(this, (mJoystick.x - mCenter.x) / mJoystickCenterRadius,
                -(mJoystick.y - mCenter.y) / mJoystickCenterRadius);
    }
}