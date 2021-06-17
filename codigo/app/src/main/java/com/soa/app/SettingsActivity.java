package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class SettingsActivity extends AppCompatActivity {
    private EditText editTextGoal = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editTextGoal = findViewById(R.id.edit_text_goal);
        Button btnSaveSettings = findViewById(R.id.button_save_settings);
        btnSaveSettings.setOnClickListener(buttonClickListener);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.settings_file_key), Context.MODE_PRIVATE);
        int defaultValue = getResources().getInteger(R.integer.settings_step_goal);
        int goal = sharedPref.getInt(getString(R.string.settings_step_goal_key), defaultValue);
        editTextGoal.setText(String.format("%s", goal));
    }

    private final View.OnClickListener buttonClickListener = v -> {
        int goal = Integer.parseInt(editTextGoal.getText().toString());
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.settings_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.settings_step_goal_key), goal);
        editor.commit();

        int defaultValue = getResources().getInteger(R.integer.settings_step_goal);
        goal = sharedPref.getInt(getString(R.string.settings_step_goal_key), defaultValue);
        editTextGoal.setText(String.format("%s", goal));
    };
}
