package com.flying_kiwi.dyna.Settings;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.flying_kiwi.dyna.R;
import com.flying_kiwi.dyna.Session;
import com.flying_kiwi.dyna.SessionType;
import com.flying_kiwi.dyna.Utils.FileManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;

public class RepeatersSettings extends Fragment {

    View view;
    HashMap<String, PresetSettings> presetSettingsMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.repeaters_settings_fragment, container, false);

        loadPresetOptions();
        setupAutoSelectText();

        view.findViewById(R.id.btnRepeaterStart).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("session", createSession());
            NavController navController = Navigation.findNavController(requireActivity(), R.id.fragmentContainerView);
            navController.navigate(R.id.action_repeatersSettings_to_repeaterLiveData, bundle);
        });
        view.findViewById(R.id.btnRepPresetSave).setOnClickListener(v -> {
            PresetSettings presetSettings = new PresetSettings(
                    getInt(R.id.etSets),
                    getInt(R.id.etReps),
                    getInt(R.id.etWork),
                    getInt(R.id.etRest),
                    getInt(R.id.etPause),
                    getInt(R.id.etCountdown),
                    getFloat(R.id.etTarget),
                    getFloat(R.id.etMarginMin),
                    getFloat(R.id.etMarginMax),
                    ((SwitchMaterial) view.findViewById(R.id.switchSound)).isChecked()
            );
            showSavePresetDialog(presetSettings);
        });

        Spinner presetSpinner = view.findViewById(R.id.spinRepeaterPreset);
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 0) return;
                String selectedValue = parentView.getItemAtPosition(position).toString();
                loadPreset(presetSettingsMap.get(selectedValue));
                Log.d("SpinnerSelection", "Selected value: " + selectedValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        return view;
    }

    private int getInt(int id) {
        TextInputEditText et = view.findViewById(id);
        try {
            return Integer.parseInt(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float getFloat(int id) {
        TextInputEditText et = view.findViewById(id);
        try {
            return Float.parseFloat(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public void loadPreset(PresetSettings settings) {
        setText(R.id.etSets, String.valueOf(settings.getNumSets()));
        setText(R.id.etReps, String.valueOf(settings.getNumReps()));
        setText(R.id.etWork, String.valueOf(settings.getWorkTime()));
        setText(R.id.etRest, String.valueOf(settings.getRestTime()));
        setText(R.id.etPause, String.valueOf(settings.getPauseTime()));
        setText(R.id.etCountdown, String.valueOf(settings.getCountdown()));
        setText(R.id.etTarget, String.valueOf(settings.getTargetWeight()));
        setText(R.id.etMarginMin, String.valueOf(settings.getTargetMarginMin()));
        setText(R.id.etMarginMax, String.valueOf(settings.getTargetMarginMax()));
        ((SwitchMaterial) view.findViewById(R.id.switchSound)).setChecked(settings.isSound());
    }

    private void setText(int id, String value) {
        ((TextInputEditText) view.findViewById(id)).setText(value);
    }

    void showSavePresetDialog(PresetSettings presetSettings) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter preset name");
        input.setHeight(51);

        new AlertDialog.Builder(requireContext())
                .setTitle("Save Preset")
                .setMessage("Please enter a name for the preset")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fileName = input.getText().toString().replace('/', '_').trim();
                    if (!fileName.isEmpty()) {
                        FileManager fm = new FileManager(requireContext());
                        presetSettings.setName(fileName);
                        if (fm.savePreset(presetSettings, SessionType.REPEATER)) {
                            presetSettingsMap.put(fileName, presetSettings);
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) ((Spinner) view.findViewById(R.id.spinRepeaterPreset)).getAdapter();
                            adapter.add(fileName);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(), "Preset saved", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Preset name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupAutoSelectText() {
        int[] ids = {R.id.etSets, R.id.etReps, R.id.etWork, R.id.etRest, R.id.etPause, R.id.etCountdown, R.id.etTarget, R.id.etMarginMin, R.id.etMarginMax};
        for (int id : ids) {
            ((TextInputEditText) view.findViewById(id)).setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) ((EditText) v).selectAll();
            });
        }
    }

    public Session createSession() {
        Session session = new Session(SessionType.REPEATER);
        session.setNumSets(getInt(R.id.etSets));
        session.setNumReps(getInt(R.id.etReps));
        session.setWorkTime(getInt(R.id.etWork));
        session.setRestTime(getInt(R.id.etRest));
        session.setPauseTime(getInt(R.id.etPause));
        session.setCountdown(getInt(R.id.etCountdown));
        session.setSound(((SwitchMaterial) view.findViewById(R.id.switchSound)).isChecked());
        session.setTargetWeight(getFloat(R.id.etTarget));
        session.setTargetMarginMin(getFloat(R.id.etMarginMin));
        session.setTargetMarginMax(getFloat(R.id.etMarginMax));
        return session;
    }

    public void loadPresetOptions() {
        HashMap<String, PresetSettings> presetSettings = new HashMap<>();
        FileManager fm = new FileManager(requireContext());
        Spinner presetSpinner = view.findViewById(R.id.spinRepeaterPreset);

        ArrayList<String> options = new ArrayList<>();
        options.add("Load a preset");
        for (PresetSettings settings : fm.getAllPresets(SessionType.REPEATER)) {
            presetSettings.put(settings.getName(), settings);
            options.add(settings.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);
        this.presetSettingsMap = presetSettings;
    }
}
