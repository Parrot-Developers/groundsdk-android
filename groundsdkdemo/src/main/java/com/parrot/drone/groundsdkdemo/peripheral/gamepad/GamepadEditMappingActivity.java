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

package com.parrot.drone.groundsdkdemo.peripheral.gamepad;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.AxisMappableAction;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacade;
import com.parrot.drone.groundsdkdemo.peripheral.gamepad.facade.GamepadFacadeProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class GamepadEditMappingActivity extends GroundSdkActivityBase {

    public static final String EXTRA_ENTRY_MODEL =
            "com.parrot.drone.groundsdkdemo.peripheral.gamepad.EXTRA_ENTRY_MODEL";

    public static final String EXTRA_ENTRY_TYPE =
            "com.parrot.drone.groundsdkdemo.peripheral.gamepad.EXTRA_ENTRY_TYPE";

    public static final String EXTRA_ENTRY_ACTION =
            "com.parrot.drone.groundsdkdemo.peripheral.gamepad.EXTRA_ENTRY_ACTION";

    private GamepadFacade mGamepad;

    private Drone.Model mDroneModel;

    private Spinner mTypeSpinner;

    private Spinner mActionSpinner;

    private Button mEditAxisBtn;

    private Button mEditButtonsBtn;

    private Button mConfirmBtn;

    private Button mDeleteBtn;

    private TextView mAxisText;

    private TextView mButtonsText;

    private TextView mNoticeText;

    private GamepadFacade.Mapping.Entry.Type mSelectedType;

    private ButtonsMappableAction mSelectedButtonsAction;

    private AxisMappableAction mSelectedAxisAction;

    private GamepadFacade.Axis.Event mSelectedAxis;

    private final Set<GamepadFacade.Button.Event> mSelectedButtons = new HashSet<>();

    private View mAxisRow;

    private boolean mEditingAxis;

    private boolean mEditingButtons;

    private boolean mMappingOverride;

    private boolean mInEditMode;

    private ArrayAdapter<GamepadFacade.Mapping.Entry.Type> mMappingTypeAdapter;

    private ArrayAdapter<ButtonsMappableAction> mButtonsActionAdapter;

    private ArrayAdapter<AxisMappableAction> mAxisActionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RemoteControl rc = groundSdk().getRemoteControl(getIntent().getStringExtra(EXTRA_DEVICE_UID));
        if (rc == null) {
            finish();
            return;
        }

        int modelOrdinal = getIntent().getIntExtra(EXTRA_ENTRY_MODEL, -1);
        if (modelOrdinal == -1) {
            finish();
        }

        mDroneModel = Drone.Model.values()[modelOrdinal];

        setContentView(R.layout.activity_gamepad_edit_mapping);

        TextView modelText = findViewById(R.id.model);
        modelText.setText(mDroneModel.toString());

        mTypeSpinner = findViewById(R.id.mapping_type);
        mActionSpinner = findViewById(R.id.mapping_action);

        mAxisText = findViewById(R.id.axis);
        mEditAxisBtn = findViewById(R.id.btn_edit_axis);
        mAxisRow = findViewById(R.id.axis_row);

        mButtonsText = findViewById(R.id.buttons);
        mEditButtonsBtn = findViewById(R.id.btn_edit_buttons);

        mNoticeText = findViewById(R.id.notice);

        mEditAxisBtn.setOnClickListener(v -> handleAxisMapping());

        mEditButtonsBtn.setOnClickListener(v -> handleButtonsMapping());

        mConfirmBtn = findViewById(R.id.btn_confirm);
        mConfirmBtn.setOnClickListener(v -> applyMapping(true));

        mDeleteBtn = findViewById(R.id.btn_delete);
        mDeleteBtn.setOnClickListener(v -> applyMapping(false));

        mMappingTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                GamepadFacade.Mapping.Entry.Type.values());

        mButtonsActionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                ButtonsMappableAction.values());

        mAxisActionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                AxisMappableAction.values());

        GamepadFacadeProvider.of(rc).getPeripheral(GamepadFacade.class, gamepad -> {
            if (gamepad == null) {
                finish();
            } else if (mGamepad == null) {
                mGamepad = gamepad;
                onGamepadAvailable();
            }
        });
    }

    @Override
    protected void onStop() {
        if (mGamepad != null) {
            mGamepad.setButtonEventListener(null).grabInputs(NO_BUTTONS, NO_AXES);
        }
        super.onStop();
    }

    private void onGamepadAvailable() {
        mTypeSpinner.setAdapter(mMappingTypeAdapter);

        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onMappingTypeChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                throw new IllegalStateException();
            }
        });

        mActionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onActionChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                throw new IllegalStateException();
            }
        });

        int typeOrdinal = getIntent().getIntExtra(EXTRA_ENTRY_TYPE, -1);
        if (typeOrdinal != -1) {
            mInEditMode = true;
            mTypeSpinner.setSelection(typeOrdinal);
            mTypeSpinner.setEnabled(false);
        }
    }

    private void onMappingTypeChanged(int position) {
        mSelectedType = mMappingTypeAdapter.getItem(position);
        assert mSelectedType != null;
        switch (mSelectedType) {
            case BUTTONS_MAPPING:
                mActionSpinner.setAdapter(mButtonsActionAdapter);
                mAxisRow.setVisibility(View.GONE);
                break;
            case AXIS_MAPPING:
                mActionSpinner.setAdapter(mAxisActionAdapter);
                mAxisRow.setVisibility(View.VISIBLE);
                break;
        }
        if (mInEditMode) {
            int actionInt = getIntent().getIntExtra(EXTRA_ENTRY_ACTION, -1);
            if (actionInt == -1) {
                throw new IllegalArgumentException();
            }
            mActionSpinner.setSelection(actionInt);
            mActionSpinner.setEnabled(false);
        }
    }

    private void onActionChanged(int position) {
        mMappingOverride = false;
        Set<GamepadFacade.Mapping.Entry> mapping = mGamepad.getMapping(mDroneModel);
        assert mapping != null;
        switch (mSelectedType) {
            case BUTTONS_MAPPING:
                mSelectedButtonsAction = mButtonsActionAdapter.getItem(position);
                for (GamepadFacade.Mapping.Entry entry : mapping) {
                    if (entry.getType() != mSelectedType) {
                        continue;
                    }
                    GamepadFacade.Button.MappingEntry buttonsEntry = entry.as(GamepadFacade.Button.MappingEntry.class);
                    if (buttonsEntry.getAction() != mSelectedButtonsAction) {
                        continue;
                    }
                    if (mInEditMode) {
                        mSelectedButtons.clear();
                        mSelectedButtons.addAll(buttonsEntry.getButtonEvents());
                    } else {
                        mMappingOverride = true;
                    }
                    break;
                }
                break;
            case AXIS_MAPPING:
                mSelectedAxisAction = mAxisActionAdapter.getItem(position);
                for (GamepadFacade.Mapping.Entry entry : mapping) {
                    if (entry.getType() != mSelectedType) {
                        continue;
                    }
                    GamepadFacade.Axis.MappingEntry axisEntry = entry.as(GamepadFacade.Axis.MappingEntry.class);
                    if (axisEntry.getAction() != mSelectedAxisAction) {
                        continue;
                    }
                    if (mInEditMode) {
                        mSelectedAxis = axisEntry.getAxisEvent();
                        mSelectedButtons.clear();
                        mSelectedButtons.addAll(axisEntry.getButtonEvents());
                    } else {
                        mMappingOverride = true;
                    }
                    break;
                }
                break;
        }
        updateUI();
    }

    private void applyMapping(boolean register) {
        GamepadFacade.Mapping.Entry entry = null;
        switch (mSelectedType) {
            case BUTTONS_MAPPING:
                entry = mGamepad.newButtonMappingEntry(mDroneModel, mSelectedButtonsAction, mSelectedButtons);
                break;
            case AXIS_MAPPING:
                entry = mGamepad.newAxisMappingEntry(mDroneModel, mSelectedAxisAction, mSelectedAxis, mSelectedButtons);
                break;
        }
        if (register) {
            mGamepad.registerMappingEntry(entry);
        } else {
            mGamepad.unregisterMappingEntry(entry);
        }
        finish();
    }

    private void handleAxisMapping() {
        mEditingAxis = !mEditingAxis;
        mGamepad.setAxisEventListener(mEditingAxis ? mAxisListener : null)
                .grabInputs(NO_BUTTONS, mEditingAxis ? mGamepad.allAxes() : NO_AXES);
        updateUI();
    }

    private void handleButtonsMapping() {
        mEditingButtons = !mEditingButtons;
        mGamepad.setButtonEventListener(mEditingButtons ? mButtonsListener : null)
                .grabInputs(mEditingButtons ? mGamepad.allButtons() : NO_BUTTONS,
                        mEditingButtons ? mGamepad.allAxes() : NO_AXES);
        updateUI();
    }

    private void updateUI() {
        mDeleteBtn.setVisibility(mInEditMode ? View.VISIBLE : View.GONE);
        mEditAxisBtn.setText(mEditingAxis ? R.string.action_done : R.string.action_edit);
        mEditAxisBtn.setEnabled(!mEditingButtons);
        mEditButtonsBtn.setText(mEditingButtons ? R.string.action_done : R.string.action_edit);
        mEditButtonsBtn.setEnabled(!mEditingAxis);
        mTypeSpinner.setEnabled(!mInEditMode && !mEditingButtons && !mEditingAxis);
        mActionSpinner.setEnabled(!mInEditMode && !mEditingButtons && !mEditingAxis);
        mConfirmBtn.setEnabled(!mEditingButtons && !mEditingAxis
                               && (mSelectedType == GamepadFacade.Mapping.Entry.Type.AXIS_MAPPING && mSelectedAxis != null
                                   || mSelectedType != GamepadFacade.Mapping.Entry.Type.AXIS_MAPPING && !mSelectedButtons.isEmpty()));
        // update notice
        if (mEditingAxis) {
            mNoticeText.setText(R.string.hint_axis_mapping_setup);
        } else if (mEditingButtons) {
            mNoticeText.setText(R.string.hint_buttons_mapping_setup);
        } else if (mMappingOverride) {
            mNoticeText.setText(R.string.hint_warn_mapping_override);
        } else {
            mNoticeText.setText(null);
        }
        // update selected buttons/axis
        mAxisText.setText(mSelectedAxis == null ? null : mSelectedAxis.toString());
        mButtonsText.setText(TextUtils.join(", ", mSelectedButtons));
    }

    private final GamepadFacade.Axis.Event.Listener mAxisListener = new GamepadFacade.Axis.Event.Listener() {

        @Override
        public void onAxisEvent(@NonNull GamepadFacade.Axis.Event event, int value) {
            if (Math.abs(value) > 75) {
                mSelectedAxis = event;
                updateUI();
            }
        }
    };

    private final GamepadFacade.Button.Event.Listener mButtonsListener = (event, state) -> {
        if (state == GamepadFacade.Button.State.PRESSED) {
            return;
        }
        if (mSelectedButtons.contains(event)) {
            mSelectedButtons.remove(event);
        } else {
            mSelectedButtons.add(event);
        }
        updateUI();
    };

    private static final Set<GamepadFacade.Button> NO_BUTTONS = Collections.emptySet();

    private static final Set<GamepadFacade.Axis> NO_AXES = Collections.emptySet();
}
