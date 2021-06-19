package com.soa.app;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.gson.JsonObject;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    // Vista
    private TextView textDailyGoal = null;
    private TextView textCurrentCount = null;
    private TextView textProgressValue = null;
    CircularProgressBar circularProgressBar = null;

    // Shared Prefferences
    SharedPreferences sharedPref;

    // Sensor
    SensorManager sensorManager;
    Sensor sensor;
    private int steps = 0;
    private final int  REQ_CODE_ACTIVITY_RECOGNITION = 0;

    // Proceso en background
    ThreadAsyncTask eventTask;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference ui components
        textDailyGoal = findViewById(R.id.text_goal_value);
        textCurrentCount = findViewById(R.id.text_current_count);
        circularProgressBar = findViewById(R.id.circular_progress_bar);
        textProgressValue = findViewById(R.id.text_progress_value);

        // shared preferences
        sharedPref = this.getSharedPreferences(getString(R.string.settings_file_key),  Context.MODE_PRIVATE);

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            // solicito permisos (api 29 o posterior)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQ_CODE_ACTIVITY_RECOGNITION);
            }
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
        // actualizo la vista
        updateUI();
        // inicio los sensores
        start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // detengo los sensores
        stop();
    }

    @Override
    protected void onDestroy() {
        eventTask.cancel(true);
        super.onDestroy();
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
            // guardo la cuenta actual
            savePrefferenceInt(sharedPref, getString(R.string.current_step_count_key), steps);
            // actualizo la vista
            updateUI();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final TriggerEventListener triggerEventListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent sensorEvent) {
            steps += sensorEvent.values[0];
            // guardo la cuenta actual
            savePrefferenceInt(sharedPref, getString(R.string.current_step_count_key), steps);
            // actualizo la vista
            updateUI();

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
        if (sensor != null) {
            switch (sensor.getType()) {
                case Sensor.TYPE_STEP_DETECTOR: {
                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    break;
                }
                case Sensor.TYPE_SIGNIFICANT_MOTION: {
                    sensorManager.requestTriggerSensor(triggerEventListener, sensor);
                }
                default:
                    break;
            }
        }
    }

    private void stop() {
        if (sensor != null) {
            switch (sensor.getType()) {
                case Sensor.TYPE_STEP_DETECTOR: {
                    sensorManager.unregisterListener(sensorEventListener, sensor);
                    break;
                }
                case Sensor.TYPE_SIGNIFICANT_MOTION: {
                    sensorManager.cancelTriggerSensor(triggerEventListener, sensor);
                }
                default:
                    break;
            }
        }
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

    private void updateUI() {
        int goal = readPrefferenceInt(sharedPref, getString(R.string.settings_step_goal_key),
                getResources().getInteger(R.integer.settings_step_goal));
        steps = readPrefferenceInt(sharedPref, getString(R.string.current_step_count_key),
                getResources().getInteger(R.integer.current_step_count));

        if (steps == goal) {
            launchThread();
        }

        textDailyGoal.setText(String.format("%d", goal));
        textCurrentCount.setText(String.format("%d", steps));
        textProgressValue.setText(String.format("%%%d", Math.round(((float) steps/goal) * 100)));
        circularProgressBar.setProgressMax(goal);
        circularProgressBar.setProgress(steps);
    }

    private void launchThread() {
        eventTask = new ThreadAsyncTask();
        eventTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // Proceso en background
    @SuppressWarnings("rawtypes")
    private class ThreadAsyncTask extends AsyncTask  {
        @Override
        protected Object doInBackground(Object[] objects) {
            // Hilo secundario!
            // En este caso usamos un ecanismo de request
            // sincronico para justificar el uso de AsyncTask
            JsonObject obj = new JsonObject();
            obj.addProperty("env", "TEST");
            obj.addProperty("type_events", "dayly_goal_reached");
            obj.addProperty("description", "Meta de pasos alcanzada!!");
            try {
                URL url = new URL("http://so-unlam.net.ar/api/api/event");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty("Authorization", "Bearer " + getToken());
                    String body = obj.toString();
                    OutputStream out = urlConnection.getOutputStream();
                    out.write(body.getBytes());
                    out.flush();
                    int responseCode = urlConnection.getResponseCode();
                    publishProgress(responseCode);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                Log.d("asdasds", "adsdasd");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            if ((int)values[0] == 200) {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro se ha enviado al servidor!!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro no pudo enviarse al servidor :(", Toast.LENGTH_LONG).show();
            }
        }

        private String getToken() {
            return "FAKE-TOKEN";
        }
    }
}
