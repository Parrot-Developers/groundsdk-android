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

package com.parrot.drone.groundsdk.internal.component;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.session.Session;

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
interface CompType {
}

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
interface MainComp extends CompType {

    String mainFunc();
}

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
interface SubComp extends MainComp {

    String subFunc();
}

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
class CompImpl extends ComponentCore {

    CompImpl(@NonNull ComponentDescriptor<CompType, ?> descriptor) {
        //noinspection ConstantConditions
        super(descriptor, null);
    }

    @Override
    @NonNull
    protected Object getProxy(@NonNull Session session) {
        return this;
    }
}

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
class MainCompImpl extends CompImpl implements MainComp {

    MainCompImpl() {
        super(DESC);
    }

    MainCompImpl(@NonNull ComponentDescriptor<CompType, ?> descriptor) {
        super(descriptor);
    }

    @Override
    public String mainFunc() {
        return "mainFunc";
    }

    @NonNull
    static final ComponentDescriptor<CompType, MainComp> DESC = ComponentDescriptor.of(MainComp.class);
}

@SuppressWarnings({"MultipleTopLevelClassesInFile", "ClassNameDiffersFromFileName"})
class SubCompImpl extends MainCompImpl implements SubComp {

    SubCompImpl() {
        super(DESC);
    }

    @Override
    public String mainFunc() {
        return "mainFunc on sub";
    }

    @Override
    public String subFunc() {
        return "subFunc";
    }

    @NonNull
    static final ComponentDescriptor<CompType, SubComp> DESC = ComponentDescriptor.of(SubComp.class, MainCompImpl.DESC);
}

