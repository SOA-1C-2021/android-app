package com.soa.app;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Vista
    private TextView textProgressValue = null;
    CircularProgressBar circularProgressBar = null;

    // Shared Prefferences
    SharedPreferences sharedPref;

    // Sensor
    SensorManager sensorManager;
    Sensor sensor;
    private boolean sensorActive = false;
    private int steps = 0;
    private int goal = 0;
    private final int  REQ_CODE_ACTIVITY_RECOGNITION = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference ui components
        circularProgressBar = findViewById(R.id.circular_progress_bar);
        textProgressValue = findViewById(R.id.text_progress_value);

        // shared preferences
        sharedPref = this.getSharedPreferences(getString(R.string.settings_file_key),  Context.MODE_PRIVATE);

        goal = readPrefferenceInt(sharedPref, getString(R.string.settings_step_goal_key),
                getResources().getInteger(R.integer.settings_step_goal));
        steps = readPrefferenceInt(sharedPref, getString(R.string.current_step_count_key),
                getResources().getInteger(R.integer.current_step_count));

        circularProgressBar.setProgressMax(goal);
        circularProgressBar.setProgress(steps);
        textProgressValue.setText(String.format("%d / %d", steps, goal));

        // solicito permisos (api 29 o posterior)
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQ_CODE_ACTIVITY_RECOGNITION);
        }

        // inentamos instanciar el sensor detector de pasos
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if(sensor == null) {
            // si no está definido usamos el sensor de movimiento significativo como backup
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
            if(sensor == null) {
                Toast.makeText(
                    MainActivity.this,
                    "El dispositivo no cuenta con podometro",
                    Toast.LENGTH_LONG
                ).show();
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Selecciono el item
        int id = item.getItemId();
        if (id == R.id.history) {
            showHistory();
            return true;
        } else if (id == R.id.settings) {
            showSettings();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            steps += sensorEvent.values[0];
            circularProgressBar.setProgress(steps);
            textProgressValue.setText(String.format("%d / %d", steps, goal));

            // guardo la cuenta actual
            savePrefferenceInt(sharedPref, getString(R.string.current_step_count_key), steps);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final TriggerEventListener triggerEventListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent sensorEvent) {
            steps += sensorEvent.values[0];
            circularProgressBar.setProgress(steps);
            textProgressValue.setText(String.format("%d / %d", steps, goal));

            // guardo la configuración
            savePrefferenceInt(sharedPref, getString(R.string.current_step_count_key), steps);

            // El sensor de movimiento significativo, dispara un evento
            // y se "duerme", entonces lo volvemos a "despertar"
            sensorManager.requestTriggerSensor(triggerEventListener, sensor);
        }
    };

    private void showHistory() {
        Intent historyActivityIntent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(historyActivityIntent);
    }

    private void showSettings() {
        Intent settingsActivityIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsActivityIntent);
    }

    private void start() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.requestTriggerSensor(triggerEventListener, sensor);
    }

    private void stop() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
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
