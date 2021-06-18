package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity {
    // Vista
    private EditText editTextGoal = null;
    private TextView textViewHiddenLabel = null;

    // Shared Prefferences
    SharedPreferences sharedPref;

    // Sensor
    SensorManager sensorManager;
    Sensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Vista
        editTextGoal = findViewById(R.id.edit_text_goal);
        textViewHiddenLabel = findViewById(R.id.hidden_label);
        textViewHiddenLabel.setVisibility(View.INVISIBLE);
        Button btnSaveSettings = findViewById(R.id.button_save_settings);
        btnSaveSettings.setOnClickListener(buttonClickListener);

        // Shared Prefferences
        sharedPref = this.getSharedPreferences(getString(R.string.settings_file_key),  Context.MODE_PRIVATE);

        int goal = readPrefferenceInt(sharedPref, getString(R.string.settings_step_goal_key),
                getResources().getInteger(R.integer.settings_step_goal));
        editTextGoal.setText(String.format("%d", goal));

        // Sensor
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(sensor == null) {
            Toast.makeText(
                SettingsActivity.this,
                "El dispositivo no cuenta con acelerómetro",
                Toast.LENGTH_LONG
            ).show();
        }
    }

   @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    private final View.OnClickListener buttonClickListener = v -> {
        // guardo la configuración
        int goal = Integer.parseInt(editTextGoal.getText().toString());
        savePrefferenceInt(sharedPref, getString(R.string.settings_step_goal_key), goal);

        // refresco el edit
        editTextGoal.setText(String.format("%s", goal));
        finish();
    };

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x = sensorEvent.values[0]; // 0: x, 1: y, 2: z
            if (x < -5) {
                // giro a la derecha
                textViewHiddenLabel.setVisibility(View.VISIBLE);
            } else if (x > 5) {
                // giro a la izquierda
                textViewHiddenLabel.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private void start() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stop() {
        sensorManager.unregisterListener(sensorEventListener);
    }

    private int readPrefferenceInt(SharedPreferences sharedPref, String key, int defaultValue) {
        if (sharedPref != null) {
            return sharedPref.getInt(key, defaultValue);
        }
        return 0;
    }

    private void savePrefferenceInt(SharedPreferences sharedPref, String key, int value) {
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(key, value);
            editor.apply();
        }
    }
}
