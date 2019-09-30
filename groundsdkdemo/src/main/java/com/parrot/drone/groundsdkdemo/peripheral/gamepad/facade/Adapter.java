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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class Adapter<FACADE_TYPE, IMPL_TYPE> {

    @NonNull
    abstract FACADE_TYPE fromImpl(@NonNull IMPL_TYPE impl);

    @NonNull
    abstract IMPL_TYPE toImpl(@NonNull FACADE_TYPE facade);

    @NonNull
    final Set<FACADE_TYPE> fromImpl(@NonNull Collection<IMPL_TYPE> impls) {
        Set<FACADE_TYPE> facades = new LinkedHashSet<>();
        for (IMPL_TYPE impl : impls) {
            facades.add(fromImpl(impl));
        }
        return facades;
    }

    @NonNull
    final <T> Map<FACADE_TYPE, T> fromImpl(@NonNull Map<IMPL_TYPE, T> impls) {
        Map<FACADE_TYPE, T> facades = new LinkedHashMap<>();
        for (Map.Entry<IMPL_TYPE, T> entry : impls.entrySet()) {
            facades.put(fromImpl(entry.getKey()), entry.getValue());
        }
        return facades;
    }

    static final class Button<IMPL_ENUM extends Enum<IMPL_ENUM>> extends EnumAdapter<GamepadFacade.Button, IMPL_ENUM> {

        Button(@NonNull Class<IMPL_ENUM> implClass) {
            super(implClass);
        }

        @NonNull
        @Override
        GamepadFacade.Button createFacadeType(@NonNull IMPL_ENUM impl) {
            return new GamepadFacade.Button() {

                @NonNull
                @Override
                public String toString() {
                    return impl.name();
                }
            };
        }
    }

    static final class ButtonEvent<IMPL_ENUM extends Enum<IMPL_ENUM>>
            extends EnumAdapter<GamepadFacade.Button.Event, IMPL_ENUM> {

        ButtonEvent(@NonNull Class<IMPL_ENUM> implClass) {
            super(implClass);
        }

        @NonNull
        @Override
        GamepadFacade.Button.Event createFacadeType(@NonNull IMPL_ENUM impl) {
            return new GamepadFacade.Button.Event() {

                @NonNull
                @Override
                public String toString() {
                    return impl.name();
                }
            };
        }
    }

    static final class ButtonState<IMPL_ENUM extends Enum<IMPL_ENUM>>
            extends EnumAdapter<GamepadFacade.Button.State, IMPL_ENUM> {

        ButtonState(@NonNull Class<IMPL_ENUM> implClass) {
            super(implClass);
        }

        @NonNull
        @Override
        GamepadFacade.Button.State createFacadeType(@NonNull IMPL_ENUM impl) {
            return GamepadFacade.Button.State.values()[impl.ordinal()];
        }
    }

    static final class Axis<IMPL_ENUM extends Enum<IMPL_ENUM>> extends EnumAdapter<GamepadFacade.Axis, IMPL_ENUM> {

        Axis(@NonNull Class<IMPL_ENUM> implClass) {
            super(implClass);
        }

        @NonNull
        @Override
        GamepadFacade.Axis createFacadeType(@NonNull IMPL_ENUM impl) {
            return new GamepadFacade.Axis() {

                @NonNull
                @Override
                public String toString() {
                    return impl.name();
                }
            };
        }
    }

    static final class AxisEvent<IMPL_ENUM extends Enum<IMPL_ENUM>>
            extends EnumAdapter<GamepadFacade.Axis.Event, IMPL_ENUM> {

        AxisEvent(@NonNull Class<IMPL_ENUM> implClass) {
            super(implClass);
        }

        @NonNull
        @Override
        GamepadFacade.Axis.Event createFacadeType(@NonNull IMPL_ENUM impl) {
            return new GamepadFacade.Axis.Event() {

                @NonNull
                @Override
                public String toString() {
                    return impl.name();
                }
            };
        }
    }

    static <FACADE_K, FACADE_V, IMPL_K extends Enum<IMPL_K>, IMPL_V extends Enum<IMPL_V>>
    Map<FACADE_K, FACADE_V> fromImpl(@NonNull Map<IMPL_K, IMPL_V> impls,
                                     @NonNull EnumAdapter<FACADE_K, IMPL_K> keyAdapter,
                                     @NonNull EnumAdapter<FACADE_V, IMPL_V> valueAdapter) {
        Map<FACADE_K, FACADE_V> facades = new LinkedHashMap<>(); // keep impl. ordering
        for (Map.Entry<IMPL_K, IMPL_V> impl : impls.entrySet()) {
            facades.put(keyAdapter.fromImpl(impl.getKey()), valueAdapter.fromImpl(impl.getValue()));
        }
        return facades;
    }

    private abstract static class EnumAdapter<FACADE_TYPE, IMPL_ENUM extends Enum<IMPL_ENUM>>
            extends Adapter<FACADE_TYPE, IMPL_ENUM> {

        @NonNull
        private final Class<IMPL_ENUM> mImplClass;

        @NonNull
        private final EnumMap<IMPL_ENUM, FACADE_TYPE> mFacadeByImpl;

        @NonNull
        private final Map<FACADE_TYPE, IMPL_ENUM> mImplByFacade;

        @NonNull
        private final List<FACADE_TYPE> mAllFacades;

        EnumAdapter(@NonNull Class<IMPL_ENUM> implClass) {
            mImplClass = implClass;
            mFacadeByImpl = new EnumMap<>(mImplClass);
            mImplByFacade = new HashMap<>();
            for (IMPL_ENUM impl : implClass.getEnumConstants()) {
                FACADE_TYPE facade = createFacadeType(impl);
                mFacadeByImpl.put(impl, facade);
                mImplByFacade.put(facade, impl);
            }
            mAllFacades = Collections.unmodifiableList(new ArrayList<>(mFacadeByImpl.values()));
        }

        @NonNull
        abstract FACADE_TYPE createFacadeType(@NonNull IMPL_ENUM impl);

        final List<FACADE_TYPE> facades() {
            return mAllFacades;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        @NonNull
        final FACADE_TYPE fromImpl(@NonNull IMPL_ENUM impl) {
            return mFacadeByImpl.get(impl);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        @NonNull
        final IMPL_ENUM toImpl(@NonNull FACADE_TYPE facade) {
            return mImplByFacade.get(facade);
        }

        @NonNull
        final EnumSet<IMPL_ENUM> toImpl(@NonNull Collection<FACADE_TYPE> facades) {
            EnumSet<IMPL_ENUM> impls = EnumSet.noneOf(mImplClass);
            for (FACADE_TYPE facade : facades) {
                impls.add(toImpl(facade));
            }
            return impls;
        }
    }
}
