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

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AttitudeView extends View {

    private static final float PITCH_ANGLE_STEP = 5.0f; // pitch angle delta between indices

    private static final float PITCH_ANGLE_TOP = 30.0f; // pitch angle represented by mRadius pixels

    private Paint mOutlinePaint;

    private Paint mIndicesPaint;

    private Paint mAirCraftPaint;

    private Paint mSkyPaint;

    private Paint mGroundPaint;

    private TextPaint mLeftTextPaint;

    private TextPaint mRightTextPaint;

    private RectF mBox;

    private PointF mCenter;

    private float mRadius;

    private Path mIndicatorClip;

    private Path mBankMarker;

    private float mLongBankIndexLength;

    private float mBankIndexOffset;

    private float mBankIndexY;

    private BankIndex[] mBankIndices;

    private float mHorizonY;

    private float mPitchIndexStep;

    private int mTopPitchValue;

    private float mTopPitchY;

    private float mTextOffsetY;

    private float mTextOffsetX;

    private float mLongPitchIndexLeft;

    private float mLongPitchIndexRight;

    private float mShortPitchIndexLeft;

    private float mShortPitchIndexRight;

    private float mTextLeft;

    private float mTextRight;

    // current pitch angle, in degrees;
    private float mPitch;

    // current bank angle, in degrees
    private float mBank;

    public AttitudeView(Context context) {
        super(context);
        init();
    }

    public AttitudeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttitudeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setPitch(float pitch) {
        // be precise to a tenth of degree
        pitch = new BigDecimal(pitch).setScale(1, RoundingMode.HALF_UP).floatValue();
        if (Double.compare(mPitch, pitch) != 0) {
            mPitch = pitch;
            computeHorizon();
            invalidate();
        }
    }

    public void setRoll(float bank) {
        // be precise to a tenth of degree
        bank = new BigDecimal(bank).setScale(1, RoundingMode.HALF_UP).floatValue();
        if (Double.compare(mBank, bank) != 0) {
            mBank = bank;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mCenter.x = (getMeasuredWidth() + getPaddingStart() - getPaddingEnd()) / 2.0f;
        mCenter.y = (getMeasuredHeight() + getPaddingTop() - getPaddingBottom()) / 2.0f;

        float totalRadius = Math.min(getMeasuredWidth() - getPaddingStart() - getPaddingEnd(),
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom()) / 2.0f;

        mRadius = totalRadius - mBankIndexOffset - mLongBankIndexLength;
        // compute path used to clip all what is 'inside' the indicator circle
        mIndicatorClip.reset();
        mIndicatorClip.addCircle(mCenter.x, mCenter.y, mRadius, Path.Direction.CW);
        // Box that contains the indicator circle
        mBox.left = mCenter.x - mRadius;
        mBox.right = mCenter.x + mRadius;
        mBox.top = mCenter.y - mRadius;
        mBox.bottom = mCenter.y + mRadius;
        // X coordinates for pitch index lines
        mLongPitchIndexLeft = mCenter.x - mRadius / 4.0f;
        mLongPitchIndexRight = mCenter.x + mRadius / 4.0f;
        mShortPitchIndexLeft = mCenter.x - mRadius / 8.0f;
        mShortPitchIndexRight = mCenter.x + mRadius / 8.0f;
        // X coordinates for pitch index texts
        mTextLeft = mLongPitchIndexLeft - mTextOffsetX;
        mTextRight = mLongPitchIndexRight + mTextOffsetX;
        // compute horizon
        computeHorizon();
        // compute bank marker
        computeBankMarker();
        // compute bank angle markers Y offset
        mBankIndexY = mCenter.y - mRadius - mBankIndexOffset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw bank angle indices
        canvas.save();
        for (BankIndex bankIndex : mBankIndices) {
            canvas.rotate(bankIndex.rotate, mCenter.x, mCenter.y);
            canvas.drawLine(mCenter.x, mBankIndexY, mCenter.x, mBankIndexY - bankIndex.length, mOutlinePaint);
        }
        canvas.restore();
        // rotate the canvas according to current bank angle
        canvas.save();
        canvas.rotate(mBank, mCenter.x, mCenter.y);
        // clip to inside indicator circle
        canvas.save();
        canvas.clipPath(mIndicatorClip);
        // draw sky
        canvas.drawRect(mBox.left, mBox.top, mBox.right, mHorizonY, mSkyPaint);
        // draw ground
        canvas.drawRect(mBox.left, mHorizonY, mBox.right, mBox.bottom, mGroundPaint);
        //draw pitch indices
        float indexY = mTopPitchY;
        int pitch = mTopPitchValue;

        while (indexY < mBox.bottom) {
            if (pitch != 0) { // don't draw 0° line, its the horizon line
                if (pitch % 2 == 0) { // even pitches (10°, 20° ...) : big line + text
                    canvas.drawLine(mLongPitchIndexLeft, indexY, mLongPitchIndexRight, indexY, mIndicesPaint);

                    String pitchStr = Integer.toString(pitch);
                    canvas.drawText(pitchStr, mTextLeft, indexY - mTextOffsetY, mRightTextPaint);
                    canvas.drawText(pitchStr, mTextRight, indexY - mTextOffsetY, mLeftTextPaint);
                } else { // odd pitches (5°, 15° ...) : short line only
                    canvas.drawLine(mShortPitchIndexLeft, indexY, mShortPitchIndexRight, indexY, mIndicesPaint);
                }
            }
            indexY += mPitchIndexStep;
            pitch -= PITCH_ANGLE_STEP;
        }

        // draw horizon
        canvas.drawLine(mBox.left, mHorizonY, mBox.right, mHorizonY, mIndicesPaint);
        // reset clipping
        canvas.restore();
        // draw indicator outline
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mOutlinePaint);
        // draw bank marker
        canvas.drawPath(mBankMarker, mAirCraftPaint);
        // reset rotation
        canvas.restore();
        // draw aircraft indicator
        canvas.drawLine(mLongPitchIndexLeft, mCenter.y, mLongPitchIndexRight, mCenter.y, mAirCraftPaint);
    }

    private void computeHorizon() {
        // compute how much pixels to offset the horizon to represent current pitch
        float pitchOffset = mRadius * mPitch / PITCH_ANGLE_TOP;
        mHorizonY = mCenter.y + pitchOffset;
        // compute pitch indices step in pixels
        mPitchIndexStep = mRadius * PITCH_ANGLE_STEP / PITCH_ANGLE_TOP;
        // compute top pitch angle, in degrees
        float topPitch = mPitch + PITCH_ANGLE_TOP;
        // round pitch to the next lowest (resp. highest) PITCH_ANGLE_STEP° for positive (resp. negative) pitches.
        mTopPitchValue = (int) (topPitch - (topPitch % PITCH_ANGLE_STEP));
        // compute Y position of top visible pitch index line
        mTopPitchY = mCenter.y - mRadius + floorMod(pitchOffset, mPitchIndexStep);
    }

    private static float floorMod(float value, float modulus) {
        return (value % modulus + modulus) % modulus;
    }

    private void computeBankMarker() {
        mBankMarker.reset();
        mBankMarker.moveTo(mCenter.x - mBankIndexOffset / 2.0f, mCenter.y - mRadius + mBankIndexOffset / 2.0f);
        mBankMarker.rLineTo(mBankIndexOffset / 2.0f, -mBankIndexOffset);
        mBankMarker.rLineTo(mBankIndexOffset / 2.0f, mBankIndexOffset);
        mBankMarker.close();
    }

    private void init() {
        Context context = getContext();
        Resources resources = getResources();

        mBox = new RectF();

        mCenter = new PointF();
        mIndicatorClip = new Path();
        mBankMarker = new Path();

        mOutlinePaint = new Paint();
        mOutlinePaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.attitude_line_thickness));
        mOutlinePaint.setColor(ContextCompat.getColor(context, android.R.color.black));
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
        mOutlinePaint.setStrokeJoin(Paint.Join.ROUND);
        mOutlinePaint.setAntiAlias(true);

        mIndicesPaint = new Paint(mOutlinePaint);
        mIndicesPaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.attitude_pitch_index_thickness));

        mAirCraftPaint = new Paint(mOutlinePaint);
        mAirCraftPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        mAirCraftPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mSkyPaint = new Paint();
        mSkyPaint.setStyle(Paint.Style.FILL);
        mSkyPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_blue_light));

        mGroundPaint = new Paint();
        mGroundPaint.setStyle(Paint.Style.FILL);
        mGroundPaint.setColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));

        mLeftTextPaint = new TextPaint();
        mLeftTextPaint.setColor(ContextCompat.getColor(context, android.R.color.black));
        mLeftTextPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.attitude_text_size));
        mLeftTextPaint.setTextAlign(Paint.Align.LEFT);
        mLeftTextPaint.setAntiAlias(true);

        mRightTextPaint = new TextPaint(mLeftTextPaint);
        mRightTextPaint.setTextAlign(Paint.Align.RIGHT);

        mBankIndexOffset = resources.getDimensionPixelOffset(R.dimen.attitude_bank_index_offset);

        mLongBankIndexLength = resources.getDimensionPixelSize(R.dimen.attitude_big_bank_index_length);
        float smallBankIndexLength = resources.getDimensionPixelSize(R.dimen.attitude_small_bank_index_length);

        mBankIndices = new BankIndex[] {
                // absolute rotation for first index
                new BankIndex(-90, mLongBankIndexLength),
                // then all rotations are relative to one another
                new BankIndex(30, mLongBankIndexLength),
                new BankIndex(15, smallBankIndexLength),
                new BankIndex(15, mLongBankIndexLength),
                new BankIndex(10, smallBankIndexLength),
                new BankIndex(10, smallBankIndexLength),
                new BankIndex(10, mLongBankIndexLength),
                new BankIndex(10, smallBankIndexLength),
                new BankIndex(10, smallBankIndexLength),
                new BankIndex(10, mLongBankIndexLength),
                new BankIndex(15, smallBankIndexLength),
                new BankIndex(15, mLongBankIndexLength),
                new BankIndex(30, mLongBankIndexLength)
        };

        mTextOffsetX = resources.getDimensionPixelOffset(R.dimen.attitude_pitch_text_offset);
        mTextOffsetY = (mLeftTextPaint.ascent() + mLeftTextPaint.descent()) / 2.0f;
    }

    private static final class BankIndex {

        final float rotate;

        final float length;

        BankIndex(float rotate, float length) {
            this.rotate = rotate;
            this.length = length;
        }
    }
}
