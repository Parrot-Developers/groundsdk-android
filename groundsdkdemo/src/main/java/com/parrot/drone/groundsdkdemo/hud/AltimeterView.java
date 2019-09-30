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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.parrot.drone.groundsdkdemo.R;

public class AltimeterView extends View {

    private static final int PRIMARY_MARK_STEP = 5;

    private Paint mOutlinePaint;

    private Paint mMarkPaint;

    private Paint mAltitudeLinePaint;

    private Paint mSkyPaint;

    private Paint mGroundPaint;

    private TextPaint mTextPaint;

    private RectF mClipRect;

    private Path mClipPath;

    private float mCornersRadius;

    private PointF mTopAltitudePoint;

    private int mTopAltitudeValue;

    private float mHorizonY;

    private int mAltimeterStep;

    private float mTextRightX;

    private float mTextXOffset;

    private float mTextYOffset;

    private float mLineStartX;

    private float mLineEndX;

    private float mAltitudeLineRadius;

    private float mTakeOffAltitude;

    private float mGroundAltitude;

    public AltimeterView(Context context) {
        super(context);
        init();
    }

    public AltimeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AltimeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setTakeOffAltitude(float takeOffAltitude) {
        if (Double.compare(mTakeOffAltitude, takeOffAltitude) != 0) {
            mTakeOffAltitude = takeOffAltitude;
            // compute top altitude value, since current altitude did change
            computeTopAltitude();
            invalidate();
        }
    }

    public void setGroundAltitude(float groundAltitude) {
        if (Double.compare(mGroundAltitude, groundAltitude) != 0) {
            mGroundAltitude = groundAltitude;
            // compute horizon position, since current ground altitude did change
            computeHorizon();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // compute view clipping rectangle, accounting for user padding
        mClipRect.top = getPaddingTop();
        mClipRect.bottom = getMeasuredHeight() - mClipRect.top - getPaddingBottom();
        mClipRect.left = getPaddingStart();
        mClipRect.right = getMeasuredWidth() - mClipRect.left - getPaddingEnd();
        // create a rounded rectangle clipping path for canvas clipping
        mClipPath.reset();
        mClipPath.addRoundRect(mClipRect, mCornersRadius, mCornersRadius, Path.Direction.CW);
        // inset the clipping rect by the size of the border
        mClipRect.inset(mOutlinePaint.getStrokeWidth() / 2.0f, mOutlinePaint.getStrokeWidth() / 2.0f);
        // compute top visible altitude point since height might have changed
        computeTopAltitude();
        // compute horizon position since height might have changed
        computeHorizon();
        // compute X alignment of altitude texts
        mTextRightX = mClipRect.right - mTextXOffset;
        // compute X start and end of altitude marker lines
        mLineStartX = mClipRect.centerX() - mAltitudeLineRadius;
        mLineEndX = mClipRect.centerX() + mAltitudeLineRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // setup canvas clipping
        canvas.clipPath(mClipPath);
        // draw sky
        canvas.drawRect(mClipRect.left, mClipRect.top, mClipRect.right, mHorizonY, mSkyPaint);
        // draw ground
        canvas.drawRect(mClipRect.left, mHorizonY, mClipRect.right, mClipRect.bottom, mGroundPaint);
        // draw current altitude indicator
        float centerY = mClipRect.centerY();
        canvas.drawLine(mClipRect.left, centerY, mClipRect.right, centerY, mAltitudeLinePaint);
        // draw outline
        canvas.drawRoundRect(mClipRect, mCornersRadius, mCornersRadius, mOutlinePaint);
        // draw altitude markers
        float y = mTopAltitudePoint.y;
        int altitude = mTopAltitudeValue;
        while (y < mClipRect.bottom + mAltimeterStep) { // +mAltimeterStep to make it clip nicely from below
            if (altitude % PRIMARY_MARK_STEP == 0) {
                // line + text marker
                canvas.drawLine(mLineStartX, y, mLineEndX, y, mMarkPaint);
                canvas.drawText(Integer.toString(altitude), mTextRightX, y + mTextYOffset, mTextPaint);
            } else {
                // intermediate point
                canvas.drawPoint(mTopAltitudePoint.x, y, mMarkPaint);
            }
            y += mAltimeterStep;
            altitude--;
        }
    }

    private void computeTopAltitude() {
        float topAltitude = mTakeOffAltitude + mClipRect.height() / (2.0f * mAltimeterStep);
        // round altitude to the next lowest (resp. highest) int for positive (resp. negative) altitudes.
        mTopAltitudeValue = (int) (topAltitude - topAltitude % 1);
        mTopAltitudePoint.x = mClipRect.centerX();
        mTopAltitudePoint.y = mClipRect.top + (mAltimeterStep * topAltitude) % mAltimeterStep;
    }

    private void computeHorizon() {
        mHorizonY = mClipRect.centerY() + mGroundAltitude * mAltimeterStep;
        mHorizonY = Math.max(mClipRect.top, Math.min(mClipRect.bottom, mHorizonY));
    }

    private void init() {
        Context context = getContext();
        Resources resources = getResources();

        mClipRect = new RectF();
        mClipPath = new Path();
        mTopAltitudePoint = new PointF();

        mOutlinePaint = new Paint();
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.altimeter_line_thickness));
        mOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
        mOutlinePaint.setStrokeJoin(Paint.Join.ROUND);
        mOutlinePaint.setAntiAlias(true);

        mMarkPaint = new Paint();
        mMarkPaint.setStyle(Paint.Style.STROKE);
        mMarkPaint.setColor(ContextCompat.getColor(context, android.R.color.black));
        mMarkPaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.altimeter_line_thickness));
        mMarkPaint.setAntiAlias(true);

        mAltitudeLinePaint = new Paint();
        mAltitudeLinePaint.setColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
        mAltitudeLinePaint.setStyle(Paint.Style.STROKE);
        mAltitudeLinePaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.altimeter_line_thickness));
        mAltitudeLinePaint.setAntiAlias(true);

        mSkyPaint = new Paint();
        mSkyPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_blue_light));
        mSkyPaint.setStyle(Paint.Style.FILL);

        mGroundPaint = new Paint();
        mGroundPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
        mGroundPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(ContextCompat.getColor(context, android.R.color.black));
        mTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.altimeter_text_size));
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setAntiAlias(true);

        // how much pixels (for current screen density) represents one meter of altitude
        mAltimeterStep = resources.getDimensionPixelSize(R.dimen.altimeter_step);
        // how much pixels to offset the altitude texts from the right of the altimeter view
        mTextXOffset = resources.getDimensionPixelOffset(R.dimen.altimeter_text_offset);
        // how much pixels half of an altitude mark line is long
        mAltitudeLineRadius = resources.getDimensionPixelSize(R.dimen.altimeter_line_radius);
        // vertical offset applied to altitude text to align them with the mark
        mTextYOffset = mTextPaint.descent();
        // radius of the outline rounded corners
        mCornersRadius = resources.getDimensionPixelSize(R.dimen.altimeter_corners);
    }
}