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

package com.parrot.drone.groundsdkdemo.animation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Candle;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.DollySlide;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Dronie;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.GenericTwistUp;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Horizontal180PhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Parabola;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.SphericalPhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Spiral;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertical180PhotoPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.VerticalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertigo;
import com.parrot.drone.groundsdkdemo.R;

public final class Animations {

    @Nullable
    public static Animation.Config defaultConfigFor(@NonNull Animation.Type type) {
        switch (type) {
            case CANDLE:
                return new Candle.Config();
            case DOLLY_SLIDE:
                return new DollySlide.Config();
            case DRONIE:
                return new Dronie.Config();
            case HORIZONTAL_180_PHOTO_PANORAMA:
                return new Horizontal180PhotoPanorama.Config();
            case HORIZONTAL_PANORAMA:
                return new HorizontalPanorama.Config();
            case HORIZONTAL_REVEAL:
                return new HorizontalReveal.Config();
            case PARABOLA:
                return new Parabola.Config();
            case SPHERICAL_PHOTO_PANORAMA:
                return new SphericalPhotoPanorama.Config();
            case SPIRAL:
                return new Spiral.Config();
            case VERTICAL_180_PHOTO_PANORAMA:
                return new Vertical180PhotoPanorama.Config();
            case VERTICAL_REVEAL:
                return new VerticalReveal.Config();
            case VERTIGO:
                return new Vertigo.Config();
            case TWIST_UP:
                return new GenericTwistUp.TwistUpConfig();
            case POSITION_TWIST_UP:
                return new GenericTwistUp.PositionTwistUpConfig();
            case FLIP:
                return null;
            case UNIDENTIFIED:
            default:
                throw new IllegalStateException();
        }
    }

    @NonNull
    public static String toString(@NonNull Context context, @NonNull Animation animation) {
        switch (animation.getType()) {
            case CANDLE:
                Candle candle = (Candle) animation;
                return context.getString(R.string.animation_candle_format, candle.getSpeed(),
                        candle.getVerticalDistance(), candle.getMode().toString());
            case DOLLY_SLIDE:
                DollySlide dollySlide = (DollySlide) animation;
                return context.getString(R.string.animation_dolly_slide_format, dollySlide.getSpeed(),
                        dollySlide.getAngle(), dollySlide.getHorizontalDistance(), dollySlide.getMode().toString());
            case DRONIE:
                Dronie dronie = (Dronie) animation;
                return context.getString(R.string.animation_dronie_format, dronie.getSpeed(), dronie.getDistance(),
                        dronie.getMode().toString());
            case FLIP:
                Flip flip = (Flip) animation;
                return context.getString(R.string.animation_flip_format, flip.getDirection());
            case HORIZONTAL_180_PHOTO_PANORAMA:
                return context.getString(R.string.animation_horizontal_180_photo_panorama_format);
            case HORIZONTAL_PANORAMA:
                HorizontalPanorama horizontalPanorama = (HorizontalPanorama) animation;
                return context.getString(R.string.animation_horizontal_panorama_format,
                        horizontalPanorama.getRotationAngle(), horizontalPanorama.getRotationSpeed());
            case HORIZONTAL_REVEAL:
                HorizontalReveal horizontalReveal = (HorizontalReveal) animation;
                return context.getString(R.string.animation_horizontal_reveal_format, horizontalReveal.getSpeed(),
                        horizontalReveal.getDistance(), horizontalReveal.getMode().toString());
            case PARABOLA:
                Parabola parabola = (Parabola) animation;
                return context.getString(R.string.animation_parabola_format, parabola.getSpeed(),
                        parabola.getVerticalDistance(), parabola.getMode().toString());
            case SPHERICAL_PHOTO_PANORAMA:
                return context.getString(R.string.animation_spherical_photo_panorama_format);
            case SPIRAL:
                Spiral spiral = (Spiral) animation;
                return context.getString(R.string.animation_spiral_format, spiral.getSpeed(),
                        spiral.getRadiusVariation(), spiral.getVerticalDistance(), spiral.getRevolutionAmount(),
                        spiral.getMode().toString());
            case VERTICAL_180_PHOTO_PANORAMA:
                return context.getString(R.string.animation_vertical_180_photo_panorama_format);
            case VERTICAL_REVEAL:
                VerticalReveal verticalReveal = (VerticalReveal) animation;
                return context.getString(R.string.animation_vertical_reveal_format, verticalReveal.getVerticalSpeed(),
                        verticalReveal.getVerticalDistance(), verticalReveal.getRotationAngle(),
                        verticalReveal.getRotationSpeed(), verticalReveal.getMode().toString());
            case VERTIGO:
                Vertigo vertigo = (Vertigo) animation;
                return context.getString(R.string.animation_vertigo_format, vertigo.getDuration(),
                        vertigo.getMaxZoomLevel(), vertigo.getFinishAction().toString(), vertigo.getMode().toString());
            case TWIST_UP:
                GenericTwistUp twistUp = (GenericTwistUp) animation;
                return context.getString(R.string.animation_twist_up_format, twistUp.getSpeed(),
                        twistUp.getVerticalDistance(), twistUp.getRotationAngle(),
                        twistUp.getRotationSpeed(), twistUp.getMode().toString());
            case POSITION_TWIST_UP:
                GenericTwistUp positionTwistUp = (GenericTwistUp) animation;
                return context.getString(R.string.animation_position_twist_up_format,
                        positionTwistUp.getSpeed(), positionTwistUp.getVerticalDistance(),
                        positionTwistUp.getRotationAngle(), positionTwistUp.getRotationSpeed(),
                        positionTwistUp.getMode().toString());
            case UNIDENTIFIED:
            default:
                return animation.getType().toString();
        }
    }

    private Animations() {
    }
}
