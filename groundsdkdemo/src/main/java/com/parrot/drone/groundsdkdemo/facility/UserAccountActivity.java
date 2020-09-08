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

package com.parrot.drone.groundsdkdemo.facility;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parrot.drone.groundsdk.facility.UserAccount;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

public class UserAccountActivity extends GroundSdkActivityBase {

    private SwitchMaterial mAllowAnonymousDataSwitch;

    private UserAccount mUserAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_account);

        EditText providerEdit = findViewById(R.id.provider_edit);
        EditText accountIdEdit = findViewById(R.id.account_id_edit);
        SwitchMaterial allowAccountlessDataSwitch = findViewById(R.id.enable_accountless_data);
        Button setUserAccountButton = findViewById(R.id.btn_set_user_account);
        Button clearButton = findViewById(R.id.btn_clear);
        mAllowAnonymousDataSwitch = findViewById(R.id.enable_anonymous_data);

        setUserAccountButton.setOnClickListener(v -> {
            UserAccount.AccountlessPersonalDataPolicy policy = allowAccountlessDataSwitch.isChecked() ?
                    UserAccount.AccountlessPersonalDataPolicy.ALLOW_UPLOAD
                    : UserAccount.AccountlessPersonalDataPolicy.DENY_UPLOAD;
            mUserAccount.set(providerEdit.getText().toString(), accountIdEdit.getText().toString(), policy);
        });

        clearButton.setOnClickListener(v -> clearUserAccount());

        mAllowAnonymousDataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> clearUserAccount());

        groundSdk().getFacility(UserAccount.class, userAccount -> {
            if (userAccount == null) {
                finish();
                return;
            }
            if (mUserAccount == null) {
                mUserAccount = userAccount;
            }
        });
    }

    private void clearUserAccount() {
        UserAccount.AnonymousDataPolicy policy = mAllowAnonymousDataSwitch.isChecked() ?
                UserAccount.AnonymousDataPolicy.ALLOW_UPLOAD : UserAccount.AnonymousDataPolicy.DENY_UPLOAD;
        mUserAccount.clear(policy);
    }
}
