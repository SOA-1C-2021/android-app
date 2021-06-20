package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
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
import com.soa.app.services.AppExecutors;
import com.soa.app.services.NetworkConnectivity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView textProgressValue = null;
    CircularProgressBar circularProgressBar = null;

    private SensorManager sensorManager = null;
    private boolean sensorActive = false;
    private int steps = 0;

    private AsyncTask eventTask;
    private NetworkConnectivity networkConnectivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference ui components
        circularProgressBar = findViewById(R.id.circular_progress_bar);
        textProgressValue = findViewById(R.id.text_progress_value);

        // instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // instantiate network connectivity checker class
        networkConnectivity = new NetworkConnectivity(AppExecutors.getInstance(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorActive = true;
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Toast.makeText(MainActivity.this, "El dispositivo no posee el sensor necesario para la aplicación", Toast.LENGTH_LONG).show();
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onDestroy() {
        if (eventTask != null) {
            eventTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorActive) {
            steps = (int) sensorEvent.values[0];
            circularProgressBar.setProgress(steps);
//            textProgressValue.setText();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
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

    private void showHistory() {
        Intent historyActivityIntent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(historyActivityIntent);
    }

    private void showSettings() {
        Intent settingsActivityIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsActivityIntent);
    }

    // TODO: Invocar launchThread() en el lugar del codigo donde se cumpla la meta
    private void launchThread() {
        networkConnectivity.checkInternetConnection((isConnected) -> {
            if (isConnected) {
                eventTask = new ThreadAsyncTask();
                eventTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Toast.makeText(MainActivity.this, "El dispositivo no puede conectarse a Internet, por favor revise la configuración de red", Toast.LENGTH_LONG).show();
            }
        });
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
                    out.close();
                    int responseCode = urlConnection.getResponseCode();
                    publishProgress(responseCode);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            int responseCode = (int)values[0];
            if (responseCode == HTTP_OK || responseCode == HTTP_CREATED) {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro se ha enviado al servidor!!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro no pudo enviarse al servidor :(", Toast.LENGTH_LONG).show();
            }
        }

        private String getToken() {
            // TODO: completar con el token real
            return "FAKE-TOKEN";
        }
    }
}
