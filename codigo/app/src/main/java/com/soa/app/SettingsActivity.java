package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class SettingsActivity extends AppCompatActivity {
    // view
    private EditText editTextGoal = null;
    private TextView textViewHiddenLabel = null;
    private Button btnSaveSettings = null;

    // Shared Preferences
    SharedPreferences historySharedPreferences = null;
    SharedPreferences settingsSharedPreferences = null;

    // Sensor
    SensorManager sensorManager;
    Sensor sensor;

    // other
    SimpleDateFormat simpleDateFormat = null;
    private final int DEFAULT_DAILY_GOAL = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // reference ui components
        editTextGoal = findViewById(R.id.edit_text_goal);
        textViewHiddenLabel = findViewById(R.id.hidden_label);
        textViewHiddenLabel.setVisibility(View.INVISIBLE);
        btnSaveSettings = findViewById(R.id.button_save_settings);

        // instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // instantiate date formatter
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        // instantiate shared preferences files
        historySharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_history), Context.MODE_PRIVATE);
        settingsSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_settings), Context.MODE_PRIVATE);

        // add listeners
        btnSaveSettings.setOnClickListener(buttonClickListener);

        // perform initialization actions
        initializeUi();
    }

   @Override
    protected void onResume() {
        super.onResume();

       sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

       if (sensor == null) {
           Toast.makeText(SettingsActivity.this, "El dispositivo no cuenta con acelerómetro", Toast.LENGTH_LONG).show();
       } else {
           sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
       }
    }

    private final View.OnClickListener buttonClickListener = v -> {

        final String EMPTY_FIELD_ERROR = "El campo no puede estar vacío";

        String goal = editTextGoal.getText().toString();
        if (goal.isEmpty()) {
            editTextGoal.setError(EMPTY_FIELD_ERROR);
            return;
        }

        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve daily steps
        int loadedSteps = historySharedPreferences.getInt(formattedDate, 0);
        Log.i(getClass().getName(), "Loaded steps: " + Integer.toString(loadedSteps));

        // retrieve new daily goal
        int newDailyGoal = Integer.parseInt(goal);

        // check if new daily goal is higher than steps already taken today
        if (newDailyGoal < loadedSteps) {
            Toast.makeText(SettingsActivity.this, "Debe especificar una meta mayor a la cantidad de pasos realizada en el día de hoy.", Toast.LENGTH_LONG).show();
        } else {
            SharedPreferences.Editor editor = settingsSharedPreferences.edit();
            editor.putInt(formattedDate, newDailyGoal);
            editor.apply();
        }

        // finish activity
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

    private void initializeUi() {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve current daily goal
        int currentDailyGoal = settingsSharedPreferences.getInt(formattedDate, DEFAULT_DAILY_GOAL);
        Log.i(getClass().getName(), "Loaded daily goal: " + Integer.toString(currentDailyGoal));

        editTextGoal.setText(Integer.toString(currentDailyGoal));
    }

}
