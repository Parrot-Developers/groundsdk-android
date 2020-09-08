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

package com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class GamepadFacadeProvider implements Peripheral.Provider {

    private static final GamepadFacade.Creator<?>[] FACADE_IMPL_CREATORS = new GamepadFacade.Creator[] {
            Sc3GamepadFacadeImpl.CREATOR,
            ScUaGamepadFacadeImpl.CREATOR
    };

    @NonNull
    public static GamepadFacadeProvider of(@NonNull RemoteControl remoteControl) {
        return new GamepadFacadeProvider(remoteControl);
    }

    @NonNull
    private final RemoteControl mRemoteControl;

    private GamepadFacadeProvider(@NonNull RemoteControl remoteControl) {
        mRemoteControl = remoteControl;
    }

    @NonNull
    public String getUid() {
        return mRemoteControl.getUid();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <P extends Peripheral> P getPeripheral(@NonNull Class<P> peripheralClass) {
        P peripheral = null;
        if (peripheralClass == GamepadFacade.class) {
            for (int i = 0; i < FACADE_IMPL_CREATORS.length && peripheral == null; i++) {
                peripheral = (P) createFacade(FACADE_IMPL_CREATORS[i]);
            }
        }
        return peripheral;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <P extends Peripheral> Ref<P> getPeripheral(@NonNull Class<P> peripheralClass,
                                                       @NonNull Ref.Observer<P> observer) {
        Ref<P> ref = NULL_REF;
        if (peripheralClass == GamepadFacade.class) {
            ref = (Ref<P>) new GamepadFacadeRef((Ref.Observer<GamepadFacade>) observer);
        }
        return ref;
    }

    @Nullable
    private <GAMEPAD_IMPL extends Peripheral> GamepadFacade createFacade(
            @NonNull GamepadFacade.Creator<GAMEPAD_IMPL> creator) {
        GAMEPAD_IMPL impl = mRemoteControl.getPeripheral(creator.getGamepadClass());
        return impl == null ? null : creator.create(impl);
    }

    private class GamepadFacadeRef extends Ref<GamepadFacade> {

        @Nullable
        private GamepadFacade mFacade;

        private final Ref.Observer<GamepadFacade> mObserver;

        private final Map<Observer<?>, Ref<?>> mImplObservers;

        GamepadFacadeRef(@NonNull Ref.Observer<GamepadFacade> observer) {
            mObserver = observer;
            mImplObservers = new HashMap<>();
            for (int i = 0; i < FACADE_IMPL_CREATORS.length && mFacade == null; i++) {
                registerObserver(FACADE_IMPL_CREATORS[i]);
            }
        }

        private <GAMEPAD_IMPL extends Peripheral> void registerObserver(
                @NonNull GamepadFacade.Creator<GAMEPAD_IMPL> creator) {
            Ref.Observer<GAMEPAD_IMPL> observer = new Observer<GAMEPAD_IMPL>() {

                @SuppressWarnings("ConstantConditions")
                @Override
                public void onChanged(@Nullable GAMEPAD_IMPL gamepad) {
                    if (mFacade == null) {
                        if (gamepad != null) {
                            // close all other observers/refs;
                            for (Iterator<Observer<?>> iter = mImplObservers.keySet().iterator(); iter.hasNext(); ) {
                                Observer<?> obs = iter.next();
                                if (obs != this) {
                                    mImplObservers.get(obs).close();
                                    iter.remove();
                                }
                            }
                            // this becomes the facade impl
                            mFacade = creator.create(gamepad);
                        }
                    }
                    if (mFacade != null) {
                        mObserver.onChanged(gamepad == null ? null : mFacade);
                    }
                }
            };

            Ref<GAMEPAD_IMPL> ref = mRemoteControl.getPeripheral(creator.getGamepadClass(), observer);
            mImplObservers.put(observer, ref);
        }

        @Nullable
        @Override
        public GamepadFacade get() {
            return mFacade;
        }

        @Override
        public void close() {
            for (Ref<?> ref : mImplObservers.values()) {
                ref.close();
            }
            mImplObservers.clear();
        }
    }

    private static final Ref NULL_REF = new Ref() {

        @Nullable
        @Override
        public Object get() {
            return null;
        }

        @Override
        public void close() {
        }
    };
}
