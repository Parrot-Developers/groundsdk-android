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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingItfController;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Candle;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.DollySlide;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Dronie;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Flip;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.GenericTwistUp;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalPanorama;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.HorizontalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Parabola;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Spiral;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.VerticalReveal;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertigo;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.AnimationCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.AnimationItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.CandleCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.DollySlideCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.DronieCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.FlipCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.GenericTwistUpCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.Horizontal180PhotoPanoramaCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.HorizontalPanoramaCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.HorizontalRevealCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.ParabolaCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.SphericalPhotoPanoramaCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.SpiralCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.Vertical180PhotoPanoramaCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.VerticalRevealCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.animation.VertigoCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureAnimation;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;

/** Animation piloting interface controller for Anafi family drones. */
public class AnafiAnimationPilotingItf extends PilotingItfController {

    /** Animation interface for which this object is the backend. */
    @NonNull
    private final AnimationItfCore mAnimationItf;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this animation interface controller.
     */
    public AnafiAnimationPilotingItf(@NonNull DroneController droneController) {
        super(droneController);
        mAnimationItf = new AnimationItfCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        mAnimationItf.publish();
    }

    @Override
    protected void onDisconnected() {
        mAnimationItf.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureAnimation.UID) {
            ArsdkFeatureAnimation.decode(command, mAnimationFeatureCallback);
        }
    }

    /**
     * Starts a {@link Flip} animation.
     *
     * @param config {@code Flip} animation configuration
     *
     * @return {@code true if} a Flip animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startFlip(@NonNull Flip.Config config) {
        ArsdkFeatureAnimation.FlipType flipType = null;
        switch (config.getDirection()) {
            case FRONT:
                flipType = ArsdkFeatureAnimation.FlipType.FRONT;
                break;
            case BACK:
                flipType = ArsdkFeatureAnimation.FlipType.BACK;
                break;
            case RIGHT:
                flipType = ArsdkFeatureAnimation.FlipType.RIGHT;
                break;
            case LEFT:
                flipType = ArsdkFeatureAnimation.FlipType.LEFT;
                break;
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartFlip(flipType));
    }

    /**
     * Starts a {@link HorizontalPanorama} animation.
     *
     * @param config {@code HorizontalPanorama} animation configuration
     *
     * @return {@code true if} a HorizontalPanorama animation start request was sent to the device, otherwise {@code
     *         false}
     */
    private boolean startHorizontalPanorama(@NonNull HorizontalPanorama.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomRotationAngle()) {
            customParamsBitField |= ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.toBitField(
                    ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.ROTATION_ANGLE);
        }
        if (config.usesCustomRotationSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.toBitField(
                    ArsdkFeatureAnimation.HorizontalPanoramaConfigParam.ROTATION_SPEED);
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartHorizontalPanorama(customParamsBitField,
                (float) Math.toRadians(config.getRotationAngle()), (float) Math.toRadians(config.getRotationSpeed())));
    }

    /**
     * Starts a {@link Dronie} animation.
     *
     * @param config {@code Dronie} animation configuration
     *
     * @return {@code true if} a Dronie animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startDronie(@NonNull Dronie.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.DronieConfigParam.toBitField(
                    ArsdkFeatureAnimation.DronieConfigParam.SPEED);
        }
        if (config.usesCustomDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.DronieConfigParam.toBitField(
                    ArsdkFeatureAnimation.DronieConfigParam.DISTANCE);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.DronieConfigParam.toBitField(
                    ArsdkFeatureAnimation.DronieConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartDronie(customParamsBitField, (float) config.getSpeed(),
                (float) config.getDistance(), playMode));
    }

    /**
     * Starts a {@link HorizontalReveal} animation.
     *
     * @param config {@code HorizontalReveal} animation configuration
     *
     * @return {@code true if} a HorizontalReveal animation start request was sent to the device, otherwise {@code
     *         false}
     */
    private boolean startHorizontalReveal(@NonNull HorizontalReveal.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.HorizontalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.HorizontalRevealConfigParam.SPEED);
        }
        if (config.usesCustomDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.HorizontalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.HorizontalRevealConfigParam.DISTANCE);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.HorizontalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.HorizontalRevealConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartHorizontalReveal(customParamsBitField,
                (float) config.getSpeed(), (float) config.getDistance(), playMode));
    }

    /**
     * Starts a {@link VerticalReveal} animation.
     *
     * @param config {@code VerticalReveal} animation configuration
     *
     * @return {@code true if} a VerticalReveal animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startVerticalReveal(@NonNull VerticalReveal.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomVerticalSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.VerticalRevealConfigParam.SPEED);
        }
        if (config.usesCustomVerticalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.VerticalRevealConfigParam.VERTICAL_DISTANCE);
        }
        if (config.usesCustomRotationAngle()) {
            customParamsBitField |= ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_ANGLE);
        }
        if (config.usesCustomRotationSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.VerticalRevealConfigParam.ROTATION_SPEED);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.VerticalRevealConfigParam.toBitField(
                    ArsdkFeatureAnimation.VerticalRevealConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartVerticalReveal(customParamsBitField,
                (float) config.getVerticalSpeed(), (float) config.getVerticalDistance(),
                (float) Math.toRadians(config.getRotationAngle()), (float) Math.toRadians(config.getRotationSpeed()),
                playMode));
    }

    /**
     * Starts a {@link Spiral} animation.
     *
     * @param config {@code Spiral} animation configuration
     *
     * @return {@code true if} a Spiral animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startSpiral(@NonNull Spiral.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                    ArsdkFeatureAnimation.SpiralConfigParam.SPEED);
        }
        if (config.usesCustomRadiusVariation()) {
            customParamsBitField |= ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                    ArsdkFeatureAnimation.SpiralConfigParam.RADIUS_VARIATION);
        }
        if (config.usesCustomVerticalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                    ArsdkFeatureAnimation.SpiralConfigParam.VERTICAL_DISTANCE);
        }
        if (config.usesCustomRevolutionAmount()) {
            customParamsBitField |= ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                    ArsdkFeatureAnimation.SpiralConfigParam.REVOLUTION_NB);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.SpiralConfigParam.toBitField(
                    ArsdkFeatureAnimation.SpiralConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartSpiral(customParamsBitField, (float) config.getSpeed(),
                (float) config.getRadiusVariation(), (float) config.getVerticalDistance(),
                (float) config.getRevolutionAmount(), playMode));
    }

    /**
     * Starts a {@link Parabola} animation.
     *
     * @param config {@code Parabola} animation configuration
     *
     * @return {@code true if} a Parabola animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startParabola(@NonNull Parabola.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.ParabolaConfigParam.toBitField(
                    ArsdkFeatureAnimation.ParabolaConfigParam.SPEED);
        }
        if (config.usesCustomVerticalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.ParabolaConfigParam.toBitField(
                    ArsdkFeatureAnimation.ParabolaConfigParam.VERTICAL_DISTANCE);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.ParabolaConfigParam.toBitField(
                    ArsdkFeatureAnimation.ParabolaConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartParabola(customParamsBitField, (float) config.getSpeed(),
                (float) config.getVerticalDistance(), playMode));
    }

    /**
     * Starts a {@link Candle} animation.
     *
     * @param config {@code Candle} animation configuration
     *
     * @return {@code true if} a Candle animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startCandle(@NonNull Candle.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.CandleConfigParam.toBitField(
                    ArsdkFeatureAnimation.CandleConfigParam.SPEED);
        }
        if (config.usesCustomVerticalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.CandleConfigParam.toBitField(
                    ArsdkFeatureAnimation.CandleConfigParam.VERTICAL_DISTANCE);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.CandleConfigParam.toBitField(
                    ArsdkFeatureAnimation.CandleConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartCandle(customParamsBitField, (float) config.getSpeed(),
                (float) config.getVerticalDistance(), playMode));
    }

    /**
     * Starts a {@link DollySlide} animation.
     *
     * @param config {@code DollySlide} animation configuration
     *
     * @return {@code true if} a DollySlide animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startDollySlide(@NonNull DollySlide.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.DollySlideConfigParam.toBitField(
                    ArsdkFeatureAnimation.DollySlideConfigParam.SPEED);
        }
        if (config.usesCustomAngle()) {
            customParamsBitField |= ArsdkFeatureAnimation.DollySlideConfigParam.toBitField(
                    ArsdkFeatureAnimation.DollySlideConfigParam.ANGLE);
        }
        if (config.usesCustomHorizontalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.DollySlideConfigParam.toBitField(
                    ArsdkFeatureAnimation.DollySlideConfigParam.HORIZONTAL_DISTANCE);
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.DollySlideConfigParam.toBitField(
                    ArsdkFeatureAnimation.DollySlideConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartDollySlide(customParamsBitField, (float) config.getSpeed(),
                (float) Math.toRadians(config.getAngle()), (float) config.getHorizontalDistance(), playMode));
    }

    /**
     * Starts a {@link Vertigo} animation.
     *
     * @param config {@code Vertigo} animation configuration
     *
     * @return {@code true if} a Vertigo animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startVertigo(@NonNull Vertigo.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomDuration()) {
            customParamsBitField |= ArsdkFeatureAnimation.VertigoConfigParam.toBitField(
                    ArsdkFeatureAnimation.VertigoConfigParam.DURATION);
        }
        if (config.usesCustomMaxZoomLevel()) {
            customParamsBitField |= ArsdkFeatureAnimation.VertigoConfigParam.toBitField(
                    ArsdkFeatureAnimation.VertigoConfigParam.MAX_ZOOM_LEVEL);
        }
        Vertigo.FinishAction finishAction = config.getFinishAction();
        ArsdkFeatureAnimation.VertigoFinishAction arsdkFinishAction = ArsdkFeatureAnimation.VertigoFinishAction.NONE;
        if (finishAction != null) {
            customParamsBitField |= ArsdkFeatureAnimation.VertigoConfigParam.toBitField(
                    ArsdkFeatureAnimation.VertigoConfigParam.FINISH_ACTION);
            switch (finishAction) {
                case NONE:
                    arsdkFinishAction = ArsdkFeatureAnimation.VertigoFinishAction.NONE;
                    break;
                case UNZOOM:
                    arsdkFinishAction = ArsdkFeatureAnimation.VertigoFinishAction.UNZOOM;
                    break;
            }
        }
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.VertigoConfigParam.toBitField(
                    ArsdkFeatureAnimation.VertigoConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartVertigo(customParamsBitField, (float) config.getDuration(),
                (float) config.getMaxZoomLevel(), arsdkFinishAction, playMode));
    }

    /**
     * Starts a twist-up animation.
     *
     * @param config twist-up animation configuration
     *
     * @return {@code true if} a twist-up animation start request was sent to the device, otherwise {@code false}
     */
    private boolean startTwistUp(@NonNull GenericTwistUp.TwistUpConfig config) {
        int customParamsBitField = createTwistUpParamsBitField(config);
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartTwistUp(customParamsBitField, (float) config.getSpeed(),
                (float) config.getVerticalDistance(), (float) Math.toRadians(config.getRotationAngle()),
                (float) Math.toRadians(config.getRotationSpeed()), playMode));
    }

    /**
     * Starts a position twist-up animation.
     *
     * @param config position twist-up animation configuration
     *
     * @return {@code true if} a position twist-up animation start request was sent to the device, otherwise {@code
     *         false}
     */
    private boolean startPositionTwistUp(@NonNull GenericTwistUp.PositionTwistUpConfig config) {
        int customParamsBitField = createTwistUpParamsBitField(config);
        Animation.Mode mode = config.getMode();
        ArsdkFeatureAnimation.PlayMode playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
        if (mode != null) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.PLAY_MODE);
            switch (mode) {
                case ONCE:
                    playMode = ArsdkFeatureAnimation.PlayMode.NORMAL;
                    break;
                case ONCE_THEN_MIRRORED:
                    playMode = ArsdkFeatureAnimation.PlayMode.ONCE_THEN_MIRRORED;
                    break;
            }
        }
        return sendCommand(ArsdkFeatureAnimation.encodeStartPositionTwistUp(customParamsBitField,
                (float) config.getSpeed(), (float) config.getVerticalDistance(),
                (float) Math.toRadians(config.getRotationAngle()), (float) Math.toRadians(config.getRotationSpeed()),
                playMode));
    }

    /** Callbacks called when a command of the feature ArsdkFeatureAnimation is decoded. */
    private final ArsdkFeatureAnimation.Callback mAnimationFeatureCallback = new ArsdkFeatureAnimation.Callback() {

        @Override
        public void onAvailability(int valuesBitField) {
            EnumSet<Animation.Type> availableAnimations = EnumSet.noneOf(Animation.Type.class);
            for (ArsdkFeatureAnimation.Type type : ArsdkFeatureAnimation.Type.fromBitfield(valuesBitField)) {
                switch (type) {
                    case NONE:
                        break;
                    case FLIP:
                        availableAnimations.add(Animation.Type.FLIP);
                        break;
                    case HORIZONTAL_PANORAMA:
                        availableAnimations.add(Animation.Type.HORIZONTAL_PANORAMA);
                        break;
                    case DRONIE:
                        availableAnimations.add(Animation.Type.DRONIE);
                        break;
                    case HORIZONTAL_REVEAL:
                        availableAnimations.add(Animation.Type.HORIZONTAL_REVEAL);
                        break;
                    case VERTICAL_REVEAL:
                        availableAnimations.add(Animation.Type.VERTICAL_REVEAL);
                        break;
                    case SPIRAL:
                        availableAnimations.add(Animation.Type.SPIRAL);
                        break;
                    case PARABOLA:
                        availableAnimations.add(Animation.Type.PARABOLA);
                        break;
                    case CANDLE:
                        availableAnimations.add(Animation.Type.CANDLE);
                        break;
                    case DOLLY_SLIDE:
                        availableAnimations.add(Animation.Type.DOLLY_SLIDE);
                        break;
                    case VERTIGO:
                        availableAnimations.add(Animation.Type.VERTIGO);
                        break;
                    case TWIST_UP:
                        availableAnimations.add(Animation.Type.TWIST_UP);
                        break;
                    case POSITION_TWIST_UP:
                        availableAnimations.add(Animation.Type.POSITION_TWIST_UP);
                        break;
                    case HORIZONTAL_180_PHOTO_PANORAMA:
                        availableAnimations.add(Animation.Type.HORIZONTAL_180_PHOTO_PANORAMA);
                        break;
                    case VERTICAL_180_PHOTO_PANORAMA:
                        availableAnimations.add(Animation.Type.VERTICAL_180_PHOTO_PANORAMA);
                        break;
                    case SPHERICAL_PHOTO_PANORAMA:
                        availableAnimations.add(Animation.Type.SPHERICAL_PHOTO_PANORAMA);
                        break;
                }
            }
            mAnimationItf.updateAvailableAnimations(availableAnimations).notifyUpdated();
        }

        @Override
        public void onState(@Nullable ArsdkFeatureAnimation.Type type, int percent) {
            if (type == null) {
                // force state animating as we don't know much more about it
                mAnimationItf.updateAnimation(AnimationCore.unidentified(), Animation.Status.ANIMATING);
            } else if (type == ArsdkFeatureAnimation.Type.NONE) {
                percent = 0;
                mAnimationItf.clearAnimation();
            }
            mAnimationItf.updateCurrentAnimationProgress(percent).notifyUpdated();
        }

        @Override
        public void onFlipState(@Nullable ArsdkFeatureAnimation.State state,
                                @Nullable ArsdkFeatureAnimation.FlipType type) {

            Animation.Status status = convertState(state);
            if (status != null) {
                Flip.Direction direction = convertFlipType(type);
                mAnimationItf.updateAnimation(direction == null ? AnimationCore.unidentified() : new FlipCore(
                        direction), status).notifyUpdated();
            }
        }

        @Override
        public void onHorizontalPanoramaState(@Nullable ArsdkFeatureAnimation.State state, float rotationAngle,
                                              float rotationSpeed) {
            Animation.Status status = convertState(state);
            if (status != null) {
                mAnimationItf.updateAnimation(new HorizontalPanoramaCore(Math.toDegrees(rotationAngle),
                        Math.toDegrees(rotationSpeed)), status).notifyUpdated();
            }
        }

        @Override
        public void onDronieState(@Nullable ArsdkFeatureAnimation.State state, float speed, float distance,
                                  @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new DronieCore(speed,
                        distance, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onHorizontalRevealState(@Nullable ArsdkFeatureAnimation.State state, float speed, float distance,
                                            @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new HorizontalRevealCore(
                        speed, distance, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onVerticalRevealState(@Nullable ArsdkFeatureAnimation.State state, float speed,
                                          float verticalDistance, float rotationAngle, float rotationSpeed,
                                          @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new VerticalRevealCore(
                        speed, verticalDistance, Math.toDegrees(rotationAngle), Math.toDegrees(rotationSpeed),
                        mode), status).notifyUpdated();
            }
        }

        @Override
        public void onSpiralState(@Nullable ArsdkFeatureAnimation.State state, float speed, float radiusVariation,
                                  float verticalDistance, float revolutionNb,
                                  @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new SpiralCore(speed,
                        radiusVariation, verticalDistance, revolutionNb, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onParabolaState(@Nullable ArsdkFeatureAnimation.State state, float speed, float verticalDistance,
                                    @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new ParabolaCore(speed,
                        verticalDistance, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onCandleState(@Nullable ArsdkFeatureAnimation.State state, float speed, float verticalDistance,
                                  @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new CandleCore(speed,
                        verticalDistance, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onDollySlideState(@Nullable ArsdkFeatureAnimation.State state, float speed, float angle,
                                      float horizontalDistance, @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new DollySlideCore(speed,
                        Math.toDegrees(angle), horizontalDistance, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onVertigoState(@Nullable ArsdkFeatureAnimation.State state, float duration, float maxZoomLevel,
                                   @Nullable ArsdkFeatureAnimation.VertigoFinishAction arsdkFinishAction,
                                   @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Vertigo.FinishAction finishAction = convertFinishAction(arsdkFinishAction);
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(((mode == null) || (finishAction == null)) ? AnimationCore.unidentified()
                        : new VertigoCore(duration, maxZoomLevel, finishAction, mode), status).notifyUpdated();
            }
        }

        @Override
        public void onTwistUpState(@Nullable ArsdkFeatureAnimation.State state, float speed, float verticalDistance,
                                   float rotationAngle, float rotationSpeed,
                                   @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new GenericTwistUpCore(
                        Animation.Type.TWIST_UP, speed, verticalDistance, Math.toDegrees(rotationAngle),
                        Math.toDegrees(rotationSpeed), mode), status).notifyUpdated();
            }
        }

        @Override
        public void onPositionTwistUpState(@Nullable ArsdkFeatureAnimation.State state, float speed,
                                           float verticalDistance, float rotationAngle, float rotationSpeed,
                                           @Nullable ArsdkFeatureAnimation.PlayMode playMode) {
            Animation.Status status = convertState(state);
            if (status != null) {
                Animation.Mode mode = convertPlayMode(playMode);
                mAnimationItf.updateAnimation(mode == null ? AnimationCore.unidentified() : new GenericTwistUpCore(
                        Animation.Type.POSITION_TWIST_UP, speed, verticalDistance, Math.toDegrees(rotationAngle),
                        Math.toDegrees(rotationSpeed), mode), status).notifyUpdated();
            }
        }

        @Override
        public void onHorizontal180PhotoPanoramaState(@Nullable ArsdkFeatureAnimation.State state) {
            Animation.Status status = convertState(state);
            if (status != null) {
                mAnimationItf.updateAnimation(new Horizontal180PhotoPanoramaCore(), status).notifyUpdated();
            }
        }

        @Override
        public void onVertical180PhotoPanoramaState(@Nullable ArsdkFeatureAnimation.State state) {
            Animation.Status status = convertState(state);
            if (status != null) {
                mAnimationItf.updateAnimation(new Vertical180PhotoPanoramaCore(), status).notifyUpdated();
            }
        }

        @Override
        public void onSphericalPhotoPanoramaState(@Nullable ArsdkFeatureAnimation.State state) {
            Animation.Status status = convertState(state);
            if (status != null) {
                mAnimationItf.updateAnimation(new SphericalPhotoPanoramaCore(), status).notifyUpdated();
            }
        }
    };

    /** Backend of AnimationItfCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final AnimationItfCore.Backend mBackend = new AnimationItfCore.Backend() {

        @Override
        public boolean startAnimation(@NonNull Animation.Config animationConfig) {
            switch (animationConfig.getAnimationType()) {
                case FLIP:
                    return startFlip((Flip.Config) animationConfig);
                case HORIZONTAL_180_PHOTO_PANORAMA:
                    return sendCommand(ArsdkFeatureAnimation.encodeStartHorizontal180PhotoPanorama());
                case HORIZONTAL_PANORAMA:
                    return startHorizontalPanorama((HorizontalPanorama.Config) animationConfig);
                case DRONIE:
                    return startDronie((Dronie.Config) animationConfig);
                case HORIZONTAL_REVEAL:
                    return startHorizontalReveal((HorizontalReveal.Config) animationConfig);
                case VERTICAL_REVEAL:
                    return startVerticalReveal((VerticalReveal.Config) animationConfig);
                case SPHERICAL_PHOTO_PANORAMA:
                    return sendCommand(ArsdkFeatureAnimation.encodeStartSphericalPhotoPanorama());
                case SPIRAL:
                    return startSpiral((Spiral.Config) animationConfig);
                case PARABOLA:
                    return startParabola((Parabola.Config) animationConfig);
                case CANDLE:
                    return startCandle((Candle.Config) animationConfig);
                case DOLLY_SLIDE:
                    return startDollySlide((DollySlide.Config) animationConfig);
                case VERTIGO:
                    return startVertigo((Vertigo.Config) animationConfig);
                case TWIST_UP:
                    return startTwistUp((GenericTwistUp.TwistUpConfig) animationConfig);
                case POSITION_TWIST_UP:
                    return startPositionTwistUp((GenericTwistUp.PositionTwistUpConfig) animationConfig);
                case VERTICAL_180_PHOTO_PANORAMA:
                    return sendCommand(ArsdkFeatureAnimation.encodeStartVertical180PhotoPanorama());
                case UNIDENTIFIED:
                    throw new IllegalArgumentException();
            }
            return false;
        }

        @Override
        public boolean abortCurrentAnimation() {
            sendCommand(ArsdkFeatureAnimation.encodeCancel());
            return true;
        }
    };

    /**
     * Converts an {@code ArsdkFeatureAnimation.State} to its groundSdk {@code Animation.Status} equivalent.
     * <p>
     * Note that a null {@code ArsdkFeatureAnimation.State} (an unsupported arsdk value from groundSdk point of view) is
     * considered as {@link Animation.Status#ANIMATING}.
     *
     * @param state {@code ArsdkFeatureAnimation.State} to convert
     *
     * @return the {@code Animation.Status} equivalent, or {@code null} if {@code state} is {@link
     *         ArsdkFeatureAnimation.State#IDLE}
     */
    @Nullable
    private static Animation.Status convertState(@Nullable ArsdkFeatureAnimation.State state) {
        if (state != null) {
            switch (state) {
                case IDLE:
                    return null;
                case RUNNING:
                    return Animation.Status.ANIMATING;
                case CANCELING:
                    return Animation.Status.ABORTING;
            }
        }
        // if we don't handle this state, assume animating
        return Animation.Status.ANIMATING;
    }

    /**
     * Converts an {@code ArsdkFeatureAnimation.FlipType} to its groundSdk {@code Flip.Direction} equivalent.
     *
     * @param type {@code ArsdkFeatureAnimation.FlipType} to convert
     *
     * @return the {@code Flip.Direction} equivalent, or {@code null} if {@code type} is {@code null} or unknown.
     */
    @Nullable
    private static Flip.Direction convertFlipType(@Nullable ArsdkFeatureAnimation.FlipType type) {
        if (type != null) {
            switch (type) {
                case FRONT:
                    return Flip.Direction.FRONT;
                case BACK:
                    return Flip.Direction.BACK;
                case LEFT:
                    return Flip.Direction.LEFT;
                case RIGHT:
                    return Flip.Direction.RIGHT;
            }
        }
        return null;
    }

    /**
     * Converts an {@code ArsdkFeatureAnimation.PlayMode} to its groundSdk {@code Animation.Mode} equivalent.
     *
     * @param mode {@code ArsdkFeatureAnimation.PlayMode} to convert
     *
     * @return the {@code Animation.Mode} equivalent, or {@code null} if {@code mode} is {@code null} or unknown.
     */
    @Nullable
    private static Animation.Mode convertPlayMode(@Nullable ArsdkFeatureAnimation.PlayMode mode) {
        if (mode != null) {
            switch (mode) {
                case NORMAL:
                    return Animation.Mode.ONCE;
                case ONCE_THEN_MIRRORED:
                    return Animation.Mode.ONCE_THEN_MIRRORED;
            }
        }
        return null;
    }

    /**
     * Converts an {@code ArsdkFeatureAnimation.VertigoFinishAction} to its groundSdk {@code Vertigo.FinishAction}
     * equivalent.
     *
     * @param action {@code ArsdkFeatureAnimation.VertigoFinishAction} to convert
     *
     * @return {@code Vertigo.FinishAction} equivalent, or {@code null} if {@code action} is {@code null} or unknown.
     */
    @Nullable
    private static Vertigo.FinishAction convertFinishAction(
            @Nullable ArsdkFeatureAnimation.VertigoFinishAction action) {
        if (action != null) {
            switch (action) {
                case NONE:
                    return Vertigo.FinishAction.NONE;
                case UNZOOM:
                    return Vertigo.FinishAction.UNZOOM;
            }
        }
        return null;
    }

    /**
     * Creates the twist-up parameters bitfield indicating which values should be used.
     *
     * @param config twist-up configuration used to create the bitfield
     *
     * @return the parameters bitfield
     */
    private static int createTwistUpParamsBitField(@NonNull GenericTwistUp.Config config) {
        int customParamsBitField = 0;
        if (config.usesCustomSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.SPEED);
        }
        if (config.usesCustomVerticalDistance()) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.VERTICAL_DISTANCE);
        }
        if (config.usesCustomRotationAngle()) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_ANGLE);
        }
        if (config.usesCustomRotationSpeed()) {
            customParamsBitField |= ArsdkFeatureAnimation.TwistUpConfigParam.toBitField(
                    ArsdkFeatureAnimation.TwistUpConfigParam.ROTATION_SPEED);
        }
        return customParamsBitField;
    }
}
