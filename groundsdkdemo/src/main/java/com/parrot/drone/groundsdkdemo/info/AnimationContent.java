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

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.AnimationItf;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.animation.Animations;

import java.util.Set;

class AnimationContent extends PilotingItfContent<Drone, AnimationItf> {

    interface OnAnimationConfigRequestListener {

        interface Response {

            void setAnimationConfig(@NonNull Animation.Config config);
        }

        void onAnimationConfigRequest(@NonNull Response response);
    }

    AnimationContent(@NonNull Drone drone) {
        super(R.layout.animation_info, drone, AnimationItf.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends PilotingItfContent.ViewHolder<AnimationContent, AnimationItf> {

        @NonNull
        private final Button mAbortAnimationButton;

        @NonNull
        private final TextView mAvailableAnimationsText;

        @NonNull
        private final TextView mCurrentAnimationText;

        @NonNull
        private final Button mStartAnimationButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mAbortAnimationButton = findViewById(R.id.abort_anim);
            mAbortAnimationButton.setOnClickListener(mClickListener);
            mAvailableAnimationsText = findViewById(R.id.available_animations);
            mCurrentAnimationText = findViewById(R.id.anim);
            mStartAnimationButton = findViewById(R.id.start_anim);
            mStartAnimationButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull AnimationContent content, @NonNull AnimationItf animationItf) {
            Set<Animation.Type> availableAnimations = animationItf.getAvailableAnimations();
            mAvailableAnimationsText.setText(TextUtils.join(", ", availableAnimations));
            mStartAnimationButton.setEnabled(!availableAnimations.isEmpty());
            Animation animation = animationItf.getCurrentAnimation();
            if (animation == null) {
                mCurrentAnimationText.setText(R.string.no_value);
                mAbortAnimationButton.setEnabled(false);
            } else {
                Animation.Status status = animation.getStatus();
                mCurrentAnimationText.setText(mContext.getString(R.string.animating_format,
                        Animations.toString(mContext, animation), animation.getProgress(), status.toString()));
                mAbortAnimationButton.setEnabled(status == Animation.Status.ANIMATING);
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View button, @NonNull AnimationContent content, @NonNull AnimationItf animationItf) {
                if (button.getId() == R.id.abort_anim) {
                    animationItf.abortCurrentAnimation();
                } else if (button.getId() == R.id.start_anim && mContext instanceof OnAnimationConfigRequestListener) {
                    ((OnAnimationConfigRequestListener) mContext).onAnimationConfigRequest(
                            animationItf::startAnimation);
                }
            }
        };
    }
}

